load("@rules_jvm_external//:defs.bzl", "maven_install")

def dependencies():
    maven_install(
        artifacts = [
            "junit:junit:4.13.2",
            "androidx.test.espresso:espresso-core:3.1.1",
            "org.hamcrest:hamcrest-library:2.2",
            "foundation.icon:javaee-api:0.9.1",
            "foundation.icon:icon-sdk:2.0.0",
            "foundation.icon:javaee-scorex:0.5.2",
        ],
        repositories = [
            "https://maven.google.com",
            "https://repo1.maven.org/maven2",
        ],
    )
