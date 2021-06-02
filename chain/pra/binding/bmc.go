// Code generated - DO NOT EDIT.
// This file is a generated binding and any manual changes will be lost.

package binding

import (
	"math/big"
	"strings"

	ethereum "github.com/ethereum/go-ethereum"
	"github.com/ethereum/go-ethereum/accounts/abi"
	"github.com/ethereum/go-ethereum/accounts/abi/bind"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/event"
)

// Reference imports to suppress errors if they are not otherwise used.
var (
	_ = big.NewInt
	_ = strings.NewReader
	_ = ethereum.NotFound
	_ = bind.Bind
	_ = common.Big1
	_ = types.BloomLookup
	_ = event.NewSubscription
)

// TypesLinkStats is an auto generated low-level Go binding around an user-defined struct.
type TypesLinkStats struct {
	RxSeq            *big.Int
	TxSeq            *big.Int
	Verifier         TypesVerifierStats
	Relays           []TypesRelayStats
	RelayIdx         *big.Int
	RotateHeight     *big.Int
	RotateTerm       *big.Int
	DelayLimit       *big.Int
	MaxAggregation   *big.Int
	RxHeightSrc      *big.Int
	RxHeight         *big.Int
	BlockIntervalSrc *big.Int
	BlockIntervalDst *big.Int
	CurrentHeight    *big.Int
}

// TypesRelayStats is an auto generated low-level Go binding around an user-defined struct.
type TypesRelayStats struct {
	Addr       common.Address
	BlockCount *big.Int
	MsgCount   *big.Int
}

// TypesRequest is an auto generated low-level Go binding around an user-defined struct.
type TypesRequest struct {
	ServiceName string
	Bsh         common.Address
}

// TypesRoute is an auto generated low-level Go binding around an user-defined struct.
type TypesRoute struct {
	Dst  string
	Next string
}

// TypesService is an auto generated low-level Go binding around an user-defined struct.
type TypesService struct {
	Svc  string
	Addr common.Address
}

// TypesVerifier is an auto generated low-level Go binding around an user-defined struct.
type TypesVerifier struct {
	Net  string
	Addr common.Address
}

// TypesVerifierStats is an auto generated low-level Go binding around an user-defined struct.
type TypesVerifierStats struct {
	HeightMTA  *big.Int
	OffsetMTA  *big.Int
	LastHeight *big.Int
	Extra      []byte
}

// BMCABI is the input ABI used to generate the binding from.
const BMCABI = "[{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_network\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"_addr\",\"type\":\"address\"}],\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_svc\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"int256\",\"name\":\"_sn\",\"type\":\"int256\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"_code\",\"type\":\"uint256\"},{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_errMsg\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"_svcErrCode\",\"type\":\"uint256\"},{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_svcErrMsg\",\"type\":\"string\"}],\"name\":\"ErrorOnBTPError\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_next\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"_seq\",\"type\":\"uint256\"},{\"indexed\":false,\"internalType\":\"bytes\",\"name\":\"_msg\",\"type\":\"bytes\"}],\"name\":\"Event\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_next\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"_seq\",\"type\":\"uint256\"},{\"indexed\":false,\"internalType\":\"bytes\",\"name\":\"_msg\",\"type\":\"bytes\"}],\"name\":\"Message\",\"type\":\"event\"},{\"inputs\":[],\"name\":\"bmcAddress\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"address\",\"name\":\"_owner\",\"type\":\"address\"}],\"name\":\"addOwner\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"address\",\"name\":\"_owner\",\"type\":\"address\"}],\"name\":\"removeOwner\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"address\",\"name\":\"_owner\",\"type\":\"address\"}],\"name\":\"isOwner\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_serviceName\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"_addr\",\"type\":\"address\"}],\"name\":\"requestAddService\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"getPendingRequest\",\"outputs\":[{\"components\":[{\"internalType\":\"string\",\"name\":\"serviceName\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"bsh\",\"type\":\"address\"}],\"internalType\":\"structTypes.Request[]\",\"name\":\"\",\"type\":\"tuple[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_prev\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"_msg\",\"type\":\"string\"}],\"name\":\"handleRelayMessage\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_dst\",\"type\":\"string\"}],\"name\":\"resolveRoute\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_to\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"_svc\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"_sn\",\"type\":\"uint256\"},{\"internalType\":\"bytes\",\"name\":\"_msg\",\"type\":\"bytes\"}],\"name\":\"sendMessage\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_svc\",\"type\":\"string\"}],\"name\":\"approveService\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_svc\",\"type\":\"string\"}],\"name\":\"removeService\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"getServices\",\"outputs\":[{\"components\":[{\"internalType\":\"string\",\"name\":\"svc\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"addr\",\"type\":\"address\"}],\"internalType\":\"structTypes.Service[]\",\"name\":\"\",\"type\":\"tuple[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_net\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"_addr\",\"type\":\"address\"}],\"name\":\"addVerifier\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_net\",\"type\":\"string\"}],\"name\":\"removeVerifier\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"getVerifiers\",\"outputs\":[{\"components\":[{\"internalType\":\"string\",\"name\":\"net\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"addr\",\"type\":\"address\"}],\"internalType\":\"structTypes.Verifier[]\",\"name\":\"\",\"type\":\"tuple[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"}],\"name\":\"addLink\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"}],\"name\":\"removeLink\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"getLinks\",\"outputs\":[{\"internalType\":\"string[]\",\"name\":\"\",\"type\":\"string[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"_blockInterval\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"_maxAggregation\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"_delayLimit\",\"type\":\"uint256\"}],\"name\":\"setLink\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_dst\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"}],\"name\":\"addRoute\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_dst\",\"type\":\"string\"}],\"name\":\"removeRoute\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"getRoutes\",\"outputs\":[{\"components\":[{\"internalType\":\"string\",\"name\":\"dst\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"next\",\"type\":\"string\"}],\"internalType\":\"structTypes.Route[]\",\"name\":\"\",\"type\":\"tuple[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"},{\"internalType\":\"address[]\",\"name\":\"_addr\",\"type\":\"address[]\"}],\"name\":\"addRelay\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"_addr\",\"type\":\"address\"}],\"name\":\"removeRelay\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"}],\"name\":\"getRelays\",\"outputs\":[{\"internalType\":\"address[]\",\"name\":\"\",\"type\":\"address[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"}],\"name\":\"getStatus\",\"outputs\":[{\"components\":[{\"internalType\":\"uint256\",\"name\":\"rxSeq\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"txSeq\",\"type\":\"uint256\"},{\"components\":[{\"internalType\":\"uint256\",\"name\":\"heightMTA\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"offsetMTA\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"lastHeight\",\"type\":\"uint256\"},{\"internalType\":\"bytes\",\"name\":\"extra\",\"type\":\"bytes\"}],\"internalType\":\"structTypes.VerifierStats\",\"name\":\"verifier\",\"type\":\"tuple\"},{\"components\":[{\"internalType\":\"address\",\"name\":\"addr\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"blockCount\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"msgCount\",\"type\":\"uint256\"}],\"internalType\":\"structTypes.RelayStats[]\",\"name\":\"relays\",\"type\":\"tuple[]\"},{\"internalType\":\"uint256\",\"name\":\"relayIdx\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"rotateHeight\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"rotateTerm\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"delayLimit\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"maxAggregation\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"rxHeightSrc\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"rxHeight\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"blockIntervalSrc\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"blockIntervalDst\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"currentHeight\",\"type\":\"uint256\"}],\"internalType\":\"structTypes.LinkStats\",\"name\":\"_linkStats\",\"type\":\"tuple\"}],\"stateMutability\":\"view\",\"type\":\"function\"}]"

