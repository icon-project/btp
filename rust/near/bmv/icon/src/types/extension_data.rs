use libraries::rlp::{self, Decodable, Encodable};

#[derive(Default, PartialEq, Eq, Debug, Clone)]
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

impl Encodable for ExtensionData {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        let mut params = rlp::RlpStream::new_list(1);
        params.begin_list(self.data.len());
        self.data.iter().for_each(|value| {
            params.append(value);
        });
        stream.append_internal(&params.out());
    }
}
