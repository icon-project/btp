# Known issues

1. Don't use multiple BMRs setup, when BMV height not up to date with the network.

    When testing on testnet of Moonbeam (Moonbase alpha) the limit of ICON blocks per transactions is 11 blocks. And to keep the BMV up to date with BTP testnet of ICON, we send a lot of transactions. The problem is other BMR don't know when the other BMR transaction confirms and they use the old state of BMV MTA height to build and send transactions, which cause BMVRevertInvalidBlockUpdateLower. So, we recommend on use multiple BMRs setup with up-to-date BMV height, to limit the error rate
   
2. The transaction sending to EVM might be failed if there are a lot of BTP messages in one ICON block because the limit of gas

    Because some of RelayMessages can be quite hungry for EVM gas consumption, an [example](https://moonbase-blockscout.testnet.moonbeam.network/tx/0x60a5c624ddde0bbed802c38914491c94e645fa67c4333f651f1db0013bb7825d/internal-transactions) of it reaches limit of gas

3. The missed BTP message couldn't recover in case the missed BTP msg in the block which already behind the block MTA offset of MTA

    Because BMV can verify BlockHeader in the range of BMV MTA offset and BMV MTA height. The solution is BMV must be redeployed

4. User must edit offset in config when BMV MTA offset changes when restart

    Because BMR currently get the offset from config in the initialized phase of BMR. This could be fixed to flush all data and get offset directly on BMV MTA offset

5. Local network setup in docker-compose/goloop2moonbeam is not the same as public networks

    Because the setup of relaychain and parachain locally of moonbeam requires complex setup. [Work in progress](https://github.com/icon-project/btp/pull/89)
   
6. BMR `common/jsonrpc/client.go Client` not recover after receive HTTP error

    It is recommend to run BMR at recovery mode with script like

    ```shell
    #!/bin/bash
    while ! bin/btpsimple start --config /home/ubuntu/btp/.config/icon2pra.config.json # or pra2icon.config.json
    do
    sleep 0.1
    echo "Restarting program..."
    done
    ```
    