package substrate

import (
	"fmt"
	"reflect"
	"runtime"
	"sort"
	"time"

	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/gammazero/workerpool"
	"github.com/icon-project/btp/common/go-ethereum/rpc"
	"github.com/icon-project/btp/common/log"
	scale "github.com/itering/scale.go"
	"github.com/itering/scale.go/source"
	scaletypes "github.com/itering/scale.go/types"
	"github.com/itering/scale.go/utiles"
)

type SubstrateAPI struct {
	*rpc.Client
	keepSubscribeFinalizeHead chan bool
	blockInterval             time.Duration
	eventDecoder              scale.EventsDecoder
	scaleDecoderOption        scaletypes.ScaleDecoderOption
	log                       log.Logger
}

func NewSubstrateClient(url string) (*SubstrateAPI, error) {
	cl, err := rpc.Dial(url)
	return &SubstrateAPI{
		Client:                    cl,
		keepSubscribeFinalizeHead: make(chan bool),
		blockInterval:             time.Second * 3,
		log:                       log.GlobalLogger(),
	}, err
}

func (c *SubstrateAPI) Init() {
	metadataRaw := c.GetMetadataRawLatest()
	m := scale.MetadataDecoder{}
	m.Init(utiles.HexToBytes(metadataRaw))
	if err := m.Process(); err != nil {
		log.Errorf("Init: metadaDecoderProcess fail %v", err)
	}

	runtimeVersion, err := c.GetRuntimeVersionLatest()
	if err != nil {
		c.log.Panicf("Init: can't fetch RuntimeVersionLatest")
	}

	if err != nil {
		c.log.Panicf("Init: can't fetch scale module")
	}

	c.eventDecoder = scale.EventsDecoder{}
	c.scaleDecoderOption = scaletypes.ScaleDecoderOption{Metadata: &m.Metadata, Spec: int(runtimeVersion.SpecVersion)}

	if typesDefinitionMap[runtimeVersion.SpecName] == "" {
		c.log.Panicf("Init: can't fetch scale type %s", runtimeVersion.SpecName)
	}

	typesDefinition := []byte(typesDefinitionMap[runtimeVersion.SpecName])

	scaletypes.RegCustomTypes(source.LoadTypeRegistry(typesDefinition))
}

func (c *SubstrateAPI) callWithBlockHash(target interface{}, method string, blockHash *SubstrateHash, args ...interface{}) error {
	if blockHash == nil {
		err := c.Call(target, method, args...)
		if err != nil {
			return err
		}
		return nil
	}
	hexHash, err := types.Hex(*blockHash)
	if err != nil {
		return err
	}
	hargs := append(args, hexHash)
	err = c.Call(target, method, hargs...)
	if err != nil {
		return err
	}
	return nil
}

func (c *SubstrateAPI) GetRuntimeVersionLatest() (*RuntimeVersion, error) {
	return c.getRuntimeVersion(nil)
}

func (c *SubstrateAPI) getRuntimeVersion(blockHash *SubstrateHash) (*RuntimeVersion, error) {
	var runtimeVersion RuntimeVersion
	err := c.callWithBlockHash(&runtimeVersion, "state_getRuntimeVersion", blockHash)
	if err != nil {
		return nil, err
	}
	return &runtimeVersion, err
}

func (c *SubstrateAPI) GetMetadataRawLatest() string {
	res, err := c.getMetadataRaw(nil)
	log.Warnf("GetMetadataRawLatest: fail %v", err)
	return res
}

func (c *SubstrateAPI) getMetadataRaw(blockHash *SubstrateHash) (string, error) {
	var res string
	err := c.callWithBlockHash(&res, "state_getMetadata", blockHash)
	if err != nil {
		return "", err
	}

	return res, err
}

func (c *SubstrateAPI) GetFinalizedHead() (SubstrateHash, error) {
	var res string

	err := c.Call(&res, "chain_getFinalizedHead")
	if err != nil {
		return SubstrateHash{}, err
	}

	return types.NewHashFromHexString(res)
}

func (c *SubstrateAPI) GetHeader(blockHash SubstrateHash) (*SubstrateHeader, error) {
	return c.getHeader(&blockHash)
}

func (c *SubstrateAPI) GetHeaderLatest() (*types.Header, error) {
	return c.getHeader(nil)
}

func (c *SubstrateAPI) getHeader(blockHash *SubstrateHash) (*types.Header, error) {
	var Header types.Header
	err := c.callWithBlockHash(&Header, "chain_getHeader", blockHash)
	if err != nil {
		return nil, err
	}
	return &Header, err
}

func (c *SubstrateAPI) GetBlockHash(blockNumber uint64) (SubstrateHash, error) {
	return c.getBlockHash(&blockNumber)
}

func (c *SubstrateAPI) GetBlockHashLatest() (SubstrateHash, error) {
	return c.getBlockHash(nil)
}

