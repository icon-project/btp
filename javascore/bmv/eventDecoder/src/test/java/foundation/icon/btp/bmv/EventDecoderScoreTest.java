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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class EventDecoderScoreTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private static Score eventDecoderScore;

    @BeforeAll
    public static void setup() throws Exception {
        eventDecoderScore = sm.deploy(owner, EventDecoderScore.class);
    }

    @Test
    void decodeSystemEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("14020000e703000000000000010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020001030b52e703000000000000010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702000204fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702000392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702000392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 5);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("e7030000000000000100"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("030b52e7030000000000000100"));
        assertArrayEquals(eventRecords.get(3).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(4).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
    }

    @Test
    void decodeUtilityEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("080201000000a7e7030b5204fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702010104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 2);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("0000a7e7030b52"));
    }

    @Test
    void decodeIndicesEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c02050092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85618076e804fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020501618076e804fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020502618076e892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85618076e8"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("0500"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("618076e8"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("0501"));
        assertArrayEquals(eventRecords.get(2).eventData, HexConverter.hexStringToByteArray("618076e892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("0502"));
    }

    @Test
    void decodeBalanceEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("2002060092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4000000000000000000000028998f5804c04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060692341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702060792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a400104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 8);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("0600"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("0601"));
        assertArrayEquals(eventRecords.get(2).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("0602"));

        assertArrayEquals(eventRecords.get(7).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a4001"));
        assertArrayEquals(eventRecords.get(7).eventIndex, HexConverter.hexStringToByteArray("0607"));
    }

    @Test
    void decodeStakingEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("2402080049a8f36300000000000000000000017cddd07a4000000000000000000000028998f5804c04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702080192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702080292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020803000f412004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e470208040104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e470208050104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702080692341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702080792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702080892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 9);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("49a8f36300000000000000000000017cddd07a4000000000000000000000028998f5804c"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("0800"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("0801"));
        assertArrayEquals(eventRecords.get(2).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("0802"));

        assertArrayEquals(eventRecords.get(8).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(8).eventIndex, HexConverter.hexStringToByteArray("0808"));
    }

    @Test
    void decodeSessionEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("04020900000f412004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 1);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("000f4120"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("0900"));
    }

    @Test
    void decodeDemocracyEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("48020a0000635eb500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0100635eb500000000000000000000017cddd07a400892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0204fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0300005d3f0104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0400005d3f04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0500005d3f04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0600005d3f04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0700005d3f0104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0992341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0a92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85001e51b904fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0b92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0c92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0d92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500005d3f04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0e92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500005d3f04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a0f92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a407023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a1092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020a1192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 18);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("00635eb500000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("0a00"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("00635eb500000000000000000000017cddd07a400892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("0a01"));

        assertArrayEquals(eventRecords.get(17).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(17).eventIndex, HexConverter.hexStringToByteArray("0a11"));
    }

    @Test
    void decodeCouncilEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("1c020b0092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500635eb592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500004d9e04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020b0192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd850100004d9e0006f5c104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020b0292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020b0392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020b0492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020b0592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020b0692341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500004d9e0006f5c104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 7);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500635eb592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500004d9e"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("0b00"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd850100004d9e0006f5c1"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("0b01"));

        assertArrayEquals(eventRecords.get(6).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500004d9e0006f5c1"));
        assertArrayEquals(eventRecords.get(6).eventIndex, HexConverter.hexStringToByteArray("0b06"));
    }

    @Test
    void decodeElectionsEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("20020c000800000000000000000000000000000000000000000000000000000000000000004c80f5988902000000000000000000000000000000000000000000000000000000000000000000000000000000000000e76a460000000000000000000000000004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020c0104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020c0204fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020c0392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020c0492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020c0592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020c0692341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020c0792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe46735140104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 8);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("0800000000000000000000000000000000000000000000000000000000000000004c80f5988902000000000000000000000000000000000000000000000000000000000000000000000000000000000000e76a4600000000000000000000000000"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("0c00"));

        assertArrayEquals(eventRecords.get(3).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(3).eventIndex, HexConverter.hexStringToByteArray("0c03"));

        assertArrayEquals(eventRecords.get(7).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351401"));
        assertArrayEquals(eventRecords.get(7).eventIndex, HexConverter.hexStringToByteArray("0c07"));
    }

    @Test
    void decodeGrandpaEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c020e0008000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020e0104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020e0204fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("080000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("0e00"));

        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("0e01"));

        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("0e02"));
    }

    @Test
    void decodeTreasuryEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("1c020f0000635eb504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020f0100000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020f0200635eb500000000000000000000017cddd07a4092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020f0300635eb500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020f0400000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020f0500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47020f0600000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 7);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("00635eb5"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("0f00"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("00000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("0f01"));
        assertArrayEquals(eventRecords.get(2).eventData, HexConverter.hexStringToByteArray("00635eb500000000000000000000017cddd07a4092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("0f02"));

        assertArrayEquals(eventRecords.get(6).eventIndex, HexConverter.hexStringToByteArray("0f06"));
        assertArrayEquals(eventRecords.get(6).eventData, HexConverter.hexStringToByteArray("00000000000000000000017cddd07a40"));
    }

    @Test
    void decodeContractEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("1802100092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702100192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd850104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702100292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702100392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e470210040000a7e704fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702100592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85240b0c03637b570e414d04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 6);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("1000"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8501"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("1001"));
        assertArrayEquals(eventRecords.get(2).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("1002"));
    }

    @Test
    void decodeSudoEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c021100010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702110192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021102010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("0100"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("1100"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("1101"));
        assertArrayEquals(eventRecords.get(2).eventData, HexConverter.hexStringToByteArray("0100"));
        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("1102"));
    }

    @Test
    void decodeImOnlineEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c02120092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702120104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e470212020892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd850b407ad0dd7c010b9f2cd2dd7c010492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8501017023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe46735140b4c80f59889020b9f2cd2dd7c0104febd97fdbc2d968df381aebe0a9211f380eaf5b5370e330d4a41f8b81000f8e40baf80f598890204fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("1200"));

        assertArrayEquals(eventRecords.get(2).eventData, HexConverter.hexStringToByteArray("0892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd850b407ad0dd7c010b9f2cd2dd7c010492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8501017023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe46735140b4c80f59889020b9f2cd2dd7c0104febd97fdbc2d968df381aebe0a9211f380eaf5b5370e330d4a41f8b81000f8e40baf80f5988902"));
        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("1202"));
    }

    @Test
    void decodeOffencesEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("04021400696d2d6f6e6c696e653a6f66666c696e10420800000100");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 1);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("696d2d6f6e6c696e653a6f66666c696e104208000001"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("1400"));
    }

    @Test
    void decodeIdentityEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("2802170092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702170192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702170292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702170392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500005d3f04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702170492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500005d3f04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702170592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500005d3f04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702170600005d3f04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702170792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702170892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702170992341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 10);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("1700"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("1701"));
        assertArrayEquals(eventRecords.get(2).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("1702"));

        assertArrayEquals(eventRecords.get(9).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(9).eventIndex, HexConverter.hexStringToByteArray("1709"));
    }

    @Test
    void decodeRecoveryEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("1802180092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702180192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702180292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe46735147023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702180392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702180492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702180592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 6);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("1800"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("1801"));
        assertArrayEquals(eventRecords.get(2).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe46735147023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514"));
        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("1802"));

        assertArrayEquals(eventRecords.get(5).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(5).eventIndex, HexConverter.hexStringToByteArray("1805"));
    }

    @Test
    void decodeVestingEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0802190092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702190192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 2);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("1900"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("1901"));
    }

    @Test
    void decodeSchedulerEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c021a00001e51b90000a7e704fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021a01001e51b90000a7e704fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021a0266674a0000000000013064656d6f63726163160000000004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("001e51b90000a7e7"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("1a00"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("001e51b90000a7e7"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("1a01"));
        assertArrayEquals(eventRecords.get(2).eventData, HexConverter.hexStringToByteArray("66674a0000000000013064656d6f637261631600000000"));
        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("1a02"));
    }

    @Test
    void decodeProxyEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0c021b00010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021b0192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351402223a04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021b0292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 3);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("0100"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("1b00"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351402223a"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("1b01"));
        assertArrayEquals(eventRecords.get(2).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("1b02"));
    }

    @Test
    void decodeMultisigEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("10021c0092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021c0192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85d01400003f1200007023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021c0292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85d01400003f1200007023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85010004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021c0392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85d01400003f1200007023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 4);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("1c00"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85d01400003f1200007023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("1c01"));
        assertArrayEquals(eventRecords.get(2).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85d01400003f1200007023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd850100"));
        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("1c02"));
    }

    @Test
    void decodeAssetsEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("2c021d0000466ae792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021d0100466ae792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85000000010032daac04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021d0200466ae792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514000000010032daac04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021d0300466ae792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85000000010032daac04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021d0400466ae792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe46735147023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351404fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021d0500466ae792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021d0600466ae792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021d0700466ae792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021d0800466ae704fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021d0900466ae792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47021d0a00466ae70000a7e704fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 11);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("00466ae792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("1d00"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("00466ae792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85000000010032daac"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("1d01"));
        assertArrayEquals(eventRecords.get(2).eventData, HexConverter.hexStringToByteArray("00466ae792341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514000000010032daac"));
        assertArrayEquals(eventRecords.get(2).eventIndex, HexConverter.hexStringToByteArray("1d02"));

        assertArrayEquals(eventRecords.get(10).eventData, HexConverter.hexStringToByteArray("00466ae70000a7e7"));
        assertArrayEquals(eventRecords.get(10).eventIndex, HexConverter.hexStringToByteArray("1d0a"));
    }

    @Test
    void decodeTreasuryRewardEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0402200000000000000000000000017cddd07a40001e51b992341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 1);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("00000000000000000000017cddd07a40001e51b992341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("2000"));
    }

    @Test
    void decodeEthereumEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0402210033e0e07ca86c869ade3fc9de9126f6c73dad105e0000000000000000000000000000000000000000de6d619626e086dd6c68171af1aeacdf7df8962bf142bf52a1244119b82d7474000004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 1);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("33e0e07ca86c869ade3fc9de9126f6c73dad105e0000000000000000000000000000000000000000de6d619626e086dd6c68171af1aeacdf7df8962bf142bf52a1244119b82d74740000"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("2100"));
    }

    @Test
    void decodeEVMEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("1c022200a1966cad854174d217235dd951c8928f42bc12a604a20e495bf7cbb7bc0ecf70d514a8e6da116c9b8b2a4e1c25d06b6b176db0de0f01030000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000004adb344600000000000000000000000000000000000000000000000000000000609f732700000000000000000000000000000000000000000000000000000000004dd18e00000000000000000000000000000000000000000000000000000000000000044d414e410000000000000000000000000000000000000000000000000000000004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702220192341e7e5c46f8b32cd39f8e425d2916fbd0ef1104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702220292341e7e5c46f8b32cd39f8e425d2916fbd0ef1104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702220392341e7e5c46f8b32cd39f8e425d2916fbd0ef1104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702220492341e7e5c46f8b32cd39f8e425d2916fbd0ef1104fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702220592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0ef1192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702220692341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0ef1192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 7);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("a1966cad854174d217235dd951c8928f42bc12a604a20e495bf7cbb7bc0ecf70d514a8e6da116c9b8b2a4e1c25d06b6b176db0de0f01030000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000004adb344600000000000000000000000000000000000000000000000000000000609f732700000000000000000000000000000000000000000000000000000000004dd18e00000000000000000000000000000000000000000000000000000000000000044d414e4100000000000000000000000000000000000000000000000000000000"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("2200"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0ef11"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("2201"));

        assertArrayEquals(eventRecords.get(6).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0ef1192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(6).eventIndex, HexConverter.hexStringToByteArray("2206"));
    }

    @Test
    void decodeChainBridgeEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("340223000000a7e704fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e470223011304fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702230292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702230392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e470223041300000000028fded392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85240b0c03637b570e414d04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e470223051300000000028fded392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85240b0c03637b570e414d240b0c03637b570e414d240b0c03637b570e414d04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e470223061300000000028fded392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85240b0c03637b570e414d04fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e470223071300000000028fded392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e470223081300000000028fded392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e470223091300000000028fded304fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702230a1300000000028fded304fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702230b1300000000028fded304fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702230c1300000000028fded304fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 13);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("0000a7e7"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("2300"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("13"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("2301"));

        assertArrayEquals(eventRecords.get(12).eventData, HexConverter.hexStringToByteArray("1300000000028fded3"));
        assertArrayEquals(eventRecords.get(12).eventIndex, HexConverter.hexStringToByteArray("230c"));
    }

    @Test
    void decodeEdgeBridgeEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("0402240092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85240b0c03637b570e414d1300000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 1);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85240b0c03637b570e414d1300000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("2400"));
    }

    @Test
    void decodeBountiesEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("1c022500000f412004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47022501000f412000000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47022502000f412004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47022503000f412092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47022504000f412000000000000000000000017cddd07a4092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47022505000f412004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47022506000f412004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 7);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("000f4120"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("2500"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("000f412000000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("2501"));

        assertArrayEquals(eventRecords.get(6).eventData, HexConverter.hexStringToByteArray("000f4120"));
        assertArrayEquals(eventRecords.get(6).eventIndex, HexConverter.hexStringToByteArray("2506"));
    }

    @Test
    void decodeTipsEvent() {
        byte[] encodedEventList = HexConverter.hexStringToByteArray("1402260092341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702260192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702260292341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702260392341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8504fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e4702260492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a4004fc4a936afd8097e18952e2df1c1f7d6eb9b6d5958ae22b7ea94ed555470d2e47");
        List<EventRecord> eventRecords = (List<EventRecord>) eventDecoderScore.call("decodeEvent", encodedEventList);
        assertEquals(eventRecords.size(), 5);
        assertArrayEquals(eventRecords.get(0).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(0).eventIndex, HexConverter.hexStringToByteArray("2600"));
        assertArrayEquals(eventRecords.get(1).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85"));
        assertArrayEquals(eventRecords.get(1).eventIndex, HexConverter.hexStringToByteArray("2601"));

        assertArrayEquals(eventRecords.get(4).eventData, HexConverter.hexStringToByteArray("92341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8592341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a40"));
        assertArrayEquals(eventRecords.get(4).eventIndex, HexConverter.hexStringToByteArray("2604"));
    }
}
