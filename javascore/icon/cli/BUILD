load("@io_bazel_rules_go//go:def.bzl")

package(default_visibility = ["//visibility:public"])


genrule(
    name = "create_bmc_account",
    outs = ["create_bmc_account.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) --help > $@",
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop:goloop",
    ],
)