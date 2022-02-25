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
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon  sendtx deploy $(location @javascore//bmc:javascore) --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --content_type application/java --step_limit 13610920001 --param _net=\"$$(cat $(location @icon//:wait_for_channel_up)).icon\" > $@",
    executable = True,
    local = True,
    tools = [
        "@icon//:goloop_config_dir",
        "@javascore//bmc:javascore",
        "@com_github_icon_project_goloop//cmd/goloop:goloop",
        "@icon//:wait_for_channel_up",
        "@icon//:wait_until_icon_up",
    ],
)

genrule(
    name = "deploy_bmv",
    outs = ["deploy_bmv.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon  sendtx deploy $(location @javascore//bmv:javascore) --key_store ~/.goloop/config/keystore.json --key_secret ~/.goloop/config/keysecret --nid $$(cat $(location @icon//:wait_for_channel_up)) --content_type application/java --step_limit 13610920001 --param _net=\"$$(cat $(location @icon//:wait_for_channel_up)).icon\" > $@",
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

