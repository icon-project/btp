def bridge(name, chains):
    native.genrule(
        name = "set_link_%s" % chains[0],
        outs = ["set_link_%s.out" % chains[0]],
        srcs = ["@%s//cli:set_%s_link" % (chains[0], chains[1])],
        cmd = "echo 'done' > $@",
        local = True,
        executable = True,
    )
    native.genrule(
        name = "set_link_%s" % chains[1],
        outs = ["set_link_%s.out" % chains[1]],
        srcs = ["@%s//cli:set_%s_link" % (chains[1], chains[0])],
        cmd = "echo 'done' > $@",
        local = True,
        executable = True,
    )
    native.genrule(
        name = "%s_and_%s" % (chains[0], chains[1]),
        outs = ["%s_and_%s.out" % (chains[0], chains[1])],
        srcs = [
            ":set_link_%s" % chains[0],
            ":set_link_%s" % chains[1],
        ],
        cmd = "echo 'done' > $@",
        local = True,
        executable = True,
    )
