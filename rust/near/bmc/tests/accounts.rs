use near_sdk::AccountId;

pub fn alice() -> AccountId {
    "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".parse::<AccountId>().unwrap()
}

pub fn bob() -> AccountId {
    "bob.near".parse::<AccountId>().unwrap()
}

pub fn carol() -> AccountId {
    "carol.near".parse::<AccountId>().unwrap()
}

pub fn charlie() -> AccountId {
    "charlie.near".parse::<AccountId>().unwrap()
}

pub fn chuck() -> AccountId {
    "chuck.near".parse::<AccountId>().unwrap()
}

pub fn verifier() -> AccountId {
    "verifier.near".parse::<AccountId>().unwrap()
}