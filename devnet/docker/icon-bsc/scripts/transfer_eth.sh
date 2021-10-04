#!/bin/sh
source env.variables.sh
source token.javascore.sh
source token.solidity.sh
source rpc.sh
source provision.sh
source utils.sh
#TODO: temp remove this later, updating the seq number to match
#bsc_updateRxSeq

# ensure alice user keystore creation
printf "\n\nStep 1: creating/ensuring Alice keystore\n"
source /btpsimple/bin/keystore.sh
ensure_key_store alice.ks.json alice.secret

#transfer 10 ETH from IRC2 token to Alice#$(coin2wei ${1:-10})
printf "\n\nStep 2: Transfer 10 ETH Tokens to Alice\n"
TOKENS_DEPOSIT_AMOUNT=$(coin2wei ${1:-10})
echo $TOKENS_DEPOSIT_AMOUNT
irc2_javascore_transfer $TOKENS_DEPOSIT_AMOUNT alice.ks.json >$CONFIG_DIR/tx.token.alice.transfer

#transfer 10 ETH from Alice to BSH
printf "\n\nStep 3: Alice Deposits 10 ETH Tokens into BSH\n"
rpcks alice.ks.json alice.secret
irc2_javascore_transfer $TOKENS_DEPOSIT_AMOUNT >$CONFIG_DIR/tx.token.bsh.transfer

#Check Alice's balance in BSH
#printf "Step 4: Alice's BSH balance after Deposit"
#bsh_javascore_balance alice.ks.json

#Check Bobs's balance before deposit
printf "\n\nStep 4: Bob's ETH balance before Deposit\n"
get_Bob_Token_Balance
echo "$BOB_BALANCE"

#initiate Transfer from ICON to BSC from BSH
printf "\n\nStep 5: Alice Initiates BTP token transfer of 10 ETH to BOB\n"
rpcks alice.ks.json alice.secret
bsh_javascore_transfer $TOKENS_DEPOSIT_AMOUNT $(get_bob_address) >$CONFIG_DIR/tx.token.icon_bsc.transfer
wait_for_file $CONFIG_DIR/tx.token.icon_bsc.transfer

#get Bob's balance after BTP transfer with wait
printf "\n\nStep 6: Bob's ETH balance after Deposit\n"
get_Bob_Token_Balance_with_wait
