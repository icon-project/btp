#!/bin/sh
source env.variables.sh
source token.solidity.sh
source token.javascore.sh

TOKENS_TRANSFER_AMOUNT=${1:-1}

# ensure alice user keystore creation
printf "\n\nStep 1: creating/ensuring Alice keystore\n"
source /btpsimple/bin/keystore.sh
ensure_key_store alice.ks.json alice.secret

#transfer eth from owner bsc.ks.json to BOB bob.ks.json
printf "\n\nStep 2: Fund Bob with  $TOKENS_TRANSFER_AMOUNT ETH \n"
deposit_token_for_bob "$TOKENS_TRANSFER_AMOUNT"

printf "\n\nStep 3: Approve BSH to use Bob's funds for $TOKENS_TRANSFER_AMOUNT ETH \n"
token_approveTransfer "$TOKENS_TRANSFER_AMOUNT"

#Check Bobs's balance before deposit
printf "\n\nStep 4: Alice's ETH balance before BTP Transfer\n"
irc2_javascore_balance alice.ks.json

printf "\n\n Fees & amount Split \n"
calculateTransferFee "$TOKENS_TRANSFER_AMOUNT"
#initiate Transfer from BSC to ICON from BSH
printf "\n\nStep 5: BOB Initiates BTP token transfer of $TOKENS_TRANSFER_AMOUNT ETH to Alice\n"
bsc_init_btp_transfer "$TOKENS_TRANSFER_AMOUNT" >$CONFIG_DIR/tx.token.bsc_icon.transfer

#Check alice balance after 20s
printf "\n\nStep 6: Alice ETH Balance after BTP token transfer\n"
check_alice_token_balance_with_wait
