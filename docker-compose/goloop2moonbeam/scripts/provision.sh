source util.sh

provision() {  
    echo "start provisioning at: $(date)" > $PROVISION_STATUS_STARTED
    echo "provisioning..."


    source deploy_javascore.sh
    source deploy_solidity.sh

    # deploy contracts 
    deploy_javascore_bmc
    deploy_solidity_bmc
    deploy_solidity_bsh
    deploy_solidity_bmv

    deploy_javascore_bmv
    deploy_javascore_IRC31Token
    deploy_javascore_NativeCoinBSH
    deploy_javascore_FeeAggregation
    # ------------------------------

    # config contracts
    goloop_bmc_addVerifier
    goloop_bmc_addLink
    goloop_bmc_addService
    goloop_bmc_addRelay
    goloop_bmc_setFeeAggregator
    goloop_bsh_config_native_coin

    moonbeam_bmc_addVerifier
    moonbeam_bmc_addLink
    moonbeam_bmc_addService
    moonbeam_bsh_registerCoin
    # -------------------------------

    ## finalizing
    echo "finished provisioning at: $(date)" > $PROVISION_STATUS_DONE
    clean_solidity_build
    sleep 1
}

if [ ! -f "$PROVISION_STATUS_DONE" ]; then
    if [ ! -f "$PROVISION_STATUS_STARTED" ]; then
        provision
    else
        while [ ! -f $PROVISION_STATUS_DONE ];
        do
            echo "waiting for an other BTP to finish provisioning settings..."
            sleep 10
        done
    fi
fi
