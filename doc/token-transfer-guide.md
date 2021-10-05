
### Preparation
Follow the  [Binance Smart Chain BTP Guide (WIP)](https://github.com/icon-project/btp/blob/btp_web3labs/doc/bsc-guide.md#binance-smart-chain-btp-guide-wip "Binance Smart Chain BTP Guide (WIP)") to start docker network containing provisioned goloop, binance smart chain and BSC ICON BTP relayer.

Note:
* scripts files in `/goloop/bin`
* Transaction related files in `/goloop/config`
  	- Transaction hash : `tx.<method>.<chain>`
  	- SCORE Address : `<score>.<chain>`
  	- BTP Address : `btp.<chain>`, `net.btp.<chain>`

### ICON to BSC Token Transfer

###### Run using script:
The provisioned btp-icon image already holds the script to initiate token transfer & native coin transfer.
##### 1. Transfer Token(ETH) from ICON (Alice) -> BSC (BOB)
1. From the `devnet/docker/icon-bsc` directory run 

 		make transfer_eth
		
Note: This initates transfer of `10 ETH token units` from Alice(ICON) to address in BOB(bsc.ks.json)(BSC) & store the transaction json in `tx.token.icon_bsc.transfer` file.

The script should display "Bob Balance after BTP transfer" after a successful transfer. For more info, check [transfer_eth.sh](https://github.com/icon-project/btp/blob/btp_web3labs/devnet/docker/icon-bsc/scripts/transfer_eth.sh)

##### 2. Transfer Token(ICX) from ICON (Alice) -> BSC (BOB)
1. From the `devnet/docker/icon-bsc` directory run 

 		make transfer_icx
		
Note: This initates transfer of `10 ICX` coins from Alice(ICON) to address in BOB(bsc.ks.json)(BSC) & store the transaction json in `tx.native.icon_bsc.transfer` file.

The script should display "Bob's Balance after BTP Native transfer" after a successful transfer. For more info, check [transfer_icx.sh](https://github.com/icon-project/btp/blob/btp_web3labs/devnet/docker/icon-bsc/scripts/transfer_icx.sh)
