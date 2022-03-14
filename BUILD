package(default_visibility = ["//visibility:public"])

genrule(
    name="tmp_dir",
    outs = ["tmp_dir.outs"],
    cmd ="mktemp -d >$@",
    local = True,
    executable = True
)