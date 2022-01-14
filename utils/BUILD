load("@rules_python//python:defs.bzl", "py_binary")
load("@util_python_deps//:requirements.bzl", "requirement")

package(default_visibility = ["//visibility:public"])

py_binary(
    name = "open_port_script",
    srcs = ["open_port_script.py"],
)

py_binary(
    name = "wait_until_curl_script",
    srcs = ["wait_until_curl_script.py"],
    deps = [
        requirement("requests"),
        requirement("backoff"),
    ],
)