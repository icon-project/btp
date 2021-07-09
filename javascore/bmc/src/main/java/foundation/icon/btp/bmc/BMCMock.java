package foundation.icon.btp.bmc;

import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.External;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

public class BMCMock {
    private final DictDB<String, Address> bshServices = Context.newDictDB("bshService", Address.class);

    public BMCMock() {
        List<String> assets = new ArrayList<String>();
        assets.add("Hello");
    }
    @External
    public void addService(String _svc, Address _addr) {
        bshServices.set(_svc, _addr);
    }

    @External
    public void sendMessage(String _to, String _svc, BigInteger _sn, byte[] _msg) {

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