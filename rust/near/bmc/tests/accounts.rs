use near_sdk::AccountId;

pub fn alice() -> AccountId {
    "alice.near".parse::<AccountId>().unwrap()
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