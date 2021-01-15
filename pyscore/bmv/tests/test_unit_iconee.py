import unittest
from ..iconee import *
from ...lib.iconee import rlp, base64
from ...lib.iconee.mta import MerkleTreeAccumulator
from iconservice import Address, AddressPrefix
from tbears.libs.scoretest.score_test_case import ScoreTestCase
from tbears.libs.icon_integrate_test import IconIntegrateTestBase
from typing import List
import secrets
import hashlib
from coincurve import PrivateKey


class Key(object):
    def __init__(self, secret=None) -> None:
        self.private_key = PrivateKey(secret)
        self.addr = address_by_public_key(self.private_key.public_key.format(False))

    def sign(self, _hash: bytes) -> bytes:
        return self.private_key.sign_recoverable(_hash, None)


class Dummy(object):
    ICON_ADDRESS_BODY_SIZE = 20

    @staticmethod
    def address() -> Address:
        return Address.from_data(AddressPrefix.EOA, secrets.token_bytes(Dummy.ICON_ADDRESS_BODY_SIZE))

    @staticmethod
    def score_address() -> Address:
        return Address.from_data(AddressPrefix.CONTRACT, secrets.token_bytes(Dummy.ICON_ADDRESS_BODY_SIZE))

    @staticmethod
    def block_header(height: int, next_validators_hash: bytes) -> BlockHeader:
        pack = []
        pack.append(bytes(0))  # version
        pack.append(height)
        pack.append(int(0))  # timestamp
        pack.append(bytes(0))  # proposer
        pack.append(bytes(0))  # prev_hash
        pack.append(bytes(0))  # vote_hash
        pack.append(next_validators_hash)
        pack.append(bytes(0))  # patch_tx_hash
        pack.append(bytes(0))  # tx_hash
        pack.append(bytes(0))  # logs_bloom
        result = []
        result.append(bytes(0))  # state_hash
        result.append(bytes(0))  # patch_receipt_hash
        result.append(bytes(0))  # receipt_hash
        pack.append(rlp.rlp_encode(result))  # result
        return BlockHeader(rlp.rlp_encode(pack))

    class ValidatorsWithKey(object):
        def __init__(self, validators: Validators, keys: List[Key]) -> None:
            self.validators = validators
            self.keys = keys

    @staticmethod
    def validators_with_key(cnt: int) -> ValidatorsWithKey:
        _addresses = []
        _keys = []
        for i in range(cnt):
            k = Key()
            _keys.append(k)
            _addresses.append(k.addr.to_bytes())
        _validators = rlp.rlp_encode(_addresses)
        return Dummy.ValidatorsWithKey(Validators(_validators), _keys)

    @staticmethod
    def votes(block_header: BlockHeader, validators_with_key: ValidatorsWithKey) -> Votes:
        _round = int(0)
        _part_count = int(1)
        _part_hash = secrets.token_bytes(32)
        _block_part_set_id = [_part_count, _part_hash]
        _timestamp = int("0x598c2d9aaf5de", 0)
        vote_items = []
        vote_msg = []
        vote_msg.append(block_header.height)
        vote_msg.append(_round)
        vote_msg.append(Votes.VOTE_TYPE_PRECOMMIT)
        vote_msg.append(block_header.hash)
        vote_msg.append(_block_part_set_id)
        for k in validators_with_key.keys:
            vote_msg.append(_timestamp)
            serialized_vote_msg = rlp.rlp_encode(vote_msg)
            msg_hash = hashlib.sha3_256(serialized_vote_msg).digest()
            signature = k.sign(msg_hash)
            vote_item = [_timestamp, signature]
            vote_items.append(vote_item)
            vote_msg.pop()
        pack = [_round, _block_part_set_id, vote_items]
        return Votes(rlp.rlp_encode(pack))

    @staticmethod
    def mta() -> MerkleTreeAccumulator:
        return MerkleTreeAccumulator()

    class Data(object):
        def __init__(self, serialized: bytes) -> None:
            unpacked = rlp.rlp_decode(serialized, [int, bytes])
            self.height = unpacked[0]
            self.hash = unpacked[1]
            print(f'Data.height:{self.height},hash:{self.hash.hex()}')


class DummyTest(unittest.TestCase):
    def test_block_header(self):
        self._validators_with_key = Dummy.validators_with_key(4)
        self._validators = self._validators_with_key.validators
        self._mta = MerkleTreeAccumulator()
        self._block_header = Dummy.block_header(1, self._validators.hash)

        for i in range(150):
            self._mta.add(self._block_header.hash)
            n_bytes = ((self._mta.height + (self._mta.height < 0)).bit_length() + 8) // 8
            bs = self._mta.height.to_bytes(n_bytes, byteorder="big", signed=True)
            print(f'{n_bytes} {bs.hex()} {rlp.rlp_encode(self._mta.height).hex()}')
            print(f'{rlp.rlp_decode(rlp.rlp_encode(self._mta.height), int)}')

            if self._mta.height < 0:
                raise Exception(f'invalid height {self._mta.height}')
            bs = bytes(self._mta)
            self._mta = MerkleTreeAccumulator(bs)
            #self._mta.dump()


