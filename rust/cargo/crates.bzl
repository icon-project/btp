"""
@generated
cargo-raze generated Bazel file.

DO NOT EDIT! Replaced on runs of cargo-raze
"""

load("@bazel_tools//tools/build_defs/repo:git.bzl", "new_git_repository")  # buildifier: disable=load
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")  # buildifier: disable=load
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")  # buildifier: disable=load

def raze_fetch_remote_crates():
    maybe(
        http_archive,
        name = "raze__Inflector__0_11_4",
        url = "https://crates.io/api/v1/crates/Inflector/0.11.4/download",
        type = "tar.gz",
        sha256 = "fe438c63458706e03479442743baae6c88256498e6431708f6dfc520a26515d3",
        strip_prefix = "Inflector-0.11.4",
        build_file = Label("//cargo/remote:BUILD.Inflector-0.11.4.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__ahash__0_4_7",
        url = "https://crates.io/api/v1/crates/ahash/0.4.7/download",
        type = "tar.gz",
        sha256 = "739f4a8db6605981345c5654f3a85b056ce52f37a39d34da03f25bf2151ea16e",
        strip_prefix = "ahash-0.4.7",
        build_file = Label("//cargo/remote:BUILD.ahash-0.4.7.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__aho_corasick__0_7_18",
        url = "https://crates.io/api/v1/crates/aho-corasick/0.7.18/download",
        type = "tar.gz",
        sha256 = "1e37cfd5e7657ada45f742d6e99ca5788580b5c529dc78faf11ece6dc702656f",
        strip_prefix = "aho-corasick-0.7.18",
        build_file = Label("//cargo/remote:BUILD.aho-corasick-0.7.18.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__arrayref__0_3_6",
        url = "https://crates.io/api/v1/crates/arrayref/0.3.6/download",
        type = "tar.gz",
        sha256 = "a4c527152e37cf757a3f78aae5a06fbeefdb07ccc535c980a3208ee3060dd544",
        strip_prefix = "arrayref-0.3.6",
        build_file = Label("//cargo/remote:BUILD.arrayref-0.3.6.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__arrayvec__0_5_2",
        url = "https://crates.io/api/v1/crates/arrayvec/0.5.2/download",
        type = "tar.gz",
        sha256 = "23b62fc65de8e4e7f52534fb52b0f3ed04746ae267519eef2a83941e8085068b",
        strip_prefix = "arrayvec-0.5.2",
        build_file = Label("//cargo/remote:BUILD.arrayvec-0.5.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__arrayvec__0_7_2",
        url = "https://crates.io/api/v1/crates/arrayvec/0.7.2/download",
        type = "tar.gz",
        sha256 = "8da52d66c7071e2e3fa2a1e5c6d088fec47b593032b254f5e980de8ea54454d6",
        strip_prefix = "arrayvec-0.7.2",
        build_file = Label("//cargo/remote:BUILD.arrayvec-0.7.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__autocfg__1_0_1",
        url = "https://crates.io/api/v1/crates/autocfg/1.0.1/download",
        type = "tar.gz",
        sha256 = "cdb031dd78e28731d87d56cc8ffef4a8f36ca26c38fe2de700543e627f8a464a",
        strip_prefix = "autocfg-1.0.1",
        build_file = Label("//cargo/remote:BUILD.autocfg-1.0.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__base64__0_11_0",
        url = "https://crates.io/api/v1/crates/base64/0.11.0/download",
        type = "tar.gz",
        sha256 = "b41b7ea54a0c9d92199de89e20e58d49f02f8e699814ef3fdf266f6f748d15c7",
        strip_prefix = "base64-0.11.0",
        build_file = Label("//cargo/remote:BUILD.base64-0.11.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__base64__0_13_0",
        url = "https://crates.io/api/v1/crates/base64/0.13.0/download",
        type = "tar.gz",
        sha256 = "904dfeac50f3cdaba28fc6f57fdcddb75f49ed61346676a78c4ffe55877802fd",
        strip_prefix = "base64-0.13.0",
        build_file = Label("//cargo/remote:BUILD.base64-0.13.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__bitvec__0_20_4",
        url = "https://crates.io/api/v1/crates/bitvec/0.20.4/download",
        type = "tar.gz",
        sha256 = "7774144344a4faa177370406a7ff5f1da24303817368584c6206c8303eb07848",
        strip_prefix = "bitvec-0.20.4",
        build_file = Label("//cargo/remote:BUILD.bitvec-0.20.4.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__blake2__0_9_2",
        url = "https://crates.io/api/v1/crates/blake2/0.9.2/download",
        type = "tar.gz",
        sha256 = "0a4e37d16930f5459780f5621038b6382b9bb37c19016f39fb6b5808d831f174",
        strip_prefix = "blake2-0.9.2",
        build_file = Label("//cargo/remote:BUILD.blake2-0.9.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__block_buffer__0_9_0",
        url = "https://crates.io/api/v1/crates/block-buffer/0.9.0/download",
        type = "tar.gz",
        sha256 = "4152116fd6e9dadb291ae18fc1ec3575ed6d84c29642d97890f4b4a3417297e4",
        strip_prefix = "block-buffer-0.9.0",
        build_file = Label("//cargo/remote:BUILD.block-buffer-0.9.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__block_padding__0_2_1",
        url = "https://crates.io/api/v1/crates/block-padding/0.2.1/download",
        type = "tar.gz",
        sha256 = "8d696c370c750c948ada61c69a0ee2cbbb9c50b1019ddb86d9317157a99c2cae",
        strip_prefix = "block-padding-0.2.1",
        build_file = Label("//cargo/remote:BUILD.block-padding-0.2.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__borsh__0_8_2",
        url = "https://crates.io/api/v1/crates/borsh/0.8.2/download",
        type = "tar.gz",
        sha256 = "09a7111f797cc721407885a323fb071636aee57f750b1a4ddc27397eba168a74",
        strip_prefix = "borsh-0.8.2",
        build_file = Label("//cargo/remote:BUILD.borsh-0.8.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__borsh_derive__0_8_2",
        url = "https://crates.io/api/v1/crates/borsh-derive/0.8.2/download",
        type = "tar.gz",
        sha256 = "307f3740906bac2c118a8122fe22681232b244f1369273e45f1156b45c43d2dd",
        strip_prefix = "borsh-derive-0.8.2",
        build_file = Label("//cargo/remote:BUILD.borsh-derive-0.8.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__borsh_derive_internal__0_8_2",
        url = "https://crates.io/api/v1/crates/borsh-derive-internal/0.8.2/download",
        type = "tar.gz",
        sha256 = "d2104c73179359431cc98e016998f2f23bc7a05bc53e79741bcba705f30047bc",
        strip_prefix = "borsh-derive-internal-0.8.2",
        build_file = Label("//cargo/remote:BUILD.borsh-derive-internal-0.8.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__borsh_schema_derive_internal__0_8_2",
        url = "https://crates.io/api/v1/crates/borsh-schema-derive-internal/0.8.2/download",
        type = "tar.gz",
        sha256 = "ae29eb8418fcd46f723f8691a2ac06857d31179d33d2f2d91eb13967de97c728",
        strip_prefix = "borsh-schema-derive-internal-0.8.2",
        build_file = Label("//cargo/remote:BUILD.borsh-schema-derive-internal-0.8.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__bs58__0_4_0",
        url = "https://crates.io/api/v1/crates/bs58/0.4.0/download",
        type = "tar.gz",
        sha256 = "771fe0050b883fcc3ea2359b1a96bcfbc090b7116eae7c3c512c7a083fdf23d3",
        strip_prefix = "bs58-0.4.0",
        build_file = Label("//cargo/remote:BUILD.bs58-0.4.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__byte_slice_cast__1_2_0",
        url = "https://crates.io/api/v1/crates/byte-slice-cast/1.2.0/download",
        type = "tar.gz",
        sha256 = "1d30c751592b77c499e7bce34d99d67c2c11bdc0574e9a488ddade14150a4698",
        strip_prefix = "byte-slice-cast-1.2.0",
        build_file = Label("//cargo/remote:BUILD.byte-slice-cast-1.2.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__byteorder__1_4_3",
        url = "https://crates.io/api/v1/crates/byteorder/1.4.3/download",
        type = "tar.gz",
        sha256 = "14c189c53d098945499cdfa7ecc63567cf3886b3332b312a5b4585d8d3a6a610",
        strip_prefix = "byteorder-1.4.3",
        build_file = Label("//cargo/remote:BUILD.byteorder-1.4.3.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__bytes__1_1_0",
        url = "https://crates.io/api/v1/crates/bytes/1.1.0/download",
        type = "tar.gz",
        sha256 = "c4872d67bab6358e59559027aa3b9157c53d9358c51423c17554809a8858e0f8",
        strip_prefix = "bytes-1.1.0",
        build_file = Label("//cargo/remote:BUILD.bytes-1.1.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__c2_chacha__0_3_3",
        url = "https://crates.io/api/v1/crates/c2-chacha/0.3.3/download",
        type = "tar.gz",
        sha256 = "d27dae93fe7b1e0424dc57179ac396908c26b035a87234809f5c4dfd1b47dc80",
        strip_prefix = "c2-chacha-0.3.3",
        build_file = Label("//cargo/remote:BUILD.c2-chacha-0.3.3.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__cc__1_0_72",
        url = "https://crates.io/api/v1/crates/cc/1.0.72/download",
        type = "tar.gz",
        sha256 = "22a9137b95ea06864e018375b72adfb7db6e6f68cfc8df5a04d00288050485ee",
        strip_prefix = "cc-1.0.72",
        build_file = Label("//cargo/remote:BUILD.cc-1.0.72.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__cfg_if__0_1_10",
        url = "https://crates.io/api/v1/crates/cfg-if/0.1.10/download",
        type = "tar.gz",
        sha256 = "4785bdd1c96b2a846b2bd7cc02e86b6b3dbf14e7e53446c4f54c92a361040822",
        strip_prefix = "cfg-if-0.1.10",
        build_file = Label("//cargo/remote:BUILD.cfg-if-0.1.10.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__cfg_if__1_0_0",
        url = "https://crates.io/api/v1/crates/cfg-if/1.0.0/download",
        type = "tar.gz",
        sha256 = "baf1de4339761588bc0619e3cbc0120ee582ebb74b53b4efbf79117bd2da40fd",
        strip_prefix = "cfg-if-1.0.0",
        build_file = Label("//cargo/remote:BUILD.cfg-if-1.0.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__chrono__0_4_19",
        url = "https://crates.io/api/v1/crates/chrono/0.4.19/download",
        type = "tar.gz",
        sha256 = "670ad68c9088c2a963aaa298cb369688cf3f9465ce5e2d4ca10e6e0098a1ce73",
        strip_prefix = "chrono-0.4.19",
        build_file = Label("//cargo/remote:BUILD.chrono-0.4.19.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__cipher__0_2_5",
        url = "https://crates.io/api/v1/crates/cipher/0.2.5/download",
        type = "tar.gz",
        sha256 = "12f8e7987cbd042a63249497f41aed09f8e65add917ea6566effbc56578d6801",
        strip_prefix = "cipher-0.2.5",
        build_file = Label("//cargo/remote:BUILD.cipher-0.2.5.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__convert_case__0_4_0",
        url = "https://crates.io/api/v1/crates/convert_case/0.4.0/download",
        type = "tar.gz",
        sha256 = "6245d59a3e82a7fc217c5828a6692dbc6dfb63a0c8c90495621f7b9d79704a0e",
        strip_prefix = "convert_case-0.4.0",
        build_file = Label("//cargo/remote:BUILD.convert_case-0.4.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__cpufeatures__0_2_1",
        url = "https://crates.io/api/v1/crates/cpufeatures/0.2.1/download",
        type = "tar.gz",
        sha256 = "95059428f66df56b63431fdb4e1947ed2190586af5c5a8a8b71122bdf5a7f469",
        strip_prefix = "cpufeatures-0.2.1",
        build_file = Label("//cargo/remote:BUILD.cpufeatures-0.2.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__crunchy__0_2_2",
        url = "https://crates.io/api/v1/crates/crunchy/0.2.2/download",
        type = "tar.gz",
        sha256 = "7a81dae078cea95a014a339291cec439d2f232ebe854a9d672b796c6afafa9b7",
        strip_prefix = "crunchy-0.2.2",
        build_file = Label("//cargo/remote:BUILD.crunchy-0.2.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__crypto_mac__0_8_0",
        url = "https://crates.io/api/v1/crates/crypto-mac/0.8.0/download",
        type = "tar.gz",
        sha256 = "b584a330336237c1eecd3e94266efb216c56ed91225d634cb2991c5f3fd1aeab",
        strip_prefix = "crypto-mac-0.8.0",
        build_file = Label("//cargo/remote:BUILD.crypto-mac-0.8.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__curve25519_dalek__3_2_0",
        url = "https://crates.io/api/v1/crates/curve25519-dalek/3.2.0/download",
        type = "tar.gz",
        sha256 = "0b9fdf9972b2bd6af2d913799d9ebc165ea4d2e65878e329d9c6b372c4491b61",
        strip_prefix = "curve25519-dalek-3.2.0",
        build_file = Label("//cargo/remote:BUILD.curve25519-dalek-3.2.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__derive_more__0_99_16",
        url = "https://crates.io/api/v1/crates/derive_more/0.99.16/download",
        type = "tar.gz",
        sha256 = "40eebddd2156ce1bb37b20bbe5151340a31828b1f2d22ba4141f3531710e38df",
        strip_prefix = "derive_more-0.99.16",
        build_file = Label("//cargo/remote:BUILD.derive_more-0.99.16.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__digest__0_9_0",
        url = "https://crates.io/api/v1/crates/digest/0.9.0/download",
        type = "tar.gz",
        sha256 = "d3dd60d1080a57a05ab032377049e0591415d2b31afd7028356dbf3cc6dcb066",
        strip_prefix = "digest-0.9.0",
        build_file = Label("//cargo/remote:BUILD.digest-0.9.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__easy_ext__0_2_9",
        url = "https://crates.io/api/v1/crates/easy-ext/0.2.9/download",
        type = "tar.gz",
        sha256 = "53aff6fdc1b181225acdcb5b14c47106726fd8e486707315b1b138baed68ee31",
        strip_prefix = "easy-ext-0.2.9",
        build_file = Label("//cargo/remote:BUILD.easy-ext-0.2.9.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__ed25519__1_3_0",
        url = "https://crates.io/api/v1/crates/ed25519/1.3.0/download",
        type = "tar.gz",
        sha256 = "74e1069e39f1454367eb2de793ed062fac4c35c2934b76a81d90dd9abcd28816",
        strip_prefix = "ed25519-1.3.0",
        build_file = Label("//cargo/remote:BUILD.ed25519-1.3.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__ed25519_dalek__1_0_1",
        url = "https://crates.io/api/v1/crates/ed25519-dalek/1.0.1/download",
        type = "tar.gz",
        sha256 = "c762bae6dcaf24c4c84667b8579785430908723d5c889f469d76a41d59cc7a9d",
        strip_prefix = "ed25519-dalek-1.0.1",
        build_file = Label("//cargo/remote:BUILD.ed25519-dalek-1.0.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__ethbloom__0_11_1",
        url = "https://crates.io/api/v1/crates/ethbloom/0.11.1/download",
        type = "tar.gz",
        sha256 = "bfb684ac8fa8f6c5759f788862bb22ec6fe3cb392f6bfd08e3c64b603661e3f8",
        strip_prefix = "ethbloom-0.11.1",
        build_file = Label("//cargo/remote:BUILD.ethbloom-0.11.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__ethereum_types__0_11_0",
        url = "https://crates.io/api/v1/crates/ethereum-types/0.11.0/download",
        type = "tar.gz",
        sha256 = "f64b5df66a228d85e4b17e5d6c6aa43b0310898ffe8a85988c4c032357aaabfd",
        strip_prefix = "ethereum-types-0.11.0",
        build_file = Label("//cargo/remote:BUILD.ethereum-types-0.11.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__fixed_hash__0_7_0",
        url = "https://crates.io/api/v1/crates/fixed-hash/0.7.0/download",
        type = "tar.gz",
        sha256 = "cfcf0ed7fe52a17a03854ec54a9f76d6d84508d1c0e66bc1793301c73fc8493c",
        strip_prefix = "fixed-hash-0.7.0",
        build_file = Label("//cargo/remote:BUILD.fixed-hash-0.7.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__form_urlencoded__1_0_1",
        url = "https://crates.io/api/v1/crates/form_urlencoded/1.0.1/download",
        type = "tar.gz",
        sha256 = "5fc25a87fa4fd2094bffb06925852034d90a17f0d1e05197d4956d3555752191",
        strip_prefix = "form_urlencoded-1.0.1",
        build_file = Label("//cargo/remote:BUILD.form_urlencoded-1.0.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__fs_extra__1_2_0",
        url = "https://crates.io/api/v1/crates/fs_extra/1.2.0/download",
        type = "tar.gz",
        sha256 = "2022715d62ab30faffd124d40b76f4134a550a87792276512b18d63272333394",
        strip_prefix = "fs_extra-1.2.0",
        build_file = Label("//cargo/remote:BUILD.fs_extra-1.2.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__funty__1_1_0",
        url = "https://crates.io/api/v1/crates/funty/1.1.0/download",
        type = "tar.gz",
        sha256 = "fed34cd105917e91daa4da6b3728c47b068749d6a62c59811f06ed2ac71d9da7",
        strip_prefix = "funty-1.1.0",
        build_file = Label("//cargo/remote:BUILD.funty-1.1.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__generic_array__0_14_4",
        url = "https://crates.io/api/v1/crates/generic-array/0.14.4/download",
        type = "tar.gz",
        sha256 = "501466ecc8a30d1d3b7fc9229b122b2ce8ed6e9d9223f1138d4babb253e51817",
        strip_prefix = "generic-array-0.14.4",
        build_file = Label("//cargo/remote:BUILD.generic-array-0.14.4.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__getrandom__0_1_16",
        url = "https://crates.io/api/v1/crates/getrandom/0.1.16/download",
        type = "tar.gz",
        sha256 = "8fc3cb4d91f53b50155bdcfd23f6a4c39ae1969c2ae85982b135750cccaf5fce",
        strip_prefix = "getrandom-0.1.16",
        build_file = Label("//cargo/remote:BUILD.getrandom-0.1.16.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__getrandom__0_2_3",
        url = "https://crates.io/api/v1/crates/getrandom/0.2.3/download",
        type = "tar.gz",
        sha256 = "7fcd999463524c52659517fe2cea98493cfe485d10565e7b0fb07dbba7ad2753",
        strip_prefix = "getrandom-0.2.3",
        build_file = Label("//cargo/remote:BUILD.getrandom-0.2.3.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__hashbrown__0_11_2",
        url = "https://crates.io/api/v1/crates/hashbrown/0.11.2/download",
        type = "tar.gz",
        sha256 = "ab5ef0d4909ef3724cc8cce6ccc8572c5c817592e9285f5464f8e86f8bd3726e",
        strip_prefix = "hashbrown-0.11.2",
        build_file = Label("//cargo/remote:BUILD.hashbrown-0.11.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__hashbrown__0_9_1",
        url = "https://crates.io/api/v1/crates/hashbrown/0.9.1/download",
        type = "tar.gz",
        sha256 = "d7afe4a420e3fe79967a00898cc1f4db7c8a49a9333a29f8a4bd76a253d5cd04",
        strip_prefix = "hashbrown-0.9.1",
        build_file = Label("//cargo/remote:BUILD.hashbrown-0.9.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__hex__0_4_3",
        url = "https://crates.io/api/v1/crates/hex/0.4.3/download",
        type = "tar.gz",
        sha256 = "7f24254aa9a54b5c858eaee2f5bccdb46aaf0e486a595ed5fd8f86ba55232a70",
        strip_prefix = "hex-0.4.3",
        build_file = Label("//cargo/remote:BUILD.hex-0.4.3.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__idna__0_2_3",
        url = "https://crates.io/api/v1/crates/idna/0.2.3/download",
        type = "tar.gz",
        sha256 = "418a0a6fab821475f634efe3ccc45c013f742efe03d853e8d3355d5cb850ecf8",
        strip_prefix = "idna-0.2.3",
        build_file = Label("//cargo/remote:BUILD.idna-0.2.3.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__impl_codec__0_5_1",
        url = "https://crates.io/api/v1/crates/impl-codec/0.5.1/download",
        type = "tar.gz",
        sha256 = "161ebdfec3c8e3b52bf61c4f3550a1eea4f9579d10dc1b936f3171ebdcd6c443",
        strip_prefix = "impl-codec-0.5.1",
        build_file = Label("//cargo/remote:BUILD.impl-codec-0.5.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__rlp_derive__0_1_0",
        url = "https://crates.io/api/v1/crates/rlp-derive/0.1.0/download",
        type = "tar.gz",
        sha256 = "e33d7b2abe0c340d8797fe2907d3f20d3b5ea5908683618bfe80df7f621f672a",
        strip_prefix = "rlp-derive-0.1.0",
        build_file = Label("//cargo/remote:BUILD.rlp-derive-0.1.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__impl_rlp__0_3_0",
        url = "https://crates.io/api/v1/crates/impl-rlp/0.3.0/download",
        type = "tar.gz",
        sha256 = "f28220f89297a075ddc7245cd538076ee98b01f2a9c23a53a4f1105d5a322808",
        strip_prefix = "impl-rlp-0.3.0",
        build_file = Label("//cargo/remote:BUILD.impl-rlp-0.3.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__impl_serde__0_3_2",
        url = "https://crates.io/api/v1/crates/impl-serde/0.3.2/download",
        type = "tar.gz",
        sha256 = "4551f042f3438e64dbd6226b20527fc84a6e1fe65688b58746a2f53623f25f5c",
        strip_prefix = "impl-serde-0.3.2",
        build_file = Label("//cargo/remote:BUILD.impl-serde-0.3.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__impl_trait_for_tuples__0_2_1",
        url = "https://crates.io/api/v1/crates/impl-trait-for-tuples/0.2.1/download",
        type = "tar.gz",
        sha256 = "d5dacb10c5b3bb92d46ba347505a9041e676bb20ad220101326bffb0c93031ee",
        strip_prefix = "impl-trait-for-tuples-0.2.1",
        build_file = Label("//cargo/remote:BUILD.impl-trait-for-tuples-0.2.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__indexmap__1_7_0",
        url = "https://crates.io/api/v1/crates/indexmap/1.7.0/download",
        type = "tar.gz",
        sha256 = "bc633605454125dec4b66843673f01c7df2b89479b32e0ed634e43a91cff62a5",
        strip_prefix = "indexmap-1.7.0",
        build_file = Label("//cargo/remote:BUILD.indexmap-1.7.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__itoa__0_4_8",
        url = "https://crates.io/api/v1/crates/itoa/0.4.8/download",
        type = "tar.gz",
        sha256 = "b71991ff56294aa922b450139ee08b3bfc70982c6b2c7562771375cf73542dd4",
        strip_prefix = "itoa-0.4.8",
        build_file = Label("//cargo/remote:BUILD.itoa-0.4.8.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__jemalloc_sys__0_3_2",
        url = "https://crates.io/api/v1/crates/jemalloc-sys/0.3.2/download",
        type = "tar.gz",
        sha256 = "0d3b9f3f5c9b31aa0f5ed3260385ac205db665baa41d49bb8338008ae94ede45",
        strip_prefix = "jemalloc-sys-0.3.2",
        build_file = Label("//cargo/remote:BUILD.jemalloc-sys-0.3.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__jemallocator__0_3_2",
        url = "https://crates.io/api/v1/crates/jemallocator/0.3.2/download",
        type = "tar.gz",
        sha256 = "43ae63fcfc45e99ab3d1b29a46782ad679e98436c3169d15a167a1108a724b69",
        strip_prefix = "jemallocator-0.3.2",
        build_file = Label("//cargo/remote:BUILD.jemallocator-0.3.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__keccak__0_1_0",
        url = "https://crates.io/api/v1/crates/keccak/0.1.0/download",
        type = "tar.gz",
        sha256 = "67c21572b4949434e4fc1e1978b99c5f77064153c59d998bf13ecd96fb5ecba7",
        strip_prefix = "keccak-0.1.0",
        build_file = Label("//cargo/remote:BUILD.keccak-0.1.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__lazy_static__1_4_0",
        url = "https://crates.io/api/v1/crates/lazy_static/1.4.0/download",
        type = "tar.gz",
        sha256 = "e2abad23fbc42b3700f2f279844dc832adb2b2eb069b2df918f455c4e18cc646",
        strip_prefix = "lazy_static-1.4.0",
        build_file = Label("//cargo/remote:BUILD.lazy_static-1.4.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__libc__0_2_108",
        url = "https://crates.io/api/v1/crates/libc/0.2.108/download",
        type = "tar.gz",
        sha256 = "8521a1b57e76b1ec69af7599e75e38e7b7fad6610f037db8c79b127201b5d119",
        strip_prefix = "libc-0.2.108",
        build_file = Label("//cargo/remote:BUILD.libc-0.2.108.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__matches__0_1_9",
        url = "https://crates.io/api/v1/crates/matches/0.1.9/download",
        type = "tar.gz",
        sha256 = "a3e378b66a060d48947b590737b30a1be76706c8dd7b8ba0f2fe3989c68a853f",
        strip_prefix = "matches-0.1.9",
        build_file = Label("//cargo/remote:BUILD.matches-0.1.9.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__memchr__2_4_1",
        url = "https://crates.io/api/v1/crates/memchr/2.4.1/download",
        type = "tar.gz",
        sha256 = "308cc39be01b73d0d18f82a0e7b2a3df85245f84af96fdddc5d202d27e47b86a",
        strip_prefix = "memchr-2.4.1",
        build_file = Label("//cargo/remote:BUILD.memchr-2.4.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__memory_units__0_4_0",
        url = "https://crates.io/api/v1/crates/memory_units/0.4.0/download",
        type = "tar.gz",
        sha256 = "8452105ba047068f40ff7093dd1d9da90898e63dd61736462e9cdda6a90ad3c3",
        strip_prefix = "memory_units-0.4.0",
        build_file = Label("//cargo/remote:BUILD.memory_units-0.4.0.bazel"),
    )

    maybe(
        new_git_repository,
        name = "raze__near_crypto__0_1_0",
        remote = "https://github.com/near/nearcore",
        commit = "d95a1edf9f4a3f751876907f8487e6eefa6594ec",
        build_file = Label("//cargo/remote:BUILD.near-crypto-0.1.0.bazel"),
        init_submodules = True,
    )

    maybe(
        new_git_repository,
        name = "raze__near_primitives__0_1_0",
        remote = "https://github.com/near/nearcore",
        commit = "d95a1edf9f4a3f751876907f8487e6eefa6594ec",
        build_file = Label("//cargo/remote:BUILD.near-primitives-0.1.0.bazel"),
        init_submodules = True,
    )

    maybe(
        new_git_repository,
        name = "raze__near_primitives_core__0_1_0",
        remote = "https://github.com/near/nearcore",
        commit = "d95a1edf9f4a3f751876907f8487e6eefa6594ec",
        build_file = Label("//cargo/remote:BUILD.near-primitives-core-0.1.0.bazel"),
        init_submodules = True,
    )

    maybe(
        new_git_repository,
        name = "raze__near_rpc_error_core__0_1_0",
        remote = "https://github.com/near/nearcore",
        commit = "d95a1edf9f4a3f751876907f8487e6eefa6594ec",
        build_file = Label("//cargo/remote:BUILD.near-rpc-error-core-0.1.0.bazel"),
        init_submodules = True,
    )

    maybe(
        new_git_repository,
        name = "raze__near_rpc_error_macro__0_1_0",
        remote = "https://github.com/near/nearcore",
        commit = "d95a1edf9f4a3f751876907f8487e6eefa6594ec",
        build_file = Label("//cargo/remote:BUILD.near-rpc-error-macro-0.1.0.bazel"),
        init_submodules = True,
    )

    maybe(
        new_git_repository,
        name = "raze__near_runtime_utils__3_0_0",
        remote = "https://github.com/near/nearcore",
        commit = "d95a1edf9f4a3f751876907f8487e6eefa6594ec",
        build_file = Label("//cargo/remote:BUILD.near-runtime-utils-3.0.0.bazel"),
        init_submodules = True,
    )

    maybe(
        new_git_repository,
        name = "raze__near_sdk__4_0_0_pre_4",
        remote = "https://github.com/HugoByte/near-sdk-rs.git",
        commit = "b18b4751c556e6da7dbbab678b3ec981f48a406d",
        build_file = Label("//cargo/remote:BUILD.near-sdk-4.0.0-pre.4.bazel"),
        init_submodules = True,
    )

    maybe(
        new_git_repository,
        name = "raze__near_sdk_macros__4_0_0_pre_4",
        remote = "https://github.com/HugoByte/near-sdk-rs.git",
        commit = "b18b4751c556e6da7dbbab678b3ec981f48a406d",
        build_file = Label("//cargo/remote:BUILD.near-sdk-macros-4.0.0-pre.4.bazel"),
        init_submodules = True,
    )

    maybe(
        new_git_repository,
        name = "raze__near_sys__0_1_0",
        remote = "https://github.com/HugoByte/near-sdk-rs.git",
        commit = "b18b4751c556e6da7dbbab678b3ec981f48a406d",
        build_file = Label("//cargo/remote:BUILD.near-sys-0.1.0.bazel"),
        init_submodules = True,
    )

    maybe(
        new_git_repository,
        name = "raze__near_vm_errors__3_0_0",
        remote = "https://github.com/near/nearcore",
        commit = "d95a1edf9f4a3f751876907f8487e6eefa6594ec",
        build_file = Label("//cargo/remote:BUILD.near-vm-errors-3.0.0.bazel"),
        init_submodules = True,
    )

    maybe(
        new_git_repository,
        name = "raze__near_vm_logic__3_0_0",
        remote = "https://github.com/near/nearcore",
        commit = "d95a1edf9f4a3f751876907f8487e6eefa6594ec",
        build_file = Label("//cargo/remote:BUILD.near-vm-logic-3.0.0.bazel"),
        init_submodules = True,
    )

    maybe(
        http_archive,
        name = "raze__num_bigint__0_3_3",
        url = "https://crates.io/api/v1/crates/num-bigint/0.3.3/download",
        type = "tar.gz",
        sha256 = "5f6f7833f2cbf2360a6cfd58cd41a53aa7a90bd4c202f5b1c7dd2ed73c57b2c3",
        strip_prefix = "num-bigint-0.3.3",
        build_file = Label("//cargo/remote:BUILD.num-bigint-0.3.3.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__num_integer__0_1_44",
        url = "https://crates.io/api/v1/crates/num-integer/0.1.44/download",
        type = "tar.gz",
        sha256 = "d2cc698a63b549a70bc047073d2949cce27cd1c7b0a4a862d08a8031bc2801db",
        strip_prefix = "num-integer-0.1.44",
        build_file = Label("//cargo/remote:BUILD.num-integer-0.1.44.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__num_rational__0_3_2",
        url = "https://crates.io/api/v1/crates/num-rational/0.3.2/download",
        type = "tar.gz",
        sha256 = "12ac428b1cb17fce6f731001d307d351ec70a6d202fc2e60f7d4c5e42d8f4f07",
        strip_prefix = "num-rational-0.3.2",
        build_file = Label("//cargo/remote:BUILD.num-rational-0.3.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__num_traits__0_2_14",
        url = "https://crates.io/api/v1/crates/num-traits/0.2.14/download",
        type = "tar.gz",
        sha256 = "9a64b1ec5cda2586e284722486d802acf1f7dbdc623e2bfc57e65ca1cd099290",
        strip_prefix = "num-traits-0.2.14",
        build_file = Label("//cargo/remote:BUILD.num-traits-0.2.14.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__opaque_debug__0_3_0",
        url = "https://crates.io/api/v1/crates/opaque-debug/0.3.0/download",
        type = "tar.gz",
        sha256 = "624a8340c38c1b80fd549087862da4ba43e08858af025b236e509b6649fc13d5",
        strip_prefix = "opaque-debug-0.3.0",
        build_file = Label("//cargo/remote:BUILD.opaque-debug-0.3.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__parity_scale_codec__2_3_1",
        url = "https://crates.io/api/v1/crates/parity-scale-codec/2.3.1/download",
        type = "tar.gz",
        sha256 = "373b1a4c1338d9cd3d1fa53b3a11bdab5ab6bd80a20f7f7becd76953ae2be909",
        strip_prefix = "parity-scale-codec-2.3.1",
        build_file = Label("//cargo/remote:BUILD.parity-scale-codec-2.3.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__parity_scale_codec_derive__2_3_1",
        url = "https://crates.io/api/v1/crates/parity-scale-codec-derive/2.3.1/download",
        type = "tar.gz",
        sha256 = "1557010476e0595c9b568d16dcfb81b93cdeb157612726f5170d31aa707bed27",
        strip_prefix = "parity-scale-codec-derive-2.3.1",
        build_file = Label("//cargo/remote:BUILD.parity-scale-codec-derive-2.3.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__parity_secp256k1__0_7_0",
        url = "https://crates.io/api/v1/crates/parity-secp256k1/0.7.0/download",
        type = "tar.gz",
        sha256 = "4fca4f82fccae37e8bbdaeb949a4a218a1bbc485d11598f193d2a908042e5fc1",
        strip_prefix = "parity-secp256k1-0.7.0",
        build_file = Label("//cargo/remote:BUILD.parity-secp256k1-0.7.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__percent_encoding__2_1_0",
        url = "https://crates.io/api/v1/crates/percent-encoding/2.1.0/download",
        type = "tar.gz",
        sha256 = "d4fd5641d01c8f18a23da7b6fe29298ff4b55afcccdf78973b24cf3175fee32e",
        strip_prefix = "percent-encoding-2.1.0",
        build_file = Label("//cargo/remote:BUILD.percent-encoding-2.1.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__pest__2_1_3",
        url = "https://crates.io/api/v1/crates/pest/2.1.3/download",
        type = "tar.gz",
        sha256 = "10f4872ae94d7b90ae48754df22fd42ad52ce740b8f370b03da4835417403e53",
        strip_prefix = "pest-2.1.3",
        build_file = Label("//cargo/remote:BUILD.pest-2.1.3.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__ppv_lite86__0_2_15",
        url = "https://crates.io/api/v1/crates/ppv-lite86/0.2.15/download",
        type = "tar.gz",
        sha256 = "ed0cfbc8191465bed66e1718596ee0b0b35d5ee1f41c5df2189d0fe8bde535ba",
        strip_prefix = "ppv-lite86-0.2.15",
        build_file = Label("//cargo/remote:BUILD.ppv-lite86-0.2.15.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__primitive_types__0_9_1",
        url = "https://crates.io/api/v1/crates/primitive-types/0.9.1/download",
        type = "tar.gz",
        sha256 = "06345ee39fbccfb06ab45f3a1a5798d9dafa04cb8921a76d227040003a234b0e",
        strip_prefix = "primitive-types-0.9.1",
        build_file = Label("//cargo/remote:BUILD.primitive-types-0.9.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__proc_macro_crate__0_1_5",
        url = "https://crates.io/api/v1/crates/proc-macro-crate/0.1.5/download",
        type = "tar.gz",
        sha256 = "1d6ea3c4595b96363c13943497db34af4460fb474a95c43f4446ad341b8c9785",
        strip_prefix = "proc-macro-crate-0.1.5",
        build_file = Label("//cargo/remote:BUILD.proc-macro-crate-0.1.5.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__proc_macro_crate__1_1_0",
        url = "https://crates.io/api/v1/crates/proc-macro-crate/1.1.0/download",
        type = "tar.gz",
        sha256 = "1ebace6889caf889b4d3f76becee12e90353f2b8c7d875534a71e5742f8f6f83",
        strip_prefix = "proc-macro-crate-1.1.0",
        build_file = Label("//cargo/remote:BUILD.proc-macro-crate-1.1.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__proc_macro2__1_0_32",
        url = "https://crates.io/api/v1/crates/proc-macro2/1.0.32/download",
        type = "tar.gz",
        sha256 = "ba508cc11742c0dc5c1659771673afbab7a0efab23aa17e854cbab0837ed0b43",
        strip_prefix = "proc-macro2-1.0.32",
        build_file = Label("//cargo/remote:BUILD.proc-macro2-1.0.32.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__quote__1_0_10",
        url = "https://crates.io/api/v1/crates/quote/1.0.10/download",
        type = "tar.gz",
        sha256 = "38bc8cc6a5f2e3655e0899c1b848643b2562f853f114bfec7be120678e3ace05",
        strip_prefix = "quote-1.0.10",
        build_file = Label("//cargo/remote:BUILD.quote-1.0.10.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__radium__0_6_2",
        url = "https://crates.io/api/v1/crates/radium/0.6.2/download",
        type = "tar.gz",
        sha256 = "643f8f41a8ebc4c5dc4515c82bb8abd397b527fc20fd681b7c011c2aee5d44fb",
        strip_prefix = "radium-0.6.2",
        build_file = Label("//cargo/remote:BUILD.radium-0.6.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__rand__0_7_3",
        url = "https://crates.io/api/v1/crates/rand/0.7.3/download",
        type = "tar.gz",
        sha256 = "6a6b1679d49b24bbfe0c803429aa1874472f50d9b363131f0e89fc356b544d03",
        strip_prefix = "rand-0.7.3",
        build_file = Label("//cargo/remote:BUILD.rand-0.7.3.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__rand__0_8_4",
        url = "https://crates.io/api/v1/crates/rand/0.8.4/download",
        type = "tar.gz",
        sha256 = "2e7573632e6454cf6b99d7aac4ccca54be06da05aca2ef7423d22d27d4d4bcd8",
        strip_prefix = "rand-0.8.4",
        build_file = Label("//cargo/remote:BUILD.rand-0.8.4.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__rand_chacha__0_2_2",
        url = "https://crates.io/api/v1/crates/rand_chacha/0.2.2/download",
        type = "tar.gz",
        sha256 = "f4c8ed856279c9737206bf725bf36935d8666ead7aa69b52be55af369d193402",
        strip_prefix = "rand_chacha-0.2.2",
        build_file = Label("//cargo/remote:BUILD.rand_chacha-0.2.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__rand_chacha__0_3_1",
        url = "https://crates.io/api/v1/crates/rand_chacha/0.3.1/download",
        type = "tar.gz",
        sha256 = "e6c10a63a0fa32252be49d21e7709d4d4baf8d231c2dbce1eaa8141b9b127d88",
        strip_prefix = "rand_chacha-0.3.1",
        build_file = Label("//cargo/remote:BUILD.rand_chacha-0.3.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__rand_core__0_5_1",
        url = "https://crates.io/api/v1/crates/rand_core/0.5.1/download",
        type = "tar.gz",
        sha256 = "90bde5296fc891b0cef12a6d03ddccc162ce7b2aff54160af9338f8d40df6d19",
        strip_prefix = "rand_core-0.5.1",
        build_file = Label("//cargo/remote:BUILD.rand_core-0.5.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__rand_core__0_6_3",
        url = "https://crates.io/api/v1/crates/rand_core/0.6.3/download",
        type = "tar.gz",
        sha256 = "d34f1408f55294453790c48b2f1ebbb1c5b4b7563eb1f418bcfcfdbb06ebb4e7",
        strip_prefix = "rand_core-0.6.3",
        build_file = Label("//cargo/remote:BUILD.rand_core-0.6.3.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__rand_hc__0_2_0",
        url = "https://crates.io/api/v1/crates/rand_hc/0.2.0/download",
        type = "tar.gz",
        sha256 = "ca3129af7b92a17112d59ad498c6f81eaf463253766b90396d39ea7a39d6613c",
        strip_prefix = "rand_hc-0.2.0",
        build_file = Label("//cargo/remote:BUILD.rand_hc-0.2.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__reed_solomon_erasure__4_0_2",
        url = "https://crates.io/api/v1/crates/reed-solomon-erasure/4.0.2/download",
        type = "tar.gz",
        sha256 = "a415a013dd7c5d4221382329a5a3482566da675737494935cbbbcdec04662f9d",
        strip_prefix = "reed-solomon-erasure-4.0.2",
        build_file = Label("//cargo/remote:BUILD.reed-solomon-erasure-4.0.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__regex__1_5_4",
        url = "https://crates.io/api/v1/crates/regex/1.5.4/download",
        type = "tar.gz",
        sha256 = "d07a8629359eb56f1e2fb1652bb04212c072a87ba68546a04065d525673ac461",
        strip_prefix = "regex-1.5.4",
        build_file = Label("//cargo/remote:BUILD.regex-1.5.4.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__regex_syntax__0_6_25",
        url = "https://crates.io/api/v1/crates/regex-syntax/0.6.25/download",
        type = "tar.gz",
        sha256 = "f497285884f3fcff424ffc933e56d7cbca511def0c9831a7f9b5f6153e3cc89b",
        strip_prefix = "regex-syntax-0.6.25",
        build_file = Label("//cargo/remote:BUILD.regex-syntax-0.6.25.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__ripemd160__0_9_1",
        url = "https://crates.io/api/v1/crates/ripemd160/0.9.1/download",
        type = "tar.gz",
        sha256 = "2eca4ecc81b7f313189bf73ce724400a07da2a6dac19588b03c8bd76a2dcc251",
        strip_prefix = "ripemd160-0.9.1",
        build_file = Label("//cargo/remote:BUILD.ripemd160-0.9.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__rlp__0_5_1",
        url = "https://crates.io/api/v1/crates/rlp/0.5.1/download",
        type = "tar.gz",
        sha256 = "999508abb0ae792aabed2460c45b89106d97fe4adac593bdaef433c2605847b5",
        strip_prefix = "rlp-0.5.1",
        build_file = Label("//cargo/remote:BUILD.rlp-0.5.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__rustc_hex__2_1_0",
        url = "https://crates.io/api/v1/crates/rustc-hex/2.1.0/download",
        type = "tar.gz",
        sha256 = "3e75f6a532d0fd9f7f13144f392b6ad56a32696bfcd9c78f797f16bbb6f072d6",
        strip_prefix = "rustc-hex-2.1.0",
        build_file = Label("//cargo/remote:BUILD.rustc-hex-2.1.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__rustc_version__0_3_3",
        url = "https://crates.io/api/v1/crates/rustc_version/0.3.3/download",
        type = "tar.gz",
        sha256 = "f0dfe2087c51c460008730de8b57e6a320782fbfb312e1f4d520e6c6fae155ee",
        strip_prefix = "rustc_version-0.3.3",
        build_file = Label("//cargo/remote:BUILD.rustc_version-0.3.3.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__ryu__1_0_5",
        url = "https://crates.io/api/v1/crates/ryu/1.0.5/download",
        type = "tar.gz",
        sha256 = "71d301d4193d031abdd79ff7e3dd721168a9572ef3fe51a1517aba235bd8f86e",
        strip_prefix = "ryu-1.0.5",
        build_file = Label("//cargo/remote:BUILD.ryu-1.0.5.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__semver__0_11_0",
        url = "https://crates.io/api/v1/crates/semver/0.11.0/download",
        type = "tar.gz",
        sha256 = "f301af10236f6df4160f7c3f04eec6dbc70ace82d23326abad5edee88801c6b6",
        strip_prefix = "semver-0.11.0",
        build_file = Label("//cargo/remote:BUILD.semver-0.11.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__semver_parser__0_10_2",
        url = "https://crates.io/api/v1/crates/semver-parser/0.10.2/download",
        type = "tar.gz",
        sha256 = "00b0bef5b7f9e0df16536d3961cfb6e84331c065b4066afb39768d0e319411f7",
        strip_prefix = "semver-parser-0.10.2",
        build_file = Label("//cargo/remote:BUILD.semver-parser-0.10.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__serde__1_0_118",
        url = "https://crates.io/api/v1/crates/serde/1.0.118/download",
        type = "tar.gz",
        sha256 = "06c64263859d87aa2eb554587e2d23183398d617427327cf2b3d0ed8c69e4800",
        strip_prefix = "serde-1.0.118",
        build_file = Label("//cargo/remote:BUILD.serde-1.0.118.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__serde_derive__1_0_118",
        url = "https://crates.io/api/v1/crates/serde_derive/1.0.118/download",
        type = "tar.gz",
        sha256 = "c84d3526699cd55261af4b941e4e725444df67aa4f9e6a3564f18030d12672df",
        strip_prefix = "serde_derive-1.0.118",
        build_file = Label("//cargo/remote:BUILD.serde_derive-1.0.118.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__serde_json__1_0_72",
        url = "https://crates.io/api/v1/crates/serde_json/1.0.72/download",
        type = "tar.gz",
        sha256 = "d0ffa0837f2dfa6fb90868c2b5468cad482e175f7dad97e7421951e663f2b527",
        strip_prefix = "serde_json-1.0.72",
        build_file = Label("//cargo/remote:BUILD.serde_json-1.0.72.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__sha2__0_9_8",
        url = "https://crates.io/api/v1/crates/sha2/0.9.8/download",
        type = "tar.gz",
        sha256 = "b69f9a4c9740d74c5baa3fd2e547f9525fa8088a8a958e0ca2409a514e33f5fa",
        strip_prefix = "sha2-0.9.8",
        build_file = Label("//cargo/remote:BUILD.sha2-0.9.8.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__sha3__0_9_1",
        url = "https://crates.io/api/v1/crates/sha3/0.9.1/download",
        type = "tar.gz",
        sha256 = "f81199417d4e5de3f04b1e871023acea7389672c4135918f05aa9cbf2f2fa809",
        strip_prefix = "sha3-0.9.1",
        build_file = Label("//cargo/remote:BUILD.sha3-0.9.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__signature__1_4_0",
        url = "https://crates.io/api/v1/crates/signature/1.4.0/download",
        type = "tar.gz",
        sha256 = "02658e48d89f2bec991f9a78e69cfa4c316f8d6a6c4ec12fae1aeb263d486788",
        strip_prefix = "signature-1.4.0",
        build_file = Label("//cargo/remote:BUILD.signature-1.4.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__smallvec__1_7_0",
        url = "https://crates.io/api/v1/crates/smallvec/1.7.0/download",
        type = "tar.gz",
        sha256 = "1ecab6c735a6bb4139c0caafd0cc3635748bbb3acf4550e8138122099251f309",
        strip_prefix = "smallvec-1.7.0",
        build_file = Label("//cargo/remote:BUILD.smallvec-1.7.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__smart_default__0_6_0",
        url = "https://crates.io/api/v1/crates/smart-default/0.6.0/download",
        type = "tar.gz",
        sha256 = "133659a15339456eeeb07572eb02a91c91e9815e9cbc89566944d2c8d3efdbf6",
        strip_prefix = "smart-default-0.6.0",
        build_file = Label("//cargo/remote:BUILD.smart-default-0.6.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__static_assertions__1_1_0",
        url = "https://crates.io/api/v1/crates/static_assertions/1.1.0/download",
        type = "tar.gz",
        sha256 = "a2eb9349b6444b326872e140eb1cf5e7c522154d69e7a0ffb0fb81c06b37543f",
        strip_prefix = "static_assertions-1.1.0",
        build_file = Label("//cargo/remote:BUILD.static_assertions-1.1.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__subtle__2_4_1",
        url = "https://crates.io/api/v1/crates/subtle/2.4.1/download",
        type = "tar.gz",
        sha256 = "6bdef32e8150c2a081110b42772ffe7d7c9032b606bc226c8260fd97e0976601",
        strip_prefix = "subtle-2.4.1",
        build_file = Label("//cargo/remote:BUILD.subtle-2.4.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__syn__1_0_57",
        url = "https://crates.io/api/v1/crates/syn/1.0.57/download",
        type = "tar.gz",
        sha256 = "4211ce9909eb971f111059df92c45640aad50a619cf55cd76476be803c4c68e6",
        strip_prefix = "syn-1.0.57",
        build_file = Label("//cargo/remote:BUILD.syn-1.0.57.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__synstructure__0_12_6",
        url = "https://crates.io/api/v1/crates/synstructure/0.12.6/download",
        type = "tar.gz",
        sha256 = "f36bdaa60a83aca3921b5259d5400cbf5e90fc51931376a9bd4a0eb79aa7210f",
        strip_prefix = "synstructure-0.12.6",
        build_file = Label("//cargo/remote:BUILD.synstructure-0.12.6.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__tap__1_0_1",
        url = "https://crates.io/api/v1/crates/tap/1.0.1/download",
        type = "tar.gz",
        sha256 = "55937e1799185b12863d447f42597ed69d9928686b8d88a1df17376a097d8369",
        strip_prefix = "tap-1.0.1",
        build_file = Label("//cargo/remote:BUILD.tap-1.0.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__thiserror__1_0_30",
        url = "https://crates.io/api/v1/crates/thiserror/1.0.30/download",
        type = "tar.gz",
        sha256 = "854babe52e4df1653706b98fcfc05843010039b406875930a70e4d9644e5c417",
        strip_prefix = "thiserror-1.0.30",
        build_file = Label("//cargo/remote:BUILD.thiserror-1.0.30.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__thiserror_impl__1_0_30",
        url = "https://crates.io/api/v1/crates/thiserror-impl/1.0.30/download",
        type = "tar.gz",
        sha256 = "aa32fd3f627f367fe16f893e2597ae3c05020f8bba2666a4e6ea73d377e5714b",
        strip_prefix = "thiserror-impl-1.0.30",
        build_file = Label("//cargo/remote:BUILD.thiserror-impl-1.0.30.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__time__0_1_44",
        url = "https://crates.io/api/v1/crates/time/0.1.44/download",
        type = "tar.gz",
        sha256 = "6db9e6914ab8b1ae1c260a4ae7a49b6c5611b40328a735b21862567685e73255",
        strip_prefix = "time-0.1.44",
        build_file = Label("//cargo/remote:BUILD.time-0.1.44.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__tiny_keccak__2_0_2",
        url = "https://crates.io/api/v1/crates/tiny-keccak/2.0.2/download",
        type = "tar.gz",
        sha256 = "2c9d3793400a45f954c52e73d068316d76b6f4e36977e3fcebb13a2721e80237",
        strip_prefix = "tiny-keccak-2.0.2",
        build_file = Label("//cargo/remote:BUILD.tiny-keccak-2.0.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__tinyvec__1_5_1",
        url = "https://crates.io/api/v1/crates/tinyvec/1.5.1/download",
        type = "tar.gz",
        sha256 = "2c1c1d5a42b6245520c249549ec267180beaffcc0615401ac8e31853d4b6d8d2",
        strip_prefix = "tinyvec-1.5.1",
        build_file = Label("//cargo/remote:BUILD.tinyvec-1.5.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__tinyvec_macros__0_1_0",
        url = "https://crates.io/api/v1/crates/tinyvec_macros/0.1.0/download",
        type = "tar.gz",
        sha256 = "cda74da7e1a664f795bb1f8a87ec406fb89a02522cf6e50620d016add6dbbf5c",
        strip_prefix = "tinyvec_macros-0.1.0",
        build_file = Label("//cargo/remote:BUILD.tinyvec_macros-0.1.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__toml__0_5_8",
        url = "https://crates.io/api/v1/crates/toml/0.5.8/download",
        type = "tar.gz",
        sha256 = "a31142970826733df8241ef35dc040ef98c679ab14d7c3e54d827099b3acecaa",
        strip_prefix = "toml-0.5.8",
        build_file = Label("//cargo/remote:BUILD.toml-0.5.8.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__typenum__1_14_0",
        url = "https://crates.io/api/v1/crates/typenum/1.14.0/download",
        type = "tar.gz",
        sha256 = "b63708a265f51345575b27fe43f9500ad611579e764c79edbc2037b1121959ec",
        strip_prefix = "typenum-1.14.0",
        build_file = Label("//cargo/remote:BUILD.typenum-1.14.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__ucd_trie__0_1_3",
        url = "https://crates.io/api/v1/crates/ucd-trie/0.1.3/download",
        type = "tar.gz",
        sha256 = "56dee185309b50d1f11bfedef0fe6d036842e3fb77413abef29f8f8d1c5d4c1c",
        strip_prefix = "ucd-trie-0.1.3",
        build_file = Label("//cargo/remote:BUILD.ucd-trie-0.1.3.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__uint__0_9_1",
        url = "https://crates.io/api/v1/crates/uint/0.9.1/download",
        type = "tar.gz",
        sha256 = "6470ab50f482bde894a037a57064480a246dbfdd5960bd65a44824693f08da5f",
        strip_prefix = "uint-0.9.1",
        build_file = Label("//cargo/remote:BUILD.uint-0.9.1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__unicode_bidi__0_3_7",
        url = "https://crates.io/api/v1/crates/unicode-bidi/0.3.7/download",
        type = "tar.gz",
        sha256 = "1a01404663e3db436ed2746d9fefef640d868edae3cceb81c3b8d5732fda678f",
        strip_prefix = "unicode-bidi-0.3.7",
        build_file = Label("//cargo/remote:BUILD.unicode-bidi-0.3.7.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__unicode_normalization__0_1_19",
        url = "https://crates.io/api/v1/crates/unicode-normalization/0.1.19/download",
        type = "tar.gz",
        sha256 = "d54590932941a9e9266f0832deed84ebe1bf2e4c9e4a3554d393d18f5e854bf9",
        strip_prefix = "unicode-normalization-0.1.19",
        build_file = Label("//cargo/remote:BUILD.unicode-normalization-0.1.19.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__unicode_xid__0_2_2",
        url = "https://crates.io/api/v1/crates/unicode-xid/0.2.2/download",
        type = "tar.gz",
        sha256 = "8ccb82d61f80a663efe1f787a51b16b5a51e3314d6ac365b08639f52387b33f3",
        strip_prefix = "unicode-xid-0.2.2",
        build_file = Label("//cargo/remote:BUILD.unicode-xid-0.2.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__url__2_2_2",
        url = "https://crates.io/api/v1/crates/url/2.2.2/download",
        type = "tar.gz",
        sha256 = "a507c383b2d33b5fc35d1861e77e6b383d158b2da5e14fe51b83dfedf6fd578c",
        strip_prefix = "url-2.2.2",
        build_file = Label("//cargo/remote:BUILD.url-2.2.2.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__validator__0_12_0",
        url = "https://crates.io/api/v1/crates/validator/0.12.0/download",
        type = "tar.gz",
        sha256 = "841d6937c33ec6039d8071bcf72933146b5bbe378d645d8fa59bdadabfc2a249",
        strip_prefix = "validator-0.12.0",
        build_file = Label("//cargo/remote:BUILD.validator-0.12.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__validator_types__0_12_0",
        url = "https://crates.io/api/v1/crates/validator_types/0.12.0/download",
        type = "tar.gz",
        sha256 = "ad9680608df133af2c1ddd5eaf1ddce91d60d61b6bc51494ef326458365a470a",
        strip_prefix = "validator_types-0.12.0",
        build_file = Label("//cargo/remote:BUILD.validator_types-0.12.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__version_check__0_9_3",
        url = "https://crates.io/api/v1/crates/version_check/0.9.3/download",
        type = "tar.gz",
        sha256 = "5fecdca9a5291cc2b8dcf7dc02453fee791a280f3743cb0905f8822ae463b3fe",
        strip_prefix = "version_check-0.9.3",
        build_file = Label("//cargo/remote:BUILD.version_check-0.9.3.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__wasi__0_10_0_wasi_snapshot_preview1",
        url = "https://crates.io/api/v1/crates/wasi/0.10.0+wasi-snapshot-preview1/download",
        type = "tar.gz",
        sha256 = "1a143597ca7c7793eff794def352d41792a93c481eb1042423ff7ff72ba2c31f",
        strip_prefix = "wasi-0.10.0+wasi-snapshot-preview1",
        build_file = Label("//cargo/remote:BUILD.wasi-0.10.0+wasi-snapshot-preview1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__wasi__0_9_0_wasi_snapshot_preview1",
        url = "https://crates.io/api/v1/crates/wasi/0.9.0+wasi-snapshot-preview1/download",
        type = "tar.gz",
        sha256 = "cccddf32554fecc6acb585f82a32a72e28b48f8c4c1883ddfeeeaa96f7d8e519",
        strip_prefix = "wasi-0.9.0+wasi-snapshot-preview1",
        build_file = Label("//cargo/remote:BUILD.wasi-0.9.0+wasi-snapshot-preview1.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__wee_alloc__0_4_5",
        url = "https://crates.io/api/v1/crates/wee_alloc/0.4.5/download",
        type = "tar.gz",
        sha256 = "dbb3b5a6b2bb17cb6ad44a2e68a43e8d2722c997da10e928665c72ec6c0a0b8e",
        strip_prefix = "wee_alloc-0.4.5",
        build_file = Label("//cargo/remote:BUILD.wee_alloc-0.4.5.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__winapi__0_3_9",
        url = "https://crates.io/api/v1/crates/winapi/0.3.9/download",
        type = "tar.gz",
        sha256 = "5c839a674fcd7a98952e593242ea400abe93992746761e38641405d28b00f419",
        strip_prefix = "winapi-0.3.9",
        build_file = Label("//cargo/remote:BUILD.winapi-0.3.9.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__winapi_i686_pc_windows_gnu__0_4_0",
        url = "https://crates.io/api/v1/crates/winapi-i686-pc-windows-gnu/0.4.0/download",
        type = "tar.gz",
        sha256 = "ac3b87c63620426dd9b991e5ce0329eff545bccbbb34f3be09ff6fb6ab51b7b6",
        strip_prefix = "winapi-i686-pc-windows-gnu-0.4.0",
        build_file = Label("//cargo/remote:BUILD.winapi-i686-pc-windows-gnu-0.4.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__winapi_x86_64_pc_windows_gnu__0_4_0",
        url = "https://crates.io/api/v1/crates/winapi-x86_64-pc-windows-gnu/0.4.0/download",
        type = "tar.gz",
        sha256 = "712e227841d057c1ee1cd2fb22fa7e5a5461ae8e48fa2ca79ec42cfc1931183f",
        strip_prefix = "winapi-x86_64-pc-windows-gnu-0.4.0",
        build_file = Label("//cargo/remote:BUILD.winapi-x86_64-pc-windows-gnu-0.4.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__wyz__0_2_0",
        url = "https://crates.io/api/v1/crates/wyz/0.2.0/download",
        type = "tar.gz",
        sha256 = "85e60b0d1b5f99db2556934e21937020776a5d31520bf169e851ac44e6420214",
        strip_prefix = "wyz-0.2.0",
        build_file = Label("//cargo/remote:BUILD.wyz-0.2.0.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__zeroize__1_4_3",
        url = "https://crates.io/api/v1/crates/zeroize/1.4.3/download",
        type = "tar.gz",
        sha256 = "d68d9dcec5f9b43a30d38c49f91dfedfaac384cb8f085faca366c26207dd1619",
        strip_prefix = "zeroize-1.4.3",
        build_file = Label("//cargo/remote:BUILD.zeroize-1.4.3.bazel"),
    )

    maybe(
        http_archive,
        name = "raze__zeroize_derive__1_2_2",
        url = "https://crates.io/api/v1/crates/zeroize_derive/1.2.2/download",
        type = "tar.gz",
        sha256 = "65f1a51723ec88c66d5d1fe80c841f17f63587d6691901d66be9bec6c3b51f73",
        strip_prefix = "zeroize_derive-1.2.2",
        build_file = Label("//cargo/remote:BUILD.zeroize_derive-1.2.2.bazel"),
    )

    maybe(
        new_git_repository,
        name = "raze__near_contract_standards__4_0_0_pre_4",
        remote = "https://github.com/HugoByte/near-sdk-rs.git",
        commit = "b18b4751c556e6da7dbbab678b3ec981f48a406d",
        build_file = Label("//cargo/remote:BUILD.near-contract-standards-4.0.0-pre.4.bazel"),
        init_submodules = True,
    )

