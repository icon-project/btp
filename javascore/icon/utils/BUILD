load("@rules_python//python:defs.bzl", "py_binary")
load("@icon_util_python_deps//:requirements.bzl", "requirement")

package(default_visibility = ["//visibility:public"])

py_binary(
    name = "wait_for_channel_script",
    srcs = ["wait_for_channel_script.py"],
    deps = [
        requirement("requests"),
        requirement("backoff"),
    ],
)
