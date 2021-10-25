package substrate

import (
	"fmt"
	"io/ioutil"
	"path"
	"reflect"
	"runtime"
	"sort"
	"time"

	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/gammazero/workerpool"
	"github.com/icon-project/btp/common/go-ethereum/rpc"
	"github.com/icon-project/btp/common/log"
	module "github.com/icon-project/btp/common/utils"
	scale "github.com/itering/scale.go"
	"github.com/itering/scale.go/source"
	scaletypes "github.com/itering/scale.go/types"
	"github.com/itering/scale.go/utiles"
)

const (
	Westend  = "westend"
	Kusama   = "kusama"
	Polkadot = "polkadot"
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
		log:                       log.New(),
	}, err
}

func (c *SubstrateAPI) Init() {
	metadataRaw := c.GetMetadataRawLatest()
	m := scale.MetadataDecoder{}
	m.Init(utiles.HexToBytes(metadataRaw))
	if err := m.Process(); err != nil {
		log.Errorf("Init: metadaDecoderProcess fail %v", err)
	}

	c.eventDecoder = scale.EventsDecoder{}
	c.scaleDecoderOption = scaletypes.ScaleDecoderOption{Metadata: &m.Metadata}

	runtimeVersion, err := c.GetRuntimeVersionLatest()
	if err != nil {
		c.log.Panicf("Init: can't fetch RuntimeVersionLatest")
	}

	modulePath, err := module.GetModulePath("github.com/itering/scale.go", "v1.1.23")
	if err != nil {
		c.log.Panicf("Init: can't fetch scale module")
	}

	typesDefinition, err := ioutil.ReadFile(path.Join(modulePath, "network/"+runtimeVersion.SpecName+".json"))
	if err != nil {
		c.log.Panicf("Init: can't fetch scale type %s", runtimeVersion.SpecName)
	}

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

func (c *SubstrateAPI) GetMetadata(blockHash SubstrateHash) (*SubstrateMetaData, error) {
	return c.getMetadata(&blockHash)
}

func (c *SubstrateAPI) GetMetadataLatest() *SubstrateMetaData {
	metadata, _ := c.getMetadata(nil)
	return metadata
}

func (c *SubstrateAPI) getMetadata(blockHash *SubstrateHash) (*SubstrateMetaData, error) {
	res, err := c.getMetadataRaw(blockHash)
	if err != nil {
		return nil, err
	}

	var metadata SubstrateMetaData
	err = types.DecodeFromHexString(res, &metadata)
	return &metadata, err
}

func (c *SubstrateAPI) GetMetadataRawLatest() string {
	res, _ := c.getMetadataRaw(nil)
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
	rtv, _ := c.GetRuntimeVersionLatest()
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
	rspChan := make(chan *SubstrateHeader, len(blockNumbers))

	for _, blockNumber := range blockNumbers {
		blockNumber := blockNumber
		wp.Submit(func() {
			blockHash, _ := c.GetBlockHash(uint64(blockNumber))
			header, _ := c.GetHeader(blockHash)
			rspChan <- header
		})
	}

	wp.StopWait()
	close(rspChan)
	for header := range rspChan {
		headers = append(headers, *header)
	}

	sort.Slice(headers, func(i, j int) bool {
		return headers[i].Number < headers[j].Number
	})

	return headers, nil
}

func (c *SubstrateAPI) GetReadProof(key SubstrateStorageKey, hash SubstrateHash) (SubstrateReadProof, error) {
	var res SubstrateReadProof
	err := c.Call(&res, "state_getReadProof", []string{key.Hex()}, hash.Hex())
	return res, err
}

func (c *SubstrateAPI) CreateStorageKey(meta *SubstrateMetaData, prefix, method string, arg []byte, arg2 []byte) (SubstrateStorageKey, error) {
	key, err := types.CreateStorageKey(meta, prefix, method, arg, arg2)
	if err != nil {
		return nil, err
	}

	return SubstrateStorageKey(key), nil
}

func (c *SubstrateAPI) GetSystemEventStorageKey(blockhash SubstrateHash) (SubstrateStorageKey, error) {
	meta := c.GetMetadataLatest()
	key, err := types.CreateStorageKey(meta, "System", "Events", nil, nil)
	if err != nil {
		return nil, err
	}
	return SubstrateStorageKey(key), nil
}

func (c *SubstrateAPI) GetGrandpaCurrentSetId(blockHash SubstrateHash) (types.U64, error) {
	meta := c.GetMetadataLatest()
	key, err := types.CreateStorageKey(meta, "Grandpa", "CurrentSetId", nil, nil)
	if err != nil {
		return 0, err
	}

	storageRaw, err := c.GetStorageRaw(key, blockHash)
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

	if spec == Kusama || spec == Polkadot {
		fp := &FinalityProof{}
		err = types.DecodeFromHexString(finalityProofHexstring, fp)

		if fp != nil {
			return &fp.Justification.EncodedJustification, fp.UnknownHeaders, err
		}
	}

	if spec == Westend {
		fp := &WestendFinalityProof{}
		err = types.DecodeFromHexString(finalityProofHexstring, fp)

		if fp != nil {
			return &fp.Justification.EncodedJustification, fp.UnknownHeaders, err
		}
	}

	return nil, nil, fmt.Errorf("not supported chain spec %s", spec)
}

func (c *SubstrateAPI) GetValidationData(blockHash SubstrateHash) (*PersistedValidationData, error) {
	meta := c.GetMetadataLatest()
	key, err := types.CreateStorageKey(meta, "ParachainSystem", "ValidationData", nil, nil)
	if err != nil {
		return nil, err
	}

	storageRaw, err := c.GetStorageRaw(key, blockHash)
	if err != nil {
		return nil, err
	}

	pvd := &PersistedValidationData{}
	err = types.DecodeFromBytes(*storageRaw, pvd)

	return pvd, err
}

func (c *SubstrateAPI) GetParachainId() (*SubstrateParachainId, error) {
	meta := c.GetMetadataLatest()
	key, err := types.CreateStorageKey(meta, "ParachainInfo", "ParachainId", nil, nil)
	if err != nil {
		return nil, err
	}

	blockHash, err := c.GetBlockHashLatest()
	if err != nil {
		return nil, err
	}

	storageRaw, err := c.GetStorageRaw(key, blockHash)
	if err != nil {
		return nil, err
	}

	var pid SubstrateParachainId
	err = types.DecodeFromBytes(*storageRaw, &pid)

	return &pid, err
}

func (c *SubstrateAPI) GetSystemEvents(blockHash SubstrateHash, section string, method string) ([]map[string]interface{}, error) {
	key := EncodeStorageKey(c.scaleDecoderOption.Metadata, "System", "Events")
	systemEventsStorageRaw, _ := c.GetStorageRaw(NewStorageKey(key.EncodeKey), blockHash)

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
