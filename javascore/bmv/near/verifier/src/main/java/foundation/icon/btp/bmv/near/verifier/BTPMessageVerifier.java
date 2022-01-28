/*
 * Copyright 2021 ICON Foundation
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

package foundation.icon.btp.bmv.near.verifier;

import foundation.icon.btp.bmv.near.verifier.types.BMVProperties;
import foundation.icon.btp.bmv.near.verifier.types.ItemList;
import foundation.icon.btp.bmv.near.verifier.types.MerkleTreeAccumulator;
import foundation.icon.btp.bmv.near.verifier.types.Validators;
import foundation.icon.btp.bmv.near.verifier.types.BlockProducer;
import foundation.icon.btp.lib.BMV;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.score.util.Logger;
import foundation.icon.score.util.StringUtil;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;
import scorex.util.ArrayList;
import scorex.util.Base64;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.near.borshj.Borsh;

public class BTPMessageVerifier implements BMV {
    private static final Logger logger = Logger.getLogger(BTPMessageVerifier.class);
    private final VarDB<BMVProperties> properties = Context.newVarDB("properties", BMVProperties.class);

    public BTPMessageVerifier(Address _bmc, String _net, String _validators, long _offset, Address _proofDecoder) {
        BMVProperties properties = getProperties();
        properties.setBmc(_bmc);
        properties.setNet(_net);
        Validators validators = Validators.fromString(_validators);
        properties.setValidators(validators);
        if (properties.getLastHeight() == 0) {
            properties.setLastHeight(_offset);
        }
        if (properties.getMta() == null) {
            MerkleTreeAccumulator mta = new MerkleTreeAccumulator();
            mta.setHeight(_offset);
            mta.setOffset(_offset);
            properties.setMta(mta);
        }
        properties.setProofDecoder(_proofDecoder);
        setProperties(properties);
    }

    public BMVProperties getProperties() {
        return properties.getOrDefault(BMVProperties.DEFAULT);
    }

    public void setProperties(BMVProperties properties) {
        this.properties.set(properties);
    }

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, String _msg) {
        // TODO Auto-generated method stub
        return null;
    }

    @External(readonly = true)
    public BMVStatus getStatus() {
        BMVProperties properties = getProperties();
        MerkleTreeAccumulator mta = properties.getMta();
        BMVStatus status = new BMVStatus();
        status.setOffset(mta.getOffset());
        status.setHeight(mta.getHeight());
        status.setLast_height(properties.getLastHeight());
        return status;
    }
}
