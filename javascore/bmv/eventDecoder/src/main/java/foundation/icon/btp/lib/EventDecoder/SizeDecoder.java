package foundation.icon.btp.lib.eventdecoder;

import java.math.BigInteger;

import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.scale.ScaleReader;

public class SizeDecoder {
    public static int accountIdSize = 32;

    public static int DispatchInfo(ByteSliceInput input, int offset) {
        return 10;
    }

    public static int DispatchError(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();
        
        int size = 1;
        input.seek(startPoint + offset);
        byte dispatchError = input.takeByte();
        if ((dispatchError & 0xff) == 3) {
            size += 2;
        }

        if ((dispatchError & 0xff) == 0x06 || (dispatchError & 0xff) == 0x07) {
            size += 1;
        }

        input.seek(startPoint);
        return size;
    }

    public static int DispatchResult(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();
        
        int size = 1;
        input.seek(startPoint + offset);
        byte dispatchResultEnum = input.takeByte();
        if ((dispatchResultEnum & 0xff) == 0x01) {
            byte dispatchErrorEnum = input.takeByte();
            size += 1;
            if ((dispatchErrorEnum & 0xff) == 0x03) {
                size += 2;
            }

            if ((dispatchErrorEnum & 0xff) == 0x06 || (dispatchErrorEnum & 0xff) == 0x07) {
                size += 1;
            }
        }

        input.seek(startPoint);
        return size;
    }

    public static int AccountId(ByteSliceInput input, int offset) {
        return accountIdSize;
    }

    public static int AccountId32(ByteSliceInput input, int offset) {
        return 32;
    }

    public static int EthereumAccountId(ByteSliceInput input, int offset) {
        return 20;
    }

    public static int Hash(ByteSliceInput input, int offset) {
        return 32;
    }

    public static int AccountIndex(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int Balance(ByteSliceInput input, int offset) {
        return 16;
    }

    public static int BalanceOf(ByteSliceInput input, int offset) {
        return 16;
    }

    public static int BalanceStatus(ByteSliceInput input, int offset) {
        return 1;
    }

    public static int EraIndex(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int SessionIndex(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int Kind(ByteSliceInput input, int offset) {
        return 16;
    }

    public static int OpaqueTimeSlot(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);

        int timeslotSize = ScaleReader.readUintCompactSize(input);
        byte[] timeslot = input.take(timeslotSize);

        int endPoint = input.getOffset();
        input.seek(startPoint);

        return endPoint - startPoint - offset;
    }

    public static int AuthorityList(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);
        int validatorSize = ScaleReader.readUintCompactSize(input);
        int compactSize = input.getOffset() - startPoint - offset;

        input.seek(startPoint);
        return (32 + 8 ) * validatorSize + compactSize;
    }

    public static int AuthorityId(ByteSliceInput input, int offset) {
        return 32;
    }

    public static int Vec_IdentificationTuple(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);
        int identificationSize = ScaleReader.readUintCompactSize(input);
        for (int i = 0; i < identificationSize; i++) {
            input.take(32); // validatorId
            BigInteger res =  ScaleReader.readCompacBigInteger(input); // totalBalance
            ScaleReader.readCompacBigInteger(input); // ownBalance
            long individualExposureSize = ScaleReader.readUintCompactSize(input);
            for (int j = 0; j < individualExposureSize; j++) {
                input.take(32); // who
                ScaleReader.readCompacBigInteger(input); // value
            }
        }
        int endPoint = input.getOffset();
        input.seek(startPoint);
        return endPoint - startPoint - offset;
    }

    public static int PropIndex(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int Vec_AccountId(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);
        int accountSize = ScaleReader.readUintCompactSize(input);
        int compactSize = input.getOffset() - startPoint - offset;

        input.seek(startPoint);
        return accountSize*accountIdSize + compactSize;
    }

