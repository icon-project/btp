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

// BmcABI is the input ABI used to generate the binding from.
const BmcABI = "[{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"}],\"name\":\"addLink\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"},{\"internalType\":\"address[]\",\"name\":\"_addrs\",\"type\":\"address[]\"}],\"name\":\"addRelay\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_dst\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"}],\"name\":\"addRoute\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_net\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"_addr\",\"type\":\"address\"}],\"name\":\"addVerifier\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_svc\",\"type\":\"string\"}],\"name\":\"approveService\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"getLinks\",\"outputs\":[{\"internalType\":\"string[]\",\"name\":\"_links\",\"type\":\"string[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"}],\"name\":\"getRelays\",\"outputs\":[{\"internalType\":\"address[]\",\"name\":\"_relayes\",\"type\":\"address[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"getRoutes\",\"outputs\":[{\"components\":[{\"internalType\":\"string\",\"name\":\"dst\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"next\",\"type\":\"string\"}],\"internalType\":\"structTypes.Route[]\",\"name\":\"_routes\",\"type\":\"tuple[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"getServices\",\"outputs\":[{\"components\":[{\"internalType\":\"string\",\"name\":\"svc\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"addr\",\"type\":\"address\"}],\"internalType\":\"structTypes.Service[]\",\"name\":\"_servicers\",\"type\":\"tuple[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"}],\"name\":\"getStatus\",\"outputs\":[{\"components\":[{\"internalType\":\"uint256\",\"name\":\"rxSeq\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"txSeq\",\"type\":\"uint256\"},{\"components\":[{\"internalType\":\"uint256\",\"name\":\"heightMTA\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"offsetMTA\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"lastHeight\",\"type\":\"uint256\"},{\"internalType\":\"bytes\",\"name\":\"extra\",\"type\":\"bytes\"}],\"internalType\":\"structTypes.VerifierStats\",\"name\":\"verifier\",\"type\":\"tuple\"},{\"components\":[{\"internalType\":\"address\",\"name\":\"addr\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"blockCount\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"msgCount\",\"type\":\"uint256\"}],\"internalType\":\"structTypes.RelayStats[]\",\"name\":\"relays\",\"type\":\"tuple[]\"},{\"internalType\":\"uint256\",\"name\":\"relayIdx\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"rotateHeight\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"rotateTerm\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"delayLimit\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"maxAggregation\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"rxHeightSrc\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"rxHeight\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"blockIntervalSrc\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"blockIntervalDst\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"currentHeight\",\"type\":\"uint256\"}],\"internalType\":\"structTypes.LinkStats\",\"name\":\"_linkStats\",\"type\":\"tuple\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"getVerifiers\",\"outputs\":[{\"components\":[{\"internalType\":\"string\",\"name\":\"net\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"addr\",\"type\":\"address\"}],\"internalType\":\"structTypes.Verifier[]\",\"name\":\"_verifiers\",\"type\":\"tuple[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_prev\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"_msg\",\"type\":\"string\"}],\"name\":\"handleRelayMessage\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"}],\"name\":\"removeLink\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"_addrs\",\"type\":\"address\"}],\"name\":\"removeRelay\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_dst\",\"type\":\"string\"}],\"name\":\"removeRoute\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_svc\",\"type\":\"string\"}],\"name\":\"removeService\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_net\",\"type\":\"string\"}],\"name\":\"removeVerifier\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_serviceName\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"_addr\",\"type\":\"address\"}],\"name\":\"requestAddService\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_to\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"_svc\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"_sn\",\"type\":\"uint256\"},{\"internalType\":\"bytes\",\"name\":\"_msg\",\"type\":\"bytes\"}],\"name\":\"sendMessage\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"_blockInterval\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"_maxAggregation\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"_delayLimit\",\"type\":\"uint256\"}],\"name\":\"setLink\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]"

// Bmc is an auto generated Go binding around an Ethereum contract.
type Bmc struct {
	BmcCaller     // Read-only binding to the contract
	BmcTransactor // Write-only binding to the contract
	BmcFilterer   // Log filterer for contract events
}

// BmcCaller is an auto generated read-only Go binding around an Ethereum contract.
type BmcCaller struct {
	contract *bind.BoundContract // Generic contract wrapper for the low level calls
}