class BlockWitnessTest(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self) -> None:
        pass

    def test_verify_with_witness(self):
        # ================================================
        #  test_verify
        # ================================================
        # Data ["dog", "cat", "elephant", "bird", "monkey", "lion", "tiger", "last"]
        # Accumulator 5 dog,cat,elephant,bird,monkey
        _mta = "-EcF-ESgLRW8UPg-kGpGwRkzumLmCqfvZJ_97J7DYioRPCizgcH4AKAUevyJu0uatfNpHBb_AJs5WDRfAtrOsbGsMXLamB2Alw=="
        self.mta = MerkleTreeAccumulator(base64.urlsafe_b64decode(_mta))
        self.mta.dump()

        # Witness 0 dog
        _data = "4gGgBc2Y_ezHRTgYKhI_PZHgMYM9o-mwolWNZlLki_MYobI="
        self.data = Dummy.Data(base64.urlsafe_b64decode(_data))

        _block_witness = "-EUF-EKg1hZgfT5LqWp08yPP_F8go8eOfKuOy9uwOxP6j_yb9kSgQPQJm3foiaGDDqEJM3EWYtW1RWXcM0PmPhEdvYzfwZ4="
        self.block_witness = BlockWitness.from_bytes(base64.urlsafe_b64decode(_block_witness))

        # verify MTARoots:5 for Witness 0 dog
        self.block_witness.verify(self.mta, self.data.hash, self.data.height)

        # ================================================
        #  test_verify_after_add
        # ================================================
        # Prepare for test_witness_old
        self.mta.set_cache_size(2)
        # Witness 5 lion at Accumulator 6
        _data = "4gagAB7_6SlDr-8ReDBwNuRqYGuZv5c_A1k6006BIm765hQ="
        self.data = Dummy.Data(base64.urlsafe_b64decode(_data))
        _block_witness = "4wbhoC0VvFD4PpBqRsEZM7pi5gqn72Sf_eyew2IqETwos4HB"
        self.block_witness = BlockWitness.from_bytes(base64.urlsafe_b64decode(_block_witness))

        # Accumulator 6 dog,cat,elephant,bird,monkey,lion
        self.mta.add(self.data.hash)
        self.mta.dump()
        print("mta", self.mta.height, "block_witness", self.block_witness.height, "data", self.data.height)
        # verify MTARoots:6 for Witness:6 Data:lion
        self.block_witness.verify(self.mta, self.data.hash, self.data.height)

        # ================================================
        #  test_witness_old
        # ================================================
        # Witness 6 tiger at Accumulator 7
        _data = "4gegG7APaC4bJqueNu5_n4dJR7wAtZJsaUUaItyZ_oz9IcY="
        data = Dummy.Data(base64.urlsafe_b64decode(_data))
        # Accumulator 7 dog,cat,elephant,bird,monkey,lion,tiger
        self.mta.add(data.hash)
        self.mta.dump()
        # verify MTARoots:7 for Witness:6 Data:lion
        self.block_witness.verify(self.mta, self.data.hash, self.data.height)

        self.data = data
        _block_witness = "wgfA"
        self.block_witness = BlockWitness.from_bytes(base64.urlsafe_b64decode(_block_witness))
        # verify MTARoots:7 for Witness:7 Data:tiger
        self.block_witness.verify(self.mta, self.data.hash, self.data.height)

        # Witness 7 last at Accumulator 8
        _data = "4gigwkLCSvS1XhxXRtw8MlvCm15yH6bJdC5zixIhRyIssEY="
        self.data = Dummy.Data(base64.urlsafe_b64decode(_data))
        _block_witness = "-GYI-GOgG7APaC4bJqueNu5_n4dJR7wAtZJsaUUaItyZ_oz9IcagC0625ghZePK1aO09juXkQVLKj1_6yEUZbJ22fPVoEX2gFHr8ibtLmrXzaRwW_wCbOVg0XwLazrGxrDFy2pgdgJc="
        self.block_witness = BlockWitness.from_bytes(base64.urlsafe_b64decode(_block_witness))
        # Accumulator 8 dog,cat,elephant,bird,monkey,lion,tiger,last
        self.mta.add(self.data.hash)
        self.mta.dump()
        # verify MTARoots:8 for Witness:8 Data:last
        self.block_witness.verify(self.mta, self.data.hash, self.data.height)


