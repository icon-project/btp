package foundation.icon.btp.bmv.edgeware;

import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.ObjectReader;
import score.ByteArrayObjectWriter;

import scorex.util.Base64;
import scorex.util.ArrayList;

import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.BlockVerifyResult;
import foundation.icon.btp.lib.Constant;
import foundation.icon.btp.lib.ErrorCode;
import foundation.icon.btp.lib.event.evmevent.BTPMessageEvmEvent;
import foundation.icon.btp.lib.blockproof.BlockProof;
import foundation.icon.btp.lib.blockupdate.BlockHeader;
import foundation.icon.btp.lib.blockupdate.BlockUpdate;
import foundation.icon.btp.lib.btpaddress.BTPAddress;
import foundation.icon.btp.lib.event.EVMLogEvent;
import foundation.icon.btp.lib.event.EventRecord;
import foundation.icon.btp.lib.event.NewAuthoritiesEvent;
import foundation.icon.btp.lib.exception.RelayMessageRLPException;

import foundation.icon.btp.lib.mta.MTAStatus;
import foundation.icon.btp.lib.mta.MerkleTreeAccumulator;
import foundation.icon.btp.lib.mta.SerializableMTA;

import foundation.icon.btp.lib.mpt.MPTNode;
import foundation.icon.btp.lib.mpt.MPTNodeType;

import foundation.icon.btp.lib.relaymessage.RelayMessage;
import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.stateproof.StateProof;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.utils.HexConverter;
import foundation.icon.btp.lib.validators.Validators;

