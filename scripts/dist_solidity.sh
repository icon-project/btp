# /bin/sh
set -e

CONTRACTS_DIST_DIR=${CONTRACTS_DIST_DIR:-$PWD/build/contracts}
SOLIDITY_DIST_DIR=${SOLIDITY_DIST_DIR:-$CONTRACTS_DIST_DIR/solidity}

rm -rf $SOLIDITY_DIST_DIR
mkdir -p $SOLIDITY_DIST_DIR
cp -r solidity/bmc $SOLIDITY_DIST_DIR/
cp -r solidity/bmv $SOLIDITY_DIST_DIR/
cp -r solidity/bsh $SOLIDITY_DIST_DIR/
cp -r solidity/nativecoinERC20 $SOLIDITY_DIST_DIR/

### cleaning
rm -rf $SOLIDITY_DIST_DIR/bmc/node_modules $SOLIDITY_DIST_DIR/bmc/test
rm -rf $SOLIDITY_DIST_DIR/bmv/node_modules $SOLIDITY_DIST_DIR/bmv/test
rm -rf $SOLIDITY_DIST_DIR/bsh/node_modules $SOLIDITY_DIST_DIR/bsh/test
rm -rf $SOLIDITY_DIST_DIR/nativecoinERC20/node_modules $SOLIDITY_DIST_DIR/nativecoinERC20/test