use libraries::rlp::{self, Decodable};

#[derive(Default, PartialEq, Eq, Debug)]
pub struct ExtensionData {
    data: Vec<Vec<u8>>,
}

impl Decodable for ExtensionData {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let data = rlp.as_val::<Vec<u8>>()?;
        let rlp = rlp::Rlp::new(&data);
        Ok(Self {
            data: rlp.as_list().unwrap_or_default(),
        })
    }
}
