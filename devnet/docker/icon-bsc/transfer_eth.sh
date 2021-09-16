#!/bin/sh

source /btpsimple/bin/provision.sh 
#TODO: temp remove this later, updating the seq number to match
#bsc_updateRxSeq

# ensure alice user keystore creation
source /btpsimple/bin/keystore.sh 
ensure_key_store alice.ks.json alice.secret 

#transfer 10 ETH from IRC2 token to Alice
irc2_javascore_transfer 0xa alice.ks.json

#transfer 10 ETH from Alice to BSH
source /btpsimple/bin/rpc.sh 
rpcks alice.ks.json alice.secret
irc2_javascore_transfer 0xa

#Check Alice's balance in BSH
bsh_javascore_balance alice.ks.json

#initiate Transfer from ICON to BSC from BSH
rpcks alice.ks.json alice.secret
bsh_javascore_transfer 0xa 0x$(cat bsc.ks.json | jq -r .address)  > $CONFIG_DIR/tx.transfer