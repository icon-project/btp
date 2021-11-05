use libraries::rlp::{self, Decodable};
use super::ExtensionData;

#[derive(Default, PartialEq, Eq, Debug)]
pub struct BlockResult {
    state_hash: Vec<u8>,
    patch_receipt_hash: Vec<u8>,
    receipt_hash: Vec<u8>,
    extension_data: ExtensionData
}

impl Decodable for BlockResult {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let data = rlp.as_val::<Vec<u8>>()?;
        let rlp = rlp::Rlp::new(&data);
        Ok(Self {
            state_hash: rlp.val_at(0).unwrap_or_default(),
            patch_receipt_hash: rlp.val_at(1).unwrap_or_default(),
            receipt_hash: rlp.val_at(2).unwrap_or_default(),
            extension_data: rlp.val_at(3).unwrap_or_default(),
        })
    }
}