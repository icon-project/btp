def create_account(name):
    native.genrule(
        name = "account_key_%s" % name,
        outs = ["account_key_%s.out" % name],
        cmd = "echo \"%s_tmp\" > $@" % name,
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
        cmd = "$(execpath :near_binary) send node0 $$(cat $(location @near//cli:encode_public_key_%s)) 50 --masterAccount node0 --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --keyPath ~/.near/localnet/node0/validator_key.json > $@" % name,
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

def create_sub_account(name):
    native.genrule(
        name = "create_sub_account_%s" % name,
        outs = ["create_sub_account_%s.out" % name],
        cmd = "$(execpath :near_binary) create-account %s.node0 --masterAccount node0 --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --keyPath ~/.near/localnet/node0/validator_key.json; $(execpath :near_binary) send node0 %s.node0 50 --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --keyPath ~/.near/localnet/node0/validator_key.json ; echo '%s.node0' > $@" % (name, name, name),
        executable = True,
        local = True,
        output_to_bindir = True,
        tools = [
            "@near//:wait_until_near_up",
            "@near//cli:near_binary",
        ],
    )

def configure_link(name):
    native.genrule(
        name = "add_%s_verifier" % name,
        srcs = ["@near//cli:deploy_%s_bmv" % name, "@near//cli:deploy_bmc"],
        outs = ["add_%s_verifier.out" % name],
        cmd = """$(execpath :near_binary) call $$(cat $(location @near//cli:encode_public_key_bmc)) add_verifier \\'\\{\\"network\\":\\"$$(cat $(location @%s//:network_address))\\"\\,\\"verifier\\":\\"$$(cat $(location @near//cli:encode_public_key_%sbmv))\\"\\}\\' --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --accountId $$(cat $(location @near//cli:encode_public_key_bmc)) > $@""" % (name, name),
        executable = True,
        local = True,
        tools = [
            "@%s//:network_address" % name,
            "@near//:wait_until_near_up",
            "@near//cli:encode_public_key_bmc",
            "@near//cli:encode_public_key_%sbmv" % name,
            "@near//cli:near_binary",
        ],
    )
    native.genrule(
        name = "add_%s_link" % name,
        srcs = [
            "@near//cli:deploy_bmc",
            "@near//cli:add_%s_verifier" % name,
        ],
        outs = ["add_%s_link.out" % name],
        cmd = """$(execpath :near_binary) call $$(cat $(location @near//cli:encode_public_key_bmc)) add_link \\'\\{\\"link\\":\\"$$(cat $(location @%s//:btp_address))\\"\\}\\' --nodeUrl $$(cat $(locations @near//:wait_until_near_up)) --accountId $$(cat $(location @near//cli:encode_public_key_bmc)) > $@""" % name,
        executable = True,
        local = True,
        tools = [
            "@%s//:btp_address" % name,
            "@near//:wait_until_near_up",
            "@near//cli:encode_public_key_bmc",
            "@near//cli:near_binary",
        ],
    )
