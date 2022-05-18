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

package foundation.icon.btp.bmv.btp;

import foundation.icon.btp.lib.BMV;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.score.util.Logger;
import score.Context;
import score.VarDB;
import score.annotation.External;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class BTPMessageVerifier implements BMV {
    private static final Logger logger = Logger.getLogger(BTPMessageVerifier.class);
    private static String HASH = "keccak-256";
    private static String SIGNATURE_ALG = "ecdsa-secp256k1";
    private static VarDB<ProofContext> proofContextVarDB;

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, String _msg) {
        RelayMessage relayMessages = RelayMessage.fromBytes(_msg.getBytes(StandardCharsets.UTF_8));
        RelayMessage.TypePrefixedMessage[] typePrefixedMessages = relayMessages.getMessages();
        for (RelayMessage.TypePrefixedMessage message : typePrefixedMessages) {
            Object msg = message.getMessage();
            if (msg instanceof BlockUpdate) {
                blockUpdate((BlockUpdate) msg);
            } else if (msg instanceof MessageProof) {
                proveMessage((MessageProof) msg);
            }
        }
        return new byte[0][];
    }

    @External(readonly = true)
    public BMVStatus getStatus() {
        return null;
    }

    private void blockUpdate(BlockUpdate blockUpdate) {

    }

    private boolean verifyBlock(NetworkTypeSectionDecision decision, Proofs proofs) {
        byte[] decisionHash = decision.getNetworkTypeSectionHash();
        byte[][] sigs = proofs.getProofs();
        List<Address> verifiedValidator = new ArrayList<>();
        ProofContext proofContext = proofContextVarDB.get();
        for (byte[] sig : sigs) {
            Address address = recoverAddress(decisionHash, sig, true);
            Context.require(proofContext.isValidator(address), "invalid validator : " + address);
            Context.require(!verifiedValidator.contains(address), "duplicated validator : " + address);
            verifiedValidator.add(address);
        }
        var verified = verifiedValidator.size();
        var validatorsCnt = proofContext.getValidators().length;
        //quorum = validator * 2/3
        return verified * 3 >= validatorsCnt * 2;
    }

    private void proveMessage(MessageProof messageProof) {}

    static byte[] hash(byte[] msg) {
        return Context.hash(HASH, msg);
    }

    static Address recoverAddress(byte[] msg, byte[] sig, boolean compressed) {
        byte[] publicKey = Context.recoverKey(SIGNATURE_ALG, msg, sig, compressed);
        int sliceLen = publicKey.length - 1;
        byte[] sliced = new byte[sliceLen];
        System.arraycopy(publicKey, 1, sliced, 0, sliceLen);
        byte[] hashed = hash(sliced);
        byte[] addr = new byte[20];
        System.arraycopy(hashed, 12, addr, 0, 20);
        return new Address(addr);
    }
}
