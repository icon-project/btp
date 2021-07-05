//! Utility functions for handling BTP addresses

pub fn split_btp_address(addr: &str) -> (String, String) {
    let tmp = split(addr, "/");
    (tmp[2].clone(), tmp[3].clone())
}

pub fn split(base: &str, value: &str) -> Vec<String> {
    let base_bytes = base.as_bytes();
    let mut offset: usize = 0;
    let mut split_count: usize = 1;
    while offset < base_bytes.len() - 1 {
        let limit = index_of(base, value, offset);
        if limit == -1 {
            break;
        } else {
            split_count += 1;
            offset += limit as usize;
        }
    }

    let mut split_arr: Vec<String> = Vec::with_capacity(split_count);

    offset = 0;
    split_count = 0;
    while offset < base_bytes.len() - 1 {
        let mut limit = index_of(base, value, offset);
        if limit == -1 {
            limit = base_bytes.len() as i32;
        }
        let mut tmp_bytes = (limit as usize - offset).to_string().as_bytes().to_vec();
        let mut j: usize = 0;
        for i in offset..limit as usize {
            tmp_bytes[j] = base_bytes[i];
            j += 1;
        }
        offset = limit as usize + 1;
        split_arr[split_count] = std::str::from_utf8(tmp_bytes.as_slice())
            .unwrap()
            .to_string();
        split_count += 1;
    }

    split_arr
}

pub fn index_of(base: &str, value: &str, offset: usize) -> i32 {
    let base_bytes = base.as_bytes();
    let value_bytes = value.as_bytes();

    assert_eq!(value_bytes.len(), 1);

    for i in offset..base_bytes.len() {
        if base_bytes[i] == value_bytes[0] {
            return i as i32;
        }
    }
    return -1;
}
