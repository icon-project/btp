// Code generated - DO NOT EDIT.
// This file is a generated binding and any manual changes will be lost.

package client

import (
	"errors"
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
	_ = errors.New
	_ = big.NewInt
	_ = strings.NewReader
	_ = ethereum.NotFound
	_ = bind.Bind
	_ = common.Big1
	_ = types.BloomLookup
	_ = event.NewSubscription
)

// IBMVVerifierStatus is an auto generated low-level Go binding around an user-defined struct.
type IBMVVerifierStatus struct {
	Height *big.Int
	Extra  []byte
}

// TypesBTPMessage is an auto generated low-level Go binding around an user-defined struct.
type TypesBTPMessage struct {
	Src     string
	Dst     string
	Svc     string
	Sn      *big.Int
	Message []byte
	Nsn     *big.Int
	FeeInfo TypesFeeInfo
}

// TypesFeeInfo is an auto generated low-level Go binding around an user-defined struct.
type TypesFeeInfo struct {
	Network string
	Values  []*big.Int
}

// TypesLinkStatus is an auto generated low-level Go binding around an user-defined struct.
type TypesLinkStatus struct {
	RxSeq         *big.Int
	TxSeq         *big.Int
	Verifier      IBMVVerifierStatus
	CurrentHeight *big.Int
}

// BMCMetaData contains all meta data concerning the BMC contract.
var BMCMetaData = &bind.MetaData{
	ABI: "[{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_src\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"int256\",\"name\":\"_nsn\",\"type\":\"int256\"},{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_next\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_event\",\"type\":\"string\"}],\"name\":\"BTPEvent\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"internalType\":\"address\",\"name\":\"_sender\",\"type\":\"address\"},{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_network\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_receiver\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"_amount\",\"type\":\"uint256\"},{\"indexed\":false,\"internalType\":\"int256\",\"name\":\"_nsn\",\"type\":\"int256\"}],\"name\":\"ClaimReward\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"internalType\":\"address\",\"name\":\"_sender\",\"type\":\"address\"},{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_network\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"int256\",\"name\":\"_nsn\",\"type\":\"int256\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"_result\",\"type\":\"uint256\"}],\"name\":\"ClaimRewardResult\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"internalType\":\"uint8\",\"name\":\"version\",\"type\":\"uint8\"}],\"name\":\"Initialized\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_next\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"_seq\",\"type\":\"uint256\"},{\"indexed\":false,\"internalType\":\"bytes\",\"name\":\"_msg\",\"type\":\"bytes\"}],\"name\":\"Message\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_prev\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"_seq\",\"type\":\"uint256\"},{\"indexed\":false,\"internalType\":\"bytes\",\"name\":\"_msg\",\"type\":\"bytes\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"_ecode\",\"type\":\"uint256\"},{\"indexed\":false,\"internalType\":\"string\",\"name\":\"_emsg\",\"type\":\"string\"}],\"name\":\"MessageDropped\",\"type\":\"event\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_network\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"_receiver\",\"type\":\"string\"}],\"name\":\"claimReward\",\"outputs\":[],\"stateMutability\":\"payable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"}],\"name\":\"clearSeq\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_prev\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"_seq\",\"type\":\"uint256\"},{\"components\":[{\"internalType\":\"string\",\"name\":\"src\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"dst\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"svc\",\"type\":\"string\"},{\"internalType\":\"int256\",\"name\":\"sn\",\"type\":\"int256\"},{\"internalType\":\"bytes\",\"name\":\"message\",\"type\":\"bytes\"},{\"internalType\":\"int256\",\"name\":\"nsn\",\"type\":\"int256\"},{\"components\":[{\"internalType\":\"string\",\"name\":\"network\",\"type\":\"string\"},{\"internalType\":\"uint256[]\",\"name\":\"values\",\"type\":\"uint256[]\"}],\"internalType\":\"structTypes.FeeInfo\",\"name\":\"feeInfo\",\"type\":\"tuple\"}],\"internalType\":\"structTypes.BTPMessage\",\"name\":\"_msg\",\"type\":\"tuple\"}],\"name\":\"dropMessage\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"address\",\"name\":\"_sender\",\"type\":\"address\"},{\"internalType\":\"string\",\"name\":\"_network\",\"type\":\"string\"},{\"internalType\":\"int256\",\"name\":\"_nsn\",\"type\":\"int256\"},{\"internalType\":\"uint256\",\"name\":\"_result\",\"type\":\"uint256\"}],\"name\":\"emitClaimRewardResult\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"getBtpAddress\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_to\",\"type\":\"string\"},{\"internalType\":\"bool\",\"name\":\"_response\",\"type\":\"bool\"}],\"name\":\"getFee\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"getNetworkAddress\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"getNetworkSn\",\"outputs\":[{\"internalType\":\"int256\",\"name\":\"\",\"type\":\"int256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_network\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"_addr\",\"type\":\"address\"}],\"name\":\"getReward\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_link\",\"type\":\"string\"}],\"name\":\"getStatus\",\"outputs\":[{\"components\":[{\"internalType\":\"uint256\",\"name\":\"rxSeq\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"txSeq\",\"type\":\"uint256\"},{\"components\":[{\"internalType\":\"uint256\",\"name\":\"height\",\"type\":\"uint256\"},{\"internalType\":\"bytes\",\"name\":\"extra\",\"type\":\"bytes\"}],\"internalType\":\"structIBMV.VerifierStatus\",\"name\":\"verifier\",\"type\":\"tuple\"},{\"internalType\":\"uint256\",\"name\":\"currentHeight\",\"type\":\"uint256\"}],\"internalType\":\"structTypes.LinkStatus\",\"name\":\"\",\"type\":\"tuple\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_prev\",\"type\":\"string\"},{\"internalType\":\"bytes\",\"name\":\"_msg\",\"type\":\"bytes\"}],\"name\":\"handleRelayMessage\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_network\",\"type\":\"string\"},{\"internalType\":\"address\",\"name\":\"_bmcManagementAddr\",\"type\":\"address\"},{\"internalType\":\"address\",\"name\":\"_bmcServiceAddr\",\"type\":\"address\"}],\"name\":\"initialize\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_next\",\"type\":\"string\"},{\"internalType\":\"bytes\",\"name\":\"_msg\",\"type\":\"bytes\"}],\"name\":\"sendInternal\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"_to\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"_svc\",\"type\":\"string\"},{\"internalType\":\"int256\",\"name\":\"_sn\",\"type\":\"int256\"},{\"internalType\":\"bytes\",\"name\":\"_msg\",\"type\":\"bytes\"}],\"name\":\"sendMessage\",\"outputs\":[{\"internalType\":\"int256\",\"name\":\"\",\"type\":\"int256\"}],\"stateMutability\":\"payable\",\"type\":\"function\"},{\"stateMutability\":\"payable\",\"type\":\"receive\"}]",
}