// BmcTransactor is an auto generated write-only Go binding around an Ethereum contract.
type BmcTransactor struct {
	contract *bind.BoundContract // Generic contract wrapper for the low level calls
}

// BmcFilterer is an auto generated log filtering Go binding around an Ethereum contract events.
type BmcFilterer struct {
	contract *bind.BoundContract // Generic contract wrapper for the low level calls
}

// BmcSession is an auto generated Go binding around an Ethereum contract,
// with pre-set call and transact options.
type BmcSession struct {
	Contract     *Bmc              // Generic contract binding to set the session for
	CallOpts     bind.CallOpts     // Call options to use throughout this session
	TransactOpts bind.TransactOpts // Transaction auth options to use throughout this session
}

// BmcCallerSession is an auto generated read-only Go binding around an Ethereum contract,
// with pre-set call options.
type BmcCallerSession struct {
	Contract *BmcCaller    // Generic contract caller binding to set the session for
	CallOpts bind.CallOpts // Call options to use throughout this session
}

// BmcTransactorSession is an auto generated write-only Go binding around an Ethereum contract,
// with pre-set transact options.
type BmcTransactorSession struct {
	Contract     *BmcTransactor    // Generic contract transactor binding to set the session for
	TransactOpts bind.TransactOpts // Transaction auth options to use throughout this session
}

// BmcRaw is an auto generated low-level Go binding around an Ethereum contract.
type BmcRaw struct {
	Contract *Bmc // Generic contract binding to access the raw methods on
}

// BmcCallerRaw is an auto generated low-level read-only Go binding around an Ethereum contract.
type BmcCallerRaw struct {
	Contract *BmcCaller // Generic read-only contract binding to access the raw methods on
}

// BmcTransactorRaw is an auto generated low-level write-only Go binding around an Ethereum contract.
type BmcTransactorRaw struct {
	Contract *BmcTransactor // Generic write-only contract binding to access the raw methods on
}

// NewBmc creates a new instance of Bmc, bound to a specific deployed contract.
func NewBmc(address common.Address, backend bind.ContractBackend) (*Bmc, error) {
	contract, err := bindBmc(address, backend, backend, backend)
	if err != nil {
		return nil, err
	}
	return &Bmc{BmcCaller: BmcCaller{contract: contract}, BmcTransactor: BmcTransactor{contract: contract}, BmcFilterer: BmcFilterer{contract: contract}}, nil
}

// NewBmcCaller creates a new read-only instance of Bmc, bound to a specific deployed contract.
func NewBmcCaller(address common.Address, caller bind.ContractCaller) (*BmcCaller, error) {
	contract, err := bindBmc(address, caller, nil, nil)
	if err != nil {
		return nil, err
	}
	return &BmcCaller{contract: contract}, nil
}

// NewBmcTransactor creates a new write-only instance of Bmc, bound to a specific deployed contract.
func NewBmcTransactor(address common.Address, transactor bind.ContractTransactor) (*BmcTransactor, error) {
	contract, err := bindBmc(address, nil, transactor, nil)
	if err != nil {
		return nil, err
	}
	return &BmcTransactor{contract: contract}, nil
}

// NewBmcFilterer creates a new log filterer instance of Bmc, bound to a specific deployed contract.
func NewBmcFilterer(address common.Address, filterer bind.ContractFilterer) (*BmcFilterer, error) {
	contract, err := bindBmc(address, nil, nil, filterer)
	if err != nil {
		return nil, err
	}
	return &BmcFilterer{contract: contract}, nil
}

// bindBmc binds a generic wrapper to an already deployed contract.
func bindBmc(address common.Address, caller bind.ContractCaller, transactor bind.ContractTransactor, filterer bind.ContractFilterer) (*bind.BoundContract, error) {
	parsed, err := abi.JSON(strings.NewReader(BmcABI))
	if err != nil {
		return nil, err
	}
	return bind.NewBoundContract(address, parsed, caller, transactor, filterer), nil
}

