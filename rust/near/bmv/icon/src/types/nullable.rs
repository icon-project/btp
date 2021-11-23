use libraries::rlp::{self, Decodable, Encodable};

#[derive(Default, PartialEq, Eq, Debug, Clone)]
pub struct Nullable<T>(Option<T>);

impl<T> Nullable<T> {
    pub fn new(item: Option<T>) -> Self {
        Self(item)
    }

    pub fn is_some(&self) -> bool {
        self.0.is_some()
    }

    pub fn is_none(&self) -> bool {
        self.0.is_none()
    }

    pub fn get(&self) -> Result<&T, &'static str> {
        self.0.as_ref().ok_or("object is null")
    }
}

impl<T: Decodable> Decodable for Nullable<T> {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        if rlp.is_null() {
            Ok(Self(None))
        } else {
            Ok(Self(Some(rlp.as_val()?)))
        }
    }
}

impl<T: Encodable> Encodable for Nullable<T> {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        if self.is_none() {
            stream.append_null();
        } else {
            stream.append(self.0.as_ref().unwrap());
        }
    }
}
