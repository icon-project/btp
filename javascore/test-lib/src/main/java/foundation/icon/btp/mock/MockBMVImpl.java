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

package foundation.icon.btp.mock;

import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.BTPException;
import foundation.icon.score.util.Logger;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;

public class MockBMVImpl implements MockBMV {
    private static final Logger logger = Logger.getLogger(MockBMVImpl.class);

    private final VarDB<MockBMVProperties> properties = Context.newVarDB("properties", MockBMVProperties.class);

    public MockBMVImpl() {
        MockBMVProperties properties = getProperties();
        if (properties == null) {
            setProperties(MockBMVProperties.DEFAULT);
        }
    }

    public MockBMVProperties getProperties() {
        return properties.get();
    }

    public void setProperties(MockBMVProperties properties) {
        this.properties.set(properties);
    }

    @External
    public void setHeight(long _height) {
        MockBMVProperties properties = getProperties();
        properties.setHeight(_height);
        setProperties(properties);
    }

    @External
    public void setOffset(long _offset) {
        MockBMVProperties properties = getProperties();
        properties.setOffset(_offset);
        setProperties(properties);
    }

    @External
    public void setLast_height(long _last_height) {
        MockBMVProperties properties = getProperties();
        properties.setLast_height(_last_height);
        setProperties(properties);
    }

    @EventLog
    public void HandleRelayMessage(byte[] _ret) {}

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, String _msg) {
        MockRelayMessage relayMessage = MockRelayMessage.fromBase64String(_msg);
        Integer revertCode = relayMessage.getRevertCode();
        if (revertCode != null) {
            String revertMsg = relayMessage.getRevertMessage();
            throw new BTPException.BMV(revertCode,
                    revertMsg == null ? revertCode.toString() : revertMsg);
        } else {
            MockBMVProperties properties = getProperties();
            boolean isUpdateProperties = false;
            Long offset = relayMessage.getOffset();
            if (offset != null && offset != properties.getOffset()) {
                logger.println("handleRelayMessage", "offset", properties.getOffset(), offset);
                properties.setOffset(offset);
                isUpdateProperties = true;
            }
            Long height = relayMessage.getHeight();
            if (height != null && height != properties.getHeight()) {
                logger.println("handleRelayMessage", "height", properties.getHeight(), height);
                properties.setHeight(height);
                isUpdateProperties = true;
            }
            Long lastHeight = relayMessage.getLastHeight();
            if (lastHeight != null && lastHeight != properties.getLast_height()) {
                logger.println("handleRelayMessage", "lastHeight", properties.getLast_height(), lastHeight);
                properties.setLast_height(lastHeight);
                isUpdateProperties = true;
            }
            if (isUpdateProperties) {
                setProperties(properties);
            }

            byte[][] ret = relayMessage.getBtpMessages();
            logger.println("handleRelayMessage", "btpMessages",
                    ret == null ? "null" : Integer.toString(ret.length));
            ret = ret == null ? new byte[0][] : ret;
            HandleRelayMessage(MockRelayMessage.toBytes(ret));
            return ret;
        }
    }

    @External(readonly = true)
    public BMVStatus getStatus() {
        return getProperties();
    }
}
