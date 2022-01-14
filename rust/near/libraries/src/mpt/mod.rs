pub mod error;
pub mod mpt;
pub use mpt::MerklePatriciaTree;
mod util;
mod traits;
pub use traits::Prove;