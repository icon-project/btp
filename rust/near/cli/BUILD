load("@build_bazel_rules_nodejs//:index.bzl", "nodejs_binary", "npm_package_bin")
load("@near//cli:defs.bzl", "create_account")
load("@rules_python//python:defs.bzl", "py_binary")
load("@near_cli_util_python_deps//:requirements.bzl", "requirement")

package(default_visibility = ["//visibility:public"])

py_binary(
    name = "encode_base58",
    srcs = ["encode_base58.py"],
    deps = [
        requirement("base58"),
    ],
)

nodejs_binary(
    name = "near_binary",
    data = [
        "@near_npm//near-cli",
    ],
    entry_point = "@near_npm//:node_modules/near-cli/bin/near",
    env = {
        "NODE_ENV": "local",
    },
)

create_account(
    name = "bmc",
)

create_account(
    name = "nativecoinbsh",
)

create_account(
    name = "tokenbsh",
)

create_account(
    name= "iconbmv",
)

genrule(
    name = "deploy_bmc",
    srcs = [
        "@near//bmc:bmc_wasm",
    ],
    outs = ["deploy_bmc.out"],
    cmd = "$(execpath :near_binary) deploy $$(cat $(location @near//cli:encode_public_key_bmc)) --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --wasmFile $(locations @near//bmc:bmc_wasm) --accountId bmc.node0 --initFunction \"new\" --initArgs '{\"network\": \"0x1.near\", \"block_interval\": 1500}' > $@",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:create_account_bmc",
        "@near//cli:encode_public_key_bmc",
        "@near//cli:near_binary",
    ],
)


genrule(
    name = "deploy_icon_bmv",
    srcs = [
        "@near//bmv/icon:bmv_icon_wasm",
    ],
    outs = ["deploy_icon_bmv.out"],
    cmd = "$(execpath :near_binary) deploy iconbmv.node0 --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --wasmFile $(locations @near//bmv/icon:bmv_icon_wasm) --accountId iconbmv.node0 --initFunction \"new\" --initArgs '{\"network\": \"0x1.near\", \"block_interval\": 1500}' > $@",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:create_account_iconbmv",
        "@near//cli:encode_public_iconbmv",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "deploy_native_coin_bsh",
    srcs = [
        "@near//bsh/nativecoin:native_coin_wasm",
    ],
    outs = ["deploy_natvie_coin_bsh.out"],
    cmd = "$(execpath :near_binary) deploy $$(cat $(location @near//cli:encode_public_key_nativecoinbsh)) --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --wasmFile $(locations @near//bsh/nativecoin:native_coin_wasm) --accountId nativecoinbsh.node0 --initFunction \"new\" --initArgs '{\"service_name\":\"nativecoin\",\"bmc\":\"bmc.node0\",\"network\":\"0x1.near\",\"native_coin\":{\"metadata\":{\"name\":\"NEAR\",\"symbol\":\"NEAR\",\"uri\":null,\"network\":\"\",\"extras\":null}},\"fee_numerator\":\"1000\"}' > $@",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:create_account_nativecoinbsh",
        "@near//cli:encode_public_key_nativecoinbsh",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "deploy_token_bsh",
    srcs = [
        "@near//bsh/token:token_bsh_wasm",
    ],
    outs = ["deploy_token_bsh.out"],
    cmd = "$(execpath :near_binary) deploy $$(cat $(location @near//cli:encode_public_key_tokenbsh)) --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --wasmFile $(locations @near//bsh/token:token_bsh_wasm) --accountId tokenbsh.node0 --initFunction \"new\" --initArgs '{\"service_name\":\"nativecoin\",\"bmc\":\"bmc.node0\",\"network\":\"0x1.near\",\"fee_numerator\":\"1000\"}' > $@",
    executable = True,
    local = True,
    tools = [
         "@near//:wait_until_near_up",
        "@near//cli:create_account_tokenbsh",
        "@near//cli:encode_public_key_tokenbsh",
        "@near//cli:near_binary",
    ],
)
