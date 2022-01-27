load("@rules_python//python:pip.bzl", "pip_install")

def dependencies():
    pip_install(
        name = "near_cli_util_python_deps",
        requirements = "@near//cli:requirements.txt",
    )