func (c *SubstrateAPI) getBlockHash(blockNumber *uint64) (SubstrateHash, error) {
	var res string
	var err error

	if blockNumber == nil {
		err = c.Call(&res, "chain_getBlockHash")
	} else {
		err = c.Call(&res, "chain_getBlockHash", *blockNumber)
	}

	if err != nil {
		return SubstrateHash{}, err
	}

	return types.NewHashFromHexString(res)
}

func (c *SubstrateAPI) GetStorageRaw(key SubstrateStorageKey, blockHash SubstrateHash) (*SubstrateStorageDataRaw, error) {
	return c.getStorageRaw(key, &blockHash)
}

func (c *SubstrateAPI) getStorageRaw(key types.StorageKey, blockHash *SubstrateHash) (*types.StorageDataRaw, error) {
	var res string
	err := c.callWithBlockHash(&res, "state_getStorage", blockHash, key.Hex())
	if err != nil {
		return nil, err
	}

	bz, err := types.HexDecodeString(res)
	if err != nil {
		return nil, err
	}

	data := types.NewStorageDataRaw(bz)
	return &data, nil
}

func (c *SubstrateAPI) GetSpecName() string {
	rtv, err := c.GetRuntimeVersionLatest()
	log.Warnf("GetMetadataRawLatest: fail %v", err)
	return rtv.SpecName
}

func (c *SubstrateAPI) GetHeaderByBlockNumber(blockNumber SubstrateBlockNumber) (*SubstrateHeader, error) {
	blockHash, err := c.GetBlockHash(uint64(blockNumber))
	if err != nil {
		return nil, err
	}

	return c.GetHeader(blockHash)
}

func (c *SubstrateAPI) GetBlockHeaderByBlockNumbers(blockNumbers []SubstrateBlockNumber) ([]SubstrateHeader, error) {
	headers := make([]SubstrateHeader, 0)

	wp := workerpool.New(runtime.NumCPU())
	rspChan := make(chan struct {
		header *SubstrateHeader
		err    error
	}, len(blockNumbers))

	for _, blockNumber := range blockNumbers {
		blockNumber := blockNumber
		wp.Submit(func() {
			blockHash, err := c.GetBlockHash(uint64(blockNumber))
			if err != nil {
				rspChan <- struct {
					header *SubstrateHeader
					err    error
				}{
					header: nil,
					err:    err,
				}
				return
			}

			header, err := c.GetHeader(blockHash)
			c.log.Tracef("GetBlockHeaderByBlockNumbers: get header of %d", blockNumber)

			rspChan <- struct {
				header *SubstrateHeader
				err    error
			}{
				header: header,
				err:    err,
			}
		})
	}

	wp.StopWait()
	close(rspChan)
	for rsp := range rspChan {
		if rsp.err != nil {
			c.log.Panicf("GetBlockHeaderByBlockNumbers: fails err:%+v", rsp.err)
		}

		headers = append(headers, *rsp.header)
	}

	sort.Slice(headers, func(i, j int) bool {
		return headers[i].Number < headers[j].Number
	})

	return headers, nil
}

func (c *SubstrateAPI) GetBlockHashesByRange(start SubstrateBlockNumber, end SubstrateBlockNumber) ([]SubstrateHash, error) {
	var blockNumbers []SubstrateBlockNumber
	hashes := make([]SubstrateHash, 0)

	for i := start; i <= end; i++ {
		blockNumbers = append(blockNumbers, i)
	}

	blockHeaders, err := c.GetBlockHeaderByBlockNumbers(blockNumbers)
	if err != nil {
		return nil, err
	}

	hash, err := c.GetBlockHash(uint64(blockNumbers[len(blockNumbers)-1]))
	if err != nil {
		return nil, err
	}

	for i, blockHeader := range blockHeaders {
		if i == 0 {
			continue
		}

		hashes = append(hashes, blockHeader.ParentHash)
	}

	hashes = append(hashes, hash)

	return hashes, nil
}

func (c *SubstrateAPI) GetReadProof(key SubstrateStorageKey, hash SubstrateHash) (SubstrateReadProof, error) {
	var res SubstrateReadProof
	err := c.Call(&res, "state_getReadProof", []string{key.Hex()}, hash.Hex())
	return res, err
}

func (c *SubstrateAPI) GetSystemEventStorageKey(blockhash SubstrateHash) (SubstrateStorageKey, error) {
	key := EncodeStorageKey(c.scaleDecoderOption.Metadata, "System", "Events")

	return NewStorageKey(key.EncodeKey), nil
}

func (c *SubstrateAPI) GetGrandpaCurrentSetId(blockHash SubstrateHash) (types.U64, error) {
	key := EncodeStorageKey(c.scaleDecoderOption.Metadata, "Grandpa", "CurrentSetId")

	storageRaw, err := c.GetStorageRaw(NewStorageKey(key.EncodeKey), blockHash)
	if err != nil {
		return 0, err
	}

	var setId types.U64
	err = types.DecodeFromBytes(*storageRaw, &setId)

	return setId, err
}

