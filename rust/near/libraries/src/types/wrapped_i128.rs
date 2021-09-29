use super::Wrapper;
use byteorder::{LittleEndian, ByteOrder, ReadBytesExt};
use bytes::{Buf, Bytes, BytesMut};
use rlp::{self, Decodable, Encodable};
use std::convert::TryFrom;
use std::io::Cursor;
use std::num::ParseIntError;

pub type WrappedI128 = Wrapper<i128>;

impl From<Vec<u8>> for WrappedI128 {
    fn from(value: Vec<u8>) -> Self {
        let mut reader = Cursor::new(value);
        Self::new(reader.read_i128::<LittleEndian>().unwrap())
    }
}

impl Decodable for WrappedI128 {
    //TODO: Handle signed integers
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let mut value = rlp.as_val::<Vec<u8>>()?;
        value.extend_from_slice(&vec![0_u8; 16 - value.len()]);
        Ok(Self::from(value))
    }
}

impl Encodable for WrappedI128 {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream.append_internal(&(self.get().to_owned() as u128));
    }
}
