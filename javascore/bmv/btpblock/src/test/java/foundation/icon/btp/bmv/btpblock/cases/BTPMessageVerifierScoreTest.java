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

package foundation.icon.btp.bmv.btpblock.cases;

import foundation.icon.btp.bmv.btpblock.score.BMCScore;
import foundation.icon.btp.bmv.btpblock.score.BMVScore;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.score.util.StringUtil;
import foundation.icon.test.*;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.*;
import scorex.util.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
    private static final byte[] FIRST_BLOCK_UPDATE1 = StringUtil.hexToBytes("f8a80a00a031a48b551e5db0c6db4ad9f2002905f1b2005bcd0f4087b284a06f0137cafb4fc00101f80001a005cd98fdecc74538182a123f3d91e031833da3e9b0a2558d6652e48bf318a1b2b85cf85af858950020e2c291b19598e1338bc7a9c373b69f6dc4c6139500dff51eb43f08ee74678e69ba29626df65f8f4e5295003f695fedf6a2aa1c7a07fe6810ed5ba7edc7c48c9500235c67cee3e2bfda66a34e884991414391fc6abf");
    private static final byte[] FIRST_BLOCK_UPDATE2 = StringUtil.hexToBytes("f8ac1400a0619411be422c780d27bcc7b121756857aaab69d4e233890374254e6f2b8b3e9be3e200a0142e0892c8baca64bfa8c6f53aac2b2f494ef854672d91a3372107ee29dc7cbc0301f80000f800b85cf85af858950055c477ef0e31bf5cb9e29135426e8d65e911f12895009e093f72a4172646e82f366325209046e6ff0abf95003cf590de7f8d4bc03a151739a22aa9f36a3c464d9500768cbda48d422bf788d2a2b2c90fb72f3e549913");
    private static final byte[] FAIL_CASE_FIRST_BLOCK_UPDATE = StringUtil.hexToBytes("f8a80a00a031a48b551e5db0c6db4ad9f2002905f1b2005bcd0f4087b284a06f0137cafb4fc00101f80001a005cd98fdecc74538182a123f3d91e031833da3e9b0a2558d6652e48bf318a1b2b85cf85af858950020e2c291b19598e1338bc7a9c373b69f6dc4c6139500dff51eb43f08ee74678e69ba29626df65f8f4e5295003f695fedf6a2aa1c7a07fe6810ed5ba7edc7c48c9500235c67cee3e2bfda66a34e884991414391fc6abf");
    static final List<String> SUCCESS_RELAY_MESSAGE1 = List.of(
            "cecdcc028ac9f800c483646f67f800",
            "f90229f90226f9020801b90204f90201f8ea1400a0619411be422c780d27bcc7b121756857aaab69d4e233890374254e6f2b8b3e9be3e201a01c4d6baea3bc5ebaceb78ff13c13e599fbd877477000216bb9141a232b49d6060103a083b60a531ee5a99ac099df397c68cd6573fd71424375307e3e718478f176c25003a04fbe4e3bbeb4387c64212b3ad1f7f980fe8202776e0464ef1027d5761836294bb85cf85af858950055c477ef0e31bf5cb9e29135426e8d65e911f12895009e093f72a4172646e82f366325209046e6ff0abf95003cf590de7f8d4bc03a151739a22aa9f36a3c464d9500768cbda48d422bf788d2a2b2c90fb72f3e549913b90112f9010ff9010cb841bcd1cdb4dd80ed343a64a1f470acad08eee999dc15c56d4bcb0d17e935ccac942967bb625ca770b05ebd520854546fc7f7544d1e930225777cf5252efb4a3baa00b841918e462b48d3e183ec8f76037f5a575b981aa1448587466201fa48c61961954b7f2cd1ee714d75361fa39560d555650d15293b37f2849c1ff4b7317e3ec8a32400b841dc1bbbcd2209a9879b99a9578a19626450798f79c4f6c2de4c98a56dc6607e7c3b67db7e80881b813d78663d1bc87465b988fd32aa87851a9b3a7700fa897cba01b841a8546b0ca9053e69f9433e4745b292556058e95efec307076ee0c84a624dbd6f7558e4b5653bd6e2bf0fb87332040b987f2077adf59bf511bf2c0ad21286a1ff00da0298d7f800d28363617488656c657068616e748462697264f800",
            "f901b2f901aff901ac01b901a8f901a5f88e1e00a0619411be422c780d27bcc7b121756857aaab69d4e233890374254e6f2b8b3e9be3e201a03f87356eedeb92cd325bdfb28bf832367e145459b0f995476a6c9ac2530718260108a0142e0892c8baca64bfa8c6f53aac2b2f494ef854672d91a3372107ee29dc7cbc03a00c8c76affe19f8c6b5425c7b43f86d208ab600617b8d6ce308cb2a4a7d8b7729f800b90112f9010ff9010cb8412dcbf039ec2c40d538b093acc36097b94ada64f26889af6aa6f7ed4f86af800b5b0138eefd7a21c221229168437c00c1e8223d9429805b9a503c6985c93eea1201b841344f9a97ec106adadbd7d46c0062652588d84c7120a3fde049b582ae7265c29d5c0b96132e76546ac5e262715b57bcada463b1c13f765f3dd2e356dd1c84e77700b84194dc69a878f53d1bc26d38cc7e3ec9d910d6baafec846df815ad6541cfa391ca320951292c1c0daae22987d39a830f5712248af0c80ad4e2bafa6bb2d677f7eb01b8412b25343cd5d16026b0e17bdca39c582c677d6d72c5903f8d643aecaf79ac19de2e12ccd4bc5eb680b2e5eeb5c805a085a86ee35994e3e09ecafc9b9255aa3f4801",
            "f868f866f502b3f2c0cc866d6f6e6b6579846c696f6ee3e201a01bb00f682e1b26ab9e36ee7f9f874947bc00b5926c69451a22dc99fe8cfd21c6ef02adece3e202a00b4eb6e6085978f2b568ed3d8ee5e44152ca8f5ffac845196c9db67cf568117dc6857469676572c0",
            "f903dbf903d8f901e901b901e5f901e2f8cb2800a06840a9de833c38011022c7b01faa8d6655643659dde4583d66c5117e248ae003e3e201a0f639f8b40f21aace387f8c905cf18ad7b649df078305d2e4ed7459927904ba97010fa0aedb7c502dc3a69f3c41774a59400f388319e00cead7bfbfb813e6de5123bab600f800b85cf85af8589500065d0929e1c52fb8a81546930ad08ed9a0729b8c9500b6b804e5d3fe7e83334bdac129a4689d37ea38659500d46626dd9c3e48890ca9b869f8b8ce730981e3489500ff9d245002caa7f0dccf9e4c1844fc6e36f0f25ab90112f9010ff9010cb841348c6c3f72aa887b767a90694ca455fc03f88bcaae1bf4fca5db6675bb4af5fe24f90dc004e6c82c0502331415c264fe7547bf91a165d8571e1fe8e0cd32458e01b841fe7ca9b5a06e04c5da0cee72e15837b640dfdca8feabf7b4c5e1e21bde71d94e5d6543f4896f351f36f9bd923d55f3a5963653d3a8ce4f0a1a6c92d7b8906adf01b841ca70995e83b72504900c6c285819e24fc6a5843ef1e79d60870798bdda35afcc2e25e0aa6c41fb45e6c451975ee76275223ccb88a440d14c5bde3010a527904100b8413b2f040ee6b5cd0d10337d65ddc825d337d61688c68afbb8df1f9fdfe3207c623aacff535671d8fab6f84d9506762d8c8dd13225d147d5f111dd185a1d6b3c2200f901e901b901e5f901e2f8cb3200a08eeeabca16ad37b4bacb1232505972b1a5ad01323996c14069ba20cb84976bb0e3e201a000b71d134936df5f281de0d15058851765d52a04642ca3ecc56230e3b8a1a446010fa08f8a3e6061e73efa63622769258ac8e26269a496400b8ad596841aaf3eabf0e900f800b85cf85af85895007d8b8c30fde1d20541d36952125a4c180c55212295005e9f6ec24ef5e452b41ed3bbef847a7fa784e29995006b07bce933e38141fe38e3bcdc8d4529fb941e4a95005f607f4f468894663fb039139be5b953998f9494b90112f9010ff9010cb841ab6826d9adaf7f2ae4f00f452192d0f55b028ea1f6bc19b37e5f404ac94b020413c1a0e81364e8d8d3b9fdc545dd31894836bd8ba9784a5c255bf48ca1bb25e201b841cf4db85ff0d0846ddc355e36bc5ca7f317e4d56619d55f2d7c9f32b862777d6a4885f47cea3c6452698d43fd3b879cf7a721f9b395b68f1eb4a609e0b65fc9aa01b84128fe030a515e94e98e90675f1679833d36397288e2abb6014b641dd455bd0afe7f8730f733599d043c6477a56943164e68b29c31f2dc0ef7aa7844e494bfb0cb00b84155569f681725e96f3406e82376e123974772fbde15d8d18792c50aca64d4756c6c5d61208c99677abc78d581d4a0d15d1db2f7027138c726dc86d45b4c17725601"
    );
    static final List<String> SUCCESS_RELAY_MESSAGE2 = List.of(
            "f901e8f901e5f901ac01b901a8f901a5f88e1e00a0619411be422c780d27bcc7b121756857aaab69d4e233890374254e6f2b8b3e9be3e200a0aedb7c502dc3a69f3c41774a59400f388319e00cead7bfbfb813e6de5123bab60300a01c4d6baea3bc5ebaceb78ff13c13e599fbd877477000216bb9141a232b49d60604a0d3debcca72ceadf5548997d9c61795151632569cbca03ff785059d2ad236ab15f800b90112f9010ff9010cb8412dcbf039ec2c40d538b093acc36097b94ada64f26889af6aa6f7ed4f86af800b5b0138eefd7a21c221229168437c00c1e8223d9429805b9a503c6985c93eea1201b841344f9a97ec106adadbd7d46c0062652588d84c7120a3fde049b582ae7265c29d5c0b96132e76546ac5e262715b57bcada463b1c13f765f3dd2e356dd1c84e77700b84194dc69a878f53d1bc26d38cc7e3ec9d910d6baafec846df815ad6541cfa391ca320951292c1c0daae22987d39a830f5712248af0c80ad4e2bafa6bb2d677f7eb01b8412b25343cd5d16026b0e17bdca39c582c677d6d72c5903f8d643aecaf79ac19de2e12ccd4bc5eb680b2e5eeb5c805a085a86ee35994e3e09ecafc9b9255aa3f4801f502b3f2c0cc866d6f6e6b6579846c696f6ee3e202a07382610d5f281c44a68856aa63e1cf0ca392897f6452ddedd4b69ff03a7059a5",
            "f90225f90222f502b3f2e3e202a00b4eb6e6085978f2b568ed3d8ee5e44152ca8f5ffac845196c9db67cf568117dcc8462697264866d6f6e6b6579c0f901e901b901e5f901e2f8cb2800a06840a9de833c38011022c7b01faa8d6655643659dde4583d66c5117e248ae003e3e200a08f8a3e6061e73efa63622769258ac8e26269a496400b8ad596841aaf3eabf0e90309a03f87356eedeb92cd325bdfb28bf832367e145459b0f995476a6c9ac25307182600f800b85cf85af8589500065d0929e1c52fb8a81546930ad08ed9a0729b8c9500b6b804e5d3fe7e83334bdac129a4689d37ea38659500d46626dd9c3e48890ca9b869f8b8ce730981e3489500ff9d245002caa7f0dccf9e4c1844fc6e36f0f25ab90112f9010ff9010cb841348c6c3f72aa887b767a90694ca455fc03f88bcaae1bf4fca5db6675bb4af5fe24f90dc004e6c82c0502331415c264fe7547bf91a165d8571e1fe8e0cd32458e01b841fe7ca9b5a06e04c5da0cee72e15837b640dfdca8feabf7b4c5e1e21bde71d94e5d6543f4896f351f36f9bd923d55f3a5963653d3a8ce4f0a1a6c92d7b8906adf01b841ca70995e83b72504900c6c285819e24fc6a5843ef1e79d60870798bdda35afcc2e25e0aa6c41fb45e6c451975ee76275223ccb88a440d14c5bde3010a527904100b8413b2f040ee6b5cd0d10337d65ddc825d337d61688c68afbb8df1f9fdfe3207c623aacff535671d8fab6f84d9506762d8c8dd13225d147d5f111dd185a1d6b3c2200",
            "f901eff901ecf901e901b901e5f901e2f8cb3200a08eeeabca16ad37b4bacb1232505972b1a5ad01323996c14069ba20cb84976bb0e3e200a0943bb20f7d3365bacac4b1efe22e5cddd14f474b653e97c272adfabdf42c7e7e0309a0f639f8b40f21aace387f8c905cf18ad7b649df078305d2e4ed7459927904ba9700f800b85cf85af85895007d8b8c30fde1d20541d36952125a4c180c55212295005e9f6ec24ef5e452b41ed3bbef847a7fa784e29995006b07bce933e38141fe38e3bcdc8d4529fb941e4a95005f607f4f468894663fb039139be5b953998f9494b90112f9010ff9010cb841ab6826d9adaf7f2ae4f00f452192d0f55b028ea1f6bc19b37e5f404ac94b020413c1a0e81364e8d8d3b9fdc545dd31894836bd8ba9784a5c255bf48ca1bb25e201b841cf4db85ff0d0846ddc355e36bc5ca7f317e4d56619d55f2d7c9f32b862777d6a4885f47cea3c6452698d43fd3b879cf7a721f9b395b68f1eb4a609e0b65fc9aa01b84128fe030a515e94e98e90675f1679833d36397288e2abb6014b641dd455bd0afe7f8730f733599d043c6477a56943164e68b29c31f2dc0ef7aa7844e494bfb0cb00b84155569f681725e96f3406e82376e123974772fbde15d8d18792c50aca64d4756c6c5d61208c99677abc78d581d4a0d15d1db2f7027138c726dc86d45b4c17725601"
    );

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        OkHttpClient ohc = new OkHttpClient.Builder().build();
        IconService iconService = new IconService(new HttpProvider(ohc, chain.getEndpointURL(3)));
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
        bmvScore = BMVScore.mustDeploy(txHandler, ownerWallet, srcNetworkID, networkTypeID, bmcScore.getAddress(), FAIL_CASE_FIRST_BLOCK_UPDATE, BigInteger.ZERO);
        var validMsg = "cecdcc028ac9f800c483646f67f800";
        String otherBMC = makeBTPAddress(otherNetworkBMC.getAddress());
        var txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), otherBMC, BigInteger.ZERO, StringUtil.hexToBytes(validMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + PERMISSION_DENIED + ")");
    }

    @Order(3)
    @Test
    public void scenario3() throws IOException, ResultTimeoutException {
        var blockUpdateMsg = "f9020ef9020bf9020801b90204f90201f8ea1400a0619411be422c780d27bcc7b121756857aaab69d4e233890374254e6f2b8b3e9be3e201a01c4d6baea3bc5ebaceb78ff13c13e599fbd877477000216bb9141a232b49d6060103a083b60a531ee5a99ac099df397c68cd6573fd71424375307e3e718478f176c25003a04fbe4e3bbeb4387c64212b3ad1f7f980fe8202776e0464ef1027d5761836294bb85cf85af858950055c477ef0e31bf5cb9e29135426e8d65e911f12895009e093f72a4172646e82f366325209046e6ff0abf95003cf590de7f8d4bc03a151739a22aa9f36a3c464d9500768cbda48d422bf788d2a2b2c90fb72f3e549913b90112f9010ff9010cb841bcd1cdb4dd80ed343a64a1f470acad08eee999dc15c56d4bcb0d17e935ccac942967bb625ca770b05ebd520854546fc7f7544d1e930225777cf5252efb4a3baa00b841918e462b48d3e183ec8f76037f5a575b981aa1448587466201fa48c61961954b7f2cd1ee714d75361fa39560d555650d15293b37f2849c1ff4b7317e3ec8a32400b841dc1bbbcd2209a9879b99a9578a19626450798f79c4f6c2de4c98a56dc6607e7c3b67db7e80881b813d78663d1bc87465b988fd32aa87851a9b3a7700fa897cba01b841a8546b0ca9053e69f9433e4745b292556058e95efec307076ee0c84a624dbd6f7558e4b5653bd6e2bf0fb87332040b987f2077adf59bf511bf2c0ad21286a1ff00";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ZERO, StringUtil.hexToBytes(blockUpdateMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_BLOCK_UPDATE + ")");

        var remainMessage = "cecdcc028ac9f800c483646f67f800";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ZERO, StringUtil.hexToBytes(remainMessage));
        txResult = txHandler.getResult(txHash);
        assertSuccess(txResult);

        var invalidNidMsg = "f9020ef9020bf9020801b90204f90201f8ea1400a0619411be422c780d27bcc7b121756857aaab69d4e233890374254e6f2b8b3e9be3e201a01c4d6baea3bc5ebaceb78ff13c13e599fbd877477000216bb9141a232b49d6060203a083b60a531ee5a99ac099df397c68cd6573fd71424375307e3e718478f176c25003a04fbe4e3bbeb4387c64212b3ad1f7f980fe8202776e0464ef1027d5761836294bb85cf85af858950055c477ef0e31bf5cb9e29135426e8d65e911f12895009e093f72a4172646e82f366325209046e6ff0abf95003cf590de7f8d4bc03a151739a22aa9f36a3c464d9500768cbda48d422bf788d2a2b2c90fb72f3e549913b90112f9010ff9010cb841bcd1cdb4dd80ed343a64a1f470acad08eee999dc15c56d4bcb0d17e935ccac942967bb625ca770b05ebd520854546fc7f7544d1e930225777cf5252efb4a3baa00b841918e462b48d3e183ec8f76037f5a575b981aa1448587466201fa48c61961954b7f2cd1ee714d75361fa39560d555650d15293b37f2849c1ff4b7317e3ec8a32400b841dc1bbbcd2209a9879b99a9578a19626450798f79c4f6c2de4c98a56dc6607e7c3b67db7e80881b813d78663d1bc87465b988fd32aa87851a9b3a7700fa897cba01b841a8546b0ca9053e69f9433e4745b292556058e95efec307076ee0c84a624dbd6f7558e4b5653bd6e2bf0fb87332040b987f2077adf59bf511bf2c0ad21286a1ff00";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidNidMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_BLOCK_UPDATE + ")");

        var invalidFirstSNMsg = "f9020ef9020bf9020801b90204f90201f8ea1400a0619411be422c780d27bcc7b121756857aaab69d4e233890374254e6f2b8b3e9be3e201a01c4d6baea3bc5ebaceb78ff13c13e599fbd877477000216bb9141a232b49d6060105a083b60a531ee5a99ac099df397c68cd6573fd71424375307e3e718478f176c25003a04fbe4e3bbeb4387c64212b3ad1f7f980fe8202776e0464ef1027d5761836294bb85cf85af858950055c477ef0e31bf5cb9e29135426e8d65e911f12895009e093f72a4172646e82f366325209046e6ff0abf95003cf590de7f8d4bc03a151739a22aa9f36a3c464d9500768cbda48d422bf788d2a2b2c90fb72f3e549913b90112f9010ff9010cb841bcd1cdb4dd80ed343a64a1f470acad08eee999dc15c56d4bcb0d17e935ccac942967bb625ca770b05ebd520854546fc7f7544d1e930225777cf5252efb4a3baa00b841918e462b48d3e183ec8f76037f5a575b981aa1448587466201fa48c61961954b7f2cd1ee714d75361fa39560d555650d15293b37f2849c1ff4b7317e3ec8a32400b841dc1bbbcd2209a9879b99a9578a19626450798f79c4f6c2de4c98a56dc6607e7c3b67db7e80881b813d78663d1bc87465b988fd32aa87851a9b3a7700fa897cba01b841a8546b0ca9053e69f9433e4745b292556058e95efec307076ee0c84a624dbd6f7558e4b5653bd6e2bf0fb87332040b987f2077adf59bf511bf2c0ad21286a1ff00";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidFirstSNMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_BLOCK_UPDATE + ")");

        var invalidPrevHashMsg = "f9020ef9020bf9020801b90204f90201f8ea1400a0619411be422c780d27bcc7b121756857aaab69d4e233890374254e6f2b8b3e9be3e201a01c4d6baea3bc5ebaceb78ff13c13e599fbd877477000216bb9141a232b49d6060103a0000000000000000000000000000000000000000000000000000000000000000003a04fbe4e3bbeb4387c64212b3ad1f7f980fe8202776e0464ef1027d5761836294bb85cf85af858950055c477ef0e31bf5cb9e29135426e8d65e911f12895009e093f72a4172646e82f366325209046e6ff0abf95003cf590de7f8d4bc03a151739a22aa9f36a3c464d9500768cbda48d422bf788d2a2b2c90fb72f3e549913b90112f9010ff9010cb841bcd1cdb4dd80ed343a64a1f470acad08eee999dc15c56d4bcb0d17e935ccac942967bb625ca770b05ebd520854546fc7f7544d1e930225777cf5252efb4a3baa00b841918e462b48d3e183ec8f76037f5a575b981aa1448587466201fa48c61961954b7f2cd1ee714d75361fa39560d555650d15293b37f2849c1ff4b7317e3ec8a32400b841dc1bbbcd2209a9879b99a9578a19626450798f79c4f6c2de4c98a56dc6607e7c3b67db7e80881b813d78663d1bc87465b988fd32aa87851a9b3a7700fa897cba01b841a8546b0ca9053e69f9433e4745b292556058e95efec307076ee0c84a624dbd6f7558e4b5653bd6e2bf0fb87332040b987f2077adf59bf511bf2c0ad21286a1ff00";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidPrevHashMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_BLOCK_UPDATE + ")");
    }

    @Order(4)
    @Test
    public void scenario4() throws IOException, ResultTimeoutException {
        var duplicatedSignatureMsg = "f9020ef9020bf9020801b90204f90201f8ea1400a0619411be422c780d27bcc7b121756857aaab69d4e233890374254e6f2b8b3e9be3e201a01c4d6baea3bc5ebaceb78ff13c13e599fbd877477000216bb9141a232b49d6060103a083b60a531ee5a99ac099df397c68cd6573fd71424375307e3e718478f176c25003a04fbe4e3bbeb4387c64212b3ad1f7f980fe8202776e0464ef1027d5761836294bb85cf85af858950055c477ef0e31bf5cb9e29135426e8d65e911f12895009e093f72a4172646e82f366325209046e6ff0abf95003cf590de7f8d4bc03a151739a22aa9f36a3c464d9500768cbda48d422bf788d2a2b2c90fb72f3e549913b90112f9010ff9010cb841bcd1cdb4dd80ed343a64a1f470acad08eee999dc15c56d4bcb0d17e935ccac942967bb625ca770b05ebd520854546fc7f7544d1e930225777cf5252efb4a3baa00b841918e462b48d3e183ec8f76037f5a575b981aa1448587466201fa48c61961954b7f2cd1ee714d75361fa39560d555650d15293b37f2849c1ff4b7317e3ec8a32400b841bcd1cdb4dd80ed343a64a1f470acad08eee999dc15c56d4bcb0d17e935ccac942967bb625ca770b05ebd520854546fc7f7544d1e930225777cf5252efb4a3baa00b841bcd1cdb4dd80ed343a64a1f470acad08eee999dc15c56d4bcb0d17e935ccac942967bb625ca770b05ebd520854546fc7f7544d1e930225777cf5252efb4a3baa00";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(duplicatedSignatureMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_PROOFS + ")");

        var proofNullMsg = "f8f7f8f5f8f301b8f0f8eef8ea1400a0619411be422c780d27bcc7b121756857aaab69d4e233890374254e6f2b8b3e9be3e201a01c4d6baea3bc5ebaceb78ff13c13e599fbd877477000216bb9141a232b49d6060103a083b60a531ee5a99ac099df397c68cd6573fd71424375307e3e718478f176c25003a04fbe4e3bbeb4387c64212b3ad1f7f980fe8202776e0464ef1027d5761836294bb85cf85af858950055c477ef0e31bf5cb9e29135426e8d65e911f12895009e093f72a4172646e82f366325209046e6ff0abf95003cf590de7f8d4bc03a151739a22aa9f36a3c464d9500768cbda48d422bf788d2a2b2c90fb72f3e549913f800";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(proofNullMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "UnknownFailure");
    }

    @Order(5)
    @Test
    public void scenario5() throws IOException, ResultTimeoutException {
        var hashMismatchMsg = "f901b3f901b0f901ad01b901a9f901a6f88f1400a0619411be422c780d27bcc7b121756857aaab69d4e233890374254e6f2b8b3e9be3e201a01c4d6baea3bc5ebaceb78ff13c13e599fbd877477000216bb9141a232b49d6060103a083b60a531ee5a99ac099df397c68cd6573fd71424375307e3e718478f176c25003a04fbe4e3bbeb4387c64212b3ad1f7f980fe8202776e0464ef1027d5761836294b82c1c0b90112f9010ff9010cb841bcd1cdb4dd80ed343a64a1f470acad08eee999dc15c56d4bcb0d17e935ccac942967bb625ca770b05ebd520854546fc7f7544d1e930225777cf5252efb4a3baa00b841918e462b48d3e183ec8f76037f5a575b981aa1448587466201fa48c61961954b7f2cd1ee714d75361fa39560d555650d15293b37f2849c1ff4b7317e3ec8a32400b841dc1bbbcd2209a9879b99a9578a19626450798f79c4f6c2de4c98a56dc6607e7c3b67db7e80881b813d78663d1bc87465b988fd32aa87851a9b3a7700fa897cba01b841a8546b0ca9053e69f9433e4745b292556058e95efec307076ee0c84a624dbd6f7558e4b5653bd6e2bf0fb87332040b987f2077adf59bf511bf2c0ad21286a1ff00";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(hashMismatchMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_BLOCK_UPDATE + ")");
    }

    @Order(6)
    @Test
    public void scenario6() throws IOException, ResultTimeoutException {
        var proofMessageMsg = "dcdbda0298d7f800d28363617488656c657068616e748462697264f800";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(proofMessageMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_MESSAGE_PROOF + ")");

        // make remain count 2
        var validBlockUpdate = "f9020ef9020bf9020801b90204f90201f8ea1400a0619411be422c780d27bcc7b121756857aaab69d4e233890374254e6f2b8b3e9be3e201a01c4d6baea3bc5ebaceb78ff13c13e599fbd877477000216bb9141a232b49d6060103a083b60a531ee5a99ac099df397c68cd6573fd71424375307e3e718478f176c25003a04fbe4e3bbeb4387c64212b3ad1f7f980fe8202776e0464ef1027d5761836294bb85cf85af858950055c477ef0e31bf5cb9e29135426e8d65e911f12895009e093f72a4172646e82f366325209046e6ff0abf95003cf590de7f8d4bc03a151739a22aa9f36a3c464d9500768cbda48d422bf788d2a2b2c90fb72f3e549913b90112f9010ff9010cb841bcd1cdb4dd80ed343a64a1f470acad08eee999dc15c56d4bcb0d17e935ccac942967bb625ca770b05ebd520854546fc7f7544d1e930225777cf5252efb4a3baa00b841918e462b48d3e183ec8f76037f5a575b981aa1448587466201fa48c61961954b7f2cd1ee714d75361fa39560d555650d15293b37f2849c1ff4b7317e3ec8a32400b841dc1bbbcd2209a9879b99a9578a19626450798f79c4f6c2de4c98a56dc6607e7c3b67db7e80881b813d78663d1bc87465b988fd32aa87851a9b3a7700fa897cba01b841a8546b0ca9053e69f9433e4745b292556058e95efec307076ee0c84a624dbd6f7558e4b5653bd6e2bf0fb87332040b987f2077adf59bf511bf2c0ad21286a1ff00";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(validBlockUpdate));
        txResult = txHandler.getResult(txHash);
        assertSuccess(txResult);

        var mismatchLeftNumMsg = "f842f840f83e02b83bf839e3e201a0d616607d3e4ba96a74f323cffc5f20a3c78e7cab8ecbdbb03b13fa8ffc9bf644d28363617488656c657068616e748462697264f800";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(mismatchLeftNumMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_MESSAGE_PROOF + ")");

        var invalidNumOfLeafMsg = "f842f840f83e02b83bf839e3e203a06ecf3422e9b5463a79c535bf0db8e1c6f589bbce30ec9ebb3cf4fe947d24cf79d28363617488656c657068616e748462697264f800";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidNumOfLeafMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_MESSAGE_PROOF + ")");

        var invalidLevelMsg = "f0efee02acebf800c483636174e3e202a06ecf3422e9b5463a79c535bf0db8e1c6f589bbce30ec9ebb3cf4fe947d24cf79";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidLevelMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_MESSAGE_PROOF + ")");

        var mismatchCountMsg = "e1e0df029ddcf800d78363617488656c657068616e7484626972648474657374f800";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(mismatchCountMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_MESSAGE_PROOF + ")");

        var mismatchRootMsg = "e0dfde029cdbf800d6876361747465737488656c657068616e748462697264f800";
        txHash = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(mismatchRootMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + INVALID_MESSAGE_PROOF + ")");
    }

    private void positiveCase(List<String> msgList, byte[] firstBlockUpdate, long[] seqs) throws TransactionFailureException, IOException, ResultTimeoutException {
        // Deploy BMV
        bmvScore = BMVScore.mustDeploy(txHandler, ownerWallet, srcNetworkID, networkTypeID, bmcScore.getAddress(), firstBlockUpdate, BigInteger.ZERO);

        var msgLength = msgList.size();
        var hashes = new Bytes[msgLength];
        for (int i = 0; i < msgLength; i++) {
            hashes[i] = bmcScore.intercallHandleRelayMessage(ownerWallet, bmvScore.getAddress(), makeBTPAddress(prevBmCScore.getAddress()), BigInteger.valueOf(seqs[i]), StringUtil.hexToBytes(msgList.get(i)));
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
