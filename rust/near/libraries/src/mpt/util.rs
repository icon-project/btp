pub fn bytes_to_nibbles(bytes: &Vec<u8>, offset: u8, nibbles: Option<Vec<u8>>) {
    let mut len = (bytes.len() - (offset as usize)) * 2;
    if nibbles.is_some() {
        len += nibbles.unwrap().len()
    }
    let mut data: Vec<u8> = vec![0u8;len];
    
    for i in 0..bytes.len() {
        data[i * 2] = (bytes[i] >> 4) & 0x0f;
        data[i * 2 + 1] = bytes[i] & 0x0f;
    }
    print!("{:?} --", data);
    
    // if (nibbles != null) {
    //     len += nibbles.length;
    // }
    // byte[] ret = new byte[len];
    // int j = 0;
    // if (nibbles != null) {
    //     System.arraycopy(nibbles, 0, ret, 0, nibbles.length);
    //     j = nibbles.length;
    // }
    // for (int i = from; i < bytes.length; i++) {
    //     ret[j++] = (byte)(bytes[i] >> 4 & 0x0F);
    //     ret[j++] = (byte)(bytes[i] & 0x0F);
    // }
    // return ret;
}
