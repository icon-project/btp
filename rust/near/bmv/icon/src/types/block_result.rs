use super::{ExtensionData, Nullable};
use libraries::rlp::{self, Decodable, Encodable};
use libraries::types::Hash;

#[derive(Default, PartialEq, Eq, Debug, Clone)]
pub struct BlockResult {
    state_hash: Hash,
    patch_receipt_hash: Nullable<Hash>,
    receipt_hash: Nullable<Hash>,
    extension_data: ExtensionData,
}

impl Decodable for BlockResult {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let data = rlp.as_val::<Vec<u8>>()?;
        let rlp = rlp::Rlp::new(&data);
        Ok(Self {
            state_hash: rlp.val_at(0)?,
            patch_receipt_hash: rlp.val_at(1)?,
            receipt_hash: rlp.val_at(2)?,
            extension_data: rlp.val_at(3).unwrap_or_default(),
        })
    }
}

impl Encodable for BlockResult {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        let mut params = rlp::RlpStream::new_list(4);
        params
            .append(&self.state_hash)
            .append(&self.patch_receipt_hash)
            .append(&self.receipt_hash)
            .append(&self.extension_data);
        stream.append(&params.out());
    }
}