// BMCABI is the input ABI used to generate the binding from.
// Deprecated: Use BMCMetaData.ABI instead.
var BMCABI = BMCMetaData.ABI

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

// GetBtpAddress is a free data retrieval call binding the contract method 0x4f63a21d.
//
// Solidity: function getBtpAddress() view returns(string)
func (_BMC *BMCCaller) GetBtpAddress(opts *bind.CallOpts) (string, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "getBtpAddress")

	if err != nil {
		return *new(string), err
	}

	out0 := *abi.ConvertType(out[0], new(string)).(*string)

	return out0, err

}

// GetBtpAddress is a free data retrieval call binding the contract method 0x4f63a21d.
//
// Solidity: function getBtpAddress() view returns(string)
func (_BMC *BMCSession) GetBtpAddress() (string, error) {
	return _BMC.Contract.GetBtpAddress(&_BMC.CallOpts)
}

// GetBtpAddress is a free data retrieval call binding the contract method 0x4f63a21d.
//
// Solidity: function getBtpAddress() view returns(string)
func (_BMC *BMCCallerSession) GetBtpAddress() (string, error) {
	return _BMC.Contract.GetBtpAddress(&_BMC.CallOpts)
}

// GetFee is a free data retrieval call binding the contract method 0x7d4c4f4a.
//
// Solidity: function getFee(string _to, bool _response) view returns(uint256)
func (_BMC *BMCCaller) GetFee(opts *bind.CallOpts, _to string, _response bool) (*big.Int, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "getFee", _to, _response)

	if err != nil {
		return *new(*big.Int), err
	}

	out0 := *abi.ConvertType(out[0], new(*big.Int)).(**big.Int)

	return out0, err

}

// GetFee is a free data retrieval call binding the contract method 0x7d4c4f4a.
//
// Solidity: function getFee(string _to, bool _response) view returns(uint256)
func (_BMC *BMCSession) GetFee(_to string, _response bool) (*big.Int, error) {
	return _BMC.Contract.GetFee(&_BMC.CallOpts, _to, _response)
}

// GetFee is a free data retrieval call binding the contract method 0x7d4c4f4a.
//
// Solidity: function getFee(string _to, bool _response) view returns(uint256)
func (_BMC *BMCCallerSession) GetFee(_to string, _response bool) (*big.Int, error) {
	return _BMC.Contract.GetFee(&_BMC.CallOpts, _to, _response)
}

// GetNetworkAddress is a free data retrieval call binding the contract method 0x6bf459cb.
//
// Solidity: function getNetworkAddress() view returns(string)
func (_BMC *BMCCaller) GetNetworkAddress(opts *bind.CallOpts) (string, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "getNetworkAddress")

	if err != nil {
		return *new(string), err
	}

	out0 := *abi.ConvertType(out[0], new(string)).(*string)

	return out0, err

}

// GetNetworkAddress is a free data retrieval call binding the contract method 0x6bf459cb.
//
// Solidity: function getNetworkAddress() view returns(string)
func (_BMC *BMCSession) GetNetworkAddress() (string, error) {
	return _BMC.Contract.GetNetworkAddress(&_BMC.CallOpts)
}

// GetNetworkAddress is a free data retrieval call binding the contract method 0x6bf459cb.
//
// Solidity: function getNetworkAddress() view returns(string)
func (_BMC *BMCCallerSession) GetNetworkAddress() (string, error) {
	return _BMC.Contract.GetNetworkAddress(&_BMC.CallOpts)
}

