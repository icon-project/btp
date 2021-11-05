pub mod errors {
    use std::fmt::{self, Error, Formatter};

    pub trait Exception {
        fn code(&self) -> u32;
        fn message(&self) -> String;
    }

    pub enum BtpException<T: Exception> {
        Base,
        Bmc(T),
        Bmv(T),
        Bsh(T),
        Reserved,
    }

    impl<T> Exception for BtpException<T>
    where
        T: Exception,
    {
        fn code(&self) -> u32 {
            match self {
                BtpException::Base => 0,
                BtpException::Bmc(error) => error.code() + 10,
                _ => todo!(),
            }
        }

        fn message(&self) -> String {
            match self {
                BtpException::Base => todo!(),
                BtpException::Bmc(error) => error.message(),
                _ => todo!(),
            }
        }
    }

    #[derive(Debug, Clone)]
    pub enum BmvError {
        Unknown,
        NotBmc,
        DecodeFailed { message: String },
        EncodeFailed { message: String },
    }

    impl Exception for BmvError {
        fn code(&self) -> u32 {
            u32::from(self)
        }
        fn message(&self) -> String {
            self.to_string()
        }
    }

    impl From<&BmvError> for u32 {
        fn from(bsh_error: &BmvError) -> Self {
            match bsh_error {
                BmvError::Unknown => 0,
                _ => 0,
            }
        }
    }

    impl fmt::Display for BmvError {
        fn fmt(&self, f: &mut Formatter<'_>) -> Result<(), Error> {
            let label = "BMVRevert";
            match self {
                BmvError::NotBmc => {
                    write!(f, "{}{}", label, "NotBMC")
                },
                BmvError::DecodeFailed { message } => {
                    write!(f, "{}{} for {}", label, "DecodeError", message)
                }
                BmvError::EncodeFailed { message } => {
                    write!(f, "{}{} for {}", label, "EncodeError", message)
                }
                _ => todo!(),
            }
        }
    }

    #[derive(Debug, Clone)]
    pub enum BshError {
        Unknown,
        LastOwner,
        OwnerExist,
        OwnerNotExist,
        PermissionNotExist,
        NotMinimumDeposit,
        NotMinimumRefundable,
        NotMinimumAmount,
        NotMinimumBalance { account: String },
        TokenExist,
        TokenNotExist { message: String },
        Failure,
        Reverted { message: String },
        NotBmc,
        InvalidService,
        DecodeFailed { message: String },
        EncodeFailed { message: String },
        InvalidSetting,
        InvalidCount { message: String },
        InvalidAddress { message: String },
        SameSenderReceiver,
        AccountNotExist,
        TokenNotRegistered,
    }

    impl Exception for BshError {
        fn code(&self) -> u32 {
            u32::from(self)
        }
        fn message(&self) -> String {
            self.to_string()
        }
    }

    impl From<&BshError> for u32 {
        fn from(bsh_error: &BshError) -> Self {
            match bsh_error {
                BshError::Unknown => 0,
                BshError::PermissionNotExist => 1,
                _ => 0,
            }
        }
    }

    impl fmt::Display for BshError {
        fn fmt(&self, f: &mut Formatter<'_>) -> Result<(), Error> {
            let label = "BSHRevert";
            match self {
                BshError::Reverted { message } => {
                    write!(f, "{}{}: {}", label, "Reverted", message)
                }
                BshError::TokenExist => {
                    write!(f, "{}{}", label, "AlreadyExistsToken")
                }
                BshError::TokenNotExist { message } => {
                    write!(f, "{}{}: {}", label, "NotExistsToken", message)
                }
                BshError::LastOwner => {
                    write!(f, "{}{}", label, "LastOwner")
                }
                BshError::OwnerExist => {
                    write!(f, "{}{}", label, "AlreadyExistsOwner")
                }
                BshError::OwnerNotExist => {
                    write!(f, "{}{}", label, "NotExistsOwner")
                }
                BshError::PermissionNotExist => {
                    write!(f, "{}{}", label, "NotExistsPermission")
                }
                BshError::NotMinimumDeposit => {
                    write!(f, "{}{}", label, "NotMinimumDeposit")
                }
                BshError::NotMinimumRefundable => {
                    write!(f, "{}{}", label, "NotMinimumRefundable")
                }
                BshError::NotBmc => {
                    write!(f, "{}{}", label, "NotBMC")
                }
                BshError::InvalidService => {
                    write!(f, "{}{}", label, "InvalidSvc")
                }
                BshError::DecodeFailed { message } => {
                    write!(f, "{}{} for {}", label, "DecodeError", message)
                }
                BshError::EncodeFailed { message } => {
                    write!(f, "{}{} for {}", label, "EncodeError", message)
                }
                BshError::InvalidSetting => {
                    write!(f, "{}{}", label, "InvalidSetting")
                }
                BshError::InvalidAddress { message } => {
                    write!(f, "{}{}: {}", label, "InvalidAddress", message)
                }
                BshError::InvalidCount { message } => {
                    write!(f, "{}{} for {}", label, "InvalidCount", message)
                }
                BshError::SameSenderReceiver => {
                    write!(f, "{}{}", label, "SameSenderReceiver")
                }
                BshError::AccountNotExist => {
                    write!(f, "{}{}", label, "AccountNotExist")
                }
                BshError::NotMinimumBalance { account } => {
                    write!(f, "{}{} for {}", label, "NotMinimumBalance", account)
                }
                BshError::NotMinimumAmount => {
                    write!(f, "{}{}", label, "NotMinimumAmount")
                }
                BshError::TokenNotRegistered => {
                    write!(f, "{}{}", label, "TokenNotRegistered")
                }
                _ => todo!(),
            }
        }
    }

    #[derive(Debug, Clone)]
    pub enum BmcError {
        DecodeFailed { message: String },
        EncodeFailed { message: String },
        ErrorDrop,
        FeeAggregatorNotAllowed { source: String },
        InternalServiceCallNotAllowed { source: String },
        InvalidAddress { description: String },
        InvalidParam,
        InvalidSerialNo,
        LastOwner,
        LinkExist,
        LinkNotExist,
        OwnerExist,
        OwnerNotExist,
        PermissionNotExist,
        RelayExist { link: String },
        RelayNotExist { link: String },
        RequestExist,
        RequestNotExist,
        RouteExist,
        RouteNotExist,
        ServiceExist,
        ServiceNotExist,
        Unknown,
        Unreachable { destination: String },
        VerifierExist,
        VerifierNotExist,
    }

    impl Exception for BmcError {
        fn code(&self) -> u32 {
            u32::from(self)
        }
        fn message(&self) -> String {
            self.to_string()
        }
    }

    impl From<&BmcError> for u32 {
        fn from(bmc_error: &BmcError) -> Self {
            match bmc_error {
                BmcError::Unknown => 0,
                BmcError::PermissionNotExist => 1,
                BmcError::InvalidSerialNo => 2,
                BmcError::VerifierExist => 3,
                BmcError::VerifierNotExist => 4,
                BmcError::ServiceExist => 5,
                BmcError::ServiceNotExist => 6,
                BmcError::LinkExist => 7,
                BmcError::LinkNotExist => 8,
                BmcError::RelayExist { link: _ } => 9,
                BmcError::RelayNotExist { link: _ } => 10,
                BmcError::Unreachable { destination: _ } => 11,
                BmcError::ErrorDrop => 12,
                _ => 0,
            }
        }
    }

    impl fmt::Display for BmcError {
        fn fmt(&self, f: &mut Formatter<'_>) -> Result<(), Error> {
            let label = "BMCRevert";
            match self {
                BmcError::InvalidAddress { description } => {
                    write!(f, "{}{}: {}", label, "InvalidAddress", description)
                }
                BmcError::RequestExist => write!(f, "{}{}", label, "RequestPending"),
                BmcError::RequestNotExist => write!(f, "{}{}", label, "NotExistRequest"),
                BmcError::ServiceExist => write!(f, "{}{}", label, "AlreadyExistsBSH"),
                BmcError::ServiceNotExist => write!(f, "{}{}", label, "NotExistBSH"),
                BmcError::PermissionNotExist => write!(f, "{}{}", label, "NotExistsPermission"),
                BmcError::LastOwner => write!(f, "{}{}", label, "LastOwner"),
                BmcError::OwnerExist => write!(f, "{}{}", label, "AlreadyExistsOwner"),
                BmcError::OwnerNotExist => write!(f, "{}{}", label, "NotExistsOwner"),
                BmcError::LinkExist => write!(f, "{}{}", label, "AlreadyExistsLink"),
                BmcError::LinkNotExist => write!(f, "{}{}", label, "NotExistsLink"),
                BmcError::RouteExist => write!(f, "{}{}", label, "AlreadyExistsRoute"),
                BmcError::RouteNotExist => write!(f, "{}{}", label, "NotExistsRoute"),
                BmcError::InvalidParam => write!(f, "{}{}", label, "InvalidParam"),
                BmcError::VerifierExist => write!(f, "{}{}", label, "AlreadyExistsBMV"),
                BmcError::VerifierNotExist => write!(f, "{}{}", label, "NotExistBMV"),
                BmcError::RelayExist { link } => {
                    write!(f, "{}{} for {}", label, "RelayExist", link)
                }
                BmcError::RelayNotExist { link } => {
                    write!(f, "{}{} for {}", label, "NotExistRelay", link)
                }
                BmcError::DecodeFailed { message } => {
                    write!(f, "{}{} for {}", label, "DecodeError", message)
                }
                BmcError::EncodeFailed { message } => {
                    write!(f, "{}{} for {}", label, "EncodeError", message)
                }
                BmcError::ErrorDrop => {
                    write!(f, "{}{}", label, "ErrorDrop")
                }
                BmcError::InternalServiceCallNotAllowed { source } => {
                    write!(
                        f,
                        "{}{} for {}",
                        label, "NotAllowedInternalServiceCall", source
                    )
                }
                BmcError::FeeAggregatorNotAllowed { source } => {
                    write!(f, "{}{} from {}", label, "NotAllowedFeeAggregator", source)
                }
                BmcError::Unreachable { destination } => {
                    write!(f, "{}{} at {}", label, "Unreachable", destination)
                }
                BmcError::Unknown => {
                    write!(f, "{}{}", label, "Unknown")
                }
                BmcError::InvalidSerialNo => {
                    write!(f, "{}{}", label, "Invalid Serial No")
                }
            }
        }
    }
}
