package foundation.icon.btp.eventdecoder.edgeware;

import com.iconloop.testsvc.Account;
import com.iconloop.testsvc.Score;
import com.iconloop.testsvc.ServiceManager;
import com.iconloop.testsvc.TestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import score.Context;
import score.Address;

import java.nio.charset.Charset;
import java.util.Arrays;

import java.util.List;

import foundation.icon.btp.lib.utils.HexConverter;
import foundation.icon.btp.lib.eventdecoder.EventRecord;
import foundation.icon.btp.lib.eventdecoder.EventDecoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventDecoderScoreTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private static Score eventDecoderScore;

    @BeforeAll
    public static void setup() throws Exception {
        eventDecoderScore = sm.deploy(owner, EventDecoderScore.class);
    }

    @Test
    void decodeEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("14000000000000005095a209000000000200000001000000220008425d9df219f93d5763c3e85204cb5b4ce33aaa0437be353f216cf7e33639101fd610c542e6a0c0109173fa1c1d8b04d34edb7c1b01040000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000006f00000000000000000000000000000000000000000000000000000000000000c0000000000000000000000000000000000000000000000000000000000000003c6274703a2f2f3078313233342e69636f6e2f6878623662353739316265306235656636373036336233633130623834306662383135313464623266640000000000000000000000000000000000000000000000000000000000000000000000081234567890abcdef000000000000000000000000000000000000000000000000000001000000210019e7e376e7c213b7e7e7e46cc70a5dd086daff2a00000000000000000000000000000000000000008acbf6d1165628e531831be68206d877913bd527f0bd7705f9b1d3ab216c50d900000000010000000000603ad8cd0000000000000001200000a0e63e883400d92f010000000000003b0000006d6f646c70792f7472737279000000000000000000000000000000000000000000");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
    }
}
