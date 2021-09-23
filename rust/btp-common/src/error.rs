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
        PermissionNotExist,
        LastOwner,
        OwnerExist,
        OwnerNotExist,
        LinkExist,
        LinkNotExist,
        RouteExist,
        RouteNotExist,
        InvalidParam,
        VerifierExist,
        VerifierNotExist,
        RelayExist { link: String },
        RelayNotExist { link: String },
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
                BMCError::PermissionNotExist => write!(f, "{}{}", label, "NotExistsPermission"),
                BMCError::LastOwner => write!(f, "{}{}", label, "LastOwner"),
                BMCError::OwnerExist => write!(f, "{}{}", label, "AlreadyExistsOwner"),
                BMCError::OwnerNotExist => write!(f, "{}{}", label, "NotExistsOwner"),
                BMCError::LinkExist => write!(f, "{}{}", label, "AlreadyExistsLink"),
                BMCError::LinkNotExist => write!(f, "{}{}", label, "NotExistsLink"),
                BMCError::RouteExist => write!(f, "{}{}", label, "AlreadyExistsRoute"),
                BMCError::RouteNotExist => write!(f, "{}{}", label, "NotExistsRoute"),
                BMCError::InvalidParam => write!(f, "{}{}", label, "InvalidParam"),
                BMCError::VerifierExist => write!(f, "{}{}", label, "AlreadyExistsBMV"),
                BMCError::VerifierNotExist => write!(f, "{}{}", label, "NotExistBMV"),
                BMCError::RelayExist { link } => {
                    write!(f, "{}{} for {}", label, "RelayExist", link)
                },
                BMCError::RelayNotExist { link } => {
                    write!(f, "{}{} for {}", label, "NotExistRelay", link)
                }
            }
        }
    }
}
