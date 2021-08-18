# /bin/sh
set -e

JAVA_VERSION=11.0.11
GRADLE_VERSION=6.7.1

install_javasdk() {
    echo "Installing... JAVASDK"
    if [ ! -d "$HOME/.sdkman" ]; then
        curl -s "https://get.sdkman.io" | bash
    fi
    source $HOME/.sdkman/bin/sdkman-init.sh
    sdk install java 11.0.11.hs-adpt
    sdk default java 11.0.11.hs-adpt
    sdk install gradle 6.7.1
    sdk default gradle 6.7.1
}

current_java_version=$(java --version | awk '/openjdk/ {print $2}')
if [ "$current_java_version" != "$JAVA_VERSION" ]; then
    echo $current_java_version
    install_javasdk
fi

current_gradle_version=$(gradle -v | awk '/Gradle/ {print $2}')
if [ "$current_gradle_version" != "$GRADLE_VERSION" ]; then
    install_javasdk
fi

CONTRACTS_DIST_DIR=${CONTRACTS_DIST_DIR:-$PWD/build/contracts}
JAVASCORE_DIST_DIR=${JAVASCORE_DIST_DIR:-$CONTRACTS_DIST_DIR/javascore}
SOURCE_CODE_DIR=$JAVASCORE_DIST_DIR/sources

rm -rf $JAVASCORE_DIST_DIR && mkdir -p "$SOURCE_CODE_DIR"
cp -r javascore/* $SOURCE_CODE_DIR/

# Overwrite the current BMV by the BMV without relaychain
cd $SOURCE_CODE_DIR && \
    ls -d ./bmv/* | grep -v "./bmv/icon" | xargs rm -rf && \
    tar -xzf bmv_without_relaychain.tar.gz

### compiling
gradle --stop
cd $SOURCE_CODE_DIR/bmv/eventDecoder && gradle buildKusamaDecoder && gradle buildMoonriverDecoder
cd $SOURCE_CODE_DIR/bmv/parachain && gradle optimizedJar
cd $SOURCE_CODE_DIR/fee_aggregation && gradle optimizedJar
cd $SOURCE_CODE_DIR/lib && gradle build
cd $SOURCE_CODE_DIR/bmc && gradle optimizedJar
cd $SOURCE_CODE_DIR/nativecoin && gradle optimizedJar && gradle optimizedJarIRC31

### packing
cp -rf $SOURCE_CODE_DIR/bmv/helper ${JAVASCORE_DIST_DIR}/
cp $SOURCE_CODE_DIR/bmv/parachain/build/libs/parachain-BMV-optimized.jar ${JAVASCORE_DIST_DIR}/
cp $SOURCE_CODE_DIR/bmv/eventDecoder/build/libs/KusamaEventDecoder-optimized.jar ${JAVASCORE_DIST_DIR}/
cp $SOURCE_CODE_DIR/bmv/eventDecoder/build/libs/MoonriverEventDecoder-optimized.jar ${JAVASCORE_DIST_DIR}/
cp $SOURCE_CODE_DIR/fee_aggregation/build/libs/fee-aggregation-system-1.0-optimized.jar ${JAVASCORE_DIST_DIR}/
cp $SOURCE_CODE_DIR/bmc/build/libs/bmc-0.1.0-optimized.jar ${JAVASCORE_DIST_DIR}/
cp $SOURCE_CODE_DIR/nativecoin/build/libs/nativecoin-0.1.0-optimized.jar ${JAVASCORE_DIST_DIR}/
cp $SOURCE_CODE_DIR/nativecoin/build/libs/irc31-0.1.0-optimized.jar ${JAVASCORE_DIST_DIR}/

### cleaning
# rm -rf $SOURCE_CODE_DIR
