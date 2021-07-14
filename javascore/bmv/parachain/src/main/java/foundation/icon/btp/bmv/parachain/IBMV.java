package foundation.icon.btp.bmv.parachain;

import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.mta.MTAStatus;

import java.util.List;
import java.math.BigInteger;

import score.Address;

public interface IBMV {
    /**
     * get encode of merkle tree accumulator of para
     */
    String paraMta();

    /**
     * get encode of merkle tree accumulator of relay
     */
    String relayMta();

    /**
     * get current mta height of para
     */
    long paraMtaHeight();

    /**
     * get current mta height of relay
     */
    long relayMtaHeight();

    /**
     * get list of para MTA root
     */
    List<byte[]> paraMtaRoot();

    /**
     * get list of relay MTA root
     */
    List<byte[]> relayMtaRoot();

    /**
     * get last block hash of para
     */
    byte[] paraMtaLastBlockHash();

    /**
     * get last block hash of relay
     */
    byte[] relayMtaLastBlockHash();

    /**
     * get current mta offset of para
     */
    long paraMtaOffset();

    /**
     * get current mta offset of relay
     */
    long relayMtaOffset();

    /**
     * get current mta cache of para
     */
    List<byte[]> paraMtaCaches();

    /**
     * get current mta cache of relay
     */
    List<byte[]> relayMtaCaches();

    /**
     * get address of event decoder of para chain
     */
    Address paraEventDecoder();

    /**
     * get address of event decoder of relay chain
     */
    Address relayEventDecoder();

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
     * current validator set id
     */
    BigInteger setId();

    /**
     * get status of BMV
     */
    BMVStatus getStatus();

    /**
     * last height
     */
    long lastHeight();

    /**
     * handle verify message from BMC and return list of event message
     */
    List<byte[]> handleRelayMessage(String bmc, String prev, BigInteger seq, String msg);
}
