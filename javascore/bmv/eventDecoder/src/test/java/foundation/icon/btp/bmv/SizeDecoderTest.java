package foundation.icon.btp.eventdecoder.edgeware;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import foundation.icon.btp.lib.utils.HexConverter;
import foundation.icon.btp.lib.eventdecoder.*;
import foundation.icon.btp.lib.utils.ByteSliceInput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class SizeDecoderTest {
    @Test
    void decodeDispatchError() {
        byte[] encoded = HexConverter.hexStringToByteArray("0000030b52001e51b9");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int decodedSize = SizeDecoder.DispatchError(input, 2);
        assertEquals(decodedSize, 3);
    }

    @Test
    void decodeDispatchResult() {
        byte[] encoded = HexConverter.hexStringToByteArray("00000001000100");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int decodedSize = SizeDecoder.DispatchResult(input, 3);
        assertEquals(decodedSize, 2);
    }

    @Test
    void decodeOpaqueTimeSlot() {
        byte[] encoded = HexConverter.hexStringToByteArray("febd97fdbc2d968df381aebe0a9211f380eaf5b5370e330d4a41f8b81000f8e4240b0c03637b570e414d");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int decodedSize = SizeDecoder.OpaqueTimeSlot(input, 32);
        assertEquals(decodedSize, 10);
    }

    @Test
    void decodeAuthorityList() {
        byte[] encoded = HexConverter.hexStringToByteArray("febd97fdbc2d968df381aebe0a9211f380eaf5b5370e330d4a41f8b81000f8e4080000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int decodedSize = SizeDecoder.AuthorityList(input, 32);
        assertEquals(decodedSize, 81);
    }

    @Test
    void decodeVec_IdentificationTuple() {
        byte[] encoded = HexConverter.hexStringToByteArray("febd97fdbc2d968df381aebe0a9211f380eaf5b5370e330d4a41f8b81000f8e40892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd850b407ad0dd7c010b9f2cd2dd7c010492341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8501017023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe46735140b4c80f59889020b9f2cd2dd7c0104febd97fdbc2d968df381aebe0a9211f380eaf5b5370e330d4a41f8b81000f8e40baf80f5988902");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int decodedSize = SizeDecoder.Vec_IdentificationTuple(input, 32);
        assertEquals(decodedSize, 168);
    }

    @Test
    void decodeVec_AccountId() {
        byte[] encoded = HexConverter.hexStringToByteArray("febd97fdbc2d968df381aebe0a9211f380eaf5b5370e330d4a41f8b81000f8e40892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd857023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int decodedSize = SizeDecoder.Vec_AccountId(input, 32);
        assertEquals(decodedSize, 65);
    }

    @Test
    void decodeVec_AccountId_Balance() {
        byte[] encoded = HexConverter.hexStringToByteArray("00000000000000000000017cddd07a400892341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd8500000000000000000000017cddd07a407023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe467351400000000000000000000017cddd07a400000");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int decodedSize = SizeDecoder.Vec_AccountId_Balance(input, 16);
        assertEquals(decodedSize, 97);
    }

    @Test
    void decodeOption_Bytes() {
        byte[] encoded = HexConverter.hexStringToByteArray("00000000000000000000017cddd07a40014092341e7e5c46f8b32cd39f8e425d2916001e51b9");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int decodedSize = SizeDecoder.Option_Bytes(input, 16);
        assertEquals(decodedSize, 18);
    }

    @Test
    void decodeHeadData() {
        byte[] encoded = HexConverter.hexStringToByteArray("00000000000000000000017cddd07a404092341e7e5c46f8b32cd39f8e425d2916001e51b9");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int decodedSize = SizeDecoder.HeadData(input, 16);
        assertEquals(decodedSize, 17);
    }

    @Test
    void decodeJunction() {
        byte[] encoded = HexConverter.hexStringToByteArray("00");
        ByteSliceInput input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 1);

        encoded = HexConverter.hexStringToByteArray("019e9f0200");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 5);

        encoded = HexConverter.hexStringToByteArray("02007023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 34);

        encoded = HexConverter.hexStringToByteArray("0201240b0c03637b570e414d7023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 44);

        encoded = HexConverter.hexStringToByteArray("02027023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 34);

        encoded = HexConverter.hexStringToByteArray("02037023494def5460aa2e93a0462e87ed1c5f00ea964d252c9744bc623fe4673514");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 34);

        encoded = HexConverter.hexStringToByteArray("0301240b0c03637b570e414dfef353ec");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 16);

        encoded = HexConverter.hexStringToByteArray("04030b0c03637b570e414d0b0c03637b570e414d0b16");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 22);

        encoded = HexConverter.hexStringToByteArray("0563");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 2);

        encoded = HexConverter.hexStringToByteArray("060b5b30a6c6e803");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 8);

        encoded = HexConverter.hexStringToByteArray("07240b0c03637b570e414d");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 11);

        encoded = HexConverter.hexStringToByteArray("090000");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 3);

        encoded = HexConverter.hexStringToByteArray("0901240b0c03637b570e414d00");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 13);

        encoded = HexConverter.hexStringToByteArray("0902de594b000162240100");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 11);

        encoded = HexConverter.hexStringToByteArray("090302de594b001ecf7a66");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 11);

        encoded = HexConverter.hexStringToByteArray("090303de594b001ecf7a66");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 11);

        encoded = HexConverter.hexStringToByteArray("090304de594b001ecf7a66");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeJunction(input);
        assertEquals(input.getOffset(), 11);
    }

    @Test
    void decodeAssetInstance() {
        byte[] encoded = HexConverter.hexStringToByteArray("00");
        ByteSliceInput input = new ByteSliceInput(encoded);
        SizeDecoder.decodeAssetInstance(input);
        assertEquals(input.getOffset(), 1);

        encoded = HexConverter.hexStringToByteArray("0163");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeAssetInstance(input);
        assertEquals(input.getOffset(), 2);

        encoded = HexConverter.hexStringToByteArray("028d01");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeAssetInstance(input);
        assertEquals(input.getOffset(), 3);

        encoded = HexConverter.hexStringToByteArray("051702f94b4c4d139c5f05");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeAssetInstance(input);
        assertEquals(input.getOffset(), 11);

        encoded = HexConverter.hexStringToByteArray("09febd97fdbc2d968df381aebe0a9211f380eaf5b5370e330d4a41f8b81000f8e4");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeAssetInstance(input);
        assertEquals(input.getOffset(), 33);

        encoded = HexConverter.hexStringToByteArray("0a240b0c03637b570e414d");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeAssetInstance(input);
        assertEquals(input.getOffset(), 11);
    }

    @Test
    void decodeMultiLocation() {
        byte[] encoded = HexConverter.hexStringToByteArray("020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec");
        ByteSliceInput input = new ByteSliceInput(encoded);
        SizeDecoder.decodeMultiLocation(input);
        assertEquals(input.getOffset(), 33);
    }

    @Test
    void decodeXcmOrder() {
        byte[] encoded = HexConverter.hexStringToByteArray("00");
        ByteSliceInput input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcmOrder(input);
        assertEquals(input.getOffset(), 1);

        encoded = HexConverter.hexStringToByteArray("01040b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcmOrder(input);
        assertEquals(input.getOffset(), 80);

        encoded = HexConverter.hexStringToByteArray("02040b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec00");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcmOrder(input);
        assertEquals(input.getOffset(), 81);

        encoded = HexConverter.hexStringToByteArray("04040b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec00");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcmOrder(input);
        assertEquals(input.getOffset(), 81);

        encoded = HexConverter.hexStringToByteArray("05040b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec00");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcmOrder(input);
        assertEquals(input.getOffset(), 81);

        encoded = HexConverter.hexStringToByteArray("03040b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d040b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcmOrder(input);
        assertEquals(input.getOffset(), 93);

        encoded = HexConverter.hexStringToByteArray("070b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d50d6120000000000cbd41200000000000100");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcmOrder(input);
        assertEquals(input.getOffset(), 64);
    }

    @Test
    void decodeMultiAsset() {
        byte[] encoded = HexConverter.hexStringToByteArray("00");
        ByteSliceInput input = new ByteSliceInput(encoded);
        SizeDecoder.decodeMultiAsset(input);
        assertEquals(input.getOffset(), 1);

        encoded = HexConverter.hexStringToByteArray("03");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeMultiAsset(input);
        assertEquals(input.getOffset(), 1);

        encoded = HexConverter.hexStringToByteArray("04240b0c03637b570e414d");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeMultiAsset(input);
        assertEquals(input.getOffset(), 11);

        encoded = HexConverter.hexStringToByteArray("06020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeMultiAsset(input);
        assertEquals(input.getOffset(), 34);

        encoded = HexConverter.hexStringToByteArray("08240b0c03637b570e414d0303519c49");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeMultiAsset(input);
        assertEquals(input.getOffset(), 16);

        encoded = HexConverter.hexStringToByteArray("0900050323329949");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeMultiAsset(input);
        assertEquals(input.getOffset(), 8);

        encoded = HexConverter.hexStringToByteArray("0a020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec16d5881d");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeMultiAsset(input);
        assertEquals(input.getOffset(), 38);

        encoded = HexConverter.hexStringToByteArray("0b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeMultiAsset(input);
        assertEquals(input.getOffset(), 45);
    }

    @Test
    void deocdeXcmResponse() {
        byte[] encoded = HexConverter.hexStringToByteArray("00040b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d");
        ByteSliceInput input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcmResponse(input);
        assertEquals(input.getOffset(), 47);
    }

    @Test
    void decodeXcm() {
        byte[] encoded = HexConverter.hexStringToByteArray("00040b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d04070b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d50d6120000000000cbd41200000000000100");
        ByteSliceInput input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcm(input);
        assertEquals(input.getOffset(), 112);

        encoded = HexConverter.hexStringToByteArray("01040b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d04070b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d50d6120000000000cbd41200000000000100");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcm(input);
        assertEquals(input.getOffset(), 112);

        encoded = HexConverter.hexStringToByteArray("02040b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d04070b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d50d6120000000000cbd41200000000000100");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcm(input);
        assertEquals(input.getOffset(), 112);

        encoded = HexConverter.hexStringToByteArray("0303abae734900040b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcm(input);
        assertEquals(input.getOffset(), 53);

        encoded = HexConverter.hexStringToByteArray("04040b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcm(input);
        assertEquals(input.getOffset(), 80);

        encoded = HexConverter.hexStringToByteArray("05040b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec04070b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec0a240b0c03637b570e414d50d6120000000000cbd41200000000000100");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcm(input);
        assertEquals(input.getOffset(), 145);

        encoded = HexConverter.hexStringToByteArray("060185b95a9830120000240b0c03637b570e414d");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcm(input);
        assertEquals(input.getOffset(), 20);

        encoded = HexConverter.hexStringToByteArray("078a72d71796a0790076340100");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcm(input);
        assertEquals(input.getOffset(), 13);

        encoded = HexConverter.hexStringToByteArray("088a72d717");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcm(input);
        assertEquals(input.getOffset(), 5);

        encoded = HexConverter.hexStringToByteArray("098a72d7178a72d7178a72d717");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcm(input);
        assertEquals(input.getOffset(), 13);
    }

    @Test
    void decodeXcmError() {
        byte[] encoded = HexConverter.hexStringToByteArray("00");
        ByteSliceInput input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcmError(input);
        assertEquals(input.getOffset(), 1);

        encoded = HexConverter.hexStringToByteArray("0b020301240b0c03637b570e414dfef353ec0301240b0c03637b570e414dfef353ec098a72d7178a72d7178a72d717");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcmError(input);
        assertEquals(input.getOffset(), 47);

        encoded = HexConverter.hexStringToByteArray("11f4e0010000000000");
        input = new ByteSliceInput(encoded);
        SizeDecoder.decodeXcmError(input);
        assertEquals(input.getOffset(), 9);
    }

    @Test
    void Outcome() {
        byte[] encoded = HexConverter.hexStringToByteArray("000000000012d677007806780a7c8d03000000");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int size = SizeDecoder.Outcome(input, 8);
        assertEquals(size, 9);

        encoded = HexConverter.hexStringToByteArray("000000000012d677017806780a7c8d030011f4e0010000000000");
        input = new ByteSliceInput(encoded);
        size = SizeDecoder.Outcome(input, 8);
        assertEquals(size, 18);

        encoded = HexConverter.hexStringToByteArray("000000000012d6770211f4e0010000000000");
        input = new ByteSliceInput(encoded);
        size = SizeDecoder.Outcome(input, 8);
        assertEquals(size, 10);
    }

    @Test
    void EvmLog() {
        byte[] encoded = HexConverter.hexStringToByteArray("0000006392341e7e5c46f8b32cd39f8e425d2916fbd0ef1108febd97fdbc2d968df381aebe0a9211f380eaf5b5370e330d4a41f8b81000f8e4febd97fdbc2d968df381aebe0a9211f380eaf5b5370e330d4a41f8b81000f8e4240b0c03637b570e414d001e51b9");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int decodedSize = SizeDecoder.EvmLog(input, 4);
        assertEquals(decodedSize, 95);
    }

    @Test
    void Bytes() {
        byte[] encoded = HexConverter.hexStringToByteArray("00000000000000000000017cddd07a404092341e7e5c46f8b32cd39f8e425d2916001e51b9");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int decodedSize = SizeDecoder.Bytes(input, 16);
        assertEquals(decodedSize, 17);
    }

    @Test
    void ExitReason() {
        byte[] encoded = HexConverter.hexStringToByteArray("000000630000001e51b9");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int decodedSize = SizeDecoder.ExitReason(input, 4);
        assertEquals(decodedSize, 2);

        encoded = HexConverter.hexStringToByteArray("00000063010d304920646f6e2774206b6e6f77001e51b9");
        input = new ByteSliceInput(encoded);
        decodedSize = SizeDecoder.ExitReason(input, 4);
        assertEquals(decodedSize, 15);

        encoded = HexConverter.hexStringToByteArray("000000630200001e51b9");
        input = new ByteSliceInput(encoded);
        decodedSize = SizeDecoder.ExitReason(input, 4);
        assertEquals(decodedSize, 2);

        encoded = HexConverter.hexStringToByteArray("0000006303020d304920646f6e2774206b6e6f77001e51b9");
        input = new ByteSliceInput(encoded);
        decodedSize = SizeDecoder.ExitReason(input, 4);
        assertEquals(decodedSize, 16);

        encoded = HexConverter.hexStringToByteArray("000000630303304920646f6e2774206b6e6f77001e51b9");
        input = new ByteSliceInput(encoded);
        decodedSize = SizeDecoder.ExitReason(input, 4);
        assertEquals(decodedSize, 15);
    }

    @Test
    void Option_AccountId() {
        byte[] encoded = HexConverter.hexStringToByteArray("000000630192341e7e5c46f8b32cd39f8e425d2916fbd0e5dfdb1818194ec02b66a52bfd85001e51b9");
        ByteSliceInput input = new ByteSliceInput(encoded);
        int decodedSize = SizeDecoder.Option_AccountId(input, 4);
        assertEquals(decodedSize, 33);
    }
}
