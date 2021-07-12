## FeeAggregation


### Requirements
- Install [ICON Local Node](https://github.com/icon-project/goloop/blob/master/doc/gochain_icon_local_node_guide.md)

### Build
```
$ ./gradlew build
$ ./gradlew optimizedJar
```
### Deploy contract
```
./gradlew deployToLocal -PkeystoreName=<your_wallet_json> -PkeystorePass=<password> 
```

### Tests
```
$ ./gradlew test
```

### Integration Tests
```
$ ./gradlew integrationTest
```