class BlockProofTest(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self) -> None:
        pass

    def test_verify_with_block_proof(self):
        # ================================================
        #  test_verify
        # ================================================
        # src -> dst
        _block_proof = "-QJ7uQEE-QEBAoIBxocFqcG2V1FqlQAnd6Rpou7Wm7RaMPRXtRM-EPEQGKA8LBxxndDlY5Hm6hiJweWUAc0KlehMCPTjRlmxlmep6qAA7aokSHhwPeJpbFltqbhFVuhmEBRgdB2Y0wCltf7l8aCpn2Fx05SUw6Tzv4_M5i0VX-lcAUFkwf92ZijMso4F-vgA-ACvAIAgcEgsGg8IhMFgUGAUKhAgh8SiSAicWAEOh4QgYEi8ej8FA0ggkVkcFjcDgIC4RvhEoF0vEnMymuewtNyafRJrf2YxyjVnrXJBryNFkkZD6C_T-ACg2XaYXHpuH6aSAsUi9U-PRFfoSoThX5bFbjPcIZqRhTv5AXGCCWD5AWugrbyWq9EVWmorHLZRS3f_eLk3XpbHyzsUZ5Q3Ht7n92SgBkCTQBWZHNp9rvTSfuefB7MfUxdfAnDCROAc0ttMQCGglQZMM50Trswu6ZGhvYS-j0IUq8i2ICvvmhtiZlPSM_eg4KTpnMquVrNoTEX6yWw9mql9U1vxgGu0lNmcdHSMHDKgZunBwDjiVUIzhUcviUu7ti39kI5r0UnUbuSQ28kLHDeggYY8Q8Oz_GSm4_Pv9EOJJRYW4wD1nFcPoEzZ_Fp8v3Kgvm6F4gj15HLrOBMBpw3fx4kHvbIWj1lt5teM7x2jJGCgcadfPuL5tLH5iob8WBTDKC4Rtv0x8uqDr8_W1185u0WgGnm7VkNN-ZJCjpriKmmLO2a4wiQ3tik8bpXHAvGm-jug1uwPxdBtsZTHoDTILG_BNveu2TzoJuVBlxDOyqXd2fmgsscowuK6m8Rg3X2bwtSFE_I7k7QlBfWgtQyxSpYiXls="
        self._block_proof = BlockProof(base64.urlsafe_b64decode(_block_proof))
        print(f'_block_proof height:{self._block_proof.height} at:{self._block_proof.block_witness.height} hash:{self._block_proof.block_header.hash.hex()}')
        _mta = "-QEbgglg-QEQoMwhqkmLf6hN-l2xCYLlgZHLR_CyNEQVS5vaO545bXmgoGca0Dt2ofGhQ-OUS3VakyVHiLNt3kQ_y7AxOlYXlnqSoNhXoevnmI3yLCWCx0ODpVfIMh9sptJF_7yBttTW5RXfoMCsNHyYIIaNDUOld-BAyhj1Vx3i1pU_6w90r18r2t41oMvHm4QPgPw_rytyyGzFxesgKT6p3ifppl8uIHNcHbSw-ACgprhY9jRbyutOqJJAHjF3SD4P-Rltrg-eaH3EIh-_MXn4AKDkU6anTtnrZPekBdhCfi6mX4Ka6SDZ8ryAO6cxfOR43fgA-ACgA7AwYma5IOdkeQuZqo05R-qpnxDTQy414OG7FKRf-VIBAADAAA=="
        self.mta = MerkleTreeAccumulator.from_bytes(base64.urlsafe_b64decode(_mta))
        self.mta.dump()
        self._block_proof.verify(self.mta)

        # dst -> src
        _block_proof = "-QL6uP_4_QKCAeyHBanBuSKHeZUAEYWEKQgUkNoqJKu0FW1XCYOaJ6GguU_spo_itIiUfPxCMCiqGTSKJB6YSArHdrTG-VjfgzGgVuqE4EZM3QTKHC8ikTxLQcOGGI_YHoM83qXgMzYBeVagqZ9hcdOUlMOk87-PzOYtFV_pXAFBZMH_dmYozLKOBfr4APgAqwCAIHBILBoPBiBCIXBgFDIQIIfBAJC4jEovF4dBoFGI7Ho6BowII1H49AS4RvhEoPYpd2_WLnWb7SolBE7xK5QX6GE2vQ0ACLjjLnCS83sG-ACgTQttEIllOj-KhVVjhs_rYlzEXYKp0NK06gM4xYzkfAz5AfaDAPjb-QHvoGqbE-mg4iF_-cy5gvwo6dyn9DuNxlTMBxozQOm1HU2voLAJd8O-PgpS71jbxghl1kOnOB0IkTOIrdNM0RInnCskoILRhWLqse9NXysxb0BuBzBwk_fbuoWMg86SwKBkYeCKoByjn0Qcq1GS4S_XA_DSa3Tmfpp6IQEp6B_LU_PJudP7oCXgpcVhOS_5zU7X7_haJe9fVXFDgyOR1bIzWm31A4DwoIDdUgf6Uk-KeE-lEXVs7VPseXJhP6YmnEg7zVfc9if-oLsh_4jfLP7TqaZInBTCgE3860_A6M68ojTPGjsCupfBoOI018IKRCew9qqHqxlLPejTDw-j_KVpxsS14So9P6sroKlE10RnHdRhpZNm9EORqAtP9d9QFhAX0YcnY5ltXeZnoHC9m5MI5HUywlzc0ZcE4pWfRZvmpGP4UjUs6h8jbnyFoOVBtOJ92NCnn9pVPuAHbFWWZGwVpyT0F2OFLBBn0LTZoKh3s0-8Gzv47Noia-e9uhmrsl-VUbKtioskMGwl99CnoFqFOE9_kGI4005vEz9IcVc_DFsCByUMeEvUhISEEAsjoAvO3WVnei9J3p3vic3tfLFq9lKdC8IAgxuWRt9IdD48oK_DtOfnzMObPYoKT_68EmEyHyEflKtNnmS7RgQLoNpk"
        self._block_proof = BlockProof(base64.urlsafe_b64decode(_block_proof))
        print(f'_block_proof height:{self._block_proof.height} at:{self._block_proof.block_witness.height} hash:{self._block_proof.block_header.hash.hex()}')
        _mta = "-QFigwD42_kBVvgAoPr9gL3us_XYR9-pJDM9rjOswVuTTwGF6JtDwk9-b2Vu-ACgvoT9Zxkkf2-5hijuDAZcHGs6F6zDZFpLP2ckgOTfLd2gl8u6nU-SirRZTLjm5QxJD20eSUVX51c_eewrqeCLHTT4AKCNYAn5dcfzkxKR3ln3dYT7Cjtr3puy1w2uNH-BXMNrzqCat9vJk9KHx1IQGv-8sBZYODybPYlQf3bZTnbw48J9KfgA-AD4AKCMj56dAI5RBNVAdoaZwYZqCdBjMdUaGw3gJ39f7Vqtz6C5l_sGJ5wESBiCAOiXUxn3ElnomuPfb6vyNfIGDfj63qCQsQ2qHcrhiN7y23j644mFmf-Tg5WYJyFTKp1JfIfGe6ASuu74yuyrZZrJgdmsZ987-uMSaunSznpbDHAfV2eoa6DVWco00FsGVEOrx4Id7LeJm2eSzb8WREM3ZeU0Fq8Z1AEAAMAA"
        self.mta = MerkleTreeAccumulator.from_bytes(base64.urlsafe_b64decode(_mta))
        self.mta.dump()
        self._block_proof.verify(self.mta)


