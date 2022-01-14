load("@rules_rust//rust:defs.bzl", "rust_shared_library")
load("@rust//:wasm_generator.bzl", "rust_wasm_generator")

package(default_visibility = ["//visibility:public"])

rust_shared_library(
    name = "token_bsh",
    srcs = [
        "src/accounting.rs",
        "src/assertion.rs",
        "src/estimate.rs",
        "src/external/bmc.rs",
        "src/external/fungible_token.rs",
        "src/external/mod.rs",
        "src/external/receiver.rs",
        "src/external/token_bsh.rs",
        "src/fee_management.rs",
        "src/lib.rs",
        "src/messaging.rs",
        "src/multi_token.rs",
        "src/owner_management.rs",
        "src/token_management.rs",
        "src/transfer.rs",
        "src/types/mod.rs",
        "src/types/registered_token.rs",
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
