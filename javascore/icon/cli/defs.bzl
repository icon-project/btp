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
        cmd = "sleep 10 && $(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc txresult $$(cat $(location @icon//cli:deploy_%s)) --uri $$(cat $(location @icon//:node_url)) | jq -r .scoreAddress > $@" % contract,
        executable = True,
        local = True,
        tools = [
            "@icon//cli:deploy_%s" % contract,
            "@com_github_icon_project_goloop//cmd/goloop:goloop",
            "@icon//:node_url",
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
        outs = ["add_%s_verifier.out" % name],
        cmd = """$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url))  sendtx  call --to $$(cat $(location @icon//cli:get_score_address_bmc)) --method addVerifier --param _net=\"$$(cat $(location @%s//:network_address))\" --param _addr=\"$$(cat $(location @icon//cli:get_score_address_%s_bmv))\" --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:network_id))\" --step_limit 13610920001  | jq -r . > $@ """ % (name, name),
        tools = [
            "@com_github_icon_project_goloop//cmd/goloop",
            "@icon//:network_id",
            "@icon//:node_url",
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
        cmd = """$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url)) sendtx  call --to $$(cat $(location @icon//cli:get_score_address_bmc))  --method addLink --param _link=$$(cat $(location @%s//:btp_address)) --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:network_id))\" --step_limit 13610920001  | jq -r . >$@ """ % name,
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

def configure_bmr(name):
    native.genrule(
        name = "generate_%s_keystore" % name,
        srcs = [
            "@com_github_icon_project_goloop//cmd/goloop",
            "@icon//cli:keysecret",
        ],
        outs = [
            "generate_%s_keystore.json" % name,
        ],
        cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) ks gen -p $$(cat $(location @icon//cli:keysecret)) -o $@",
        executable = True,
        local = True,
    )

    native.genrule(
        name = "get_wallet_%s_keystore" % name,
        srcs = [
            "@icon//cli:generate_%s_keystore" % name,
        ],
        outs = ["get_wallet_%s_keystore.out" % name],
        cmd = "echo \"$$(cat $(location :generate_%s_keystore))\" | jq .address | echo $$(tr -d '\"') >$@" % name,
        executable = True,
        local = True,
    )

    native.genrule(
        name = "transfer_amount_%s_address" % name,
        srcs = [
            "@icon//cli:generate_%s_keystore" % name,
            "@icon//cli:get_wallet_%s_keystore" % name,
            "@com_github_icon_project_goloop//cmd/goloop",
            "@icon//:goloop_config_dir",
            "@icon//:network_id",
            "@icon//:node_url",
        ],
        outs = ["%s_keystore.json" % name],
        cmd = "$(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc --uri $$(cat $(location @icon//:node_url)) sendtx transfer --to $$(cat $(location @icon//cli:get_wallet_%s_keystore)) --value 1000000000000000000000000 --key_store $$(cat $(location @icon//:goloop_config_dir))/keystore.json --key_secret $$(cat $(location @icon//:goloop_config_dir))/keysecret --nid \"$$(cat $(location @icon//:network_id))\" --step_limit 13610920001 && echo \"$$(cat $(location @icon//cli:generate_%s_keystore))\"| jq -r . >$@" % (name, name),
        executable = True,
        local = True,
    )
