load("@icon//cli:defs.bzl", "configure_bmr", "configure_link", "create_keystore", "get_score_address")

package(default_visibility = ["//visibility:public"])

create_keystore(
    name = "bmc",
)

create_keystore(
    name = "bmv",
)

get_score_address(
    name = "get_score_address_bmc",
    contract = "bmc",
)

get_score_address(
    name = "get_score_address_near_bmv",
    contract = "near_bmv",
)

get_score_address(
    name = "get_score_address_irc",
    contract = "irc",
)

get_score_address(
    name = "get_score_address_nativecoin",
    contract = "nativecoin",
)

configure_bmr(
    name = "icon",
)

genrule(
    name = "deploy_bmc",
    srcs = ["@icon//:goloop_config_dir"],
    outs = ["deploy_bmc.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url)) sendtx deploy $(location @javascore//bmc:javascore) --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:network_id))\" --content_type application/java --step_limit 13610920001 --param _net=\"$$(cat $(location @icon//:network_address))\" | echo $$(tr -d '\"') > $@",
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:network_address",
        "@icon//:network_id",
        "@icon//:node_url",
        "@javascore//bmc:javascore",
    ],
)

genrule(
    name = "deploy_near_bmv",
    srcs = ["@icon//:goloop_config_dir"],
    outs = ["deploy_near_bmv.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url)) sendtx deploy \"$(location @javascore//bmv/near/verifier:javascore)\" --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:network_id))\" --content_type application/java --step_limit 13610920001 --param _bmc=\"$$(cat $(location @icon//cli:get_score_address_bmc))\" --param _validators=\"\" --param _offset=10 --param _proofDecoder=\"cx0000000000000000000000000000000000000000\" --param _net=\"0x1.near\" | echo $$(tr -d '\"') > $@",
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:network_id",
        "@icon//:node_url",
        "@icon//cli:get_score_address_bmc",
        "@javascore//bmv/near/verifier:javascore",
    ],
)

genrule(
    name = "deploy_nativecoin",
    srcs = ["@icon//:goloop_config_dir"],
    outs = ["deploy_nativecoin.out"],
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url))  sendtx deploy $(location @javascore//nativecoin:javascore_native) --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:network_id))\" --content_type application/java --step_limit 13610920001 --param _bmc=$$(cat $(location @icon//cli:get_score_address_bmc)) --param _irc31=$$(cat $(location @icon//cli:get_score_address_irc)) --param _name=ICX  | echo $$(tr -d '\"') > $@",
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:network_id",
        "@icon//:node_url",
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
    cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url)) sendtx deploy $(location @javascore//nativecoin:javascore_irc) --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:network_id))\" --content_type application/java --step_limit 13610920001 | echo $$(tr -d '\"') > $@",
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:network_id",
        "@icon//:node_url",
        "@javascore//nativecoin:javascore_irc",
    ],
)

genrule(
    name = "set_near_link_rotate_term",
    srcs = [
        "@near//:btp_address",
        "@icon//:goloop_config_dir",
        "@icon//cli:add_near_link",
    ],
    outs = ["set_near_link_rotate_term.out"],
    cmd = """$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url)) sendtx  call --to $$(cat $(location @icon//cli:get_score_address_bmc))  --method setLinkRotateTerm --param _link=$$(cat $(location @near//:btp_address)) --param _block_interval=0x1770 --param _max_agg=0x08 --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:network_id))\" --step_limit 13610920001  | jq -r . >$@ """,
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:network_id",
        "@icon//:node_url",
        "@icon//cli:get_score_address_bmc",
        "@javascore//bmc:javascore",
    ],
)

genrule(
    name = "set_near_link_delay_limit",
    srcs = [
        "@near//:btp_address",
        "@icon//:goloop_config_dir",
        "@icon//cli:add_near_link",
    ],
    outs = ["set_near_link_delay_limit.out"],
    cmd = """$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url)) sendtx  call --to $$(cat $(location @icon//cli:get_score_address_bmc)) --method setLinkDelayLimit --param _link=$$(cat $(location @near//:btp_address)) --param _value=4 --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:network_id))\" --step_limit 13610920001  | jq -r . >$@ """,
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:network_id",
        "@icon//:node_url",
        "@icon//cli:get_score_address_bmc",
        "@javascore//bmc:javascore",
    ],
)

