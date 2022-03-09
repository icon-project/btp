load(
    "@io_bazel_rules_docker//container:container.bzl",
    "container_image",
)
load("@btp//utils:defs.bzl", "open_port")

package(default_visibility = ["//visibility:public"])

genrule(
    name = "goloop_config_dir",
    outs = ["goloop_config_dir.out"],
    cmd = "mkdir -p \"$$PWD/goloop_config\"; echo ~/.goloop/goloop_config > $@",
    executable = True,
    local = True,
    output_to_bindir = True,
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
        "@icon//cli:get_score_address_bmc",
        "@icon//:network_address"
    ],
)

genrule(
    name = "network_address",
    outs = ["network_address.out"],
    cmd = "echo \"$$(cat $(location @icon//:wait_for_channel_up)).icon\" > $@",
    executable = True,
    local = True,
    tools = [
        "@icon//:wait_for_channel_up",
    ],
)

genrule(
    name = "bmr_config_dir",
    outs = ["bmr_config_dir.out"],
    cmd = "mkdir -p \"$$PWD/icon_bmr_config_dir\"; echo \"$$PWD/icon_bmr_config_dir\" > $@",
    executable = True,
    local = True,
)

genrule(
    name = "endpoint_docker",
    outs = ["endpoint.out"],
    cmd = "echo \"http://172.17.0.1:$$(cat $(location :deploy_node))/api/v3/icon\" > $@",
    executable = True,
    local = True,
    tools = [
        ":deploy_node",
    ],
)
