use near_sdk::serde_json::Value;
use std::collections::HashSet;
use std::hash::{Hash, Hasher};
use std::iter::FromIterator;

#[derive(Debug)]
pub struct HashedCollection<T>(HashSet<T>);

impl<T: PartialEq + Hash + Eq> HashedCollection<T> {
    pub fn new() -> HashedCollection<T> {
        Self(HashSet::new())
    }
    pub fn add(&mut self, element: T) {
        self.0.insert(element);
    }
}

impl<T: PartialEq + Hash + Eq> PartialEq for HashedCollection<T> {
    fn eq(&self, other: &Self) -> bool {
        self.0 == other.0
    }
}

#[derive(Debug)]
pub struct HashedValue(Value);

impl Hash for HashedValue {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.0.to_string().hash(state)
    }
}

impl PartialEq for HashedValue {
    fn eq(&self, other: &Self) -> bool {
        self.0.to_string() == other.0.to_string()
    }
}

impl Eq for HashedValue {}

impl From<Value> for HashedValue {
    fn from(value: Value) -> Self {
        Self(value)
    }
}

impl FromIterator<Value> for HashedCollection<HashedValue> {
    fn from_iter<I: IntoIterator<Item = Value>>(iter: I) -> Self {
        let mut c = HashedCollection::new();
        for i in iter {
            c.add(HashedValue(i));
        }
        c
    }
}
