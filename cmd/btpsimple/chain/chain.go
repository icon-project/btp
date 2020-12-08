package chain

import (
	"encoding/base64"
	"encoding/hex"
	"fmt"
	"path/filepath"
	"sort"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	"github.com/icon-project/btp/cmd/btpsimple/module"
	"github.com/icon-project/btp/cmd/btpsimple/module/iconee"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/db"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mta"
	"github.com/icon-project/btp/common/wallet"
	"github.com/icon-project/btp/p2p"
)

const (
	DefaultDBDir  = "db"
	DefaultDBType = db.GoLevelDBBackend
	//Base64 in:out = 6:8
	DefaultBufferScaleOfBlockProof  = 0.5
	DefaultBufferNumberOfBlockProof = 100
	DefaultBufferInterval           = 5 * time.Second
	DefaultReconnectDelay           = time.Second
	DefaultRelayReSendInterval      = time.Second
)

type SimpleChain struct {
	s         module.Sender
	r         module.Receiver
	srv       *p2p.Server
	d         *p2p.Dialer
	c         *p2p.Conn
	src       module.BtpAddress
	acc       *mta.ExtAccumulator
	dst       module.BtpAddress
	bmcStatus *module.BMCLinkStatus //getstatus(dst.src)
	rm        *module.RelayMessage
	relayCh   chan *module.RelayMessage
	relaySeq  uint64
	rmBuffer  []*module.RelayMessage
	rmBufMtx  sync.Mutex
	ts        time.Time
	l         log.Logger
	cfg       *Config
}

//TODO change to normal func with mutex
func (s *SimpleChain) relayLoop() {
	s.l.Debugln("start relayLoop")
Loop:
	for {
		select {
		case rm, ok := <-s.relayCh:
			if !ok {
				s.l.Debugln("stop relayLoop")
				return
			}

			s.rmBufMtx.Lock()
			if len(s.rmBuffer) > 0 {
				sort.Slice(s.rmBuffer, func(i, j int) bool {
					return s.rmBuffer[i].Seq < s.rmBuffer[j].Seq
				})
				for _, trm := range s.rmBuffer {
					s.l.Debugf("relayLoop.rmBuffer from:%s, height:%s, bu:%d, bp:%v, rps:%d",
						trm.From.NetworkAddress(), relayMessageHeight(trm),
						len(trm.BlockUpdates), trm.BlockProof != nil, len(trm.ReceiptProofs))

					if p, err := s.s.Relay(trm); err != nil {
						s.l.Panicf("fail to Relay err:%+v", err)
					} else {
						go s._result(p, rm)
					}
				}
				s.rmBuffer = s.rmBuffer[:0]
			}
			s.rmBufMtx.Unlock()

			if rm == nil {
				s.l.Debugln("relayMessage is nil, ignore")
				continue Loop
			}

			s.l.Debugf("relayLoop from:%s, height:%s, bu:%d, bp:%v, rps:%d",
				rm.From.NetworkAddress(), relayMessageHeight(rm),
				len(rm.BlockUpdates), rm.BlockProof != nil, len(rm.ReceiptProofs))
			if p, err := s.s.Relay(rm); err != nil {
				//TODO retry
				s.l.Panicf("fail to Relay err:%+v", err)
			} else {
				go s._result(p, rm)
			}
		}
	}
}

