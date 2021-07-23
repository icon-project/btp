# BMR (BTP Message Relay)

## Introduction

* Unidirectional relay
* Monitor BTP events  
* Gather proofs for the events

## Integrated blockchain
* [ICON](icon.md)
* [ICON Enterprise Edition](iconee.md)
* [Moonriver](moonriver.md)

## General
* [Build Guide](build.md): please notice that for BMR, we only need to build executables
* [Keystore](keystore.md)
* [PoC ICON-ICON Tutorial](tutorial.md)

## Quick start

If you already installed docker and docker-compose and want to take a quick view.   
Just run:
```bash
make run-docker
```

In docker environment, We have issolated a simple case of relaying between 2 relayers.
After starting docker successfully. Now you can make a test by simply run this command:
```bash
make run-test-scenario
```

## Simple run

* Example assumptions
  * ICON endpoint is `http://goloop:9080/api/v3/icon`
  * Moonriver endpoint are `wss://moonbeam:34008` and `http://moonbeam:34007`
  * ICON BMC BTP address: `btp://0x3.icon/cx8eb24849a7ceb16b8fa537f5a8b378c6af4a0247`
  * PRA BMC BTP address: `btp://0x501.pra/0x5b5B619E6A040EBCB620155E0aAAe89AfA45D090`

* To generate relay keystore: please follow [keystore](keystore.md).
* To get BMCStatusLink, to getStatus get offset and check relays list contains address of relay keystore
  1. dst chain is goloop/gochain: please run multiple instructions as below
    ```bash
      $ goloop rpc --uri http://goloop:9080/api/v3/icon \
          call --to $JAVA_SCORE_BMC_ADDRESS \
          --method getStatus \
          --param _link=$BTPSIMPLE_SRC_ADDRESS
      # Example
      $ goloop rpc --uri http://goloop:9080/api/v3/icon \
        call --to cx8eb24849a7ceb16b8fa537f5a8b378c6af4a0247 \
        --method getStatus \
        --param _link=btp://0x501.pra/0x5b5B619E6A040EBCB620155E0aAAe89AfA45D090
      {
        "block_interval_dst": "0x3e8",
        "block_interval_src": "0x7d0",
        "cur_height": "0x14d54",
        "delay_limit": "0x3",
        "max_agg": "0x10",
        "relay_idx": "0x0",
        "relays": [
          {
            "address": "hx2dbd4f999f0e6b3c017c029d569dd86950c23104",
            "block_count": "0xd00",
            "msg_count": "0x0"
          }
        ],
        "rotate_height": "0x137ea",
        "rotate_term": "0x8",
        "rx_height": "0xca",
        "rx_height_src": "0x16770",
        "rx_seq": "0x0",
        "tx_seq": "0x2",
        "verifier": {
          "height": "0x235fe",
          "last_height": "0x234a0",
          "offset": "0x234a0"
        }
      }

      $ echo $((0x234a0))
      144544
    ```
  2. dst chain is EVM: please run multiple instructions as below
  
    ```javascript
      $ make dist-sol-bmc
      $ cd ${BTP_PROJ_DIR}/solidity/bmc
      $ truffle console --network moonbeamlocal
      $ truffle(moonbeamlocal)> let bmcPeriphery = await BMCPeriphery.at("0x5b5B619E6A040EBCB620155E0aAAe89AfA45D090")
      $ truffle(moonbeamlocal)> await bmcPeriphery.getStatus("btp://0x3.icon/cx8eb24849a7ceb16b8fa537f5a8b378c6af4a0247")
      // Example of BMCStatusLink
      [
        '0',
        '26',
        [
          '3710',
          '3562',
          '490',
          '0x',
          heightMTA: '3710',
          offsetMTA: '3562',
          lastHeight: '490',
          extra: '0x'
        ],
        [
          [
            '0x3Cd0A705a2DC65e5b1E1205896BaA2be8A07c6e0',
            '3220',
            '0',
            addr: '0x3Cd0A705a2DC65e5b1E1205896BaA2be8A07c6e0',
            blockCount: '3220',
            msgCount: '0'
          ]
        ],
        '0',
        '94486',
        '15',
        '3',
        '5',
        '3710',
        '92401',
        '1000',
        '3000',
        '171550',
        rxSeq: '0',
        txSeq: '26',
        verifier: [
          '3710',
          '3562',
          '3710',
          '0x',
          heightMTA: '3710',
          offsetMTA: '3562', # Offset is here
          lastHeight: '3710',
          extra: '0x'
        ],
        relays: [
          [
            '0x3Cd0A705a2DC65e5b1E1205896BaA2be8A07c6e0',
            '3220',
            '0',
            addr: '0x3Cd0A705a2DC65e5b1E1205896BaA2be8A07c6e0',
            blockCount: '3220',
            msgCount: '0'
          ]
        ],
        relayIdx: '0',
        rotateHeight: '94486',
        rotateTerm: '15',
        delayLimit: '3',
        maxAggregation: '5',
        rxHeightSrc: '490',
        rxHeight: '92401',
        blockIntervalSrc: '1000',
        blockIntervalDst: '3000',
        currentHeight: '171550'
      ]
      ```

* To create a configuration file
  ```bash
    # Make sure btpsimple executable exist
    make btpsimple
    
    # Set btpsimple in path
    export PATH="$PATH:${PWD}/bin"
    # Make entrypoint executable
    chmod +x ./entrypoint.sh

    BTPSIMPLE_CONFIG=path/to/config/dst.config.json \
    BTPSIMPLE_SRC_ADDRESS=btp://0x3.icon/cx8eb24849a7ceb16b8fa537f5a8b378c6af4a0247 \
    BTPSIMPLE_SRC_ENDPOINT=http://goloop:9080/api/v3/icon \
    BTPSIMPLE_DST_ADDRESS=btp://0x501.pra/0x5b5B619E6A040EBCB620155E0aAAe89AfA45D090 \
    BTPSIMPLE_DST_ENDPOINT=wss://moonbeam:34008 \
    BTPSIMPLE_OFFSET=3562 \
    BTPSIMPLE_KEY_STORE=path/to/config/dst.ks.json \
    BTPSIMPLE_KEY_SECRET=path/to/config/dst.secret \
    BTPSIMPLE_LOG_WRITER_FILENAME=path/to/config/log/dst.log \
    ./entrypoint.sh
  ```
* To run a btpsimple
  ```bash
  bin/btpsimple start --config path/to/config/dst.config.json
  ```
## Management
* [btpsimple command line](btpsimple_cli.md)
{"mode":"full","isActive":false}
