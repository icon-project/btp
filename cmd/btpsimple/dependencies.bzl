load("@io_bazel_rules_docker//container:container.bzl", "container_pull")

def dependencies():
    container_pull(
        name = "golang_base",
        registry = "index.docker.io",
        repository = "golang",
        tag = "alpine",
    )
