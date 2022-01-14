load("@io_bazel_rules_docker//container:container.bzl", "container_pull")
load("//utils:dependencies.bzl", util_dependencies = "dependencies")

def dependencies():
    container_pull(
        name = "goloop_base",
        registry = "index.docker.io",
        repository = "iconloop/goloop",
        tag = "latest",
    )
    util_dependencies()