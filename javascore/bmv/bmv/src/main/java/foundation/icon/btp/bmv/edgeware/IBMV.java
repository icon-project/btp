package foundation.icon.btp.bmv.edgeware;

import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.mta.MTAStatus;

import score.Address;

public interface IBMV {
    /**
     * get encode of merkle tree accumulator
     */
    String mta();

    /**
     * get address of bmc
     */
    Address bmc();

    /**
     * net address that bmv verify
     */
    String netAddress();

    /**
     * list addresses of validator
     */
    String validators();

    /**
     * get status of BMV
     */
    BMVStatus getStatus();

    /**
     * handle verify message from BMC and return list of event message
     */
    String[] handleRelayMessage(String bmc, String prev, long seq, String msg);
}
