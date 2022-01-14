def open_port(name):
    native.genrule(
        name = "open_port_%s" % name,
        outs = ["open_port.out"],
        cmd = "$(location @btp//utils:open_port_script) > $@",
        executable = True,
        tools = [
            "@btp//utils:open_port_script",
        ],
    )