// BMC is an auto generated Go binding around an Ethereum contract.
type BMC struct {
	BMCCaller     // Read-only binding to the contract
	BMCTransactor // Write-only binding to the contract
	BMCFilterer   // Log filterer for contract events
}

// BMCCaller is an auto generated read-only Go binding around an Ethereum contract.
type BMCCaller struct {
	contract *bind.BoundContract // Generic contract wrapper for the low level calls
}

// BMCTransactor is an auto generated write-only Go binding around an Ethereum contract.
type BMCTransactor struct {
	contract *bind.BoundContract // Generic contract wrapper for the low level calls
}

// BMCFilterer is an auto generated log filtering Go binding around an Ethereum contract events.
type BMCFilterer struct {
	contract *bind.BoundContract // Generic contract wrapper for the low level calls
}

// BMCSession is an auto generated Go binding around an Ethereum contract,
// with pre-set call and transact options.
type BMCSession struct {
	Contract     *BMC              // Generic contract binding to set the session for
	CallOpts     bind.CallOpts     // Call options to use throughout this session
	TransactOpts bind.TransactOpts // Transaction auth options to use throughout this session
}

// BMCCallerSession is an auto generated read-only Go binding around an Ethereum contract,
// with pre-set call options.
type BMCCallerSession struct {
	Contract *BMCCaller    // Generic contract caller binding to set the session for
	CallOpts bind.CallOpts // Call options to use throughout this session
}

// BMCTransactorSession is an auto generated write-only Go binding around an Ethereum contract,
// with pre-set transact options.
type BMCTransactorSession struct {
	Contract     *BMCTransactor    // Generic contract transactor binding to set the session for
	TransactOpts bind.TransactOpts // Transaction auth options to use throughout this session
}

// BMCRaw is an auto generated low-level Go binding around an Ethereum contract.
type BMCRaw struct {
	Contract *BMC // Generic contract binding to access the raw methods on
}

// BMCCallerRaw is an auto generated low-level read-only Go binding around an Ethereum contract.
type BMCCallerRaw struct {
	Contract *BMCCaller // Generic read-only contract binding to access the raw methods on
}

// BMCTransactorRaw is an auto generated low-level write-only Go binding around an Ethereum contract.
type BMCTransactorRaw struct {
	Contract *BMCTransactor // Generic write-only contract binding to access the raw methods on
}

// NewBMC creates a new instance of BMC, bound to a specific deployed contract.
func NewBMC(address common.Address, backend bind.ContractBackend) (*BMC, error) {
	contract, err := bindBMC(address, backend, backend, backend)
	if err != nil {
		return nil, err
	}
	return &BMC{BMCCaller: BMCCaller{contract: contract}, BMCTransactor: BMCTransactor{contract: contract}, BMCFilterer: BMCFilterer{contract: contract}}, nil
}

// NewBMCCaller creates a new read-only instance of BMC, bound to a specific deployed contract.
func NewBMCCaller(address common.Address, caller bind.ContractCaller) (*BMCCaller, error) {
	contract, err := bindBMC(address, caller, nil, nil)
	if err != nil {
		return nil, err
	}
	return &BMCCaller{contract: contract}, nil
}

// NewBMCTransactor creates a new write-only instance of BMC, bound to a specific deployed contract.
func NewBMCTransactor(address common.Address, transactor bind.ContractTransactor) (*BMCTransactor, error) {
	contract, err := bindBMC(address, nil, transactor, nil)
	if err != nil {
		return nil, err
	}
	return &BMCTransactor{contract: contract}, nil
}

// NewBMCFilterer creates a new log filterer instance of BMC, bound to a specific deployed contract.
func NewBMCFilterer(address common.Address, filterer bind.ContractFilterer) (*BMCFilterer, error) {
	contract, err := bindBMC(address, nil, nil, filterer)
	if err != nil {
		return nil, err
	}
	return &BMCFilterer{contract: contract}, nil
}

// bindBMC binds a generic wrapper to an already deployed contract.
func bindBMC(address common.Address, caller bind.ContractCaller, transactor bind.ContractTransactor, filterer bind.ContractFilterer) (*bind.BoundContract, error) {
	parsed, err := abi.JSON(strings.NewReader(BMCABI))
	if err != nil {
		return nil, err
	}
	return bind.NewBoundContract(address, parsed, caller, transactor, filterer), nil
}

// Call invokes the (constant) contract method with params as input values and
// sets the output to result. The result type might be a single field for simple
// returns, a slice of interfaces for anonymous returns and a struct for named
// returns.
func (_BMC *BMCRaw) Call(opts *bind.CallOpts, result *[]interface{}, method string, params ...interface{}) error {
	return _BMC.Contract.BMCCaller.contract.Call(opts, result, method, params...)
}

// Transfer initiates a plain transaction to move funds to the contract, calling
// its default method if one is available.
func (_BMC *BMCRaw) Transfer(opts *bind.TransactOpts) (*types.Transaction, error) {
	return _BMC.Contract.BMCTransactor.contract.Transfer(opts)
}

// Transact invokes the (paid) contract method with params as input values.
func (_BMC *BMCRaw) Transact(opts *bind.TransactOpts, method string, params ...interface{}) (*types.Transaction, error) {
	return _BMC.Contract.BMCTransactor.contract.Transact(opts, method, params...)
}

// Call invokes the (constant) contract method with params as input values and
// sets the output to result. The result type might be a single field for simple
// returns, a slice of interfaces for anonymous returns and a struct for named
// returns.
func (_BMC *BMCCallerRaw) Call(opts *bind.CallOpts, result *[]interface{}, method string, params ...interface{}) error {
	return _BMC.Contract.contract.Call(opts, result, method, params...)
}

// Transfer initiates a plain transaction to move funds to the contract, calling
// its default method if one is available.
func (_BMC *BMCTransactorRaw) Transfer(opts *bind.TransactOpts) (*types.Transaction, error) {
	return _BMC.Contract.contract.Transfer(opts)
}

// Transact invokes the (paid) contract method with params as input values.
func (_BMC *BMCTransactorRaw) Transact(opts *bind.TransactOpts, method string, params ...interface{}) (*types.Transaction, error) {
	return _BMC.Contract.contract.Transact(opts, method, params...)
}

