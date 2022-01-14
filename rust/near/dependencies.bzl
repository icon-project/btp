load("@io_bazel_rules_docker//container:container.bzl", "container_pull")
load("@build_bazel_rules_nodejs//:index.bzl", "node_repositories", "npm_install")

def dependencies():
    container_pull(
        name = "nearup_base",
        registry = "index.docker.io",
        repository = "nearprotocol/nearup",
        tag = "latest",
    )

    node_repositories(
        node_repositories = {
            "16.3.0-linux_arm64": ("node-v16.3.0-linux-arm64.tar.gz", "node-v16.3.0-linux-arm64", "7040a1f2a0a1aa9cf0f66ec46d0049c6638cb4c05490c13ca71d298fa94ed8ce"),
            "16.3.0-linux_amd64": ("node-v16.3.0-linux-x64.tar.gz", "node-v16.3.0-linux-x64", "86f6d06c05021ae73b51f57bb56569a2eebd4a2ecc0b881972a0572e465b5d27"),
            "16.3.0-darwin_amd64": ("node-v16.3.0-darwin-x64.tar.gz", "node-v16.3.0-darwin-x64", "3e075bcfb6130dda84bfd04633cb228ec71e72d9a844c57efb7cfff130b4be89"),
            "16.3.0-darwin_arm64": ("node-v16.3.0-darwin-arm64.tar.gz", "node-v16.3.0-darwin-arm64", "aeac294dbe54a4dfd222eedfbae704b185c40702254810e2c5917f6dbc80e017"),
            "16.3.0-windows_amd64": ("node-v16.3.0-win-x64.zip", "node-v16.3.0-win-x64", "3352e58d3603cf58964409d07f39f3816285317d638ddb0a0bf3af5deb2ff364"),
        },
        node_version = "16.3.0",
    )

    npm_install(
        name = "near_npm",
        package_json = "@near//cli:package.json",
        package_lock_json = "@near//cli:package-lock.json",
        symlink_node_modules = False,
    )
