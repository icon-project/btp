load("@near//:dependencies.bzl", near_dependencies = "dependencies")
load("@icon//:dependencies.bzl", icon_dependencies = "dependencies")

def chain_dependencies():
    near_dependencies()
    icon_dependencies()