genrule(
    name = "set_near_link",
    srcs = [
        "@icon//cli:set_near_link_rotate_term",
        "@icon//cli:set_near_link_delay_limit",
        "@icon//cli:add_service_icon",
    ],
    outs = ["set_near_link.out"],
    cmd = "echo 'set_link_completed' >$@",
    executable = True,
    local = True,
)

configure_link(
    name = "near",
)

genrule(
    name = "keysecret",
    outs = [
        "keysecret.out",
    ],
    cmd = "echo 'btpsimple' > $@",
    executable = True,
    local = True,
)

genrule(
    name = "add_service_icon",
    srcs = ["@icon//cli:deploy_nativecoin"],
    outs = ["add_service_icon.out"],
    cmd = """$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url)) sendtx  call --to $$(cat $(location @icon//cli:get_score_address_bmc)) --method addService --param _addr=$$(cat $(location @icon//cli:get_score_address_nativecoin)) --param _svc=nativecoin --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:network_id))\" --step_limit 13610920001  | jq -r . >$@ """,
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:goloop_config_dir",
        "@icon//:network_id",
        "@icon//:node_url",
        "@icon//cli:get_score_address_bmc",
        "@icon//cli:get_score_address_nativecoin",
    ],
)

genrule(
    name = "get_icon_validators",
    srcs = ["@btp//cmd/iconvalidators"],
    outs = ["get_icon_validators.out"],
    cmd = """ GOLOOP_RPC_URI=\"$$(cat $(location @icon//:node_url))\" $(execpath @btp//cmd/iconvalidators:iconvalidators) build $$(cat $(location @icon//:latest_block_height)) | echo $$(tr -d '\"') >$@""",
    executable = True,
    local = True,
    tools = [
        "@icon//:latest_block_height",
        "@icon//:node_url",
    ],
)

genrule(
    name = "add_near_bmr",
    srcs = [
        ":set_near_link",
        "@icon//cli:get_wallet_icon_keystore",
        "@near//:btp_address",
    ],
    outs = ["add_near_bmr.out"],
    cmd = """
    $(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url)) sendtx  call --to $$(cat $(location @icon//cli:get_score_address_bmc)) --method addRelay  --param _link=$$(cat $(location @near//:btp_address)) --param _addr=$$(cat $(location @icon//cli:get_wallet_icon_keystore))  --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:network_id))\" --step_limit 13610920001  | jq -r . >$@
    """,
    executable = True,
    local = True,
    tools = [
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:goloop_config_dir",
        "@icon//:network_id",
        "@icon//:node_url",
        "@icon//cli:get_score_address_bmc",
    ],
)

genrule(
    name = "send_icx_to_near",
    srcs = [
        ":add_service_icon",
        ":set_near_link",
        "@com_github_icon_project_goloop//cmd/goloop",
        "@icon//:goloop_config_dir",
        "@icon//cli:get_score_address_nativecoin",
        "@near//cli:encode_public_key_alice",
    ],
    outs = ["send_icx_to_near.out"],
    cmd = """
     $(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url)) sendtx  call --to $$(cat $(location get_score_address_nativecoin)) --method transferNativeCoin  --param _to=\"btp://0x1.near/$$(cat $(location @near//cli:encode_public_key_alice))\"  --value 1000000000000000000000000 --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:network_id))\" --step_limit 13610920001 >$@
    """,
    executable = True,
    local = True,
    tools = [
        "@icon//:network_id",
        "@icon//:node_url",
    ],
)

select({
    "@icon//:localnet": exports_files(glob(["configs/localnet/*"])),
    "@icon//:docker_localnet": exports_files(glob(["configs/localnet/*"])),
    "@icon//:lisbon": exports_files(["configs/lisbon/keystore.json"]),
    "@icon//:berlin": exports_files(["configs/berlin/keystore.json"]),
    "//conditions:default": []
})
