//! Link Management Module
//! 
use super::*;

#[near_bindgen]
impl BTPMessageCenter {
    pub fn get_status(&self) {}

    #[owner]
    pub fn add_link(&mut self, link: String) {
        let link = BTPAddress::new(link);
        assert!(
            link.is_valid().is_ok(),
            "{}{}",
            BMCError::Generic,
            BTPError::InvalidBTPAddress {
                description: link.is_valid().unwrap_err()
            }
        );
        assert!(
            !self.links.contains(&link.to_string()),
            "{}",
            BMCError::ExistLink
        );

        self.links.add(&link.to_string());

        // TODO: propagateInternal
        for link in self.links.to_vec().iter() {
            emit!(BMCMessage::Link {
                link: link.to_string()
            });
        }
        // TODO: sendInternal
    }

    #[owner]
    pub fn remove_link(&mut self, link: String) {
        let link = BTPAddress::new(link);
        assert!(
            link.is_valid().is_ok(),
            "{}{}",
            BMCError::Generic,
            BTPError::InvalidBTPAddress {
                description: link.is_valid().unwrap_err()
            }
        );
        assert!(
            self.links.contains(&link.to_string()),
            "{}",
            BMCError::NotExistsLink
        );
        self.links.remove(&link.to_string());

        // TODO: propagateInternal
        for link in self.links.to_vec().iter() {
            emit!(BMCMessage::Unlink {
                link: link.to_string()
            });
        }
    }

    #[owner]
    pub fn set_link(&mut self, link: String, block_interval: u64, max_aggregation: u64, delay_limit: u64) {
        let link = BTPAddress::new(link);
        assert!(
            link.is_valid().is_ok(),
            "{}{}",
            BMCError::Generic,
            BTPError::InvalidBTPAddress {
                description: link.is_valid().unwrap_err()
            }
        );
        assert!(
            self.links.contains(&link.to_string()),
            "{}",
            BMCError::NotExistsLink
        );
        assert!(block_interval >= 1 && delay_limit >= 1, BMCError::InvalidParam);
        self.links.set(&link.to_string(), Some(block_interval), Some(max_aggregation), Some(delay_limit), None)
    }
}
