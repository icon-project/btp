pub trait Utils {
    fn ceil_div(num1: u128, num2: u128) -> u128;
    fn get_scale(block_interval_src: u128, block_interval_dst: u128) -> u128;
    fn get_rotate_term(max_agg: u128, scale: u128) -> u128;
}

impl Utils for u128 {
    fn ceil_div(num1: u128, num2: u128) -> u128 {
        if num1 % num2 == 0 {
            return (num1 / num2) + 1;
        }
        (num1 / num2) + 1
    }

    fn get_scale(block_interval_src: u128, block_interval_dst: u128) -> u128 {
        Self::ceil_div(block_interval_src * 10_u128.pow(6), block_interval_dst)
    }

    fn get_rotate_term(max_agg: u128, scale: u128) -> u128 {
        if scale > 0 {
            return Self::ceil_div(max_agg * 10_u128.pow(6), scale);
        }
        0
    }
}