func (s *SimpleChain) _result(p module.GetResultParam, rm *module.RelayMessage) {
	_, err := s.s.GetResult(p)
	if err != nil {
		if ec, ok := errors.CoderOf(err); ok {
			s.l.Debugf("result revert by %s from:%s, height:%s, bu:%d, bp:%v, rps:%d",
				module.BMVRevertCodeNames[ec.ErrorCode()], rm.From.NetworkAddress(), relayMessageHeight(rm),
				len(rm.BlockUpdates), rm.BlockProof != nil, len(rm.ReceiptProofs))
			switch ec.ErrorCode() {
			case module.BMVRevertInvalidBlockWitnessOld:
				s.l.Debugf("revert by BMVRevertInvalidBlockWitnessOld from:%s, height:%s, bu:%d, bp:%v, rps:%d",
					rm.From.NetworkAddress(), relayMessageHeight(rm),
					len(rm.BlockUpdates), rm.BlockProof != nil, len(rm.ReceiptProofs))
				if rm.BlockProof, err = s.newBlockProof(rm.BlockProof.BlockWitness.Height, rm.BlockProof.Header); err != nil {
					s.l.Panicf("fail to newBlockProof err:%+v", err)
				}
				fallthrough
			case module.BMVRevertInvalidSequenceHigher, module.BMVRevertInvalidBlockUpdateHigher, module.BMVRevertInvalidBlockProofHigher:
				s.rmBufMtx.Lock()
				s.rmBuffer = append(s.rmBuffer, rm)
				s.rmBufMtx.Unlock()
			case module.BMVRevertInvalidSequence:
				s.l.Infof("drop by BMVRevertInvalidSequence from:%s, height:%s, bu:%d, bp:%v, rps:%d",
					rm.From.NetworkAddress(), relayMessageHeight(rm),
					len(rm.BlockUpdates), rm.BlockProof != nil, len(rm.ReceiptProofs))
			case module.BMVRevertInvalidBlockUpdateLower:
				s.l.Infof("drop by BMVRevertInvalidBlockUpdateLower from:%s, height:%s by %s, bu:%d, bp:%v, rps:%d",
					rm.From.NetworkAddress(), relayMessageHeight(rm),
					len(rm.BlockUpdates), rm.BlockProof != nil, len(rm.ReceiptProofs))
			default:
				s.l.Panicf("fail to GetResult err:%+v, txHash:%v from:%s, height:%s, bu:%d, bp:%v, rps:%d",
					err, p, rm.From.NetworkAddress(), relayMessageHeight(rm),
					len(rm.BlockUpdates), rm.BlockProof != nil, len(rm.ReceiptProofs))
			}
		} else {
			s.l.Panicf("fail to GetResult txHash:%v from:%s, height:%s by err:%+v, bu:%d, bp:%v, rps:%d",
				p, rm.From.NetworkAddress(), relayMessageHeight(rm), err,
				len(rm.BlockUpdates), rm.BlockProof != nil, len(rm.ReceiptProofs))
		}
	} else {
		s.l.Debugf("GetResult finish from:%s, height:%s, bu:%d, bp:%v, rps:%d",
			rm.From.NetworkAddress(), relayMessageHeight(rm),
			len(rm.BlockUpdates), rm.BlockProof != nil, len(rm.ReceiptProofs))
	}
	return
}

