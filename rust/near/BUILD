load(
    "@io_bazel_rules_docker//container:container.bzl",
    "container_image",
)
load("@btp//utils:defs.bzl", "open_port")
load(":variables.bzl", "NEAR_MAINNET", "NEAR_LOCALNET", "NEAR_TESTNET")

package(default_visibility = ["//visibility:public"])

genrule(
    name = "near_config_dir",
    outs = ["near_config_dir.out"],
    srcs = select({
        "@near//:mainnet": ["@near//cli:configs/mainnet/master_key.json"],
        "@near//:testnet": ["@near//cli:configs/testnet/master_key.json"],
        "@near//:localnet": ["@near//cli:configs/localnet/master_key.json"],
        "@near//:docker_localnet": [
            "@near//cli:configs/localnet/config.json",
            "@near//cli:configs/localnet/genesis.json",
            "@near//cli:configs/localnet/master_key.json",
            "@near//cli:configs/localnet/node_key.json"
        ]
    }),
    cmd = "mkdir -p \"$$(cat $(location @btp//:tmp_dir))/near_config_dir/\"; cp -LR $(SRCS) $$(cat $(location @btp//:tmp_dir))/near_config_dir/;echo \"$$(cat $(location @btp//:tmp_dir))/near_config_dir/\" > $@",
    executable = True,
    local = True,
    tools = [
        "@btp//:tmp_dir",
    ],
)

container_image(
    name = "near_node_image",
    base = "@nearup_base//image",
    cmd = "/root/start.sh run localnet --num-nodes 1",
    entrypoint = None,
)

open_port(
    name = "near",
)

genrule(
    name = "deploy_node",
    outs = ["near_node"],
    cmd = """
            docker run -d -v $$(cat $(location :near_config_dir)):/root/.near -p $$(cat $(location :open_port_near)):3030 bazel:near_node_image
            cat $(location :open_port_near) > \"$@\" """,
    executable = True,
    tools = [
        ":near_config_dir",
        ":near_node_image",
        ":open_port_near",
    ],
)

genrule(
    name = "wait_until_near_up",
    outs = ["wait_until_near_up.out"],
    cmd = "$(location @btp//utils:wait_until_curl_script) http://localhost:$$(cat $(location :deploy_node)) status 300 > $@",
    executable = True,
    output_to_bindir = True,
    tags = [
        "no-cache",
    ],
    tools = [
        ":deploy_node",
        "@btp//utils:wait_until_curl_script",
    ],
)

genrule(
    name = "btp_address",
    srcs = ["@near//cli:deploy_bmc"],
    outs = ["btp_address.out"],
    cmd = "echo \"btp://$$(cat $(location @near//:network_address))/$$(cat $(location @near//cli:encode_public_key_bmc))\" > $@",
    executable = True,
    local = True,
    tools = [
        "@near//:network_address",
        "@near//:node_url",
        "@near//cli:encode_public_key_bmc",
    ],
)

genrule(
    name = "network_address",
    outs = ["network_address.out"],
    cmd = "echo \"0x1.near\" > $@",
    executable = True,
    local = True,
)

genrule(
    name = "bmr_config_dir",
    outs = ["bmr_config_dir.out"],
    cmd = "mkdir -p \"$$(cat $(location @btp//:tmp_dir))/near_bmr_config_dir\"  && echo \"$$(cat $(location @btp//:tmp_dir))/near_bmr_config_dir\" > $@",
    executable = True,
    local = True,
    output_to_bindir = True,
    tools = [
        "@btp//:tmp_dir",
    ],
)

genrule(
    name = "endpoint_docker",
    outs = ["endpoint.out"],
    cmd = "echo \"" + select({
        "//conditions:default": "$$(cat $(location :node_url))",
        ":docker_localnet": "http://172.17.0.1:$$(cat $(location :deploy_node))",
    }) + "\" > $@",
    executable = True,
    local = True,
    tools = select({
        ":docker_localnet": [":deploy_node"],
        "//conditions:default": [":node_url"],
    }),
)

genrule(
    name = "latest_block_height",
    outs = ["latest_block_height.out"],
    cmd = "curl $$(cat $(location :node_url))/status | jq .sync_info.latest_block_height >$@",
    executable = True,
    local = True,
    tools = [
        "@near//:node_url",
        "@near//cli:near_binary",
    ],
)

config_setting(
    name = "testnet",
    define_values = {"near_network": "testnet"},
)

config_setting(
    name = "localnet",
    define_values = {"near_network": "localnet"},
)

config_setting(
    name = "mainnet",
    define_values = {"near_network": "mainnet"},
)

config_setting(
    name = "docker_localnet",
    define_values = {
        "near_network": "localnet",
        "docker": "true",
    },
)

genrule(
    name = "node_url",
    srcs = select({
        ":docker_localnet": ["@near//:wait_until_near_up"],
        "//conditions:default": [],
    }),
    outs = ["node_url.out"],
    cmd = "echo " + select({
        ":mainnet": NEAR_MAINNET,
        ":testnet": NEAR_TESTNET,
        ":localnet": NEAR_LOCALNET,
        ":docker_localnet": "$$(cat $(locations @near//:wait_until_near_up))",
    }) + " > $@",
    executable = True,
    local = True,
)
