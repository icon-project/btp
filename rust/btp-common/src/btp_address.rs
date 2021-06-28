pub struct BTPAddress(pub String);

impl BTPAddress {
    pub fn to_string(&self) -> String {
        self.0.to_string()
    }

    pub fn blockchain(&self) -> Result<String, String> {
        match self.network() {
            Ok((_, blockchain)) => return Ok(blockchain.to_string()),
            Err(error) => return Err(error),
        }
    }

    pub fn network_id(&self) -> Result<String, String> {
        match self.network() {
            Ok((network_id, _)) => return Ok(network_id.to_string()),
            Err(error) => return Err(error),
        }
    }

    pub fn protocol(&self) -> Result<(&str, &str), String> {
        match self.0.find("://").unwrap_or(0) {
            0 => return Err(format!("invalid btp address")),
            size => return Ok((&self.0[..size], &self.0[size..])),
        }
    }

    pub fn network_address(&self) -> Result<String, String> {
        match self.protocol() {
            Ok((_, network)) => {
                let s: Vec<&str> = network.split("/").collect();
                if s.len() > 2 {
                    return Ok(s[2].to_string());
                }
                return Err(format!("empty network address"));
            }
            Err(error) => return Err(error),
        }
    }

    pub fn network(&self) -> Result<(String, String), String> {
        match self.network_address() {
            Ok(protocol) => {
                let s: Vec<&str> = protocol.split(".").collect();
                if s.len() > 1 {
                    return Ok((s[0].to_string(), s[1].to_string()));
                } else if s.len() > 0 {
                    return Ok(("".to_string(), s[0].to_string()));
                }
                return Err(format!("invalid address"));
            }
            Err(error) => return Err(error),
        }
    }

    pub fn contract_address(&self) -> Result<String, String> {
        match self.protocol() {
            Ok((_, network)) => {
                let s: Vec<&str> = network.split("/").collect();
                if s.len() > 3 && !s[3].is_empty(){
                    return Ok(s[3].to_string());
                }
                return Err(format!("empty contract address"));
            }
            Err(error) => return Err(error),
        }
    }

    pub fn is_valid(&self) -> Result<bool, String> {
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
