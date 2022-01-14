use crate::types::Math;

pub fn bytes_to_nibbles(bytes: &[u8], nibbles: Option<Vec<u8>>) -> Vec<u8> {
    let mut data: Vec<u8> = vec![];
    let mut expanded: Vec<u8> = vec![0u8; bytes.len() * 2];
    for i in 0..bytes.len() {
        expanded[i * 2] = (bytes[i] >> 4) & 0x0f;
        expanded[i * 2 + 1] = bytes[i] & 0x0f;
    }

    
    if nibbles.is_some() {
        data.append(&mut nibbles.unwrap());
        
    }
    data.append(&mut expanded);
    data
}
