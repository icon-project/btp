pub struct BTPAddress {
    pub btp_address: String,
}

impl BTPAddress {
    pub fn network(&self) -> (String, String) {
        let na = self.network_address();
        if na != "" {
            let s: Vec<&str> = na.split(".").collect();

            if s.len() > 1 {
                return (s[0].to_string(), s[1].to_string());
            } else {
                return ("".to_string(), s[0].to_string());
            }
        }

        return ("".to_string(), "".to_string());
    }

    pub fn protocol(&self) -> String {
        let p = self.btp_address.to_string();

        let index = p.find("://").unwrap_or(0);

        if index > 0 {
            return String::from(&p[index..]);
        }

        return "".to_string();
    }
    pub fn block_chain(&self) -> String {
        let (_, y) = self.network();

        return y.to_string();
    }
    pub fn network_address(&self) -> String {
        let p = self.protocol();

        if p != "" {
            let s: Vec<&str> = p.split("/").collect();

            if s.len() > 2 {
                return s[2].to_string();
            }
        }

        return "".to_string();
    }

    pub fn network_id(&self) -> String {
        let (x, _) = self.network();

        return x.to_string();
    }

    pub fn contract_address(&self) -> String {
        let p = self.protocol();

        if p != "" {
            let s: Vec<&str> = p.split("/").collect();

            if s.len() > 3 {
                return s[3].to_string();
            }
        }

        return "".to_string();
    }
    pub fn string(&self) -> String {
        return self.btp_address.to_string();
    }
    pub fn validate_btp_address(&self) -> String {
        let p = self.protocol();

        if p.as_str() != "btp" {
            return format!("not supported protocol {}", p.as_str());
        }
        let v = self.block_chain();
        if v.as_str() != "icon" || v.as_str() != "iconee" {
            return format!("not supported blockchain {}", p.as_str());
        }

        if self.contract_address().len() < 1 {
            return format!("empty contract address");
        }

        return "".to_string();
    }
}
