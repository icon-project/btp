use std::fmt::{self, Error, Formatter};

pub enum BMCError {
    InvalidAddress,
    RequestExist,
    RequestNotExist,
    ServiceExist,
    ServiceNotExist,
    NotExistsPermission,
    LastOwner,
    OwnerExist,
    NotExistsOwner,
}

impl fmt::Display for BMCError {
    fn fmt(&self, f: &mut Formatter<'_>) -> Result<(), Error> {
        let label = "BMCRevert";
        match self {
            BMCError::InvalidAddress => write!(f, "{}{}", label, "InvalidAddress"),
            BMCError::RequestExist => write!(f, "{}{}", label, "RequestPending"),
            BMCError::RequestNotExist => write!(f, "{}{}", label, "NotExistRequest"),
            BMCError::ServiceExist => write!(f, "{}{}", label, "AlreadyExistsBSH"),
            BMCError::ServiceNotExist => write!(f, "{}{}", label, "NotExistBSH"),
            BMCError::NotExistsPermission => write!(f, "{}{}", label, "NotExistsPermission"),
            BMCError::LastOwner => write!(f, "{}{}", label, "LastOwner"),
            BMCError::OwnerExist => write!(f, "{}{}", label, "AlreadyExistsOwner"),
            BMCError::NotExistsOwner => write!(f, "{}{}", label, "NotExistsOwner"),
        }
    }
}