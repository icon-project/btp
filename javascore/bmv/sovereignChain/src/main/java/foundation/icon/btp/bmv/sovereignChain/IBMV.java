package foundation.icon.btp.bmv.sovereignChain;

import foundation.icon.btp.lib.BMVStatus;

import score.Address;

import java.util.List;
import java.math.BigInteger;

public interface IBMV {
    /**
     * get encode of merkle tree accumulator
     */
    String mta();

    /**
     * get current mta height
     */
    long mtaHeight();

    /**
     * get current mta height
     */
    List<byte[]> mtaRoot();

    /**
     * get last block hash
     */
    byte[] mtaLastBlockHash();

    /**
     * get current mta offset
     */
    long mtaOffset();

    /**
     * get current mta caches
     */
    List<byte[]> mtaCaches();

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
    List<byte[]> validators();

    /**
     * get status of BMV
     */
    BMVStatus getStatus();

    /**
     * get address of eventDecoder
     */
    Address eventDecoder();

    /**
     * last height
     */
    long lastHeight();

    /**
     * current set id
     */
    BigInteger setId();

    /**
     * handle verify message from BMC and return list of event message
     */
    List<byte[]> handleRelayMessage(String bmc, String prev, BigInteger seq, String msg);
}