// Call invokes the (constant) contract method with params as input values and
// sets the output to result. The result type might be a single field for simple
// returns, a slice of interfaces for anonymous returns and a struct for named
// returns.
func (_Bmc *BmcRaw) Call(opts *bind.CallOpts, result *[]interface{}, method string, params ...interface{}) error {
	return _Bmc.Contract.BmcCaller.contract.Call(opts, result, method, params...)
}

// Transfer initiates a plain transaction to move funds to the contract, calling
// its default method if one is available.
func (_Bmc *BmcRaw) Transfer(opts *bind.TransactOpts) (*types.Transaction, error) {
	return _Bmc.Contract.BmcTransactor.contract.Transfer(opts)
}

// Transact invokes the (paid) contract method with params as input values.
func (_Bmc *BmcRaw) Transact(opts *bind.TransactOpts, method string, params ...interface{}) (*types.Transaction, error) {
	return _Bmc.Contract.BmcTransactor.contract.Transact(opts, method, params...)
}

// Call invokes the (constant) contract method with params as input values and
// sets the output to result. The result type might be a single field for simple
// returns, a slice of interfaces for anonymous returns and a struct for named
// returns.
func (_Bmc *BmcCallerRaw) Call(opts *bind.CallOpts, result *[]interface{}, method string, params ...interface{}) error {
	return _Bmc.Contract.contract.Call(opts, result, method, params...)
}

// Transfer initiates a plain transaction to move funds to the contract, calling
// its default method if one is available.
func (_Bmc *BmcTransactorRaw) Transfer(opts *bind.TransactOpts) (*types.Transaction, error) {
	return _Bmc.Contract.contract.Transfer(opts)
}

// Transact invokes the (paid) contract method with params as input values.
func (_Bmc *BmcTransactorRaw) Transact(opts *bind.TransactOpts, method string, params ...interface{}) (*types.Transaction, error) {
	return _Bmc.Contract.contract.Transact(opts, method, params...)
}

// GetLinks is a free data retrieval call binding the contract method 0xf66ddcbb.
//
// Solidity: function getLinks() view returns(string[] _links)
func (_Bmc *BmcCaller) GetLinks(opts *bind.CallOpts) ([]string, error) {
	var out []interface{}
	err := _Bmc.contract.Call(opts, &out, "getLinks")

	if err != nil {
		return *new([]string), err
	}

	out0 := *abi.ConvertType(out[0], new([]string)).(*[]string)

	return out0, err

}

// GetLinks is a free data retrieval call binding the contract method 0xf66ddcbb.
//
// Solidity: function getLinks() view returns(string[] _links)
func (_Bmc *BmcSession) GetLinks() ([]string, error) {
	return _Bmc.Contract.GetLinks(&_Bmc.CallOpts)
}

// GetLinks is a free data retrieval call binding the contract method 0xf66ddcbb.
//
// Solidity: function getLinks() view returns(string[] _links)
func (_Bmc *BmcCallerSession) GetLinks() ([]string, error) {
	return _Bmc.Contract.GetLinks(&_Bmc.CallOpts)
}

// GetRelays is a free data retrieval call binding the contract method 0x40926734.
//
// Solidity: function getRelays(string _link) view returns(address[] _relayes)
func (_Bmc *BmcCaller) GetRelays(opts *bind.CallOpts, _link string) ([]common.Address, error) {
	var out []interface{}
	err := _Bmc.contract.Call(opts, &out, "getRelays", _link)

	if err != nil {
		return *new([]common.Address), err
	}

	out0 := *abi.ConvertType(out[0], new([]common.Address)).(*[]common.Address)

	return out0, err

}

// GetRelays is a free data retrieval call binding the contract method 0x40926734.
//
// Solidity: function getRelays(string _link) view returns(address[] _relayes)
func (_Bmc *BmcSession) GetRelays(_link string) ([]common.Address, error) {
	return _Bmc.Contract.GetRelays(&_Bmc.CallOpts, _link)
}

// GetRelays is a free data retrieval call binding the contract method 0x40926734.
//
// Solidity: function getRelays(string _link) view returns(address[] _relayes)
func (_Bmc *BmcCallerSession) GetRelays(_link string) ([]common.Address, error) {
	return _Bmc.Contract.GetRelays(&_Bmc.CallOpts, _link)
}

