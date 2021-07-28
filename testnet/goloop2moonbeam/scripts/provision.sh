export CONFIG_DIR=${CONFIG_DIR:-/btpsimple/config}
export SCRIPT_DIR=${SCRIPT_DIR:-/btpsimple/scripts}
export SOLIDITY_DIST_DIR=${SOLIDITY_DIST_DIR:-/btpsimple/contracts/solidity}
export JAVASCORE_DIST_DIR=${JAVASCORE_DIST_DIR:-/btpsimple/contracts/javascore}
export JAVASCORE_HELPER_DIR=${JAVASCORE_HELPER_DIR:-$JAVASCORE_DIST_DIR/helper}
export MOONBEAM_CHAIN_ID=1281 # https://github.com/PureStake/moonbeam#chain-ids
export MOONBEAM_RPC_URL=${MOONBEAM_RPC_URL:-'http://moonbeam:9933'}

source goloop_rpc.sh
rpcch

source deploy_javascore.sh
source deploy_solidity.sh

# deploy_javascore_bmc
# deploy_javascore_bmv

deploy_solidity_bmc
deploy_solidity_bsh
deploy_solidity_bmv
moonbeam_bmc_addVerifier
moonbeam_bmc_addLink
moonbeam_bmc_addService
moonbeam_bsh_registerCoin