// GetNetworkSn is a free data retrieval call binding the contract method 0x676286ba.
//
// Solidity: function getNetworkSn() view returns(int256)
func (_BMC *BMCCaller) GetNetworkSn(opts *bind.CallOpts) (*big.Int, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "getNetworkSn")

	if err != nil {
		return *new(*big.Int), err
	}

	out0 := *abi.ConvertType(out[0], new(*big.Int)).(**big.Int)

	return out0, err

}

// GetNetworkSn is a free data retrieval call binding the contract method 0x676286ba.
//
// Solidity: function getNetworkSn() view returns(int256)
func (_BMC *BMCSession) GetNetworkSn() (*big.Int, error) {
	return _BMC.Contract.GetNetworkSn(&_BMC.CallOpts)
}

// GetNetworkSn is a free data retrieval call binding the contract method 0x676286ba.
//
// Solidity: function getNetworkSn() view returns(int256)
func (_BMC *BMCCallerSession) GetNetworkSn() (*big.Int, error) {
	return _BMC.Contract.GetNetworkSn(&_BMC.CallOpts)
}

// GetReward is a free data retrieval call binding the contract method 0x64cd3750.
//
// Solidity: function getReward(string _network, address _addr) view returns(uint256)
func (_BMC *BMCCaller) GetReward(opts *bind.CallOpts, _network string, _addr common.Address) (*big.Int, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "getReward", _network, _addr)

	if err != nil {
		return *new(*big.Int), err
	}

	out0 := *abi.ConvertType(out[0], new(*big.Int)).(**big.Int)

	return out0, err

}

// GetReward is a free data retrieval call binding the contract method 0x64cd3750.
//
// Solidity: function getReward(string _network, address _addr) view returns(uint256)
func (_BMC *BMCSession) GetReward(_network string, _addr common.Address) (*big.Int, error) {
	return _BMC.Contract.GetReward(&_BMC.CallOpts, _network, _addr)
}

// GetReward is a free data retrieval call binding the contract method 0x64cd3750.
//
// Solidity: function getReward(string _network, address _addr) view returns(uint256)
func (_BMC *BMCCallerSession) GetReward(_network string, _addr common.Address) (*big.Int, error) {
	return _BMC.Contract.GetReward(&_BMC.CallOpts, _network, _addr)
}

// GetStatus is a free data retrieval call binding the contract method 0x22b05ed2.
//
// Solidity: function getStatus(string _link) view returns((uint256,uint256,(uint256,bytes),uint256))
func (_BMC *BMCCaller) GetStatus(opts *bind.CallOpts, _link string) (TypesLinkStatus, error) {
	var out []interface{}
	err := _BMC.contract.Call(opts, &out, "getStatus", _link)

	if err != nil {
		return *new(TypesLinkStatus), err
	}

	out0 := *abi.ConvertType(out[0], new(TypesLinkStatus)).(*TypesLinkStatus)

	return out0, err

}

// GetStatus is a free data retrieval call binding the contract method 0x22b05ed2.
//
// Solidity: function getStatus(string _link) view returns((uint256,uint256,(uint256,bytes),uint256))
func (_BMC *BMCSession) GetStatus(_link string) (TypesLinkStatus, error) {
	return _BMC.Contract.GetStatus(&_BMC.CallOpts, _link)
}

// GetStatus is a free data retrieval call binding the contract method 0x22b05ed2.
//
// Solidity: function getStatus(string _link) view returns((uint256,uint256,(uint256,bytes),uint256))
func (_BMC *BMCCallerSession) GetStatus(_link string) (TypesLinkStatus, error) {
	return _BMC.Contract.GetStatus(&_BMC.CallOpts, _link)
}

// ClaimReward is a paid mutator transaction binding the contract method 0xcb7477b5.
//
// Solidity: function claimReward(string _network, string _receiver) payable returns()
func (_BMC *BMCTransactor) ClaimReward(opts *bind.TransactOpts, _network string, _receiver string) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "claimReward", _network, _receiver)
}

// ClaimReward is a paid mutator transaction binding the contract method 0xcb7477b5.
//
// Solidity: function claimReward(string _network, string _receiver) payable returns()
func (_BMC *BMCSession) ClaimReward(_network string, _receiver string) (*types.Transaction, error) {
	return _BMC.Contract.ClaimReward(&_BMC.TransactOpts, _network, _receiver)
}

// ClaimReward is a paid mutator transaction binding the contract method 0xcb7477b5.
//
// Solidity: function claimReward(string _network, string _receiver) payable returns()
func (_BMC *BMCTransactorSession) ClaimReward(_network string, _receiver string) (*types.Transaction, error) {
	return _BMC.Contract.ClaimReward(&_BMC.TransactOpts, _network, _receiver)
}

// ClearSeq is a paid mutator transaction binding the contract method 0x019d2420.
//
// Solidity: function clearSeq(string _link) returns()
func (_BMC *BMCTransactor) ClearSeq(opts *bind.TransactOpts, _link string) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "clearSeq", _link)
}

// ClearSeq is a paid mutator transaction binding the contract method 0x019d2420.
//
// Solidity: function clearSeq(string _link) returns()
func (_BMC *BMCSession) ClearSeq(_link string) (*types.Transaction, error) {
	return _BMC.Contract.ClearSeq(&_BMC.TransactOpts, _link)
}