// GetRoutes is a free data retrieval call binding the contract method 0x7e928072.
//
// Solidity: function getRoutes() view returns((string,string)[] _routes)
func (_Bmc *BmcCaller) GetRoutes(opts *bind.CallOpts) ([]TypesRoute, error) {
	var out []interface{}
	err := _Bmc.contract.Call(opts, &out, "getRoutes")

	if err != nil {
		return *new([]TypesRoute), err
	}

	out0 := *abi.ConvertType(out[0], new([]TypesRoute)).(*[]TypesRoute)

	return out0, err

}

// GetRoutes is a free data retrieval call binding the contract method 0x7e928072.
//
// Solidity: function getRoutes() view returns((string,string)[] _routes)
func (_Bmc *BmcSession) GetRoutes() ([]TypesRoute, error) {
	return _Bmc.Contract.GetRoutes(&_Bmc.CallOpts)
}

// GetRoutes is a free data retrieval call binding the contract method 0x7e928072.
//
// Solidity: function getRoutes() view returns((string,string)[] _routes)
func (_Bmc *BmcCallerSession) GetRoutes() ([]TypesRoute, error) {
	return _Bmc.Contract.GetRoutes(&_Bmc.CallOpts)
}

// GetServices is a free data retrieval call binding the contract method 0x75417851.
//
// Solidity: function getServices() view returns((string,address)[] _servicers)
func (_Bmc *BmcCaller) GetServices(opts *bind.CallOpts) ([]TypesService, error) {
	var out []interface{}
	err := _Bmc.contract.Call(opts, &out, "getServices")

	if err != nil {
		return *new([]TypesService), err
	}

	out0 := *abi.ConvertType(out[0], new([]TypesService)).(*[]TypesService)

	return out0, err

}

// GetServices is a free data retrieval call binding the contract method 0x75417851.
//
// Solidity: function getServices() view returns((string,address)[] _servicers)
func (_Bmc *BmcSession) GetServices() ([]TypesService, error) {
	return _Bmc.Contract.GetServices(&_Bmc.CallOpts)
}

// GetServices is a free data retrieval call binding the contract method 0x75417851.
//
// Solidity: function getServices() view returns((string,address)[] _servicers)
func (_Bmc *BmcCallerSession) GetServices() ([]TypesService, error) {
	return _Bmc.Contract.GetServices(&_Bmc.CallOpts)
}

// GetStatus is a free data retrieval call binding the contract method 0x22b05ed2.
//
// Solidity: function getStatus(string _link) view returns((uint256,uint256,(uint256,uint256,uint256,bytes),(address,uint256,uint256)[],uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256) _linkStats)
func (_Bmc *BmcCaller) GetStatus(opts *bind.CallOpts, _link string) (TypesLinkStats, error) {
	var out []interface{}
	err := _Bmc.contract.Call(opts, &out, "getStatus", _link)

	if err != nil {
		return *new(TypesLinkStats), err
	}

	out0 := *abi.ConvertType(out[0], new(TypesLinkStats)).(*TypesLinkStats)

	return out0, err

}

// GetStatus is a free data retrieval call binding the contract method 0x22b05ed2.
//
// Solidity: function getStatus(string _link) view returns((uint256,uint256,(uint256,uint256,uint256,bytes),(address,uint256,uint256)[],uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256) _linkStats)
func (_Bmc *BmcSession) GetStatus(_link string) (TypesLinkStats, error) {
	return _Bmc.Contract.GetStatus(&_Bmc.CallOpts, _link)
}

// GetStatus is a free data retrieval call binding the contract method 0x22b05ed2.
//
// Solidity: function getStatus(string _link) view returns((uint256,uint256,(uint256,uint256,uint256,bytes),(address,uint256,uint256)[],uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256,uint256) _linkStats)
func (_Bmc *BmcCallerSession) GetStatus(_link string) (TypesLinkStats, error) {
	return _Bmc.Contract.GetStatus(&_Bmc.CallOpts, _link)
}

