pub fn bytes_to_nibbles(bytes: &Vec<u8>, offset: u8, nibbles: Option<Vec<u8>>) -> Vec<u8> {
    let mut len = (bytes.len() - (offset as usize)) * 2;
    if nibbles.is_some() {
        len += nibbles.unwrap().len()
    }
    let mut data: Vec<u8> = vec![0u8;len];
    
    for i in 0..bytes.len() {
        data[i * 2] = (bytes[i] >> 4) & 0x0f;
        data[i * 2 + 1] = bytes[i] & 0x0f;
    }
    data
}
