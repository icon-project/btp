package icon

import (
	"time"

	"github.com/icon-project/btp/common/log"
)

const (
	maxDefaultRetries = 10
	defaultRetryDelay = time.Second
)

type PraBmvClient struct {
	*Client
	address string
	log     log.Logger
}

type PraBmvStatus struct {
	RelayMtaHeight int64
	RelayMtaOffset int64
	RelaySetId     int64
	ParaMtaHeight  int64
}

func NewPraBmvClient(url string, address string, log log.Logger) PraBmvClient {
	pC := PraBmvClient{
		Client:  NewClient(url, log),
		address: address,
		log:     log,
	}

	return pC
}

func (c *PraBmvClient) GetRelayMtaHeight() uint64 {
	p := &CallParam{
		ToAddress: Address(c.address),
		DataType:  "call",
		Data: CallData{
			Method: "relayMtaHeight",
		},
	}

	tries := 0
	for {
		tries++
		var height HexInt
		err := mapError(c.Call(p, &height))
		if err != nil && tries < maxDefaultRetries {
			c.log.Debugf("getRelayMtaHeight: retry %d", tries)
			<-time.After(defaultRetryDelay)
			continue
		}

		value, err := height.Value()
		if err != nil {
			c.log.Debugf("getRelayMtaHeight: failed")
		}

		return uint64(value)
	}
}

func (c *PraBmvClient) GetRelayMtaOffset() uint64 {
	p := &CallParam{
		ToAddress: Address(c.address),
		DataType:  "call",
		Data: CallData{
			Method: "relayMtaOffset",
		},
	}

	tries := 0
	for {
		tries++
		var height HexInt
		err := mapError(c.Call(p, &height))
		if err != nil && tries < maxDefaultRetries {
			c.log.Debugf("getRelayMtaOffset: failed retry %d", tries)
			<-time.After(defaultRetryDelay)
			continue
		}

		value, err := height.Value()
		if err != nil {
			c.log.Debugf("getRelayMtaOffset: failed")
		}

		return uint64(value)
	}
}

func (c *PraBmvClient) GetParaMtaHeight() uint64 {
	p := &CallParam{
		ToAddress: Address(c.address),
		DataType:  "call",
		Data: CallData{
			Method: "paraMtaHeight",
		},
	}

	tries := 0
	for {
		tries++
		var height HexInt
		err := mapError(c.Call(p, &height))
		if err != nil && tries < maxDefaultRetries {
			c.log.Debugf("getParaMtaHeight: failed retry %d", tries)
			<-time.After(defaultRetryDelay)
			continue
		}

		value, err := height.Value()
		if err != nil {
			c.log.Debugf("getParaMtaHeight: failed")
		}

		return uint64(value)
	}
}

func (c *PraBmvClient) GetSetId() uint64 {
	p := &CallParam{
		ToAddress: Address(c.address),
		DataType:  "call",
		Data: CallData{
			Method: "setId",
		},
	}

	tries := 0
	for {
		tries++
		var setId HexInt
		err := mapError(c.Call(p, &setId))
		if err != nil && tries < maxDefaultRetries {
			c.log.Debugf("getSetId: failed retry %d", tries)
			<-time.After(defaultRetryDelay)
			continue
		}

		value, err := setId.Value()
		if err != nil {
			c.log.Debugf("getSetId: failed")
		}

		return uint64(value)
	}
}

func (c *PraBmvClient) GetPraBmvStatus() PraBmvStatus {
	relayMtaHeightChan := make(chan int64)
	relayMtaOffsetChan := make(chan int64)
	relaySetIdChan := make(chan int64)
	paraMtaHeightChan := make(chan int64)

	for i := 0; i < 4; i++ {
		index := i
		go func() {
			switch index {
			case 0:
				defer close(relayMtaHeightChan)
				relayMtaHeightChan <- int64(c.GetRelayMtaHeight())
			case 1:
				defer close(relayMtaOffsetChan)
				relayMtaOffsetChan <- int64(c.GetRelayMtaOffset())
			case 2:
				defer close(relaySetIdChan)
				relaySetIdChan <- int64(c.GetSetId())
			case 3:
				defer close(paraMtaHeightChan)
				paraMtaHeightChan <- int64(c.GetParaMtaHeight())
			}
		}()
	}

	return PraBmvStatus{
		RelayMtaHeight: <-relayMtaHeightChan,
		RelayMtaOffset: <-relayMtaOffsetChan,
		RelaySetId:     <-relaySetIdChan,
		ParaMtaHeight:  <-paraMtaHeightChan,
	}
}
