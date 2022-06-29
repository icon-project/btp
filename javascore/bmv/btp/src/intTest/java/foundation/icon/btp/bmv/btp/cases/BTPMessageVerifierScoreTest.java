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
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.score.util.StringUtil;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import scorex.util.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BTPMessageVerifierScoreTest extends TestBase {
    private static final int UNKNOWN = 25;
    private static final int PERMISSION_DENIED = 26;
    private static final int INVALID_PROOFS = 27;
    private static final int INVALID_BLOCK_UPDATE = 28;
    private static final int INVALID_MESSAGE_PROOF = 29;
    private static TransactionHandler txHandler;
    private static KeyWallet ownerWallet;
    private static BMVScore bmvScore;
    private static BMCScore bmcScore;
    private static BMCScore prevBmCScore;
    private static BMCScore otherNetworkBMC;
    private static final Base64.Decoder decoder = Base64.getUrlDecoder();
    private static final byte[] srcNetworkID = StringUtil.hexToBytes("6274703a2f2f3078312e69636f6e");
    private static final BigInteger networkTypeID = BigInteger.ONE;
    private static final byte[] FIRST_BLOCK_UPDATE1 = StringUtil.hexToBytes("f8aa0a00a031a48b551e5db0c6db4ad9f2002905f1b2005bcd0f4087b284a06f0137cafb4fc00101f80001a005cd98fdecc74538182a123f3d91e031833da3e9b0a2558d6652e48bf318a1b2f800b85cf85af858950020e2c291b19598e1338bc7a9c373b69f6dc4c6139500dff51eb43f08ee74678e69ba29626df65f8f4e5295003f695fedf6a2aa1c7a07fe6810ed5ba7edc7c48c9500235c67cee3e2bfda66a34e884991414391fc6abf");
    private static final byte[] FIRST_BLOCK_UPDATE2 = StringUtil.hexToBytes("f8ae1400a0619411be422c780d27bcc7b121756857aaab69d4e233890374254e6f2b8b3e9be3e200a0142e0892c8baca64bfa8c6f53aac2b2f494ef854672d91a3372107ee29dc7cbc0301f80000f800f800b85cf85af858950055c477ef0e31bf5cb9e29135426e8d65e911f12895009e093f72a4172646e82f366325209046e6ff0abf95003cf590de7f8d4bc03a151739a22aa9f36a3c464d9500768cbda48d422bf788d2a2b2c90fb72f3e549913");
    private static final byte[] FAIL_CASE_FIRST_BLOCK_UPDATE = StringUtil.hexToBytes("f8aa0a00a031a48b551e5db0c6db4ad9f2002905f1b2005bcd0f4087b284a06f0137cafb4fc00101f80001a005cd98fdecc74538182a123f3d91e031833da3e9b0a2558d6652e48bf318a1b2f800b85cf85af858950020e2c291b19598e1338bc7a9c373b69f6dc4c6139500dff51eb43f08ee74678e69ba29626df65f8f4e5295003f695fedf6a2aa1c7a07fe6810ed5ba7edc7c48c9500235c67cee3e2bfda66a34e884991414391fc6abf");
    static final List<String> SUCCESS_RELAY_MESSAGE1 = List.of(
            "zs3MAorJ-ADEg2RvZ_gA",
            "-QIn-QIk-QIGAbkCAvkB_xQAoGGUEb5CLHgNJ7zHsSF1aFeqq2nU4jOJA3QlTm8riz6b4-IBoBxNa66jvF66zreP8TwT5Zn72HdHcAAha7kUGiMrSdYGAQOgg7YKUx7lqZrAmd85fGjNZXP9cUJDdTB-PnGEePF2wlADoE--Tju-tDh8ZCErOtH3-YD-ggJ3bgRk7xAn1XYYNilLuQES-QEP-QEMuEG80c203YDtNDpkofRwrK0I7umZ3BXFbUvLDRfpNcyslClnu2Jcp3CwXr1SCFRUb8f3VE0ekwIld3z1JS77SjuqALhBkY5GK0jT4YPsj3YDf1pXW5gaoUSFh0ZiAfpIxhlhlUt_LNHucU11Nh-jlWDVVWUNFSk7N_KEnB_0tzF-PsijJAC4Qdwbu80iCamHm5mpV4oZYmRQeY95xPbC3kyYpW3GYH58O2fbfoCIG4E9eGY9G8h0ZbmI_TKqh4Uamzp3APqJfLoBuEGoVGsMqQU-aflDPkdFspJVYFjpXv7DBwdu4MhKYk29b3VY5LVlO9bivw-4czIEC5h_IHet9Zv1Eb8sCtIShqH_ALhc-Fr4WJUAVcR37w4xv1y54pE1Qm6NZekR8SiVAJ4JP3KkFyZG6C82YyUgkEbm_wq_lQA89ZDef41LwDoVFzmiKqnzajxGTZUAdoy9pI1CK_eI0qKyyQ-3Lz5UmRPaApjX-ADSg2NhdIhlbGVwaGFudIRiaXJk-AA=",
            "-QGw-QGt-QGqAbkBpvkBox4AoGGUEb5CLHgNJ7zHsSF1aFeqq2nU4jOJA3QlTm8riz6b4-IBoD-HNW7t65LNMlvfsov4MjZ-FFRZsPmVR2psmsJTBxgmAQigFC4Iksi6ymS_qMb1OqwrL0lO-FRnLZGjNyEH7incfLwDoAyMdq_-GfjGtUJce0P4bSCKtgBhe41s4wjLKkp9i3cpuQES-QEP-QEMuEEty_A57CxA1Tiwk6zDYJe5Stpk8miJr2qm9-1Phq-AC1sBOO79eiHCISKRaEN8AMHoIj2UKYBbmlA8aYXJPuoSAbhBNE-al-wQatrb19RsAGJlJYjYTHEgo_3gSbWCrnJlwp1cC5YTLnZUasXiYnFbV7ytpGOxwT92Xz3S41bdHITndwC4QZTcaah49T0bwm04zH4-ydkQ1rqv7IRt-BWtZUHPo5HKMglRKSwcDariKYfTmoMPVxIkivDICtTiuvprstZ39-sBuEErJTQ81dFgJrDhe9yjnFgsZ31tcsWQP41kOuyveawZ3i4SzNS8XraAsuXutcgFoIWobuNZlOPgnsr8m5JVqj9IAfgA",
            "-Gj4ZvUCs_LAzIZtb25rZXmEbGlvbuPiAaAbsA9oLhsmq5427n-fh0lHvAC1kmxpRRoi3Jn-jP0hxu8Crezj4gKgC0625ghZePK1aO09juXkQVLKj1_6yEUZbJ22fPVoEX3GhXRpZ2VywA==",
            "-QPX-QPU-QHnAbkB4_kB4CgAoGhAqd6DPDgBECLHsB-qjWZVZDZZ3eRYPWbFEX4kiuAD4-IBoPY5-LQPIarOOH-MkFzxite2Sd8HgwXS5O10WZJ5BLqXAQ-grtt8UC3Dpp88QXdKWUAPOIMZ4Azq17-_uBPm3lEjurYA-AC5ARL5AQ_5AQy4QTSMbD9yqoh7dnqQaUykVfwD-IvKrhv0_KXbZnW7SvX-JPkNwATmyCwFAjMUFcJk_nVHv5GhZdhXHh_o4M0yRY4BuEH-fKm1oG4ExdoM7nLhWDe2QN_cqP6r97TF4eIb3nHZTl1lQ_SJbzUfNvm9kj1V86WWNlPTqM5PChpskte4kGrfAbhBynCZXoO3JQSQDGwoWBniT8alhD7x551ghweYvdo1r8wuJeCqbEH7RebEUZde52J1IjzLiKRA0Uxb3jAQpSeQQQC4QTsvBA7mtc0NEDN9Zd3IJdM31haIxor7uN8fn9_jIHxiOqz_U1Zx2Pq2-E2VBnYtjI3RMiXRR9XxEd0YWh1rPCIAuFz4WvhYlQAGXQkp4cUvuKgVRpMK0I7ZoHKbjJUAtrgE5dP-foMzS9rBKaRonTfqOGWVANRmJt2cPkiJDKm4afi4znMJgeNIlQD_nSRQAsqn8NzPnkwYRPxuNvDyWvkB5wG5AeP5AeAyAKCO7qvKFq03tLrLEjJQWXKxpa0BMjmWwUBpuiDLhJdrsOPiAaAAtx0TSTbfXygd4NFQWIUXZdUqBGQso-zFYjDjuKGkRgEPoI-KPmBh5z76Y2InaSWKyOJiaaSWQAuK1ZaEGq8-q_DpAPgAuQES-QEP-QEMuEGraCbZra9_KuTwD0UhktD1WwKOofa8GbN-X0BKyUsCBBPBoOgTZOjY07n9xUXdMYlINr2LqXhKXCVb9IyhuyXiAbhBz024X_DQhG3cNV42vFyn8xfk1WYZ1V8tfJ8yuGJ3fWpIhfR86jxkUmmNQ_07h5z3pyH5s5W2jx60pgngtl_JqgG4QSj-AwpRXpTpjpBnXxZ5gz02OXKI4qu2AUtkHdRVvQr-f4cw9zNZnQQ8ZHelaUMWTmiynDHy3A73qnhE5JS_sMsAuEFVVp9oFyXpbzQG6CN24SOXR3L73hXY0YeSxQrKZNR1bGxdYSCMmWd6vHjVgdSg0V0dsvcCcTjHJtyG1FtMF3JWAbhc-Fr4WJUAfYuMMP3h0gVB02lSElpMGAxVISKVAF6fbsJO9eRStB7Tu--Een-nhOKZlQBrB7zpM-OBQf4447zcjUUp-5QeSpUAX2B_T0aIlGY_sDkTm-W5U5mPlJQ="
    );
    static final List<String> SUCCESS_RELAY_MESSAGE2 = List.of(
            "-QHm-QHj-QGqAbkBpvkBox4AoGGUEb5CLHgNJ7zHsSF1aFeqq2nU4jOJA3QlTm8riz6b4-IAoK7bfFAtw6afPEF3SllADziDGeAM6te_v7gT5t5RI7q2AwCgHE1rrqO8XrrOt4_xPBPlmfvYd0dwACFruRQaIytJ1gYEoNPevMpyzq31VImX2cYXlRUWMlacvKA_94UFnSrSNqsVuQES-QEP-QEMuEEty_A57CxA1Tiwk6zDYJe5Stpk8miJr2qm9-1Phq-AC1sBOO79eiHCISKRaEN8AMHoIj2UKYBbmlA8aYXJPuoSAbhBNE-al-wQatrb19RsAGJlJYjYTHEgo_3gSbWCrnJlwp1cC5YTLnZUasXiYnFbV7ytpGOxwT92Xz3S41bdHITndwC4QZTcaah49T0bwm04zH4-ydkQ1rqv7IRt-BWtZUHPo5HKMglRKSwcDariKYfTmoMPVxIkivDICtTiuvprstZ39-sBuEErJTQ81dFgJrDhe9yjnFgsZ31tcsWQP41kOuyveawZ3i4SzNS8XraAsuXutcgFoIWobuNZlOPgnsr8m5JVqj9IAfgA9QKz8sDMhm1vbmtleYRsaW9u4-ICoHOCYQ1fKBxEpohWqmPhzwyjkol_ZFLd7dS2n_A6cFml",
            "-QIj-QIg9QKz8uPiAqALTrbmCFl48rVo7T2O5eRBUsqPX_rIRRlsnbZ89WgRfcyEYmlyZIZtb25rZXnA-QHnAbkB4_kB4CgAoGhAqd6DPDgBECLHsB-qjWZVZDZZ3eRYPWbFEX4kiuAD4-IAoI-KPmBh5z76Y2InaSWKyOJiaaSWQAuK1ZaEGq8-q_DpAwmgP4c1bu3rks0yW9-yi_gyNn4UVFmw-ZVHamyawlMHGCYA-AC5ARL5AQ_5AQy4QTSMbD9yqoh7dnqQaUykVfwD-IvKrhv0_KXbZnW7SvX-JPkNwATmyCwFAjMUFcJk_nVHv5GhZdhXHh_o4M0yRY4BuEH-fKm1oG4ExdoM7nLhWDe2QN_cqP6r97TF4eIb3nHZTl1lQ_SJbzUfNvm9kj1V86WWNlPTqM5PChpskte4kGrfAbhBynCZXoO3JQSQDGwoWBniT8alhD7x551ghweYvdo1r8wuJeCqbEH7RebEUZde52J1IjzLiKRA0Uxb3jAQpSeQQQC4QTsvBA7mtc0NEDN9Zd3IJdM31haIxor7uN8fn9_jIHxiOqz_U1Zx2Pq2-E2VBnYtjI3RMiXRR9XxEd0YWh1rPCIAuFz4WvhYlQAGXQkp4cUvuKgVRpMK0I7ZoHKbjJUAtrgE5dP-foMzS9rBKaRonTfqOGWVANRmJt2cPkiJDKm4afi4znMJgeNIlQD_nSRQAsqn8NzPnkwYRPxuNvDyWg==",
            "-QHt-QHq-QHnAbkB4_kB4DIAoI7uq8oWrTe0ussSMlBZcrGlrQEyOZbBQGm6IMuEl2uw4-IAoJQ7sg99M2W6ysSx7-IuXN3RT0dLZT6XwnKt-r30LH5-Awmg9jn4tA8hqs44f4yQXPGK17ZJ3weDBdLk7XRZknkEupcA-AC5ARL5AQ_5AQy4QatoJtmtr38q5PAPRSGS0PVbAo6h9rwZs35fQErJSwIEE8Gg6BNk6NjTuf3FRd0xiUg2vYupeEpcJVv0jKG7JeIBuEHPTbhf8NCEbdw1Xja8XKfzF-TVZhnVXy18nzK4Ynd9akiF9HzqPGRSaY1D_TuHnPenIfmzlbaPHrSmCeC2X8mqAbhBKP4DClFelOmOkGdfFnmDPTY5cojiq7YBS2Qd1FW9Cv5_hzD3M1mdBDxkd6VpQxZOaLKcMfLcDveqeETklL-wywC4QVVWn2gXJelvNAboI3bhI5dHcvveFdjRh5LFCspk1HVsbF1hIIyZZ3q8eNWB1KDRXR2y9wJxOMcm3IbUW0wXclYBuFz4WvhYlQB9i4ww_eHSBUHTaVISWkwYDFUhIpUAXp9uwk715FK0HtO774R6f6eE4pmVAGsHvOkz44FB_jjjvNyNRSn7lB5KlQBfYH9PRoiUZj-wOROb5blTmY-UlA=="
    );

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        OkHttpClient ohc = new OkHttpClient.Builder().build();
        IconService iconService = new IconService(new HttpProvider(ohc, chain.getEndpointURL()));
        txHandler = new TransactionHandler(iconService, chain);

        // init wallets
        BigInteger amount = ICX.multiply(BigInteger.valueOf(3000));
        ownerWallet = KeyWallet.create();
        txHandler.transfer(ownerWallet.getAddress(), amount);
        ensureIcxBalance(txHandler, ownerWallet.getAddress(), BigInteger.ZERO, amount);

        // Deploy BMCs
        bmcScore = BMCScore.mustDeploy(txHandler, ownerWallet, "0x1.icon");
        prevBmCScore = BMCScore.mustDeploy(txHandler, ownerWallet, "0x1.icon");
        otherNetworkBMC = BMCScore.mustDeploy(txHandler, ownerWallet, "0x2.icon");
    }

    @Order(1)
    @Test
    public void positiveCases() throws TransactionFailureException, IOException, ResultTimeoutException {
        positiveCase(SUCCESS_RELAY_MESSAGE1, FIRST_BLOCK_UPDATE1, new long[]{0, 1, 4, 4, 7});
        positiveCase(SUCCESS_RELAY_MESSAGE2, FIRST_BLOCK_UPDATE2, new long[]{0, 2, 4});
    }

    @Order(2)
    @Test
    public void scenario2() throws TransactionFailureException, IOException, ResultTimeoutException {
        bmvScore = BMVScore.mustDeploy(txHandler, ownerWallet, srcNetworkID, networkTypeID, bmcScore.getAddress(), FAIL_CASE_FIRST_BLOCK_UPDATE);
        var encodedValidMsg = "zs3MAorJ-ADEg2RvZ_gA";
        String otherBMC = makeBTPAddress(otherNetworkBMC.getAddress());
        var txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), otherBMC, BigInteger.ZERO, decoder.decode(encodedValidMsg.getBytes()));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + PERMISSION_DENIED + ")");
    }

    @Order(3)
    @Test
    public void scenario3() throws IOException, ResultTimeoutException {
        var encodedBlockUpdateMsg = "-QIM-QIJ-QIGAbkCAvkB_xQAoGGUEb5CLHgNJ7zHsSF1aFeqq2nU4jOJA3QlTm8riz6b4-IBoBxNa66jvF66zreP8TwT5Zn72HdHcAAha7kUGiMrSdYGAQOgg7YKUx7lqZrAmd85fGjNZXP9cUJDdTB-PnGEePF2wlADoE--Tju-tDh8ZCErOtH3-YD-ggJ3bgRk7xAn1XYYNilLuQES-QEP-QEMuEG80c203YDtNDpkofRwrK0I7umZ3BXFbUvLDRfpNcyslClnu2Jcp3CwXr1SCFRUb8f3VE0ekwIld3z1JS77SjuqALhBkY5GK0jT4YPsj3YDf1pXW5gaoUSFh0ZiAfpIxhlhlUt_LNHucU11Nh-jlWDVVWUNFSk7N_KEnB_0tzF-PsijJAC4Qdwbu80iCamHm5mpV4oZYmRQeY95xPbC3kyYpW3GYH58O2fbfoCIG4E9eGY9G8h0ZbmI_TKqh4Uamzp3APqJfLoBuEGoVGsMqQU-aflDPkdFspJVYFjpXv7DBwdu4MhKYk29b3VY5LVlO9bivw-4czIEC5h_IHet9Zv1Eb8sCtIShqH_ALhc-Fr4WJUAVcR37w4xv1y54pE1Qm6NZekR8SiVAJ4JP3KkFyZG6C82YyUgkEbm_wq_lQA89ZDef41LwDoVFzmiKqnzajxGTZUAdoy9pI1CK_eI0qKyyQ-3Lz5UmRM=";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ZERO, decoder.decode(encodedBlockUpdateMsg.getBytes()));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_BLOCK_UPDATE + ")");

        var remainMessage = "zs3MAorJ-ADEg2RvZ_gA";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ZERO, decoder.decode(remainMessage.getBytes()));
        txResult = txHandler.getResult(txHash);
        assertSuccess(txResult);

        var encodedInvalidNidMsg = "-QIM-QIJ-QIGAbkCAvkB_xQAoGGUEb5CLHgNJ7zHsSF1aFeqq2nU4jOJA3QlTm8riz6b4-IBoBxNa66jvF66zreP8TwT5Zn72HdHcAAha7kUGiMrSdYGAgOgg7YKUx7lqZrAmd85fGjNZXP9cUJDdTB-PnGEePF2wlADoE--Tju-tDh8ZCErOtH3-YD-ggJ3bgRk7xAn1XYYNilLuQES-QEP-QEMuEG80c203YDtNDpkofRwrK0I7umZ3BXFbUvLDRfpNcyslClnu2Jcp3CwXr1SCFRUb8f3VE0ekwIld3z1JS77SjuqALhBkY5GK0jT4YPsj3YDf1pXW5gaoUSFh0ZiAfpIxhlhlUt_LNHucU11Nh-jlWDVVWUNFSk7N_KEnB_0tzF-PsijJAC4Qdwbu80iCamHm5mpV4oZYmRQeY95xPbC3kyYpW3GYH58O2fbfoCIG4E9eGY9G8h0ZbmI_TKqh4Uamzp3APqJfLoBuEGoVGsMqQU-aflDPkdFspJVYFjpXv7DBwdu4MhKYk29b3VY5LVlO9bivw-4czIEC5h_IHet9Zv1Eb8sCtIShqH_ALhc-Fr4WJUAVcR37w4xv1y54pE1Qm6NZekR8SiVAJ4JP3KkFyZG6C82YyUgkEbm_wq_lQA89ZDef41LwDoVFzmiKqnzajxGTZUAdoy9pI1CK_eI0qKyyQ-3Lz5UmRM=";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, decoder.decode(encodedInvalidNidMsg.getBytes()));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_BLOCK_UPDATE + ")");

        var encodedInvalidFirstSNMsg = "-QIM-QIJ-QIGAbkCAvkB_xQAoGGUEb5CLHgNJ7zHsSF1aFeqq2nU4jOJA3QlTm8riz6b4-IBoBxNa66jvF66zreP8TwT5Zn72HdHcAAha7kUGiMrSdYGAQWgg7YKUx7lqZrAmd85fGjNZXP9cUJDdTB-PnGEePF2wlADoE--Tju-tDh8ZCErOtH3-YD-ggJ3bgRk7xAn1XYYNilLuQES-QEP-QEMuEG80c203YDtNDpkofRwrK0I7umZ3BXFbUvLDRfpNcyslClnu2Jcp3CwXr1SCFRUb8f3VE0ekwIld3z1JS77SjuqALhBkY5GK0jT4YPsj3YDf1pXW5gaoUSFh0ZiAfpIxhlhlUt_LNHucU11Nh-jlWDVVWUNFSk7N_KEnB_0tzF-PsijJAC4Qdwbu80iCamHm5mpV4oZYmRQeY95xPbC3kyYpW3GYH58O2fbfoCIG4E9eGY9G8h0ZbmI_TKqh4Uamzp3APqJfLoBuEGoVGsMqQU-aflDPkdFspJVYFjpXv7DBwdu4MhKYk29b3VY5LVlO9bivw-4czIEC5h_IHet9Zv1Eb8sCtIShqH_ALhc-Fr4WJUAVcR37w4xv1y54pE1Qm6NZekR8SiVAJ4JP3KkFyZG6C82YyUgkEbm_wq_lQA89ZDef41LwDoVFzmiKqnzajxGTZUAdoy9pI1CK_eI0qKyyQ-3Lz5UmRM=";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, decoder.decode(encodedInvalidFirstSNMsg.getBytes()));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_BLOCK_UPDATE + ")");

        var encodedInvalidPrevHashMsg = "-QIM-QIJ-QIGAbkCAvkB_xQAoGGUEb5CLHgNJ7zHsSF1aFeqq2nU4jOJA3QlTm8riz6b4-IBoBxNa66jvF66zreP8TwT5Zn72HdHcAAha7kUGiMrSdYGAQOgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADoE--Tju-tDh8ZCErOtH3-YD-ggJ3bgRk7xAn1XYYNilLuQES-QEP-QEMuEG80c203YDtNDpkofRwrK0I7umZ3BXFbUvLDRfpNcyslClnu2Jcp3CwXr1SCFRUb8f3VE0ekwIld3z1JS77SjuqALhBkY5GK0jT4YPsj3YDf1pXW5gaoUSFh0ZiAfpIxhlhlUt_LNHucU11Nh-jlWDVVWUNFSk7N_KEnB_0tzF-PsijJAC4Qdwbu80iCamHm5mpV4oZYmRQeY95xPbC3kyYpW3GYH58O2fbfoCIG4E9eGY9G8h0ZbmI_TKqh4Uamzp3APqJfLoBuEGoVGsMqQU-aflDPkdFspJVYFjpXv7DBwdu4MhKYk29b3VY5LVlO9bivw-4czIEC5h_IHet9Zv1Eb8sCtIShqH_ALhc-Fr4WJUAVcR37w4xv1y54pE1Qm6NZekR8SiVAJ4JP3KkFyZG6C82YyUgkEbm_wq_lQA89ZDef41LwDoVFzmiKqnzajxGTZUAdoy9pI1CK_eI0qKyyQ-3Lz5UmRM=";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, decoder.decode(encodedInvalidPrevHashMsg.getBytes()));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_BLOCK_UPDATE + ")");
    }

    @Order(4)
    @Test
    public void scenario4() throws IOException, ResultTimeoutException {
        var encodedDuplicatedSignatureMsg = "-QIM-QIJ-QIGAbkCAvkB_xQAoGGUEb5CLHgNJ7zHsSF1aFeqq2nU4jOJA3QlTm8riz6b4-IBoBxNa66jvF66zreP8TwT5Zn72HdHcAAha7kUGiMrSdYGAQOgg7YKUx7lqZrAmd85fGjNZXP9cUJDdTB-PnGEePF2wlADoE--Tju-tDh8ZCErOtH3-YD-ggJ3bgRk7xAn1XYYNilLuQES-QEP-QEMuEG80c203YDtNDpkofRwrK0I7umZ3BXFbUvLDRfpNcyslClnu2Jcp3CwXr1SCFRUb8f3VE0ekwIld3z1JS77SjuqALhBkY5GK0jT4YPsj3YDf1pXW5gaoUSFh0ZiAfpIxhlhlUt_LNHucU11Nh-jlWDVVWUNFSk7N_KEnB_0tzF-PsijJAC4QbzRzbTdgO00OmSh9HCsrQju6ZncFcVtS8sNF-k1zKyUKWe7YlyncLBevVIIVFRvx_dUTR6TAiV3fPUlLvtKO6oAuEG80c203YDtNDpkofRwrK0I7umZ3BXFbUvLDRfpNcyslClnu2Jcp3CwXr1SCFRUb8f3VE0ekwIld3z1JS77SjuqALhc-Fr4WJUAVcR37w4xv1y54pE1Qm6NZekR8SiVAJ4JP3KkFyZG6C82YyUgkEbm_wq_lQA89ZDef41LwDoVFzmiKqnzajxGTZUAdoy9pI1CK_eI0qKyyQ-3Lz5UmRM=";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, decoder.decode(encodedDuplicatedSignatureMsg.getBytes()));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_PROOFS + ")");

        var proofNullMsg = "-PX48_jxAbju-OwUAKBhlBG-Qix4DSe8x7EhdWhXqqtp1OIziQN0JU5vK4s-m-PiAaAcTWuuo7xeus63j_E8E-WZ-9h3R3AAIWu5FBojK0nWBgEDoIO2ClMe5amawJnfOXxozWVz_XFCQ3Uwfj5xhHjxdsJQA6BPvk47vrQ4fGQhKzrR9_mA_oICd24EZO8QJ9V2GDYpS_gAuFz4WvhYlQBVxHfvDjG_XLnikTVCbo1l6RHxKJUAngk_cqQXJkboLzZjJSCQRub_Cr-VADz1kN5_jUvAOhUXOaIqqfNqPEZNlQB2jL2kjUIr94jSorLJD7cvPlSZEw==";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, decoder.decode(proofNullMsg.getBytes()));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "UnknownFailure");
    }

    @Order(5)
    @Test
    public void scenario5() throws IOException, ResultTimeoutException {
        var encodedHashMismatchMsg = "-QIM-QIJ-QIGAbkCAvkB_xQAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA4-IBoBxNa66jvF66zreP8TwT5Zn72HdHcAAha7kUGiMrSdYGAQOgg7YKUx7lqZrAmd85fGjNZXP9cUJDdTB-PnGEePF2wlADoE--Tju-tDh8ZCErOtH3-YD-ggJ3bgRk7xAn1XYYNilLuQES-QEP-QEMuEG80c203YDtNDpkofRwrK0I7umZ3BXFbUvLDRfpNcyslClnu2Jcp3CwXr1SCFRUb8f3VE0ekwIld3z1JS77SjuqALhBkY5GK0jT4YPsj3YDf1pXW5gaoUSFh0ZiAfpIxhlhlUt_LNHucU11Nh-jlWDVVWUNFSk7N_KEnB_0tzF-PsijJAC4Qdwbu80iCamHm5mpV4oZYmRQeY95xPbC3kyYpW3GYH58O2fbfoCIG4E9eGY9G8h0ZbmI_TKqh4Uamzp3APqJfLoBuEGoVGsMqQU-aflDPkdFspJVYFjpXv7DBwdu4MhKYk29b3VY5LVlO9bivw-4czIEC5h_IHet9Zv1Eb8sCtIShqH_ALhc-Fr4WJUAVcR37w4xv1y54pE1Qm6NZekR8SiVAJ4JP3KkFyZG6C82YyUgkEbm_wq_lQA89ZDef41LwDoVFzmiKqnzajxGTZUAdoy9pI1CK_eI0qKyyQ-3Lz5UmRM=";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, decoder.decode(encodedHashMismatchMsg.getBytes()));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_BLOCK_UPDATE + ")");
    }

    @Order(6)
    @Test
    public void scenario6() throws IOException, ResultTimeoutException {
        var encodedProofMessageMsg = "3NvaApjX-ADSg2NhdIhlbGVwaGFudIRiaXJk-AA=";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ZERO, decoder.decode(encodedProofMessageMsg.getBytes()));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_MESSAGE_PROOF + ")");

        // make remain count 2
        var encodedValidBlockUpdate = "-QIM-QIJ-QIGAbkCAvkB_xQAoGGUEb5CLHgNJ7zHsSF1aFeqq2nU4jOJA3QlTm8riz6b4-IBoBxNa66jvF66zreP8TwT5Zn72HdHcAAha7kUGiMrSdYGAQOgg7YKUx7lqZrAmd85fGjNZXP9cUJDdTB-PnGEePF2wlADoE--Tju-tDh8ZCErOtH3-YD-ggJ3bgRk7xAn1XYYNilLuQES-QEP-QEMuEG80c203YDtNDpkofRwrK0I7umZ3BXFbUvLDRfpNcyslClnu2Jcp3CwXr1SCFRUb8f3VE0ekwIld3z1JS77SjuqALhBkY5GK0jT4YPsj3YDf1pXW5gaoUSFh0ZiAfpIxhlhlUt_LNHucU11Nh-jlWDVVWUNFSk7N_KEnB_0tzF-PsijJAC4Qdwbu80iCamHm5mpV4oZYmRQeY95xPbC3kyYpW3GYH58O2fbfoCIG4E9eGY9G8h0ZbmI_TKqh4Uamzp3APqJfLoBuEGoVGsMqQU-aflDPkdFspJVYFjpXv7DBwdu4MhKYk29b3VY5LVlO9bivw-4czIEC5h_IHet9Zv1Eb8sCtIShqH_ALhc-Fr4WJUAVcR37w4xv1y54pE1Qm6NZekR8SiVAJ4JP3KkFyZG6C82YyUgkEbm_wq_lQA89ZDef41LwDoVFzmiKqnzajxGTZUAdoy9pI1CK_eI0qKyyQ-3Lz5UmRM=";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, decoder.decode(encodedValidBlockUpdate.getBytes()));
        txResult = txHandler.getResult(txHash);
        assertSuccess(txResult);

        var encodedMismatchLeftNumMsg = "-EL4QPg-Arg7-Dnj4gGg1hZgfT5LqWp08yPP_F8go8eOfKuOy9uwOxP6j_yb9kTSg2NhdIhlbGVwaGFudIRiaXJk-AA=";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, decoder.decode(encodedMismatchLeftNumMsg.getBytes()));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_MESSAGE_PROOF + ")");

        var encodedInvalidNumOfLeafMsg = "-EL4QPg-Arg7-Dnj4gOgbs80Ium1Rjp5xTW_DbjhxvWJu84w7J67PPT-lH0kz3nSg2NhdIhlbGVwaGFudIRiaXJk-AA=";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, decoder.decode(encodedInvalidNumOfLeafMsg.getBytes()));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_MESSAGE_PROOF + ")");

        var encodedInvalidLevelMsg = "8O_uAqzr-ADEg2NhdOPiAqBuzzQi6bVGOnnFNb8NuOHG9Ym7zjDsnrs89P6UfSTPeQ==";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, decoder.decode(encodedInvalidLevelMsg.getBytes()));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_MESSAGE_PROOF + ")");

        var encodedMismatchCountMsg = "4eDfAp3c-ADXg2NhdIhlbGVwaGFudIRiaXJkhHRlc3T4AA==";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, decoder.decode(encodedMismatchCountMsg.getBytes()));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_MESSAGE_PROOF + ")");

        var encodedMismatchRootMsg = "4N_eApzb-ADWh2NhdHRlc3SIZWxlcGhhbnSEYmlyZPgA";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, decoder.decode(encodedMismatchRootMsg.getBytes()));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_MESSAGE_PROOF + ")");
    }

    private void positiveCase(List<String> msgList, byte[] firstBlockUpdate, long[] seqs) throws TransactionFailureException, IOException, ResultTimeoutException {
        // Deploy BMV
        bmvScore = BMVScore.mustDeploy(txHandler, ownerWallet, srcNetworkID, networkTypeID, bmcScore.getAddress(), firstBlockUpdate);

        var msgLength = msgList.size();
        var hashes = new Bytes[msgLength];
        for (int i = 0; i < msgLength; i++) {
             hashes[i] = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), makeBTPAddress(prevBmCScore.getAddress()), BigInteger.valueOf(1), decoder.decode(msgList.get(i).getBytes()));
        }
        for (Bytes h : hashes) {
            assertSuccess(txHandler.getResult(h));
        }
    }

    private String makeBTPAddress(Address address) {
        var net = BMCScore.bmcNetWork.get(address);
        return "btp://" + net + "/" + address.toString();
    }
}