// BmcAddress is a free data retrieval call binding the contract method 0xc64ad5a0.
//
// Solidity: function bmcAddress() view returns(string)
func (_BMC *BMCCaller) BmcAddress(opts *bind.CallOpts) (string, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "bmcAddress")

	if err != nil {
		return *new(string), err
	}

	out0 := *abi.ConvertType(out[0], new(string)).(*string)

	return out0, err

}

// BmcAddress is a free data retrieval call binding the contract method 0xc64ad5a0.
//
// Solidity: function bmcAddress() view returns(string)
func (_BMC *BMCSession) BmcAddress() (string, error) {
	return _BMC.Contract.BmcAddress(&_BMC.CallOpts)
}

// BmcAddress is a free data retrieval call binding the contract method 0xc64ad5a0.
//
// Solidity: function bmcAddress() view returns(string)
func (_BMC *BMCCallerSession) BmcAddress() (string, error) {
	return _BMC.Contract.BmcAddress(&_BMC.CallOpts)
}

// GetLinks is a free data retrieval call binding the contract method 0xf66ddcbb.
//
// Solidity: function getLinks() view returns(string[])
func (_BMC *BMCCaller) GetLinks(opts *bind.CallOpts) ([]string, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "getLinks")

	if err != nil {
		return *new([]string), err
	}

	out0 := *abi.ConvertType(out[0], new([]string)).(*[]string)

	return out0, err

}

// GetLinks is a free data retrieval call binding the contract method 0xf66ddcbb.
//
// Solidity: function getLinks() view returns(string[])
func (_BMC *BMCSession) GetLinks() ([]string, error) {
	return _BMC.Contract.GetLinks(&_BMC.CallOpts)
}

// GetLinks is a free data retrieval call binding the contract method 0xf66ddcbb.
//
// Solidity: function getLinks() view returns(string[])
func (_BMC *BMCCallerSession) GetLinks() ([]string, error) {
	return _BMC.Contract.GetLinks(&_BMC.CallOpts)
}

// GetPendingRequest is a free data retrieval call binding the contract method 0x90f4fe72.
//
// Solidity: function getPendingRequest() view returns((string,address)[])
func (_BMC *BMCCaller) GetPendingRequest(opts *bind.CallOpts) ([]TypesRequest, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "getPendingRequest")

	if err != nil {
		return *new([]TypesRequest), err
	}

	out0 := *abi.ConvertType(out[0], new([]TypesRequest)).(*[]TypesRequest)

	return out0, err

}

// GetPendingRequest is a free data retrieval call binding the contract method 0x90f4fe72.
//
// Solidity: function getPendingRequest() view returns((string,address)[])
func (_BMC *BMCSession) GetPendingRequest() ([]TypesRequest, error) {
	return _BMC.Contract.GetPendingRequest(&_BMC.CallOpts)
}

// GetPendingRequest is a free data retrieval call binding the contract method 0x90f4fe72.
//
// Solidity: function getPendingRequest() view returns((string,address)[])
func (_BMC *BMCCallerSession) GetPendingRequest() ([]TypesRequest, error) {
	return _BMC.Contract.GetPendingRequest(&_BMC.CallOpts)
}

// GetRelays is a free data retrieval call binding the contract method 0x40926734.
//
// Solidity: function getRelays(string _link) view returns(address[])
func (_BMC *BMCCaller) GetRelays(opts *bind.CallOpts, _link string) ([]common.Address, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "getRelays", _link)

	if err != nil {
		return *new([]common.Address), err
	}

	out0 := *abi.ConvertType(out[0], new([]common.Address)).(*[]common.Address)

	return out0, err

}

// GetRelays is a free data retrieval call binding the contract method 0x40926734.
//
// Solidity: function getRelays(string _link) view returns(address[])
func (_BMC *BMCSession) GetRelays(_link string) ([]common.Address, error) {
	return _BMC.Contract.GetRelays(&_BMC.CallOpts, _link)
}

// GetRelays is a free data retrieval call binding the contract method 0x40926734.
//
// Solidity: function getRelays(string _link) view returns(address[])
func (_BMC *BMCCallerSession) GetRelays(_link string) ([]common.Address, error) {
	return _BMC.Contract.GetRelays(&_BMC.CallOpts, _link)
}

// GetRoutes is a free data retrieval call binding the contract method 0x7e928072.
//
// Solidity: function getRoutes() view returns((string,string)[])
func (_BMC *BMCCaller) GetRoutes(opts *bind.CallOpts) ([]TypesRoute, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "getRoutes")

	if err != nil {
		return *new([]TypesRoute), err
	}

	out0 := *abi.ConvertType(out[0], new([]TypesRoute)).(*[]TypesRoute)

	return out0, err

}

// GetRoutes is a free data retrieval call binding the contract method 0x7e928072.
//
// Solidity: function getRoutes() view returns((string,string)[])
func (_BMC *BMCSession) GetRoutes() ([]TypesRoute, error) {
	return _BMC.Contract.GetRoutes(&_BMC.CallOpts)
}

// GetRoutes is a free data retrieval call binding the contract method 0x7e928072.
//
// Solidity: function getRoutes() view returns((string,string)[])
func (_BMC *BMCCallerSession) GetRoutes() ([]TypesRoute, error) {
	return _BMC.Contract.GetRoutes(&_BMC.CallOpts)
}

// GetServices is a free data retrieval call binding the contract method 0x75417851.
//
// Solidity: function getServices() view returns((string,address)[])
func (_BMC *BMCCaller) GetServices(opts *bind.CallOpts) ([]TypesService, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "getServices")

	if err != nil {
		return *new([]TypesService), err
	}

	out0 := *abi.ConvertType(out[0], new([]TypesService)).(*[]TypesService)

	return out0, err

}

// GetServices is a free data retrieval call binding the contract method 0x75417851.
//
// Solidity: function getServices() view returns((string,address)[])
func (_BMC *BMCSession) GetServices() ([]TypesService, error) {
	return _BMC.Contract.GetServices(&_BMC.CallOpts)
}

// GetServices is a free data retrieval call binding the contract method 0x75417851.
//
// Solidity: function getServices() view returns((string,address)[])
func (_BMC *BMCCallerSession) GetServices() ([]TypesService, error) {
	return _BMC.Contract.GetServices(&_BMC.CallOpts)
}

// GetStatus is a free data retrieval call binding the contract method 0x22b05ed2.
//
// Solidity: function getStatus(string _link) view returns((uint256,uint256,(uint256,uint256,uint256,bytes),(address,uint256,uint256)[],uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256) _linkStats)
func (_BMC *BMCCaller) GetStatus(opts *bind.CallOpts, _link string) (TypesLinkStats, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "getStatus", _link)

	if err != nil {
		return *new(TypesLinkStats), err
	}

	out0 := *abi.ConvertType(out[0], new(TypesLinkStats)).(*TypesLinkStats)

	return out0, err

}

// GetStatus is a free data retrieval call binding the contract method 0x22b05ed2.
//
// Solidity: function getStatus(string _link) view returns((uint256,uint256,(uint256,uint256,uint256,bytes),(address,uint256,uint256)[],uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256) _linkStats)
func (_BMC *BMCSession) GetStatus(_link string) (TypesLinkStats, error) {
	return _BMC.Contract.GetStatus(&_BMC.CallOpts, _link)
}