func (c *SubstrateAPI) GetFinalitiyProof(blockNumber types.BlockNumber) (*FinalityProof, error) {
	var finalityProofHexstring string
	err := c.Call(&finalityProofHexstring, "grandpa_proveFinality", types.NewU32(uint32(blockNumber)))
	if err != nil {
		return nil, err
	}

	fp := &FinalityProof{}
	err = types.DecodeFromHexString(finalityProofHexstring, fp)
	if err != nil {
		return nil, err
	}

	return fp, err
}

func (c *SubstrateAPI) GetJustificationsAndUnknownHeaders(blockNumber types.BlockNumber) (*GrandpaJustification, []SubstrateHeader, error) {
	var finalityProofHexstring string
	err := c.Call(&finalityProofHexstring, "grandpa_proveFinality", types.NewU32(uint32(blockNumber)))
	if err != nil {
		return nil, nil, err
	}

	spec := c.GetSpecName()

	if spec == "kusama" || spec == "polkadot" {
		fp := &FinalityProof{}
		err = types.DecodeFromHexString(finalityProofHexstring, fp)

		if fp != nil {
			return &fp.Justification.EncodedJustification, fp.UnknownHeaders, err
		}
	}

	if spec == "westend" {
		fp := &WestendFinalityProof{}
		err = types.DecodeFromHexString(finalityProofHexstring, fp)

		if fp != nil {
			return &fp.Justification.EncodedJustification, fp.UnknownHeaders, err
		}
	}

	return nil, nil, fmt.Errorf("not supported chain spec %s", spec)
}

func (c *SubstrateAPI) GetValidationData(blockHash SubstrateHash) (*PersistedValidationData, error) {
	key := EncodeStorageKey(c.scaleDecoderOption.Metadata, "ParachainSystem", "ValidationData")

	storageRaw, err := c.GetStorageRaw(NewStorageKey(key.EncodeKey), blockHash)
	if err != nil {
		return nil, err
	}

	pvd := &PersistedValidationData{}
	err = types.DecodeFromBytes(*storageRaw, pvd)

	return pvd, err
}

func (c *SubstrateAPI) GetParachainId() (*SubstrateParachainId, error) {
	key := EncodeStorageKey(c.scaleDecoderOption.Metadata, "ParachainInfo", "ParachainId")

	blockHash, err := c.GetBlockHashLatest()
	if err != nil {
		return nil, err
	}

	storageRaw, err := c.GetStorageRaw(NewStorageKey(key.EncodeKey), blockHash)
	if err != nil {
		return nil, err
	}

	var pid SubstrateParachainId
	err = types.DecodeFromBytes(*storageRaw, &pid)

	return &pid, err
}

func (c *SubstrateAPI) GetSystemEvents(blockHash SubstrateHash, section string, method string) ([]map[string]interface{}, error) {
	key := EncodeStorageKey(c.scaleDecoderOption.Metadata, "System", "Events")
	systemEventsStorageRaw, err := c.GetStorageRaw(NewStorageKey(key.EncodeKey), blockHash)
	log.Warnf("GetMetadataRawLatest: fail %v", err)

	c.eventDecoder.Init(scaletypes.ScaleBytes{Data: *systemEventsStorageRaw}, &c.scaleDecoderOption)
	c.eventDecoder.Process()
	eventsVal := reflect.ValueOf(c.eventDecoder.Value)

	returnEvents := make([]map[string]interface{}, 0)
	for i := 0; i < eventsVal.Len(); i++ {
		mapVal := reflect.ValueOf(eventsVal.Index(i).Interface())
		eventId := mapVal.MapIndex(reflect.ValueOf("event_id")).Interface().(string)
		moduleId := mapVal.MapIndex(reflect.ValueOf("module_id")).Interface().(string)

		if eventId == method && moduleId == section {
			eventMap, ok := mapVal.Interface().(map[string]interface{})
			if !ok {
				c.log.Panicf("GetSystemEvents: event can't decodable")
			}
			returnEvents = append(returnEvents, eventMap)
		}
	}

	return returnEvents, nil
}

func (c *SubstrateAPI) SubcribeFinalizedHeadAt(height uint64, cb func(*SubstrateHash)) error {
	current := height

	for {
		select {
		case <-c.keepSubscribeFinalizeHead:
			return nil
		default:
			finalizedHeadHash, err := c.GetFinalizedHead()
			if err != nil {
				return err
			}

			finalizedHead, err := c.GetHeader(finalizedHeadHash)
			if err != nil {
				return err
			}

			if current > uint64(finalizedHead.Number) {
				c.log.Tracef("block not yet finalized target:%v latest:%v", current, finalizedHead.Number)
				<-time.After(c.blockInterval * (time.Duration(current) - time.Duration(finalizedHead.Number)))
				continue
			}

			if current == uint64(finalizedHead.Number) {
				cb(&finalizedHeadHash)
			}

			if current < uint64(finalizedHead.Number) {
				currentHash, err := c.GetBlockHash(current)
				if err != nil {
					return err
				}

				cb(&currentHash)
			}

			current++
		}
	}
}