class VotesTest(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self) -> None:
        pass

    def test_verify_with_votes(self):
        # ================================================
        #  test_verify
        # ================================================
        self._validators_with_key = Dummy.validators_with_key(1)
        self._validators = self._validators_with_key.validators
        self._block_header = Dummy.block_header(1, self._validators.hash)
        self._votes = Dummy.votes(self._block_header, self._validators_with_key)
        self._votes.verify(self._block_header.height, self._block_header.hash, self._validators)


class ReceiptProofTest(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self) -> None:
        pass

    def test_prove(self):
        # ================================================
        #  test_prove
        # ================================================
        # proof of transfer
        _root_hash = bytes.fromhex("f342117b9b4320a4ea66ea3400815e9bb02bff4f2e73ab6f13b275aa176ec9b4")
        _receipt_proof = "-QQkAPkBQbkBPvkBO4IgALkBNfkBMgCVAd6Rio-0i5QT5xVHnoEmNfB7pqS4AAAAuPECAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAAQYAAAAAAAAAIAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAQABEAAAAAAAAAAAAAAACAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAEAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAQAAAAAAgAAAAAAAAAAAAQAB-AD4AKAHtRv3wWETe--i8IX6DR1Kv42-_MRImW2c5XyDVTgVWvkC3PjyAPjvo-IQoLg95FOJ_gft6ssfcpiSgxpeZurXrLr1s4RvEmNkhYE1uHP4caBoAuVd-ZklrdfBxSOeSAKqmtNCTWh2bgKoskLuc_5N36CeIPLQiw7vJ9C1xWP-TGtvj4jqQvr3oDrJVgAOyorns6Cf3PuWCxXOUlu6jFN0Du5xZsTbVXe07TbxQMG-tYqJY4CAgICAgICAgICAgICAuFT4UiC4T_hNlQHekYqPtIuUE-cVR56BJjXwe6akuPWYTWVzc2FnZShzdHIsc3RyLEFkZHJlc3MpMIR0ZXN0lQHekYqPtIuUE-cVR56BJjXwe6akuMD48gH476PiEKC4PeRTif4H7erLH3KYkoMaXmbq16y69bOEbxJjZIWBNbhz-HGgaALlXfmZJa3XwcUjnkgCqprTQk1odm4CqLJC7nP-Td-gniDy0IsO7yfQtcVj_kxrb4-I6kL696A6yVYADsqK57Ogn9z7lgsVzlJbuoxTdA7ucWbE21V3tO028UDBvrWKiWOAgICAgICAgICAgICAgLhU-FIguE_4TZUB3pGKj7SLlBPnFUeegSY18HumpLj1mE1lc3NhZ2Uoc3RyLHN0cixBZGRyZXNzKTGEdGVzdJUB3pGKj7SLlBPnFUeegSY18HumpLjA-PIC-O-j4hCguD3kU4n-B-3qyx9ymJKDGl5m6tesuvWzhG8SY2SFgTW4c_hxoGgC5V35mSWt18HFI55IAqqa00JNaHZuAqiyQu5z_k3foJ4g8tCLDu8n0LXFY_5Ma2-PiOpC-vegOslWAA7KiuezoJ_c-5YLFc5SW7qMU3QO7nFmxNtVd7TtNvFAwb61ioljgICAgICAgICAgICAgIC4VPhSILhP-E2VAd6Rio-0i5QT5xVHnoEmNfB7pqS49ZhNZXNzYWdlKHN0cixzdHIsQWRkcmVzcykyhHRlc3SVAd6Rio-0i5QT5xVHnoEmNfB7pqS4wA=="

        serialized_receipt_proof = base64.urlsafe_b64decode(_receipt_proof)
        self.receipt_proof = ReceiptProof(serialized_receipt_proof)
        receipt = self.receipt_proof.prove(_root_hash)
        print("receipt:", receipt.__dict__)


