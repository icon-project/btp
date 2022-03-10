def bridge(name, chains):
    native.genrule(
        name = "set_link_%s" % chains[0],
        outs = ["set_link_%s.out" % chains[0]],
        srcs = ["@%s//cli:set_%s_link" % (chains[0], chains[1])],
        cmd = "echo 'done' > $@",
        local = True,
        executable = True,
    )
    native.genrule(
        name = "set_link_%s" % chains[1],
        outs = ["set_link_%s.out" % chains[1]],
        srcs = ["@%s//cli:set_%s_link" % (chains[1], chains[0])],
        cmd = "echo 'done' > $@",
        local = True,
        executable = True,
    )
    native.genrule(
        name = "%s_and_%s" % (chains[0], chains[1]),
        outs = ["%s_and_%s.out" % (chains[0], chains[1])],
        srcs = [
            ":set_link_%s" % chains[0],
            ":set_link_%s" % chains[1],
            ":deploy_%s_bmr" % chains[0],
            ":deploy_%s_bmr" % chains[1],
        ],
        cmd = "echo 'done' > $@",
        local = True,
        executable = True,
    )

    native.genrule(
        name = "btpsimple_%s_configuration" % (chains[0]),
        outs = ["btpsimple_%s_configuration.json" % (chains[0])],
        srcs = [
            "@btp//cmd/btpsimple:btpsimple",
            "@%s//:bmr_config_dir" % (chains[0]),
            "@%s//cli:add_%s_bmr" % (chains[0], chains[1]),
            "@%s//cli:transfer_amount_%s_address" % (chains[1], chains[1]),
            "@%s//cli:keysecret" % chains[1],
            "@%s//:endpoint_docker" % chains[0],
            "@%s//:endpoint_docker" % chains[1],
            "@%s//:btp_address" % chains[0],
            "@%s//:btp_address" % chains[1],
            "@%s//:latest_block_height" % chains[1],
        ],
        cmd = """echo $$(cat $(location @%s//cli:transfer_amount_%s_address)) > $$(cat $(location @%s//:bmr_config_dir))/keystore.json
        export BTPSIMPLE_SRC_OPTIONS=[mtaRootSize=8]
        export BTPSIMPLE_DST_OPTIONS=[mtaRootSize=8]
        export BTPSIMPLE_LOG_WRITER_MAXSIZE=1024
        export BTPSIMPLE_BASE_DIR=\"./config/data\"
        export BTPSIMPLE_OFFSET=$$(cat $(location @%s//:latest_block_height))
        export BTPSIMPLE_LOG_WRITER_FILENAME=\"./config/log/btpsimple.log\"
        $(execpath @btp//cmd/btpsimple:btpsimple) --key_password $$(cat $(location @%s//cli:keysecret)) --key_store $$(cat $(location @%s//:bmr_config_dir))/keystore.json \
            --src.address $$(cat $(location @%s//:btp_address)) \
            --src.endpoint $$(cat $(location @%s//:endpoint_docker)) \
            --dst.address $$(cat $(location @%s//:btp_address)) \
            --dst.endpoint $$(cat $(location @%s//:endpoint_docker)) \
        save $@; cp $@ $$(cat $(location @%s//:bmr_config_dir))""" % (chains[1], chains[1], chains[0], chains[1], chains[1], chains[0], chains[0], chains[0], chains[1], chains[1], chains[0]),
        local = True,
        executable = True,
    )

    native.genrule(
        name = "btpsimple_%s_configuration" % (chains[1]),
        outs = ["btpsimple_%s_configuration.json" % (chains[1])],
        srcs = [
            "@btp//cmd/btpsimple:btpsimple",
            "@%s//:bmr_config_dir" % (chains[1]),
            "@%s//cli:add_%s_bmr" % (chains[1], chains[0]),
            "@%s//cli:transfer_amount_%s_address" % (chains[0], chains[0]),
            "@%s//cli:keysecret" % chains[0],
            "@%s//:endpoint_docker" % chains[1],
            "@%s//:endpoint_docker" % chains[0],
            "@%s//:btp_address" % chains[0],
            "@%s//:btp_address" % chains[1],
            "@%s//:latest_block_height" % chains[0],
        ],
        cmd = """echo $$(cat $(location @%s//cli:transfer_amount_%s_address)) > $$(cat $(location @%s//:bmr_config_dir))/keystore.json
        export BTPSIMPLE_SRC_OPTIONS=[mtaRootSize=8]
        export BTPSIMPLE_DST_OPTIONS=[mtaRootSize=8]
        export BTPSIMPLE_LOG_WRITER_MAXSIZE=1024
        export BTPSIMPLE_BASE_DIR=\"./config/data\"
        export BTPSIMPLE_OFFSET=$$(cat $(location @%s//:latest_block_height))
        export BTPSIMPLE_LOG_WRITER_FILENAME=\"./config/log/btpsimple.log\"
        $(execpath @btp//cmd/btpsimple:btpsimple) --key_password $$(cat $(location @%s//cli:keysecret)) --key_store $$(cat $(location @%s//:bmr_config_dir))/keystore.json \
            --src.address $$(cat $(location @%s//:btp_address)) \
            --src.endpoint $$(cat $(location @%s//:endpoint_docker)) \
            --dst.address $$(cat $(location @%s//:btp_address)) \
            --dst.endpoint $$(cat $(location @%s//:endpoint_docker)) \
        save $@; cp $@ $$(cat $(location @%s//:bmr_config_dir))""" % (chains[0], chains[0], chains[1], chains[0], chains[0], chains[1], chains[1], chains[1], chains[0], chains[0], chains[1]),
        local = True,
        executable = True,
    )

    native.genrule(
        name = "deploy_%s_bmr" % (chains[0]),
        outs = ["deploy_%s_bmr.out" % (chains[0])],
        cmd = """
            docker run -d -v $$(cat $(location @%s//:bmr_config_dir)):/config bazel/cmd/btpsimple:btpsimple_image start --config "/config/btpsimple_%s_configuration.json" --key_password $$(cat $(location @%s//cli:keysecret))
            echo 'done'> \"$@\" """ % (chains[0], chains[0], chains[1]),
        executable = True,
        output_to_bindir = True,
        srcs = [
            ":btpsimple_%s_configuration" % (chains[0]),
            "@%s//:bmr_config_dir" % (chains[0]),
            "@%s//cli:keysecret" % chains[1],
        ],
    )

    native.genrule(
        name = "deploy_%s_bmr" % (chains[1]),
        outs = ["deploy_%s_bmr.out" % (chains[1])],
        cmd = """
            docker run -d -v $$(cat $(location @%s//:bmr_config_dir)):/config bazel/cmd/btpsimple:btpsimple_image start --config "/config/btpsimple_%s_configuration.json" --key_password $$(cat $(location @%s//cli:keysecret))
            echo 'done'> \"$@\" """ % (chains[1], chains[1], chains[0]),
        executable = True,
        output_to_bindir = True,
        srcs = [
            ":btpsimple_%s_configuration" % (chains[1]),
            "@%s//:bmr_config_dir" % (chains[1]),
            "@%s//cli:keysecret" % chains[0],
        ],
    )
