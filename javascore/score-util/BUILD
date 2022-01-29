load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
   name = "build_score_util_jar",
    srcs = glob(["src/main/**/*.java"]),
    deps = [
        "@maven//:foundation_icon_javaee_api",
        "@maven//:foundation_icon_javaee_scorex"
    ],
)
 