package foundation.icon.btp.lib.mpt;

import java.util.Arrays;

import foundation.icon.btp.lib.mpt.MPTNode;
import foundation.icon.btp.lib.utils.HexConverter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;
import score.Context;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

// @Disabled	
@TestMethodOrder(OrderAnnotation.class)
class MPTNodeTest {
    // @BeforeAll
    // public static void setup() {
    // }

    @Test
    @Order(1)
    public void initializedBranchNodeWithoutValueAndNibbles() {
        byte[] serialized = HexConverter.hexStringToByteArray("80ffff801ee4115c4d5894ff9b5e6bb83611e2a8a8503482fe35a3f086d500fd1f1119618088e994a9533991b66e5d837744f5be103692c64b08ea63d63abe8eaf9fb7416f8060758d475a9259fa27c831fd9ebd23bafeb33d25badffb8f86ac80f311acb593803b98133180f6aed60e8423fffa8c033c481d94e0ee9ddacc6c8d302d4cde9786806af5c4f477504b627b1af9c106960aefe0e0a0632386b542897157486e0145a480428917537502720167a40e6b6124a29b3769d45ab7fd5525e2801e7067dfbbac800eb754c27d6302344f80fc4f785eae09c7c6acf58ee0ebddbd2f1755eb37a7de80487b2dc7bef5a919013bf34dfca66bbaae571ebedbdb0eeb0c8149dfa0d3545c80b3b0100693e0bdb4ce7a61e395a504aebc2e3fe02c8d184b4b7711182864c8aa80a4160361b01b32b8d402e9386b1f97e2c01b963aa1a1281f1802ed386892495080b54561a0ca6f56c484ed434f3cdd323985e74088f456e6c32ed47bd65dd7269f80a29da6bb7b31bd16f291157e07f2cd4de7e8daf8a2fdfcdc5abfd828110e697c8050375ff3701450c89230e01b2460aeb3de4d8b3d782d6b70ca2b7642f74450ec80b911d65bff5f4ce6c0520d83d55b8ee182c7e87664f67664849276b731b45117801c6d0738400fc6e6e66e28c80f63837a790bb5d58559ec858b82af37b52e4c818060ce44be0abff4ce2bb1065fac78836b7b4276fae13d916fe84c60b54533123c");
        MPTNode mptNode = spy(new MPTNode(serialized));

        assertEquals(mptNode.getType(), MPTNodeType.BRANCH);
        assertEquals(mptNode.getNibbles(), null);
        assertArrayEquals(mptNode.getHash(), Context.hash("blake2b-256", serialized));

        byte[][] decodedChildrens = mptNode.getChildrens();
        assertEquals(decodedChildrens.length, 16);
        assertEquals(decodedChildrens[0].length, 32);
        assertEquals(decodedChildrens[15].length, 32);

        assertTrue(mptNode.getData() == null);
    }

    @Test
    @Order(2)
    public void initializedBranchNodeWithNibblesAndWithoutValue() {
        byte[] serialized = HexConverter.hexStringToByteArray("9eaa394eea5630e07c48ae0c9558cef7299f805d6230a65d4fa930e02ff40a3b457bfcf20e2b5641b6d15e9056bc438742719980eee808e9dd713935c5f10f716cb1b2952f5bff9afe639f9a89248f6544b7def44c5f0684a022a34dd8bfa2baaf44f172b710040180faf3b170fe418b88973c2286677767f3b09ed5e3fc3f79687520ce68e9b67d05808d5b41c21e2ed4daa1ade60726d08086674726fe4dc500b65f69c1ff7c97e6b58047e43c6b5ec576398a1a9eecddf19bc92bbddf9f6f6c195b347a0aeb06c7887280f7eeac1afaad9023e4811ed99731eb9cd89afd618ada2b44941d2e1d53933a704c5f021aab032aaa6e946ca50ad39ab666030401705f09cce9c888469bb1a0dceaa129672ef8287420706f6c6b61646f74");
        byte[] hash = Context.hash("blake2b-256", serialized);
        MPTNode mptNode = spy(new MPTNode(serialized));

        assertEquals(mptNode.getType(), MPTNodeType.BRANCH);
        assertArrayEquals(mptNode.getNibbles().getRaw(), HexConverter.hexStringToByteArray("aa394eea5630e07c48ae0c9558cef7"));
        assertArrayEquals(mptNode.getHash(), hash);

        byte[][] decodedChildrens = mptNode.getChildrens();
        assertEquals(decodedChildrens.length, 16);
        assertArrayEquals(decodedChildrens[0], HexConverter.hexStringToByteArray("5d6230a65d4fa930e02ff40a3b457bfcf20e2b5641b6d15e9056bc4387427199"));
        assertArrayEquals(decodedChildrens[3], HexConverter.hexStringToByteArray("eee808e9dd713935c5f10f716cb1b2952f5bff9afe639f9a89248f6544b7def4"));
        assertArrayEquals(decodedChildrens[5], HexConverter.hexStringToByteArray("5f0684a022a34dd8bfa2baaf44f172b7100401"));
        assertArrayEquals(decodedChildrens[15], HexConverter.hexStringToByteArray("5f09cce9c888469bb1a0dceaa129672ef8287420706f6c6b61646f74"));

        assertTrue(mptNode.getData() == null);
    }

