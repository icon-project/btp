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
