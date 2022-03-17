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
        cmd = "mv ~/.near-credentials/" + select({"@near//:localnet": "local", "@near//:testnet": "testnet"}) + "/$$(cat $(location @near//cli:account_key_%s)).json  ~/.near-credentials/" % name + select({"@near//:localnet": "local", "@near//:testnet": "testnet"}) + "/$$(cat $(location @near//cli:encode_public_key_%s)).json;  echo 'copied' > $@" % name,
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
        cmd = "$(execpath :near_binary) send $$(cat $(location @near//cli:get_master_account)) $$(cat $(location @near//cli:encode_public_key_%s)) 50 --masterAccount $$(cat $(location @near//cli:get_master_account)) --nodeUrl $$(cat $(locations @near//:node_url)) --keyPath $$(cat $(location @near//:near_config_dir))/master_key.json > $@" % name,
        executable = True,
        local = True,
        output_to_bindir = True,
        tools = [
            "@near//:node_url",
            "@near//cli:near_binary",
            "@near//cli:encode_public_key_%s" % name,
            "@near//cli:rename_account_%s" % name,
            "@near//:near_config_dir",
            "@near//cli:get_master_account"
        ],
    )

def create_sub_account(name):
    native.genrule(
        name = "create_sub_account_%s" % name,
        outs = ["create_sub_account_%s.out" % name],
        cmd = "$(execpath :near_binary) create-account %s.$$(cat $(location @near//cli:get_master_account)) --masterAccount $$(cat $(location @near//cli:get_master_account)) --nodeUrl $$(cat $(locations @near//:node_url)) --keyPath $$(cat $(location @near//:near_config_dir))/master_key.json; $(execpath :near_binary) send $$(cat $(location :@near//cli:get_master_account)) %s.$$(cat $(location :@near//cli:get_master_account)) 50 --nodeUrl $$(cat $(locations @near//:node_url)) --keyPath $$(cat $(location @near//:near_config_dir))/master_key.json ; echo '%s.$$(cat $(location :@near//cli:get_master_account))' > $@" % (name, name, name),
        executable = True,
        local = True,
        output_to_bindir = True,
        tools = [
            "@near//:node_url",
            "@near//cli:near_binary",
            "@near//:near_config_dir",
            "@near//cli:get_master_account"
        ],
    )

def configure_link(name):
    native.genrule(
        name = "add_%s_verifier" % name,
        srcs = ["@near//cli:deploy_%s_bmv" % name, "@near//cli:deploy_bmc"],
        outs = ["add_%s_verifier.out" % name],
        cmd = """$(execpath :near_binary) call $$(cat $(location @near//cli:encode_public_key_bmc)) add_verifier \\'\\{\\"network\\":\\"$$(cat $(location @%s//:network_address))\\"\\,\\"verifier\\":\\"$$(cat $(location @near//cli:encode_public_key_%sbmv))\\"\\}\\' --nodeUrl $$(cat $(locations @near//:node_url)) --accountId $$(cat $(location @near//cli:encode_public_key_bmc)) > $@""" % (name, name),
        executable = True,
        local = True,
        tools = [
            "@%s//:network_address" % name,
            "@near//:node_url",
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
        cmd = """$(execpath :near_binary) call $$(cat $(location @near//cli:encode_public_key_bmc)) add_link \\'\\{\\"link\\":\\"$$(cat $(location @%s//:btp_address))\\"\\}\\' --nodeUrl $$(cat $(locations @near//:node_url)) --accountId $$(cat $(location @near//cli:encode_public_key_bmc)) > $@""" % name,
        executable = True,
        local = True,
        tools = [
            "@%s//:btp_address" % name,
            "@near//:node_url",
            "@near//cli:encode_public_key_bmc",
            "@near//cli:near_binary",
        ],
    )

def configure_bmr(name):
    native.genrule(
        name = "generate_%s_keystore" % name,
        srcs = [
            "@com_github_hugobyte_keygen//:keygen",
            "@near//cli:keysecret",
        ],
        outs = ["generate_%s_keystore.json" % name],
        cmd = "$(execpath @com_github_hugobyte_keygen//:keygen) generate -p $$(cat $(location :keysecret)) -o $@",
        executable = True,
        local = True,
    )
    native.genrule(
        name = "get_wallet_%s_keystore" % name,
        srcs = [
            "@near//cli:generate_%s_keystore" % name,
        ],
        outs = ["get_wallet_%s_keystore.out" % name],
        cmd = "echo \"$$(cat $(location :generate_%s_keystore))\" | jq .address | echo $$(tr -d '\"') >$@" % name,
        executable = True,
        local = True,
    )
    native.genrule(
        name = "transfer_amount_%s_address" % name,
        srcs = [
            "@near//cli:get_wallet_%s_keystore" % name,
            "@near//cli:generate_%s_keystore" % name,
            "@near//:near_config_dir",
        ],
        outs = ["%s_keystore.json" % name],
        cmd = "$(execpath @near//cli:near_binary) send $$(cat $(location @near//cli:get_master_account)) $$(cat $(location @near//cli:get_wallet_%s_keystore)) 50 --masterAccount $$(cat $(location @near//cli:get_master_account)) --nodeUrl $$(cat $(locations @near//:node_url)) --keyPath $$(cat $(location @near//:near_config_dir))/master_key.json; echo \"$$(cat $(location @near//cli:generate_%s_keystore))\"| jq -r . > $@" % (name, name),
        executable = True,
        local = True,
        tools = [
            "@near//:node_url",
            "@near//cli:near_binary",
            "@near//cli:get_master_account"
        ],
    )
