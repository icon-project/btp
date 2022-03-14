load(
    "@io_bazel_rules_docker//container:container.bzl",
    "container_image",
)
load("@btp//utils:defs.bzl", "open_port")

package(default_visibility = ["//visibility:public"])

genrule(
    name = "goloop_config_dir",
    outs = ["goloop_config_dir.out"],
    cmd = "mkdir -p \"$$(cat $(location @btp//:tmp_dir))/goloop_config_dir\";  cp -LR $$PWD/javascore/icon/cli/configs/"+ select({"@icon//:localnet":"localnet","@icon//:testnet": "testnet"}) +"/* -t $$(cat $(location @btp//:tmp_dir))/goloop_config_dir/  ; echo $$(cat $(location @btp//:tmp_dir))/goloop_config_dir > $@",
    executable = True,
    local = True,
    output_to_bindir = True,
    tools = [
        "@btp//:tmp_dir",
    ],
)

container_image(
    name = "icon_node_image",
    base = "@goloop_base//image",
    env = {
        "GOLOOP_NODE_DIR": "/goloop/data/goloop",
        "GOLOOP_LOG_WRITER_FILENAME": "/goloop/data/log/goloop.log",
    },
    files = [
        "docker/entrypoint",
        "docker/provision.sh",
        "docker/server.sh",
    ],
)

open_port(
    name = "icon",
)

genrule(
    name = "deploy_node",
    outs = ["goloop_node"],
    cmd = """
            docker run -d -v $$(cat $(location :goloop_config_dir)):/goloop/config -p $$(cat $(location :open_port_icon)):9080 bazel:icon_node_image 
            cat $(location :open_port_icon) > \"$@\" """,
    executable = True,
    output_to_bindir = True,
    tools = [
        ":goloop_config_dir",
        ":icon_node_image",
        ":open_port_icon",
    ],
)

genrule(
    name = "wait_for_channel_up",
    outs = ["wait_for_channel_up.out"],
    cmd = "$(location @icon//utils:wait_for_channel_script) $$(cat $(location @icon//:wait_until_icon_up)) admin/chain 300 > $@",
    executable = True,
    output_to_bindir = True,
    tags = [
        "no-cache",
    ],
    tools = [
        "@icon//:wait_until_icon_up",
        "@icon//utils:wait_for_channel_script",
    ],
)

genrule(
    name = "wait_until_icon_up",
    outs = ["wait_until_icon_up.out"],
    cmd = "$(location @btp//utils:wait_until_curl_script) http://localhost:$$(cat $(location :deploy_node)) admin/system 300 > $@",
    executable = True,
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
    outs = ["btp_address.out"],
    cmd = "echo \"btp://$$(cat $(location @icon//:network_address))/$$(cat $(location @icon//cli:get_score_address_bmc))\" > $@",
    executable = True,
    local = True,
    tools = [
        "@icon//:network_address",
        "@icon//cli:get_score_address_bmc",
    ],
)

genrule(
    name = "network_address",
    outs = ["network_address.out"],
    cmd = "echo \"$$(cat $(location @icon//:network_id)).icon\" > $@",
    executable = True,
    local = True,
    tools = [
        "@icon//:network_id",
    ],
)

genrule(
    name = "bmr_config_dir",
    outs = ["bmr_config_dir.out"],
    cmd = "mkdir -p \"$$(cat $(location @btp//:tmp_dir))/icon_bmr_config_dir\"  && echo \"$$(cat $(location @btp//:tmp_dir))/icon_bmr_config_dir\" > $@",
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
    srcs = [
        "@icon//:node_url",
    ],
    outs = ["latest_block_height.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url)) lastblock | jq .height >$@",
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
    ],
)

genrule(
    name = "previous_known_block_hash",
    srcs = [
        ":latest_block_height",
        "@icon//:node_url",
    ],
    outs = ["previous_known_block_hash.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url)) blockbyheight $$(($$(cat $(location :latest_block_height))-1)) | jq .block_hash | echo $$(tr -d '\"')  >$@",
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
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
    srcs = select({
        ":localnet": [
            "@icon//:wait_for_channel_up",
            "@icon//:wait_until_icon_up",
        ],
        ":testnet": [
            
        ],
    }),
    outs = ["node_url.out"],
    cmd = "echo " + select({
        ":testnet": "https://lisbon.net.solidwallet.io/api/v3/icon_dex",
        ":localnet": "$$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon",
    }) + " > $@",
    executable = True,
    local = True,
)

genrule (
    name = "network_id",
    srcs = select({
        ":localnet": [
            "@icon//:wait_for_channel_up",
        ],
        ":testnet": [],
    }),
    outs = ["network_id.out"],
    cmd = "echo " + select({
        ":testnet": "0x2",
        ":localnet": "$$(cat $(location @icon//:wait_for_channel_up))",
    }) + " > $@",
    executable = True,
    local = True,
)