package(default_visibility = ["//visibility:public"])

genrule(
    name = "javascore_native",
    outs = [
        "optimized",
    ],
    cmd = "gradle -q -b $$PWD/external/javascore/nativecoin/build.gradle build; cp -r $$PWD/external/javascore/nativecoin/build/libs/  $@",
    executable = True,
    local = True,
    tools = [
        "@javascore//lib:btp_lib_javascore",
    ],
)
