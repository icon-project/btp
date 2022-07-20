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

package foundation.icon.btp.bmc;

import score.Context;

public class RelayRotation {
    public static final int RELAY_ROTATION_NONE = 0;
    public static final int RELAY_ROTATION_DST_BASED = 1;
    public static final int RELAY_ROTATION_SRC_BASED = 2;
    //TODO define default max_aggregation
    public static final int DEFAULT_MAX_AGGREGATION = 10;
    //TODO define default delay_limit, if delay_limit < 3, too sensitive
    public static final int DEFAULT_DELAY_LIMIT = 3;

    public static final RelayRotation DEFAULT;
    static {
        DEFAULT = new RelayRotation();
        DEFAULT.policy = RELAY_ROTATION_NONE;
        DEFAULT.maxAggregation = DEFAULT_MAX_AGGREGATION;
        DEFAULT.delayLimit = DEFAULT_DELAY_LIMIT;
    }

    public static final long SCALE_PRECISION = 10 ^ 6;

    private int policy;
    private int maxAggregation;
    private int delayLimit;
    //guess only
    private long rotateTerm; //maxAggregation or maxAggregation/scale
    private long blockIntervalDst;
    private long scale;

    private int relayIdx;
    private long rotateHeight;
    //guess only
    private long rxHeight; //initialize with BMC.block_height
    private long rxHeightSrc; //initialize with BMV._offset

    /**
     * Divide and ceil
     *
     * @param x the dividend
     * @param y the divisor
     * @return ceil(x / y)
     */
    public static long ceilDiv(long x, long y) {
        return x % y == 0 ? x / y : x / y + 1;
    }

    static public int rotateCount(long duration, long term) {
        int count = (int) ceilDiv(duration, term);
        return count < 0 ? 0 : count;
    }

    public void rotate(long msgHeight, boolean hasMsg, int numOfRelays) {
        rotate(Context.getBlockHeight(), msgHeight, hasMsg, numOfRelays);
    }

    public void rotate(long currentHeight, long msgHeight, boolean hasMsg, int numOfRelays) {
        int rotateCnt;
        long baseHeight;
        switch (policy) {
            case RELAY_ROTATION_SRC_BASED:
                if (hasMsg) {
                    long guessHeight = rxHeight + ceilDiv((msgHeight - rxHeightSrc) * SCALE_PRECISION, scale) - 1;
                    if (guessHeight > currentHeight) {
                        guessHeight = currentHeight;
                    }
                    rotateCnt = rotateCount(guessHeight - rotateHeight, rotateTerm);
                    int skipCnt = rotateCount(currentHeight - guessHeight, delayLimit) - 1;
                    if (skipCnt > 0) {
                        rotateCnt += skipCnt;
                        baseHeight = currentHeight;
                    } else {
                        baseHeight = rotateHeight + ((rotateCnt - 1) * rotateTerm);
                    }
                    rxHeight = currentHeight;
                    rxHeightSrc = msgHeight;
                    break;
                }
            case RELAY_ROTATION_DST_BASED:
                rotateCnt = rotateCount(currentHeight - rotateHeight, rotateTerm);
                baseHeight = rotateHeight + ((rotateCnt - 1) * rotateTerm);
                break;
            case RELAY_ROTATION_NONE:
            default:
                return;
        }
        if (rotateCnt > 0) {
            rotateHeight = baseHeight + rotateTerm;
            relayIdx += rotateCnt;
            if (relayIdx >= numOfRelays) {
                relayIdx = relayIdx % numOfRelays;
            }
        }
    }

    public void reset(int blockIntervalSrc, int blockIntervalDst, int maxAggregation, int delayLimit) {
        policy = RELAY_ROTATION_NONE;
        int minDelayLimit = 0;
        if (blockIntervalDst < -1) {
            policy = RELAY_ROTATION_DST_BASED;
            rotateTerm = maxAggregation;
        } else if (blockIntervalDst > 0) {
            policy = RELAY_ROTATION_SRC_BASED;
            minDelayLimit = DEFAULT_DELAY_LIMIT;
            scale = ceilDiv(blockIntervalSrc * RelayRotation.SCALE_PRECISION, blockIntervalDst);
            rotateTerm = ceilDiv(maxAggregation * RelayRotation.SCALE_PRECISION, scale);
        }
        if (maxAggregation < 1) {
            throw BMCException.unknown("invalid param maxAgg");
        }
        if (delayLimit < minDelayLimit) {
            throw BMCException.unknown("invalid param delayLimit");
        }
        this.blockIntervalDst = blockIntervalDst;
        this.maxAggregation = maxAggregation;
        this.delayLimit = delayLimit;
        rotateHeight = Context.getBlockHeight() + rotateTerm;
    }

    public boolean isRelayRotationEnabled() {
        return policy != RELAY_ROTATION_NONE;
    }
    ////

    public int getPolicy() {
        return policy;
    }

    public void setPolicy(int policy) {
        this.policy = policy;
    }

    public int getRelayIdx() {
        return relayIdx;
    }

    public void setRelayIdx(int relayIdx) {
        this.relayIdx = relayIdx;
    }

    public long getRotateHeight() {
        return rotateHeight;
    }

    public void setRotateHeight(long rotateHeight) {
        this.rotateHeight = rotateHeight;
    }

    public long getRotateTerm() {
        return rotateTerm;
    }

    public void setRotateTerm(long rotateTerm) {
        this.rotateTerm = rotateTerm;
    }

    public int getMaxAggregation() {
        return maxAggregation;
    }

    public void setMaxAggregation(int maxAggregation) {
        this.maxAggregation = maxAggregation;
    }

    public int getDelayLimit() {
        return delayLimit;
    }

    public void setDelayLimit(int delayLimit) {
        this.delayLimit = delayLimit;
    }

    public long getBlockIntervalDst() {
        return blockIntervalDst;
    }

    public void setBlockIntervalDst(long blockIntervalDst) {
        this.blockIntervalDst = blockIntervalDst;
    }

    public long getScale() {
        return scale;
    }

    public void setScale(long scale) {
        this.scale = scale;
    }

    public long getRxHeight() {
        return rxHeight;
    }

    public void setRxHeight(long rxHeight) {
        this.rxHeight = rxHeight;
    }

    public long getRxHeightSrc() {
        return rxHeightSrc;
    }

    public void setRxHeightSrc(long rxHeightSrc) {
        this.rxHeightSrc = rxHeightSrc;
    }
}
