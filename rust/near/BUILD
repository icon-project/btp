load(
    "@io_bazel_rules_docker//container:container.bzl",
    "container_image",
)
load("@btp//utils:defs.bzl", "open_port")

package(default_visibility = ["//visibility:public"])

genrule(
    name = "near_config_dir",
    outs = ["near_config_dir.out"],
    cmd = """mkdir -p \"$$PWD/near_config_dir/localnet/node0\"
    cp -t $$PWD/near_config_dir/localnet/node0 -r ~/.near/localnet/node0/{config.json,genesis.json,node_key.json,validator_key.json} ;
    echo \"$$PWD/near_config_dir\" > $@""",
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
    cmd = "$(location @btp//utils:wait_until_curl_script) http://localhost:49363 status 300 > $@",
    executable = True,
    output_to_bindir = True,
    tags = [
        "no-cache",
    ],
    tools = [
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
        "@near//:wait_until_near_up",
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
    cmd = "mkdir -p \"$$PWD/near_bmr_config_dir\"  && echo \"$$PWD/near_bmr_config_dir\" > $@",
    output_to_bindir = True,
    executable = True,
    local = True,
)

genrule(
    name = "endpoint_docker",
    outs = ["endpoint.out"],
    cmd = "echo \"http://172.17.0.1:49363\" > $@",
    executable = True,
    local = True,
    tools = [
    ],
)

genrule(
    name = "latest_block_height",
    outs = ["latest_block_height.out"],
    cmd = "curl http://localhost:49363/status | jq .sync_info.latest_block_height >$@",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:near_binary",
    ],
)
