use super::{BlockProof, BlockUpdate, ReceiptProof};
use btp_common::errors::BmvError;
use libraries::{
    rlp::{self, Decodable, Encodable}
};
use near_sdk::{
    base64::{self, URL_SAFE_NO_PAD},
    serde::{de, ser, Deserialize, Serialize},
};
use std::convert::TryFrom;

#[derive(PartialEq, Eq, Debug)]
pub struct RelayMessage {
    block_updates: Vec<BlockUpdate>,
    //block_proof: BlockProof,
//receipt_proofs: Vec<ReceiptProof>,
}

impl Decodable for RelayMessage {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            block_updates: rlp.list_at(0)?,
            //<Vec<BlockUpdate>>::try_from(relay_message.get(0).ok_or(rlp::DecoderError::Custom("Failed to get from index 1"))?)?,
            //block_proof: BlockProof::try_from(relay_message.get(1).ok_or(rlp::DecoderError::Custom("Failed to get from index 1"))?)?
        })
    }
}

impl TryFrom<String> for RelayMessage {
    type Error = BmvError;
    fn try_from(value: String) -> Result<Self, Self::Error> {
        let decoded = base64::decode_config(value, URL_SAFE_NO_PAD).map_err(|error| {
            BmvError::DecodeFailed {
                message: format!("base64: {}", error),
            }
        })?;
        let rlp = rlp::Rlp::new(&decoded);
        Self::decode(&rlp).map_err(|error| BmvError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}

impl<'de> Deserialize<'de> for RelayMessage {
    fn deserialize<D>(deserializer: D) -> Result<Self, <D as de::Deserializer<'de>>::Error>
    where
        D: de::Deserializer<'de>,
    {
        <String as Deserialize>::deserialize(deserializer)
            .and_then(|s| Self::try_from(s).map_err(de::Error::custom))
    }
}

#[cfg(test)]
mod tests {
    use crate::types::BlockHeader;

    use super::*;

    #[test]
    fn deserialize_relay_message() {
        let relay_message = RelayMessage::try_from("-RM1-RMwuQEw-QEtuLP4sQKCECSHBdAbvP3v5pUA7x18HkxLEWiR2_PkKHqclSEZPz-guezyKLZQDMyn4K9elNxurWSK0u5LNBGunxpGbWRDv4Sg7u-N-Vu4HAugJ7pKN1cLXnZXGEhJ8hTvmFMbzQ4JltKgwLvAKp9cwrElds628DT6UyDLkqeGWZ-cppWr8I2Pjr74APgAgKbloIyTZCne5ziErf_0yZGiVmldZqgALrVZmjEgwA9PdFat-AD4ALh1-HMA4gGgfnFmxXkSfxK6AYub6mpeLiWsk7PVus79rms9gPichJX4TfhLhwXQG70NqF64QVqi8QRTU6vo46-Y03nA8VECqlxdJsYAgUcoKjbDxiVOLtSk2-OITyah5DsCqJCJPJDYQpZgj363sIudUDrk8T0AgLkBMPkBLbiz-LECghAlhwXQG70NqF6VAO8dfB5MSxFokdvz5Ch6nJUhGT8_oBRhccCaBhOXyHSPYfLF8Sd0mj667ksyHCR0snUR8a3ioBODm5Q85h1xk3wpQGeUqPUvvWrcqtihK2hyfZ8zWld2oMC7wCqfXMKxJXbOtvA0-lMgy5KnhlmfnKaVq_CNj46--AD4AICm5aCMk2Qp3uc4hK3_9MmRolZpXWaoAC61WZoxIMAPT3RWrfgA-AC4dfhzAOIBoA7HoC3z6VL-PUPT4ctvOi-KcO4SYoX6dPMLD_cfaEGE-E34S4cF0Bu9HVILuEGlw4XAhEDBTlT-ee508icztmKVxiY_aR1c8nhaZG2bIHVMI692bTlChCp29U1ttkzh9pvHIGy6cjqwYBVAcUBmAIC5ATD5AS24s_ixAoIQJocF0Bu9HVILlQDvHXweTEsRaJHb8-QoepyVIRk_P6BcAf4jQhDRsySgC-VicwHXzBad66loBK9-Qz6CxJCws6BNUzPqU4pDCv5U_gAQCfMv6wduiYwNo1pxe-YxLijwSqDAu8Aqn1zCsSV2zrbwNPpTIMuSp4ZZn5ymlavwjY-OvvgA-ACApuWgjJNkKd7nOISt__TJkaJWaV1mqAAutVmaMSDAD090Vq34APgAuHX4cwDiAaBJpDKb9qSSUaR2dqIuX3XalFTRgYQag9OI4_lEhlD6HPhN-EuHBdAbvSzbIbhBQo3YAl074lOLrdGfrrN9NGf0t6TmyMg_kWZv4CNbyKU7CxIJHgGcrLlTppYdOHtm5nh8Bi_hj5DJ_PIEFrrh5QGAuQEw-QEtuLP4sQKCECeHBdAbvSzbIZUA7x18HkxLEWiR2_PkKHqclSEZPz-gBJCnemxxveIsXZyS2IEfDThI5V2JwQgYPRZe6NBOZQCg9oGpxd1wbTux5yo8UzlTJLaDyLod42QBQpIEB0zP8OqgwLvAKp9cwrElds628DT6UyDLkqeGWZ-cppWr8I2Pjr74APgAgKbloIyTZCne5ziErf_0yZGiVmldZqgALrVZmjEgwA9PdFat-AD4ALh1-HMA4gGgY2pGLjxRFpRtJuQe83GYrHVr3AGGq25sds-e1z3ynlr4TfhLhwXQG708TIW4QSTq5NeCxM54N8VLYQS-YTcN2i4ouNA3KUfPOBW9UmqfXmZZ2SSekwkgRmhm1h9qfVRJzcuRCQ2gPvB6ZBNe5PwBgLkBMPkBLbiz-LECghAohwXQG708TIWVAO8dfB5MSxFokdvz5Ch6nJUhGT8_oCeUtrKxWS40Zrh2FqxuEitGfsx7gvv80nzEa4t4EWl2oNrsJir5FJUY3eoUdwPXoDmG0jHjEI0e3NofaVCrpzizoMC7wCqfXMKxJXbOtvA0-lMgy5KnhlmfnKaVq_CNj46--AD4AICm5aCMk2Qp3uc4hK3_9MmRolZpXWaoAC61WZoxIMAPT3RWrfgA-AC4dfhzAOIBoEasM3agepheUWJ97pozEeYNCf0gB_0jl9C0Mb_UTMeb-E34S4cF0Bu9S9UEuEHaOegDQP73vQRzLMV9jyDaW-DltsnLrtP7sJtfevPTxy6s_7bSU2zx1r59x1CaWe4MWvBymnJPfpaRxm0aUYdBAYC5ATD5AS24s_ixAoIQKYcF0Bu9S9UElQDvHXweTEsRaJHb8-QoepyVIRk_P6CpHSFOp19g44WMTpahcTy7QrkpLLAer4EnM5ifIWsIaaB4bnqvnoa65uCRXkkTCUzZKFYUvcen3qaZ-8rsjs2roaDAu8Aqn1zCsSV2zrbwNPpTIMuSp4ZZn5ymlavwjY-OvvgA-ACApuWgjJNkKd7nOISt__TJkaJWaV1mqAAutVmaMSDAD090Vq34APgAuHX4cwDiAaB1OOPotS4wGCanwHNSpouMm1uXYUTfJxxF184H9Bg--PhN-EuHBdAbvVtmdLhBcNcqN-zJU6n9s3asW9umkc4dSGYYqtVBJRNe0O3JhcQeHBV-a3ffkCd51tzqFXLiukkVK_-gPcke_R2ojqGaUgCAuQEw-QEtuLP4sQKCECqHBdAbvVtmdJUA7x18HkxLEWiR2_PkKHqclSEZPz-gQsVl43J-reTa48DNByvITgG_9nPb3GFHJDUAZ5D2QdGgLUJm10KNxn0ibVd5GdyPkaZzkUk9z4GpBIJTD1mGZ7KgwLvAKp9cwrElds628DT6UyDLkqeGWZ-cppWr8I2Pjr74APgAgKbloIyTZCne5ziErf_0yZGiVmldZqgALrVZmjEgwA9PdFat-AD4ALh1-HMA4gGgeE0ACr4gcMBoVpgF6mqMMTEZD9bmXlN0xJLag590rG74TfhLhwXQG71quaG4QQKncl56MbDRy1tDLGrELpZdRAEuWiilUxf4AmUkNb0yA7kEk4-gWDR3gPXl5pgZ9KKBqobU17AHf_qKOZ8kuTIBgLkBMPkBLbiz-LECghArhwXQG71quaGVAO8dfB5MSxFokdvz5Ch6nJUhGT8_oFCeB-EMNIiXLCHwJ07eqdKauAAQjTg83di8-uydlIwOoCr2bg_evzdBWZWjQoy7QGac4CIdOifJAUyr-dSGcG4hoMC7wCqfXMKxJXbOtvA0-lMgy5KnhlmfnKaVq_CNj46--AD4AICm5aCMk2Qp3uc4hK3_9MmRolZpXWaoAC61WZoxIMAPT3RWrfgA-AC4dfhzAOIBoBHEDNsjTZWFHkEM0iarX1gVtjz6yrt_xYROy78cclLo-E34S4cF0Bu9ejYouEEHF2Kyt7JrW5KGq-tgBdELnIMkQz3YAN4MRPnV8ZSbVj1YduMUyHr88cf73YytOijhFDO2F0wK_7z16OTnWVtMAYC5ATD5AS24s_ixAoIQLIcF0Bu9ejYolQDvHXweTEsRaJHb8-QoepyVIRk_P6BIt4YG1A00Uvfwggpp9SuCb4CS_N3CEzfbq4jfwGO6ZaDWX33QWXDU0MwOdxWuifmVUIPvZ3yWWqDyqvpXbx3dsKDAu8Aqn1zCsSV2zrbwNPpTIMuSp4ZZn5ymlavwjY-OvvgA-ACApuWgjJNkKd7nOISt__TJkaJWaV1mqAAutVmaMSDAD090Vq34APgAuHX4cwDiAaDMgdhF94q9CqfDQEYmFIGVpoQkAOe61jN87L5ZdbrGZfhN-EuHBdAbvYnppbhBMqR8jzjUifCVnnAcMnZckepmgsl0Y4MUZc7npny-ceYQrfeG3RQKWklgTrhC9dmzIG3ozT1Fp_yJvWckD73r9ACAuQEw-QEtuLP4sQKCEC2HBdAbvYnppZUA7x18HkxLEWiR2_PkKHqclSEZPz-gXoNJPsM-k78GgJWQYUi1gqHFOaxo_wKjeHXfJ_qR0r2ghPQ-gtGOluKVWacml0RKudvSj4Cs8dVx_863NkFxjz6gwLvAKp9cwrElds628DT6UyDLkqeGWZ-cppWr8I2Pjr74APgAgKbloIyTZCne5ziErf_0yZGiVmldZqgALrVZmjEgwA9PdFat-AD4ALh1-HMA4gGgoEhi0S3mSa7G4pV0jHj1DzEFTYbdacj_dTeFBp1GG6H4TfhLhwXQG72ZQWS4QaG9E9RHdoNSVO4G9XiUZoKVQp4ru8Kq1jjADi1YcKJsDSMhAhDVfftlFEpum_RsjvDWId29_54h8l3yLtMn2wYAgLkBMPkBLbiz-LECghAuhwXQG72ZQWSVAO8dfB5MSxFokdvz5Ch6nJUhGT8_oPGnrv4losCeNl_QSxqiJIPuw3Kid3LZTiJfKGjqrH0PoCYgFKk6nKMw9Arr8MkOmFv7xGEcuuwYBMcK5YpbuONboMC7wCqfXMKxJXbOtvA0-lMgy5KnhlmfnKaVq_CNj46--AD4AICm5aCMk2Qp3uc4hK3_9MmRolZpXWaoAC61WZoxIMAPT3RWrfgA-AC4dfhzAOIBoDwn53kpHQAd-Er2J0q44ZWw5nSxEgSCiIxMqYi_xGbm-E34S4cF0Bu9qJncuEFzaFoRqje0ePcbqAPpVmkZPIOJtehvRsdNgijGLBCjgBFW3fKvoBN2iFgCpFfHGBgACMfbzWmwgt1LXb6g6QuqAIC5ATD5AS24s_ixAoIQL4cF0Bu9qJnclQDvHXweTEsRaJHb8-QoepyVIRk_P6BeCmjvhvCrVKCzmsXXezJVsw07iruuQzIkidNEsq3ud6AeBwY18BVq4N2ooeq1EkbfufnwobbdV5mkDHMtDKE6kKDAu8Aqn1zCsSV2zrbwNPpTIMuSp4ZZn5ymlavwjY-OvvgA-ACApuWgjJNkKd7nOISt__TJkaJWaV1mqAAutVmaMSDAD090Vq34APgAuHX4cwDiAaAMDig0o1bpH6ORun4RM9lUJN7ZYpo-9RVFgBe6f7RtUPhN-EuHBdAbvbjaG7hBu7wMYG1Ayqq84OvyzKY9k6dgniiXVRyWqk08wKhO0dwW0YNfvzGpwtpBZkBwySN2Tuyr9NTeuZkCwH13ZFQURAGAuQEw-QEtuLP4sQKCEDCHBdAbvbjaG5UA7x18HkxLEWiR2_PkKHqclSEZPz-g-i5mwy7sdd_G2X2BNn5PQnp3pMGk3KM2hz9vvSxKA3OgbL-b5mZxM4BwCJ5nORcIWH3wlttL8xU1L69TI13zyaugwLvAKp9cwrElds628DT6UyDLkqeGWZ-cppWr8I2Pjr74APgAgKbloIyTZCne5ziErf_0yZGiVmldZqgALrVZmjEgwA9PdFat-AD4ALh1-HMA4gGgQyeEzgyKepc6956los96DH4DyYs8HdGQqwM5I6KECM_4TfhLhwXQG73ID024QalsmVns-TS23anAnbbqNVP9Ac1n9NFfx1JOK8TRRKQibOcROPVBRXeJHT8FCVJoEavc_GGU-jhN33QGIKpYuXYBgLkBMPkBLbiz-LECghAxhwXQG73ID02VAO8dfB5MSxFokdvz5Ch6nJUhGT8_oPP0khxaLdAg0f2Ge5hJHhIvcShA5jqnEUGzbsEloeOWoJa-NBSXJWec5Jfv2dBOjkfSoxq-F-aL_1pbw4sDDcE7oMC7wCqfXMKxJXbOtvA0-lMgy5KnhlmfnKaVq_CNj46--AD4AICm5aCMk2Qp3uc4hK3_9MmRolZpXWaoAC61WZoxIMAPT3RWrfgA-AC4dfhzAOIBoFluGsZtrWVmcoStczBX6vlunIrVWqUQypgiH-QII2ZV-E34S4cF0Bu912QPuEEMB-M8J_eY5cL0mxHLQsrJAQaQg2AhK9zl-x9U0HdYpmadkjkxz704Kk_vNLCm3d8V_j03-R_mLjF-kDW69qxVAIC5ATD5AS24s_ixAoIQMocF0Bu912QPlQDvHXweTEsRaJHb8-QoepyVIRk_P6AeMrqdcA_uosoPO5Yao5gCV1lU9knDq3dRsjS1uJ3DfKDXxb6b--h8W-rqBv-_EWe6LePMiFZs8AxxnkfIGoSYLaDAu8Aqn1zCsSV2zrbwNPpTIMuSp4ZZn5ymlavwjY-OvvgA-ACApuWgjJNkKd7nOISt__TJkaJWaV1mqAAutVmaMSDAD090Vq34APgAuHX4cwDiAaBPY-_8PjJcEn1Mkd7LbFz6db-LmB3Q3WId96cH7F-jl_hN-EuHBdAbvecMILhBfbfnC-t0lCjBEdDpJRu6RCIdmVvLdPK1qEfs6FYsbotF2Q8UhHVGDmOokf1PhGtLrwOddNeCGL8jXCDN4wqVggCAuQEw-QEtuLP4sQKCEDOHBdAbvecMIJUA7x18HkxLEWiR2_PkKHqclSEZPz-gsaNpcnMfJsmfcq8BYjte7r9Oeb5cyB-srjgSapcD4FyghFqsIznTLGvmyv_PiRXZ8ekXngok1xG1orX_K5jkJLWgwLvAKp9cwrElds628DT6UyDLkqeGWZ-cppWr8I2Pjr74APgAgKbloIyTZCne5ziErf_0yZGiVmldZqgALrVZmjEgwA9PdFat-AD4ALh1-HMA4gGgYleLKz9CCxl-TuNFO3Kw0Sg-nTSH8TjAmthKnHhhWJj4TfhLhwXQG732qeW4QW_WeUEGoZI5SXy_EVUc2IO-42if-lXhe9LSk5B90GcKPwg8CSavOnZuEP9-fqIHmOdoiSNiGcVt6VzfqkLkKbABgIDA".to_string()).unwrap();
        assert_eq!(relay_message, RelayMessage {
            block_updates: vec![BlockUpdate::default()],
            // block_proof: BlockProof::default()
        })
    }

    #[test]
    fn serialize_relay_message() {}
}
