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

## Run Integration Tests
Follow local gochain setup guide:
[gochain_icon_local_node_guide](https://github.com/icon-project/goloop/blob/master/doc/gochain_icon_local_node_guide.md)

From the integration-tests project, run the following:

``` ./gradlew testJavaScore -DNO_SERVER=true -DCHAIN_ENV=./data/env.properties ```

For a specific test, use --tests <testname>

``` ./gradlew testJavaScore -DNO_SERVER=true --tests MTATest -DCHAIN_ENV=./data/env.properties ```