// ClearSeq is a paid mutator transaction binding the contract method 0x019d2420.
//
// Solidity: function clearSeq(string _link) returns()
func (_BMC *BMCTransactorSession) ClearSeq(_link string) (*types.Transaction, error) {
	return _BMC.Contract.ClearSeq(&_BMC.TransactOpts, _link)
}

// DropMessage is a paid mutator transaction binding the contract method 0x39ff9dc1.
//
// Solidity: function dropMessage(string _prev, uint256 _seq, (string,string,string,int256,bytes,int256,(string,uint256[])) _msg) returns()
func (_BMC *BMCTransactor) DropMessage(opts *bind.TransactOpts, _prev string, _seq *big.Int, _msg TypesBTPMessage) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "dropMessage", _prev, _seq, _msg)
}

// DropMessage is a paid mutator transaction binding the contract method 0x39ff9dc1.
//
// Solidity: function dropMessage(string _prev, uint256 _seq, (string,string,string,int256,bytes,int256,(string,uint256[])) _msg) returns()
func (_BMC *BMCSession) DropMessage(_prev string, _seq *big.Int, _msg TypesBTPMessage) (*types.Transaction, error) {
	return _BMC.Contract.DropMessage(&_BMC.TransactOpts, _prev, _seq, _msg)
}

// DropMessage is a paid mutator transaction binding the contract method 0x39ff9dc1.
//
// Solidity: function dropMessage(string _prev, uint256 _seq, (string,string,string,int256,bytes,int256,(string,uint256[])) _msg) returns()
func (_BMC *BMCTransactorSession) DropMessage(_prev string, _seq *big.Int, _msg TypesBTPMessage) (*types.Transaction, error) {
	return _BMC.Contract.DropMessage(&_BMC.TransactOpts, _prev, _seq, _msg)
}

// EmitClaimRewardResult is a paid mutator transaction binding the contract method 0x947dfac2.
//
// Solidity: function emitClaimRewardResult(address _sender, string _network, int256 _nsn, uint256 _result) returns()
func (_BMC *BMCTransactor) EmitClaimRewardResult(opts *bind.TransactOpts, _sender common.Address, _network string, _nsn *big.Int, _result *big.Int) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "emitClaimRewardResult", _sender, _network, _nsn, _result)
}

// EmitClaimRewardResult is a paid mutator transaction binding the contract method 0x947dfac2.
//
// Solidity: function emitClaimRewardResult(address _sender, string _network, int256 _nsn, uint256 _result) returns()
func (_BMC *BMCSession) EmitClaimRewardResult(_sender common.Address, _network string, _nsn *big.Int, _result *big.Int) (*types.Transaction, error) {
	return _BMC.Contract.EmitClaimRewardResult(&_BMC.TransactOpts, _sender, _network, _nsn, _result)
}

// EmitClaimRewardResult is a paid mutator transaction binding the contract method 0x947dfac2.
//
// Solidity: function emitClaimRewardResult(address _sender, string _network, int256 _nsn, uint256 _result) returns()
func (_BMC *BMCTransactorSession) EmitClaimRewardResult(_sender common.Address, _network string, _nsn *big.Int, _result *big.Int) (*types.Transaction, error) {
	return _BMC.Contract.EmitClaimRewardResult(&_BMC.TransactOpts, _sender, _network, _nsn, _result)
}

// HandleRelayMessage is a paid mutator transaction binding the contract method 0x21b1e9bb.
//
// Solidity: function handleRelayMessage(string _prev, bytes _msg) returns()
func (_BMC *BMCTransactor) HandleRelayMessage(opts *bind.TransactOpts, _prev string, _msg []byte) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "handleRelayMessage", _prev, _msg)
}

// HandleRelayMessage is a paid mutator transaction binding the contract method 0x21b1e9bb.
//
// Solidity: function handleRelayMessage(string _prev, bytes _msg) returns()
func (_BMC *BMCSession) HandleRelayMessage(_prev string, _msg []byte) (*types.Transaction, error) {
	return _BMC.Contract.HandleRelayMessage(&_BMC.TransactOpts, _prev, _msg)
}

// HandleRelayMessage is a paid mutator transaction binding the contract method 0x21b1e9bb.
//
// Solidity: function handleRelayMessage(string _prev, bytes _msg) returns()
func (_BMC *BMCTransactorSession) HandleRelayMessage(_prev string, _msg []byte) (*types.Transaction, error) {
	return _BMC.Contract.HandleRelayMessage(&_BMC.TransactOpts, _prev, _msg)
}

// Initialize is a paid mutator transaction binding the contract method 0x463fd1af.
//
// Solidity: function initialize(string _network, address _bmcManagementAddr, address _bmcServiceAddr) returns()
func (_BMC *BMCTransactor) Initialize(opts *bind.TransactOpts, _network string, _bmcManagementAddr common.Address, _bmcServiceAddr common.Address) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "initialize", _network, _bmcManagementAddr, _bmcServiceAddr)
}

