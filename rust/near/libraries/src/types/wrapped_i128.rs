use std::ops::Neg;

use super::Wrapper;
use rlp::{self, Decodable, Encodable};

pub type WrappedI128 = Wrapper<i128>;

impl WrappedI128 {
    pub fn negate(&self) -> WrappedI128 {
        WrappedI128::new(self.get().neg())
    }
}
impl Decodable for WrappedI128 {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let value = rlp.as_val::<u128>()?;
        Ok(Self::new(match rlp.as_val::<Vec<u8>>()?.len() as u32 {
            len if len == u8::BITS / 8 => value as i8 as i128,
            len if len == u16::BITS / 8 => value as i16 as i128,
            len if len == u32::BITS / 8 => value as i32 as i128,
            len if len == u64::BITS / 8 => value as i64 as i128,
            _ => value as i128,
        }))
    }
}

impl Encodable for WrappedI128 {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        let value = self.get().to_owned();
        if value >= i8::MIN.into() && value <= i8::MAX.into() {
            stream.append_internal(&(value as u8));
        } else if value >= i16::MIN.into() && value <= i16::MAX.into() {
            stream.append_internal(&(value as u16));
        } else if value >= i32::MIN.into() && value <= i32::MAX.into() {
            stream.append_internal(&(value as u32));
        } else if value >= i64::MIN.into() && value <= i64::MAX.into() {
            stream.append_internal(&(value as u64));
        } else {
            stream.append_internal(&(value as u128));
        };
    }
}
