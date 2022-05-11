load("@io_bazel_rules_go//go:def.bzl", "go_library", "go_test")

go_library(
    name = "chain",
    srcs = [
        "adapter.go",
        "chain.go",
        "config.go",
    ],
    importpath = "github.com/icon-project/btp/cmd/btpsimple/chain",
    visibility = ["//visibility:public"],
    deps = [
        "//cmd/btpsimple/module/base",
        "//cmd/btpsimple/module/bsc",
        "//cmd/btpsimple/module/icon",
        "//cmd/btpsimple/module/registry",
        "//common/codec",
        "//common/config",
        "//common/db",
        "//common/errors",
        "//common/log",
        "//common/mta",
        "//common/wallet",
        "@com_github_stretchr_testify//mock",
    ],
)

go_test(
    name = "chain_test",
    srcs = ["type_test.go"],
    embed = [":chain"],
    deps = ["@com_github_stretchr_testify//assert"],
)