// GetVerifiers is a free data retrieval call binding the contract method 0xa935e766.
//
// Solidity: function getVerifiers() view returns((string,address)[] _verifiers)
func (_Bmc *BmcCaller) GetVerifiers(opts *bind.CallOpts) ([]TypesVerifier, error) {
	var out []interface{}
	err := _Bmc.contract.Call(opts, &out, "getVerifiers")

	if err != nil {
		return *new([]TypesVerifier), err
	}

	out0 := *abi.ConvertType(out[0], new([]TypesVerifier)).(*[]TypesVerifier)

	return out0, err

}

// GetVerifiers is a free data retrieval call binding the contract method 0xa935e766.
//
// Solidity: function getVerifiers() view returns((string,address)[] _verifiers)
func (_Bmc *BmcSession) GetVerifiers() ([]TypesVerifier, error) {
	return _Bmc.Contract.GetVerifiers(&_Bmc.CallOpts)
}

// GetVerifiers is a free data retrieval call binding the contract method 0xa935e766.
//
// Solidity: function getVerifiers() view returns((string,address)[] _verifiers)
func (_Bmc *BmcCallerSession) GetVerifiers() ([]TypesVerifier, error) {
	return _Bmc.Contract.GetVerifiers(&_Bmc.CallOpts)
}

// AddLink is a paid mutator transaction binding the contract method 0x22a618fa.
//
// Solidity: function addLink(string _link) returns()
func (_Bmc *BmcTransactor) AddLink(opts *bind.TransactOpts, _link string) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "addLink", _link)
}

// AddLink is a paid mutator transaction binding the contract method 0x22a618fa.
//
// Solidity: function addLink(string _link) returns()
func (_Bmc *BmcSession) AddLink(_link string) (*types.Transaction, error) {
	return _Bmc.Contract.AddLink(&_Bmc.TransactOpts, _link)
}

// AddLink is a paid mutator transaction binding the contract method 0x22a618fa.
//
// Solidity: function addLink(string _link) returns()
func (_Bmc *BmcTransactorSession) AddLink(_link string) (*types.Transaction, error) {
	return _Bmc.Contract.AddLink(&_Bmc.TransactOpts, _link)
}

// AddRelay is a paid mutator transaction binding the contract method 0x0748ea7a.
//
// Solidity: function addRelay(string _link, address[] _addrs) returns()
func (_Bmc *BmcTransactor) AddRelay(opts *bind.TransactOpts, _link string, _addrs []common.Address) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "addRelay", _link, _addrs)
}

// AddRelay is a paid mutator transaction binding the contract method 0x0748ea7a.
//
// Solidity: function addRelay(string _link, address[] _addrs) returns()
func (_Bmc *BmcSession) AddRelay(_link string, _addrs []common.Address) (*types.Transaction, error) {
	return _Bmc.Contract.AddRelay(&_Bmc.TransactOpts, _link, _addrs)
}

// AddRelay is a paid mutator transaction binding the contract method 0x0748ea7a.
//
// Solidity: function addRelay(string _link, address[] _addrs) returns()
func (_Bmc *BmcTransactorSession) AddRelay(_link string, _addrs []common.Address) (*types.Transaction, error) {
	return _Bmc.Contract.AddRelay(&_Bmc.TransactOpts, _link, _addrs)
}

// AddRoute is a paid mutator transaction binding the contract method 0x065a9e9b.
//
// Solidity: function addRoute(string _dst, string _link) returns()
func (_Bmc *BmcTransactor) AddRoute(opts *bind.TransactOpts, _dst string, _link string) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "addRoute", _dst, _link)
}

// AddRoute is a paid mutator transaction binding the contract method 0x065a9e9b.
//
// Solidity: function addRoute(string _dst, string _link) returns()
func (_Bmc *BmcSession) AddRoute(_dst string, _link string) (*types.Transaction, error) {
	return _Bmc.Contract.AddRoute(&_Bmc.TransactOpts, _dst, _link)
}

// AddRoute is a paid mutator transaction binding the contract method 0x065a9e9b.
//
// Solidity: function addRoute(string _dst, string _link) returns()
func (_Bmc *BmcTransactorSession) AddRoute(_dst string, _link string) (*types.Transaction, error) {
	return _Bmc.Contract.AddRoute(&_Bmc.TransactOpts, _dst, _link)
}

