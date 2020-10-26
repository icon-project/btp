import unittest
from ..iconee import *
from ...lib.iconee import rlp, base64
from ...lib.iconee.mta import MerkleTreeAccumulator
from iconservice import Address, AddressPrefix
from tbears.libs.scoretest.score_test_case import ScoreTestCase
from tbears.libs.icon_integrate_test import IconIntegrateTestBase
from typing import List
import secrets
import secp256k1
import hashlib


class Key(object):
    def __init__(self, base: secp256k1.Base = None) -> None:
        self.private_key = secp256k1.PrivateKey(base)
        self.addr = address_by_public_key(self.private_key.pubkey.serialize(False))

    def sign(self, _hash: bytes) -> bytes:
        recoverable = self.private_key.ecdsa_sign_recoverable(_hash, True)
        signature, recovery_id = self.private_key.ecdsa_recoverable_serialize(recoverable)
        return bytes(bytearray(signature) + recovery_id.to_bytes(1, 'big'))


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
        pack.append(type_converter.convert_to_bytes(height))
        for i in range(4):
            pack.append(bytes(0))
        pack.append(next_validators_hash)
        for i in range(3):
            pack.append(bytes(0))
        mpt_roots = []
        for i in range(2):
            mpt_roots.append(bytes(0))
        mpt_roots.append(bytes(0))  # receipt_hash
        pack.append(rlp.rlp_encode(mpt_roots))
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
        return Dummy.ValidatorsWithKey(Validators.from_bytes(_validators), _keys)

    @staticmethod
    def votes(block_header: BlockHeader, validators_with_key: ValidatorsWithKey) -> Votes:
        _round = type_converter.convert_to_bytes(0)
        _part_count = type_converter.convert_to_bytes(1)
        _part_hash = secrets.token_bytes(32)
        _block_part_set_id = [_part_count, _part_hash]
        _timestamp = type_converter.convert_to_bytes(int("0x598c2d9aaf5de", 0))
        vote_items = []
        vote_msg = []
        vote_msg.append(type_converter.convert_to_bytes(block_header.height))
        vote_msg.append(_round)
        vote_msg.append(type_converter.convert_to_bytes(Votes.VOTE_TYPE_PRECOMMIT))
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
        self._mta = MerkleTreeAccumulator(0)
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
            self._mta = MerkleTreeAccumulator.from_bytes(bs)
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
        self.mta = MerkleTreeAccumulator.from_bytes(base64.urlsafe_b64decode(_mta))
        self.mta.dump()

        # Witness 0 dog
        _data = "4gCgBc2Y_ezHRTgYKhI_PZHgMYM9o-mwolWNZlLki_MYobI="
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
        _data = "4gWgAB7_6SlDr-8ReDBwNuRqYGuZv5c_A1k6006BIm765hQ="
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
        _data = "4gagG7APaC4bJqueNu5_n4dJR7wAtZJsaUUaItyZ_oz9IcY="
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

        _data = "4gegwkLCSvS1XhxXRtw8MlvCm15yH6bJdC5zixIhRyIssEY="
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
        _msg = "-QS0wLkBrvkBq7j_-P0CLYcFqPXCmEudlQCrRSf1elua_hXCZx5U6ujisSJNzqBIwtSGS81-queSeGQlfs_YPZpl_1_7FaDJHrI7jn_eraAWPMeTq70t6qpoA_R4nxXNFT74QJpXxEkuO3FNYb5wF6Cpn2Fx05SUw6Tzv4_M5i0VX-lcAUFkwf92ZijMso4F-vgA-ACtAIAgcEgsGg8GQEIhcGAUMhAgh4AhUSisWh8Oi8aiURjcGA0PgUMIEDkUegsBuEb4RKBVBB_yVpf8QhzWQpja3K8x2WbW1XJSaQ4o9isrBPRgfPgAoMfzd8Hfo7bPBLzwUrHKUnoAm_SiDgRZeA-YouBr6bxl-Kgt-KWgerT74rjiDRI_hwraLW7nv0gxc0LiXzzSDBzBGKBk7Xmg0s2GxHcdcQtzjLzG_2YiUjxinUiBQtxlVL8kTDg9RjegeJV8VAcMOTNj-NceQOnJUtQW5GgZmSw36jTr2lZN-N2ggkQBFKx0kITXRXNwXI2IkrmfRVnXnzdlkQuc4xikBaCgTj63EPwOlG5AjjiSTlnBFEuc5ReY0U-2JEl7EWPXZYX5Av-5Avz5AvkAuQFC-QE_uQE8-QE5giAAuQEz-QEwAJUBdmBZ5vhff4-tSwbXrBoDY7vPI0wAAAC47wEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGAAAAAAAAAAAAAAEAAAAAAAAAAAAAQAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA-AD4AKCszRxTcIlpI2YsC7E6P8E9ajU9hkxlz7sDfA1_V5vrWvkBsPkBrQC5Aan5Aaa5AaP5AaCCIAC5AZr5AZeVAXZgWeb4X3-PrUsG16waA2O7zyNM-FqWTWVzc2FnZShzdHIsaW50LGJ5dGVzKbhAYnRwOi8vMHg3N2JiODQuaWNvbmVlL2N4MjJmN2MwYWIwZjU5N2ViZmQ5NDQxZGM5ZDgzNDZiMTk3MDhkZjg3MQH5ASK5AR_5ARy4QGJ0cDovLzB4ZDU3ODc1Lmljb25lZS9jeDc2NjA1OWU2Zjg1ZjdmOGZhZDRiMDZkN2FjMWEwMzYzYmJjZjIzNGO4QGJ0cDovLzB4NzdiYjg0Lmljb25lZS9jeDIyZjdjMGFiMGY1OTdlYmZkOTQ0MWRjOWQ4MzQ2YjE5NzA4ZGY4NzGGX2V2ZW50ALiO-IyETGlua_iFuEBidHA6Ly8weGQ1Nzg3NS5pY29uZWUvY3g3NjYwNTllNmY4NWY3ZjhmYWQ0YjA2ZDdhYzFhMDM2M2JiY2YyMzRjuEBidHA6Ly8weDc3YmI4NC5pY29uZWUvY3gyMmY3YzBhYjBmNTk3ZWJmZDk0NDFkYzlkODM0NmIxOTcwOGRmODcxgA=="
        serialized = base64.urlsafe_b64decode(_msg)
        rm = RelayMessage(serialized)
        for block_update in rm.block_updates:
            print(block_update.height)


