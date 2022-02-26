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

def get_score_address(name):
    native.genrule(
        name = "get_score_address_%s" % name,
        outs = ["score_address_%s.out" % name],
        cmd = "sleep 10 && $(execpath @com_github_icon_project_goloop//cmd/goloop:goloop) rpc txresult $$(cat $(location @icon//cli:deploy_%s)) --uri $$(cat $(location @icon//:wait_until_icon_up))/api/v3/icon | jq -r .scoreAddress > $@" % name,
        executable = True,
        local = True,
        tools = [
            "@icon//cli:deploy_%s" % name,
            "@com_github_icon_project_goloop//cmd/goloop:goloop",
            "@icon//:wait_until_icon_up",
        ],
    )
