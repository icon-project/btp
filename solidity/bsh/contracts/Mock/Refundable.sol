pragma solidity >=0.5.0 <0.8.0;

//  This contract does not have any method
//  that allows to receives coins, i.e. receive() external payable/fallback() external payable
//  Instead, it has a method deposit() payable to receive coins
contract Refundable {
    function deposit() external payable {}

    function transfer(address _bsh, string calldata _to, uint _amt) external {
        _bsh.call{value: _amt}(
            abi.encodeWithSignature("transfer(string)", _to)
        );
    }

    receive() external payable {}
}