// AddVerifier is a paid mutator transaction binding the contract method 0x76b503c3.
//
// Solidity: function addVerifier(string _net, address _addr) returns()
func (_Bmc *BmcTransactor) AddVerifier(opts *bind.TransactOpts, _net string, _addr common.Address) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "addVerifier", _net, _addr)
}

// AddVerifier is a paid mutator transaction binding the contract method 0x76b503c3.
//
// Solidity: function addVerifier(string _net, address _addr) returns()
func (_Bmc *BmcSession) AddVerifier(_net string, _addr common.Address) (*types.Transaction, error) {
	return _Bmc.Contract.AddVerifier(&_Bmc.TransactOpts, _net, _addr)
}

// AddVerifier is a paid mutator transaction binding the contract method 0x76b503c3.
//
// Solidity: function addVerifier(string _net, address _addr) returns()
func (_Bmc *BmcTransactorSession) AddVerifier(_net string, _addr common.Address) (*types.Transaction, error) {
	return _Bmc.Contract.AddVerifier(&_Bmc.TransactOpts, _net, _addr)
}

// ApproveService is a paid mutator transaction binding the contract method 0xbe6c8dc9.
//
// Solidity: function approveService(string _svc) returns()
func (_Bmc *BmcTransactor) ApproveService(opts *bind.TransactOpts, _svc string) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "approveService", _svc)
}

// ApproveService is a paid mutator transaction binding the contract method 0xbe6c8dc9.
//
// Solidity: function approveService(string _svc) returns()
func (_Bmc *BmcSession) ApproveService(_svc string) (*types.Transaction, error) {
	return _Bmc.Contract.ApproveService(&_Bmc.TransactOpts, _svc)
}

// ApproveService is a paid mutator transaction binding the contract method 0xbe6c8dc9.
//
// Solidity: function approveService(string _svc) returns()
func (_Bmc *BmcTransactorSession) ApproveService(_svc string) (*types.Transaction, error) {
	return _Bmc.Contract.ApproveService(&_Bmc.TransactOpts, _svc)
}

// HandleRelayMessage is a paid mutator transaction binding the contract method 0x6f4779cc.
//
// Solidity: function handleRelayMessage(string _prev, string _msg) returns()
func (_Bmc *BmcTransactor) HandleRelayMessage(opts *bind.TransactOpts, _prev string, _msg string) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "handleRelayMessage", _prev, _msg)
}

// HandleRelayMessage is a paid mutator transaction binding the contract method 0x6f4779cc.
//
// Solidity: function handleRelayMessage(string _prev, string _msg) returns()
func (_Bmc *BmcSession) HandleRelayMessage(_prev string, _msg string) (*types.Transaction, error) {
	return _Bmc.Contract.HandleRelayMessage(&_Bmc.TransactOpts, _prev, _msg)
}

// HandleRelayMessage is a paid mutator transaction binding the contract method 0x6f4779cc.
//
// Solidity: function handleRelayMessage(string _prev, string _msg) returns()
func (_Bmc *BmcTransactorSession) HandleRelayMessage(_prev string, _msg string) (*types.Transaction, error) {
	return _Bmc.Contract.HandleRelayMessage(&_Bmc.TransactOpts, _prev, _msg)
}

// RemoveLink is a paid mutator transaction binding the contract method 0x6e4060d7.
//
// Solidity: function removeLink(string _link) returns()
func (_Bmc *BmcTransactor) RemoveLink(opts *bind.TransactOpts, _link string) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "removeLink", _link)
}

// RemoveLink is a paid mutator transaction binding the contract method 0x6e4060d7.
//
// Solidity: function removeLink(string _link) returns()
func (_Bmc *BmcSession) RemoveLink(_link string) (*types.Transaction, error) {
	return _Bmc.Contract.RemoveLink(&_Bmc.TransactOpts, _link)
}

// RemoveLink is a paid mutator transaction binding the contract method 0x6e4060d7.
//
// Solidity: function removeLink(string _link) returns()
func (_Bmc *BmcTransactorSession) RemoveLink(_link string) (*types.Transaction, error) {
	return _Bmc.Contract.RemoveLink(&_Bmc.TransactOpts, _link)
}

