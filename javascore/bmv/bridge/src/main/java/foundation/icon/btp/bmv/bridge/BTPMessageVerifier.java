/*
 * Copyright 2022 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package foundation.icon.btp.bmv.bridge;

import foundation.icon.btp.lib.BMV;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.lib.BTPException;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class BTPMessageVerifier implements BMV {
    public static final int REVERT_UNKNOWN = 0;
    public static final int REVERT_INVALID_SEQ_NUMBER = 1;

    private VarDB<Address> varBMCAddress = Context.newVarDB("bmcAddress", Address.class);
    private VarDB<String> varNetAddress = Context.newVarDB("netAddress", String.class);
    private VarDB<BigInteger> varHeight = Context.newVarDB("height", BigInteger.class);

    public BTPMessageVerifier(Address _bmc, String _net, BigInteger _height) {
        varBMCAddress.set(_bmc);
        varNetAddress.set(_net);
        varHeight.set(_height);
    }

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, byte[] _msg) {
        BTPAddress curAddr = BTPAddress.valueOf(_bmc);
        BTPAddress prevAddr = BTPAddress.valueOf(_prev);
        checkAccessible(curAddr, prevAddr);

        BigInteger next_seq = _seq.add(BigInteger.ONE);
        RelayMessage rm = RelayMessage.fromBytes(_msg);
        BigInteger height = varHeight.getOrDefault(BigInteger.ZERO);
        List<byte[]> msgs = new ArrayList<>();
        for (ReceiptProof rp : rm.getReceiptProofs()) {
            if (rp.getHeight().compareTo(height) < 0) {
                //ignore lower height
                continue;
            }
            height = rp.getHeight();
            for (EventDataBTPMessage ev : rp.getEvents()) {
                //skip compare ev.next_bmc == _bmc
                int compare = ev.getSeq().compareTo(next_seq);
                if (compare < 0) {
                    //ignore lower seq
                    continue;
                } else if (compare > 0) {//ev.seq > next
                    throw new BTPException.BMV(REVERT_INVALID_SEQ_NUMBER,
                            "invalid sequence "+ev.getSeq() + " expected:"+next_seq);
                }
                msgs.add(ev.getMsg());
                next_seq = next_seq.add(BigInteger.ONE);
            }
        }
        varHeight.set(height);
        byte[][] ret = new byte[msgs.size()][];
        int i = 0;
        for (byte[] msg : msgs) {
            ret[i] = msg;
        }
        return ret;
    }

    @External(readonly = true)
    public Map getStatus() {
        return Map.of("height", varHeight.get());
    }

    private void checkAccessible(BTPAddress curAddr, BTPAddress fromAddr) {
        Address bmcAddress = varBMCAddress.get();
        if (!varNetAddress.get().equals(fromAddr.net())) {
            throw new BTPException.BMV(REVERT_UNKNOWN,"not acceptable from");
        } else if (!Context.getCaller().equals(bmcAddress)) {
            throw new BTPException.BMV(REVERT_UNKNOWN, "not acceptable bmc");
        } else if (!Address.fromString(curAddr.account()).equals(bmcAddress)) {
            throw new BTPException.BMV(REVERT_UNKNOWN, "not acceptable bmc");
        }
    }
}
