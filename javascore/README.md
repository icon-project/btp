# ICON BTP BSC

ICON Blockchain Transmission Protocol for Binance Smart Chain (WIP)

## Requirements

- [ICON javaee](https://github.com/icon-project/goloop/tree/master/javaee)
- [ICON sdk](https://github.com/icon-project/goloop/tree/master/sdk/java) 
- [ICON goloop](https://github.com/icon-project/goloop) (for running the integration tests).
#### Obtain the goloop repo
```
$ git clone git@github.com:icon-project/goloop.git
$ GOLOOP_ROOT=/path/to/goloop
```

## Build
##### Dependencies
This project currently depends on building local maven snapshot for the ICON Java SCORE. 
###### Build & publish javaee
1. goto the api folder
    ``` cd ${GOLOOP_ROOT}/javaee/ ```
2. run 
    ``` ./gradlew publishToMavenLocal -x:signMavenJavaPublication ```

###### Build & publish sdk
1. goto the icon-sdk folder
    ``` cd ${GOLOOP_ROOT}/sdk/java ```
2. run 
    ``` ./gradlew publishToMavenLocal -x:signMavenJavaPublication ```

##### Build bmv and bsh
``` ./gradlew bmv:build ```

## Run Integration Tests with deployment using integration test cases
Follow local gochain setup guide:
[gochain_icon_local_node_guide](https://github.com/icon-project/goloop/blob/master/doc/gochain_icon_local_node_guide.md)

From the integration-tests project, run the following:

``` ./gradlew testJavaScore -DNO_SERVER=true -DCHAIN_ENV=./data/env.properties ```

For a specific test, use --tests <testname>

``` ./gradlew <project_name>:testJavaScore -DNO_SERVER=true --tests MTATest -DCHAIN_ENV=./data/env.properties ```


### Deployment in a local node using scripts & integration test for local node

Run integration tests from local deployment:

steps:

1. clean & create optimizeJar of BMC, BSH, BMV

``` gradle <project_name>:clean ```

``` gradle <project_name>:optimizedJar```

2. run ```deploy-script.sh deployToLocal```
4. substitute the BMC score address, BMV score address & BMV score deploy Txn address from the output of above script at setup() method in BMVLocalTest.java file
5. Also pass appropriate keystore file & password in setup() method in BMVLocalTest.java file
3. run BMV local Test from integration test
   ``` gradle bsh:testJavaScore -DNO_SERVER=true --tests BMVLocalTest -DCHAIN_ENV=./data/env.properties -PkeystoreName=keystore -PkeystorePass=Admin@123```
   
Other commands:


BMC:

``` gradle bmc:deployToLocal -PkeystoreName=../keys/keystore_god.json -PkeystorePass=gochain ```

Deploying BMC.zip (replace with proper parameters):

``` goloop rpc --uri http://btp.net.solidwallet.io/api/v3 sendtx deploy bmc.zip \
    --key_store keystore --key_password Admin@123 \
    --nid 0x42 --step_limit 13610920001 \
    --content_type application/zip \
    --param _net="0x07.icon" ```
BMV:

``` gradle bmv:deployToLocal -DBMC_ADDRESS=<BMC_SCORE_ADDRESS> -PkeystoreName=../keys/keystore_god.json -PkeystorePass=gochain ```

BSH:

``` gradle bsh :deployToLocal -DBMC_ADDRESS=<BMC_SCORE_ADDRESS> -PkeystoreName=../keys/keystore_god.json -PkeystorePass=gochain ```