    @Test
    @Order(3)
    public void initializedBranchNodeWithNibblesAndValue() {
        byte[] serialized = HexConverter.hexStringToByteArray("deaa394eea5630e07c48ae0c9558cef7299f1001230a00805d6230a65d4fa930e02ff40a3b457bfcf20e2b5641b6d15e9056bc438742719980eee808e9dd713935c5f10f716cb1b2952f5bff9afe639f9a89248f6544b7def44c5f0684a022a34dd8bfa2baaf44f172b710040180faf3b170fe418b88973c2286677767f3b09ed5e3fc3f79687520ce68e9b67d05808d5b41c21e2ed4daa1ade60726d08086674726fe4dc500b65f69c1ff7c97e6b58047e43c6b5ec576398a1a9eecddf19bc92bbddf9f6f6c195b347a0aeb06c7887280f7eeac1afaad9023e4811ed99731eb9cd89afd618ada2b44941d2e1d53933a704c5f021aab032aaa6e946ca50ad39ab666030401705f09cce9c888469bb1a0dceaa129672ef8287420706f6c6b61646f74");
        byte[] hash = Context.hash("blake2b-256", serialized);
        MPTNode mptNode = spy(new MPTNode(serialized));

        assertEquals(mptNode.getType(), MPTNodeType.BRANCH_WITH_VALUE);
        assertArrayEquals(mptNode.getNibbles().getRaw(), HexConverter.hexStringToByteArray("aa394eea5630e07c48ae0c9558cef7"));
        assertArrayEquals(mptNode.getHash(), hash);

        byte[][] decodedChildrens = mptNode.getChildrens();
        assertEquals(decodedChildrens.length, 16);
        assertArrayEquals(decodedChildrens[0], HexConverter.hexStringToByteArray("5d6230a65d4fa930e02ff40a3b457bfcf20e2b5641b6d15e9056bc4387427199"));
        assertArrayEquals(decodedChildrens[3], HexConverter.hexStringToByteArray("eee808e9dd713935c5f10f716cb1b2952f5bff9afe639f9a89248f6544b7def4"));
        assertArrayEquals(decodedChildrens[5], HexConverter.hexStringToByteArray("5f0684a022a34dd8bfa2baaf44f172b7100401"));
        assertArrayEquals(decodedChildrens[15], HexConverter.hexStringToByteArray("5f09cce9c888469bb1a0dceaa129672ef8287420706f6c6b61646f74"));

        assertArrayEquals(mptNode.getData(), HexConverter.hexStringToByteArray("01230a00"));
    }

