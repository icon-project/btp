# buildifier: disable=module-docstring
load("@rules_rust//rust:defs.bzl", "rust_common")

# buildifier: disable=bzl-visibility
load("@rules_rust//rust/private:transitions.bzl", "wasm_bindgen_transition")

def rust_wasm_generator_impl(ctx):
    """The generated wasm.
    """
    toolchain = ctx.toolchains[Label("@rules_rust//wasm_bindgen:wasm_bindgen_toolchain")]
    bindgen_bin = toolchain.bindgen
    if len(ctx.attr.wasm_file) == 1 and rust_common.crate_info in ctx.attr.wasm_file[0]:
        target = ctx.attr.wasm_file[0]
        crate_info = target[rust_common.crate_info]

        if rust_common.crate_info in target:
            supported_types = ["cdylib", "bin"]
            if crate_info.type not in supported_types:
                fail("The target '{}' is not a supported type: {}".format(
                    ctx.attr.crate.label,
                    supported_types,
                ))

        progress_message_label = target.label
        input_file = crate_info.output
    else:
        progress_message_label = ctx.file.wasm_file.path
        input_file = ctx.file.wasm_file

    bindgen_wasm_module = ctx.actions.declare_file(ctx.attr.name + "_bg.wasm")

    outputs = [bindgen_wasm_module]

    args = ctx.actions.args()
    args.add("--target", ctx.attr.target)
    args.add("--out-dir", bindgen_wasm_module.dirname)
    args.add("--out-name", ctx.attr.name)
    args.add_all(ctx.attr.bindgen_flags)
    args.add(input_file)

    ctx.actions.run(
        executable = bindgen_bin,
        inputs = [input_file],
        outputs = outputs,
        mnemonic = "RustWasmBindgen",
        progress_message = "Generating WebAssembly bindings for {}...".format(progress_message_label),
        arguments = [args],
    )

    return [
        DefaultInfo(
            files = depset(outputs),
        ),
    ]

rust_wasm_generator = rule(
    implementation = rust_wasm_generator_impl,
    attrs = {
        "bindgen_flags": attr.string_list(
            doc = "Flags to pass directly to the bindgen executable. See https://github.com/rustwasm/wasm-bindgen/ for details.",
        ),
        "target": attr.string(
            doc = "The type of output to generate. See https://rustwasm.github.io/wasm-bindgen/reference/deployment.html for details.",
            default = "bundler",
            values = ["web", "bundler", "nodejs", "no-modules", "deno"],
        ),
        "wasm_file": attr.label(
            doc = "The crate to generate wasm for.",
            allow_single_file = True,
            cfg = wasm_bindgen_transition,
            mandatory = True,
        ),
        "_allowlist_function_transition": attr.label(
            default = Label("@rules_rust//tools/allowlists/function_transition_allowlist"),
        ),
    },
    toolchains = [
        str(Label("@rules_rust//wasm_bindgen:wasm_bindgen_toolchain")),
    ],
    incompatible_use_toolchain_transition = True,
)
