load("@rules_rust//rust:defs.bzl", "rust_shared_library")
load("@rust//:wasm_generator.bzl", "rust_wasm_generator")

package(default_visibility = ["//visibility:public"])

rust_shared_library(
    name = "native_coin",
    srcs = [
        "src/external/bmc.rs",
        "src/external/mod.rs",
        "src/external/native_coin.rs",
        "src/external/nep141.rs",
        "src/types/mod.rs",
        "src/types/registered_coins.rs",
        "src/accounting.rs",
        "src/assertion.rs",
        "src/coin_management.rs",
        "src/estimate.rs",
        "src/fee_management.rs",
        "src/lib.rs",
        "src/messaging.rs",
        "src/owner_management.rs",
        "src/transfer.rs",
        "src/util.rs",
    ],
    data = [
        "res/NEP141_CONTRACT.wasm"
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
        "@rust//near/bsh/nativecoin/cargo:near_sdk",
        "@rust//near/libraries",
        "@rust//near/bsh/nativecoin/cargo:near_contract_standards",
    ],
)

rust_wasm_generator(
    name = "native_coin_wasm",
    wasm_file = ":native_coin",
)