func (s *SimpleChain) _relayWithLimit(limit, buSize int, bu *module.BlockUpdate, rm *module.RelayMessage) {
	bpSize := 0
	for buSize > 0 || len(rm.ReceiptProofs) > 0 {
		rmSize := buSize + bpSize
		if rmSize == 0 {
			if rm.BlockProof == nil {
				var err error
				if rm.BlockProof, err = s.newBlockProof(bu.Height, bu.Header); err != nil {
					s.l.Panicf("fail to newBlockProof err:%+v", err)
				}
			}
			bpSize += len(rm.BlockProof.Header)
			bpSize += 8 //rm.BlockProof.BlockWitness.Height
			bpSize += len(rm.BlockProof.BlockWitness.Witness) * mta.HashSize
		}
		lastRp := -1
		lastEp := -1
	RpLoop:
		for i, rp := range rm.ReceiptProofs {
			rpSize := len(rp.Proof)
			rmSize += rpSize
			if rmSize > limit {
				rmSize -= rpSize
				break RpLoop
			}
			for j, ep := range rp.EventProofs {
				epSize := len(ep.Proof)
				rmSize += epSize
				if rmSize > limit {
					lastEp = j - 1
					rmSize -= epSize
					if lastEp < 0 {
						rmSize -= rpSize
						if lastRp < 0 && buSize == 0 {
							s.l.Panicf("invalid ReceiptProof, rpSize:%d + epSize:%d greater than limit:%d",
								rpSize, epSize, limit)
						}
					}
					break RpLoop
				}
			}
			lastEp = -1
			lastRp = i
		}

		trm := &module.RelayMessage{
			From: rm.From,
		}

		if buSize > 0 {
			if buSize > limit {
				lastBu := len(rm.BlockUpdates) - 1
				trm.BlockUpdates = rm.BlockUpdates[:lastBu]
				rm.BlockUpdates = rm.BlockUpdates[lastBu:]
				//ignored bu.Height, bu.BlockHash, bu.Header
				buSize = len(bu.Proof)
			} else {
				trm.BlockUpdates = rm.BlockUpdates[:]
				rm.BlockUpdates = rm.BlockUpdates[:0]
				buSize = 0
			}
		} else {
			trm.BlockProof = rm.BlockProof
		}

		if lastRp >= 0 {
			lastRp += 1
			trm.ReceiptProofs = rm.ReceiptProofs[:lastRp]
			rm.ReceiptProofs = rm.ReceiptProofs[lastRp:]
			if len(trm.BlockUpdates) == 0 {
				trm.BlockProof = rm.BlockProof
			}
		}

		if lastEp >= 0 {
			lastEp += 1
			if trm.ReceiptProofs == nil {
				trm.ReceiptProofs = make([]*module.ReceiptProof, 0)
			}
			rp := &module.ReceiptProof{
				Index:       rm.ReceiptProofs[0].Index,
				Proof:       rm.ReceiptProofs[0].Proof,
				EventProofs: rm.ReceiptProofs[0].EventProofs[:lastEp],
			}
			trm.ReceiptProofs = append(trm.ReceiptProofs, rp)
			rm.ReceiptProofs[0].EventProofs = rm.ReceiptProofs[0].EventProofs[lastEp:]
		}
		trm.Seq = atomic.AddUint64(&s.relaySeq, 1)
		s.relayCh <- trm
	}
}

