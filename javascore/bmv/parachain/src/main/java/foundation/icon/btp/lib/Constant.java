package foundation.icon.btp.lib.parachain;

import foundation.icon.btp.lib.utils.HexConverter;

public class Constant {
    public static final byte[] EventStorageKey = HexConverter.hexStringToByteArray("26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7");
    public static final byte[] MessageEventTopic = HexConverter.hexStringToByteArray("37be353f216cf7e33639101fd610c542e6a0c0109173fa1c1d8b04d34edb7c1b"); // kekak256("Message(string,uint256,bytes)");
}