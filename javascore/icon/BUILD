load(
    "@io_bazel_rules_docker//container:container.bzl",
    "container_image",
)
load("@btp//utils:defs.bzl", "open_port")

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
    name = "icon"
)

genrule(
    name = "deploy_node",
    outs = ["goloop_node"],
    cmd = """
            docker run -d -v ~/.goloop/config:/goloop/config -p $$(cat $(location :open_port_icon)):9080 bazel:icon_node_image
            cat $(location :open_port_icon) > \"$@\" """,
    executable = True,
    tools = [
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
    output_to_bindir = True,
    tags = [
        "no-cache",
    ],
    tools = [
        ":deploy_node",
        "@btp//utils:wait_until_curl_script",
    ],
)