// RemoveRelay is a paid mutator transaction binding the contract method 0xdef59f5e.
//
// Solidity: function removeRelay(string _link, address _addrs) returns()
func (_Bmc *BmcTransactor) RemoveRelay(opts *bind.TransactOpts, _link string, _addrs common.Address) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "removeRelay", _link, _addrs)
}

// RemoveRelay is a paid mutator transaction binding the contract method 0xdef59f5e.
//
// Solidity: function removeRelay(string _link, address _addrs) returns()
func (_Bmc *BmcSession) RemoveRelay(_link string, _addrs common.Address) (*types.Transaction, error) {
	return _Bmc.Contract.RemoveRelay(&_Bmc.TransactOpts, _link, _addrs)
}

// RemoveRelay is a paid mutator transaction binding the contract method 0xdef59f5e.
//
// Solidity: function removeRelay(string _link, address _addrs) returns()
func (_Bmc *BmcTransactorSession) RemoveRelay(_link string, _addrs common.Address) (*types.Transaction, error) {
	return _Bmc.Contract.RemoveRelay(&_Bmc.TransactOpts, _link, _addrs)
}

// RemoveRoute is a paid mutator transaction binding the contract method 0xbd0a0bb3.
//
// Solidity: function removeRoute(string _dst) returns()
func (_Bmc *BmcTransactor) RemoveRoute(opts *bind.TransactOpts, _dst string) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "removeRoute", _dst)
}

// RemoveRoute is a paid mutator transaction binding the contract method 0xbd0a0bb3.
//
// Solidity: function removeRoute(string _dst) returns()
func (_Bmc *BmcSession) RemoveRoute(_dst string) (*types.Transaction, error) {
	return _Bmc.Contract.RemoveRoute(&_Bmc.TransactOpts, _dst)
}

// RemoveRoute is a paid mutator transaction binding the contract method 0xbd0a0bb3.
//
// Solidity: function removeRoute(string _dst) returns()
func (_Bmc *BmcTransactorSession) RemoveRoute(_dst string) (*types.Transaction, error) {
	return _Bmc.Contract.RemoveRoute(&_Bmc.TransactOpts, _dst)
}

// RemoveService is a paid mutator transaction binding the contract method 0xf51acaea.
//
// Solidity: function removeService(string _svc) returns()
func (_Bmc *BmcTransactor) RemoveService(opts *bind.TransactOpts, _svc string) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "removeService", _svc)
}

// RemoveService is a paid mutator transaction binding the contract method 0xf51acaea.
//
// Solidity: function removeService(string _svc) returns()
func (_Bmc *BmcSession) RemoveService(_svc string) (*types.Transaction, error) {
	return _Bmc.Contract.RemoveService(&_Bmc.TransactOpts, _svc)
}

// RemoveService is a paid mutator transaction binding the contract method 0xf51acaea.
//
// Solidity: function removeService(string _svc) returns()
func (_Bmc *BmcTransactorSession) RemoveService(_svc string) (*types.Transaction, error) {
	return _Bmc.Contract.RemoveService(&_Bmc.TransactOpts, _svc)
}

// RemoveVerifier is a paid mutator transaction binding the contract method 0xd740945f.
//
// Solidity: function removeVerifier(string _net) returns()
func (_Bmc *BmcTransactor) RemoveVerifier(opts *bind.TransactOpts, _net string) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "removeVerifier", _net)
}

// RemoveVerifier is a paid mutator transaction binding the contract method 0xd740945f.
//
// Solidity: function removeVerifier(string _net) returns()
func (_Bmc *BmcSession) RemoveVerifier(_net string) (*types.Transaction, error) {
	return _Bmc.Contract.RemoveVerifier(&_Bmc.TransactOpts, _net)
}

// RemoveVerifier is a paid mutator transaction binding the contract method 0xd740945f.
//
// Solidity: function removeVerifier(string _net) returns()
func (_Bmc *BmcTransactorSession) RemoveVerifier(_net string) (*types.Transaction, error) {
	return _Bmc.Contract.RemoveVerifier(&_Bmc.TransactOpts, _net)
}

