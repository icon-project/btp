load("@btp//bridge:defs.bzl", "bridge")

bridge(
    name = "near_icon",
    destination = "icon",
    source = "near",
)

genrule(
    name = "configure_smartcontracts_icon",
    srcs = [
        "@icon//cli:set_link_icon"
    ],
    outs = ["configure_smartcontracts_icon.out"],
    cmd = "echo 'finished deploying and configuring icon contratcs' >$@",
    executable = True,
    local = True,
)

genrule(
    name = "configure_smartcontracts_near",
    srcs = [
        "@near//cli:set_link_near",
    ],
    outs = ["configure_smartcontracts_near.out"],
    cmd = "echo 'finished deploying and configuring near contratcs' >$@",
    executable = True,
    local = True,
)

genrule(
    name = "configure_smart_contracts",
    srcs = [
        "@btp//bridge:configure_smartcontracts_icon",
        "@btp//bridge:configure_smartcontracts_near"
    ],
    outs = ["configure_smart_contracts.out"],
    cmd = "echo 'finished deploying and configuring contratcs' >$@",
    executable = True,
    local = True,

)
