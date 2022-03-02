load("@icon//cli:defs.bzl", "create_keystore", "get_score_address")

package(default_visibility = ["//visibility:public"])

create_keystore(
    name = "bmc",
)

create_keystore(
    name = "bmv",
)

get_score_address(
    name = "bmc",
)

get_score_address(
    name = "near_bmv",
)

get_score_address(
    name = "irc",
)

get_score_address(
    name = "nativecoin",
)

genrule(
    name = "deploy_bmc",
    srcs = ["@icon//:goloop_config_dir"],
    outs = ["deploy_bmc.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon  sendtx deploy $(location @javascore//bmc:javascore) --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --content_type application/java --step_limit 13610920001 --param _net=\"$$(cat $(location @icon//:wait_for_channel_up)).icon\" | echo $$(tr -d '\"') > $@",
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:wait_for_channel_up",
        "@icon//:wait_until_icon_up",
        "@javascore//bmc:javascore",
    ],
)

genrule(
    name = "deploy_near_bmv",
    srcs = ["@icon//:goloop_config_dir"],
    outs = ["deploy_near_bmv.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon sendtx deploy \"$(location @javascore//bmv/near/verifier:javascore)\" --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --content_type application/java --step_limit 13610920001 --param _bmc=\"$$(cat $(location @icon//cli:get_score_address_bmc))\" --param _validators=\"\" --param _offset=10 --param _proofDecoder=\"cx0000000000000000000000000000000000000000\" --param _net=\"0x1.near\" | echo $$(tr -d '\"') > $@",
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:wait_for_channel_up",
        "@icon//:wait_until_icon_up",
        "@icon//cli:get_score_address_bmc",
        "@javascore//bmv/near/verifier:javascore",
    ],
)

genrule(
    name = "deploy_nativecoin",
    srcs = ["@icon//:goloop_config_dir"],
    outs = ["deploy_nativecoin.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon  sendtx deploy $(location @javascore//nativecoin:javascore_native) --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --content_type application/java --step_limit 13610920001 --param _bmc=$$(cat $(location @icon//cli:get_score_address_bmc)) --param _irc31=$$(cat $(location @icon//cli:get_score_address_irc)) --param _name=ICX  | echo $$(tr -d '\"') > $@",
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:wait_for_channel_up",
        "@icon//:wait_until_icon_up",
        "@icon//cli:deploy_bmc",
        "@icon//cli:deploy_irc",
        "@icon//cli:get_score_address_bmc",
        "@icon//cli:get_score_address_irc",
        "@javascore//nativecoin:javascore_native",
    ],
)

genrule(
    name = "deploy_irc",
    srcs = ["@icon//:goloop_config_dir"],
    outs = ["deploy_irc.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon  sendtx deploy $(location @javascore//nativecoin:javascore_irc) --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --content_type application/java --step_limit 13610920001 | echo $$(tr -d '\"') > $@",
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:wait_for_channel_up",
        "@icon//:wait_until_icon_up",
        "@javascore//nativecoin:javascore_irc",
    ],
)

genrule(
    name = "set_near_link_rotate_term",
    srcs = [
        "@near//:btp_address_near",
        "@icon//:goloop_config_dir",
        "@icon//cli:add_link_icon",
    ],
    outs = ["set_near_link_rotate_term.out"],
    cmd = """$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon sendtx  call --to $$(cat $(location @icon//cli:get_score_address_bmc))  --method setLinkRotateTerm --param _link=$$(cat $(location @near//:btp_address_near)) --param _block_interval=0x1770 --param _max_agg=0x08 --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --step_limit 13610920001  | jq -r . >$@ """,
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:wait_for_channel_up",
        "@icon//:wait_until_icon_up",
        "@icon//cli:get_score_address_bmc",
        "@javascore//bmc:javascore",
    ],
)

genrule(
    name = "set_near_link_delay_limit",
    srcs = [
        "@near//:btp_address_near",
        "@icon//:goloop_config_dir",
        "@icon//cli:add_link_icon",
    ],
    outs = ["set_near_link_delay_limit.out"],
    cmd = """$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon sendtx  call --to $$(cat $(location @icon//cli:get_score_address_bmc)) --method setLinkDelayLimit --param _link=$$(cat $(location @near//:btp_address_near)) --param _value=4 --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --step_limit 13610920001  | jq -r . >$@ """,
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:wait_for_channel_up",
        "@icon//:wait_until_icon_up",
        "@icon//cli:get_score_address_bmc",
        "@javascore//bmc:javascore",
    ],
)

genrule(
    name = "set_near_link",
    srcs = [
        "@icon//cli:set_near_link_rotate_term",
        "@icon//cli:set_near_link_delay_limit"
    ],
    outs = ["set_near_link.out"],
    cmd = "echo 'set_link_completed' >$@",
    executable = True,
    local = True,
)