// GetStatus is a free data retrieval call binding the contract method 0x22b05ed2.
//
// Solidity: function getStatus(string _link) view returns((uint256,uint256,(uint256,uint256,uint256,bytes),(address,uint256,uint256)[],uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256) _linkStats)
func (_BMC *BMCCallerSession) GetStatus(_link string) (TypesLinkStats, error) {
	return _BMC.Contract.GetStatus(&_BMC.CallOpts, _link)
}

// GetVerifiers is a free data retrieval call binding the contract method 0xa935e766.
//
// Solidity: function getVerifiers() view returns((string,address)[])
func (_BMC *BMCCaller) GetVerifiers(opts *bind.CallOpts) ([]TypesVerifier, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "getVerifiers")

	if err != nil {
		return *new([]TypesVerifier), err
	}

	out0 := *abi.ConvertType(out[0], new([]TypesVerifier)).(*[]TypesVerifier)

	return out0, err

}

// GetVerifiers is a free data retrieval call binding the contract method 0xa935e766.
//
// Solidity: function getVerifiers() view returns((string,address)[])
func (_BMC *BMCSession) GetVerifiers() ([]TypesVerifier, error) {
	return _BMC.Contract.GetVerifiers(&_BMC.CallOpts)
}

// GetVerifiers is a free data retrieval call binding the contract method 0xa935e766.
//
// Solidity: function getVerifiers() view returns((string,address)[])
func (_BMC *BMCCallerSession) GetVerifiers() ([]TypesVerifier, error) {
	return _BMC.Contract.GetVerifiers(&_BMC.CallOpts)
}

// IsOwner is a free data retrieval call binding the contract method 0x2f54bf6e.
//
// Solidity: function isOwner(address _owner) view returns(bool)
func (_BMC *BMCCaller) IsOwner(opts *bind.CallOpts, _owner common.Address) (bool, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "isOwner", _owner)

	if err != nil {
		return *new(bool), err
	}

	out0 := *abi.ConvertType(out[0], new(bool)).(*bool)

	return out0, err

}

// IsOwner is a free data retrieval call binding the contract method 0x2f54bf6e.
//
// Solidity: function isOwner(address _owner) view returns(bool)
func (_BMC *BMCSession) IsOwner(_owner common.Address) (bool, error) {
	return _BMC.Contract.IsOwner(&_BMC.CallOpts, _owner)
}

// IsOwner is a free data retrieval call binding the contract method 0x2f54bf6e.
//
// Solidity: function isOwner(address _owner) view returns(bool)
func (_BMC *BMCCallerSession) IsOwner(_owner common.Address) (bool, error) {
	return _BMC.Contract.IsOwner(&_BMC.CallOpts, _owner)
}

// ResolveRoute is a free data retrieval call binding the contract method 0xbe7f8676.
//
// Solidity: function resolveRoute(string _dst) view returns(string)
func (_BMC *BMCCaller) ResolveRoute(opts *bind.CallOpts, _dst string) (string, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "resolveRoute", _dst)

	if err != nil {
		return *new(string), err
	}

	out0 := *abi.ConvertType(out[0], new(string)).(*string)

	return out0, err

}

// ResolveRoute is a free data retrieval call binding the contract method 0xbe7f8676.
//
// Solidity: function resolveRoute(string _dst) view returns(string)
func (_BMC *BMCSession) ResolveRoute(_dst string) (string, error) {
	return _BMC.Contract.ResolveRoute(&_BMC.CallOpts, _dst)
}

// ResolveRoute is a free data retrieval call binding the contract method 0xbe7f8676.
//
// Solidity: function resolveRoute(string _dst) view returns(string)
func (_BMC *BMCCallerSession) ResolveRoute(_dst string) (string, error) {
	return _BMC.Contract.ResolveRoute(&_BMC.CallOpts, _dst)
}

// AddLink is a paid mutator transaction binding the contract method 0x22a618fa.
//
// Solidity: function addLink(string _link) returns()
func (_BMC *BMCTransactor) AddLink(opts *bind.TransactOpts, _link string) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "addLink", _link)
}

// AddLink is a paid mutator transaction binding the contract method 0x22a618fa.
//
// Solidity: function addLink(string _link) returns()
func (_BMC *BMCSession) AddLink(_link string) (*types.Transaction, error) {
	return _BMC.Contract.AddLink(&_BMC.TransactOpts, _link)
}

// AddLink is a paid mutator transaction binding the contract method 0x22a618fa.
//
// Solidity: function addLink(string _link) returns()
func (_BMC *BMCTransactorSession) AddLink(_link string) (*types.Transaction, error) {
	return _BMC.Contract.AddLink(&_BMC.TransactOpts, _link)
}

// AddOwner is a paid mutator transaction binding the contract method 0x7065cb48.
//
// Solidity: function addOwner(address _owner) returns()
func (_BMC *BMCTransactor) AddOwner(opts *bind.TransactOpts, _owner common.Address) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "addOwner", _owner)
}

// AddOwner is a paid mutator transaction binding the contract method 0x7065cb48.
//
// Solidity: function addOwner(address _owner) returns()
func (_BMC *BMCSession) AddOwner(_owner common.Address) (*types.Transaction, error) {
	return _BMC.Contract.AddOwner(&_BMC.TransactOpts, _owner)
}

// AddOwner is a paid mutator transaction binding the contract method 0x7065cb48.
//
// Solidity: function addOwner(address _owner) returns()
func (_BMC *BMCTransactorSession) AddOwner(_owner common.Address) (*types.Transaction, error) {
	return _BMC.Contract.AddOwner(&_BMC.TransactOpts, _owner)
}

// AddRelay is a paid mutator transaction binding the contract method 0x0748ea7a.
//
// Solidity: function addRelay(string _link, address[] _addr) returns()
func (_BMC *BMCTransactor) AddRelay(opts *bind.TransactOpts, _link string, _addr []common.Address) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "addRelay", _link, _addr)
}

// AddRelay is a paid mutator transaction binding the contract method 0x0748ea7a.
//
// Solidity: function addRelay(string _link, address[] _addr) returns()
func (_BMC *BMCSession) AddRelay(_link string, _addr []common.Address) (*types.Transaction, error) {
	return _BMC.Contract.AddRelay(&_BMC.TransactOpts, _link, _addr)
}

// AddRelay is a paid mutator transaction binding the contract method 0x0748ea7a.
//
// Solidity: function addRelay(string _link, address[] _addr) returns()
func (_BMC *BMCTransactorSession) AddRelay(_link string, _addr []common.Address) (*types.Transaction, error) {
	return _BMC.Contract.AddRelay(&_BMC.TransactOpts, _link, _addr)
}

