load("@icon//cli:defs.bzl", "create_keystore")

package(default_visibility = ["//visibility:public"])


create_keystore(
    name = "bmc"
)

create_keystore(
    name = "bmv"
)

genrule(
    name = "deploy_bmc",
    outs = ["deploy_bmc.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon  sendtx deploy $(location @javascore//bmc:javascore) --key_store $(location @icon//cli:create_keystore_bmc) --key_password \"gochain\" --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --content_type application/java --step_limit 13610920001 --param _net=\"$$(cat $(location @icon//:wait_for_channel_up)).icon\" > $@",
    executable = True,
    local = True,
    tools = [
        "@icon//cli:create_keystore_bmc",
        "@javascore//bmc:javascore",
        "@com_github_icon_project_goloop//cmd/goloop:goloop",
        "@icon//:wait_for_channel_up",
        "@icon//:wait_until_icon_up",
    ],
)

genrule(
    name = "deploy_bmv",
    outs = ["deploy_bmv.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/$$(cat $(location @icon//:wait_for_channel_up))  sendtx deploy $(location @javascore//bmv:javascore) --key_store $(location @icon//cli:create_keystore_bmc) --key_password \"gochain\" --nid \"0x2\" --content_type application/java --step_limit 13610920001 --param _net=\"0x2.icon\" > $@",
    executable = True,
    local = True,
    tools = [
        "@icon//cli:create_keystore_bmv",
        "@javascore//bmv:javascore",
        "@com_github_icon_project_goloop//cmd/goloop:goloop",
        "@icon//:wait_for_channel_up",
        "@icon//:wait_until_icon_up",
    ],
)

