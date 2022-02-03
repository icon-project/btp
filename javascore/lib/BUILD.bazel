package(default_visibility = ["//visibility:public"])

genrule(
    name = "btp_lib_javascore",
    outs = ["btp_lib_javascore.out"],
    cmd = "gradle -q -b $$PWD/external/javascore/lib/build.gradle build > $@",
    executable = True,
    local = True,
)
