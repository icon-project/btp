package foundation.icon.test.cases;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import scorex.util.ArrayList;

import score.util.Crypto;
import scorex.util.ArrayList;

import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;

public class NewAuthoritiesProof {
    private final byte[] eventKey = HexConverter.hexStringToByteArray("26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7");
    private final List<byte[]> newValidators;
    private final List<RlpType> proofs = new ArrayList<RlpType>(5);

    public NewAuthoritiesProof(List<byte[]> newValidators) {
        this.newValidators = newValidators;
        ByteSliceOutput proof0 = new ByteSliceOutput(100);
        ByteSliceOutput proof1 = new ByteSliceOutput(100);
        ByteSliceOutput proof2 = new ByteSliceOutput(100);
        ByteSliceOutput proof3 = new ByteSliceOutput(100);
        ByteSliceOutput eventLeafNode = new ByteSliceOutput(100);

        eventLeafNode.addMany(HexConverter.hexStringToByteArray("5ed41e5e16056765bc8461851072c9d7")); // node header
        ScaleWriter.writeCompactUint(eventLeafNode, 31 + 3 + newValidators.size()*(32 + 8) + 1 + (newValidators.size() > 63 ? 2: 1));
        eventLeafNode.addMany(HexConverter.hexStringToByteArray("10020c010002090037080000000000000000000020df0e0b00000000020000")); // other events
        eventLeafNode.addMany(HexConverter.hexStringToByteArray("010e00")); // new authorities event phase, index
        ScaleWriter.writeCompactUint(eventLeafNode, newValidators.size());
        for (byte[] validator: newValidators ) {
            eventLeafNode.addMany(validator);
            ScaleWriter.writeU64(eventLeafNode, BigInteger.valueOf(1));
        }
        eventLeafNode.addMany(HexConverter.hexStringToByteArray("00")); // empty topic
        proofs.add(RlpString.create(eventLeafNode.toArray()));

        proof3.addMany(HexConverter.hexStringToByteArray("80810480"));
        proof3.addMany(Crypto.hash("blake2b-256", eventLeafNode.toArray()));
        proof3.addMany(HexConverter.hexStringToByteArray("545e8d434d6125b40443fe11fd292d13a4100300000080985a2c32d72399aa5d04da503ead1ff1a15007afe9b119dec3aa53528d9948c9"));
        proofs.add(RlpString.create(proof3.toArray()));

        proof2.addMany(HexConverter.hexStringToByteArray("9eaa394eea5630e07c48ae0c9558cef7299f8043580dd17bfb375845229c0dd74fc5f9be81d5f4cf569c3ee845e3acf5271556804afcd87f2329d61d655e551a438a2720a92951e383690f5a623d245cfb5ef4444c5f0684a022a34dd8bfa2baaf44f172b710040180"));
        proof2.addMany(Crypto.hash("blake2b-256", proof3.toArray()));
        proof2.addMany(HexConverter.hexStringToByteArray("80fbf36a2eb1b78c879a6ee9b08a04f9f341b284ca2bec0a74f4f39b1df1386ace80586079736657bdf54edfb3d4aab0c8221717c3e1e1ad5e6beeaa7c64dbbe515e80b5a6f3ec81bb61e573624e88e3d279154a7183905d05c656f8744d5788766f614c5f021aab032aaa6e946ca50ad39ab666030401705f09cce9c888469bb1a0dceaa129672ef8287820706f6c6b61646f74"));
        proofs.add(RlpString.create(proof2.toArray()));

        proof1.addMany(HexConverter.hexStringToByteArray("80499c804db64d4c6dd7655a3ac8bc482884de58452960029ea5b37c03ec11e678ec9a7c801ba7abc7e25fb80c99c4bff68d0688c9af3e1ff8cfb2bd5a1754ae1b86f94d1c80"));
        proof1.addMany(Crypto.hash("blake2b-256", proof2.toArray()));
        proof1.addMany(HexConverter.hexStringToByteArray("804a9786b7fa887adfd76874bc414a0f8854227ea92a4739e1bccefe699449460a808310dbebe8a5a91f783843a9916605b01caa58ea3ad8abce80684686d8605f96803f6f047a1cb4d78714bcb64168513cca61d3120e8c0f5c30affb7393620f84b480623848e735dd707e95d569e06cbca4c4437a925d0887bfd1ce67b035d44aec80"));
        proofs.add(RlpString.create(proof1.toArray()));

        proof0.addMany(HexConverter.hexStringToByteArray("80ffff801ee4115c4d5894ff9b5e6bb83611e2a8a8503482fe35a3f086d500fd1f111961803440cfd059e558ed0fc83a7de22ffb14d56220e3ce446d897355c85a53401afe80"));
        proof0.addMany(Crypto.hash("blake2b-256", proof1.toArray()));
        proof0.addMany(HexConverter.hexStringToByteArray("80ede785a30002092980072d34de5a0cc038742d403a539e486808c64f1aeeac8c806af5c4f477504b627b1af9c106960aefe0e0a0632386b542897157486e0145a480c7b7787db289ac25daf82f320e421dc298a601a72cea3aa9784e5700251f169d800eb754c27d6302344f80fc4f785eae09c7c6acf58ee0ebddbd2f1755eb37a7de805839bffac46f817f1bebedade41179b15a7d92a7542b47cb484f1c866cde44f1807d33e56d4ed9f11cb8b9f268896373ac89efc53825d1e4b97e6ff61dfa19463e80660bfa0a74da0e496ac072e6b8611312e9febca06b53ad0a59389f22efd704be80b54561a0ca6f56c484ed434f3cdd323985e74088f456e6c32ed47bd65dd7269f80d651383d73b736e843be7a54db613711ee9ee632c7af16ce2ad548ec004d22bb8062e7f9dbfb626f0f424f63b66346221223d8aaf87a6e126f49617c1d0d967d9e80bf27ea2cbcca83c65622797c4e923d04fad6e7bdd603cc19648f362c7071316780913f5f3a6fa129f8706219077741eda1cdd4b834c5c7e529270858a1324a4cf080879382ecb9b6b8a1d0b0822b9c817182231b2df2039811bb54f2e87d6641f756"));
        proofs.add(RlpString.create(proof0.toArray()));
    }

    public List<RlpType> getProof() {
        return this.proofs;
    }

    public byte[] getRoot() {
        return Crypto.hash("blake2b-256", ((RlpString) proofs.get(4)).getBytes());
    }

    public byte[] getEventKey() {
        return this.eventKey;
    }
}