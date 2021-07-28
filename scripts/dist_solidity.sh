# /bin/sh
JAVA_VERSION=11.0.11
GRADLE_VERSION=6.7.1

install_javasdk() {
    echo "Installing... JAVASDK"
    curl -s "https://get.sdkman.io" | bash
    source $HOME/.sdkman/bin/sdkman-init.sh
    sdk install java 11.0.11.hs-adpt
    sdk install gradle 6.7.1
}

current_java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
if [[ "$current_java_version" != "$JAVA_VERSION" ]]; then
    install_javasdk
fi

current_gradle_version=$(gradle -v 2>&1 | awk '/Gradle/ {print $2}')
if [[ "$current_gradle_version" != "$GRADLE_VERSION" ]]; then
    echo "required gradle version $GRADLE_VERSION current $current_gradle_version"
    exit 0
fi

CONTRACTS_DIST_DIR=${CONTRACTS_DIST_DIR:-$PWD/build/contracts}
SOLIDITY_DIST_DIR=${SOLIDITY_DIST_DIR:-$CONTRACTS_DIST_DIR/solidity}

rm -rf $SOLIDITY_DIST_DIR
mkdir -p $SOLIDITY_DIST_DIR
cp -r solidity/bmc $SOLIDITY_DIST_DIR/
cp -r solidity/bmv $SOLIDITY_DIST_DIR/
cp -r solidity/bsh $SOLIDITY_DIST_DIR/

### cleaning
rm -rf $SOLIDITY_DIST_DIR/bmc/node_modules $SOLIDITY_DIST_DIR/bmc/test
rm -rf $SOLIDITY_DIST_DIR/bmv/node_modules $SOLIDITY_DIST_DIR/bmv/test
rm -rf $SOLIDITY_DIST_DIR/bsh/node_modules $SOLIDITY_DIST_DIR/bsh/test