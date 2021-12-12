pub trait Math<T> {
    fn add(&mut self, rhs: T) -> Result<&mut Self, String>;
    fn sub(&mut self, rhs: T) -> Result<&mut Self, String>;
    fn mul(&mut self, rhs: T) -> Result<&mut Self, String>;
    fn div(&mut self, rhs: T) -> Result<&mut Self, String>;
    fn div_ceil(&mut self, rhs: T) -> &mut Self;
}

impl Math<u128> for u128 {
    fn add(&mut self, rhs: u128) -> Result<&mut Self, String> {
        self.clone_from(
            &&self
                .checked_add(rhs)
                .ok_or_else(|| "overflow occured".to_string())?,
        );
        Ok(self)
    }

    fn sub(&mut self, rhs: u128) -> Result<&mut Self, String> {
        self.clone_from(
            &&self
                .checked_sub(rhs)
                .ok_or_else(|| "underflow occured".to_string())?,
        );
        Ok(self)
    }

    fn mul(&mut self, rhs: u128) -> Result<&mut Self, String> {
        self.clone_from(
            &self
                .checked_mul(rhs)
                .ok_or_else(|| "overflow occured".to_string())?,
        );
        Ok(self)
    }

    fn div(&mut self, rhs: u128) -> Result<&mut Self, String> {
        self.clone_from(
            &&self
                .checked_div(rhs)
                .ok_or_else(|| "underflow occured".to_string())?,
        );
        Ok(self)
    }

    fn div_ceil(&mut self, rhs: u128) -> &mut Self {
        if *self % rhs == 0 {
            self.clone_from(&(*self / rhs));
        } else {
            self.clone_from(&((*self / rhs) + 1));
        }
        self
    }
}

impl Math<u64> for u64 {
    fn add(&mut self, rhs: u64) -> Result<&mut Self, String> {
        self.clone_from(
            &&self
                .checked_add(rhs)
                .ok_or_else(|| "overflow occured".to_string())?,
        );
        Ok(self)
    }

    fn sub(&mut self, rhs: u64) -> Result<&mut Self, String> {
        self.clone_from(
            &&self
                .checked_sub(rhs)
                .ok_or_else(|| "underflow occured".to_string())?,
        );
        Ok(self)
    }

    fn mul(&mut self, rhs: u64) -> Result<&mut Self, String> {
        self.clone_from(
            &self
                .checked_mul(rhs)
                .ok_or_else(|| "overflow occured".to_string())?,
        );
        Ok(self)
    }

    fn div(&mut self, rhs: u64) -> Result<&mut Self, String> {
        self.clone_from(
            &&self
                .checked_div(rhs)
                .ok_or_else(|| "underflow occured".to_string())?,
        );
        Ok(self)
    }

    fn div_ceil(&mut self, rhs: u64) -> &mut Self {
        if *self % rhs == 0 {
            self.clone_from(&(*self / rhs));
        } else {
            self.clone_from(&((*self / rhs) + 1));
        }
        self
    }
}


impl Math<usize> for usize {
    fn add(&mut self, rhs: usize) -> Result<&mut Self, String> {
        self.clone_from(
            &&self
                .checked_add(rhs)
                .ok_or_else(|| "overflow occured".to_string())?,
        );
        Ok(self)
    }

    fn sub(&mut self, rhs: usize) -> Result<&mut Self, String> {
        self.clone_from(
            &&self
                .checked_sub(rhs)
                .ok_or_else(|| "underflow occured".to_string())?,
        );
        Ok(self)
    }

    fn mul(&mut self, rhs: usize) -> Result<&mut Self, String> {
        self.clone_from(
            &self
                .checked_mul(rhs)
                .ok_or_else(|| "overflow occured".to_string())?,
        );
        Ok(self)
    }

    fn div(&mut self, rhs: usize) -> Result<&mut Self, String> {
        self.clone_from(
            &&self
                .checked_div(rhs)
                .ok_or_else(|| "underflow occured".to_string())?,
        );
        Ok(self)
    }

    fn div_ceil(&mut self, rhs: usize) -> &mut Self {
        if *self % rhs == 0 {
            self.clone_from(&(*self / rhs));
        } else {
            self.clone_from(&((*self / rhs) + 1));
        }
        self
    }
}