func (s *SimpleChain) relay(bu *module.BlockUpdate, rps []*module.ReceiptProof, bp *module.BlockProof) {
	if s.d != nil {
		if s.c == nil {
			var err error
			for {
				if s.c, err = s.d.Dial(s); err != nil {
					s.l.Infof("fail to dial err:%+v", err)
					<-time.After(DefaultReconnectDelay)
				} else {
					break
				}
			}
		}
		req := &RelayRequest{
			From:          s.src.String(),
			BlockUpdate:   bu,
			ReceiptProofs: rps,
			BlockProof:    bp,
		}
		pkt := p2p.NewPacket(ProtoRelay, s.encode(req))
		for {
			s.l.Debugf("RelayRequest from:%s, height:%d, bp:%v, rps:%d",
				s.src.NetworkAddress(), bu.Height, req.BlockProof != nil, len(req.ReceiptProofs))
			rpkt, err := s.c.SendAndReceive(pkt)
			if err != nil {
				s.c.CloseByError(err)
				s.l.Panicf("fail to send and receive err:%+v", err)
			}
			resp := &RelayResponse{}
			s.decode(rpkt.Payload(), resp)
			if resp.Error != "" {
				s.l.Debugf("RelayResponse.Error:%s", resp.Error)
				<-time.After(DefaultRelayReSendInterval)
			} else {
				break
			}
		}
		return
	}

	rm := s.rm
	if rm == nil {
		rm = &module.RelayMessage{
			From:          s.src,
			BlockUpdates:  make([]*module.BlockUpdate, 0),
			BlockProof:    nil,
			ReceiptProofs: nil,
		}
		s.rm = rm
	}
	rm.ReceiptProofs = rps
	limit := int(float64(s.s.LimitOfTransactionSize()) * DefaultBufferScaleOfBlockProof)
	if bp == nil {
		nbu := len(rm.BlockUpdates)
		if nbu > 0 {
			next := rm.BlockUpdates[nbu-1].Height + 1
			if next < bu.Height {
				s.l.Panicf("found missing blockUpdate dst:%s bu:%d expected:%d ",
					s.dst.String(), bu.Height, next)
			} else if next > bu.Height {
				if len(rps) > 0 {
					trm := &module.RelayMessage{
						From:          rm.From,
						BlockUpdates:  make([]*module.BlockUpdate, 0),
						ReceiptProofs: rps,
					}
					min := rm.BlockUpdates[0].Height
					if min > bu.Height {
						trm.BlockUpdates = append(trm.BlockUpdates, bu)
					} else {
						trm.BlockUpdates = append(trm.BlockUpdates, rm.BlockUpdates[0:bu.Height-min+1]...)
						rm.BlockUpdates = rm.BlockUpdates[bu.Height-min+1:]
					}
					buSize := 0
					for _, u := range trm.BlockUpdates {
						//ignored u.Height, u.BlockHash, u.Header
						buSize += len(u.Proof)
					}
					s._relayWithLimit(limit, buSize, bu, trm)
					bu = rm.BlockUpdates[len(rm.BlockUpdates)-1]
					rps = rps[:0]
				} else {
					s.l.Infof("ignore duplicated blockUpdate dst:%s height:%d expected:%d",
						s.dst.String(), bu.Height, next)
				}
			} else {
				rm.BlockUpdates = append(rm.BlockUpdates, bu)
			}
		} else {
			rm.BlockUpdates = append(rm.BlockUpdates, bu)
		}
	} else {
		if len(bp.Header) == 0 || bp.BlockWitness == nil {
			s.l.Panicf("invalid BlockProof")
		}
		rm.BlockProof = bp
	}
	buSize := 0
	for _, u := range rm.BlockUpdates {
		//ignored u.Height, u.BlockHash, u.Header
		buSize += len(u.Proof)
	}
	now := time.Now()
	if len(rps) < 1 && len(rm.BlockUpdates) < DefaultBufferNumberOfBlockProof &&
		buSize < limit && now.Sub(s.ts) < DefaultBufferInterval {
		return
	}
	s.rm = nil
	s._relayWithLimit(limit, buSize, bu, rm)
	s.ts = time.Now()
}

func (s *SimpleChain) OnBlock(bu *module.BlockUpdate, rps []*module.ReceiptProof) {
	s.l.Tracef("onBlock height:%d, bu.Height:%d", s.acc.Height(), bu.Height)

	var bp *module.BlockProof
	next := s.acc.Height() + 1
	if next < bu.Height {
		s.l.Panicf("found missing block next:%d bu:%d", next, bu.Height)
	}
	if next == bu.Height {
		//s.l.Debugf("onBlock acc.AddHash %d, %s", bu.Height, hex.EncodeToString(bu.BlockHash))
		s.acc.AddHash(bu.BlockHash)
		if err := s.acc.Flush(); err != nil {
			//TODO MTA Flush error handling
			s.l.Panicf("fail to MTA Flush err:%+v", err)
		}
	}

	if s.bmcStatus.Verifier.Height >= bu.Height { //old blocks
		if len(rps) > 0 {
			s.l.Debugf("onBlock relay old block dst:%s, bu.Height:%d, len(rps):%d",
				s.dst.NetworkAddress(), bu.Height, len(rps))
			var err error
			if bp, err = s.newBlockProof(bu.Height, bu.Header); err != nil {
				s.l.Panicf("fail to newBlockProof err:%+v", err)
			}
			if err = s.acc.VerifyAt(mta.HashesToWitness(bp.BlockWitness.Witness, bu.Height-1-s.acc.Offset()),
				bu.BlockHash, s.bmcStatus.Verifier.Height, s.bmcStatus.Verifier.Offset); err != nil {
				s.l.Panicf("fail to VerifyAt hash:%s err:%+v",hex.EncodeToString(bu.BlockHash), err)
			}
		} else {
			if s.d == nil {
				s.l.Tracef("onBlock ignore old block dst:%s, bu.Height:%d, dst.Verifier:%d",
					s.dst.NetworkAddress(), bu.Height, s.bmcStatus.Verifier.Height)
				return
			}
		}
	} else {
		s.l.Tracef("onBlock relay dst:%s, bu.Height:%d, rps:%d",
			s.dst.NetworkAddress(), bu.Height, len(rps))
	}
	s.relay(bu, rps, bp)
}