// AddRoute is a paid mutator transaction binding the contract method 0x065a9e9b.
//
// Solidity: function addRoute(string _dst, string _link) returns()
func (_BMC *BMCTransactor) AddRoute(opts *bind.TransactOpts, _dst string, _link string) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "addRoute", _dst, _link)
}

// AddRoute is a paid mutator transaction binding the contract method 0x065a9e9b.
//
// Solidity: function addRoute(string _dst, string _link) returns()
func (_BMC *BMCSession) AddRoute(_dst string, _link string) (*types.Transaction, error) {
	return _BMC.Contract.AddRoute(&_BMC.TransactOpts, _dst, _link)
}

// AddRoute is a paid mutator transaction binding the contract method 0x065a9e9b.
//
// Solidity: function addRoute(string _dst, string _link) returns()
func (_BMC *BMCTransactorSession) AddRoute(_dst string, _link string) (*types.Transaction, error) {
	return _BMC.Contract.AddRoute(&_BMC.TransactOpts, _dst, _link)
}

// AddVerifier is a paid mutator transaction binding the contract method 0x76b503c3.
//
// Solidity: function addVerifier(string _net, address _addr) returns()
func (_BMC *BMCTransactor) AddVerifier(opts *bind.TransactOpts, _net string, _addr common.Address) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "addVerifier", _net, _addr)
}

// AddVerifier is a paid mutator transaction binding the contract method 0x76b503c3.
//
// Solidity: function addVerifier(string _net, address _addr) returns()
func (_BMC *BMCSession) AddVerifier(_net string, _addr common.Address) (*types.Transaction, error) {
	return _BMC.Contract.AddVerifier(&_BMC.TransactOpts, _net, _addr)
}

// AddVerifier is a paid mutator transaction binding the contract method 0x76b503c3.
//
// Solidity: function addVerifier(string _net, address _addr) returns()
func (_BMC *BMCTransactorSession) AddVerifier(_net string, _addr common.Address) (*types.Transaction, error) {
	return _BMC.Contract.AddVerifier(&_BMC.TransactOpts, _net, _addr)
}

// ApproveService is a paid mutator transaction binding the contract method 0xbe6c8dc9.
//
// Solidity: function approveService(string _svc) returns()
func (_BMC *BMCTransactor) ApproveService(opts *bind.TransactOpts, _svc string) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "approveService", _svc)
}

// ApproveService is a paid mutator transaction binding the contract method 0xbe6c8dc9.
//
// Solidity: function approveService(string _svc) returns()
func (_BMC *BMCSession) ApproveService(_svc string) (*types.Transaction, error) {
	return _BMC.Contract.ApproveService(&_BMC.TransactOpts, _svc)
}

// ApproveService is a paid mutator transaction binding the contract method 0xbe6c8dc9.
//
// Solidity: function approveService(string _svc) returns()
func (_BMC *BMCTransactorSession) ApproveService(_svc string) (*types.Transaction, error) {
	return _BMC.Contract.ApproveService(&_BMC.TransactOpts, _svc)
}

// HandleRelayMessage is a paid mutator transaction binding the contract method 0x6f4779cc.
//
// Solidity: function handleRelayMessage(string _prev, string _msg) returns()
func (_BMC *BMCTransactor) HandleRelayMessage(opts *bind.TransactOpts, _prev string, _msg string) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "handleRelayMessage", _prev, _msg)
}

// HandleRelayMessage is a paid mutator transaction binding the contract method 0x6f4779cc.
//
// Solidity: function handleRelayMessage(string _prev, string _msg) returns()
func (_BMC *BMCSession) HandleRelayMessage(_prev string, _msg string) (*types.Transaction, error) {
	return _BMC.Contract.HandleRelayMessage(&_BMC.TransactOpts, _prev, _msg)
}

// HandleRelayMessage is a paid mutator transaction binding the contract method 0x6f4779cc.
//
// Solidity: function handleRelayMessage(string _prev, string _msg) returns()
func (_BMC *BMCTransactorSession) HandleRelayMessage(_prev string, _msg string) (*types.Transaction, error) {
	return _BMC.Contract.HandleRelayMessage(&_BMC.TransactOpts, _prev, _msg)
}

// RemoveLink is a paid mutator transaction binding the contract method 0x6e4060d7.
//
// Solidity: function removeLink(string _link) returns()
func (_BMC *BMCTransactor) RemoveLink(opts *bind.TransactOpts, _link string) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "removeLink", _link)
}

// RemoveLink is a paid mutator transaction binding the contract method 0x6e4060d7.
//
// Solidity: function removeLink(string _link) returns()
func (_BMC *BMCSession) RemoveLink(_link string) (*types.Transaction, error) {
	return _BMC.Contract.RemoveLink(&_BMC.TransactOpts, _link)
}

// RemoveLink is a paid mutator transaction binding the contract method 0x6e4060d7.
//
// Solidity: function removeLink(string _link) returns()
func (_BMC *BMCTransactorSession) RemoveLink(_link string) (*types.Transaction, error) {
	return _BMC.Contract.RemoveLink(&_BMC.TransactOpts, _link)
}

// RemoveOwner is a paid mutator transaction binding the contract method 0x173825d9.
//
// Solidity: function removeOwner(address _owner) returns()
func (_BMC *BMCTransactor) RemoveOwner(opts *bind.TransactOpts, _owner common.Address) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "removeOwner", _owner)
}

// RemoveOwner is a paid mutator transaction binding the contract method 0x173825d9.
//
// Solidity: function removeOwner(address _owner) returns()
func (_BMC *BMCSession) RemoveOwner(_owner common.Address) (*types.Transaction, error) {
	return _BMC.Contract.RemoveOwner(&_BMC.TransactOpts, _owner)
}

// RemoveOwner is a paid mutator transaction binding the contract method 0x173825d9.
//
// Solidity: function removeOwner(address _owner) returns()
func (_BMC *BMCTransactorSession) RemoveOwner(_owner common.Address) (*types.Transaction, error) {
	return _BMC.Contract.RemoveOwner(&_BMC.TransactOpts, _owner)
}

// RemoveRelay is a paid mutator transaction binding the contract method 0xdef59f5e.
//
// Solidity: function removeRelay(string _link, address _addr) returns()
func (_BMC *BMCTransactor) RemoveRelay(opts *bind.TransactOpts, _link string, _addr common.Address) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "removeRelay", _link, _addr)
}

// RemoveRelay is a paid mutator transaction binding the contract method 0xdef59f5e.
//
// Solidity: function removeRelay(string _link, address _addr) returns()
func (_BMC *BMCSession) RemoveRelay(_link string, _addr common.Address) (*types.Transaction, error) {
	return _BMC.Contract.RemoveRelay(&_BMC.TransactOpts, _link, _addr)
}

// RemoveRelay is a paid mutator transaction binding the contract method 0xdef59f5e.
//
// Solidity: function removeRelay(string _link, address _addr) returns()
func (_BMC *BMCTransactorSession) RemoveRelay(_link string, _addr common.Address) (*types.Transaction, error) {
	return _BMC.Contract.RemoveRelay(&_BMC.TransactOpts, _link, _addr)
}

