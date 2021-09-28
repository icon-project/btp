#[derive(Clone, PartialEq, Eq, Debug)]
pub struct Wrapper<T>(T);

impl<T> Wrapper<T> {
    pub fn new(value: T) -> Self {
        Self(value)
    }
}
