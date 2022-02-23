package(default_visibility = ["//visibility:public"])

genrule(
    name = "javascore",
    outs = ["bmv-0.1.0-optimized.jar"],
    cmd = "gradle -q -b $$PWD/external/javascore/bmv/build.gradle build; cp $$PWD/external/javascore/bmc/build/libs/bmv-0.1.0-optimized.jar $@",
    executable = True,
    local = True,
    tools = [
        "@javascore//lib:btp_lib_javascore",
    ],
)
