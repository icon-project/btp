
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
The provisioned btp-icon image already holds the script to initiate token transfer.
1. sh into the btp-icon container
 `docker exec -it btp-icon sh`
2. Create keystore for Alice to transfer funds from  `/btpsimple/config` dir

    	source /btpsimple/bin/keystore.sh 
    	ensure_key_store alice.ks.json alice.secret 
3. To check the BSC user balance before and after the token transfer, run,

		source /btpsimple/bin/provision.sh
		getBalance
4. From  `/btpsimple/config` dir 

      	 /btpsimple/bin/transfer_eth.sh
This should initate transfer `10 ETH` tokens from alice to address in bsc.ks.json of the binance smart chain & store the transaction json in  `tx.transfer` file

Check the balance of the BSC user using the getBalance after the script run to ensure the transfer completion.