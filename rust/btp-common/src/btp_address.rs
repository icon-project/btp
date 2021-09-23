pub trait Address {
    fn as_ref(&self) -> &String;

    fn protocol(&self) -> Result<(&str, &str), String> {
        match self.as_ref().find("://").unwrap_or(0) {
            0 => return Err("invalid btp address".to_string()),
            size => return Ok((&self.as_ref()[..size], &self.as_ref()[size..])),
        }
    }

    fn network_address(&self) -> Result<String, String> {
        match self.protocol() {
            Ok((_, network)) => {
                let s: Vec<&str> = network.split("/").collect();
                if s.len() > 2 {
                    return Ok(s[2].to_string());
                }
                return Err("empty network address".to_string());
            }
            Err(error) => return Err(error),
        }
    }

    fn network(&self) -> Result<(String, String), String> {
        match self.network_address() {
            Ok(protocol) => {
                let s: Vec<&str> = protocol.split(".").collect();
                if s.len() > 1 {
                    return Ok((s[0].to_string(), s[1].to_string()));
                } else if s.len() > 0 {
                    return Ok(("".to_string(), s[0].to_string()));
                }
                return Err("invalid address".to_string());
            }
            Err(error) => return Err(error),
        }
    }

    fn blockchain(&self) -> Result<String, String> {
        match self.network() {
            Ok((_, blockchain)) => {
                return {
                    if blockchain.to_string().is_empty() {
                        return Ok("empty".to_string());
                    }
                    Ok(blockchain.to_string())
                }
            }
            Err(error) => return Err(error),
        }
    }

    fn network_id(&self) -> Result<String, String> {
        match self.network() {
            Ok((network_id, _)) => return Ok(network_id.to_string()),
            Err(error) => return Err(error),
        }
    }

    fn contract_address(&self) -> Result<String, String> {
        match self.protocol() {
            Ok((_, network)) => {
                let s: Vec<&str> = network.split("/").collect();
                if s.len() > 3 && !s[3].is_empty() {
                    return Ok(s[3].to_string());
                }
                return Err(format!("empty contract address"));
            }
            Err(error) => return Err(error),
        }
    }

    fn is_valid(&self) -> Result<bool, String> {
        match self.protocol() {
            Ok((protocol, _)) => match protocol {
                "btp" => (),
                unsupported => return Err(format!("not supported protocol {}", unsupported)),
            },
            Err(error) => return Err(error),
        }
        return match self.contract_address() {
            Err(error) => return Err(error),
            _ => Ok(true),
        };
    }
}

struct BTPAddress(String);

impl BTPAddress {
    pub fn new(string: String) -> Self {
        Self(string)
    }
}
impl Address for BTPAddress {
    fn as_ref(&self) -> &String {
        &self.0
    }
}
pub fn validate_btp_address(address: &str) -> Result<bool, String> {
    BTPAddress::new(address.to_string()).is_valid()
}