// RequestAddService is a paid mutator transaction binding the contract method 0x7e09bd86.
//
// Solidity: function requestAddService(string _serviceName, address _addr) returns()
func (_Bmc *BmcTransactor) RequestAddService(opts *bind.TransactOpts, _serviceName string, _addr common.Address) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "requestAddService", _serviceName, _addr)
}

// RequestAddService is a paid mutator transaction binding the contract method 0x7e09bd86.
//
// Solidity: function requestAddService(string _serviceName, address _addr) returns()
func (_Bmc *BmcSession) RequestAddService(_serviceName string, _addr common.Address) (*types.Transaction, error) {
	return _Bmc.Contract.RequestAddService(&_Bmc.TransactOpts, _serviceName, _addr)
}

// RequestAddService is a paid mutator transaction binding the contract method 0x7e09bd86.
//
// Solidity: function requestAddService(string _serviceName, address _addr) returns()
func (_Bmc *BmcTransactorSession) RequestAddService(_serviceName string, _addr common.Address) (*types.Transaction, error) {
	return _Bmc.Contract.RequestAddService(&_Bmc.TransactOpts, _serviceName, _addr)
}

// SendMessage is a paid mutator transaction binding the contract method 0xbf6c1d9a.
//
// Solidity: function sendMessage(string _to, string _svc, uint256 _sn, bytes _msg) returns()
func (_Bmc *BmcTransactor) SendMessage(opts *bind.TransactOpts, _to string, _svc string, _sn *big.Int, _msg []byte) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "sendMessage", _to, _svc, _sn, _msg)
}

// SendMessage is a paid mutator transaction binding the contract method 0xbf6c1d9a.
//
// Solidity: function sendMessage(string _to, string _svc, uint256 _sn, bytes _msg) returns()
func (_Bmc *BmcSession) SendMessage(_to string, _svc string, _sn *big.Int, _msg []byte) (*types.Transaction, error) {
	return _Bmc.Contract.SendMessage(&_Bmc.TransactOpts, _to, _svc, _sn, _msg)
}

// SendMessage is a paid mutator transaction binding the contract method 0xbf6c1d9a.
//
// Solidity: function sendMessage(string _to, string _svc, uint256 _sn, bytes _msg) returns()
func (_Bmc *BmcTransactorSession) SendMessage(_to string, _svc string, _sn *big.Int, _msg []byte) (*types.Transaction, error) {
	return _Bmc.Contract.SendMessage(&_Bmc.TransactOpts, _to, _svc, _sn, _msg)
}

// SetLink is a paid mutator transaction binding the contract method 0xf216b155.
//
// Solidity: function setLink(string _link, uint256 _blockInterval, uint256 _maxAggregation, uint256 _delayLimit) returns()
func (_Bmc *BmcTransactor) SetLink(opts *bind.TransactOpts, _link string, _blockInterval *big.Int, _maxAggregation *big.Int, _delayLimit *big.Int) (*types.Transaction, error) {
	return _Bmc.contract.Transact(opts, "setLink", _link, _blockInterval, _maxAggregation, _delayLimit)
}

// SetLink is a paid mutator transaction binding the contract method 0xf216b155.
//
// Solidity: function setLink(string _link, uint256 _blockInterval, uint256 _maxAggregation, uint256 _delayLimit) returns()
func (_Bmc *BmcSession) SetLink(_link string, _blockInterval *big.Int, _maxAggregation *big.Int, _delayLimit *big.Int) (*types.Transaction, error) {
	return _Bmc.Contract.SetLink(&_Bmc.TransactOpts, _link, _blockInterval, _maxAggregation, _delayLimit)
}

// SetLink is a paid mutator transaction binding the contract method 0xf216b155.
//
// Solidity: function setLink(string _link, uint256 _blockInterval, uint256 _maxAggregation, uint256 _delayLimit) returns()
func (_Bmc *BmcTransactorSession) SetLink(_link string, _blockInterval *big.Int, _maxAggregation *big.Int, _delayLimit *big.Int) (*types.Transaction, error) {
	return _Bmc.Contract.SetLink(&_Bmc.TransactOpts, _link, _blockInterval, _maxAggregation, _delayLimit)
}
