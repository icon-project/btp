load("@build_bazel_rules_nodejs//:index.bzl", "nodejs_binary", "npm_package_bin")
load("@near//cli:defs.bzl", "configure_bmr", "configure_link", "create_account", "create_sub_account")
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
    cmd = "$(execpath :near_binary) deploy $$(cat $(location @near//cli:encode_public_key_bmc)) --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --wasmFile $(locations @near//bmc:bmc_wasm) --accountId $$(cat $(location @near//cli:encode_public_key_bmc)) --initFunction \"new\" --initArgs '{\"network\": \"0x1.near\", \"block_interval\": 1500}' > $@",
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
        "@icon//cli:get_icon_validators",
        "@icon//:latest_block_height",
    ],
    outs = ["deploy_icon_bmv.out"],
    cmd = """
    $(execpath :near_binary) deploy $$(cat $(location @near//cli:encode_public_key_iconbmv))  --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --wasmFile $(locations @near//bmv/icon:bmv_icon_wasm) --accountId $$(cat $(location @near//cli:encode_public_key_iconbmv)) --initFunction \"new\" --initArgs \\'\\{\\"bmc\\":\\"$$(cat $(location @near//cli:encode_public_key_bmc))\\"\\,\\"network\\":\\"$$(cat $(location @icon//:wait_for_channel_up)).icon\\"\\,\\"validators\\":\\"$$(cat $(location @icon//cli:get_icon_validators))\\"\\,\\"offset\\":\\"$$(($$(cat $(location  @icon//:latest_block_height))-1))\\"\\}\\' > $@""",
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
    cmd = """$(execpath :near_binary) deploy $$(cat $(location @near//cli:encode_public_key_tokenbsh)) --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --wasmFile $(locations @near//bsh/token:token_bsh_wasm) --accountId $$(cat $(location @near//cli:create_sub_account_tokenbsh)) --initFunction \"new\" --initArgs \\'\\{\\"service_name\\":\\"tokenbsh\\"\\,\\"bmc\\":\\"$$(cat $(location @near//cli:encode_public_key_bmc))\\"\\,\\"network\\":\\"0x1.near\\"\\,\\"fee_numerator\\":\\"1000\\"\\}\\' > $@""",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:create_account_tokenbsh",
        "@near//cli:create_sub_account_tokenbsh",
        "@near//cli:encode_public_key_bmc",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "set_icon_link",
    srcs = [
        "@near//cli:deploy_bmc",
        "@near//cli:add_icon_verifier",
        "@near//cli:add_icon_link",
        ":register_wrapped_coin_native_coin_bsh",
    ],
    outs = ["set_icon_link.out"],
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
        "@near//cli:deploy_native_coin_bsh",
    ],
    outs = ["add_service_bmc_near.out"],
    cmd = """$(execpath :near_binary) call $$(cat $(location @near//cli:encode_public_key_bmc)) add_service \\'\\{\\"name\\":\\"nativecoin\\"\\,\\"service\\":\\"$$(cat $(location @near//cli:create_sub_account_nativecoinbsh))\\"\\}\\' --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --accountId $$(cat $(location @near//cli:encode_public_key_bmc)) > $@ """,
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:create_sub_account_nativecoinbsh",
        "@near//cli:encode_public_key_bmc",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "register_wrapped_coin_native_coin_bsh",
    srcs = [
        "@near//cli:add_service_bmc_near",
        "@icon//:network_address",
    ],
    outs = ["regsiter_new_wrapped_coin.out"],
    cmd = """$(execpath :near_binary) call $$(cat $(location @near//cli:create_sub_account_nativecoinbsh)) register \\'\\{\\"coin\\":\\{\\"metadata\\":\\{\\"name\\":\\"ICX\\"\\,\\"symbol\\":\\"icx\\",\\"uri\\":\\"wicx.$$(cat $(location @near//cli:create_sub_account_nativecoinbsh))\\"\\,\\"network\\":\\"$$(cat $(location @icon//:network_address))\\"\\,\\"extras\\":\\{\\"spec\\":\\"ft-1.0.0\\",\\"icon\\":null,\\"reference\\":null,\\"reference_hash\\":null,\\"decimals\\":24\\}\\}\\}\\}\\' --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --accountId $$(cat $(location @near//cli:create_sub_account_nativecoinbsh)) --amount 10 --gas 300000000000000 >$@""",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:create_sub_account_nativecoinbsh",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "add_icon_bmr",
    srcs = [
        "@near//cli:set_icon_link",
        "@near//cli:get_wallet_near_keystore",
    ],
    outs = ["add_icon_bmr.out"],
    cmd = """
    $(execpath :near_binary) call $$(cat $(location @near//cli:encode_public_key_bmc)) add_relay \\'\\{\\"link\\":\\"$$(cat $(location @icon//:btp_address))\\"\\,\\"relay\\":\\"$$(cat $(location @near//cli:get_wallet_near_keystore))\\"\\}\\' --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --accountId $$(cat $(location @near//cli:encode_public_key_bmc)) > $@ 
    """,
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
    name = "generate_keystore",
    srcs = [
        "@com_github_hugobyte_keygen//:keygen",
        ":keysecret",
    ],
    outs = ["keystore.json"],
    cmd = "$(execpath @com_github_hugobyte_keygen//:keygen) generate -p $$(cat $(location :keysecret)) -o $@",
    executable = True,
    local = True,
)

genrule(
    name = "keysecret",
    outs = [
        "keysecret.out",
    ],
    cmd = "echo 'btpsimple' > $@",
    executable = True,
    local = True,
)

configure_link(
    name = "icon",
)

configure_bmr(
    name = "near",
)