    @Test
    @Order(4)
    public void initializedLeafNodeWithNibbles() {
        byte[] serialized = HexConverter.hexStringToByteArray("5f04abf5cb34d6244378cddbf18e849d9660000000000000000000000000000000009073a47e01000000");
        byte[] hash = Context.hash("blake2b-256", serialized);
        MPTNode mptNode = spy(new MPTNode(serialized));

        assertEquals(mptNode.getType(), MPTNodeType.LEAF);
        assertArrayEquals(mptNode.getNibbles().getRaw(), HexConverter.hexStringToByteArray("04abf5cb34d6244378cddbf18e849d96"));
        assertArrayEquals(mptNode.getHash(), hash);

        byte[][] decodedChildrens = mptNode.getChildrens();
        assertEquals(decodedChildrens, null);

        assertArrayEquals(mptNode.getData(), HexConverter.hexStringToByteArray("000000000000000000000000000000009073a47e01000000"));
    }

    @Test
    @Order(5)
    public void initializedLeafNodeWithoutNibbles() {
        byte[] serialized = HexConverter.hexStringToByteArray("408928450148db5df0d3ca31fc34372efe1e2bbcfef9300cb83a45caaad87d8e7de6c8d57ce9720ea305f54dddc749807e1ab8c6a1870209d0635f6246288fe412feeae511587f7a3311bf8bec0f6851639c04a1a6d68cdb3905f33abdb3b891d2102f4878801026e09dada5e8a0e437775eda1f9890e12ebae2ecb9b9aab21c4b042e77a87b9e64bfd4bb9e1afc9dcabed7bc37ce60fc9568edb5a99aad82904349be3dc29149362adbc390496e90c3b4a96352c7af91e3915344cdff06d91b36f18c4cf3805cbd5fe0c56b4b7a57d37f3747f85616be45ac5c7423630ff495e577093aa2f63b1a033477435303437cd9b13e0ec1291e35bdddcee3b59a9f3a70f66e256687872bceca9ce75e9d36aba390ffa76ce22b71fb86208ca0b81101abc7e335ffd507d6d512c13b09c27f6b1aeddea1ff0f8172ab4ed4a228226468806087a310fb642dc2c16ee4672cd922ca845d4b8fbf2b00a282c089fbfc33dfb211581f5ee0ac49d7a9e9c4b032b69627a7b23a2169d9bb733192d153cf41386ebdff471804e45e64afaf1f73de8ebbb7b1dde373276da9bac76916634252cae441cf3b39c51e2a048fdda1f135d8ac78d7153be643fc6b81f69fdab03a13b5e57c52b13ee8d39bcc65face95c2b7b134e972b934cbf32fe4ae6ed9da8ad4bb9128e997378a353cb4982156f6c5cfb09061f590ea5bcce785d283d0dacfeb3a168f481c456e580df10ad96e571e91148d149a592037f1ee1e5c0a067df6994b9c1118b9efc18db86367c3291d73c8a249ace6034c1cc68ad6537ff235ab555e2e045170da709f94e324eafd1de2a45b8ee3abe2602adc13cb0327ed5c09f4b2ff593716be0359e9ff9e9f13605e05fdbdc6b5806ac270a34f1007e33844b9aa1bf5c26199e15e983ce2ca98830bc3823f9b73bcd563a647b229bf9b1360a97a4d0b5e7b0a6bbbcc11225894363b27b4849a2e8f4df79ed6255eb59d94d0d9a189f4c2adb05edfa8edf9b167ec94cb1fdd47209ad8cba16de2df337bc105a48384a2208434858ebb7716e1b559b23d159a55af2cf17fccecd13d97aa1382d4fa82e609fc2ed9d129184f099e82996e8b0b94453fdd2e9efc5c020cbddd94cebe92bd573bf2cd4e3d7bd91d4cd11d10ace6ca356278cda0eab293b7639051c7a65571e272af8317ad37b49a7d876631aea5171d3e9311b8ee36f6654d06b39cb2ab274218ce45004eb60f57c9cee3edffab5b70391dcf6f9cf8b51bd3d55eab9ac41bd60a6d52bf31edf396b2924811927222e365a420e56ffc78b6e0a00cb03b0743d5201c0665bda06fd091844cb6b2b0b87b0720e28c2d7656dfa29a3a5f6a67aa900fdc27884379afd8af9e258145822725acb483c42ae2645045d232ff48c7a060cf058641167fc656e6d8c8941311191b8369f496fc563b4c4f7736d1cdacaee6edc9accaeb090f331397c8d4bcddaa692ee8c087e869e1611ef609f3dfa8239f98a907f0790afc246e5343f9c3f04523f8250a9e92fec802ba62f247087242511ee78b2df4e03dba3a89d0960b9c1cf924a56f2230c29cc922adbb035db398c92baee12aadaf1a20c8e654d6ac79d881772d0f20b8d7baa578f3c991834a9d09664799f87c3b100325d536570b4368f42173321dbb40b341ec242f19d6d43c2f7a78560274048ec0bb23cbf13ec6ef0603d700ac036e7dfafe6b06d3729ea80ef54fd422b94f6f9f7b51ce6de67d1d8630652d18af1ddaa776ec3602627853af7fcdebf2f356a579d535dbef0f0db5e64af49a4de047500308baaed40c71a760f100d7742eefbb85942b73710ae6bea046725caae1d618c93ffdee508fb1f58a27ca3038b772d3d3c8cabe77ccedaf7b923b90bfad2a0faa03af45fddffcb6393a811659860ffb9dfc0abca7d17665a2f61fdb013c15c2dad838a62ae10525d682f5078d32976202aadec04dd58871ac29abb72d19e2eab8453de85585467212d69b4fe07f4cedcc7d2c437b73101d86baf5a6b16f22b65487510f4d56c63172434d11c8f4f9828cab415f5cfa93f23c249c59e481b585e5fc3305b9010f45ee8787c95eae3ff4b220772c92d622fc375ebda31ec1a8da92b2dcf1d103895803df89608b92e5ce97d54359fb7d373a89c4b8fea79964def152d0023c9c2283ef7abe6337f33466e340628e66a32d1643bd6c05fcf3c3336b4755aa3dc20ce0ffd97c21ef825ff2e028dd66c08e9e958efa33d4c24c5195035866ee4e3b2f4563fdc23c5c5bca6f69ded059956910bdf8e5871455930ef3212e4b76d4c9e8eeede7f6050b46184e1485d60dbddc1e38c9687dad0e67d315cb04a296f56ab391821e6fc9d57c851c759961fa611025ba50d0dca92189e041fbd82b71a2340909b65546e2184b5f648a3a1e1bc474e76a7992dca01b0cf76ea70e9156171203f64a880ad5a25494fa8dd3fc88aa96a742ad077a8b6f7495548e9f8bb35991ebb0dd9e30352b8cb8ceb38306841de27c2244a1bda77473a4c7f807b03cba6b41df4256c5821c7f087ae3964ce9de0fe19c02b595a429b86c261f97aac5e72ca884c587d1bfbe6a01b7c2d925927edfa27d044a728a17093e0b1aa2c20e2cdbd19830f12141a2e76a36a1e3ce69fd98b9d97d88509166eb941190d3bbdcf36d12f5c07b4e343d90274f1cb14551e20a429393f789af87156d10598f77928e495902fcfda9cfd64a71224e70c289071bc2e495db071ad409d6c6f6a54d58534d63ab61f181c97fd4ee4dd6628387e3ec1024e40c1a74d493b3a259e910b87e92ac9fcc31907ba328b2478757ab787a590b516426955d072c254fe6cb3002f3b6661bbc95f7d304da167ed14feca431e7190c7d4c3f547f65c602f823ef1d2d5dab6220ec235bfedec3041e818993f9da0b69fc0b80f863f99661a1b29f7c3eaf98f6afe32c5ff45881171b4b972c9c7dbfc264978bc615352c46939c217433e31b74d54427edb5994afef4dd890c4bada7e74ba4f80e7d98984129d7d8d1fe96285b14aed5e9ac591aa021b8db8de77ee7424e372e250815c0225aa5c16e56e1c39510f22b7335da1cd1c985f4472dc46517df8a47a3e799becf3ff8ddb852ba459bdf928e5159c9ef7924b98bbc90d01495ea17e8112c146b421ebfb43d4396da76ada605199b1b649195d32141f2d9dde33400e499cfff64b086a8367ec051d4e9bf3066fd4e8f8cd30afffc1d03d125147a34f07cc72e6a85e42c2d76680c4ccfa75e343ce26dbeeefd60aeda06374e0984bcffc39c378faa954d033c8ebc6073f2c7754a27406b51b4b7befb5cd61f6ae93b526f25d8de5bc260a23558cecfac9f8af2272fe29b6ee07ec14afe70c3d71c8fd8e41300f992d8d30b767f1f974fcc050826a7de965321b5b6b5845d82224c910a04e80f80d27974512cdfd89d02b9c9c2b4640e7f81e02f99a9189747d0b7c2fe1a6abb91aeb588de9d8e6c0245cab75499679c2e521c96a2db9f7257b30a06919845c275a19df563a378fd09844c56580fc84043ea0d8b19708fcc039ea4cc6f3db2ef3e85be9152e02378e668bf19fa9984c37d8c7e5c3e64edbccef27d33ff278a66eca6acdfd2ddc411e044990e92d564073b89057482ab06547412e8d3848e15a53683782d19b393909f5b20e3a1");
        byte[] hash = Context.hash("blake2b-256", serialized);
        MPTNode mptNode = spy(new MPTNode(serialized));

        assertEquals(mptNode.getType(), MPTNodeType.LEAF);
        assertEquals(mptNode.getNibbles(), null);
        assertArrayEquals(mptNode.getHash(), hash);

        byte[][] decodedChildrens = mptNode.getChildrens();
        assertEquals(decodedChildrens, null);

        assertArrayEquals(mptNode.getData(), HexConverter.hexStringToByteArray("450148db5df0d3ca31fc34372efe1e2bbcfef9300cb83a45caaad87d8e7de6c8d57ce9720ea305f54dddc749807e1ab8c6a1870209d0635f6246288fe412feeae511587f7a3311bf8bec0f6851639c04a1a6d68cdb3905f33abdb3b891d2102f4878801026e09dada5e8a0e437775eda1f9890e12ebae2ecb9b9aab21c4b042e77a87b9e64bfd4bb9e1afc9dcabed7bc37ce60fc9568edb5a99aad82904349be3dc29149362adbc390496e90c3b4a96352c7af91e3915344cdff06d91b36f18c4cf3805cbd5fe0c56b4b7a57d37f3747f85616be45ac5c7423630ff495e577093aa2f63b1a033477435303437cd9b13e0ec1291e35bdddcee3b59a9f3a70f66e256687872bceca9ce75e9d36aba390ffa76ce22b71fb86208ca0b81101abc7e335ffd507d6d512c13b09c27f6b1aeddea1ff0f8172ab4ed4a228226468806087a310fb642dc2c16ee4672cd922ca845d4b8fbf2b00a282c089fbfc33dfb211581f5ee0ac49d7a9e9c4b032b69627a7b23a2169d9bb733192d153cf41386ebdff471804e45e64afaf1f73de8ebbb7b1dde373276da9bac76916634252cae441cf3b39c51e2a048fdda1f135d8ac78d7153be643fc6b81f69fdab03a13b5e57c52b13ee8d39bcc65face95c2b7b134e972b934cbf32fe4ae6ed9da8ad4bb9128e997378a353cb4982156f6c5cfb09061f590ea5bcce785d283d0dacfeb3a168f481c456e580df10ad96e571e91148d149a592037f1ee1e5c0a067df6994b9c1118b9efc18db86367c3291d73c8a249ace6034c1cc68ad6537ff235ab555e2e045170da709f94e324eafd1de2a45b8ee3abe2602adc13cb0327ed5c09f4b2ff593716be0359e9ff9e9f13605e05fdbdc6b5806ac270a34f1007e33844b9aa1bf5c26199e15e983ce2ca98830bc3823f9b73bcd563a647b229bf9b1360a97a4d0b5e7b0a6bbbcc11225894363b27b4849a2e8f4df79ed6255eb59d94d0d9a189f4c2adb05edfa8edf9b167ec94cb1fdd47209ad8cba16de2df337bc105a48384a2208434858ebb7716e1b559b23d159a55af2cf17fccecd13d97aa1382d4fa82e609fc2ed9d129184f099e82996e8b0b94453fdd2e9efc5c020cbddd94cebe92bd573bf2cd4e3d7bd91d4cd11d10ace6ca356278cda0eab293b7639051c7a65571e272af8317ad37b49a7d876631aea5171d3e9311b8ee36f6654d06b39cb2ab274218ce45004eb60f57c9cee3edffab5b70391dcf6f9cf8b51bd3d55eab9ac41bd60a6d52bf31edf396b2924811927222e365a420e56ffc78b6e0a00cb03b0743d5201c0665bda06fd091844cb6b2b0b87b0720e28c2d7656dfa29a3a5f6a67aa900fdc27884379afd8af9e258145822725acb483c42ae2645045d232ff48c7a060cf058641167fc656e6d8c8941311191b8369f496fc563b4c4f7736d1cdacaee6edc9accaeb090f331397c8d4bcddaa692ee8c087e869e1611ef609f3dfa8239f98a907f0790afc246e5343f9c3f04523f8250a9e92fec802ba62f247087242511ee78b2df4e03dba3a89d0960b9c1cf924a56f2230c29cc922adbb035db398c92baee12aadaf1a20c8e654d6ac79d881772d0f20b8d7baa578f3c991834a9d09664799f87c3b100325d536570b4368f42173321dbb40b341ec242f19d6d43c2f7a78560274048ec0bb23cbf13ec6ef0603d700ac036e7dfafe6b06d3729ea80ef54fd422b94f6f9f7b51ce6de67d1d8630652d18af1ddaa776ec3602627853af7fcdebf2f356a579d535dbef0f0db5e64af49a4de047500308baaed40c71a760f100d7742eefbb85942b73710ae6bea046725caae1d618c93ffdee508fb1f58a27ca3038b772d3d3c8cabe77ccedaf7b923b90bfad2a0faa03af45fddffcb6393a811659860ffb9dfc0abca7d17665a2f61fdb013c15c2dad838a62ae10525d682f5078d32976202aadec04dd58871ac29abb72d19e2eab8453de85585467212d69b4fe07f4cedcc7d2c437b73101d86baf5a6b16f22b65487510f4d56c63172434d11c8f4f9828cab415f5cfa93f23c249c59e481b585e5fc3305b9010f45ee8787c95eae3ff4b220772c92d622fc375ebda31ec1a8da92b2dcf1d103895803df89608b92e5ce97d54359fb7d373a89c4b8fea79964def152d0023c9c2283ef7abe6337f33466e340628e66a32d1643bd6c05fcf3c3336b4755aa3dc20ce0ffd97c21ef825ff2e028dd66c08e9e958efa33d4c24c5195035866ee4e3b2f4563fdc23c5c5bca6f69ded059956910bdf8e5871455930ef3212e4b76d4c9e8eeede7f6050b46184e1485d60dbddc1e38c9687dad0e67d315cb04a296f56ab391821e6fc9d57c851c759961fa611025ba50d0dca92189e041fbd82b71a2340909b65546e2184b5f648a3a1e1bc474e76a7992dca01b0cf76ea70e9156171203f64a880ad5a25494fa8dd3fc88aa96a742ad077a8b6f7495548e9f8bb35991ebb0dd9e30352b8cb8ceb38306841de27c2244a1bda77473a4c7f807b03cba6b41df4256c5821c7f087ae3964ce9de0fe19c02b595a429b86c261f97aac5e72ca884c587d1bfbe6a01b7c2d925927edfa27d044a728a17093e0b1aa2c20e2cdbd19830f12141a2e76a36a1e3ce69fd98b9d97d88509166eb941190d3bbdcf36d12f5c07b4e343d90274f1cb14551e20a429393f789af87156d10598f77928e495902fcfda9cfd64a71224e70c289071bc2e495db071ad409d6c6f6a54d58534d63ab61f181c97fd4ee4dd6628387e3ec1024e40c1a74d493b3a259e910b87e92ac9fcc31907ba328b2478757ab787a590b516426955d072c254fe6cb3002f3b6661bbc95f7d304da167ed14feca431e7190c7d4c3f547f65c602f823ef1d2d5dab6220ec235bfedec3041e818993f9da0b69fc0b80f863f99661a1b29f7c3eaf98f6afe32c5ff45881171b4b972c9c7dbfc264978bc615352c46939c217433e31b74d54427edb5994afef4dd890c4bada7e74ba4f80e7d98984129d7d8d1fe96285b14aed5e9ac591aa021b8db8de77ee7424e372e250815c0225aa5c16e56e1c39510f22b7335da1cd1c985f4472dc46517df8a47a3e799becf3ff8ddb852ba459bdf928e5159c9ef7924b98bbc90d01495ea17e8112c146b421ebfb43d4396da76ada605199b1b649195d32141f2d9dde33400e499cfff64b086a8367ec051d4e9bf3066fd4e8f8cd30afffc1d03d125147a34f07cc72e6a85e42c2d76680c4ccfa75e343ce26dbeeefd60aeda06374e0984bcffc39c378faa954d033c8ebc6073f2c7754a27406b51b4b7befb5cd61f6ae93b526f25d8de5bc260a23558cecfac9f8af2272fe29b6ee07ec14afe70c3d71c8fd8e41300f992d8d30b767f1f974fcc050826a7de965321b5b6b5845d82224c910a04e80f80d27974512cdfd89d02b9c9c2b4640e7f81e02f99a9189747d0b7c2fe1a6abb91aeb588de9d8e6c0245cab75499679c2e521c96a2db9f7257b30a06919845c275a19df563a378fd09844c56580fc84043ea0d8b19708fcc039ea4cc6f3db2ef3e85be9152e02378e668bf19fa9984c37d8c7e5c3e64edbccef27d33ff278a66eca6acdfd2ddc411e044990e92d564073b89057482ab06547412e8d3848e15a53683782d19b393909f5b20e3a1"));
    }

