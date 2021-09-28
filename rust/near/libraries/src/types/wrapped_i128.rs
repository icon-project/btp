use super::Wrapper;
use bytes::{Buf, Bytes, BytesMut};
use rlp::{self, Decodable, Encodable};
use std::convert::TryFrom;
use std::num::ParseIntError;
use byteorder::{BigEndian, ByteOrder};

pub type WrappedI128 = Wrapper<i128>;

impl TryFrom<String> for WrappedI128 {
    type Error = ParseIntError;

    fn try_from(value: String) -> Result<Self, Self::Error> {
        Ok(Self::new(i128::from_str_radix(&value, 16)?))
    }
}

impl From<BytesMut> for WrappedI128 {
    fn from(mut value: BytesMut) -> Self {
        Self::new(value.get_i128_le())
    }
}

impl Decodable for WrappedI128 {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self::from(rlp.as_val::<BytesMut>()?))
    }
}
