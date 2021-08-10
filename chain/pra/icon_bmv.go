package pra

import (
	"path"
	"time"

	"github.com/icon-project/btp/chain/icon"
	"github.com/icon-project/btp/common/db"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mta"
)

const (
	MaxDefaultRetries = 10
	DefaultRetryDelay = time.Second
)

type relayStoreConfig struct {
	relayChainUniqueName string
	absPath              string
	backendType          db.BackendType
}

type praBmvClient struct {
	*icon.Client
	address     string
	store       *mta.ExtAccumulator
	storeConfig relayStoreConfig
	log         log.Logger
}

func NewRelayStoreConfig(absPath, relayChainUniqueName string) relayStoreConfig {
	return relayStoreConfig{
		absPath:              absPath,
		relayChainUniqueName: relayChainUniqueName,
		backendType:          db.GoLevelDBBackend,
	}
}

func NewPraBmvClient(url string, log log.Logger, address string, config relayStoreConfig) praBmvClient {
	pC := praBmvClient{
		Client:      icon.NewClient(url, log),
		address:     address,
		storeConfig: config,
		log:         log,
	}

	return pC
}

func (c *praBmvClient) getRelayMtaHeight() uint64 {
	p := &icon.CallParam{
		ToAddress: icon.Address(c.address),
		DataType:  "call",
		Data: icon.CallData{
			Method: "relayMtaHeight",
		},
	}

	tries := 0
	for {
		tries++
		var height icon.HexInt
		err := icon.MapError(c.Call(p, &height))
		if err != nil && tries < MaxDefaultRetries {
			c.log.Debugf("getRelayMtaHeight: retry %d", tries)
			<-time.After(DefaultRetryDelay)
			continue
		}

		value, err := height.Value()
		if err != nil {
			c.log.Debugf("getRelayMtaHeight: failed")
		}

		return uint64(value)
	}
}

func (c *praBmvClient) getRelayMtaOffset() uint64 {
	p := &icon.CallParam{
		ToAddress: icon.Address(c.address),
		DataType:  "call",
		Data: icon.CallData{
			Method: "relayMtaOffset",
		},
	}

	tries := 0
	for {
		tries++
		var height icon.HexInt
		err := icon.MapError(c.Call(p, &height))
		if err != nil && tries < MaxDefaultRetries {
			c.log.Debugf("getRelayMtaOffset: failed retry %d", tries)
			<-time.After(DefaultRetryDelay)
			continue
		}

		value, err := height.Value()
		if err != nil {
			c.log.Debugf("getRelayMtaOffset: failed")
		}

		return uint64(value)
	}
}

func (c *praBmvClient) getParaMtaHeight() uint64 {
	p := &icon.CallParam{
		ToAddress: icon.Address(c.address),
		DataType:  "call",
		Data: icon.CallData{
			Method: "paraMtaHeight",
		},
	}

	tries := 0
	for {
		tries++
		var height icon.HexInt
		err := icon.MapError(c.Call(p, &height))
		if err != nil && tries < MaxDefaultRetries {
			c.log.Debugf("getParaMtaHeight: failed retry %d", tries)
			<-time.After(DefaultRetryDelay)
			continue
		}

		value, err := height.Value()
		if err != nil {
			c.log.Debugf("getParaMtaHeight: failed")
		}

		return uint64(value)
	}
}

func (c *praBmvClient) getSetId() uint64 {
	p := &icon.CallParam{
		ToAddress: icon.Address(c.address),
		DataType:  "call",
		Data: icon.CallData{
			Method: "setId",
		},
	}

	tries := 0
	for {
		tries++
		var setId icon.HexInt
		err := icon.MapError(c.Call(p, &setId))
		if err != nil && tries < MaxDefaultRetries {
			c.log.Debugf("getSetId: failed retry %d", tries)
			<-time.After(DefaultRetryDelay)
			continue
		}

		value, err := setId.Value()
		if err != nil {
			c.log.Debugf("getSetId: failed")
		}

		return uint64(value)
	}
}

func (c *praBmvClient) prepareDatabase(offset int64) error {
	c.log.Debugln("open database", path.Join(c.storeConfig.absPath, c.storeConfig.relayChainUniqueName))
	database, err := db.Open(c.storeConfig.absPath, string(c.storeConfig.backendType), c.storeConfig.relayChainUniqueName)
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

	// A lot of int64 variable should be uint64
	if offset < 0 {
		offset = 0
	}

	c.store = mta.NewExtAccumulator(k, bk, offset)
	if bk.Has(k) {
		if err = c.store.Recover(); err != nil {
			return errors.Wrapf(err, "fail to acc.Recover cause:%v", err)
		}
		c.log.Debugf("recover Accumulator offset:%d, height:%d", c.store.Offset(), c.store.Height())
		// b.log.Fatal("Stop")
		//TODO sync offset
	}
	return nil
}