func dumpBlockProof(bp *module.BlockProof) string {
	fmt.Print("dumpBlockProof.height:", bp.BlockWitness.Height, ",witness:[")
	for _, w := range bp.BlockWitness.Witness {
		fmt.Print(hex.EncodeToString(w), ",")
	}
	fmt.Println("]")
	b, _ := codec.RLP.MarshalToBytes(bp)
	return base64.URLEncoding.EncodeToString(b)
}

func (s *SimpleChain) newBlockProof(height int64, header []byte) (*module.BlockProof, error) {
	if _, err := s.GetStatus(); err != nil {
		return nil, err
	}

	if n, err := s.acc.GetNode(height); err != nil {
		s.l.Debugf("height:%d, accLen:%d, err:%+v",
			height,
			s.acc.Len(),
			err)
	} else {
		s.l.Debugf("height:%d, accLen:%d, hash:%s\n",
			height,
			s.acc.Len(),
			hex.EncodeToString(n.Hash()))
	}

	//at := s.bmcStatus.Verifier.Height
	//w, err := s.acc.WitnessForWithAccLength(height-s.acc.Offset(), at-s.bmcStatus.Verifier.Offset)
	at, w, err := s.acc.WitnessForAt(height, s.bmcStatus.Verifier.Height, s.bmcStatus.Verifier.Offset)
	if err != nil {
		return nil, err
	}

	s.l.Debugf("newBlockProof height:%d, at:%d, w:%d", height, at, len(w))
	bw := &module.BlockWitness{
		Height:  at,
		Witness: mta.WitnessesToHashes(w),
	}
	bp := &module.BlockProof{
		Header:       header,
		BlockWitness: bw,
	}
	fmt.Println(dumpBlockProof(bp))
	return bp, nil
}

func (s *SimpleChain) prepareDatabase(offset int64) error {
	s.l.Debugln("open database",filepath.Join(s.cfg.AbsBaseDir(),s.cfg.Dst.Address.NetworkAddress()))
	database, err := db.Open(s.cfg.AbsBaseDir(), string(DefaultDBType), s.cfg.Dst.Address.NetworkAddress())
	if err != nil {
		return errors.Wrap(err, "fail to open database")
	}
	defer func() {
		if err != nil {
			database.Close()
		}
	}()
	var bk db.Bucket
	if bk, err = database.GetBucket("Accumulator"); err != nil {
		return err
	}
	k := []byte("Accumulator")
	if offset < 0 {
		offset = 0
	}
	s.acc = mta.NewExtAccumulator(k, bk, offset)
	if bk.Has(k) {
		//offset will be ignore
		if err = s.acc.Recover(); err != nil {
			err = errors.Wrap(err, "fail to acc.Recover")
			//TODO MTA Recover error handling
			return err
		}
		s.l.Debugf("recover Accumulator offset:%d, height:%d", s.acc.Offset(), s.acc.Height())

		////TODO sync offset
		//if s.acc.Offset() > offset {
		//	hashes := make([][]byte, s.acc.Offset() - offset)
		//	for i := 0; i < len(hashes); i++ {
		//		hashes[i] = getBlockHashByHeight(offset)
		//		offset++
		//	}
		//	s.acc.AddHashesToHead(hashes)
		//} else if s.acc.Offset() < offset {
		//	s.acc.RemoveHashesFromHead(offset-s.acc.Offset())
		//}
	}
	return nil
}

