def create_keystore(name):
    native.genrule(
        name = "create_keystore_%s" % name,
        outs = ["keystore_%s.json" % name],
        cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) ks gen -o $@",
        executable = True,
        local = True,
        tools = [
            "@com_github_icon_project_goloop//cmd/goloop:goloop",
        ],
    )

def get_score_address(name, contract):
    native.genrule(
        name = "get_score_address_%s" % contract,
        outs = ["score_address_%s.out" % contract],
        cmd = "sleep 10 && $(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc txresult $$(cat $(location @icon//cli:deploy_%s)) --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon | jq -r .scoreAddress > $@" % contract,
        executable = True,
        local = True,
        tools = [
            "@icon//cli:deploy_%s" % contract,
            "@com_github_icon_project_goloop//cmd/goloop:goloop",
            "@icon//:wait_until_icon_up",
        ],
    )

def configure_link(name):
    native.genrule(
        name = "add_%s_verifier" % name,
        srcs = [
            "@%s//:btp_address" % name,
            "@%s//:network_address" % name,
            "@icon//:goloop_config_dir",
            "@icon//cli:get_score_address_bmc",
            "@icon//cli:get_score_address_%s_bmv" % name,
        ],
        outs = ["add_%s_verifier.out" % name ],
        cmd = """$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon  sendtx  call --to $$(cat $(location @icon//cli:get_score_address_bmc)) --method addVerifier --param _net=\"$$(cat $(location @%s//:network_address))\" --param _addr=\"$$(cat $(location @icon//cli:get_score_address_%s_bmv))\" --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --step_limit 13610920001  | jq -r . > $@ """ % (name, name),
        tools = [
            "@com_github_icon_project_goloop//cmd/goloop",
            "@icon//:wait_for_channel_up",
            "@icon//:wait_until_icon_up",
        ],
    )
    native.genrule(
        name = "add_%s_link" % name,
        srcs = [
            "@%s//:btp_address" % name,
            "@icon//:goloop_config_dir",
            "@icon//cli:add_%s_verifier" % name,
        ],
        outs = ["add_%s_link.out" % name],
        cmd = """$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon sendtx  call --to $$(cat $(location @icon//cli:get_score_address_bmc))  --method addLink --param _link=$$(cat $(location @%s//:btp_address)) --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:wait_for_channel_up))\" --step_limit 13610920001  | jq -r . >$@ """ % name,
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
