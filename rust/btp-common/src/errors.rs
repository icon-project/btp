use std::fmt::{self, Error, Formatter};

pub enum BMCError {
    InvalidAddress,
    RequestExist,
    RequestNotExist,
    ServiceExist,
    ServiceNotExist,
    NotExistsPermission
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
        }
    }
}