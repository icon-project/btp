use libraries::types::Hasher;
use tiny_keccak::{Hasher as KeccakHasher, Sha3};
use std::marker::PhantomData;

#[derive(Copy, Clone, PartialEq, Eq, Debug)]
pub struct V256;

impl Hasher for V256 {
    fn hash(input: &[u8]) -> [u8; 32] {
        let mut output = [0; 32];
        let mut sha3 = Sha3::v256();
        sha3.update(input);
        sha3.finalize(&mut output);
        output
    }
}

#[derive(Copy, Clone, PartialEq, Eq, Debug)]
pub struct Sha3Fips202<T: Hasher>(PhantomData<T>);

impl<T: Hasher> Hasher for Sha3Fips202<T> {
    fn hash(input: &[u8]) -> [u8; 32] {
        T::hash(input)
    }
}

pub type Sha256 = Sha3Fips202<V256>;