    public static int ReferendumIndex(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int VoteThreshold(ByteSliceInput input, int offset) {
        return 1;
    }

    public static int bool(ByteSliceInput input, int offset) {
        return 1;
    }

    public static int BlockNumber(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int ProposalIndex(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int MemberCount(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int Vec_AccountId_Balance(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);
        int vectorSize = ScaleReader.readUintCompactSize(input);
        int compactSize = input.getOffset() - startPoint - offset;

        input.seek(startPoint);
        return vectorSize*(accountIdSize + 16) + compactSize;
    }

    public static int PhantomData(ByteSliceInput input, int offset) {
        return 0;
    }

    public static int EthereumAddress(ByteSliceInput input, int offset) {
        return 20;
    }

    public static int u32(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int RegistrarIndex(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int TaskAddress(ByteSliceInput input, int offset) {
        return 8;
    }

    public static int Option_Bytes(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);
        int isHasBytes = input.takeUByte();
        if (isHasBytes > 0) {
            byte[] b = ScaleReader.readBytes(input);
        }

        int endPoint = input.getOffset();
        input.seek(startPoint);
        return endPoint - startPoint - offset;
    }

    public static int ProxyType(ByteSliceInput input, int offset) {
        return 1;
    }

    public static int u16(ByteSliceInput input, int offset) {
        return 2;
    }

    public static int CallHash(ByteSliceInput input, int offset) {
        return 32;
    }

    public static int Timepoint(ByteSliceInput input, int offset) {
        return 8;
    }

    public static int BountyIndex(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int ElectionCompute(ByteSliceInput input, int offset) {
        return 1;
    }

    public static int Option_ElectionCompute(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);
        int isHasBytes = input.takeUByte();
        if (isHasBytes > 0) {
            input.takeByte();
        }

        int endPoint = input.getOffset();
        input.seek(startPoint);
        return endPoint - startPoint - offset;
    }

    public static int ActiveIndex(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int CandidateReceipt(ByteSliceInput input, int offset) {
        // descriptor: 'CandidateDescriptor',
        //     paraId: 'ParaId',  u32
        //     relayParent: 'RelayChainHash', Hash
        //     collatorId: 'CollatorId', H256
        //     persistedValidationDataHash: 'Hash',
        //     povHash: 'Hash',
        //     erasureRoot: 'Hash',
        //     signature: 'CollatorSignature', 64bytes
        //     paraHead: 'Hash',
        //     validationCodeHash: 'ValidationCodeHash' Hash
        // commitmentsHash: 'Hash'
        return 324;
    }

    public static int HeadData(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);
        byte[] headData = ScaleReader.readBytes(input);

        int endPoint = input.getOffset();
        input.seek(startPoint);
        return endPoint - startPoint - offset;
    }

    public static int CoreIndex(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int GroupIndex(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int ParaId(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int MessageId(ByteSliceInput input, int offset) {
        return 32;
    }

    public static void decodeJunction(ByteSliceInput input) {
        int junctionEnum = input.takeUByte();
        switch (junctionEnum) {
            case 1: // Parachain: 'Compact<u32>',
                ScaleReader.readCompacBigInteger(input);
                break;
            case 2: // AccountId32: 'AccountId32Junction',
                int networkIdEnum = input.takeUByte();
                if (networkIdEnum == 1) {
                    ScaleReader.readBytes(input);
                }
                input.take(32); // accountId
                break;
            case 3: // AccountIndex64: 'AccountIndex64Junction',
                networkIdEnum = input.takeUByte();
                if (networkIdEnum == 1) {
                    ScaleReader.readBytes(input);
                }
                ScaleReader.readCompacBigInteger(input); // index: 'Compact<u64>'
                break;
            case 4: // AccountId32: 'AccountId32Junction',
                networkIdEnum = input.takeUByte();
                if (networkIdEnum == 1) {
                    ScaleReader.readBytes(input);
                }
                input.take(20); // index: '[u8; 20]'
                break;
            case 5:
                input.takeByte(); // PalletInstance: 'u8',
                break;
            case 6:
            ScaleReader.readCompacBigInteger(input); // GeneralIndex: 'Compact<u128>',
                break;
            case 7:
            ScaleReader.readBytes(input);
                break;
            case 9: // Plurality: 'PluralityJunction'
                int bodyIdEnum = input.takeUByte();
                if (bodyIdEnum == 1) {
                    ScaleReader.readBytes(input); // Named: 'Vec<u8>',
                }
                if (bodyIdEnum == 2) {
                    ScaleReader.readCompacBigInteger(input); // Index: 'Compact<u32>',
                }

                int bodyPartEnum = input.takeUByte();
                if (bodyPartEnum == 1) {
                    ScaleReader.readCompacBigInteger(input); // Members: 'Compact<u32>',
                }
                if (bodyPartEnum > 1) {
                    ScaleReader.readCompacBigInteger(input); // nom: 'Compact<u32>', denom: 'Compact<u32>'
                    ScaleReader.readCompacBigInteger(input);
                }
                break;
            default:
                break;
        }
    }

    public static void decodeAssetInstance(ByteSliceInput input) {
        int assetInstanceEnum = input.takeUByte();
        switch (assetInstanceEnum) {
            case 1:
                input.takeByte();
                break;
            case 2:
            case 3:
            case 4:
            case 5:
                ScaleReader.readCompacBigInteger(input);
                break;
            case 6:
                input.take(4);
                break;
            case 7:
                input.take(8);
                break;
            case 8:
                input.take(16);
                break;
            case 9:
                input.take(32);
                break;
            case 10:
                ScaleReader.readBytes(input);
                break;
            default:
                break;
        }
    }

    public static void decodeMultiAsset(ByteSliceInput input) {
        int multiAssetEnum = input.takeUByte();
        switch (multiAssetEnum) {
            case 4:
            case 5:
                ScaleReader.readBytes(input);
                break;
            case 6:
            case 7:
                decodeMultiLocation(input);
                break;
            case 8:
                ScaleReader.readBytes(input);
                ScaleReader.readCompacBigInteger(input);
                break;
            case 9:
                ScaleReader.readBytes(input);
                decodeAssetInstance(input);
                break;
            case 10:
                decodeMultiLocation(input);
                ScaleReader.readCompacBigInteger(input);
                break;
            case 11:
                decodeMultiLocation(input);
                decodeAssetInstance(input);
                break;
            default:
                break;
        }
    }

    public static void decodeXcmOrder(ByteSliceInput input) {
        int xcmOrderEnum = input.takeUByte();
        switch (xcmOrderEnum) {
            case 1:
                int multiAssetSize = ScaleReader.readUintCompactSize(input);
                for (int i = 0; i< multiAssetSize; i++) {
                    decodeMultiAsset(input);
                }
                decodeMultiLocation(input);
                break;
            case 2:
            case 4:
            case 5:
                multiAssetSize = ScaleReader.readUintCompactSize(input);
                for (int i = 0; i< multiAssetSize; i++) {
                    decodeMultiAsset(input);
                }
                decodeMultiLocation(input);

                int xcmOrderSize = ScaleReader.readUintCompactSize(input);
                for (int i = 0; i< xcmOrderSize; i++) {
                    decodeXcmOrder(input);
                }
                break;
            case 3:
                multiAssetSize = ScaleReader.readUintCompactSize(input);
                for (int i = 0; i< multiAssetSize; i++) {
                    decodeMultiAsset(input);
                }

                multiAssetSize = ScaleReader.readUintCompactSize(input);
                for (int i = 0; i< multiAssetSize; i++) {
                    decodeMultiAsset(input);
                }
                break;
            case 6:
                ScaleReader.readCompacBigInteger(input);
                decodeMultiLocation(input);
                multiAssetSize = ScaleReader.readUintCompactSize(input);
                for (int i = 0; i< multiAssetSize; i++) {
                    decodeMultiAsset(input);
                }
                break;
            case 7:
                decodeMultiAsset(input);
                input.take(17);
                int xcmSize = ScaleReader.readUintCompactSize(input);
                for (int i = 0; i< xcmSize; i++) {
                    decodeXcm(input);
                }
                break;
            default:
                break;
        }
    }


    public static void decodeMultiLocation(ByteSliceInput input) {
        int multiLocationEnum = input.takeUByte();
        for (int i = 0; i < multiLocationEnum; i++) {
            decodeJunction(input);
        }
    }

    public static void decodeXcmResponse(ByteSliceInput input) {
        int xcmResponseEnum = input.takeUByte();
        switch (xcmResponseEnum) {
            case 0:
                int multiAssetSize = ScaleReader.readUintCompactSize(input);
                for (int i = 0; i< multiAssetSize; i++) {
                    decodeMultiAsset(input);
                }
            default:
                break;
        }
    }

    public static void decodeXcm(ByteSliceInput input) {
        int xcmEnum = input.takeUByte();
        switch (xcmEnum) {
            case 0:
            case 1:
            case 2:
                int multiAssetSize = ScaleReader.readUintCompactSize(input);
                for (int i = 0; i< multiAssetSize; i++) {
                    decodeMultiAsset(input);
                }
                int xcmOrderSize = ScaleReader.readUintCompactSize(input);
                for (int i = 0; i< xcmOrderSize; i++) {
                    decodeXcmOrder(input);
                }
                break;
            case 3:
                ScaleReader.readCompacBigInteger(input);
                decodeXcmResponse(input);
                break;
            case 4:
                multiAssetSize = ScaleReader.readUintCompactSize(input);
                for (int i = 0; i< multiAssetSize; i++) {
                    decodeMultiAsset(input);
                }
                decodeMultiLocation(input);
                break;
            case 5:
                multiAssetSize = ScaleReader.readUintCompactSize(input);
                for (int i = 0; i< multiAssetSize; i++) {
                    decodeMultiAsset(input);
                }
                decodeMultiLocation(input);
                xcmOrderSize = ScaleReader.readUintCompactSize(input);
                for (int i = 0; i< xcmOrderSize; i++) {
                    decodeXcmOrder(input);
                }
                break;
            case 6:
                input.takeByte();
                input.take(8);
                ScaleReader.readBytes(input);
                break;
            case 7:
                ScaleReader.readCompacBigInteger(input);
                ScaleReader.readCompacBigInteger(input);
                ScaleReader.readCompacBigInteger(input);
                break;
            case 8:
                ScaleReader.readCompacBigInteger(input);
                break;
            case 9:
                ScaleReader.readCompacBigInteger(input);
                ScaleReader.readCompacBigInteger(input);
                ScaleReader.readCompacBigInteger(input);
                break;
            case 10:
                decodeMultiLocation(input);
                decodeXcm(input);
            default:
                break;
        }
    }

    public static void decodeXcmError(ByteSliceInput input) {
        int xcmErrorEnum = input.takeUByte();
        switch (xcmErrorEnum) {
            case 11:
                decodeMultiLocation(input);
                decodeXcm(input);
                break;
            case 17:
                input.take(8);
                break;
            default:
                break;
        }
    }

    public static int Outcome(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);
        int outComeEnum = input.takeUByte();
        if (outComeEnum == 0) {
            input.take(8); // Weight
        }

        if (outComeEnum == 1) {
            input.take(8); // Weight
            decodeXcmError(input);
        }

        if (outComeEnum == 2) {
            input.take(8); // Weight
            decodeXcmError(input);
        }

        int endPoint = input.getOffset();
        input.seek(startPoint);
        return endPoint - startPoint - offset;
    }

    public static int Weight(ByteSliceInput input, int offset) {
        return 8;
    }

    public static int HrmpChannelId(ByteSliceInput input, int offset) {
        return 8;
    }

    public static int LeasePeriod(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int SlotRange(ByteSliceInput input, int offset) {
        return 1;
    }

    public static int AuctionIndex(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int Bytes(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);
        ScaleReader.readBytes(input);

        int endPoint = input.getOffset();
        input.seek(startPoint);

        return endPoint - startPoint - offset;
    }

    public static int MultiLocation(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);
        decodeMultiLocation(input);

        int endPoint = input.getOffset();
        input.seek(startPoint);

        return endPoint - startPoint - offset;
    }

    public static int Xcm(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);
        decodeXcm(input);

        int endPoint = input.getOffset();
        input.seek(startPoint);

        return endPoint - startPoint - offset;
    }

    public static int EvmLog(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();
        input.seek(startPoint + offset);

        byte[] address = input.take(20); // 20 bytes address of contract
        int topicSize = ScaleReader.readUintCompactSize(input); // u32 compact number of item in list
        for (int i = 0; i < topicSize; i++) {
            input.take(32); // 32 bytes of topic;
        }

        int evmDataSize = ScaleReader.readUintCompactSize(input); // u32 compact number of bytes of evm data
        byte[] evmData = input.take(evmDataSize);

        int endPoint = input.getOffset();
        input.seek(startPoint);

        return endPoint - startPoint - offset;
    }

    public static int H160(ByteSliceInput input, int offset) {
        return 20;
    }

    public static int U256(ByteSliceInput input, int offset) {
        return 32;
    }

    public static int ChainId(ByteSliceInput input, int offset) {
        return 1;
    }

    public static int DepositNonce(ByteSliceInput input, int offset) {
        return 8;
    }

    public static int ResourceId(ByteSliceInput input, int offset) {
        return 32;
    }

    public static int AssetId(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int TAssetBalance(ByteSliceInput input, int offset) {
        return 8;
    }

    public static int H256(ByteSliceInput input, int offset) {
        return 32;
    }

    public static byte decodeExitError(ByteSliceInput input) {
        byte error = input.takeByte();
        if ((error & 0xff) == 0x0d) {
            int textSize = ScaleReader.readUintCompactSize(input);
            input.take(textSize);
        }
        return error;
    }

    public static int ExitReason(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();
        input.seek(startPoint + offset);

        byte exitReasonEnum = input.takeByte();
        if ((exitReasonEnum & 0xff) == 0x00) {
            byte success = input.takeByte();
        } else if ((exitReasonEnum & 0xff) == 0x01) {
            decodeExitError(input);
        } else if ((exitReasonEnum & 0xff) == 0x02) {
            byte revert = input.takeByte();
        } else if ((exitReasonEnum & 0xff) == 0x03) {
            byte fatal = input.takeByte();
            if ((fatal & 0xff) == 0x02) {
                decodeExitError(input);
            }

            if ((fatal & 0xff) == 0x03) {
                int textSize = ScaleReader.readUintCompactSize(input);
                input.take(textSize);
            }
        }

        int endPoint = input.getOffset();
        input.seek(startPoint);

        return endPoint - startPoint - offset;
    }

    public static int RelayChainBlockNumber(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int RoundIndex(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int Percent(ByteSliceInput input, int offset) {
        return 1;
    }

    public static int Perbill(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int RelayChainAccountId(ByteSliceInput input, int offset) {
        return 32;
    }

    public static int Option_AccountId(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);
        int isHasAccountId = input.takeUByte();
        if (isHasAccountId > 0) {
            input.take(accountIdSize);
        }

        int endPoint = input.getOffset();
        input.seek(startPoint);
        return endPoint - startPoint - offset;
    }

    public static int AuthorId(ByteSliceInput input, int offset) {
        return 32;
    }

    public static int CurrencyId(ByteSliceInput input, int offset) {
        return 8;
    }

    public static int CurrencyIdOf(ByteSliceInput input, int offset) {
        return 8;
    }

    public static int AmountOf(ByteSliceInput input, int offset) {
        return 16;
    }

    public static int ClassId(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int ClassIdOf(ByteSliceInput input, int offset) {
        return 4;
    }

    public static int TokenId(ByteSliceInput input, int offset) {
        return 8;
    }

    public static int TokenIdOf(ByteSliceInput input, int offset) {
        return 8;
    }

    public static int u8(ByteSliceInput input, int offset) {
        return 1;
    }

    public static int DustHandlerType(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();

        input.seek(startPoint + offset);
        int dustHandlerTypeEnum = input.takeUByte();
        if (dustHandlerTypeEnum > 0) {
            input.take(accountIdSize);
        }

        int endPoint = input.getOffset();
        input.seek(startPoint);
        return endPoint - startPoint - offset;
    }

    public static int NominatorAdded(ByteSliceInput input, int offset) {
        int startPoint = input.getOffset();
        
        input.seek(startPoint + offset);

        int size = 1;
        byte nominatorAddedEnum = input.takeByte();
        if ((nominatorAddedEnum & 0xff) == 1) {
            size += 16;
        }

        input.seek(startPoint);
        return size;
    }
}