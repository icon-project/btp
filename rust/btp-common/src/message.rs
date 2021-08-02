pub mod messages {
    pub struct BTPMessage {
        src: String,
        dst: String,
        svc: String,
        sn: isize,
        msg: Vec<u8>,
    }
    
    #[derive(Debug)]
    pub enum BMCMessage {
        Init {
            links: Vec<String>,
        },
        Link {
            link: String,
        },
        Unlink {
            link: String,
        },
        Service {
            service: String,
            payload: Vec<u8>,
        },
        GatherFee {
            fee_aggregator: String,
            services: Vec<String>,
        },
        Exception {

        }
    }
}
