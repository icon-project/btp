load("@icon//cli:defs.bzl", "create_keystore","get_score_address")

package(default_visibility = ["//visibility:public"])


create_keystore(
    name = "bmc"
)

create_keystore(
    name = "bmv"
)

get_score_address(
    name = "bmc"
)

get_score_address(
    name = "bmv"
)

get_score_address(
    name = "irc"
)

get_score_address(
    name = "nativecoin"
)


genrule(
    name = "deploy_bmc",
    outs = ["deploy_bmc.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon  sendtx deploy $(location @javascore//bmc:javascore) --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --content_type application/java --step_limit 13610920001 --param _net=\"$$(cat $(location @icon//:wait_for_channel_up)).icon\" | echo $$(tr -d '\"') > $@",
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
    name = "deploy_near_bmv",
    outs = ["deploy_near_bmv.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon sendtx deploy \"$(location @javascore//bmv/near/verifier:javascore)\" --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --content_type application/java --step_limit 13610920001 --param _bmc=\"$$(cat $(location @icon//cli:get_score_address_bmc))\" --param _validators=\"\" --param _offset=10 --param _proofDecoder=\"\" --param _net=\"0x1.near\" | echo $$(tr -d '\"') > $@",
    executable = True,
    local = True,
    tools = [
        "@icon//:goloop_config_dir",
        "@icon//cli:get_score_address_bmc",
        "@javascore//bmv/near/verifier:javascore",
        "@com_github_icon_project_goloop//cmd/goloop:goloop",
        "@icon//:wait_for_channel_up",
        "@icon//:wait_until_icon_up",
    ],
)

genrule(
    name = "deploy_nativecoin",
    outs = ["deploy_nativecoin.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon  sendtx deploy $(location @javascore//nativecoin:javascore_native) --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --content_type application/java --step_limit 13610920001 --param _bmc=$$(cat $(location @icon//cli:get_score_address_bmc)) --param _irc31=$$(cat $(location @icon//cli:get_score_address_irc)) --param _name=ICX  | echo $$(tr -d '\"') > $@",
    executable = True,
    local = True,
    tools = [
        "@icon//:goloop_config_dir",
        "@icon//cli:deploy_irc",
        "@icon//cli:deploy_bmc",
        "@icon//cli:get_score_address_irc",
        "@icon//cli:get_score_address_bmc",
        "@javascore//nativecoin:javascore_native",
        "@com_github_icon_project_goloop//cmd/goloop:goloop",
        "@icon//:wait_for_channel_up",
        "@icon//:wait_until_icon_up",
    ],
)

genrule(
    name = "deploy_irc",
    outs = ["deploy_irc.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon  sendtx deploy $(location @javascore//nativecoin:javascore_irc) --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --content_type application/java --step_limit 13610920001 | echo $$(tr -d '\"') > $@",
    executable = True,
    local = True,
    tools = [
        "@icon//:goloop_config_dir",
        "@javascore//nativecoin:javascore_irc",
        "@com_github_icon_project_goloop//cmd/goloop:goloop",
        "@icon//:wait_for_channel_up",
        "@icon//:wait_until_icon_up",
    ],
)
