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

package com.iconloop.btp.lib;

import score.annotation.Keep;

import java.math.BigInteger;

public class BMCStatus {
    private BigInteger rx_seq;
    private BigInteger tx_seq;
    private BMVStatus verifier;
    private BMRStatus[] relays;

    private int block_interval_src;
    private int block_interval_dst;
    private int max_agg;
    private int delay_limit;

    private int relay_idx;
    private long rotate_height;
    private int rotate_term;
    private long rx_height; //initialize with BMC.block_height
    private long rx_height_src; //initialize with BMV._offset

    private long cur_height;

    //SACK
//    private int sack_term; //0: disable sack
//    private int sack_next;
//    private int sack_height;
//    private int sack_seq;

    @Keep
    public BigInteger getRx_seq() {
        return rx_seq;
    }

    @Keep
    public void setRx_seq(BigInteger rx_seq) {
        this.rx_seq = rx_seq;
    }

    @Keep
    public BigInteger getTx_seq() {
        return tx_seq;
    }

    @Keep
    public void setTx_seq(BigInteger tx_seq) {
        this.tx_seq = tx_seq;
    }

    @Keep
    public BMVStatus getVerifier() {
        return verifier;
    }

    @Keep
    public void setVerifier(BMVStatus verifier) {
        this.verifier = verifier;
    }

    @Keep
    public BMRStatus[] getRelays() {
        return relays;
    }

    @Keep
    public void setRelays(BMRStatus[] relays) {
        this.relays = relays;
    }

    @Keep
    public int getBlock_interval_src() {
        return block_interval_src;
    }

    @Keep
    public void setBlock_interval_src(int block_interval_src) {
        this.block_interval_src = block_interval_src;
    }

    @Keep
    public int getBlock_interval_dst() {
        return block_interval_dst;
    }

    @Keep
    public void setBlock_interval_dst(int block_interval_dst) {
        this.block_interval_dst = block_interval_dst;
    }

    @Keep
    public int getMax_agg() {
        return max_agg;
    }

    @Keep
    public void setMax_agg(int max_agg) {
        this.max_agg = max_agg;
    }

    @Keep
    public int getDelay_limit() {
        return delay_limit;
    }

    @Keep
    public void setDelay_limit(int delay_limit) {
        this.delay_limit = delay_limit;
    }

    @Keep
    public int getRelay_idx() {
        return relay_idx;
    }

    @Keep
    public void setRelay_idx(int relay_idx) {
        this.relay_idx = relay_idx;
    }

    @Keep
    public long getRotate_height() {
        return rotate_height;
    }

    @Keep
    public void setRotate_height(long rotate_height) {
        this.rotate_height = rotate_height;
    }

    @Keep
    public int getRotate_term() {
        return rotate_term;
    }

    @Keep
    public void setRotate_term(int rotate_term) {
        this.rotate_term = rotate_term;
    }

    @Keep
    public long getRx_height() {
        return rx_height;
    }

    @Keep
    public void setRx_height(long rx_height) {
        this.rx_height = rx_height;
    }

    @Keep
    public long getRx_height_src() {
        return rx_height_src;
    }

    @Keep
    public void setRx_height_src(long rx_height_src) {
        this.rx_height_src = rx_height_src;
    }

    @Keep
    public long getCur_height() {
        return cur_height;
    }

    @Keep
    public void setCur_height(long cur_height) {
        this.cur_height = cur_height;
    }
}
