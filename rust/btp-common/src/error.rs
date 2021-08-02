pub mod errors {
    use std::fmt::{self, Error, Formatter};

    pub enum BTPError {
        InvalidBTPAddress { description: String },
    }
    impl fmt::Display for BTPError {
        fn fmt(&self, f: &mut Formatter<'_>) -> Result<(), Error> {
            match self {
                BTPError::InvalidBTPAddress { description } => {
                    write!(f, "{}: {}", "InvalidAddress", description)
                }
            }
        }
    }

    pub enum BMCError {
        Generic,
        InvalidAddress,
        RequestExist,
        RequestNotExist,
        ServiceExist,
        ServiceNotExist,
        NotExistsPermission,
        LastOwner,
        OwnerExist,
        NotExistsOwner,
        ExistLink,
        NotExistsLink,
        InvalidParam,
    }

    impl fmt::Display for BMCError {
        fn fmt(&self, f: &mut Formatter<'_>) -> Result<(), Error> {
            let label = "BMCRevert";
            match self {
                BMCError::Generic => write!(f, "{}", label),
                BMCError::InvalidAddress => write!(f, "{}{}", label, "InvalidAddress"),
                BMCError::RequestExist => write!(f, "{}{}", label, "RequestPending"),
                BMCError::RequestNotExist => write!(f, "{}{}", label, "NotExistRequest"),
                BMCError::ServiceExist => write!(f, "{}{}", label, "AlreadyExistsBSH"),
                BMCError::ServiceNotExist => write!(f, "{}{}", label, "NotExistBSH"),
                BMCError::NotExistsPermission => write!(f, "{}{}", label, "NotExistsPermission"),
                BMCError::LastOwner => write!(f, "{}{}", label, "LastOwner"),
                BMCError::OwnerExist => write!(f, "{}{}", label, "AlreadyExistsOwner"),
                BMCError::NotExistsOwner => write!(f, "{}{}", label, "NotExistsOwner"),
                BMCError::ExistLink => write!(f, "{}{}", label, "AlreadyExistsLink"),
                BMCError::NotExistsLink => write!(f, "{}{}", label, "NotExistsLink"),
                BMCError::InvalidParam => write!(f, "{}{}", label, "InvalidParam"),
            }
        }
    }
}
