# Arbitrary Call Service

The reference implementation of the arbitrary call service (a.k.a. `xcall`) specification between ICON to ICON.

## How to test

Assuming the [gochain-local](https://github.com/icon-project/gochain-local) docker container is running,
you can start integration testing with the following command.

```
$ ./gradlew clean xcall:test -PintegrationTest=true \
    -Pscore-test.default.keyStore=<path_to_god_wallet_json> -Pscore-test.default.keyPassword=gochain
```
