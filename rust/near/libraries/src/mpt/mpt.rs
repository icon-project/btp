use super::error::MptError;
use crate::{
    rlp::{self, Decodable, Encodable},
    types::{Hash, Hasher},
};
use std::{convert::TryFrom, marker::PhantomData, ops::Range};

const LEAF_EVEN_NIBBLES: u8 = 0x20;
const LEAF_ODD_NIBBLES: u8 = 0x3;
const EXTENSION_ODD_NIBBLES: u8 = 0x1;
const EXTENSION_EVEN_NIBBLES: u8 = 0x00;

const EMPTY_TRIE: u8 = 0;
const LEAF_NODE_OFFSET: u8 = 1;
const EXTENSION_NODE_OFFSET: u8 = 128;
const BRANCH_NODE_NO_VALUE: u8 = 254;
const BRANCH_NODE_WITH_VALUE: u8 = 255;
const LEAF_NODE_OVER: u8 = EXTENSION_NODE_OFFSET - LEAF_NODE_OFFSET;
const EXTENSION_NODE_OVER: u8 = BRANCH_NODE_NO_VALUE - EXTENSION_NODE_OFFSET;
const LEAF_NODE_LAST: u8 = EXTENSION_NODE_OFFSET - 1;
const EXTENSION_NODE_LAST: u8 = BRANCH_NODE_NO_VALUE - 1;

#[derive(Copy, Clone, PartialEq, Eq, Debug)]
struct ByteSliceInput<'a> {
    data: &'a [u8],
    offset: usize,
}

impl<'a> ByteSliceInput<'a> {
    fn new(data: &'a [u8]) -> Self {
        ByteSliceInput { data, offset: 0 }
    }

    fn take(&mut self, count: usize) -> Result<Range<usize>, &str> {
        if self.offset + count > self.data.len() {
            return Err("out of data");
        }

        let range = self.offset..(self.offset + count);
        self.offset += count;
        Ok(range)
    }
}

pub trait Input {
    /// Should return the remaining length of the input data. If no information about the input
    /// length is available, `None` should be returned.
    ///
    /// The length is used to constrain the preallocation while decoding. Returning a garbage
    /// length can open the doors for a denial of service attack to your application.
    /// Otherwise, returning `None` can decrease the performance of your application.
    fn remaining_len(&mut self) -> Result<Option<usize>, &str>;

    /// Read the exact number of bytes required to fill the given buffer.
    ///
    /// Note that this function is similar to `std::io::Read::read_exact` and not
    /// `std::io::Read::read`.
    fn read(&mut self, into: &mut [u8]) -> Result<(), &str>;

    /// Read a single byte from the input.
    fn read_byte(&mut self) -> Result<u8, &str> {
        let mut buf = [0u8];
        self.read(&mut buf[..])?;
        Ok(buf[0])
    }

    /// Descend into nested reference when decoding.
    /// This is called when decoding a new refence-based instance,
    /// such as `Vec` or `Box`. Currently all such types are
    /// allocated on the heap.
    fn descend_ref(&mut self) -> Result<(), &str> {
        Ok(())
    }

    /// Ascend to previous structure level when decoding.
    /// This is called when decoding reference-based type is finished.
    fn ascend_ref(&mut self) {}
}

impl<'a> Input for ByteSliceInput<'a> {
    fn remaining_len(&mut self) -> Result<Option<usize>, &str> {
        let remaining = if self.offset <= self.data.len() {
            Some(self.data.len() - self.offset)
        } else {
            None
        };
        Ok(remaining)
    }

    fn read(&mut self, into: &mut [u8]) -> Result<(), &str> {
        if self.offset + into.len() > self.data.len() {
            return Err("out of data");
        }
        let range = self.offset..(self.offset + into.len());
        self.offset += into.len();
        into.copy_from_slice(&self.data[range]);
        Ok(())
    }

    fn read_byte(&mut self) -> Result<u8, &str> {
        if self.offset + 1 > self.data.len() {
            return Err("out of data".into());
        }

        let byte = self.data[self.offset];
        self.offset += 1;
        Ok(byte)
    }
}

#[derive(Copy, Clone, PartialEq, Eq, Debug)]
enum NodeHeader {
    Null,
    Branch(bool),
    Extension(usize),
    Leaf(usize),
}

impl Decodable for NodeHeader {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let data = rlp.as_val::<Vec<u8>>()?;
        let mut input = ByteSliceInput::new(&data);
        Ok(
            match input
                .read_byte()
                .map_err(|_| rlp::DecoderError::Custom(""))?
            {
                EMPTY_TRIE => NodeHeader::Null,
                BRANCH_NODE_NO_VALUE => NodeHeader::Branch(false),
                BRANCH_NODE_WITH_VALUE => NodeHeader::Branch(true),
                i @ LEAF_NODE_OFFSET..=LEAF_NODE_LAST => {
                    NodeHeader::Leaf((i - LEAF_NODE_OFFSET) as usize)
                }
                i @ EXTENSION_NODE_OFFSET..=EXTENSION_NODE_LAST => {
                    NodeHeader::Extension((i - EXTENSION_NODE_OFFSET) as usize)
                }
            },
        )
    }
}

#[derive(Copy, Clone, PartialEq, Eq, Debug)]
struct Node {
    header: NodeHeader,
    // nibbles: Vec<u8>,
    // children: Vec<Self>,
    // data: Vec<u8>,
}

impl Decodable for Node {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let header: NodeHeader = rlp.val_at(0)?;
        println!("{:?}", header);
        let data = rlp.val_at::<Vec<u8>>(1)?;
        let rlp = rlp::Rlp::new(&data);

        let data = rlp.val_at::<Vec<u8>>(0)?;
        let rlp = rlp::Rlp::new(&data);
        println!("{:?}", rlp.is_list());
        // match header {
        //     NodeHeader::Leaf(nibble_count) => {
        //         let partial = rlp.as_val::<Vec<u8>>()?.take(
        //             (nibble_count + (nibble_ops::NIBBLE_PER_BYTE - 1))
        //                 / nibble_ops::NIBBLE_PER_BYTE,
        //         )?;
        //         let partial_padding = nibble_ops::number_padding(nibble_count);
        //         let count = <Compact<u32>>::decode(&mut input)?.0 as usize;
        //         let value = input.take(count)?;
        //         Ok(NodePlan::Leaf {
        //             partial: NibbleSlicePlan::new(partial, partial_padding),
        //             value: ValuePlan::Inline(value),
        //         })
        //     },
        //     _ => Ok(())
        // }
        Ok(Self { header })
    }
}

#[derive(Copy, Clone, PartialEq, Eq, Debug)]
pub struct MerklePatriciaTree<H> {
    root: Node,
    hash: Hash,
    _hasher: PhantomData<H>,
}

impl<H: Hasher> Decodable for MerklePatriciaTree<H> {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            root: rlp.as_val()?,
            hash: Hash::new::<H>(rlp.as_raw()),
            _hasher: PhantomData::default(),
        })
    }
}

impl<H: Hasher> TryFrom<&Vec<u8>> for MerklePatriciaTree<H> {
    type Error = MptError;

    fn try_from(value: &Vec<u8>) -> Result<Self, Self::Error> {
        let rlp = rlp::Rlp::new(&value);
        Self::decode(&rlp).map_err(|error| MptError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}
