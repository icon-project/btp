/*
 * Copyright 2022 ICONLOOP Inc.
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

package foundation.icon.btp.bmv.btp.cases;

import foundation.icon.btp.bmv.btp.*;
import foundation.icon.btp.bmv.btp.score.BMCScore;
import foundation.icon.btp.bmv.btp.score.BMVScore;
import foundation.icon.btp.bmv.btp.score.ChainScore;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.score.util.StringUtil;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scorex.util.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class BTPMessageVerifierScoreTest extends TestBase {
    private static TransactionHandler txHandler;
    private static KeyWallet ownerWallet;
    private static ChainScore chainScore;
    private static IconService iconService;
    private static BMVScore bmvScore;
    private static BMCScore validBmcScore;
    private static BMCScore invalidBmCScore;
    private static Base64.Decoder decoder = Base64.getUrlDecoder();
    private static final byte[] srcNetworkID = StringUtil.hexToBytes("6274703a2f2f3078312e69636f6e");
    private static final BigInteger networkTypeID = BigInteger.ONE;
    private static final byte[] firstBlockUpdate1 = StringUtil.hexToBytes("f8aa0a00a031a48b551e5db0c6db4ad9f2002905f1b2005bcd0f4087b284a06f0137cafb4fc00101f80001a005cd98fdecc74538182a123f3d91e031833da3e9b0a2558d6652e48bf318a1b2f800b85cf85af858950020e2c291b19598e1338bc7a9c373b69f6dc4c6139500dff51eb43f08ee74678e69ba29626df65f8f4e5295003f695fedf6a2aa1c7a07fe6810ed5ba7edc7c48c9500235c67cee3e2bfda66a34e884991414391fc6abf");
    static final List<String> SUCCESS_RELAY_MESSAGE1 = List.of(
            "zs3MAorJ-ADEg2RvZ_gA",
            "-QIn-QIk-QIGAbkCAvkB_xQAoGGUEb5CLHgNJ7zHsSF1aFeqq2nU4jOJA3QlTm8riz6b4-IBoBxNa66jvF66zreP8TwT5Zn72HdHcAAha7kUGiMrSdYGAQOgg7YKUx7lqZrAmd85fGjNZXP9cUJDdTB-PnGEePF2wlADoE--Tju-tDh8ZCErOtH3-YD-ggJ3bgRk7xAn1XYYNilLuQES-QEP-QEMuEG80c203YDtNDpkofRwrK0I7umZ3BXFbUvLDRfpNcyslClnu2Jcp3CwXr1SCFRUb8f3VE0ekwIld3z1JS77SjuqALhBkY5GK0jT4YPsj3YDf1pXW5gaoUSFh0ZiAfpIxhlhlUt_LNHucU11Nh-jlWDVVWUNFSk7N_KEnB_0tzF-PsijJAC4Qdwbu80iCamHm5mpV4oZYmRQeY95xPbC3kyYpW3GYH58O2fbfoCIG4E9eGY9G8h0ZbmI_TKqh4Uamzp3APqJfLoBuEGoVGsMqQU-aflDPkdFspJVYFjpXv7DBwdu4MhKYk29b3VY5LVlO9bivw-4czIEC5h_IHet9Zv1Eb8sCtIShqH_ALhc-Fr4WJUAVcR37w4xv1y54pE1Qm6NZekR8SiVAJ4JP3KkFyZG6C82YyUgkEbm_wq_lQA89ZDef41LwDoVFzmiKqnzajxGTZUAdoy9pI1CK_eI0qKyyQ-3Lz5UmRPaApjX-ADSg2NhdIhlbGVwaGFudIRiaXJk-AA=",
            "-QGw-QGt-QGqAbkBpvkBox4AoGGUEb5CLHgNJ7zHsSF1aFeqq2nU4jOJA3QlTm8riz6b4-IBoD-HNW7t65LNMlvfsov4MjZ-FFRZsPmVR2psmsJTBxgmAQigFC4Iksi6ymS_qMb1OqwrL0lO-FRnLZGjNyEH7incfLwDoAyMdq_-GfjGtUJce0P4bSCKtgBhe41s4wjLKkp9i3cpuQES-QEP-QEMuEEty_A57CxA1Tiwk6zDYJe5Stpk8miJr2qm9-1Phq-AC1sBOO79eiHCISKRaEN8AMHoIj2UKYBbmlA8aYXJPuoSAbhBNE-al-wQatrb19RsAGJlJYjYTHEgo_3gSbWCrnJlwp1cC5YTLnZUasXiYnFbV7ytpGOxwT92Xz3S41bdHITndwC4QZTcaah49T0bwm04zH4-ydkQ1rqv7IRt-BWtZUHPo5HKMglRKSwcDariKYfTmoMPVxIkivDICtTiuvprstZ39-sBuEErJTQ81dFgJrDhe9yjnFgsZ31tcsWQP41kOuyveawZ3i4SzNS8XraAsuXutcgFoIWobuNZlOPgnsr8m5JVqj9IAfgA",
            "-Gj4ZvUCs_LAzIZtb25rZXmEbGlvbuPiAaAbsA9oLhsmq5427n-fh0lHvAC1kmxpRRoi3Jn-jP0hxu8Crezj4gKgC0625ghZePK1aO09juXkQVLKj1_6yEUZbJ22fPVoEX3GhXRpZ2VywA==",
            "-QPX-QPU-QHnAbkB4_kB4CgAoGhAqd6DPDgBECLHsB-qjWZVZDZZ3eRYPWbFEX4kiuAD4-IBoPY5-LQPIarOOH-MkFzxite2Sd8HgwXS5O10WZJ5BLqXAQ-grtt8UC3Dpp88QXdKWUAPOIMZ4Azq17-_uBPm3lEjurYA-AC5ARL5AQ_5AQy4QTSMbD9yqoh7dnqQaUykVfwD-IvKrhv0_KXbZnW7SvX-JPkNwATmyCwFAjMUFcJk_nVHv5GhZdhXHh_o4M0yRY4BuEH-fKm1oG4ExdoM7nLhWDe2QN_cqP6r97TF4eIb3nHZTl1lQ_SJbzUfNvm9kj1V86WWNlPTqM5PChpskte4kGrfAbhBynCZXoO3JQSQDGwoWBniT8alhD7x551ghweYvdo1r8wuJeCqbEH7RebEUZde52J1IjzLiKRA0Uxb3jAQpSeQQQC4QTsvBA7mtc0NEDN9Zd3IJdM31haIxor7uN8fn9_jIHxiOqz_U1Zx2Pq2-E2VBnYtjI3RMiXRR9XxEd0YWh1rPCIAuFz4WvhYlQAGXQkp4cUvuKgVRpMK0I7ZoHKbjJUAtrgE5dP-foMzS9rBKaRonTfqOGWVANRmJt2cPkiJDKm4afi4znMJgeNIlQD_nSRQAsqn8NzPnkwYRPxuNvDyWvkB5wG5AeP5AeAyAKCO7qvKFq03tLrLEjJQWXKxpa0BMjmWwUBpuiDLhJdrsOPiAaAAtx0TSTbfXygd4NFQWIUXZdUqBGQso-zFYjDjuKGkRgEPoI-KPmBh5z76Y2InaSWKyOJiaaSWQAuK1ZaEGq8-q_DpAPgAuQES-QEP-QEMuEGraCbZra9_KuTwD0UhktD1WwKOofa8GbN-X0BKyUsCBBPBoOgTZOjY07n9xUXdMYlINr2LqXhKXCVb9IyhuyXiAbhBz024X_DQhG3cNV42vFyn8xfk1WYZ1V8tfJ8yuGJ3fWpIhfR86jxkUmmNQ_07h5z3pyH5s5W2jx60pgngtl_JqgG4QSj-AwpRXpTpjpBnXxZ5gz02OXKI4qu2AUtkHdRVvQr-f4cw9zNZnQQ8ZHelaUMWTmiynDHy3A73qnhE5JS_sMsAuEFVVp9oFyXpbzQG6CN24SOXR3L73hXY0YeSxQrKZNR1bGxdYSCMmWd6vHjVgdSg0V0dsvcCcTjHJtyG1FtMF3JWAbhc-Fr4WJUAfYuMMP3h0gVB02lSElpMGAxVISKVAF6fbsJO9eRStB7Tu--Een-nhOKZlQBrB7zpM-OBQf4447zcjUUp-5QeSpUAX2B_T0aIlGY_sDkTm-W5U5mPlJQ="
    );
    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        OkHttpClient ohc = new OkHttpClient.Builder().build();
        iconService = new IconService(new HttpProvider(ohc, chain.getEndpointURL()));
        txHandler = new TransactionHandler(iconService, chain);

        // init wallets
        BigInteger amount = ICX.multiply(BigInteger.valueOf(3000));
        ownerWallet = KeyWallet.create();
        txHandler.transfer(ownerWallet.getAddress(), amount);
        ensureIcxBalance(txHandler, ownerWallet.getAddress(), BigInteger.ZERO, amount);

        // Deploy BMCs
        validBmcScore = BMCScore.mustDeploy(txHandler, ownerWallet, "0x1.icon");
        invalidBmCScore = BMCScore.mustDeploy(txHandler, ownerWallet, "0x1.icon");

    }
    @Test
    public void positiveCases() throws TransactionFailureException, IOException, ResultTimeoutException {
        // Deploy BMV
        bmvScore = BMVScore.mustDeploy(txHandler, ownerWallet, srcNetworkID, networkTypeID, validBmcScore.getAddress(), firstBlockUpdate1);

        var msgLength = SUCCESS_RELAY_MESSAGE1.size();
        var txHashes = new Bytes[msgLength];
        for (int i = 0; i < msgLength; i++) {
            txHashes[i] = validBmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), makeBTPAddress(invalidBmCScore.getAddress()), BigInteger.ZERO, decoder.decode(SUCCESS_RELAY_MESSAGE1.get(i).getBytes()));
        }
        for (Bytes hash : txHashes) {
            assertSuccess(txHandler.getResult(hash));
        }
    }

    private String makeBTPAddress(Address address) {
        var network = new String(srcNetworkID);
        return network + "/" + address.toString();
    }
}
