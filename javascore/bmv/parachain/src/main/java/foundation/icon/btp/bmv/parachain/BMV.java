package foundation.icon.btp.bmv.parachain;

import score.Address;
import score.Context;
import score.VarDB;
import score.ObjectReader;
import score.ByteArrayObjectWriter;

import scorex.util.Base64;
import scorex.util.ArrayList;

import score.annotation.External;

import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.BlockVerifyResult;
import foundation.icon.btp.lib.parachain.Constant;
import foundation.icon.btp.lib.ErrorCode;
import foundation.icon.btp.lib.event.evmevent.BTPMessageEvmEvent;
import foundation.icon.btp.lib.blockproof.BlockProof;
import foundation.icon.btp.lib.blockheader.BlockHeader;
import foundation.icon.btp.lib.btpaddress.BTPAddress;
import foundation.icon.btp.lib.event.EVMLogEvent;
import foundation.icon.btp.lib.event.CandidateIncludedEvent;
import foundation.icon.btp.lib.event.EventRecord;
import foundation.icon.btp.lib.event.NewAuthoritiesEvent;
import foundation.icon.btp.lib.exception.RelayMessageRLPException;

import foundation.icon.btp.lib.mta.MTAStatus;
import foundation.icon.btp.lib.mta.SerializableMTA;

import foundation.icon.btp.lib.mpt.*;

import foundation.icon.btp.lib.stateproof.StateProof;
import foundation.icon.btp.lib.utils.HexConverter;
import foundation.icon.btp.lib.validators.Validators;