func (s *SimpleChain) GetStatus() (*module.BMCLinkStatus, error) {
	if s.d != nil {
		if s.c == nil {
			if c, err := s.d.Dial(s); err != nil {
				return nil, err
			} else {
				s.c = c
			}
		}
		req := &GetLinkStatusRequest{Link: s.src.String()}
		pkt := p2p.NewPacket(ProtoGetLinkStatus, s.encode(req))
		rpkt, err := s.c.SendAndReceive(pkt)
		if err != nil {
			return nil, err
		}
		resp := &GetLinkStatusResponse{}
		s.decode(rpkt.Payload(), resp)
		if resp.Error != "" {
			return nil, fmt.Errorf(resp.Error)
		}
		s.bmcStatus = resp.LinkStatus
	} else {
		bmcStatus, err := s.s.GetStatus()
		if err != nil {
			return nil, err
		}
		s.bmcStatus = bmcStatus
	}
	return s.bmcStatus, nil
}

func (s *SimpleChain) _init() error {
	if _, err := s.GetStatus(); err != nil {
		return err
	}
	if s.relayCh == nil {
		s.relayCh = make(chan *module.RelayMessage)
		s.rmBuffer = make([]*module.RelayMessage, 0)
		go s.relayLoop()
	}
	s.l.Debugf("_init height:%d, dst(%s, height:%d, seq:%d, last:%d), receive:%d",
		s.acc.Height(), s.dst, s.bmcStatus.Verifier.Height, s.bmcStatus.RxSeq, s.bmcStatus.Verifier.LastHeight, s.receiveHeight())
	return nil
}

func (s *SimpleChain) receiveHeight() int64 {
	//min(max(s.acc.Height(), s.bmcStatus.Verifier.Offset), s.bmcStatus.Verifier.LastHeight)
	max := s.acc.Height()
	if max < s.bmcStatus.Verifier.Offset {
		max = s.bmcStatus.Verifier.Offset
	}
	max += 1
	min := s.bmcStatus.Verifier.LastHeight
	if max < min {
		min = max
	}
	return min
}

func (s *SimpleChain) Serve() error {
	if err := s._init(); err != nil {
		return err
	}
	if s.srv != nil {
		return s.srv.ListenAndServe()
	} else {
		return s.r.ReceiveLoop(
			s.receiveHeight(),
			s.bmcStatus.RxSeq,
			s.OnBlock,
			nil)
	}
}

func (s *SimpleChain) Start() error {
	if err := s._init(); err != nil {
		return err
	}
	errCh := make(chan error)
	go func() {
		if s.srv != nil {
			errCh <- s.srv.Start()
		} else {
			err := s.r.ReceiveLoop(
				s.receiveHeight(),
				s.bmcStatus.RxSeq,
				s.OnBlock,
				func() {
					errCh <- nil
				})
			select {
			case errCh <- err:
			default:
			}
		}
	}()
	return <-errCh
}

func (s *SimpleChain) Stop() error {
	if s.srv != nil {
		if s.c != nil {
			s.c.Close("stop")
		}
		return s.srv.Stop()
	} else {
		s.r.StopReceiveLoop()
	}
	s.rm = nil
	return nil
}

func (s *SimpleChain) encode(v interface{}) []byte {
	return codec.MP.MustMarshalToBytes(v)
}

func (s *SimpleChain) decode(b []byte, v interface{}) {
	codec.MP.MustUnmarshalFromBytes(b, v)
}

func (s *SimpleChain) OnConn(c *p2p.Conn) {
	s.l.Debugln("OnConn", c)
	if c.Incoming() {
		if s.cfg.Dst.Address.String() != c.Channel() {
			c.Close(fmt.Sprintf("mismatch connection %s", c.String()))
			return
		}
		if s.c != nil {
			s.c.Close(fmt.Sprintf("duplicated connection %s", c.String()))
		}
		s.c = c
	}
}