// import foundation.icon.btp.lib.eventdecoder.EventDecoder;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BMV {
  private final VarDB<Address> bmcDb = Context.newVarDB("bmc", Address.class);
  private final VarDB<String> netAddressDb = Context.newVarDB("netAddr", String.class);
  private final VarDB<Long> lastHeightDb = Context.newVarDB("lastHeight", Long.class);
  private final VarDB<SerializableMTA> mtaDb = Context.newVarDB("mta", SerializableMTA.class);
  private final VarDB<Validators> validatorsDb = Context.newVarDB("validators", Validators.class);
  private final VarDB<Address> eventDecoderDb = Context.newVarDB("eventDecoder", Address.class);
  private final VarDB<BigInteger> setIdDb = Context.newVarDB("setId", BigInteger.class);

  public BMV(
    Address bmc,
    String net,
    String encodedValidators,
    long mtaOffset,
    int mtaRootSize,
    int mtaCacheSize,
    boolean mtaIsAllowNewerWitness,
    byte[] lastBlockHash,
    Address eventDecoderAddress,
    long currentSetId
  ) {
    this.bmcDb.set(bmc);
    this.netAddressDb.set(net);

    byte[] serializedValidator = Base64.getUrlDecoder().decode(encodedValidators.getBytes());

    ObjectReader r = Context.newByteArrayObjectReader("RLPn", serializedValidator);
    r.beginList();
    List<byte[]> validators = new ArrayList<byte[]>(150);
    while (r.hasNext()) {
        byte[] v = r.readByteArray();
        validators.add(v);
    }
    r.end();

    Validators validatorsObject = new Validators(validators);
    this.validatorsDb.set(validatorsObject);
    this.setIdDb.set(BigInteger.valueOf(currentSetId));

    SerializableMTA mta = new SerializableMTA(0, mtaOffset, mtaRootSize, mtaCacheSize, mtaIsAllowNewerWitness, lastBlockHash, null, null);
    this.lastHeightDb.set(mtaOffset);
    this.mtaDb.set(mta);

    this.eventDecoderDb.set(eventDecoderAddress);

    // TODO call bmc to addVerifier
    // Context.call(bmc, "addVerifier", Context.getAddress());
  }

  /**
   * get encode of merkle tree accumulator
   */
  @External(readonly=true)
  public String mta() {
    ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
    SerializableMTA.writeObject(w, this.mtaDb.get());
    return new String(Base64.getUrlEncoder().encode(w.toByteArray()));
  }

  /**
   * get current mta height
   */
  @External(readonly=true)
  public long mtaHeight() {
    return this.mtaDb.get().height();
  }

  /**
   * get current mta height
   */
  @External(readonly=true)
  public List<byte[]> mtaRoot() {
    return this.mtaDb.get().getRootList();
  }

  /**
   * get last block hash
   */
  @External(readonly=true)
  public byte[] mtaLastBlockHash() {
    return this.mtaDb.get().lastBlockHash();
  }

  /**
   * get current mta offset
   */
  @External(readonly=true)
  public long mtaOffset() {
    return this.mtaDb.get().offset();
  }

  @External(readonly=true)
  public List<byte[]> mtaCaches() {
    return this.mtaDb.get().getCacheList();
  }

  /**
   * handle verify message from BMC and return list of event message
   */
  @External
  public List<byte[]> handleRelayMessage(String bmc, String prev, BigInteger seq, String msg) {
    BTPAddress currentAddress = BTPAddress.fromString(bmc);
    BTPAddress prevAddress = BTPAddress.fromString(prev);
    this.checkAccessible(currentAddress, prevAddress);

    byte[] serializedMsg;
    try {
      serializedMsg = Base64.getUrlDecoder().decode(msg.getBytes());
    } catch (Exception e) {
      Context.revert(ErrorCode.DECODE_ERROR, "decode base64 msg error: " + e.toString());
      serializedMsg = null;
    }

    RelayMessage relayMessage;
    try {
      relayMessage = RelayMessage.fromBytes(serializedMsg);
    } catch (RelayMessageRLPException e) {
      Context.revert(ErrorCode.DECODE_ERROR, "RelayMessage RLP decode error: " + e.getScope() + " " + e.getOriginalError());
      relayMessage = null;
    }

    if (relayMessage.getBlockUpdate().size() == 0  && relayMessage.getBlockProof() == null) {
      Context.revert(ErrorCode.BMV_ERROR, "invalid RelayMessage not exists BlockUpdate or BlockProof");
    }

    BlockVerifyResult blockVerifyResult = this.verifyBlock(relayMessage);
    
    BigInteger nextSeq = seq.add(BigInteger.valueOf(1));

    // list of bytes message return to bmc
    List<byte[]> bmcMsgs = new ArrayList<byte[]>(5);
    for (StateProof stateProof : relayMessage.getStateProof()) {
      byte[] encodedStorage = stateProof.prove(blockVerifyResult.stateRoot);
      if (Arrays.equals(stateProof.getKey(), Constant.EventStorageKey)) {
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) Context.call(this.eventDecoderDb.get(), "decodeEvent", encodedStorage);
        for (Map<String, Object> rawEventRecord: eventRecords) {
          // update new validator set
          EventRecord eventRecord = new EventRecord(rawEventRecord);
          if (eventRecord.getEventIndex()[0] == Constant.NewAuthoritiesEventIndex[0] && eventRecord.getEventIndex()[1] == Constant.NewAuthoritiesEventIndex[1]) {
            NewAuthoritiesEvent newAuthoritiesEvent = new NewAuthoritiesEvent(eventRecord.getEventData());
            List<byte[]> newValidators = newAuthoritiesEvent.getValidators();
            Validators newValidatorsObject = new Validators(newValidators);
            this.validatorsDb.set(newValidatorsObject);
            this.setIdDb.set(this.setIdDb.get().add(BigInteger.valueOf(1)));
            continue;
          }

          // filter evm log of BMC
          if (eventRecord.getEventIndex()[0] == Constant.EvmEventIndex[0] && eventRecord.getEventIndex()[1] == Constant.EvmEventIndex[1]) {
            EVMLogEvent evmLogEvent = new EVMLogEvent(eventRecord.getEventData());
            if (evmLogEvent.getEvmTopics().size() == 0 || !Arrays.equals(evmLogEvent.getEvmTopics().get(0), Constant.MessageEventTopic)) {
              continue;
            }

            if (!Arrays.equals(evmLogEvent.getAddress(), HexConverter.hexStringToByteArray(prevAddress.getAddresss()))) {
              continue;
            }

            BTPMessageEvmEvent btpMessageEvmEvent = new BTPMessageEvmEvent(evmLogEvent.getEvmEventData());

            if (btpMessageEvmEvent.getNextBmc().equals(bmc)) {
              int seqCompareRes = btpMessageEvmEvent.getSeq().compareTo(nextSeq);
              if (seqCompareRes > 0) {
                Context.revert(ErrorCode.INVALID_SEQUENCE_HIGHER, "invalid sequence: " + btpMessageEvmEvent.getSeq() + "; expected: " + nextSeq);
              } else if (seqCompareRes < 0) {
                Context.revert(ErrorCode.INVALID_SEQUENCE, "invalid sequence: " + btpMessageEvmEvent.getSeq() + "; expected: " + nextSeq);
              }
              bmcMsgs.add(btpMessageEvmEvent.getMsg());
            }
            nextSeq = seq.add(BigInteger.valueOf(1));
          }
        }
      }
    }

    if (bmcMsgs.size() > 0) {
      this.lastHeightDb.set(blockVerifyResult.lastHeight);
    }

    return bmcMsgs;
  }

  /**
   * get address of bmc
   */
  @External(readonly=true)
  public Address bmc() {
    return this.bmcDb.get();
  }

  /**
   * get address of bmc
   */
  @External(readonly=true)
  public Address eventDecoder() {
    return this.eventDecoderDb.get();
  }

  /**
   * net address that bmv verify
   */
  @External(readonly=true)
  public String netAddress() {
    return this.netAddressDb.get();
  }

  /**
   * last height
   */
  @External(readonly=true)
  public long lastHeight() {
    return this.lastHeightDb.get();
  }

  /**
   * list public keys of validators
   */
  @External(readonly=true)
  public List<byte[]> validators() {
    Validators v = this.validatorsDb.get();
    return v.get();
  }

  /**
   * get status of BMV
   */
  @External(readonly=true)
  public BMVStatus getStatus() {
    SerializableMTA mta = this.mtaDb.get();
    MTAStatus mtaStatus = mta.getStatus();
    long lastHeight = this.lastHeightDb.get();
    return new BMVStatus(mtaStatus.height, mtaStatus.offset, lastHeight);
  }

  private void checkAccessible(BTPAddress currentAddress, BTPAddress prevAddress) {
    if (!this.netAddressDb.get().equals(prevAddress.getNet())) {
      Context.revert(ErrorCode.NOT_ACCEPTED_FROM_NETWORK_ERROR, "not acceptable from");
    }
    if (!Context.getCaller().equals(this.bmcDb.get())) {
      Context.revert(ErrorCode.NOT_ACCEPTED_BMC_ADDR_ERROR, "not acceptable bmc");
    }
    if (!Address.fromString(currentAddress.getAddresss()).equals(this.bmcDb.get())) { // actualy don't need to check it
      Context.revert(ErrorCode.NOT_ACCEPTED_BMC_ADDR_ERROR, "not acceptable bmc");
    }
  }

  private BlockVerifyResult verifyBlock(RelayMessage relayMessage) {
    byte[] stateRoot = null;
    long lastHeight = 0;
    SerializableMTA currentMTA = this.mtaDb.get();
    Validators currenValidators = this.validatorsDb.get();

    List<BlockUpdate> blockUpdates = relayMessage.getBlockUpdate();
    for(int i = 0; i < blockUpdates.size(); i++) {
      long nextHeight = currentMTA.height() + 1;
      BlockHeader currentBlockHeader = blockUpdates.get(i).getBlockHeader();
      if (nextHeight == currentBlockHeader.getNumber()) {
        if (i == blockUpdates.size() - 1) { // only verify signatures of last updating block
            blockUpdates.get(i).verify(currenValidators.get(), this.setIdDb.get());
        } else {
          if (!Arrays.equals(currentBlockHeader.getParentHash(), currentMTA.lastBlockHash())) {
            Context.revert(ErrorCode.BMV_ERROR, "parent block hash does not match, parent: " + HexConverter.bytesToHex(currentBlockHeader.getParentHash()) + " current: " + HexConverter.bytesToHex(currentMTA.lastBlockHash()));
          }
        }
        
        currentMTA.add(currentBlockHeader.getHash());
        lastHeight = nextHeight;
        stateRoot = currentBlockHeader.getStateRoot();
      } else if (nextHeight < currentBlockHeader.getNumber()) {
        Context.revert(ErrorCode.INVALID_BLOCK_UPDATE_HEIGHT_HIGHER, "invalid blockUpdate height: " + currentBlockHeader.getNumber() + "; expected: " + nextHeight);
      } else {
        Context.revert(ErrorCode.INVALID_BLOCK_UPDATE_HEIGHT_LOWER, "invalid blockUpdate height: " + currentBlockHeader.getNumber() + "; expected: " + nextHeight);
      }
    }

    BlockProof blockProof = relayMessage.getBlockProof();
    if (blockProof != null) {
      blockProof.verify(currentMTA);
      lastHeight = blockProof.getBlockHeader().getNumber();
      stateRoot = blockProof.getBlockHeader().getStateRoot();
    }

    this.mtaDb.set(currentMTA);

    return new BlockVerifyResult(stateRoot, lastHeight);
  }
}
