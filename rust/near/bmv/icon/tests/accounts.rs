use near_sdk::AccountId;

pub fn alice() -> AccountId {
    "7448a64f98a7f2ca3e4e292dd18984ef4466e0e7739fbc7add59f7a580630c6d".parse::<AccountId>().unwrap()
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

pub fn bmc() -> AccountId {
    "bmc.near".parse::<AccountId>().unwrap()
}

pub fn base_service() -> AccountId {
    "base_service.near".parse::<AccountId>().unwrap()
}