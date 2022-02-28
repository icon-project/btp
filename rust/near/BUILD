load(
    "@io_bazel_rules_docker//container:container.bzl",
    "container_image",
)
load("@btp//utils:defs.bzl", "open_port")

package(default_visibility = ["//visibility:public"])

genrule(
    name = "near_config_dir",
    outs = ["near_config_dir.out"],
    cmd = "mkdir \"$$PWD/near_config_dir\"; echo \"$$PWD/near_config_dir\" > $@",
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
    name = "near"
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
    name = "btp_address_near",
    srcs = ["@near//cli:deploy_bmc"],
    outs = ["btp_address_near.out"],
    cmd = "echo \"btp://0x1.near/$$(cat $(location @near//cli:encode_public_key_bmc))\" > $@",
    executable = True,
    local = True,
    tools = [
        "@near//:wait_until_near_up",
        "@near//cli:encode_public_key_bmc",
    ],
)