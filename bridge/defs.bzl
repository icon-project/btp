def bridge(name, source, destination):
    native.genrule(
        name = "%s_to_%s" % (source, destination),
        outs = ["%s_to_%s.out" % (source, destination)],
        cmd = "echo 'done' > $@",
        local = True,
        executable = True,
        tools = [
            "@%s//cli:add_link_%s" % (source,destination)
        ]
    )
