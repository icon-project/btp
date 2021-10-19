/*
 * Copyright 2021 ICON Foundation
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
package foundation.icon.btp.bmv;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import score.Context;
import score.ObjectReader;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EthereumExamplesDecodeTest {
    final static String RLPn = "RLPn";

    private class DecodeTest {
        String input;
        String output;
        Exception ex;
        public DecodeTest(String in, String out) {
            input = in;
            output = out;
        }
        public DecodeTest(String in, Exception e) {
            input = in;
            ex = e;
        }
        public boolean throwsException() {
            return ex != null;
        }
    }

    private String convertByteArrayToString(byte[] arr) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for(int i = 0; i < arr.length-1; i++) {
            sb.append(arr[i]);
            sb.append(',');
        }
        sb.append(arr[arr.length-1]);
        sb.append('}');
        return sb.toString();
    }

    @Test
    public void exampleDecode() {
        byte[] input = Hex.decode("C90A1486666F6F626172");
        if (input == null) {
            System.out.println("ERROR");
        }
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, input);
        reader.beginList();
        int A = reader.readInt();
        int B = reader.readInt();
        String output = reader.readString();

        assertEquals(10, A);
        assertEquals(20, B);
        assertEquals("foobar", output);
    }

    @Test
    public void exampleDecode_structTagNil() {
        byte[] input = {(byte) 0xC1, (byte) 0x80};
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, input);
        reader.beginList();
        String output = reader.readString();
        assertEquals("", output);
    }

    @Test
    public void testDecodeBoolean() {
        List<DecodeTest> decodeTests = new ArrayList<>();
        decodeTests.add(new DecodeTest("01", "true"));
//        decodeTests.add(new DecodeTest("80", "false")); // expected false, but error
//        decodeTests.add(new DecodeTest("02", new IllegalStateException())); // expected error, but true

        for(int i = 0; i < decodeTests.size(); i++) {
            DecodeTest decodeTest = decodeTests.get(i);
            byte[] input = Hex.decode(decodeTest.input);
            if (input == null) { System.out.println("Error: Input is null"); }
            ObjectReader reader = Context.newByteArrayObjectReader(RLPn, input);

            if(decodeTest.throwsException()) {
                assertThrows(decodeTest.ex.getClass(), () -> reader.readBoolean());
            } else {
                boolean output = reader.readBoolean();
                System.out.println("input: " + decodeTests.get(i) + ", output: " + output);
            }
        }
    }

    @Test
    public void testDecodeInteger() {
        List<DecodeTest> decodeTests = new ArrayList<>();
        decodeTests.add(new DecodeTest("05", "5"));
//        decodeTests.add(new DecodeTest("80", "0")); // expected 0, but error
        decodeTests.add(new DecodeTest("820505", "1285"));
        decodeTests.add(new DecodeTest("83050505", "328965"));
        decodeTests.add(new DecodeTest("8405050505", "84215045"));
//        decodeTests.add(new DecodeTest("850505050505", "84215045")); // rlp: input string too long for uint32, but same as "8405050505"
        decodeTests.add(new DecodeTest("C0", new IllegalStateException())); // rlp: expected input string or byte for uint32, -> error
        decodeTests.add(new DecodeTest("00", "0")); // rlp: non-canonical integer (leading zero bytes) for uint32, but 0
//        decodeTests.add(new DecodeTest("8105", new IllegalStateException())); // rlp: non-canonical size information for uint32, but 5
//        decodeTests.add(new DecodeTest("820004", new IllegalStateException())); // rlp: non-canonical integer (leading zero bytes) for uint32, but 4
//        decodeTests.add(new DecodeTest("B8020004", new IllegalStateException())); // rlp: non-canonical size information for uint32, but 4

        for(int i = 0; i < decodeTests.size(); i++) {
            DecodeTest decodeTest = decodeTests.get(i);
            byte[] input = Hex.decode(decodeTest.input);
            if (input == null) { System.out.println("Error: Input is null"); }
                ObjectReader reader = Context.newByteArrayObjectReader(RLPn, input);
                if(decodeTest.throwsException()) {
                    assertThrows(decodeTest.ex.getClass(), () -> reader.readInt());
                } else {
                    Integer output = reader.readInt();
                    System.out.println("i: " + i + ", input: " + decodeTest.input + ", output: " + output);
                    assertEquals(decodeTest.output, output.toString());
                }
        }
    }

    @Test
    public void testDecodeByteArray() {
        List<DecodeTest> decodeTests = new ArrayList<>();
        decodeTests.add(new DecodeTest("01", "{1}")); // {1} -> true
//        decodeTests.add(new DecodeTest("80", "")); // {} -> true
        decodeTests.add(new DecodeTest("8D6162636465666768696A6B6C6D", "{97,98,99,100,101,102,103,104,105,106,107,108,109}")); // abcdefghijklm -> true
        decodeTests.add(new DecodeTest("C0", new IllegalStateException())); // rlp: expected input string or byte for []uint8, error -> true
//        decodeTests.add(new DecodeTest("8105", "")); // rlp: non-canonical size information for []uint8, but{5} !!!
        decodeTests.add(new DecodeTest("02", "{2}")); // {2} -> true
//        decodeTests.add(new DecodeTest("8180", "{128}")); // {128}, but {-128} !!!
        decodeTests.add(new DecodeTest("850102030405", "{1,2,3,4,5}")); // {1,2,3,4,5} -> true
        decodeTests.add(new DecodeTest("C3010203", new IllegalStateException())); // rlp: expected input string or byte for [5]uint8, error
        decodeTests.add(new DecodeTest("86010203040506", "{1,2,3,4,5,6}")); // actual {1,2,3,4,5,6} -> true
//        decodeTests.add(new DecodeTest("817F", new IllegalStateException())); // rlp: non-canonical size information for [1]uint8, but {127} !!!

        for(int i = 0; i < decodeTests.size(); i++) {
            DecodeTest decodeTest = decodeTests.get(i);
            byte[] input = Hex.decode(decodeTest.input);
            if (input == null) { System.out.println("Error: Input is null"); }
            ObjectReader reader = Context.newByteArrayObjectReader(RLPn, input);
            if(decodeTest.throwsException()) {
                assertThrows(decodeTest.ex.getClass(), () -> reader.readByteArray());
            } else {
                byte[] output = reader.readByteArray();
                System.out.println("i: " + i + ", input: " + decodeTest.input + ", output: " + convertByteArrayToString(output));
                assertEquals(decodeTest.output, convertByteArrayToString(output));
            }
        }
    }

    @Test
    public void testDecodeString() {
        List<DecodeTest> decodeTests = new ArrayList<>();
        decodeTests.add(new DecodeTest("00", "\000")); //"\000" -> true
        decodeTests.add(new DecodeTest("8D6162636465666768696A6B6C6D", "abcdefghijklm")); // "abcdefghijklm" -> true
        decodeTests.add(new DecodeTest("C0", new IllegalStateException())); // error rlp: expected input string or byte for string, error -> true

        for(int i = 0; i < decodeTests.size(); i++) {
            DecodeTest decodeTest = decodeTests.get(i);
            byte[] input = Hex.decode(decodeTest.input);
            if (input == null) { System.out.println("Error: Input is null"); }
            ObjectReader reader = Context.newByteArrayObjectReader(RLPn, input);
            if(decodeTest.throwsException()) {
                assertThrows(decodeTest.ex.getClass(), () -> reader.readString());
            } else {
                String output = reader.readString();
                System.out.println("i: " + i + ", input: " + decodeTest.input + ", output: " + output);
                assertEquals(decodeTest.output, output);
            }
        }
    }

    @Test
    public void testDecodeBigInt() {
        List<DecodeTest> decodeTests = new ArrayList<>();
//        decodeTests.add(new DecodeTest("80", "0")); // expected 0, was error !!!
        decodeTests.add(new DecodeTest("01", "1")); // expected 1, was 1 -> true
//        decodeTests.add(new DecodeTest("89FFFFFFFFFFFFFFFFFF", "")); // expected veryBigInt, was -1 !!!
//        decodeTests.add(new DecodeTest("B848FFFFFFFFFFFFFFFFF800000000000000001BFFFFFFFFFFFFFFFFC8000000000000000045FFFFFFFFFFFFFFFFC800000000000000001BFFFFFFFFFFFFFFFFF8000000000000000001", ""));
        // !!! expected veryVeryBigInt, was -418993997810706159361377742188191160219457531023272903013737175939963828137567717399381613215210242049250576467864511639025666769248495914890316335808511
        decodeTests.add(new DecodeTest("10", "16")); // expected 16, was 16 -> true
        decodeTests.add(new DecodeTest("C0", new IllegalStateException())); // expected error rlp: expected input string or byte for *big.Int, was error -> true
//        decodeTests.add(new DecodeTest("00", new IllegalStateException())); // expected error rlp: non-canonical integer (leading zero bytes) for *big.Int, was 0 !!!
//        decodeTests.add(new DecodeTest("820001", new IllegalStateException())); // error rlp: non-canonical integer (leading zero bytes) for *big.Int, was 1 !!!
//        decodeTests.add(new DecodeTest("8105", new IllegalStateException())); // error rlp: non-canonical size information for *big.Int, was 5 !!!

        for(int i = 0; i < decodeTests.size(); i++) {
            DecodeTest decodeTest = decodeTests.get(i);
            byte[] input = Hex.decode(decodeTest.input);
            if (input == null) { System.out.println("Error: Input is null"); }
            ObjectReader reader = Context.newByteArrayObjectReader(RLPn, input);
            if(decodeTest.throwsException()) {
                assertThrows(decodeTest.ex.getClass(), () -> reader.readBigInteger());
            } else {
                BigInteger output = reader.readBigInteger();
                System.out.println("i: " + i + ", input: " + decodeTest.input + ", output: " + output);
                assertEquals(decodeTest.output, output.toString());
            }
        }
    }

    @Test
    public void testDecodeStruct() {
        // input 1
        byte[] input1 = Hex.decode("C50583343434");
        if (input1 == null) { System.out.println("Error: Input is null"); }
        try {
            ObjectReader reader = Context.newByteArrayObjectReader(RLPn, input1);
            reader.beginList();
            Integer id = reader.readInt();
            String name = reader.readString();
            System.out.println("i: " + 0 + ", id: " + id + ", name: " + name);
            assertEquals(5, id);
            assertEquals("444", name);
        } catch (Exception ex) {}

        // input 2
        byte[] input2 = Hex.decode("C601C402C203C0");
        if (input2 == null) { System.out.println("Error: Input is null"); }
        try {
            ObjectReader reader = Context.newByteArrayObjectReader(RLPn, input2);
            reader.beginList();
            reader.beginList();
            reader.beginList();
            Integer id = reader.readNullable(Integer.class);
            System.out.println("i: " + 1 + ", id: " + id);
            assertEquals(2, id);
        } catch (Exception ex) {}

        // input 3
        byte[] input3 = Hex.decode("C58083343434");
        if (input3 == null) { System.out.println("Error: Input is null"); }
        try {
            ObjectReader reader = Context.newByteArrayObjectReader(RLPn, input3);
            reader.beginList();
            BigInteger id = reader.readBigInteger();
            String name = reader.readString();
            System.out.println("i: " + 2 + ", id: " + id + ", name: " + name);
            assertEquals("444", name);
        } catch (Exception ex) {}

        // input 4
        byte[] input4 = Hex.decode("C3010203");
        if (input3 == null) { System.out.println("Error: Input is null"); }
        try {
            ObjectReader reader = Context.newByteArrayObjectReader(RLPn, input4);
            reader.beginList();
            BigInteger id = reader.readBigInteger();
            byte id1 = reader.readByte();
            byte id2 = reader.readByte();
            System.out.println("i: " + 3 + ", id: " + id + ", id1: " + id1 + ", id2: " + id2);
            assertEquals("1", id.toString());
            assertEquals(2, id1);
            assertEquals(3, id2);
        } catch (Exception ex) {}
    }

    @Test
    public void testDecodeStructErrors() {
        List<DecodeTest> decodeTests = new ArrayList();
        decodeTests.add(new DecodeTest("C0", new IndexOutOfBoundsException()));
        decodeTests.add(new DecodeTest("C105", new IndexOutOfBoundsException()));
        decodeTests.add(new DecodeTest("C7C50583343434C0", new IllegalStateException()));
        decodeTests.add(new DecodeTest("83222222", new IllegalStateException()));
        decodeTests.add(new DecodeTest("C3010101", new IllegalStateException()));
        decodeTests.add(new DecodeTest("C501C3C00000", new IllegalStateException()));
        decodeTests.add(new DecodeTest("C103", new IllegalStateException()));
        decodeTests.add(new DecodeTest("C50102C20102", new IllegalStateException()));
        decodeTests.add(new DecodeTest("C0", new IllegalStateException()));

        for(int i = 0; i < decodeTests.size(); i++) {
            DecodeTest decodeTest = decodeTests.get(2);
            byte[] input1 = Hex.decode(decodeTest.input);
            if (input1 == null) { System.out.println("Error: Input is null"); }
            if(decodeTest.throwsException()) {
                System.out.println("i: " + i);
                ObjectReader reader = Context.newByteArrayObjectReader(RLPn, input1);
                reader.beginList();
                assertThrows(decodeTest.ex.getClass(), () -> reader.readInt());
                assertThrows(decodeTest.ex.getClass(), () -> reader.readString());
            }
        }
    }
}