class BTPMessageVerifierTest(ScoreTestCase):
    def setUp(self):
        super().setUp()
        self._bmc_addr = Dummy.score_address()
        self._validators_with_key = Dummy.validators_with_key(4)
        self._validators = self._validators_with_key.validators
        self._mta = Dummy.mta()
        self.score = self.get_score_instance(BTPMessageVerifier, self.test_account1,
                                             on_install_params={
                                                  '_bmc': self._bmc_addr,
                                                 '_net_addr': '0x1.iconee',
                                                 '_validators': base64.urlsafe_b64encode(self._validators.to_bytes()),
                                                 '_mta_offset': 0
                                             })

    def test_handleRelayMessage(self):
        # ================================================
        #  test_block_update
        # ================================================

        block_proofs = []
        _block_header = Dummy.block_header(1, self._validators.hash)
        _votes = Dummy.votes(_block_header, self._validators_with_key)
        block_proof = rlp.rlp_encode([_block_header._bytes, _votes._bytes, bytes(0), bytes(0)])
        block_proofs.append(block_proof)
        receipt_proofs = []
        _msgs = rlp.rlp_encode([block_proofs, receipt_proofs])

        self.score.handleRelayMessage("", "", 0, base64.urlsafe_b64encode(_msgs))


if __name__ == '__main__':
    unittest.main()
