#!/bin/sh
source env.variables.sh
source rpc.sh
source provision.sh
source nativeCoin.javascore.sh
source nativeCoin.solidity.sh

# ensure alice user keystore creation
printf "\n\nStep 1: creating/ensuring Alice keystore \n"
source /btpsimple/bin/keystore.sh
ensure_key_store alice.ks.json alice.secret

#deposit 100 to Alice

ICX_DEPOSIT_AMOUNT=$(coin2wei ${1:-10})
printf "\n\nStep 2: Deposit $(wei2coin $ICX_DEPOSIT_AMOUNT) ICX to Alice account\n"
deposit_ICX_for_Alice >$CONFIG_DIR/tx.native.alice.transfer

printf "\n\nStep 3 Bob's balance before BTP Native transfer \n"
get_bob_ICX_balance
echo "$BOB_BALANCE"

#transfer native 10 ICX from Alice to BSC BOB
ICX_TRANSER_AMOUNT=$(coin2wei ${2:-10})
printf "\n\nStep 4: Alice Initiates BTP Native transfer of $(wei2coin $ICX_TRANSER_AMOUNT) ICX to BOB \n"
rpcks alice.ks.json alice.secret
transfer_ICX_from_Alice_to_Bob $ICX_TRANSER_AMOUNT >$CONFIG_DIR/tx.native.icon_bsc.transfer
wait_for_file $CONFIG_DIR/tx.native.icon_bsc.transfer

#get Bob's balance after BTP transfer with wait
printf "\n\nStep 5: Bob's ICX balance after Deposit \n"
get_Bob_ICX_Balance_with_wait
