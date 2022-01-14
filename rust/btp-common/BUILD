load("@rules_rust//rust:defs.bzl", "rust_library")

package(default_visibility = ["//visibility:public"])

rust_library(
    name = "btp_common",
    srcs = [
        "src/btp_address.rs",
        "src/error.rs",
        "src/lib.rs"
    ],
    deps = [
        "@rust//btp-common/cargo:serde",
    ],
)