func (s *SimpleChain) OnPacket(pkt *p2p.Packet, c *p2p.Conn) {
	s.l.Traceln("OnPacket", pkt, c)
	if c.Incoming() && pkt.Sn() > 0 {
		switch pkt.Protocol() {
		case ProtoRelay:
			req := &RelayRequest{}
			resp := &RelayResponse{}
			s.decode(pkt.Payload(), req)
			if s.cfg.Src.Address.String() != req.From {
				resp.Error = fmt.Sprintf("mismatch address")
			} else {
				s.OnBlock(req.BlockUpdate, req.ReceiptProofs)
			}
			rpkt := pkt.Response(s.encode(resp))
			if err := c.Send(rpkt); err != nil {
				s.l.Debugf("fail to send ProtoRelay response height:%d sn:%d resp.Error:%s err:%+v",
					req.BlockUpdate.Height, rpkt.Sn(), resp.Error, err)
			}
		case ProtoGetLinkStatus:
			req := &GetLinkStatusRequest{}
			resp := &GetLinkStatusResponse{}
			s.decode(pkt.Payload(), req)
			resp.Link = req.Link
			if s.cfg.Src.Address.String() != req.Link {
				resp.Error = fmt.Sprintf("mismatch address")
			} else {
				bmcStatus, sErr := s.GetStatus()
				if sErr != nil {
					resp.Error = sErr.Error()
				} else {
					resp.LinkStatus = &module.BMCLinkStatus{}
					*resp.LinkStatus = *bmcStatus
					if resp.LinkStatus.Verifier.LastHeight > s.acc.Height() {
						resp.LinkStatus.Verifier.LastHeight = s.acc.Height()
					}
				}
			}
			rpkt := pkt.Response(s.encode(resp))
			if err := c.Send(rpkt); err != nil {
				s.l.Debugf("fail to send ProtoGetLinkStatus response sn:%d resp.Error:%s err:%+v",
					rpkt.Sn(), resp.Error, err)
			}
		default:
			//Ignore
		}
	}
}

func (s *SimpleChain) OnError(err error, c *p2p.Conn, pkt *p2p.Packet) {
	panic("implement me")
}

func (s *SimpleChain) OnClose(c *p2p.Conn) {
	s.l.Debugln("OnClose", c, c.CloseInfo())
	s.c = nil
}

func NewSimpleChain(cfg *Config, w wallet.Wallet, l log.Logger) (*SimpleChain, error) {
	s := &SimpleChain{
		src: cfg.Src.Address,
		dst: cfg.Dst.Address,
		cfg: cfg,
		l: l.WithFields(log.Fields{log.FieldKeyChain:
		fmt.Sprintf("%s->%s", cfg.Src.Address.NetworkAddress(), cfg.Dst.Address.NetworkAddress())}),
	}

	if IsP2PAddress(cfg.Src.Endpoint) {
		s.srv = &p2p.Server{
			Address:       ResolveP2PAddress(cfg.Src.Endpoint),
			Logger:        l,
			Handler:       s,
			Authenticator: p2p.NewAuthenticator(w, s.l),
		}
	} else {
		s.r = iconee.NewReceiver(cfg.Src.Address, cfg.Dst.Address, cfg.Src.Endpoint, cfg.Src.Options, s.l)
	}

	if IsP2PAddress(cfg.Dst.Endpoint) {
		s.d = &p2p.Dialer{
			Chain:         cfg.Dst.Address.String(),
			Address:       ResolveP2PAddress(cfg.Dst.Endpoint),
			Logger:        l,
			Authenticator: p2p.NewAuthenticator(w, s.l),
		}
	} else {
		s.s = iconee.NewSender(cfg.Src.Address, cfg.Dst.Address, w, cfg.Dst.Endpoint, cfg.Dst.Options, s.l)
	}
	if err := s.prepareDatabase(cfg.Offset); err != nil {
		return nil, err
	}
	return s, nil
}

func relayMessageHeight(rm *module.RelayMessage) string {
	var height string
	if len(rm.BlockUpdates) > 0 {
		ss := make([]string, 0)
		for _, bu := range rm.BlockUpdates {
			ss = append(ss, fmt.Sprintf("%d", bu.Height))
		}
		height = strings.Join(ss, ",")
	} else {
		height = fmt.Sprintf("%d", rm.BlockProof.BlockWitness.Height)
	}
	return height
}
