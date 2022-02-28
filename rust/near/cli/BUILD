load("@build_bazel_rules_nodejs//:index.bzl", "nodejs_binary", "npm_package_bin")
load("@near//cli:defs.bzl", "create_account", "create_sub_account")
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

create_sub_account(
    name = "nativecoinbsh",
)

create_sub_account(
    name = "tokenbsh",
)

create_account(
    name = "iconbmv",
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
    cmd = """
    $(execpath :near_binary) deploy $$(cat $(location @near//cli:encode_public_key_iconbmv))  --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --wasmFile $(locations @near//bmv/icon:bmv_icon_wasm) --accountId $$(cat $(location @near//cli:encode_public_key_iconbmv)) --initFunction \"new\" --initArgs \\'\\{\\"bmc\\":\\"$$(cat $(location @near//cli:encode_public_key_bmc))\\"\\,\\"network\\":\\"$$(cat $(location @icon//:wait_for_channel_up)).icon\\"\\,\\"validators\\":[]\\,\\"offset\\":\\"10\\"\\}\\' > $@""",
    executable = True,
    local = True,
    tools = [
        "@icon//:wait_for_channel_up",
        "@near//:wait_until_near_up",
        "@near//cli:create_account_iconbmv",
        "@near//cli:encode_public_key_bmc",
        "@near//cli:encode_public_key_iconbmv",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "deploy_native_coin_bsh",
    srcs = [
        "@near//bsh/nativecoin:native_coin_wasm",
    ],
    outs = ["deploy_natvie_coin_bsh.out"],
    cmd = """$(execpath :near_binary) deploy $$(cat $(location @near//cli:create_sub_account_nativecoinbsh)) --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --wasmFile $(locations @near//bsh/nativecoin:native_coin_wasm) --accountId $$(cat $(location @near//cli:create_sub_account_nativecoinbsh)) --initFunction \"new\" --initArgs  \\'\\{\\"service_name\\":\\"nativecoin\\"\\,\\"bmc\\":\\"$$(cat $(location @near//cli:encode_public_key_bmc))\\"\\,\\"network\\":\\"0x1.near\\"\\,\\"fee_numerator\\":\\"1000\\"\\,\\"native_coin\\":\\{\\"metadata\\":\\{\\"name\\":\\"NEAR\\"\\,\\"symbol\\":\\"NEAR\\"\\,\\"uri\\":null\\,\\"network\\":\\"0x1.near\\"\\,\\"extras\\":null\\}\\}\\}\\' > $@""",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:create_account_bmc",
        "@near//cli:create_sub_account_nativecoinbsh",
        "@near//cli:encode_public_key_bmc",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "deploy_token_bsh",
    srcs = [
        "@near//bsh/token:token_bsh_wasm",
    ],
    outs = ["deploy_token_bsh.out"],
    cmd = """$(execpath :near_binary) deploy $$(cat $(location @near//cli:encode_public_key_tokenbsh)) --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --wasmFile $(locations @near//bsh/token:token_bsh_wasm) --accountId tokenbsh.node0 --initFunction \"new\" --initArgs \\'\\{\\"service_name\\":\\"tokenbsh\\"\\,\\"bmc\\":\\"$$(cat $(location @near//cli:encode_public_key_bmc))\\"\\,\\"network\\":\\"0x1.near\\"\\,\\"fee_numerator\\":\\"1000\\"\\}\\' > $@""",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:create_account_tokenbsh",
        "@near//cli:encode_public_key_bmc",
        "@near//cli:encode_public_key_tokenbsh",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "add_icon_verifier",
    srcs = ["@near//cli:deploy_icon_bmv"],
    outs = ["add_icon_verifier.out"],
    cmd = """$(execpath :near_binary) call $$(cat $(location @near//cli:encode_public_key_bmc)) add_verifier \\'\\{\\"network\\":\\"$$(cat $(location @icon//:wait_for_channel_up)).icon\\"\\,\\"verifier\\":\\"$$(cat $(location @near//cli:encode_public_key_iconbmv))\\"\\}\\' --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --accountId $$(cat $(location @near//cli:encode_public_key_bmc)) > $@""",
    executable = True,
    local = True,
    tools = [
        "@icon//:wait_for_channel_up",
        "@near//:wait_until_near_up",
        "@near//cli:encode_public_key_bmc",
        "@near//cli:encode_public_key_iconbmv",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "add_link_icon",
    srcs = [
        "@near//cli:deploy_bmc",
        "@near//cli:add_icon_verifier",
    ],
    outs = ["add_link_icon.out"],
    cmd = """$(execpath :near_binary) call $$(cat $(location @near//cli:encode_public_key_bmc)) add_link \\'\\{\\"link\\":\\"$$(cat $(location @icon//:btp_address))\\"\\}\\' --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --accountId $$(cat $(location @near//cli:encode_public_key_bmc)) > $@""",
    executable = True,
    local = True,
    tools = [
        "@icon//:btp_address",
        "@near//:wait_until_near_up",
        "@near//cli:encode_public_key_bmc",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "set_link_icon",
    srcs = [
        "@near//cli:deploy_bmc",
        "@near//cli:add_icon_verifier",
        "@near//cli:add_link_icon",
    ],
    outs = ["set_link_icon.out"],
    cmd = """$(execpath :near_binary) call $$(cat $(location @near//cli:encode_public_key_bmc)) set_link \\'\\{\\"link\\":\\"$$(cat $(location @icon//:btp_address))\\"\\,\\"block_interval\\":6000\\,\\"max_aggregation\\":8\\,\\"delay_limit\\":100\\}\\' --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --accountId $$(cat $(location @near//cli:encode_public_key_bmc)) > $@""",
    executable = True,
    local = True,
    tools = [
        "@icon//:btp_address",
        "@near//:wait_until_near_up",
        "@near//cli:encode_public_key_bmc",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "add_service_bmc_near",
    srcs = [
        "@near//cli:deploy_bmc",
        "@near//cli:add_icon_verifier",
        "@near//cli:add_link_icon",
        "@near//cli:set_link_icon",
        "@near//cli:deploy_native_coin_bsh",
    ],
    outs = ["add_service_bmc_near.out"],
    cmd = """$(execpath :near_binary) call $$(cat $(location @near//cli:encode_public_key_bmc)) add_service \\'\\{\\"name\\":\\"nativecoin\\"\\,\\"service\\":\\"$$(cat $(location @near//cli:encode_public_key_nativecoinbsh))\\"\\}\\' --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --accountId $$(cat $(location @near//cli:encode_public_key_bmc)) > $@ """,
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:encode_public_key_bmc",
        "@near//cli:encode_public_key_nativecoinbsh",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "register_wrapped_coin_native_coin_bsh",
    srcs = ["@near//cli:deploy_native_coin_bsh"],
    outs = ["regsiter_new_wrapped_coin.out"],
    cmd = """$(execpath :near_binary) call $$(cat $(location @near//cli:create_sub_account_nativecoinbsh)) register \\'\\{\\"coin\\":\\{\\"metadata\\":\\{\\"name\\":\\"WrappedICX\\"\\,\\"symbol\\":\\"wICX\\",\\"uri\\":\\"wicx.$$(cat $(location @near//cli:create_sub_account_nativecoinbsh))\\"\\,\\"network\\":\\"0x1.icon\\"\\,\\"extras\\":\\{\\"spec\\":\\"ft-1.0.0\\",\\"icon\\":null,\\"reference\\":null,\\"reference_hash\\":null,\\"decimals\\":24\\}\\}\\}\\}\\' --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --accountId $$(cat $(location @near//cli:create_sub_account_nativecoinbsh)) --amount 10 --gas 300000000000000 >$@""",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:create_sub_account_nativecoinbsh",
        "@near//cli:near_binary",
    ],
)
