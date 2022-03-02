package foundation.icon.btp.eventDecoder;

import com.iconloop.testsvc.Account;
import com.iconloop.testsvc.Score;
import com.iconloop.testsvc.ServiceManager;
import com.iconloop.testsvc.TestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.List;
import java.util.Map;

import foundation.icon.btp.lib.utils.HexConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

// only compatible with monbase relay metadata, comment @Disabled to test event decoder of moonbase relay chain
@Disabled
class MoonbaseRelayEventDecoderScoreTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private static Score eventDecoderScore;

    @BeforeAll
    public static void setup() throws Exception {
        eventDecoderScore = sm.deploy(owner, EventDecoderScore.class);
    }

    @Test
    void decodeSystemEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("18020000e703000000000000010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020001030b52e703000000000000010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702000204fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702000392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702000492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702000592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 6);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("e7030000000000000100"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventData"), HexConverter.hexStringToByteArray("030b52e7030000000000000100"));
        assertArrayEquals( (byte[]) eventRecords.get(3).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals( (byte[]) eventRecords.get(4).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals( (byte[]) eventRecords.get(5).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
    }

    @Test
    void decodeIndicesEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c02030092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85618076e804fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020301618076e804fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020302618076e892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85618076e8"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("0300"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventData"), HexConverter.hexStringToByteArray("618076e8"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventIndex"), HexConverter.hexStringToByteArray("0301"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventData"), HexConverter.hexStringToByteArray("618076e892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventIndex"), HexConverter.hexStringToByteArray("0302"));
    }

    @Test
    void decodeBalanceEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("2002040092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702040192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702040292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702040392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4000000000000000000000028998f5804c04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702040492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702040592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702040692341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702040792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a400104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 8);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("0400"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventIndex"), HexConverter.hexStringToByteArray("0401"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a40"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventIndex"), HexConverter.hexStringToByteArray("0402"));

        assertArrayEquals( (byte[]) eventRecords.get(7).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a4001"));
        assertArrayEquals( (byte[]) eventRecords.get(7).get("eventIndex"), HexConverter.hexStringToByteArray("0407"));
    }

    @Test
    void decodeStakingEvent1() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("2002060049a8f36300000000000000000000017cddd07a4000000000000000000000028998f5804c04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020603000f412004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060692341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 8);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("49a8f36300000000000000000000017cddd07a4000000000000000000000028998f5804c"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("0600"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventIndex"), HexConverter.hexStringToByteArray("0601"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventIndex"), HexConverter.hexStringToByteArray("0602"));

        assertArrayEquals( (byte[]) eventRecords.get(7).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals( (byte[]) eventRecords.get(7).get("eventIndex"), HexConverter.hexStringToByteArray("0607"));
    }

    @Test
    void decodeStakingEvent2() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("1002060892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060904fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060afc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4704fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060b470d2e47fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4704fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 4);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("0608"));

        assertArrayEquals( (byte[]) eventRecords.get(3).get("eventData"), HexConverter.hexStringToByteArray("470d2e47fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47"));
        assertArrayEquals( (byte[]) eventRecords.get(3).get("eventIndex"), HexConverter.hexStringToByteArray("060b"));
    }

    @Test
    void decodeOffencesEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("04020700696d2d6f6e6c696e653a6f66666c696e104208000000");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 1);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("696d2d6f6e6c696e653a6f66666c696e1042080000"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("0700"));
    }

    @Test
    void decodeSessionEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("04020800000f412004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 1);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("000f4120"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("0800"));
    }

    @Test
    void decodeGrandpaEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c020a0008000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0204fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("080000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("0a00"));

        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventIndex"), HexConverter.hexStringToByteArray("0a01"));

        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventIndex"), HexConverter.hexStringToByteArray("0a02"));
    }

    @Test
    void decodeImOnlineEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c020b0092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020b0104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020b020892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd850b407ad0dd7c010b9f2cd2dd7c0104120b407ad0dd7c010b9f2cd2dd7c0104120492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8501017023494def5460aa2e93a0462e8792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd850b407ad0dd7c010b9f2cd2dd7c0104120b407ad0dd7c010b9f2cd2dd7c0104120492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8501017023494def5460aa2e93a0462e8704fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("0b00"));

        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventData"), HexConverter.hexStringToByteArray("0892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd850b407ad0dd7c010b9f2cd2dd7c0104120b407ad0dd7c010b9f2cd2dd7c0104120492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8501017023494def5460aa2e93a0462e8792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd850b407ad0dd7c010b9f2cd2dd7c0104120b407ad0dd7c010b9f2cd2dd7c0104120492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8501017023494def5460aa2e93a0462e87"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventIndex"), HexConverter.hexStringToByteArray("0b02"));
    }

    @Test
    void decodeUtilityEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c0210000000a7e7030b5204fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702100104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702100204fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("0000a7e7030b52"));
    }

    @Test
    void decodeIdentityEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("2802110092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702110192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702110292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702110392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500005d3f04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702110492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500005d3f04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702110592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500005d3f04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702110600005d3f04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702110792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702110892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702110992341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 10);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("1100"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventIndex"), HexConverter.hexStringToByteArray("1101"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventIndex"), HexConverter.hexStringToByteArray("1102"));

        assertArrayEquals( (byte[]) eventRecords.get(9).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a40"));
        assertArrayEquals( (byte[]) eventRecords.get(9).get("eventIndex"), HexConverter.hexStringToByteArray("1109"));
    }

    @Test
    void decodeRecoveryEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("1802120092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702120192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702120292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe46735147023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702120392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702120492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702120592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 6);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("1200"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventIndex"), HexConverter.hexStringToByteArray("1201"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe46735147023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventIndex"), HexConverter.hexStringToByteArray("1202"));

        assertArrayEquals( (byte[]) eventRecords.get(5).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals( (byte[]) eventRecords.get(5).get("eventIndex"), HexConverter.hexStringToByteArray("1205"));
    }

    @Test
    void decodeVestingEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0802130092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702130192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 2);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("1300"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventIndex"), HexConverter.hexStringToByteArray("1301"));
    }

    @Test
    void decodeSchedulerEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c021400001e51b90000a7e704fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021401001e51b90000a7e704fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702140266674a0000000000013064656d6f63726163160000000004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("001e51b90000a7e7"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("1400"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventData"), HexConverter.hexStringToByteArray("001e51b90000a7e7"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventIndex"), HexConverter.hexStringToByteArray("1401"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventData"), HexConverter.hexStringToByteArray("66674a0000000000013064656d6f637261631600000000"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventIndex"), HexConverter.hexStringToByteArray("1402"));
    }

    @Test
    void decodeSudoEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c021500010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702150192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021502010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("0100"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("1500"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventIndex"), HexConverter.hexStringToByteArray("1501"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventData"), HexConverter.hexStringToByteArray("0100"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventIndex"), HexConverter.hexStringToByteArray("1502"));
    }

    @Test
    void decodeProxyEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("10021600010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702160192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351402223a04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702160292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702160392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe46735140192341e7e04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 4);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("0100"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("1600"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351402223a"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventIndex"), HexConverter.hexStringToByteArray("1601"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventIndex"), HexConverter.hexStringToByteArray("1602"));
        assertArrayEquals( (byte[]) eventRecords.get(3).get("eventData"), HexConverter.hexStringToByteArray("92341E7E5C46F8B32CD39F8E425D2916FBD0E5DFDB1818194EC02B66A52BFD857023494DEF5460AA2E93A0462E87ED1C5F00EA964D252C9744BC623FE46735140192341E7E"));
        assertArrayEquals( (byte[]) eventRecords.get(3).get("eventIndex"), HexConverter.hexStringToByteArray("1603"));
    }

    @Test
    void decodeMultisigEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("1002170092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702170192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85d01400003f1200007023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702170292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85d01400003f1200007023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702170392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85d01400003f1200007023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 4);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("1700"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85d01400003f1200007023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventIndex"), HexConverter.hexStringToByteArray("1701"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventData"), HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85d01400003f1200007023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd850100"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventIndex"), HexConverter.hexStringToByteArray("1702"));
        assertArrayEquals( (byte[]) eventRecords.get(3).get("eventData"), HexConverter.hexStringToByteArray("92341E7E5C46F8B32CD39F8E425D2916FBD0E5DFDB1818194EC02B66A52BFD85D01400003F1200007023494DEF5460AA2E93A0462E87ED1C5F00EA964D252C9744BC623FE467351492341E7E5C46F8B32CD39F8E425D2916FBD0E5DFDB1818194EC02B66A52BFD85"));
        assertArrayEquals( (byte[]) eventRecords.get(3).get("eventIndex"), HexConverter.hexStringToByteArray("1703"));
    }

    @Test
    void decodeCandidateBackedEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c00000000000000b814fb0a0000000002000000010000002c00e80300003eb52c89491cc4e107e32b6a66edb3698fc9960487b81e58229a1dccd3bc7a95d423dff1dbaef3ca4573bf8be0dc81037d32133bbff768f5b6409880954fda5a6bf5f9b09d615c0a98ae0a428fea8573d500d58163ac25503006696f93849668d597ac1a4699a507c68466cecaf38d1e672e0b515adc4033045f5c86aba994e931e36a4fedda6e70030d51162884c0a914f43ed30daa7af9f556511a1242a22b8af39b79b39848366a9e324761d25fa7ac8cac8070a84309b196b991689eba1db2b472dbd5add083626d18924a18ef121c9390f569ec7e4650837c6ad5190a8e13afcb4c45d42ddc022a4d1f65b10ebf2bbbcb9c4138c997ecfef323960bd482f5acd6bb417bda10d0667b2e51e50596eaa01120e0893c68de6751e3794365f213b7bd972fae17ffa90b39c7b9d0500fae0d8fae40e33c785599d21e84ad5c19e90391c0bc1d2da78eef8c03fb354a6fea7f25f06b0f38b29a060c4a71fc73849016ded81c00fc98709e979e91f4e46fd3d8a99bf090d6e49e775d29b00d92612389858a0775a5288bbb0c2c886cb7ad1d4468b5cc9c5c985df86b8b1d7601d665563eedd6990c046e6d627380268f2f2d9db24df09f7ce18c0a73ad60a621b346b579729a34fe4a7baecdf31f0466726f6e880164442b4ec52ac2017bff6ee86e398b63feac52b7b84edf557b239861fd75d9d700056e6d62730101c2f22459fd0ba639ff8d73c54c761a7ad25fdb9ff877926bf75aa27b6f1d0a5e80038d37528593f9f57b24fee32f9d9f02180cc5b35711246a0a43b51f01e486000000000000000000000100000000002039e80e00000000020000");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("0000"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventIndex"), HexConverter.hexStringToByteArray("2c00"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventData"), HexConverter.hexStringToByteArray("e80300003eb52c89491cc4e107e32b6a66edb3698fc9960487b81e58229a1dccd3bc7a95d423dff1dbaef3ca4573bf8be0dc81037d32133bbff768f5b6409880954fda5a6bf5f9b09d615c0a98ae0a428fea8573d500d58163ac25503006696f93849668d597ac1a4699a507c68466cecaf38d1e672e0b515adc4033045f5c86aba994e931e36a4fedda6e70030d51162884c0a914f43ed30daa7af9f556511a1242a22b8af39b79b39848366a9e324761d25fa7ac8cac8070a84309b196b991689eba1db2b472dbd5add083626d18924a18ef121c9390f569ec7e4650837c6ad5190a8e13afcb4c45d42ddc022a4d1f65b10ebf2bbbcb9c4138c997ecfef323960bd482f5acd6bb417bda10d0667b2e51e50596eaa01120e0893c68de6751e3794365f213b7bd972fae17ffa90b39c7b9d0500fae0d8fae40e33c785599d21e84ad5c19e90391c0bc1d2da78eef8c03fb354a6fea7f25f06b0f38b29a060c4a71fc73849016ded81c00fc98709e979e91f4e46fd3d8a99bf090d6e49e775d29b00d92612389858a0775a5288bbb0c2c886cb7ad1d4468b5cc9c5c985df86b8b1d7601d665563eedd6990c046e6d627380268f2f2d9db24df09f7ce18c0a73ad60a621b346b579729a34fe4a7baecdf31f0466726f6e880164442b4ec52ac2017bff6ee86e398b63feac52b7b84edf557b239861fd75d9d700056e6d62730101c2f22459fd0ba639ff8d73c54c761a7ad25fdb9ff877926bf75aa27b6f1d0a5e80038d37528593f9f57b24fee32f9d9f02180cc5b35711246a0a43b51f01e4860000000000000000"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventIndex"), HexConverter.hexStringToByteArray("0000"));
    }

    @Test
    void decodeCandidateIncludeEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c0000000000000080ca83090000000002000000010000002c01e80300008acf9bde755569bb14b55095585998840ffc88476d4bffceb783727cb76c72e49efae44515105018d646455cfb29363abf38985fcc1411a3624ad4482fe7125db66cd25f32eaeaeb56cd88ecc7b10559305735fa7415d96d14e3550a467603fdb8a1172749841d254df4730b2e1143fe196a15cc4736ccc685a70433791ceba2150f6e6c9e0ab09704eaa10c51d641dba2c1df48a9e14e8d8e5ce89fdc5c6e294cc7a263300d5f96b46316b51954362a58880603e3874ac2cdcab13e85d77723e17becfce7ef08d8204a2f3695ff31d03e841e1ef388c804c087cad7209e5f8f429b4c2c28f986e9bfc4288fcfebc58e8731af31870fd91b3105672b960b04eb6f3f6ebe9ae1b7c2339c16ca32e30173bb5d652aec8f082718ffde64950e54933ca6ffd25ca9656d929898658e60a8847e211928d15866aefeadecffa1aa51afed0ff5a9d14d4f8939ef1a1e499bb541a8f309df45c5e003e2e241976c9cf1887020e2853d00dbb24b34c85a23142e8ba7c8f7ced9e2afc8d4803ad859f0d9fc173d59e8fe5f4994f489667cf982158beed75ed78909f8673a10a897f5336e0e7169b6b93a1d0c046e6d627380bcdad08691a234570b989a2bc146ef51a2eabb99e6ff6d655e4edc922543677d0466726f6e890c01d706212a3c44944e09f6dc8c8709496648486fc065ac00efc51dac9d3286c9d8609b4637601edc667abda9918477931ce1bbee6c4a2811176b5b27384b1ba7316bd9f7dfc18edd7eff28670b38040c98714740382df0f3b160824b15b6a57fbe3c68b68af976098229eaad1a2e6a034c96a6262b641534e2c65f073cc447f478117ef1086cd0ba1a269b3c5338545880fc27cb67c7cf71757575fa35029e0e18f91ae8aeed849f2dab9700cc93a0429a52a9a232f309dfb64b7b31802f534970032c17227ecbd80a1a93b56c213f018fc04b81c9b0ed885309fe8dd158622080deb646b9e0dba3b7f8a25357a1a24760047b57790e051fae37c3996ab554e9e3899197123784bb0a38e38dd6aa91a3e5acdc1389903d2eb02b70656bc5619163ef0982e9463c57d04be783094a92198a3650f44c8bf98b1ef513528ba182a209df08f739f3e3c20e7235bcbb47e6a8ee2466588f7eda545207213c49dce4d56648857b66567a060231f11647bcfc7cd541c1567ad9d911e4e6e1c97d6184199d1669036ca87d9e778ab87f9ef8e017f0a93c220b4170842bbf1363369e913290fe45c995031f0f63383052de6f0b4431435667a574899b1c0fb64e14fac46cba3569cdadfbf9dc7ba4945b79934854ec52d7d7813e089b69210ddcf38527b6731b197a018e75c27fd9586a5f35b4db1b2af437d3f79fece345be3fd078dfc5198ed1d879a32914780f24374207f3c48202efa11a1207b8b9f7c2612ae33575a213d18802eece677b06c19be069212416eaf60277c5ef46fea52a34c09d43d77a8a1040cc27ab139ffc22e14395c45f413bff754bd4274abe25b78e25b11334462f95872786c20bd13fe08a9e37eb49b1f5ace935fb27b19e018f167fa62a4666ece0c1bc3bc11f8c2c22f889bc208329627b06b23d54618b16767293efdc7f89e771f07454554fee0c44d36f10990532a71d113f6f62c3e0f03c0309dc58db75c60f40fb463aa39739947ae73e210b76529a35b9d3646f3a1e8347c91fc41db822fe25c67b77dcefe53846fd4422c9e8eca551ec26cfdbcbe081137ddc4cdb324292929f8a2526a12e61f1f0b11927a1752fe41a2200d7efca4255aa61eecb6bb2056e6d62730101344cff384177ba467f56e049c92ebd69451113801c9321f239478b452de4b03a112d36fdd278875e5bf2425aebdf3880d5aa5e19c162f9c15284f5fe2e6e9c860000000000000000000001000000000080b2e60e00000000020000");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("0000"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventIndex"), HexConverter.hexStringToByteArray("2c01"));
        assertArrayEquals( (byte[]) eventRecords.get(1).get("eventData"), HexConverter.hexStringToByteArray("e80300008acf9bde755569bb14b55095585998840ffc88476d4bffceb783727cb76c72e49efae44515105018d646455cfb29363abf38985fcc1411a3624ad4482fe7125db66cd25f32eaeaeb56cd88ecc7b10559305735fa7415d96d14e3550a467603fdb8a1172749841d254df4730b2e1143fe196a15cc4736ccc685a70433791ceba2150f6e6c9e0ab09704eaa10c51d641dba2c1df48a9e14e8d8e5ce89fdc5c6e294cc7a263300d5f96b46316b51954362a58880603e3874ac2cdcab13e85d77723e17becfce7ef08d8204a2f3695ff31d03e841e1ef388c804c087cad7209e5f8f429b4c2c28f986e9bfc4288fcfebc58e8731af31870fd91b3105672b960b04eb6f3f6ebe9ae1b7c2339c16ca32e30173bb5d652aec8f082718ffde64950e54933ca6ffd25ca9656d929898658e60a8847e211928d15866aefeadecffa1aa51afed0ff5a9d14d4f8939ef1a1e499bb541a8f309df45c5e003e2e241976c9cf1887020e2853d00dbb24b34c85a23142e8ba7c8f7ced9e2afc8d4803ad859f0d9fc173d59e8fe5f4994f489667cf982158beed75ed78909f8673a10a897f5336e0e7169b6b93a1d0c046e6d627380bcdad08691a234570b989a2bc146ef51a2eabb99e6ff6d655e4edc922543677d0466726f6e890c01d706212a3c44944e09f6dc8c8709496648486fc065ac00efc51dac9d3286c9d8609b4637601edc667abda9918477931ce1bbee6c4a2811176b5b27384b1ba7316bd9f7dfc18edd7eff28670b38040c98714740382df0f3b160824b15b6a57fbe3c68b68af976098229eaad1a2e6a034c96a6262b641534e2c65f073cc447f478117ef1086cd0ba1a269b3c5338545880fc27cb67c7cf71757575fa35029e0e18f91ae8aeed849f2dab9700cc93a0429a52a9a232f309dfb64b7b31802f534970032c17227ecbd80a1a93b56c213f018fc04b81c9b0ed885309fe8dd158622080deb646b9e0dba3b7f8a25357a1a24760047b57790e051fae37c3996ab554e9e3899197123784bb0a38e38dd6aa91a3e5acdc1389903d2eb02b70656bc5619163ef0982e9463c57d04be783094a92198a3650f44c8bf98b1ef513528ba182a209df08f739f3e3c20e7235bcbb47e6a8ee2466588f7eda545207213c49dce4d56648857b66567a060231f11647bcfc7cd541c1567ad9d911e4e6e1c97d6184199d1669036ca87d9e778ab87f9ef8e017f0a93c220b4170842bbf1363369e913290fe45c995031f0f63383052de6f0b4431435667a574899b1c0fb64e14fac46cba3569cdadfbf9dc7ba4945b79934854ec52d7d7813e089b69210ddcf38527b6731b197a018e75c27fd9586a5f35b4db1b2af437d3f79fece345be3fd078dfc5198ed1d879a32914780f24374207f3c48202efa11a1207b8b9f7c2612ae33575a213d18802eece677b06c19be069212416eaf60277c5ef46fea52a34c09d43d77a8a1040cc27ab139ffc22e14395c45f413bff754bd4274abe25b78e25b11334462f95872786c20bd13fe08a9e37eb49b1f5ace935fb27b19e018f167fa62a4666ece0c1bc3bc11f8c2c22f889bc208329627b06b23d54618b16767293efdc7f89e771f07454554fee0c44d36f10990532a71d113f6f62c3e0f03c0309dc58db75c60f40fb463aa39739947ae73e210b76529a35b9d3646f3a1e8347c91fc41db822fe25c67b77dcefe53846fd4422c9e8eca551ec26cfdbcbe081137ddc4cdb324292929f8a2526a12e61f1f0b11927a1752fe41a2200d7efca4255aa61eecb6bb2056e6d62730101344cff384177ba467f56e049c92ebd69451113801c9321f239478b452de4b03a112d36fdd278875e5bf2425aebdf3880d5aa5e19c162f9c15284f5fe2e6e9c860000000000000000"));
        assertArrayEquals( (byte[]) eventRecords.get(2).get("eventIndex"), HexConverter.hexStringToByteArray("0000"));
    }

    @Test
    void decodeCandidateTimedOutEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0400010000002c0228080000ab6d3509b954bcc6c721339c26af7dcb201b17291b9079e1078c42b901f38919dcb8363437d401fb20903d96215f75f4b8bde76a83e879e3ff901d23b9b3ed6bc6389b1790d8839e4725f31be5852baa40b4234ffe51fa99a76186d0b28b5f47205f43667c6868fa882d17363d6b954c82453e49620c87db08ff94564f32d72f1e362ba0dbba661b69cfe13bf82d21c89fe33de6008eb647b966f1e9907322eea665a3bd8ff3b7b5d22364656f8e24e3c1a9b6d5a1c2d5eabe1de3a6a349d86701040104ec1f7c38adae4ba70d42bec9ae5d53a57152c7d9e049cc2a9425528856ad8369c7c6abbe867efe0d092b8aa8a226672ba82e4f2a581a274b227292c83cd446c9a1bd71d0ce47495a92d001a3047b17ea1afafade18e5892db4ebce47ceb81aa851dbd372cb5558c77de6b43aaead5766c9d154bb6d73a6d4756b1e6ce9022d027c40d6b35e7a3c414d3aa09b72ea723b032ac54eb1f77f099df0fad1728112c50800e17efcd4913471beddf3c2911fca03bba0a10bcdd0366b0215b8fc1086dd0287d7049d1f977b5fa6834a89b5f47a5e3a934c2e843336928860e4fa21dd1f6704080661757261201cd61e080000000005617572610101608ec121e970296dd4f88b5b63a5b319a802746fdd8b462562e48feaa810150f9b56dd4d4fb179680221c18046837d509e55e5c7616dc26f5ab2c11572ee93890900000000");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 1);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("2c02"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("28080000ab6d3509b954bcc6c721339c26af7dcb201b17291b9079e1078c42b901f38919dcb8363437d401fb20903d96215f75f4b8bde76a83e879e3ff901d23b9b3ed6bc6389b1790d8839e4725f31be5852baa40b4234ffe51fa99a76186d0b28b5f47205f43667c6868fa882d17363d6b954c82453e49620c87db08ff94564f32d72f1e362ba0dbba661b69cfe13bf82d21c89fe33de6008eb647b966f1e9907322eea665a3bd8ff3b7b5d22364656f8e24e3c1a9b6d5a1c2d5eabe1de3a6a349d86701040104ec1f7c38adae4ba70d42bec9ae5d53a57152c7d9e049cc2a9425528856ad8369c7c6abbe867efe0d092b8aa8a226672ba82e4f2a581a274b227292c83cd446c9a1bd71d0ce47495a92d001a3047b17ea1afafade18e5892db4ebce47ceb81aa851dbd372cb5558c77de6b43aaead5766c9d154bb6d73a6d4756b1e6ce9022d027c40d6b35e7a3c414d3aa09b72ea723b032ac54eb1f77f099df0fad1728112c50800e17efcd4913471beddf3c2911fca03bba0a10bcdd0366b0215b8fc1086dd0287d7049d1f977b5fa6834a89b5f47a5e3a934c2e843336928860e4fa21dd1f6704080661757261201cd61e080000000005617572610101608ec121e970296dd4f88b5b63a5b319a802746fdd8b462562e48feaa810150f9b56dd4d4fb179680221c18046837d509e55e5c7616dc26f5ab2c11572ee938909000000"));
    }

    @Test
    void decodeUmpExecutedUpwardEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0400010000003202f57e77ed42bc08b71992f2731635636926ecc1a878c73dc1d1904a217fb310b600005ed0b20000000000");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 1);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("3202"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("f57e77ed42bc08b71992f2731635636926ecc1a878c73dc1d1904a217fb310b600005ed0b200000000"));
    }

    @Test
    void decodeCrowdloanMemoUpdatedEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("04000200000040083857b16e5fa4d5c4ea4603ece90a1e2e8a3e603fb100ab73e0693109fae7a3132708000014576f726c6400");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 1);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("4008"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("3857b16e5fa4d5c4ea4603ece90a1e2e8a3e603fb100ab73e0693109fae7a3132708000014576f726c64"));
    }

    @Test
    void decodeXcmAttemptedEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("04000200000063000000ca9a3b0000000000");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 1);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("6300"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("0000ca9a3b00000000"));
    }

    @Test
    void decodeXcmSentEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("040263010000000100a10f04060202286bee88010319755aae83cded7d2966ce2958195c5d02c6b8b625436c27cb0aae50f168c10800");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 1);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("6301"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("0000000100a10f04060202286bee88010319755aae83cded7d2966ce2958195c5d02c6b8b625436c27cb0aae50f168c108"));
    }

    @Test
    void decodeXcmSupportedVersionChangedEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0402630d000100a10f0000000000");
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 1);
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventIndex"), HexConverter.hexStringToByteArray("630d"));
        assertArrayEquals( (byte[]) eventRecords.get(0).get("eventData"), HexConverter.hexStringToByteArray("000100a10f00000000"));
    }
}
