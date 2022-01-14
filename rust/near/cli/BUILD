load("@build_bazel_rules_nodejs//:index.bzl", "nodejs_binary", "npm_package_bin")

package(default_visibility = ["//visibility:public"])

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

genrule(
    name = "create_bmc_account",
    outs = ["create_bmc_account.out"],
    cmd = "$(execpath :near_binary) create-account bmc.node0 --masterAccount node0 --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --keyPath ~/.near/localnet/node0/validator_key.json > $@",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "deploy_bmc",
    srcs = [
        "@near//bmc:bmc_wasm",
    ],
    outs = ["deploy_bmc.out"],
    cmd = "$(execpath :near_binary) deploy bmc.node0 --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --wasmFile $(locations @near//bmc:bmc_wasm) --accountId bmc.node0 --initFunction \"new\" --initArgs '{\"network\": \"0x1.near\", \"block_interval\": 1500}' > $@",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:create_bmc_account",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "create_icon_bmv_account",
    outs = ["create_icon_bmv_account.out"],
    cmd = "$(execpath :near_binary) create-account icon_bmv.node0 --masterAccount node0 --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --keyPath ~/.near/localnet/node0/validator_key.json > $@",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "create_nativecoin_bsh_account",
    outs = ["create_nativecoin_bsh_account.out"],
    cmd = "$(execpath :near_binary) create-account nativecoin_bsh.node0 --masterAccount node0 --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --keyPath ~/.near/localnet/node0/validator_key.json > $@",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "create_token_bsh_account",
    outs = ["create_token_bsh_account.out"],
    cmd = "$(execpath :near_binary) create-account token_bsh.node0 --masterAccount node0 --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --keyPath ~/.near/localnet/node0/validator_key.json > $@",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:near_binary",
    ],
)

genrule(
    name = "deploy_icon_bmv",
    srcs = [
        "@near//bmv/icon:bmv_icon_wasm",
    ],
    outs = ["deploy_icon_bmv.out"],
    cmd = "$(execpath :near_binary) deploy icon_bmv.node0 --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --wasmFile $(locations @near//bmv/icon:bmv_icon_wasm) --accountId icon_bmv.node0 --initFunction \"new\" --initArgs '{\"network\": \"0x1.near\", \"block_interval\": 1500}' > $@",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:create_icon_bmv_account",
        "@near//cli:near_binary",
    ],
)

# nodejs_binary(
#     name = "deploy_bmv",
#     data = [
#         "@near//:wait_until_near_up",
#         "@near//bmv:bmv_wasm",
#         "@near//cli:create_bmv_account",
#         "@near_npm//near-cli",
#     ],
# )

# nodejs_binary(
#     name = "deploy_coin_bsh",
#     data = [
#         "@near//:wait_until_near_up",
#         "@near//bsh/coin_bsh:coin_bsh_wasm",
#         "@near//cli:create_coin_bsh_account",
#         "@near_npm//near-cli",
#     ],
# )

# nodejs_binary(
#     name = "deploy_token_bsh",
#     data = [
#         "@near//:wait_until_near_up",
#         "@near//bsh/token_bsh:token_bsh_wasm",
#         "@near//cli:create_token_bsh_account",
#         "@near_npm//near-cli",
#     ],
# )