// RemoveRoute is a paid mutator transaction binding the contract method 0xbd0a0bb3.
//
// Solidity: function removeRoute(string _dst) returns()
func (_BMC *BMCTransactor) RemoveRoute(opts *bind.TransactOpts, _dst string) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "removeRoute", _dst)
}

// RemoveRoute is a paid mutator transaction binding the contract method 0xbd0a0bb3.
//
// Solidity: function removeRoute(string _dst) returns()
func (_BMC *BMCSession) RemoveRoute(_dst string) (*types.Transaction, error) {
	return _BMC.Contract.RemoveRoute(&_BMC.TransactOpts, _dst)
}

// RemoveRoute is a paid mutator transaction binding the contract method 0xbd0a0bb3.
//
// Solidity: function removeRoute(string _dst) returns()
func (_BMC *BMCTransactorSession) RemoveRoute(_dst string) (*types.Transaction, error) {
	return _BMC.Contract.RemoveRoute(&_BMC.TransactOpts, _dst)
}

// RemoveService is a paid mutator transaction binding the contract method 0xf51acaea.
//
// Solidity: function removeService(string _svc) returns()
func (_BMC *BMCTransactor) RemoveService(opts *bind.TransactOpts, _svc string) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "removeService", _svc)
}

// RemoveService is a paid mutator transaction binding the contract method 0xf51acaea.
//
// Solidity: function removeService(string _svc) returns()
func (_BMC *BMCSession) RemoveService(_svc string) (*types.Transaction, error) {
	return _BMC.Contract.RemoveService(&_BMC.TransactOpts, _svc)
}

// RemoveService is a paid mutator transaction binding the contract method 0xf51acaea.
//
// Solidity: function removeService(string _svc) returns()
func (_BMC *BMCTransactorSession) RemoveService(_svc string) (*types.Transaction, error) {
	return _BMC.Contract.RemoveService(&_BMC.TransactOpts, _svc)
}

// RemoveVerifier is a paid mutator transaction binding the contract method 0xd740945f.
//
// Solidity: function removeVerifier(string _net) returns()
func (_BMC *BMCTransactor) RemoveVerifier(opts *bind.TransactOpts, _net string) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "removeVerifier", _net)
}

// RemoveVerifier is a paid mutator transaction binding the contract method 0xd740945f.
//
// Solidity: function removeVerifier(string _net) returns()
func (_BMC *BMCSession) RemoveVerifier(_net string) (*types.Transaction, error) {
	return _BMC.Contract.RemoveVerifier(&_BMC.TransactOpts, _net)
}

// RemoveVerifier is a paid mutator transaction binding the contract method 0xd740945f.
//
// Solidity: function removeVerifier(string _net) returns()
func (_BMC *BMCTransactorSession) RemoveVerifier(_net string) (*types.Transaction, error) {
	return _BMC.Contract.RemoveVerifier(&_BMC.TransactOpts, _net)
}

// RequestAddService is a paid mutator transaction binding the contract method 0x7e09bd86.
//
// Solidity: function requestAddService(string _serviceName, address _addr) returns()
func (_BMC *BMCTransactor) RequestAddService(opts *bind.TransactOpts, _serviceName string, _addr common.Address) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "requestAddService", _serviceName, _addr)
}

// RequestAddService is a paid mutator transaction binding the contract method 0x7e09bd86.
//
// Solidity: function requestAddService(string _serviceName, address _addr) returns()
func (_BMC *BMCSession) RequestAddService(_serviceName string, _addr common.Address) (*types.Transaction, error) {
	return _BMC.Contract.RequestAddService(&_BMC.TransactOpts, _serviceName, _addr)
}

// RequestAddService is a paid mutator transaction binding the contract method 0x7e09bd86.
//
// Solidity: function requestAddService(string _serviceName, address _addr) returns()
func (_BMC *BMCTransactorSession) RequestAddService(_serviceName string, _addr common.Address) (*types.Transaction, error) {
	return _BMC.Contract.RequestAddService(&_BMC.TransactOpts, _serviceName, _addr)
}

// SendMessage is a paid mutator transaction binding the contract method 0xbf6c1d9a.
//
// Solidity: function sendMessage(string _to, string _svc, uint256 _sn, bytes _msg) returns()
func (_BMC *BMCTransactor) SendMessage(opts *bind.TransactOpts, _to string, _svc string, _sn *big.Int, _msg []byte) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "sendMessage", _to, _svc, _sn, _msg)
}

// SendMessage is a paid mutator transaction binding the contract method 0xbf6c1d9a.
//
// Solidity: function sendMessage(string _to, string _svc, uint256 _sn, bytes _msg) returns()
func (_BMC *BMCSession) SendMessage(_to string, _svc string, _sn *big.Int, _msg []byte) (*types.Transaction, error) {
	return _BMC.Contract.SendMessage(&_BMC.TransactOpts, _to, _svc, _sn, _msg)
}

// SendMessage is a paid mutator transaction binding the contract method 0xbf6c1d9a.
//
// Solidity: function sendMessage(string _to, string _svc, uint256 _sn, bytes _msg) returns()
func (_BMC *BMCTransactorSession) SendMessage(_to string, _svc string, _sn *big.Int, _msg []byte) (*types.Transaction, error) {
	return _BMC.Contract.SendMessage(&_BMC.TransactOpts, _to, _svc, _sn, _msg)
}

// SetLink is a paid mutator transaction binding the contract method 0xf216b155.
//
// Solidity: function setLink(string _link, uint256 _blockInterval, uint256 _maxAggregation, uint256 _delayLimit) returns()
func (_BMC *BMCTransactor) SetLink(opts *bind.TransactOpts, _link string, _blockInterval *big.Int, _maxAggregation *big.Int, _delayLimit *big.Int) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "setLink", _link, _blockInterval, _maxAggregation, _delayLimit)
}

// SetLink is a paid mutator transaction binding the contract method 0xf216b155.
//
// Solidity: function setLink(string _link, uint256 _blockInterval, uint256 _maxAggregation, uint256 _delayLimit) returns()
func (_BMC *BMCSession) SetLink(_link string, _blockInterval *big.Int, _maxAggregation *big.Int, _delayLimit *big.Int) (*types.Transaction, error) {
	return _BMC.Contract.SetLink(&_BMC.TransactOpts, _link, _blockInterval, _maxAggregation, _delayLimit)
}

// SetLink is a paid mutator transaction binding the contract method 0xf216b155.
//
// Solidity: function setLink(string _link, uint256 _blockInterval, uint256 _maxAggregation, uint256 _delayLimit) returns()
func (_BMC *BMCTransactorSession) SetLink(_link string, _blockInterval *big.Int, _maxAggregation *big.Int, _delayLimit *big.Int) (*types.Transaction, error) {
	return _BMC.Contract.SetLink(&_BMC.TransactOpts, _link, _blockInterval, _maxAggregation, _delayLimit)
}