// Initialize is a paid mutator transaction binding the contract method 0x463fd1af.
//
// Solidity: function initialize(string _network, address _bmcManagementAddr, address _bmcServiceAddr) returns()
func (_BMC *BMCSession) Initialize(_network string, _bmcManagementAddr common.Address, _bmcServiceAddr common.Address) (*types.Transaction, error) {
	return _BMC.Contract.Initialize(&_BMC.TransactOpts, _network, _bmcManagementAddr, _bmcServiceAddr)
}

// Initialize is a paid mutator transaction binding the contract method 0x463fd1af.
//
// Solidity: function initialize(string _network, address _bmcManagementAddr, address _bmcServiceAddr) returns()
func (_BMC *BMCTransactorSession) Initialize(_network string, _bmcManagementAddr common.Address, _bmcServiceAddr common.Address) (*types.Transaction, error) {
	return _BMC.Contract.Initialize(&_BMC.TransactOpts, _network, _bmcManagementAddr, _bmcServiceAddr)
}

// SendInternal is a paid mutator transaction binding the contract method 0xdf9ccb10.
//
// Solidity: function sendInternal(string _next, bytes _msg) returns()
func (_BMC *BMCTransactor) SendInternal(opts *bind.TransactOpts, _next string, _msg []byte) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "sendInternal", _next, _msg)
}

// SendInternal is a paid mutator transaction binding the contract method 0xdf9ccb10.
//
// Solidity: function sendInternal(string _next, bytes _msg) returns()
func (_BMC *BMCSession) SendInternal(_next string, _msg []byte) (*types.Transaction, error) {
	return _BMC.Contract.SendInternal(&_BMC.TransactOpts, _next, _msg)
}

// SendInternal is a paid mutator transaction binding the contract method 0xdf9ccb10.
//
// Solidity: function sendInternal(string _next, bytes _msg) returns()
func (_BMC *BMCTransactorSession) SendInternal(_next string, _msg []byte) (*types.Transaction, error) {
	return _BMC.Contract.SendInternal(&_BMC.TransactOpts, _next, _msg)
}

// SendMessage is a paid mutator transaction binding the contract method 0x522a901e.
//
// Solidity: function sendMessage(string _to, string _svc, int256 _sn, bytes _msg) payable returns(int256)
func (_BMC *BMCTransactor) SendMessage(opts *bind.TransactOpts, _to string, _svc string, _sn *big.Int, _msg []byte) (*types.Transaction, error) {
	return _BMC.contract.Transact(opts, "sendMessage", _to, _svc, _sn, _msg)
}

// SendMessage is a paid mutator transaction binding the contract method 0x522a901e.
//
// Solidity: function sendMessage(string _to, string _svc, int256 _sn, bytes _msg) payable returns(int256)
func (_BMC *BMCSession) SendMessage(_to string, _svc string, _sn *big.Int, _msg []byte) (*types.Transaction, error) {
	return _BMC.Contract.SendMessage(&_BMC.TransactOpts, _to, _svc, _sn, _msg)
}

// SendMessage is a paid mutator transaction binding the contract method 0x522a901e.
//
// Solidity: function sendMessage(string _to, string _svc, int256 _sn, bytes _msg) payable returns(int256)
func (_BMC *BMCTransactorSession) SendMessage(_to string, _svc string, _sn *big.Int, _msg []byte) (*types.Transaction, error) {
	return _BMC.Contract.SendMessage(&_BMC.TransactOpts, _to, _svc, _sn, _msg)
}

// Receive is a paid mutator transaction binding the contract receive function.
//
// Solidity: receive() payable returns()
func (_BMC *BMCTransactor) Receive(opts *bind.TransactOpts) (*types.Transaction, error) {
	return _BMC.contract.RawTransact(opts, nil) // calldata is disallowed for receive function
}

// Receive is a paid mutator transaction binding the contract receive function.
//
// Solidity: receive() payable returns()
func (_BMC *BMCSession) Receive() (*types.Transaction, error) {
	return _BMC.Contract.Receive(&_BMC.TransactOpts)
}

// Receive is a paid mutator transaction binding the contract receive function.
//
// Solidity: receive() payable returns()
func (_BMC *BMCTransactorSession) Receive() (*types.Transaction, error) {
	return _BMC.Contract.Receive(&_BMC.TransactOpts)
}

