#!/bin/sh
source env.variables.sh
source nativeCoin.solidity.sh
source nativeCoin.javascore.sh

TOKENS_TRANSFER_AMOUNT=${1:-1}
NATIVE_COIN_NAME="BNB"
#Check Alice's balance before deposit
printf "\n\nStep 1: Alice's BNB balance before BTP Transfer\n"
get_alice_native_balance $NATIVE_COIN_NAME

#initiate Transfer from BSC to ICON from BSH
printf "\n\nStep 2: BOB Initiates BTP Native coin transfer of $TOKENS_TRANSFER_AMOUNT ($NATIVE_COIN_NAME) to Alice\n"
bsc_init_native_btp_transfer "$TOKENS_TRANSFER_AMOUNT" >$CONFIG_DIR/tx.native.bsc_icon.transfer

#Check alice balance after 20s
printf "\n\nStep 4: Alice ETH Balance after BTP token transfer\n"
check_alice_native_balance_with_wait $NATIVE_COIN_NAME
