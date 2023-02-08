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
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BTPMessageVerifierScoreTest extends TestBase {
    private static final int UNKNOWN = 25;
    private static final int NOT_VERIFIABLE = 26;
    private static TransactionHandler txHandler;
    private static KeyWallet ownerWallet;
    private static BMVScore bmvScore;
    private static BMCScore bmcScore;
    private static BMCScore prevBmCScore;
    private static BMCScore otherNetworkBMC;
    private static final String srcNetworkID = "btp://0x1.icon";
    private static final BigInteger networkTypeID = BigInteger.TWO;
    private static final byte[] FIRST_BLOCK_HEADER1 = StringUtil.hexToBytes("f8a40a00a08d647190ed786d4f8a522f32351ff2269732f6e3771b908271ca29aa73c2c03cc00201f80001a041791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7ab858f856f85494e3aee81071e40c44dde872e568d8d09429c5970e94b3d988f28443446c9d7ac62f673085e2736ca4e094f7646979981a5d7dcc05ea261ae26ecd2c2c81889477fec3da1ba8b192f4b278057395a5124392c88b");
    private static final byte[] FIRST_BLOCK_HEADER2 = StringUtil.hexToBytes("f8a81400a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e200a0335fc784176e18f4d3efefc662503f0fd8fe120751ed4251a66aaa93864001520401f80000f800b858f856f85494911ac74dd9ff8f4cdd91e747afcfdc9410a926e99497e36fb88560a3023c509704801eb1149acecf4394a9b0a74b2b63ab9cd20c6e38c88195c8175beb4694432b6448f3471aef190819b3c4f549a3a689d83a");
    private static final byte[] FAIL_CASE_FIRST_BLOCK_HEADER = StringUtil.hexToBytes("f8a40a00a08d647190ed786d4f8a522f32351ff2269732f6e3771b908271ca29aa73c2c03cc00201f80001a041791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7ab858f856f85494e3aee81071e40c44dde872e568d8d09429c5970e94b3d988f28443446c9d7ac62f673085e2736ca4e094f7646979981a5d7dcc05ea261ae26ecd2c2c81889477fec3da1ba8b192f4b278057395a5124392c88b");
    static final List<String> SUCCESS_RELAY_MESSAGE1 = List.of(
            "cecdcc028ac9f800c483646f67f800",
            "f90227f90224f9020601b90202f901ffb8e8f8e61400a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494911ac74dd9ff8f4cdd91e747afcfdc9410a926e99497e36fb88560a3023c509704801eb1149acecf4394a9b0a74b2b63ab9cd20c6e38c88195c8175beb4694432b6448f3471aef190819b3c4f549a3a689d83ab90112f9010ff9010cb8411c8e1c4e89ea6f2a22c06d49984d8088f40b5d8bc1ff547d790df8374454f9ff73f4324168cb68a1c78d74ee05a2cccb1a471c26c3dcd05af4c21241d31a8fd301b8411f915ae7f047db8fee3fec42882a1747aa898c5149e8751e479235f33779fd414f52c3a63d0dd24953f3e8ed77eb06965410c2c365f669abc74d0239dbc7052a01b841d21638f8aee5194920df53652adc906f66944044e1d9176e45b9d3d80d010ded7967ff4dd944b6f9214794f0c967529663eeda3d3b51c2b7bcd56ff8f6a41d3800b8416be3ab56807f762f262a9feb819aa1aac1e83330e1568764bb9e766519135fb64f72adef323187c700e6b30b3a44e766ecb8885f1290074c43f7bd3bb06f56dc01da0298d7f800d28363617488656c657068616e748462697264f800",
            "f901b4f901b1f901ae01b901aaf901a7b890f88e1e00a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0dd7d7cd1229649a054dbfd03969abe0ce4e439afc09f36e07165b95f866f565c0208a0335fc784176e18f4d3efefc662503f0fd8fe120751ed4251a66aaa938640015203a086c05cf325d1a9fc932360037a87b8871c838f274eab4d8010ae9c81b3de24a9f800b90112f9010ff9010cb841128dd79e62e79f27067e51e192322a76d889e2b6e2a28897c6ab2649a0352e9a7991e5891e841a2c82d80817ebe763dd797f0d1c09c535d2ac6aa427edabf16500b8418e7f3249ec892000e44ee25576bbf63d00258adf9cb74033f6b04a8c35fb47ec202f27dadc243d7ba6768231152aa3de0db21c779a243e6abcdd037fd1723e0b00b841a7a888d0cc7acc6a0d69e52fd955ae350ec3ec0fc0b9587b3f342cf5dc63889d7a5496eeb7e81f0a94ea43b1b5f5aa198fa6ea52f60bef6bc195e8a1f516bdbb01b84116d124bdb4b67e63dad5b911f26c338d9501221778d16058cf787381b234561a4987039f35ba1435db164c34dff4b9a7b01a49ab3319132c9ce547c7878f05e700",
            "f868f866f502b3f2c0cc866d6f6e6b6579846c696f6ee3e201a038f4921087750bcc254ecc791170ebb01a0297d49c630a8844c18f6104a5da07ef02adece3e202a09dc633d90e96b8744b27aaa5bb6bb2cca28f187c196cf7af1c82d8d1e8cd5f6fc6857469676572c0",
            "f903d7f903d4f901e701b901e3f901e0b8c9f8c72800a0d24c4f3d98a310f94047f0ea18b553f9f570195605189e4cb7b33ca88a29008ae3e201a08c46a84b0bad8d2ee375d1fb666256ef0abb7bfcf208b0e6e9a83718e2dda4c8020fa049da67cde1ed94d33df761d65abe0f8b17bedd41a133df50de63aeb80aa9a7e700f800b858f856f85494dccf35820a337512baa66780f112fef74593375d940536e6d35901f056ed48f19c8d2b8e76c6292399942182851a75a5b1657d7435e3178975ba97372083940f951e1fa3f96a2a3b92f05f382c6a87126945cbb90112f9010ff9010cb841e841970cd4ea08870f949bfaef59e6efab1a7132a2f7156e9ed21427eb7631eb6ed1e46b1befab6f080399f0d2bef6492081d8aa3e716de819cc8a7e99c25d9400b8414a0688b6ebf8ac47b423267fa86cff8bdef6095934fa151dbf07b7a861fd5e4400557247b0278df25b4c865b3c8a067f9b40693ef75f31e680030a2eaf25cd0a01b841b1158d5d63fe8f080ff55d136e3eb07b7adf3f1fac409d541599a1eb1111c4011b62649875f724509de431ff61cd80e700cc5cfa317b9e33df67e65aa6d5cecf00b84198fc2f6d8695775685b80c68562ffb2e487389fc5b839d5c95c365426b58bf812c6f1eb33baa775291ad284e02ddb11dd5e9476fdf2ba1f6bdd4ea722ef6dcb200f901e701b901e3f901e0b8c9f8c73200a0f15b61d91392ddcb9fb60beb138a8a1fff821be0371d3c20f9caa063b9df8e3ae3e201a0d1cdfff9f24d50ba88a34f19cbdf5c214169df63629e299d9baffb242a1bb428020fa0efe5785c35214f54e0ee595ebbe96842cd158d9329e3f3f6ed7e30f8cfa835f900f800b858f856f85494f28a5305dbe556fd0103596aca9129d730a916b89442aa62a65b7d03ed68a06b95ab3445d6af294be99433a9779f75b36f372a4a6600579b9a9959e4568794c9ae50b4423509c64dd74aee98ee2aa0e0fed323b90112f9010ff9010cb841e6d82ebc01e10cce3d6867c6caa2337f8eb487efec4fb00f81aa1c79158f218455945df9705d5fbccd0c14990d3d36db83085b13c81a0d5d63c1710bc51f965e01b84119cfcaa2ae283d6eefc52e9fec010ef53a8e4b4aa853a9ed6a026f58c930a937745cf1b21202c50865cc85a2bc505a307df7f6e3a107993d35eed249377d1d2a01b841c807ddaf5cef5ddf7688c32f6eed930fbf9f79893a0bcc355472e4f904103c0e0c6643c6e860249a068f8754d75a8f90d8dccd20a0cbb0899882d2bd3edd03b200b841ed78341a0f2ad318a3fff4f2e118c25e7a70cd6579de184a3c2817120ef32cc228f610348252c047b6192f86fd79fba9a39afe4e51ad9d205867918c6a45feca00"
    );
    static final List<String> SUCCESS_RELAY_MESSAGE2 = List.of(
            "f901eaf901e7f901ae01b901aaf901a7b890f88e1e00a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e200a049da67cde1ed94d33df761d65abe0f8b17bedd41a133df50de63aeb80aa9a7e70400a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac04a087b6db234476ea7fc1b573795cec8442233298ecc2d23b548dd619f92d306a2df800b90112f9010ff9010cb841128dd79e62e79f27067e51e192322a76d889e2b6e2a28897c6ab2649a0352e9a7991e5891e841a2c82d80817ebe763dd797f0d1c09c535d2ac6aa427edabf16500b8418e7f3249ec892000e44ee25576bbf63d00258adf9cb74033f6b04a8c35fb47ec202f27dadc243d7ba6768231152aa3de0db21c779a243e6abcdd037fd1723e0b00b841a7a888d0cc7acc6a0d69e52fd955ae350ec3ec0fc0b9587b3f342cf5dc63889d7a5496eeb7e81f0a94ea43b1b5f5aa198fa6ea52f60bef6bc195e8a1f516bdbb01b84116d124bdb4b67e63dad5b911f26c338d9501221778d16058cf787381b234561a4987039f35ba1435db164c34dff4b9a7b01a49ab3319132c9ce547c7878f05e700f502b3f2c0cc866d6f6e6b6579846c696f6ee3e202a0d3f9be5d7f0324950c636665ce754816c87f5bd9cd64aee0ce1c0bd0fa8a2242",
            "f90223f90220f502b3f2e3e202a09dc633d90e96b8744b27aaa5bb6bb2cca28f187c196cf7af1c82d8d1e8cd5f6fcc8462697264866d6f6e6b6579c0f901e701b901e3f901e0b8c9f8c72800a0d24c4f3d98a310f94047f0ea18b553f9f570195605189e4cb7b33ca88a29008ae3e200a0efe5785c35214f54e0ee595ebbe96842cd158d9329e3f3f6ed7e30f8cfa835f90409a0dd7d7cd1229649a054dbfd03969abe0ce4e439afc09f36e07165b95f866f565c00f800b858f856f85494dccf35820a337512baa66780f112fef74593375d940536e6d35901f056ed48f19c8d2b8e76c6292399942182851a75a5b1657d7435e3178975ba97372083940f951e1fa3f96a2a3b92f05f382c6a87126945cbb90112f9010ff9010cb841e841970cd4ea08870f949bfaef59e6efab1a7132a2f7156e9ed21427eb7631eb6ed1e46b1befab6f080399f0d2bef6492081d8aa3e716de819cc8a7e99c25d9400b8414a0688b6ebf8ac47b423267fa86cff8bdef6095934fa151dbf07b7a861fd5e4400557247b0278df25b4c865b3c8a067f9b40693ef75f31e680030a2eaf25cd0a01b841b1158d5d63fe8f080ff55d136e3eb07b7adf3f1fac409d541599a1eb1111c4011b62649875f724509de431ff61cd80e700cc5cfa317b9e33df67e65aa6d5cecf00b84198fc2f6d8695775685b80c68562ffb2e487389fc5b839d5c95c365426b58bf812c6f1eb33baa775291ad284e02ddb11dd5e9476fdf2ba1f6bdd4ea722ef6dcb200",
            "f901edf901eaf901e701b901e3f901e0b8c9f8c73200a0f15b61d91392ddcb9fb60beb138a8a1fff821be0371d3c20f9caa063b9df8e3ae3e200a0d33456b7b455a6381b4e523716b33f9593d3dfe00c7219a3656c983ed99ab8a90409a08c46a84b0bad8d2ee375d1fb666256ef0abb7bfcf208b0e6e9a83718e2dda4c800f800b858f856f85494f28a5305dbe556fd0103596aca9129d730a916b89442aa62a65b7d03ed68a06b95ab3445d6af294be99433a9779f75b36f372a4a6600579b9a9959e4568794c9ae50b4423509c64dd74aee98ee2aa0e0fed323b90112f9010ff9010cb841e6d82ebc01e10cce3d6867c6caa2337f8eb487efec4fb00f81aa1c79158f218455945df9705d5fbccd0c14990d3d36db83085b13c81a0d5d63c1710bc51f965e01b84119cfcaa2ae283d6eefc52e9fec010ef53a8e4b4aa853a9ed6a026f58c930a937745cf1b21202c50865cc85a2bc505a307df7f6e3a107993d35eed249377d1d2a01b841c807ddaf5cef5ddf7688c32f6eed930fbf9f79893a0bcc355472e4f904103c0e0c6643c6e860249a068f8754d75a8f90d8dccd20a0cbb0899882d2bd3edd03b200b841ed78341a0f2ad318a3fff4f2e118c25e7a70cd6579de184a3c2817120ef32cc228f610348252c047b6192f86fd79fba9a39afe4e51ad9d205867918c6a45feca00"
    );

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        IconService iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
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
        positiveCase(SUCCESS_RELAY_MESSAGE1, FIRST_BLOCK_HEADER1, new long[]{0, 1, 4, 4, 7});
        positiveCase(SUCCESS_RELAY_MESSAGE2, FIRST_BLOCK_HEADER2, new long[]{0, 2, 4});
    }

    @Order(2)
    @Test
    public void scenario2() throws TransactionFailureException, IOException, ResultTimeoutException {
        bmvScore = BMVScore.mustDeploy(txHandler, ownerWallet, srcNetworkID, networkTypeID, bmcScore.getAddress(), FAIL_CASE_FIRST_BLOCK_HEADER, BigInteger.ZERO);
        var validMsg = "cecdcc028ac9f800c483646f67f800";
        String otherBMC = makeBTPAddress(otherNetworkBMC.getAddress());
        var txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), otherBMC, BigInteger.ZERO, StringUtil.hexToBytes(validMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");
    }

    @Order(3)
    @Test
    public void scenario3() throws IOException, ResultTimeoutException {
        var blockUpdateMsg = "f9020cf90209f9020601b90202f901ffb8e8f8e61400a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494911ac74dd9ff8f4cdd91e747afcfdc9410a926e99497e36fb88560a3023c509704801eb1149acecf4394a9b0a74b2b63ab9cd20c6e38c88195c8175beb4694432b6448f3471aef190819b3c4f549a3a689d83ab90112f9010ff9010cb8411c8e1c4e89ea6f2a22c06d49984d8088f40b5d8bc1ff547d790df8374454f9ff73f4324168cb68a1c78d74ee05a2cccb1a471c26c3dcd05af4c21241d31a8fd301b8411f915ae7f047db8fee3fec42882a1747aa898c5149e8751e479235f33779fd414f52c3a63d0dd24953f3e8ed77eb06965410c2c365f669abc74d0239dbc7052a01b841d21638f8aee5194920df53652adc906f66944044e1d9176e45b9d3d80d010ded7967ff4dd944b6f9214794f0c967529663eeda3d3b51c2b7bcd56ff8f6a41d3800b8416be3ab56807f762f262a9feb819aa1aac1e83330e1568764bb9e766519135fb64f72adef323187c700e6b30b3a44e766ecb8885f1290074c43f7bd3bb06f56dc01";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ZERO, StringUtil.hexToBytes(blockUpdateMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var remainMessage = "cecdcc028ac9f800c483646f67f800";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ZERO, StringUtil.hexToBytes(remainMessage));
        txResult = txHandler.getResult(txHash);
        assertSuccess(txResult);

        var invalidNidMsg = "f9020cf90209f9020601b90202f901ffb8e8f8e61400a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0303a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494911ac74dd9ff8f4cdd91e747afcfdc9410a926e99497e36fb88560a3023c509704801eb1149acecf4394a9b0a74b2b63ab9cd20c6e38c88195c8175beb4694432b6448f3471aef190819b3c4f549a3a689d83ab90112f9010ff9010cb8411c8e1c4e89ea6f2a22c06d49984d8088f40b5d8bc1ff547d790df8374454f9ff73f4324168cb68a1c78d74ee05a2cccb1a471c26c3dcd05af4c21241d31a8fd301b8411f915ae7f047db8fee3fec42882a1747aa898c5149e8751e479235f33779fd414f52c3a63d0dd24953f3e8ed77eb06965410c2c365f669abc74d0239dbc7052a01b841d21638f8aee5194920df53652adc906f66944044e1d9176e45b9d3d80d010ded7967ff4dd944b6f9214794f0c967529663eeda3d3b51c2b7bcd56ff8f6a41d3800b8416be3ab56807f762f262a9feb819aa1aac1e83330e1568764bb9e766519135fb64f72adef323187c700e6b30b3a44e766ecb8885f1290074c43f7bd3bb06f56dc01";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidNidMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var invalidFirstSNMsg = "f9020cf90209f9020601b90202f901ffb8e8f8e61400a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0205a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494911ac74dd9ff8f4cdd91e747afcfdc9410a926e99497e36fb88560a3023c509704801eb1149acecf4394a9b0a74b2b63ab9cd20c6e38c88195c8175beb4694432b6448f3471aef190819b3c4f549a3a689d83ab90112f9010ff9010cb8411c8e1c4e89ea6f2a22c06d49984d8088f40b5d8bc1ff547d790df8374454f9ff73f4324168cb68a1c78d74ee05a2cccb1a471c26c3dcd05af4c21241d31a8fd301b8411f915ae7f047db8fee3fec42882a1747aa898c5149e8751e479235f33779fd414f52c3a63d0dd24953f3e8ed77eb06965410c2c365f669abc74d0239dbc7052a01b841d21638f8aee5194920df53652adc906f66944044e1d9176e45b9d3d80d010ded7967ff4dd944b6f9214794f0c967529663eeda3d3b51c2b7bcd56ff8f6a41d3800b8416be3ab56807f762f262a9feb819aa1aac1e83330e1568764bb9e766519135fb64f72adef323187c700e6b30b3a44e766ecb8885f1290074c43f7bd3bb06f56dc01";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidFirstSNMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + NOT_VERIFIABLE + ")");

        var invalidPrevHashMsg = "f9020cf90209f9020601b90202f901ffb8e8f8e61400a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0000000000000000000000000000000000000000000000000000000000000000003a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494911ac74dd9ff8f4cdd91e747afcfdc9410a926e99497e36fb88560a3023c509704801eb1149acecf4394a9b0a74b2b63ab9cd20c6e38c88195c8175beb4694432b6448f3471aef190819b3c4f549a3a689d83ab90112f9010ff9010cb8411c8e1c4e89ea6f2a22c06d49984d8088f40b5d8bc1ff547d790df8374454f9ff73f4324168cb68a1c78d74ee05a2cccb1a471c26c3dcd05af4c21241d31a8fd301b8411f915ae7f047db8fee3fec42882a1747aa898c5149e8751e479235f33779fd414f52c3a63d0dd24953f3e8ed77eb06965410c2c365f669abc74d0239dbc7052a01b841d21638f8aee5194920df53652adc906f66944044e1d9176e45b9d3d80d010ded7967ff4dd944b6f9214794f0c967529663eeda3d3b51c2b7bcd56ff8f6a41d3800b8416be3ab56807f762f262a9feb819aa1aac1e83330e1568764bb9e766519135fb64f72adef323187c700e6b30b3a44e766ecb8885f1290074c43f7bd3bb06f56dc01";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidPrevHashMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");
    }

    @Order(4)
    @Test
    public void scenario4() throws IOException, ResultTimeoutException {
        var duplicatedSignatureMsg = "f9020cf90209f9020601b90202f901ffb8e8f8e61400a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494911ac74dd9ff8f4cdd91e747afcfdc9410a926e99497e36fb88560a3023c509704801eb1149acecf4394a9b0a74b2b63ab9cd20c6e38c88195c8175beb4694432b6448f3471aef190819b3c4f549a3a689d83ab90112f9010ff9010cb8411c8e1c4e89ea6f2a22c06d49984d8088f40b5d8bc1ff547d790df8374454f9ff73f4324168cb68a1c78d74ee05a2cccb1a471c26c3dcd05af4c21241d31a8fd301b8411f915ae7f047db8fee3fec42882a1747aa898c5149e8751e479235f33779fd414f52c3a63d0dd24953f3e8ed77eb06965410c2c365f669abc74d0239dbc7052a01b8411c8e1c4e89ea6f2a22c06d49984d8088f40b5d8bc1ff547d790df8374454f9ff73f4324168cb68a1c78d74ee05a2cccb1a471c26c3dcd05af4c21241d31a8fd301b8411c8e1c4e89ea6f2a22c06d49984d8088f40b5d8bc1ff547d790df8374454f9ff73f4324168cb68a1c78d74ee05a2cccb1a471c26c3dcd05af4c21241d31a8fd301";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(duplicatedSignatureMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var proofNullMsg = "f8f5f8f3f8f101b8eef8ecb8e8f8e61400a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494911ac74dd9ff8f4cdd91e747afcfdc9410a926e99497e36fb88560a3023c509704801eb1149acecf4394a9b0a74b2b63ab9cd20c6e38c88195c8175beb4694432b6448f3471aef190819b3c4f549a3a689d83af800";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(proofNullMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "UnknownFailure");
    }

    @Order(5)
    @Test
    public void scenario5() throws IOException, ResultTimeoutException {
        var hashMismatchMsg = "f901b5f901b2f901af01b901abf901a8b891f88f1400a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd2682c1c0b90112f9010ff9010cb8411c8e1c4e89ea6f2a22c06d49984d8088f40b5d8bc1ff547d790df8374454f9ff73f4324168cb68a1c78d74ee05a2cccb1a471c26c3dcd05af4c21241d31a8fd301b8411f915ae7f047db8fee3fec42882a1747aa898c5149e8751e479235f33779fd414f52c3a63d0dd24953f3e8ed77eb06965410c2c365f669abc74d0239dbc7052a01b841d21638f8aee5194920df53652adc906f66944044e1d9176e45b9d3d80d010ded7967ff4dd944b6f9214794f0c967529663eeda3d3b51c2b7bcd56ff8f6a41d3800b8416be3ab56807f762f262a9feb819aa1aac1e83330e1568764bb9e766519135fb64f72adef323187c700e6b30b3a44e766ecb8885f1290074c43f7bd3bb06f56dc01";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(hashMismatchMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");
    }

    @Order(6)
    @Test
    public void scenario6() throws IOException, ResultTimeoutException {
        var proofMessageMsg = "dcdbda0298d7f800d28363617488656c657068616e748462697264f800";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(proofMessageMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        // make remain count 2
        var validBlockUpdate = "f9020cf90209f9020601b90202f901ffb8e8f8e61400a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494911ac74dd9ff8f4cdd91e747afcfdc9410a926e99497e36fb88560a3023c509704801eb1149acecf4394a9b0a74b2b63ab9cd20c6e38c88195c8175beb4694432b6448f3471aef190819b3c4f549a3a689d83ab90112f9010ff9010cb8411c8e1c4e89ea6f2a22c06d49984d8088f40b5d8bc1ff547d790df8374454f9ff73f4324168cb68a1c78d74ee05a2cccb1a471c26c3dcd05af4c21241d31a8fd301b8411f915ae7f047db8fee3fec42882a1747aa898c5149e8751e479235f33779fd414f52c3a63d0dd24953f3e8ed77eb06965410c2c365f669abc74d0239dbc7052a01b841d21638f8aee5194920df53652adc906f66944044e1d9176e45b9d3d80d010ded7967ff4dd944b6f9214794f0c967529663eeda3d3b51c2b7bcd56ff8f6a41d3800b8416be3ab56807f762f262a9feb819aa1aac1e83330e1568764bb9e766519135fb64f72adef323187c700e6b30b3a44e766ecb8885f1290074c43f7bd3bb06f56dc01";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(validBlockUpdate));
        txResult = txHandler.getResult(txHash);
        assertSuccess(txResult);

        var mismatchLeftNumMsg = "f83cf83af83802b6f5e3e201a052763589e772702fa7977a28b3cfb6ca534f0208a2b2d55f7558af664eac478ace88656c657068616e748462697264f800";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(mismatchLeftNumMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var invalidNumOfLeafMsg = "f842f840f83e02b83bf839e3e203a0468412432735e704136dcef80532ffc5db671fd0361b59f77d1462bcb83995e9d28363617488656c657068616e748462697264f800";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidNumOfLeafMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var invalidLevelMsg = "f0efee02acebf800c483636174e3e202a0468412432735e704136dcef80532ffc5db671fd0361b59f77d1462bcb83995e9";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidLevelMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var mismatchCountMsg = "f856f854f85202b84ff84df800f847b84052763589e772702fa7977a28b3cfb6ca534f0208a2b2d55f7558af664eac478a468412432735e704136dcef80532ffc5db671fd0361b59f77d1462bcb83995e98462697264f800";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(mismatchCountMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var mismatchRootMsg = "f85ff85df85b02b858f856f800f850b84452763589e772702fa7977a28b3cfb6ca534f0208a2b2d55f7558af664eac478a468412432735e704136dcef80532ffc5db671fd0361b59f77d1462bcb83995e97465737484626972648462697264f800";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(mismatchRootMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");
    }

    private void positiveCase(List<String> msgList, byte[] blockHeader, long[] seqs) throws TransactionFailureException, IOException, ResultTimeoutException {
        // Deploy BMV
        bmvScore = BMVScore.mustDeploy(txHandler, ownerWallet, srcNetworkID, networkTypeID, bmcScore.getAddress(), blockHeader, BigInteger.ZERO);

        var msgLength = msgList.size();
        var hashes = new Bytes[msgLength];
        for (int i = 0; i < msgLength; i++) {
            hashes[i] = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), makeBTPAddress(prevBmCScore.getAddress()), BigInteger.valueOf(seqs[i]), StringUtil.hexToBytes(msgList.get(i)));
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