// BMCBTPEventIterator is returned from FilterBTPEvent and is used to iterate over the raw logs and unpacked data for BTPEvent events raised by the BMC contract.
type BMCBTPEventIterator struct {
	Event *BMCBTPEvent // Event containing the contract specifics and raw log

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
func (it *BMCBTPEventIterator) Next() bool {
	// If the iterator failed, stop iterating
	if it.fail != nil {
		return false
	}
	// If the iterator completed, deliver directly whatever's available
	if it.done {
		select {
		case log := <-it.logs:
			it.Event = new(BMCBTPEvent)
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
		it.Event = new(BMCBTPEvent)
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
func (it *BMCBTPEventIterator) Error() error {
	return it.fail
}

// Close terminates the iteration process, releasing any pending underlying
// resources.
func (it *BMCBTPEventIterator) Close() error {
	it.sub.Unsubscribe()
	return nil
}

// BMCBTPEvent represents a BTPEvent event raised by the BMC contract.
type BMCBTPEvent struct {
	Src   string
	Nsn   *big.Int
	Next  string
	Event string
	Raw   types.Log // Blockchain specific contextual infos
}

// FilterBTPEvent is a free log retrieval operation binding the contract event 0x51f135d1c44e53689ca91af3b1bce4918d2b590d92bb76a854ab30e7de741828.
//
// Solidity: event BTPEvent(string _src, int256 _nsn, string _next, string _event)
func (_BMC *BMCFilterer) FilterBTPEvent(opts *bind.FilterOpts) (*BMCBTPEventIterator, error) {

	logs, sub, err := _BMC.contract.FilterLogs(opts, "BTPEvent")
	if err != nil {
		return nil, err
	}
	return &BMCBTPEventIterator{contract: _BMC.contract, event: "BTPEvent", logs: logs, sub: sub}, nil
}

// WatchBTPEvent is a free log subscription operation binding the contract event 0x51f135d1c44e53689ca91af3b1bce4918d2b590d92bb76a854ab30e7de741828.
//
// Solidity: event BTPEvent(string _src, int256 _nsn, string _next, string _event)
func (_BMC *BMCFilterer) WatchBTPEvent(opts *bind.WatchOpts, sink chan<- *BMCBTPEvent) (event.Subscription, error) {

	logs, sub, err := _BMC.contract.WatchLogs(opts, "BTPEvent")
	if err != nil {
		return nil, err
	}
	return event.NewSubscription(func(quit <-chan struct{}) error {
		defer sub.Unsubscribe()
		for {
			select {
			case log := <-logs:
				// New log arrived, parse the event and forward to the user
				event := new(BMCBTPEvent)
				if err := _BMC.contract.UnpackLog(event, "BTPEvent", log); err != nil {
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

// ParseBTPEvent is a log parse operation binding the contract event 0x51f135d1c44e53689ca91af3b1bce4918d2b590d92bb76a854ab30e7de741828.
//
// Solidity: event BTPEvent(string _src, int256 _nsn, string _next, string _event)
func (_BMC *BMCFilterer) ParseBTPEvent(log types.Log) (*BMCBTPEvent, error) {
	event := new(BMCBTPEvent)
	if err := _BMC.contract.UnpackLog(event, "BTPEvent", log); err != nil {
		return nil, err
	}
	event.Raw = log
	return event, nil
}

// BMCClaimRewardIterator is returned from FilterClaimReward and is used to iterate over the raw logs and unpacked data for ClaimReward events raised by the BMC contract.
type BMCClaimRewardIterator struct {
	Event *BMCClaimReward // Event containing the contract specifics and raw log

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
func (it *BMCClaimRewardIterator) Next() bool {
	// If the iterator failed, stop iterating
	if it.fail != nil {
		return false
	}
	// If the iterator completed, deliver directly whatever's available
	if it.done {
		select {
		case log := <-it.logs:
			it.Event = new(BMCClaimReward)
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
		it.Event = new(BMCClaimReward)
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
func (it *BMCClaimRewardIterator) Error() error {
	return it.fail
}

// Close terminates the iteration process, releasing any pending underlying
// resources.
func (it *BMCClaimRewardIterator) Close() error {
	it.sub.Unsubscribe()
	return nil
}

// BMCClaimReward represents a ClaimReward event raised by the BMC contract.
type BMCClaimReward struct {
	Sender   common.Address
	Network  string
	Receiver string
	Amount   *big.Int
	Nsn      *big.Int
	Raw      types.Log // Blockchain specific contextual infos
}

// FilterClaimReward is a free log retrieval operation binding the contract event 0x92a1bf3ab6e4839bb58db5f99ae98936355e94c1223779807563d1a08a3bf449.
//
// Solidity: event ClaimReward(address _sender, string _network, string _receiver, uint256 _amount, int256 _nsn)
func (_BMC *BMCFilterer) FilterClaimReward(opts *bind.FilterOpts) (*BMCClaimRewardIterator, error) {

	logs, sub, err := _BMC.contract.FilterLogs(opts, "ClaimReward")
	if err != nil {
		return nil, err
	}
	return &BMCClaimRewardIterator{contract: _BMC.contract, event: "ClaimReward", logs: logs, sub: sub}, nil
}

// WatchClaimReward is a free log subscription operation binding the contract event 0x92a1bf3ab6e4839bb58db5f99ae98936355e94c1223779807563d1a08a3bf449.
//
// Solidity: event ClaimReward(address _sender, string _network, string _receiver, uint256 _amount, int256 _nsn)
func (_BMC *BMCFilterer) WatchClaimReward(opts *bind.WatchOpts, sink chan<- *BMCClaimReward) (event.Subscription, error) {

	logs, sub, err := _BMC.contract.WatchLogs(opts, "ClaimReward")
	if err != nil {
		return nil, err
	}
	return event.NewSubscription(func(quit <-chan struct{}) error {
		defer sub.Unsubscribe()
		for {
			select {
			case log := <-logs:
				// New log arrived, parse the event and forward to the user
				event := new(BMCClaimReward)
				if err := _BMC.contract.UnpackLog(event, "ClaimReward", log); err != nil {
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

// ParseClaimReward is a log parse operation binding the contract event 0x92a1bf3ab6e4839bb58db5f99ae98936355e94c1223779807563d1a08a3bf449.
//
// Solidity: event ClaimReward(address _sender, string _network, string _receiver, uint256 _amount, int256 _nsn)
func (_BMC *BMCFilterer) ParseClaimReward(log types.Log) (*BMCClaimReward, error) {
	event := new(BMCClaimReward)
	if err := _BMC.contract.UnpackLog(event, "ClaimReward", log); err != nil {
		return nil, err
	}
	event.Raw = log
	return event, nil
}

// BMCClaimRewardResultIterator is returned from FilterClaimRewardResult and is used to iterate over the raw logs and unpacked data for ClaimRewardResult events raised by the BMC contract.
type BMCClaimRewardResultIterator struct {
	Event *BMCClaimRewardResult // Event containing the contract specifics and raw log

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
func (it *BMCClaimRewardResultIterator) Next() bool {
	// If the iterator failed, stop iterating
	if it.fail != nil {
		return false
	}
	// If the iterator completed, deliver directly whatever's available
	if it.done {
		select {
		case log := <-it.logs:
			it.Event = new(BMCClaimRewardResult)
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
		it.Event = new(BMCClaimRewardResult)
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
func (it *BMCClaimRewardResultIterator) Error() error {
	return it.fail
}

// Close terminates the iteration process, releasing any pending underlying
// resources.
func (it *BMCClaimRewardResultIterator) Close() error {
	it.sub.Unsubscribe()
	return nil
}

// BMCClaimRewardResult represents a ClaimRewardResult event raised by the BMC contract.
type BMCClaimRewardResult struct {
	Sender  common.Address
	Network string
	Nsn     *big.Int
	Result  *big.Int
	Raw     types.Log // Blockchain specific contextual infos
}

// FilterClaimRewardResult is a free log retrieval operation binding the contract event 0xe4a536fde1966a83411167748abc5fbd80e0247bffe941ca4ddb5079fd730636.
//
// Solidity: event ClaimRewardResult(address _sender, string _network, int256 _nsn, uint256 _result)
func (_BMC *BMCFilterer) FilterClaimRewardResult(opts *bind.FilterOpts) (*BMCClaimRewardResultIterator, error) {

	logs, sub, err := _BMC.contract.FilterLogs(opts, "ClaimRewardResult")
	if err != nil {
		return nil, err
	}
	return &BMCClaimRewardResultIterator{contract: _BMC.contract, event: "ClaimRewardResult", logs: logs, sub: sub}, nil
}

// WatchClaimRewardResult is a free log subscription operation binding the contract event 0xe4a536fde1966a83411167748abc5fbd80e0247bffe941ca4ddb5079fd730636.
//
// Solidity: event ClaimRewardResult(address _sender, string _network, int256 _nsn, uint256 _result)
func (_BMC *BMCFilterer) WatchClaimRewardResult(opts *bind.WatchOpts, sink chan<- *BMCClaimRewardResult) (event.Subscription, error) {

	logs, sub, err := _BMC.contract.WatchLogs(opts, "ClaimRewardResult")
	if err != nil {
		return nil, err
	}
	return event.NewSubscription(func(quit <-chan struct{}) error {
		defer sub.Unsubscribe()
		for {
			select {
			case log := <-logs:
				// New log arrived, parse the event and forward to the user
				event := new(BMCClaimRewardResult)
				if err := _BMC.contract.UnpackLog(event, "ClaimRewardResult", log); err != nil {
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

// ParseClaimRewardResult is a log parse operation binding the contract event 0xe4a536fde1966a83411167748abc5fbd80e0247bffe941ca4ddb5079fd730636.
//
// Solidity: event ClaimRewardResult(address _sender, string _network, int256 _nsn, uint256 _result)
func (_BMC *BMCFilterer) ParseClaimRewardResult(log types.Log) (*BMCClaimRewardResult, error) {
	event := new(BMCClaimRewardResult)
	if err := _BMC.contract.UnpackLog(event, "ClaimRewardResult", log); err != nil {
		return nil, err
	}
	event.Raw = log
	return event, nil
}

// BMCInitializedIterator is returned from FilterInitialized and is used to iterate over the raw logs and unpacked data for Initialized events raised by the BMC contract.
type BMCInitializedIterator struct {
	Event *BMCInitialized // Event containing the contract specifics and raw log

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
func (it *BMCInitializedIterator) Next() bool {
	// If the iterator failed, stop iterating
	if it.fail != nil {
		return false
	}
	// If the iterator completed, deliver directly whatever's available
	if it.done {
		select {
		case log := <-it.logs:
			it.Event = new(BMCInitialized)
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
		it.Event = new(BMCInitialized)
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
func (it *BMCInitializedIterator) Error() error {
	return it.fail
}

// Close terminates the iteration process, releasing any pending underlying
// resources.
func (it *BMCInitializedIterator) Close() error {
	it.sub.Unsubscribe()
	return nil
}

// BMCInitialized represents a Initialized event raised by the BMC contract.
type BMCInitialized struct {
	Version uint8
	Raw     types.Log // Blockchain specific contextual infos
}

// FilterInitialized is a free log retrieval operation binding the contract event 0x7f26b83ff96e1f2b6a682f133852f6798a09c465da95921460cefb3847402498.
//
// Solidity: event Initialized(uint8 version)
func (_BMC *BMCFilterer) FilterInitialized(opts *bind.FilterOpts) (*BMCInitializedIterator, error) {

	logs, sub, err := _BMC.contract.FilterLogs(opts, "Initialized")
	if err != nil {
		return nil, err
	}
	return &BMCInitializedIterator{contract: _BMC.contract, event: "Initialized", logs: logs, sub: sub}, nil
}

// WatchInitialized is a free log subscription operation binding the contract event 0x7f26b83ff96e1f2b6a682f133852f6798a09c465da95921460cefb3847402498.
//
// Solidity: event Initialized(uint8 version)
func (_BMC *BMCFilterer) WatchInitialized(opts *bind.WatchOpts, sink chan<- *BMCInitialized) (event.Subscription, error) {

	logs, sub, err := _BMC.contract.WatchLogs(opts, "Initialized")
	if err != nil {
		return nil, err
	}
	return event.NewSubscription(func(quit <-chan struct{}) error {
		defer sub.Unsubscribe()
		for {
			select {
			case log := <-logs:
				// New log arrived, parse the event and forward to the user
				event := new(BMCInitialized)
				if err := _BMC.contract.UnpackLog(event, "Initialized", log); err != nil {
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

// ParseInitialized is a log parse operation binding the contract event 0x7f26b83ff96e1f2b6a682f133852f6798a09c465da95921460cefb3847402498.
//
// Solidity: event Initialized(uint8 version)
func (_BMC *BMCFilterer) ParseInitialized(log types.Log) (*BMCInitialized, error) {
	event := new(BMCInitialized)
	if err := _BMC.contract.UnpackLog(event, "Initialized", log); err != nil {
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

// BMCMessageDroppedIterator is returned from FilterMessageDropped and is used to iterate over the raw logs and unpacked data for MessageDropped events raised by the BMC contract.
type BMCMessageDroppedIterator struct {
	Event *BMCMessageDropped // Event containing the contract specifics and raw log

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
func (it *BMCMessageDroppedIterator) Next() bool {
	// If the iterator failed, stop iterating
	if it.fail != nil {
		return false
	}
	// If the iterator completed, deliver directly whatever's available
	if it.done {
		select {
		case log := <-it.logs:
			it.Event = new(BMCMessageDropped)
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
		it.Event = new(BMCMessageDropped)
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
func (it *BMCMessageDroppedIterator) Error() error {
	return it.fail
}

// Close terminates the iteration process, releasing any pending underlying
// resources.
func (it *BMCMessageDroppedIterator) Close() error {
	it.sub.Unsubscribe()
	return nil
}

// BMCMessageDropped represents a MessageDropped event raised by the BMC contract.
type BMCMessageDropped struct {
	Prev  string
	Seq   *big.Int
	Msg   []byte
	Ecode *big.Int
	Emsg  string
	Raw   types.Log // Blockchain specific contextual infos
}

// FilterMessageDropped is a free log retrieval operation binding the contract event 0x35348898083dc8da7eb4e604320d7cc250732cc1f584d2596f670b5d6bab8de5.
//
// Solidity: event MessageDropped(string _prev, uint256 _seq, bytes _msg, uint256 _ecode, string _emsg)
func (_BMC *BMCFilterer) FilterMessageDropped(opts *bind.FilterOpts) (*BMCMessageDroppedIterator, error) {

	logs, sub, err := _BMC.contract.FilterLogs(opts, "MessageDropped")
	if err != nil {
		return nil, err
	}
	return &BMCMessageDroppedIterator{contract: _BMC.contract, event: "MessageDropped", logs: logs, sub: sub}, nil
}

// WatchMessageDropped is a free log subscription operation binding the contract event 0x35348898083dc8da7eb4e604320d7cc250732cc1f584d2596f670b5d6bab8de5.
//
// Solidity: event MessageDropped(string _prev, uint256 _seq, bytes _msg, uint256 _ecode, string _emsg)
func (_BMC *BMCFilterer) WatchMessageDropped(opts *bind.WatchOpts, sink chan<- *BMCMessageDropped) (event.Subscription, error) {

	logs, sub, err := _BMC.contract.WatchLogs(opts, "MessageDropped")
	if err != nil {
		return nil, err
	}
	return event.NewSubscription(func(quit <-chan struct{}) error {
		defer sub.Unsubscribe()
		for {
			select {
			case log := <-logs:
				// New log arrived, parse the event and forward to the user
				event := new(BMCMessageDropped)
				if err := _BMC.contract.UnpackLog(event, "MessageDropped", log); err != nil {
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

// ParseMessageDropped is a log parse operation binding the contract event 0x35348898083dc8da7eb4e604320d7cc250732cc1f584d2596f670b5d6bab8de5.
//
// Solidity: event MessageDropped(string _prev, uint256 _seq, bytes _msg, uint256 _ecode, string _emsg)
func (_BMC *BMCFilterer) ParseMessageDropped(log types.Log) (*BMCMessageDropped, error) {
	event := new(BMCMessageDropped)
	if err := _BMC.contract.UnpackLog(event, "MessageDropped", log); err != nil {
		return nil, err
	}
	event.Raw = log
	return event, nil
}
