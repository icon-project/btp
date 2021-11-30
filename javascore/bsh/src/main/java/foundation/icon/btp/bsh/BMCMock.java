package foundation.icon.btp.bsh;

import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.External;

import java.math.BigInteger;

public class BMCMock {
    private final DictDB<String, Address> bshServices = Context.newDictDB("bshService", Address.class);

    public BMCMock() {

    }

    @External
    public void addService(String _svc, Address _addr) {
        bshServices.set(_svc, _addr);
    }

    @External
    public void sendMessage(String _to, String _svc, BigInteger _sn, byte[] _msg) {
        Address addr = bshServices.get(_svc);
        if (addr == null) {
            Context.revert("BSH doesnt exist");
        }
        if (!Context.getCaller().equals(addr)) {
            Context.revert("unauthorized");
        }
        if (_sn.compareTo(BigInteger.ZERO) < 1) {
            //Context.revert("invalid sn");
        }
    }

    @External
    public void handleBTPMessage(String from, String svc, BigInteger sn, byte[] msg) {
        Address _addr = bshServices.get(svc);
        Context.call(_addr, "handleBTPMessage", from, svc, sn, msg);
    }

    @External
    public void handleFeeGathering(String _fa, String _svc) {
        Address _addr = bshServices.get(_svc);
        Context.call(_addr, "handleFeeGathering", _fa, _svc);
    }
}