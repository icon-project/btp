def create_account(name):
    native.genrule(
        name = "account_key_%s" % name,
        outs = ["account_key_%s.out" % name],
        cmd = "echo 'tmp' > $@",
        executable = True,
    )

    native.genrule(
        name = "generate_key_%s" % name,
        outs = ["generate_key_%s.out" % name],
        cmd = "$(execpath :near_binary) generate-key $$(cat $(locations @near//cli:account_key_%s)) > $@" % name,
        executable = True,
        local = True,
        tools = [
            "@near//cli:account_key_%s" % name,
            "@near//cli:near_binary",
        ],
    )
    native.genrule(
        name = "encode_public_key_%s" % name,
        outs = ["encode_public_key_%s.out" % name],
        cmd = "$(location @near//cli:encode_base58) \"$$(cat $(location @near//cli:generate_key_%s))\" > $@" % name,
        executable = True,
        output_to_bindir = True,
        tools = [
            "@near//cli:generate_key_%s" % name,
            "@near//cli:encode_base58",
        ],
    )

    native.genrule(
        name = "rename_account_%s" % name,
        outs = ["rename_account_%s.out" % name],
        cmd = "mv ~/.near-credentials/local/$$(cat $(location @near//cli:account_key_%s)).json  ~/.near-credentials/local/$$(cat $(location @near//cli:encode_public_key_%s)).json;  echo 'copied' > $@" % (name, name),
        executable = True,
        local = True,
        output_to_bindir = True,
        tools = [
            "@near//cli:encode_public_key_%s" % name,
            "@near//cli:account_key_%s" % name,
        ],
    )

    native.genrule(
        name = "create_account_%s" % name,
        outs = ["create_account_%s.out" % name],
        cmd = "$(execpath :near_binary) send node0 $$(cat $(location @near//cli:encode_public_key_%s)) 10 --masterAccount node0 --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --keyPath ~/.near/localnet/node0/validator_key.json > $@" % name,
        executable = True,
        local = True,
        output_to_bindir = True,
        tools = [
            "@near//:wait_until_near_up",
            "@near//cli:near_binary",
            "@near//cli:encode_public_key_%s" % name,
            "@near//cli:rename_account_%s" % name,
        ],
    )