import foundation.icon.btp.lib.parachain.relaymessage.RelayMessage;
import foundation.icon.btp.lib.parachain.blockupdate.*;
import foundation.icon.btp.lib.parachain.relaychaindata.RelayChainData;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BMV implements IBMV {
  private final VarDB<Address> bmcDb = Context.newVarDB("bmc", Address.class);
  private final VarDB<String> netAddressDb = Context.newVarDB("netAddr", String.class);
  private final VarDB<Long> lastHeightDb = Context.newVarDB("lastHeight", Long.class);
  private final VarDB<Long> paraChainIdDb = Context.newVarDB("paraId", Long.class);
  private final VarDB<SerializableMTA> paraMtaDb = Context.newVarDB("paraMta", SerializableMTA.class);
  private final VarDB<SerializableMTA> relayMtaDb = Context.newVarDB("relayMta", SerializableMTA.class);
  private final VarDB<Validators> validatorsDb = Context.newVarDB("validators", Validators.class);
  private final VarDB<Address> paraEventDecoderDb = Context.newVarDB("paraEventDecoder", Address.class);
  private final VarDB<Address> relayEventDecoderDb = Context.newVarDB("relayEventDecoder", Address.class);
  private final VarDB<BigInteger> relaySetIdDb = Context.newVarDB("relaySetId", BigInteger.class);

  public BMV(
    Address bmc,
    String net,
    String encodedValidators,
    long relayMtaOffset,
    long paraMtaOffset,
    int mtaRootSize,
    int mtaCacheSize,
    boolean mtaIsAllowNewerWitness,
    byte[] relayLastBlockHash,
    byte[] paraLastBlockHash,
    Address relayEventDecoderAddress,
    Address paraEventDecoderAddress,
    long relayCurrentSetId,
    long paraChainId
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
    this.relaySetIdDb.set(BigInteger.valueOf(relayCurrentSetId));

    SerializableMTA paraMta = new SerializableMTA(0, paraMtaOffset, mtaRootSize, mtaCacheSize, mtaIsAllowNewerWitness, paraLastBlockHash, null, null);
    SerializableMTA relayMta = new SerializableMTA(0, relayMtaOffset, mtaRootSize, mtaCacheSize, mtaIsAllowNewerWitness, relayLastBlockHash, null, null);
    this.lastHeightDb.set(paraMtaOffset);
    this.paraMtaDb.set(paraMta);
    this.relayMtaDb.set(relayMta);

    this.paraEventDecoderDb.set(paraEventDecoderAddress);
    this.relayEventDecoderDb.set(relayEventDecoderAddress);

    this.paraChainIdDb.set(paraChainId);
  }

  /**
   * get encode of merkle tree accumulator
   */
  @External(readonly=true)
  public String paraMta() {
    ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
    SerializableMTA.writeObject(w, this.paraMtaDb.get());
    return new String(Base64.getUrlEncoder().encode(w.toByteArray()));
  }

  @External(readonly=true)
  public String relayMta() {
    ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
    SerializableMTA.writeObject(w, this.relayMtaDb.get());
    return new String(Base64.getUrlEncoder().encode(w.toByteArray()));
  }

  /**
   * get current mta height
   */
  @External(readonly=true)
  public long paraMtaHeight() {
    return this.paraMtaDb.get().height();
  }

  @External(readonly=true)
  public long relayMtaHeight() {
    return this.relayMtaDb.get().height();
  }

  /**
   * get list of MTA root
   */
  @External(readonly=true)
  public List<byte[]> paraMtaRoot() {
    return this.paraMtaDb.get().getRootList();
  }

  @External(readonly=true)
  public List<byte[]> relayMtaRoot() {
    return this.relayMtaDb.get().getRootList();
  }

  /**
   * get last block hash
   */
  @External(readonly=true)
  public byte[] paraMtaLastBlockHash() {
    return this.paraMtaDb.get().lastBlockHash();
  }

  @External(readonly=true)
  public byte[] relayMtaLastBlockHash() {
    return this.relayMtaDb.get().lastBlockHash();
  }

  /**
   * get current mta offset
   */
  @External(readonly=true)
  public long paraMtaOffset() {
    return this.paraMtaDb.get().offset();
  }

  @External(readonly=true)
  public long relayMtaOffset() {
    return this.relayMtaDb.get().offset();
  }

  @External(readonly=true)
  public List<byte[]> paraMtaCaches() {
    return this.paraMtaDb.get().getCacheList();
  }

  @External(readonly=true)
  public List<byte[]> relayMtaCaches() {
    return this.relayMtaDb.get().getCacheList();
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
      Context.revert(ErrorCode.BMV_ERROR, "invalid RelayMessage not exists BlockUpdate and BlockProof");
    }

    BlockVerifyResult paraBlockVerifyResult = this.verifyParaChainBlock(relayMessage);
    
    BigInteger nextSeq = seq.add(BigInteger.valueOf(1));

    // list of bytes message return to bmc
    List<byte[]> bmcMsgs = new ArrayList<byte[]>(5);
    if (paraBlockVerifyResult != null) {
      for (StateProof stateProof : relayMessage.getStateProof()) {
        byte[] encodedStorage = stateProof.prove(paraBlockVerifyResult.stateRoot);
        if (Arrays.equals(stateProof.getKey(), Constant.EventStorageKey)) {
          List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) Context.call(this.paraEventDecoderDb.get(), "decodeEvent", encodedStorage);
          for (Map<String, Object> rawEventRecord: eventRecords) {
            EventRecord eventRecord = new EventRecord(rawEventRecord);

            // filter evm log of BMC
            if (eventRecord.getEventIndex()[0] == Constant.EvmEventIndex[0] && eventRecord.getEventIndex()[1] == Constant.EvmEventIndex[1]) {
              EVMLogEvent evmLogEvent = new EVMLogEvent(eventRecord.getEventData());
              if (evmLogEvent.getEvmTopics().size() == 0 || !Arrays.equals(evmLogEvent.getEvmTopics().get(0), Constant.MessageEventTopic)) {
                continue;
              }

              if (!Arrays.equals(evmLogEvent.getAddress(), HexConverter.hexStringToByteArray(prevAddress.getAddress()))) {
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
              nextSeq = nextSeq.add(BigInteger.valueOf(1));
            }
          }
        }
      }
    }

    if (bmcMsgs.size() > 0) {
      this.lastHeightDb.set(paraBlockVerifyResult.lastHeight);
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
   * get address of event decoder
   */
  @External(readonly=true)
  public Address paraEventDecoder() {
    return this.paraEventDecoderDb.get();
  }

  @External(readonly=true)
  public Address relayEventDecoder() {
    return this.relayEventDecoderDb.get();
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
   * current set id
   */
  @External(readonly=true)
  public BigInteger setId() {
    return this.relaySetIdDb.get();
  }

  /**
   * get status of BMV
   */
  @External(readonly=true)
  public BMVStatus getStatus() {
    SerializableMTA mta = this.paraMtaDb.get();
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
    if (!Address.fromString(currentAddress.getAddress()).equals(this.bmcDb.get())) { // actualy don't need to check it
      Context.revert(ErrorCode.NOT_ACCEPTED_BMC_ADDR_ERROR, "not acceptable bmc");
    }
  }

  private BlockVerifyResult verifyRelayChainBlock(RelayChainData relayChainData) {
    byte[] stateRoot = null;
    long lastHeight = 0;
    SerializableMTA currentMTA = this.relayMtaDb.get();
    Validators currenValidators = this.validatorsDb.get();

    List<RelayBlockUpdate> blockUpdates = relayChainData.getBlockUpdate();
    for(int i = 0; i < blockUpdates.size(); i++) {
      long nextHeight = currentMTA.height() + 1;
      BlockHeader currentBlockHeader = blockUpdates.get(i).getBlockHeader();
      if (nextHeight == currentBlockHeader.getNumber()) {
        if (!Arrays.equals(currentBlockHeader.getParentHash(), currentMTA.lastBlockHash())) {
          Context.revert(ErrorCode.BMV_ERROR, "parent relay block hash does not match, parent: " + HexConverter.bytesToHex(currentBlockHeader.getParentHash()) + " current: " + HexConverter.bytesToHex(currentMTA.lastBlockHash()));
        }

        if (i == blockUpdates.size() - 1) { // only verify signatures of last updating block
            blockUpdates.get(i).verify(currenValidators.get(), this.relaySetIdDb.get());
        }
        
        currentMTA.add(currentBlockHeader.getHash());
        lastHeight = nextHeight;
        stateRoot = currentBlockHeader.getStateRoot();
      } else if (nextHeight < currentBlockHeader.getNumber()) {
        Context.revert(ErrorCode.INVALID_BLOCK_UPDATE_HEIGHT_HIGHER, "invalid relay blockUpdate height: " + currentBlockHeader.getNumber() + "; expected: " + nextHeight);
      } else {
        Context.revert(ErrorCode.INVALID_BLOCK_UPDATE_HEIGHT_LOWER, "invalid relay blockUpdate height: " + currentBlockHeader.getNumber() + "; expected: " + nextHeight);
      }
    }

    BlockProof blockProof = relayChainData.getBlockProof();
    if (blockProof != null) {
      blockProof.verify(currentMTA);
      lastHeight = blockProof.getBlockHeader().getNumber();
      stateRoot = blockProof.getBlockHeader().getStateRoot();
    }

    this.relayMtaDb.set(currentMTA);

    return new BlockVerifyResult(stateRoot, lastHeight);
  }

  private BlockVerifyResult verifyParaChainBlock(RelayMessage relayMessage) {
    byte[] stateRoot = null;
    long lastHeight = 0;
    SerializableMTA currentMTA = this.paraMtaDb.get();

    List<ParaBlockUpdate> blockUpdates = relayMessage.getBlockUpdate();
    if (blockUpdates.size() == 1 && blockUpdates.get(0).getBlockHeader() == null) {
      RelayChainData relayChainData = blockUpdates.get(0).getRelayChainData();
      if (relayChainData == null) {
        Context.revert(ErrorCode.BMV_ERROR, "Missing relay chain data");
      }
      BlockVerifyResult relayBlockVerifyResult = this.verifyRelayChainBlock(relayChainData);
      this.verifyRelayChainState(relayChainData.getStateProof(), relayBlockVerifyResult);
    } else {
      for(int i = 0; i < blockUpdates.size(); i++) {
        long nextHeight = currentMTA.height() + 1;
        BlockHeader currentBlockHeader = blockUpdates.get(i).getBlockHeader();
        if (nextHeight == currentBlockHeader.getNumber()) {
          if (!Arrays.equals(currentBlockHeader.getParentHash(), currentMTA.lastBlockHash())) {
            Context.revert(ErrorCode.BMV_ERROR, "parent para block hash does not match, parent: " + HexConverter.bytesToHex(currentBlockHeader.getParentHash()) + " current: " + HexConverter.bytesToHex(currentMTA.lastBlockHash()));
          }

          if (i == blockUpdates.size() - 1) { // last blockUpdate of parachain must has the same hash with ParaHead in CandidateIncludedEvent data
            RelayChainData relayChainData = blockUpdates.get(i).getRelayChainData();
            if (relayChainData == null) {
              Context.revert(ErrorCode.BMV_ERROR, "Missing relay chain data");
            }
            BlockVerifyResult relayBlockVerifyResult = this.verifyRelayChainBlock(relayChainData);
            byte[] lastIncludedParaChainBlockHash = this.verifyRelayChainState(relayChainData.getStateProof(), relayBlockVerifyResult);
            if (lastIncludedParaChainBlockHash == null) {
              Context.revert(ErrorCode.BMV_ERROR, "can not find parachain data in relay chain data");
            }
            if (!Arrays.equals(blockUpdates.get(i).getBlockHeader().getHash(), lastIncludedParaChainBlockHash)) {
              Context.revert(ErrorCode.BMV_ERROR, "block hash does not match with relay chain, para block hash: " + HexConverter.bytesToHex(blockUpdates.get(i).getBlockHeader().getHash()) + " relay inclusion: " + HexConverter.bytesToHex(lastIncludedParaChainBlockHash));
            }
          }
          
          currentMTA.add(currentBlockHeader.getHash());
          lastHeight = nextHeight;
          stateRoot = currentBlockHeader.getStateRoot();
        } else if (nextHeight < currentBlockHeader.getNumber()) {
          Context.revert(ErrorCode.INVALID_BLOCK_UPDATE_HEIGHT_HIGHER, "invalid para blockUpdate height: " + currentBlockHeader.getNumber() + "; expected: " + nextHeight);
        } else {
          Context.revert(ErrorCode.INVALID_BLOCK_UPDATE_HEIGHT_LOWER, "invalid para blockUpdate height: " + currentBlockHeader.getNumber() + "; expected: " + nextHeight);
        }
      }
    }

    BlockProof blockProof = relayMessage.getBlockProof();
    if (blockProof != null) {
      blockProof.verify(currentMTA);
      lastHeight = blockProof.getBlockHeader().getNumber();
      stateRoot = blockProof.getBlockHeader().getStateRoot();
    }

    this.paraMtaDb.set(currentMTA);

    if (stateRoot != null && lastHeight != 0) {
      return new BlockVerifyResult(stateRoot, lastHeight);
    }

    return null;
  }

  private byte[] verifyRelayChainState(List<StateProof> relayChainStateProofs, BlockVerifyResult relayChainBlockVerifyResult) {
    byte[] paraIncludedHead = null;
    for (StateProof stateProof : relayChainStateProofs) {
      byte[] encodedStorage = stateProof.prove(relayChainBlockVerifyResult.stateRoot);
      if (Arrays.equals(stateProof.getKey(), Constant.EventStorageKey)) {
        List<Map<String, Object>> eventRecords = (List<Map<String, Object>>) Context.call(this.relayEventDecoderDb.get(), "decodeEvent", encodedStorage);
        for (Map<String, Object> rawEventRecord: eventRecords) {
          EventRecord eventRecord = new EventRecord(rawEventRecord);
          // update new validator set
          if (eventRecord.getEventIndex()[0] == Constant.NewAuthoritiesEventIndex[0] && eventRecord.getEventIndex()[1] == Constant.NewAuthoritiesEventIndex[1]) {
            NewAuthoritiesEvent newAuthoritiesEvent = new NewAuthoritiesEvent(eventRecord.getEventData());
            List<byte[]> newValidators = newAuthoritiesEvent.getValidators();
            Validators newValidatorsObject = new Validators(newValidators);
            this.validatorsDb.set(newValidatorsObject);
            this.relaySetIdDb.set(this.relaySetIdDb.get().add(BigInteger.valueOf(1)));
            continue;
          }

          // filter Candidate Included Event of Relay chain
          if (eventRecord.getEventIndex()[0] == Constant.CandidateIncludedEventIndex[0] && eventRecord.getEventIndex()[1] == Constant.CandidateIncludedEventIndex[1]) {
            CandidateIncludedEvent candidateIncludedEvent = new CandidateIncludedEvent(eventRecord.getEventData());
            // check inclusion of current paraChain Id and return included block hash
            if (candidateIncludedEvent.getCandidateReceipt().getCandidateDescriptor().getParaId() == this.paraChainIdDb.get()) {
              paraIncludedHead = candidateIncludedEvent.getCandidateReceipt().getCandidateDescriptor().getParaHead();
            }
          }
        }
      }
    }
    return paraIncludedHead;
  }
}
