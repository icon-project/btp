load("@rules_rust//rust:defs.bzl", "rust_shared_library")
load("@rust//:wasm_generator.bzl", "rust_wasm_generator")

package(default_visibility = ["//visibility:public"])

rust_shared_library(
    name = "bmc",
    srcs = [
        "src/assertion.rs",
        "src/estimate.rs",
        "src/external/bmc.rs",
        "src/external/bmv.rs",
        "src/external/bsh.rs",
        "src/external/mod.rs",
        "src/internal_service.rs",
        "src/lib.rs",
        "src/link_management.rs",
        "src/messaging.rs",
        "src/owner_management.rs",
        "src/relay_management.rs",
        "src/route_management.rs",
        "src/service_management.rs",
        "src/utils.rs",
        "src/verifier_management.rs",
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
        "@rust//near/libraries",
        "@rust//near/libraries/cargo:near_sdk",
    ],
)

rust_wasm_generator(
    name = "bmc_wasm",
    wasm_file = ":bmc",
)
