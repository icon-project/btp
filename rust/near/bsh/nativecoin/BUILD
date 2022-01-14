load("@rules_rust//rust:defs.bzl", "rust_shared_library")
load("@rust//:wasm_generator.bzl", "rust_wasm_generator")

package(default_visibility = ["//visibility:public"])

rust_shared_library(
    name = "bmv_icon",
    srcs = [
        "src/accounting.rs",
        "src/assertion.rs",
        "src/coin_management.rs",
        "src/estimate.rs",
        "src/external/bmc.rs",
        "src/external/mod.rs",
        "src/external/native_coin.rs",
        "src/external/receiver.rs",
        "src/fee_management.rs",
        "src/lib.rs",
        "src/messaging.rs",
        "src/multi_token.rs",
        "src/owner_management.rs",
        "src/transfer.rs",
        "src/util.rs",
    ],
    rustc_flags = [
        "--codegen=link-arg=-s",
        "--crate-type=cdylib",
        "--codegen=lto=fat",
        "--codegen=opt-level=z",
        "--codegen=panic=abort",
        "--codegen=overflow-checks=on",
        "--codegen=debuginfo=0",
    ],
    deps = [
        "@rules_rust//wasm_bindgen/raze:wasm_bindgen",
        "@rust//btp-common:btp_common",
        "@rust//near/bmv/icon/cargo:hex",
        "@rust//near/bmv/icon/cargo:near_sdk",
        "@rust//near/bmv/icon/cargo:tiny_keccak",
        "@rust//near/libraries",
    ],
)

rust_wasm_generator(
    name = "bmv_icon_wasm",
    wasm_file = ":bmv_icon",
)