class RelayMessageTest(unittest.TestCase):
    def test_decode(self):
        _msg = "-Ra7-RO5uQI6-QI3uNL40AKCNp-HBbenu2HrrJUA-exb-cGMOyWH7TXKUKquGXtQ_6Kg9fgLrNxNF6moajLZ36xk6INKlWW0f6no1WFJ0lUM3MagyX44V7xcJdRQBH5e0BtVQew7g7_Jyr9TlLXgdFaIOl-gqZ9hcdOUlMOk87-PzOYtFV_pXAFBZMH_dmYozLKOBfr4AKAFeujZxHKhxq-Je39r8Jx7H_L-PoGWrzdwlvTVeNDxcICm5aAQC2Yv1F4qceTyFPZwWPA7ZrSnF51KjN9ItAevSbQhSfgA-AC5AV75AVsA4gGgUC2lKjr_GQboZrwdfdC527ML1XoE-2vt5tjeGjU_d035ATT4S4cFt6e7cYNLuEHCw15-paAZ7tzyS7Zi2RincV4Uh6_Yb-43sNflNqk6Vwessluw_HRzMoq_Dfz4a7GPXfztXgi1TIA-cwBdOxU4AfhLhwW3p7tyAH24QRgZBkxx3XBNkRStzbHjC5g0wJsJFxB48WCC1Q9lhiQvNIK29-5D5R877QJjaQ43JCSTowCWmCfn9OmDSQOhAdkA-EuHBbenu3GrzrhBOoPA0JU6eq7Z-8PzUfYlc9l89B-l9Yc39hCheGy4yyNtvOOAM4_ULQ3RihO_abAAuQ35I6T5iRfCEfR3IB8mzwH4S4cFt6e7cd1RuEHK0AhvlMfmUMO8tuMg8dKepefIGyPEuSY-XLaCQHER5jdpFxAHtB-XA4uCdlGGgR4HpvDvDZ1fTJJJ5J1RGagTAfgAuQI8-QI5uNT40gKCNqCHBbenu3HEj5UAEYWEKQgUkNoqJKu0FW1XCYOaJ6Ggc3PcHGD25S9K2vadHmz9hCLGu2X1wGUbfv6HmhQSDtGg5NBCmJsE36f1rmLbjKmQ7fK5xV4tR3aC20oLF8ywtLKgqZ9hcdOUlMOk87-PzOYtFV_pXAFBZMH_dmYozLKOBfr4APgAgLhG-ESgp1mH7SwqivrPD_LEyfVmgwn8OjFbrRs3j5ymAxrr_gv4AKCQtJdjaoWzOm-3Q4aoG7u5tkjh3swNZizjem-8VDqK6LkBXvkBWwDiAaA6Nu43WkG_08KjmFl5ADmIb2K2taheCFU2WjfM2-JkYfkBNPhLhwW3p7uAVTm4Qe1eNtjWaAKZC84pIlPKUlEYhaJHBPCInkID1BErHbf5TY8AFkrDkxOYg3SGo2l72hkXKd8ltRXqQPQtfdARGVwA-EuHBbenu4BrNLhBQ_l3OSifwSigHbF-H2O1m6fxoTYatqolJKy6rNOVph5iveLi-hxpxTYw-qtO_T7p8wbPq92y9C1L4bNNaIXtQgH4S4cFt6e7gFVMuEHH2T-FjJA4XKrazfTtK5cUxEiFLug-pWUWm-DYCD3ashsxUP1irR2OHItzuExFfgvhcK0ODo_NP3klB31cLBHPAfhLhwW3p7uAVU24Qc25N7Dk1avRaWysv7m0bU8bTbpLeQswjFSOkbhlJj1QH_9CdDQ4LZH_37Kdr64pAy_00GMsVOs7QvADqBFdhZEB-AC5Ahv5Ahi4s_ixAoI2oYcFt6e7gFVMlQCrRSf1elua_hXCZx5U6ujisSJNzqCEzQC9LseCLAm_R9B-KR_BeSOSEtjgkCSW4d-7F5DGrKDC9V-amG8QwARMl1DZ4l8slICX3VLz0jIkq3sgbIMKbqCpn2Fx05SUw6Tzv4_M5i0VX-lcAUFkwf92ZijMso4F-vgA-ACApuWgp1mH7SwqivrPD_LEyfVmgwn8OjFbrRs3j5ymAxrr_gv4APgAuQFe-QFbAOIBoDplJqW_vby0x60FeKnq_CYAT5vzPpkBjq3Flv7W9Y8R-QE0-EuHBbenu4-QEbhBayKh96IJVn4tirHEtRPPfhi_oEcd0aICDxfer7rphtlQlKVEExYxtHAOW06t4nX4kfC0FdQhiEaKUNURAhYurwH4S4cFt6e7j5BjuEFuSmuSGFpethcKmFcRJS5KnBo1zfusO_bUL7wS3t8j2Wn7vsI4pId6oagp887VQgTKHnLLf3yMyYs9b1cQ-PNXAPhLhwW3p7uPkBG4QTDae0mldNhhnqyeXmrKFu8QrxGzl3TqjdzEDhpszNhVVBFrTdDVrUlndUR6r61ho7FxkY_EDSIblXK7u7k_b-EB-EuHBbenu4-lubhBCFNndMldeUmKc4V9HFRzlAE51OfL0Nt3HQRsMHED7CpEaM5reS7nm8ZuWhqvfqMDxw9xx3b_q27YZCndQXasPgD4ALkCG_kCGLiz-LECgjaihwW3p7uPkDqVACd3pGmi7tabtFow9Fe1Ez4Q8RAYoOnTQ13IXvuLzy5ryzeIYy4hPhlPlJEItatauPKMtvTRoD4D3mosTFrYHCHnZXoDMxoixVASr6iHYXY8epfd1GS_oKmfYXHTlJTDpPO_j8zmLRVf6VwBQWTB_3ZmKMyyjgX6-AD4AICm5aCnWYftLCqK-s8P8sTJ9WaDCfw6MVutGzePnKYDGuv-C_gA-AC5AV75AVsA4gGgpNHEqZSjvJKbfR22EMLZzTuKRdWMG4AvjZmQQW2guNf5ATT4S4cFt6e7nwh1uEEE-sZxvy464K9ZgiJr9RSSb9IS80FtD3NMPfQvijFOgwpaObx0QTw7Q35ChLwioGhWkI0BE6AKSgWoCgl-p-N5AfhLhwW3p7ue4Ji4Qf12NgbdzfNsJY3PDfz6L5XksqXG-7On0Hi-XO2Dh8ppR20zvTQFL7JppEivnZmQzqZ2INmQzPSC2mb4CaEZJJcB-EuHBbenu57gu7hBOkbsbJkXVQI7FsdlQT0nb53k4NSJMLf8_JmrY6ppHLMRjI07lSACcTb0UEYM-LGyPYY143P-whvorb2Xi3ipSQD4S4cFt6e7nsKKuEGv7-EdL9xl0tMdbaoFBUw9XDS8EGeTQrEiOqnRLRm21Vp6rYlq0eXGP7_EX48E4De0I_2P5cHu8DlAVugqFkdnAPgAuQIb-QIYuLP4sQKCNqOHBbenu57gqZUA-exb-cGMOyWH7TXKUKquGXtQ_6KgRFR1na35y8IhIxb2kxSyHbZdXXkOsUxnWIr1qNgKEcagE6VtZRKZZ7PMrA_-_Sy5Hn8EzlFFe6qS7nn6PWJaqmSgqZ9hcdOUlMOk87-PzOYtFV_pXAFBZMH_dmYozLKOBfr4APgAgKbloKdZh-0sKor6zw_yxMn1ZoMJ_DoxW60bN4-cpgMa6_4L-AD4ALkBXvkBWwDiAaCnjm-Wwr4Bgamxyrou8NdKLH552BHiwtUWLZKQco-wovkBNPhLhwW3p7uuF9i4Qfbatg2cuRz7TVdLM1y096FyQU-ThKD6IisvCFhTBjhNMQIbxJJbRqyktTmmUF-iQlJSEaf8E1F_Z_3GJtUuqq4B-EuHBbenu642bLhBb-DK5ewsiOmT7f6Y4rEazMqr16Yk7jokfieBXp_tFUUgp0qS9LinOo_LQEvRvWYbE9PKpM_c-zaoPbKY7wfStgD4S4cFt6e7rmaKuEGSBswJh2r02-Mt4CaOv1014jopb7aBNe_wX2wWTlDdIDZBnbU8x2b_kan0uv-OYD-KdGb5z0n-kQnLBxjAMtnqAfhLhwW3p7uuNmq4QaW_38L5-pHEk4KUoy7xLcunt2VvD6pBmSWwycxMmHlJcOKK0NZ5aWHDNLHBvFXOLZc0XhxoulCuuBpg6ilrH7MA-AC5Ahv5Ahi4s_ixAoI2pIcFt6e7rjZrlQARhYQpCBSQ2iokq7QVbVcJg5onoaD_mbOKpv-eZi5g2CMyL_oiSLcDYSm8AotFX-ub-XxaTKDsTKAD7IDMPZk0WJNvmfJmxSYjs-S1Z_AiEMs6dFjD5qCpn2Fx05SUw6Tzv4_M5i0VX-lcAUFkwf92ZijMso4F-vgA-ACApuWgp1mH7SwqivrPD_LEyfVmgwn8OjFbrRs3j5ymAxrr_gv4APgAuQFe-QFbAOIBoHLHy1fNW4NVHVk4--gwrkxoKRC-ziBhaGtO2JQVWPnL-QE0-EuHBbenu71yfbhBbsuJVwelEghF1cMENVisINOXl-QW2ruDjJKg3BJadHBeiA5ZALp6Y3Dy6Wb0LvmQ_sgeUpwRos3ROEzkJp0w6wD4S4cFt6e7vZequEE7llZ6nFVjL73ps-LS4vVzxesZuc3Fbkwi04he_WRlviqBZjpOrU_x93mmJt6jfh4TAhH-NuKmCGob_tL5PvJRAfhLhwW3p7u9rce4QcYd4Z_oHvdpaHqxlRCeYeM3i9m_EuJKWvs5RRE2-TsBOhIkS0S_mqSda9aaSrpOye_RchM3lW0ZNxXQKPdJntQA-EuHBbenu72XkLhBJo5yZbSRJt7rmWTzWGp-J7OhtiaNMoLOcRGDsJyN0owjoSmBFRZCNFsDMnIDJHuiT81uBisIHmiiszWUw7f9xwH4ALkCG_kCGLiz-LECgjalhwW3p7u9l52VAKtFJ_V6W5r-FcJnHlTq6OKxIk3OoKeSETM4zrNxpVvtT-M6hR4WiI8CoSdPUEFZz7PUyDlHoKsrHspGmVYGEHRMw3WKABapGXTL35c2aCQw5ikxEYsEoKmfYXHTlJTDpPO_j8zmLRVf6VwBQWTB_3ZmKMyyjgX6-AD4AICm5aCnWYftLCqK-s8P8sTJ9WaDCfw6MVutGzePnKYDGuv-C_gA-AC5AV75AVsA4gGgTD73RNJZYU7Ml8-JxLmwSvvAMSBl6Mjo5e-1z4iHS6v5ATT4S4cFt6e7zNYsuEHMjx-h8VO5v3etE6J2SVfGtHrbFeoFS4WE9y008AjNVSvhfogrTy8W1-Zj21hZgUvAdc9Byt-djCN48GuygcVjAPhLhwW3p7vMuqC4QbNPsQ9Y4Ob40vcBZo-qMBiYqY89wqH-fKAKC-_dnsi1PuBM4BCguGxPsrCTDPOqUJ81G4LZxO7WiAktKIpSoLQB-EuHBbenu8y6oLhBHpCPRRBEVpnDoaLJV_bzb2kZ5WIjo4Cx2odBqkXd3INqtHXUN9TMPTSctPtPgkGtB641_aasBOR133GbUIOmkQH4S4cFt6e7zLqHuEEeskFTlypUKJ9ObDlDgQ_Lwfv5r1ckzC4twEjg3saur1dUCnqwcYl1arcUi5-QkxrbrqhGkNuPrIhwz9SXO8hwAfgAuQI6-QI3uNL40AKCNqaHBbenu8y6oJUAJ3ekaaLu1pu0WjD0V7UTPhDxEBigIs9e4ATWfTrt28qlij3odgYpYji4Zqt6uJ63hll_vm6gz0QhXULi-4hcuGcSYUWExNl5zY2It0FhU9-vR3Q_qGagqZ9hcdOUlMOk87-PzOYtFV_pXAFBZMH_dmYozLKOBfr4AKCHMupPOBoM9WpVtt6EsAipVZg28CWVExs4-62SHd3RVoCm5aCnWYftLCqK-s8P8sTJ9WaDCfw6MVutGzePnKYDGuv-C_gA-AC5AV75AVsA4gGgBe3x6bLtaIhoNoZ2L-zH6V3Bq-vRl2TkOpcNTXJcXbP5ATT4S4cFt6e73D1MuEEDMlPtWXo0jsa2QYql5jwi6L9M76mFkeugs3fnXdIsDDwnqTZzCUy-xkYHGIohe_AF53OlfrUoHL4xnZVX8tquAPhLhwW3p7vcVJy4QU7qdcLnMlS2PfQoGDmoU2v6OOBlDmDFd8656gNYttK7AbCDOnFbt2LyR7hxClcsv2XHGvlraWIavDZM7743LQMA-EuHBbenu9wltLhBVlbghhHBdbUSK52MKXeBTdJ1-QehM_QFPaFs5PJcM9woH83qluSLjL3n6imBY7xV6RwnirRjJvmJOxQIx0_s9gH4S4cFt6e73A9NuEF0K-OSuv4WOnxmrev33tBMweYrYFnXlR2rzUccpDTchynu--LGNtGeG9kl-vmi4ibFrmaflfSNXsWxSVS1zjeTAfgAuQJn-QJkuP_4_QKCNqeHBbenu9wxgJUA-exb-cGMOyWH7TXKUKquGXtQ_6Kgpe6pNXJcpLGLE40vQX80rom2q3FJ8Jf8ou8i5ClBLCigBXQ-P0b84I3DTeTeNVPavQHEJsnoYa49HF1IA7X3obygqZ9hcdOUlMOk87-PzOYtFV_pXAFBZMH_dmYozLKOBfr4APgAqxCAIHBILBoPCITCoGAoXBxBDoUgIjFIdEIbFYQAYSBIzHgABo_IoJG4TAS4RvhEoCvEqOlmMQqEyeumrTIjL1w0tm2teDabJyAEAlT9hexy-ACgjwcuRPHLBtMx3_Q8Pz4vTLcOMV3rJOU4YItbcqtru-C5AV75AVsA4gGgD9OhmT4GU32xdHcI0H0IbbdvfZL8uSruwhlgC209DVb5ATT4S4cFt6e760SzuEFOmlBNT7zSI2KznWI8wWEw09EbK_c_h2NQfPtF4hwaVGcgG-pCi4668mrynXsx7XjZtAZo-BhSaVWLU6pcA_oEAfhLhwW3p7vriiW4Qd8jNpIXAqU6rfiktfvP9WvLhIeaWmGBUwei01GGDWbvM7PuI96sHMPTWMXDKQLfMmrta4wQ2Yd5r1o-5nZREVIB-EuHBbenu-thy7hBi3nGrGa73ylxv6F6iFv307K2RTrO_2sh2gRRQh-v4LoFZTL2QO810wnfEElseeG6nEDj0GcYildPCeX2subj1QD4S4cFt6e762HbuEHNwgkaWcd3tm0Ikn7-IG3cB06yUYjGXvusw1OOCP8HvG4ZfuuBdrq6zqIu22937I6HgtzmG5xUXMqburgYAu1EAfgA-AD5Avq5Avf5AvQAuQFC-QE_uQE8-QE5giAAuQEz-QEwAJUBAxL7YOZIYMJPHeJFpf96porU2IoAAAC47yEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAA-AD4AKDrCVhpZQU22XyF4HU6o5zrh-um5wM3I27LKwINhDFUuPkBq_kBqAC5AaT5AaG5AZ75AZuCIAC5AZX5AZKVAQMS-2DmSGDCTx3iRaX_eqaK1NiK-FqWTWVzc2FnZShzdHIsaW50LGJ5dGVzKbhAYnRwOi8vMHhkNTBhZjIuaWNvbmVlL2N4ZTE5NDdhM2NhYzkwNzZlYjNhMTUzODI5Zjc5ZDU1NDE2YTU1OTNhMwH5AR25ARr5ARe4PmJ0cDovLzB4ZTkxZi5pY29uZWUvY3gwMzEyZmI2MGU2NDg2MGMyNGYxZGUyNDVhNWZmN2FhNjhhZDRkODhhuEBidHA6Ly8weGQ1MGFmMi5pY29uZWUvY3hlMTk0N2EzY2FjOTA3NmViM2ExNTM4MjlmNzlkNTU0MTZhNTU5M2Ezhl9ldmVudAC4i_iJhExpbmv4grg-YnRwOi8vMHhlOTFmLmljb25lZS9jeDAzMTJmYjYwZTY0ODYwYzI0ZjFkZTI0NWE1ZmY3YWE2OGFkNGQ4OGG4QGJ0cDovLzB4ZDUwYWYyLmljb25lZS9jeGUxOTQ3YTNjYWM5MDc2ZWIzYTE1MzgyOWY3OWQ1NTQxNmE1NTkzYTM="
        serialized = base64.urlsafe_b64decode(_msg)
        rm = RelayMessage(serialized)
        bh: BlockHeader = None
        for bu in rm.block_updates:
            bu: BlockUpdate = bu
            print(f'bu.height:{bu.height}')
            bh = bu.block_header
        for rp in rm.receipt_proofs:
            rp: ReceiptProof = rp
            print(f'rp.index:{rp.index}')
            for ep in rp.event_proofs:
                ep: EventProof = ep
                print(f'ep.index:{ep.index}')
            r = rp.prove(bh.receipt_hash)
            for el in r.event_logs:
                el: EventLog = el
                msg = el.to_message_event()
                if msg is not None:
                    print(f'msg.next_bmc:{msg.next_bmc}')
                    print(f'msg.seq:{msg.seq}')


