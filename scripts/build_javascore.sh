# /bin/sh


java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
if [[ "$java_version" != "11.0.11" ]]; then
    echo required java version 11.0.11 current "$java_version"
    exit 0
fi

gradle_version=$(gradle -v 2>&1 | awk '/Gradle/ {print $2}')
if [[ "$gradle_version" != "6.7.1" ]]; then
    echo required gradle version 6.7.1 current "$gradle_version"
    exit 0
fi


CONTRACTS_DIST_DIR=${CONTRACTS_DIST_DIR:-$PWD/build/contracts}
JAVASCORE_DIST_DIR=${JAVASCORE_DIST_DIR:-$CONTRACTS_DIST_DIR/javascore}
SOURCE_CODE_DIR=$JAVASCORE_DIST_DIR/sources

rm -rf $JAVASCORE_DIST_DIR
mkdir -p $SOURCE_CODE_DIR
cp -r javascore/* $SOURCE_CODE_DIR

cd $SOURCE_CODE_DIR && tar -xzf iconloop.tar && mv javascore iconloop

### compiling
cd $SOURCE_CODE_DIR/bmv/eventDecoder && gradle buildKusamaDecoder && gradle buildMoonriverDecoder
cd $SOURCE_CODE_DIR/bmv/parachain && gradle optimizedJar
cd $SOURCE_CODE_DIR/iconloop/bmc && gradle optimizedJar
cd $SOURCE_CODE_DIR/iconloop/nativecoin && gradle optimizedJar
cd $SOURCE_CODE_DIR/iconloop/javaee-tokens && gradle optimizedJar

### packing
cp -rf $SOURCE_CODE_DIR/bmv/helper ${JAVASCORE_DIST_DIR}/
cp $SOURCE_CODE_DIR/bmv/parachain/build/libs/parachain-BMV-optimized.jar ${JAVASCORE_DIST_DIR}/
cp $SOURCE_CODE_DIR/bmv/eventDecoder/build/libs/KusamaEventDecoder-optimized.jar ${JAVASCORE_DIST_DIR}/
cp $SOURCE_CODE_DIR/bmv/eventDecoder/build/libs/MoonriverEventDecoder-optimized.jar ${JAVASCORE_DIST_DIR}/
cp $SOURCE_CODE_DIR/iconloop/bmc/build/libs/bmc-0.1.0-debug.jar ${JAVASCORE_DIST_DIR}/
cp $SOURCE_CODE_DIR/iconloop/nativecoin/build/libs/nativecoin-0.1.0-debug.jar ${JAVASCORE_DIST_DIR}/
cp $SOURCE_CODE_DIR/iconloop/javaee-tokens/build/libs/irc31-0.1.0-debug.jar ${JAVASCORE_DIST_DIR}/

### cleaning
rm -rf $SOURCE_CODE_DIR
