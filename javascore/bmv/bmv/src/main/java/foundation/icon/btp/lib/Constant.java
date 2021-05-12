package foundation.icon.btp.lib;

import foundation.icon.btp.lib.utils.HexConverter;

// import io.emeraldpay.polkaj.tx.Hashing;
import score.Context;

public class Constant {
    public static final byte[] EventStorageKey = HexConverter.hexStringToByteArray("26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7");

    public static final byte[] NewAuthoritiesEventIndex = new byte[]{ (byte) 0xff, (byte) 0xff };
    public static final byte[] EvmEventIndex = new byte[]{ (byte) 0x22, (byte) 0x00 };
    public static final byte[] MessageEventTopic = HexConverter.hexStringToByteArray("37be353f216cf7e33639101fd610c542e6a0c0109173fa1c1d8b04d34edb7c1b"); // kekak256("Message(string,uint256,bytes)");
}