class BTPMessageVerifierTest(ScoreTestCase):
    def setUp(self):
        super().setUp()
        self._bmc = Dummy.score_address()
        self._btp_bmc = BTPAddress(BTPAddress.PROTOCOL_BTP, '0x1.iconee', str(self._bmc))
        self._validators_with_key = Dummy.validators_with_key(4)
        self._validators = self._validators_with_key.validators
        self._mta = Dummy.mta()
        self.register_interface_score(self._bmc)
        self.score = self.get_score_instance(BTPMessageVerifier, self.test_account1,
                                             on_install_params={
                                                 '_bmc': self._bmc,
                                                 '_net': self._btp_bmc.net,
                                                 '_validators': base64.urlsafe_b64encode(self._validators.to_bytes()),
                                                 '_offset': 0
                                             })

    def test_handleRelayMessage(self):
        # ================================================
        #  test_block_update
        # ================================================

        block_updates = []
        _block_header = Dummy.block_header(1, self._validators.hash)
        _votes = Dummy.votes(_block_header, self._validators_with_key)
        _next_validators = None
        block_update = rlp.rlp_encode([_block_header._bytes, _votes._bytes, _next_validators])
        block_updates.append(block_update)
        block_proof = None
        receipt_proofs = []
        _msgs = rlp.rlp_encode([block_updates, block_proof, receipt_proofs])
        self.set_msg(Address.from_string(self._btp_bmc.contract))
        btp_msgs = self.score.handleRelayMessage(str(self._btp_bmc), str(self._btp_bmc), 1, base64.urlsafe_b64encode(_msgs))
        print(btp_msgs)


if __name__ == '__main__':
    unittest.main()