    @Test
    @Order(6)
    public void initializedInlineLeaf() {
        byte[] serialized = HexConverter.hexStringToByteArray("5f078d434d6125b40443fe11fd292d13a41003000000");
        MPTNode mptNode = spy(new MPTNode(serialized));

        assertEquals(mptNode.getType(), MPTNodeType.LEAF);
        assertArrayEquals(mptNode.getNibbles().getRaw(), HexConverter.hexStringToByteArray("078d434d6125b40443fe11fd292d13a4"));

        byte[][] decodedChildrens = mptNode.getChildrens();
        assertEquals(decodedChildrens, null);

        assertArrayEquals(mptNode.getData(), HexConverter.hexStringToByteArray("03000000"));
    }

    @Test
    @Order(7)
    public void initializedEmptyNode() {
        byte[] serialized = HexConverter.hexStringToByteArray("00");
        MPTNode mptNode = spy(new MPTNode(serialized));

        assertEquals(mptNode.getType(), MPTNodeType.EMPTY);
        assertEquals(mptNode.getNibbles(), null);

        byte[][] decodedChildrens = mptNode.getChildrens();
        assertEquals(decodedChildrens, null);

        assertArrayEquals(mptNode.getData(), null);
    }

    @Test
    @Order(11)
    public void throwErrorIfNibblePaddingInvalid() {
        byte[] serialized = HexConverter.hexStringToByteArray("9f11aa394eea5630e07c48ae0c9558cef7299f805d6230a65d4fa930e02ff40a3b457bfcf20e2b5641b6d15e9056bc438742719980eee808e9dd713935c5f10f716cb1b2952f5bff9afe639f9a89248f6544b7def44c5f0684a022a34dd8bfa2baaf44f172b710040180faf3b170fe418b88973c2286677767f3b09ed5e3fc3f79687520ce68e9b67d05808d5b41c21e2ed4daa1ade60726d08086674726fe4dc500b65f69c1ff7c97e6b58047e43c6b5ec576398a1a9eecddf19bc92bbddf9f6f6c195b347a0aeb06c7887280f7eeac1afaad9023e4811ed99731eb9cd89afd618ada2b44941d2e1d53933a704c5f021aab032aaa6e946ca50ad39ab666030401705f09cce9c888469bb1a0dceaa129672ef8287420706f6c6b61646f74");

        AssertionError thrown = assertThrows(
            AssertionError.class,
           () -> new MPTNode(serialized)
        );

        assertTrue(thrown.getMessage().contains("invalid mpt node format"));
    }
}