// BMCErrorOnBTPErrorIterator is returned from FilterErrorOnBTPError and is used to iterate over the raw logs and unpacked data for ErrorOnBTPError events raised by the BMC contract.
type BMCErrorOnBTPErrorIterator struct {
	Event *BMCErrorOnBTPError // Event containing the contract specifics and raw log

	contract *bind.BoundContract // Generic contract to use for unpacking event data
	event    string              // Event name to use for unpacking event data

	logs chan types.Log        // Log channel receiving the found contract events
	sub  ethereum.Subscription // Subscription for errors, completion and termination
	done bool                  // Whether the subscription completed delivering logs
	fail error                 // Occurred error to stop iteration
}

// Next advances the iterator to the subsequent event, returning whether there
// are any more events found. In case of a retrieval or parsing error, false is
// returned and Error() can be queried for the exact failure.
func (it *BMCErrorOnBTPErrorIterator) Next() bool {
	// If the iterator failed, stop iterating
	if it.fail != nil {
		return false
	}
	// If the iterator completed, deliver directly whatever's available
	if it.done {
		select {
		case log := <-it.logs:
			it.Event = new(BMCErrorOnBTPError)
			if err := it.contract.UnpackLog(it.Event, it.event, log); err != nil {
				it.fail = err
				return false
			}
			it.Event.Raw = log
			return true

		default:
			return false
		}
	}
	// Iterator still in progress, wait for either a data or an error event
	select {
	case log := <-it.logs:
		it.Event = new(BMCErrorOnBTPError)
		if err := it.contract.UnpackLog(it.Event, it.event, log); err != nil {
			it.fail = err
			return false
		}
		it.Event.Raw = log
		return true

	case err := <-it.sub.Err():
		it.done = true
		it.fail = err
		return it.Next()
	}
}

// Error returns any retrieval or parsing error occurred during filtering.
func (it *BMCErrorOnBTPErrorIterator) Error() error {
	return it.fail
}

// Close terminates the iteration process, releasing any pending underlying
// resources.
func (it *BMCErrorOnBTPErrorIterator) Close() error {
	it.sub.Unsubscribe()
	return nil
}

// BMCErrorOnBTPError represents a ErrorOnBTPError event raised by the BMC contract.
type BMCErrorOnBTPError struct {
	Svc        string
	Sn         *big.Int
	Code       *big.Int
	ErrMsg     string
	SvcErrCode *big.Int
	SvcErrMsg  string
	Raw        types.Log // Blockchain specific contextual infos
}

// FilterErrorOnBTPError is a free log retrieval operation binding the contract event 0x45eab163faa71c8b113fcbc0dcc77bd39e7e3365be446895b5169bd97fc5522a.
//
// Solidity: event ErrorOnBTPError(string _svc, int256 _sn, uint256 _code, string _errMsg, uint256 _svcErrCode, string _svcErrMsg)
func (_BMC *BMCFilterer) FilterErrorOnBTPError(opts *bind.FilterOpts) (*BMCErrorOnBTPErrorIterator, error) {

	logs, sub, err := _BMC.contract.FilterLogs(opts, "ErrorOnBTPError")
	if err != nil {
		return nil, err
	}
	return &BMCErrorOnBTPErrorIterator{contract: _BMC.contract, event: "ErrorOnBTPError", logs: logs, sub: sub}, nil
}

// WatchErrorOnBTPError is a free log subscription operation binding the contract event 0x45eab163faa71c8b113fcbc0dcc77bd39e7e3365be446895b5169bd97fc5522a.
//
// Solidity: event ErrorOnBTPError(string _svc, int256 _sn, uint256 _code, string _errMsg, uint256 _svcErrCode, string _svcErrMsg)
func (_BMC *BMCFilterer) WatchErrorOnBTPError(opts *bind.WatchOpts, sink chan<- *BMCErrorOnBTPError) (event.Subscription, error) {

	logs, sub, err := _BMC.contract.WatchLogs(opts, "ErrorOnBTPError")
	if err != nil {
		return nil, err
	}
	return event.NewSubscription(func(quit <-chan struct{}) error {
		defer sub.Unsubscribe()
		for {
			select {
			case log := <-logs:
				// New log arrived, parse the event and forward to the user
				event := new(BMCErrorOnBTPError)
				if err := _BMC.contract.UnpackLog(event, "ErrorOnBTPError", log); err != nil {
					return err
				}
				event.Raw = log

				select {
				case sink <- event:
				case err := <-sub.Err():
					return err
				case <-quit:
					return nil
				}
			case err := <-sub.Err():
				return err
			case <-quit:
				return nil
			}
		}
	}), nil
}

// ParseErrorOnBTPError is a log parse operation binding the contract event 0x45eab163faa71c8b113fcbc0dcc77bd39e7e3365be446895b5169bd97fc5522a.
//
// Solidity: event ErrorOnBTPError(string _svc, int256 _sn, uint256 _code, string _errMsg, uint256 _svcErrCode, string _svcErrMsg)
func (_BMC *BMCFilterer) ParseErrorOnBTPError(log types.Log) (*BMCErrorOnBTPError, error) {
	event := new(BMCErrorOnBTPError)
	if err := _BMC.contract.UnpackLog(event, "ErrorOnBTPError", log); err != nil {
		return nil, err
	}
	event.Raw = log
	return event, nil
}

// BMCEventIterator is returned from FilterEvent and is used to iterate over the raw logs and unpacked data for Event events raised by the BMC contract.
type BMCEventIterator struct {
	Event *BMCEvent // Event containing the contract specifics and raw log

	contract *bind.BoundContract // Generic contract to use for unpacking event data
	event    string              // Event name to use for unpacking event data

	logs chan types.Log        // Log channel receiving the found contract events
	sub  ethereum.Subscription // Subscription for errors, completion and termination
	done bool                  // Whether the subscription completed delivering logs
	fail error                 // Occurred error to stop iteration
}

// Next advances the iterator to the subsequent event, returning whether there
// are any more events found. In case of a retrieval or parsing error, false is
// returned and Error() can be queried for the exact failure.
func (it *BMCEventIterator) Next() bool {
	// If the iterator failed, stop iterating
	if it.fail != nil {
		return false
	}
	// If the iterator completed, deliver directly whatever's available
	if it.done {
		select {
		case log := <-it.logs:
			it.Event = new(BMCEvent)
			if err := it.contract.UnpackLog(it.Event, it.event, log); err != nil {
				it.fail = err
				return false
			}
			it.Event.Raw = log
			return true

		default:
			return false
		}
	}
	// Iterator still in progress, wait for either a data or an error event
	select {
	case log := <-it.logs:
		it.Event = new(BMCEvent)
		if err := it.contract.UnpackLog(it.Event, it.event, log); err != nil {
			it.fail = err
			return false
		}
		it.Event.Raw = log
		return true

	case err := <-it.sub.Err():
		it.done = true
		it.fail = err
		return it.Next()
	}
}

// Error returns any retrieval or parsing error occurred during filtering.
func (it *BMCEventIterator) Error() error {
	return it.fail
}

