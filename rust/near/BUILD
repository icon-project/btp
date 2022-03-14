load(
    "@io_bazel_rules_docker//container:container.bzl",
    "container_image",
)
load("@btp//utils:defs.bzl", "open_port")

package(default_visibility = ["//visibility:public"])

genrule(
    name = "near_config_dir",
    outs = ["near_config_dir.out"],
    cmd = "mkdir -p \"$$(cat $(location @btp//:tmp_dir))/near_config_dir/\"; cp -LR $$PWD/rust/near/cli/configs/"+ select({"@near//:localnet":"localnet","@near//:testnet": "testnet"}) +"/* -t $$(cat $(location @btp//:tmp_dir))/near_config_dir/;echo \"$$(cat $(location @btp//:tmp_dir))/near_config_dir/\" > $@",
    tools = [
        "@btp//:tmp_dir"
    ],
    executable = True,
    local = True,
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
        "@btp//utils:wait_until_curl_script",
        ":deploy_node"
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
    output_to_bindir = True,
    executable = True,
    local = True,
    tools = [
        "@btp//:tmp_dir"
    ],
)

genrule(
    name = "endpoint_docker",
    outs = ["endpoint.out"],
    cmd = "echo \"$$(cat $(location :node_url))\" > $@",
    executable = True,
    local = True,
    tools = [
        ":node_url"
    ],
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
    define_values = {"network": "testnet"},
)

config_setting(
    name = "localnet",
    define_values = {"network": "localnet"},
)

genrule(
    name = "node_url",

    outs = ["node_url.out"],
    srcs = select({
        ":localnet": ["@near//:wait_until_near_up"],
        ":testnet": []
    }),
    cmd = "echo " + select({
        ":testnet":"https://rpc.testnet.near.org",
        ":localnet":"$$(cat $(locations @near//:wait_until_near_up))"
    }) + " > $@",
    executable = True,
    local = True,
)


