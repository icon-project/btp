package client

import (
	"context"
	"fmt"
	"io"
	nhttp "net/http"

	eth2client "github.com/attestantio/go-eth2-client"
	api "github.com/attestantio/go-eth2-client/api/v1"
	"github.com/attestantio/go-eth2-client/http"
	"github.com/attestantio/go-eth2-client/spec"
	"github.com/attestantio/go-eth2-client/spec/altair"
	"github.com/attestantio/go-eth2-client/spec/phase0"
	"github.com/rs/zerolog"

	"github.com/icon-project/btp/common/log"
)

type ConsensusLayer struct {
	ctx     context.Context
	cancel  context.CancelFunc
	service eth2client.Service
	log     log.Logger
}

func (c *ConsensusLayer) Genesis() (*api.Genesis, error) {
	return c.service.(*http.Service).Genesis(c.ctx)
}

func (c *ConsensusLayer) BeaconBlockHeader(blockID string) (*api.BeaconBlockHeader, error) {
	return c.service.(*http.Service).BeaconBlockHeader(c.ctx, blockID)
}

func (c *ConsensusLayer) BeaconBlock(blockID string) (*spec.VersionedSignedBeaconBlock, error) {
	return c.service.(*http.Service).SignedBeaconBlock(c.ctx, blockID)
}

func (c *ConsensusLayer) BeaconBlockRoot(blockID string) (*phase0.Root, error) {
	return c.service.(*http.Service).BeaconBlockRoot(c.ctx, blockID)
}

func (c *ConsensusLayer) Events(topics []string, handler eth2client.EventHandlerFunc) error {
	return c.service.(eth2client.EventsProvider).Events(c.ctx, topics, handler)
}

func (c *ConsensusLayer) LightClientBootstrap(blockRoot phase0.Root) (*altair.LightClientBootstrap, error) {
	return c.service.(*http.Service).LightClientBootstrap(c.ctx, blockRoot)
}

func (c *ConsensusLayer) LightClientUpdates(startPeriod, count uint64) ([]*altair.LightClientUpdate, error) {
	return c.service.(*http.Service).LightClientUpdates(c.ctx, startPeriod, count)
}

func (c *ConsensusLayer) LightClientOptimisticUpdate() (*altair.LightClientOptimisticUpdate, error) {
	return c.service.(*http.Service).LightClientOptimisticUpdate(c.ctx)
}

func (c *ConsensusLayer) LightClientFinalityUpdate() (*altair.LightClientFinalityUpdate, error) {
	return c.service.(*http.Service).LightClientFinalityUpdate(c.ctx)
}

func (c *ConsensusLayer) GetStateProofWithPath(stateId, path string) ([]byte, error) {
	// TODO this api is not public
	url := fmt.Sprintf("https://lodestar-mainnet.chainsafe.io/eth/v0/beacon/proof/state/%s?paths=[%s]",
		stateId, path)
	resp, err := nhttp.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	return io.ReadAll(resp.Body)
}

func (c *ConsensusLayer) Term() {
	c.cancel()
}

func NewConsensusLayer(uri string, log log.Logger) (*ConsensusLayer, error) {
	ctx, cancel := context.WithCancel(context.Background())
	service, err := http.New(
		ctx,
		http.WithAddress(uri),
		http.WithLogLevel(zerolog.WarnLevel),
	)
	if err != nil {
		cancel()
		return nil, err
	}
	return &ConsensusLayer{
		ctx:     ctx,
		cancel:  cancel,
		service: service,
		log:     log,
	}, nil
}