// Close terminates the iteration process, releasing any pending underlying
// resources.
func (it *BMCEventIterator) Close() error {
	it.sub.Unsubscribe()
	return nil
}

// BMCEvent represents a Event event raised by the BMC contract.
type BMCEvent struct {
	Next string
	Seq  *big.Int
	Msg  []byte
	Raw  types.Log // Blockchain specific contextual infos
}

// FilterEvent is a free log retrieval operation binding the contract event 0x1a2c5215c284f5cdc84c0c302ec8805007bf6e649689b02828131a8cc5551674.
//
// Solidity: event Event(string _next, uint256 _seq, bytes _msg)
func (_BMC *BMCFilterer) FilterEvent(opts *bind.FilterOpts) (*BMCEventIterator, error) {

	logs, sub, err := _BMC.contract.FilterLogs(opts, "Event")
	if err != nil {
		return nil, err
	}
	return &BMCEventIterator{contract: _BMC.contract, event: "Event", logs: logs, sub: sub}, nil
}

// WatchEvent is a free log subscription operation binding the contract event 0x1a2c5215c284f5cdc84c0c302ec8805007bf6e649689b02828131a8cc5551674.
//
// Solidity: event Event(string _next, uint256 _seq, bytes _msg)
func (_BMC *BMCFilterer) WatchEvent(opts *bind.WatchOpts, sink chan<- *BMCEvent) (event.Subscription, error) {

	logs, sub, err := _BMC.contract.WatchLogs(opts, "Event")
	if err != nil {
		return nil, err
	}
	return event.NewSubscription(func(quit <-chan struct{}) error {
		defer sub.Unsubscribe()
		for {
			select {
			case log := <-logs:
				// New log arrived, parse the event and forward to the user
				event := new(BMCEvent)
				if err := _BMC.contract.UnpackLog(event, "Event", log); err != nil {
					return err
				}
				event.Raw = log

				select {
				case sink <- event:
				case err := <-sub.Err():
					return err
				case <-quit:
					return nil
				}
			case err := <-sub.Err():
				return err
			case <-quit:
				return nil
			}
		}
	}), nil
}

// ParseEvent is a log parse operation binding the contract event 0x1a2c5215c284f5cdc84c0c302ec8805007bf6e649689b02828131a8cc5551674.
//
// Solidity: event Event(string _next, uint256 _seq, bytes _msg)
func (_BMC *BMCFilterer) ParseEvent(log types.Log) (*BMCEvent, error) {
	event := new(BMCEvent)
	if err := _BMC.contract.UnpackLog(event, "Event", log); err != nil {
		return nil, err
	}
	event.Raw = log
	return event, nil
}

// BMCMessageIterator is returned from FilterMessage and is used to iterate over the raw logs and unpacked data for Message events raised by the BMC contract.
type BMCMessageIterator struct {
	Event *BMCMessage // Event containing the contract specifics and raw log

	contract *bind.BoundContract // Generic contract to use for unpacking event data
	event    string              // Event name to use for unpacking event data

	logs chan types.Log        // Log channel receiving the found contract events
	sub  ethereum.Subscription // Subscription for errors, completion and termination
	done bool                  // Whether the subscription completed delivering logs
	fail error                 // Occurred error to stop iteration
}

// Next advances the iterator to the subsequent event, returning whether there
// are any more events found. In case of a retrieval or parsing error, false is
// returned and Error() can be queried for the exact failure.
func (it *BMCMessageIterator) Next() bool {
	// If the iterator failed, stop iterating
	if it.fail != nil {
		return false
	}
	// If the iterator completed, deliver directly whatever's available
	if it.done {
		select {
		case log := <-it.logs:
			it.Event = new(BMCMessage)
			if err := it.contract.UnpackLog(it.Event, it.event, log); err != nil {
				it.fail = err
				return false
			}
			it.Event.Raw = log
			return true

		default:
			return false
		}
	}
	// Iterator still in progress, wait for either a data or an error event
	select {
	case log := <-it.logs:
		it.Event = new(BMCMessage)
		if err := it.contract.UnpackLog(it.Event, it.event, log); err != nil {
			it.fail = err
			return false
		}
		it.Event.Raw = log
		return true

	case err := <-it.sub.Err():
		it.done = true
		it.fail = err
		return it.Next()
	}
}

// Error returns any retrieval or parsing error occurred during filtering.
func (it *BMCMessageIterator) Error() error {
	return it.fail
}

// Close terminates the iteration process, releasing any pending underlying
// resources.
func (it *BMCMessageIterator) Close() error {
	it.sub.Unsubscribe()
	return nil
}

// BMCMessage represents a Message event raised by the BMC contract.
type BMCMessage struct {
	Next string
	Seq  *big.Int
	Msg  []byte
	Raw  types.Log // Blockchain specific contextual infos
}

// FilterMessage is a free log retrieval operation binding the contract event 0x37be353f216cf7e33639101fd610c542e6a0c0109173fa1c1d8b04d34edb7c1b.
//
// Solidity: event Message(string _next, uint256 _seq, bytes _msg)
func (_BMC *BMCFilterer) FilterMessage(opts *bind.FilterOpts) (*BMCMessageIterator, error) {

	logs, sub, err := _BMC.contract.FilterLogs(opts, "Message")
	if err != nil {
		return nil, err
	}
	return &BMCMessageIterator{contract: _BMC.contract, event: "Message", logs: logs, sub: sub}, nil
}

// WatchMessage is a free log subscription operation binding the contract event 0x37be353f216cf7e33639101fd610c542e6a0c0109173fa1c1d8b04d34edb7c1b.
//
// Solidity: event Message(string _next, uint256 _seq, bytes _msg)
func (_BMC *BMCFilterer) WatchMessage(opts *bind.WatchOpts, sink chan<- *BMCMessage) (event.Subscription, error) {

	logs, sub, err := _BMC.contract.WatchLogs(opts, "Message")
	if err != nil {
		return nil, err
	}
	return event.NewSubscription(func(quit <-chan struct{}) error {
		defer sub.Unsubscribe()
		for {
			select {
			case log := <-logs:
				// New log arrived, parse the event and forward to the user
				event := new(BMCMessage)
				if err := _BMC.contract.UnpackLog(event, "Message", log); err != nil {
					return err
				}
				event.Raw = log

				select {
				case sink <- event:
				case err := <-sub.Err():
					return err
				case <-quit:
					return nil
				}
			case err := <-sub.Err():
				return err
			case <-quit:
				return nil
			}
		}
	}), nil
}

// ParseMessage is a log parse operation binding the contract event 0x37be353f216cf7e33639101fd610c542e6a0c0109173fa1c1d8b04d34edb7c1b.
//
// Solidity: event Message(string _next, uint256 _seq, bytes _msg)
func (_BMC *BMCFilterer) ParseMessage(log types.Log) (*BMCMessage, error) {
	event := new(BMCMessage)
	if err := _BMC.contract.UnpackLog(event, "Message", log); err != nil {
		return nil, err
	}
	event.Raw = log
	return event, nil
}
