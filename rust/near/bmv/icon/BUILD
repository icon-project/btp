load("@rules_rust//rust:defs.bzl", "rust_shared_library")
load("@rust//:wasm_generator.bzl", "rust_wasm_generator")

package(default_visibility = ["//visibility:public"])

rust_shared_library(
    name = "bmv_icon",
    srcs = [
        "src/assertion.rs",
        "src/block_validation.rs",
        "src/lib.rs",
        "src/management.rs",
        "src/messaging.rs",
        "src/receipt_validation.rs",
        "src/types/block_header.rs",
        "src/types/block_proof.rs",
        "src/types/block_result.rs",
        "src/types/block_update.rs",
        "src/types/block_witness.rs",
        "src/types/bmv_error.rs",
        "src/types/event_log.rs",
        "src/types/event_proof.rs",
        "src/types/extension_data.rs",
        "src/types/mod.rs",
        "src/types/nullable.rs",
        "src/types/part_set_id.rs",
        "src/types/proof.rs",
        "src/types/receipt.rs",
        "src/types/receipt_proof.rs",
        "src/types/relay_message.rs",
        "src/types/sha3_fips202.rs",
        "src/types/state_change.rs",
        "src/types/validator.rs",
        "src/types/vote.rs",
        "src/types/vote_message.rs",
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
        "@rust//near/bmv/icon/cargo:near_sdk",
        "@rules_rust//wasm_bindgen/raze:wasm_bindgen",
        "@rust//btp-common:btp_common",
        "@rust//near/bmv/icon/cargo:hex",
        "@rust//near/bmv/icon/cargo:tiny_keccak",
        "@rust//near/libraries",
    ],
)

rust_wasm_generator(
    name = "bmv_icon_wasm",
    wasm_file = ":bmv_icon",
)
