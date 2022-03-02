package test_data


var MockContractStateChange = map[int64][]byte{

	377825: []byte(`{
        "block_hash": "DDbjZ12VbmV36trcJDPxAAHsDWTtGEC9DB6ZSVLE9N1c",
        "changes": [
            {
                "cause": {
                    "receipt_hash": "2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G",
                    "type": "receipt_processing"
                },
                "change": {
                    "account_id": "alice.node1",
                    "key_base64": "bWVzc2FnZQ==",
                    "value_base64": "cAEAAHsibmV4dCI6ImJ0cDovLzB4MS5pY29uL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1YiIsInNlcXVlbmNlIjoiMCIsIm1lc3NhZ2UiOiIrTW1hWW5Sd09pOHZNSGd4TG01bFlYSXZZV3hwWTJVdWJtOWtaVEc0T1dKMGNEb3ZMekI0TVM1cFkyOXVMMk40T0RkbFpEa3dORGhpTlRrMFlqazFNVGs1WmpNeU5tWmpOelpsTnpaaE9XUXpNMlJrTmpZMVlvTmliV09CZ0xocitHa1Z1R1pDVFVOU1pYWmxjblJWYm5KbFlXTm9ZV0pzWlNCaGRDQmlkSEE2THk4d2VEVXVjSEpoTHpnNFltUXdOVFEwTWpZNE5tSmxNR0UxWkdZM1pHRXpNMkkyWmpFd09EbGxZbVpsWVRNM05qbGlNVGxrWW1JeU5EYzNabVV3WTJRMlpUQm1NVEkyWlRRPSJ9"
                },
                "type": "data_update"
            },
            {
                "cause": {
                    "receipt_hash": "3QQeZHZxs8N4gVhoj8nAKrMQfN1L3n4jU7wSAULoww8y",
                    "type": "receipt_processing"
                },
                "change": {
                    "account_id": "alice.node1",
                    "key_base64": "bWVzc2FnZQ==",
                    "value_base64": "cAEAAHsibmV4dCI6ImJ0cDovLzB4MS5pY29uL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1YiIsInNlcXVlbmNlIjoiMCIsIm1lc3NhZ2UiOiIrTW1hWW5Sd09pOHZNSGd4TG01bFlYSXZZV3hwWTJVdWJtOWtaVEc0T1dKMGNEb3ZMekI0TVM1cFkyOXVMMk40T0RkbFpEa3dORGhpTlRrMFlqazFNVGs1WmpNeU5tWmpOelpsTnpaaE9XUXpNMlJrTmpZMVlvTmliV09CZ0xocitHa1Z1R1pDVFVOU1pYWmxjblJWYm5KbFlXTm9ZV0pzWlNCaGRDQmlkSEE2THk4d2VEVXVjSEpoTHpnNFltUXdOVFEwTWpZNE5tSmxNR0UxWkdZM1pHRXpNMkkyWmpFd09EbGxZbVpsWVRNM05qbGlNVGxrWW1JeU5EYzNabVV3WTJRMlpUQm1NVEkyWlRRPSJ9"
                },
                "type": "data_update"
            }
        ]
    }`),
	377826: []byte(`
    {
        "block_hash": "5YqjrSoiQjqrmrMHQJVZB25at7yQ2BZEC2exweLFmc6w",
        "changes": [
            {
                "cause": {
                    "receipt_hash": "2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G",
                    "type": "receipt_processing"
                },
                "change": {
                    "account_id": "alice.node1",
                    "key_base64": "bWVzc2FnZQ==",
                    "value_base64": "cAEAAHsibmV4dCI6ImJ0cDovLzB4MS5pY29uL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1YiIsInNlcXVlbmNlIjoiMCIsIm1lc3NhZ2UiOiIrTW1hWW5Sd09pOHZNSGd4TG01bFlYSXZZV3hwWTJVdWJtOWtaVEc0T1dKMGNEb3ZMekI0TVM1cFkyOXVMMk40T0RkbFpEa3dORGhpTlRrMFlqazFNVGs1WmpNeU5tWmpOelpsTnpaaE9XUXpNMlJrTmpZMVlvTmliV09CZ0xocitHa1Z1R1pDVFVOU1pYWmxjblJWYm5KbFlXTm9ZV0pzWlNCaGRDQmlkSEE2THk4d2VEVXVjSEpoTHpnNFltUXdOVFEwTWpZNE5tSmxNR0UxWkdZM1pHRXpNMkkyWmpFd09EbGxZbVpsWVRNM05qbGlNVGxrWW1JeU5EYzNabVV3WTJRMlpUQm1NVEkyWlRRPSJ9"
                },
                "type": "data_update"
            },
            {
                "cause": {
                    "receipt_hash": "3QQeZHZxs8N4gVhoj8nAKrMQfN1L3n4jU7wSAULoww8y",
                    "type": "receipt_processing"
                },
                "change": {
                    "account_id": "alice.node1",
                    "key_base64": "bWVzc2FnZQ==",
                    "value_base64": "cAEAAHsibmV4dCI6ImJ0cDovLzB4MS5pY29uL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1YiIsInNlcXVlbmNlIjoiMCIsIm1lc3NhZ2UiOiIrTW1hWW5Sd09pOHZNSGd4TG01bFlYSXZZV3hwWTJVdWJtOWtaVEc0T1dKMGNEb3ZMekI0TVM1cFkyOXVMMk40T0RkbFpEa3dORGhpTlRrMFlqazFNVGs1WmpNeU5tWmpOelpsTnpaaE9XUXpNMlJrTmpZMVlvTmliV09CZ0xocitHa1Z1R1pDVFVOU1pYWmxjblJWYm5KbFlXTm9ZV0pzWlNCaGRDQmlkSEE2THk4d2VEVXVjSEpoTHpnNFltUXdOVFEwTWpZNE5tSmxNR0UxWkdZM1pHRXpNMkkyWmpFd09EbGxZbVpsWVRNM05qbGlNVGxrWW1JeU5EYzNabVV3WTJRMlpUQm1NVEkyWlRRPSJ9"
                },
                "type": "data_update"
            }
        ]
    }`),
	377827: []byte(`
    {
        "block_hash": "9HhNTVZBu7sBFFsn4dCFnJehVXvMUJ8TDLXDffRJa1hz",
        "changes": [
            {
                "cause": {
                    "receipt_hash": "2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G",
                    "type": "receipt_processing"
                },
                "change": {
                    "account_id": "alice.node1",
                    "key_base64": "bWVzc2FnZQ==",
                    "value_base64": "cAEAAHsibmV4dCI6ImJ0cDovLzB4MS5pY29uL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1YiIsInNlcXVlbmNlIjoiMCIsIm1lc3NhZ2UiOiIrTW1hWW5Sd09pOHZNSGd4TG01bFlYSXZZV3hwWTJVdWJtOWtaVEc0T1dKMGNEb3ZMekI0TVM1cFkyOXVMMk40T0RkbFpEa3dORGhpTlRrMFlqazFNVGs1WmpNeU5tWmpOelpsTnpaaE9XUXpNMlJrTmpZMVlvTmliV09CZ0xocitHa1Z1R1pDVFVOU1pYWmxjblJWYm5KbFlXTm9ZV0pzWlNCaGRDQmlkSEE2THk4d2VEVXVjSEpoTHpnNFltUXdOVFEwTWpZNE5tSmxNR0UxWkdZM1pHRXpNMkkyWmpFd09EbGxZbVpsWVRNM05qbGlNVGxrWW1JeU5EYzNabVV3WTJRMlpUQm1NVEkyWlRRPSJ9"
                },
                "type": "data_update"
            },
            {
                "cause": {
                    "receipt_hash": "3QQeZHZxs8N4gVhoj8nAKrMQfN1L3n4jU7wSAULoww8y",
                    "type": "receipt_processing"
                },
                "change": {
                    "account_id": "alice.node1",
                    "key_base64": "bWVzc2FnZQ==",
                    "value_base64": "cAEAAHsibmV4dCI6ImJ0cDovLzB4MS5pY29uL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1YiIsInNlcXVlbmNlIjoiMCIsIm1lc3NhZ2UiOiIrTW1hWW5Sd09pOHZNSGd4TG01bFlYSXZZV3hwWTJVdWJtOWtaVEc0T1dKMGNEb3ZMekI0TVM1cFkyOXVMMk40T0RkbFpEa3dORGhpTlRrMFlqazFNVGs1WmpNeU5tWmpOelpsTnpaaE9XUXpNMlJrTmpZMVlvTmliV09CZ0xocitHa1Z1R1pDVFVOU1pYWmxjblJWYm5KbFlXTm9ZV0pzWlNCaGRDQmlkSEE2THk4d2VEVXVjSEpoTHpnNFltUXdOVFEwTWpZNE5tSmxNR0UxWkdZM1pHRXpNMkkyWmpFd09EbGxZbVpsWVRNM05qbGlNVGxrWW1JeU5EYzNabVV3WTJRMlpUQm1NVEkyWlRRPSJ9"
                },
                "type": "data_update"
            }
        ]
    }`),
	377828: []byte(`
    {
        "block_hash": "4CSFBudwkUAgoHHQNh6UnVx78EP9ubeUhbc9ZHoC5w4u",
        "changes": [
            {
                "cause": {
                    "receipt_hash": "2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G",
                    "type": "receipt_processing"
                },
                "change": {
                    "account_id": "alice.node1",
                    "key_base64": "bWVzc2FnZQ==",
                    "value_base64": "cAEAAHsibmV4dCI6ImJ0cDovLzB4MS5pY29uL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1YiIsInNlcXVlbmNlIjoiMCIsIm1lc3NhZ2UiOiIrTW1hWW5Sd09pOHZNSGd4TG01bFlYSXZZV3hwWTJVdWJtOWtaVEc0T1dKMGNEb3ZMekI0TVM1cFkyOXVMMk40T0RkbFpEa3dORGhpTlRrMFlqazFNVGs1WmpNeU5tWmpOelpsTnpaaE9XUXpNMlJrTmpZMVlvTmliV09CZ0xocitHa1Z1R1pDVFVOU1pYWmxjblJWYm5KbFlXTm9ZV0pzWlNCaGRDQmlkSEE2THk4d2VEVXVjSEpoTHpnNFltUXdOVFEwTWpZNE5tSmxNR0UxWkdZM1pHRXpNMkkyWmpFd09EbGxZbVpsWVRNM05qbGlNVGxrWW1JeU5EYzNabVV3WTJRMlpUQm1NVEkyWlRRPSJ9"
                },
                "type": "data_update"
            },
            {
                "cause": {
                    "receipt_hash": "3QQeZHZxs8N4gVhoj8nAKrMQfN1L3n4jU7wSAULoww8y",
                    "type": "receipt_processing"
                },
                "change": {
                    "account_id": "alice.node1",
                    "key_base64": "bWVzc2FnZQ==",
                    "value_base64": "cAEAAHsibmV4dCI6ImJ0cDovLzB4MS5pY29uL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1YiIsInNlcXVlbmNlIjoiMCIsIm1lc3NhZ2UiOiIrTW1hWW5Sd09pOHZNSGd4TG01bFlYSXZZV3hwWTJVdWJtOWtaVEc0T1dKMGNEb3ZMekI0TVM1cFkyOXVMMk40T0RkbFpEa3dORGhpTlRrMFlqazFNVGs1WmpNeU5tWmpOelpsTnpaaE9XUXpNMlJrTmpZMVlvTmliV09CZ0xocitHa1Z1R1pDVFVOU1pYWmxjblJWYm5KbFlXTm9ZV0pzWlNCaGRDQmlkSEE2THk4d2VEVXVjSEpoTHpnNFltUXdOVFEwTWpZNE5tSmxNR0UxWkdZM1pHRXpNMkkyWmpFd09EbGxZbVpsWVRNM05qbGlNVGxrWW1JeU5EYzNabVV3WTJRMlpUQm1NVEkyWlRRPSJ9"
                },
                "type": "data_update"
            }
        ]
    }`),
	377829: []byte(`
    {
        "block_hash": "3m8kMqwyMqVSkaFn4LrGim4qy6RdNVVxx3ebp11btHFd",
        "changes": [
            {
                "cause": {
                    "receipt_hash": "2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G",
                    "type": "receipt_processing"
                },
                "change": {
                    "account_id": "alice.node1",
                    "key_base64": "bWVzc2FnZQ==",
                    "value_base64": "cAEAAHsibmV4dCI6ImJ0cDovLzB4MS5pY29uL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1YiIsInNlcXVlbmNlIjoiMCIsIm1lc3NhZ2UiOiIrTW1hWW5Sd09pOHZNSGd4TG01bFlYSXZZV3hwWTJVdWJtOWtaVEc0T1dKMGNEb3ZMekI0TVM1cFkyOXVMMk40T0RkbFpEa3dORGhpTlRrMFlqazFNVGs1WmpNeU5tWmpOelpsTnpaaE9XUXpNMlJrTmpZMVlvTmliV09CZ0xocitHa1Z1R1pDVFVOU1pYWmxjblJWYm5KbFlXTm9ZV0pzWlNCaGRDQmlkSEE2THk4d2VEVXVjSEpoTHpnNFltUXdOVFEwTWpZNE5tSmxNR0UxWkdZM1pHRXpNMkkyWmpFd09EbGxZbVpsWVRNM05qbGlNVGxrWW1JeU5EYzNabVV3WTJRMlpUQm1NVEkyWlRRPSJ9"
                },
                "type": "data_update"
            },
            {
                "cause": {
                    "receipt_hash": "3QQeZHZxs8N4gVhoj8nAKrMQfN1L3n4jU7wSAULoww8y",
                    "type": "receipt_processing"
                },
                "change": {
                    "account_id": "alice.node1",
                    "key_base64": "bWVzc2FnZQ==",
                    "value_base64": "cAEAAHsibmV4dCI6ImJ0cDovLzB4MS5pY29uL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1YiIsInNlcXVlbmNlIjoiMCIsIm1lc3NhZ2UiOiIrTW1hWW5Sd09pOHZNSGd4TG01bFlYSXZZV3hwWTJVdWJtOWtaVEc0T1dKMGNEb3ZMekI0TVM1cFkyOXVMMk40T0RkbFpEa3dORGhpTlRrMFlqazFNVGs1WmpNeU5tWmpOelpsTnpaaE9XUXpNMlJrTmpZMVlvTmliV09CZ0xocitHa1Z1R1pDVFVOU1pYWmxjblJWYm5KbFlXTm9ZV0pzWlNCaGRDQmlkSEE2THk4d2VEVXVjSEpoTHpnNFltUXdOVFEwTWpZNE5tSmxNR0UxWkdZM1pHRXpNMkkyWmpFd09EbGxZbVpsWVRNM05qbGlNVGxrWW1JeU5EYzNabVV3WTJRMlpUQm1NVEkyWlRRPSJ9"
                },
                "type": "data_update"
            }
        ]
    }`),
}

var MockStatChangeProof = map[string][]byte{

	"2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G": []byte(`
    {
        "block_header_lite": {
            "inner_lite": {
                "block_merkle_root": "HZsF2tVjoVnVQBokGNYyUCdc8cvWbfe8bCy9R94fdCnq",
                "epoch_id": "FpxMgCMY533wxSQv7GRjre2qemdzBS6wHpfptRQCXFeb",
                "height": 70096077,
                "next_bp_hash": "DVNXiaDSyscYNXVJTtUrr7do83jfeo85GpYBoEVF2uyC",
                "next_epoch_id": "6b8WJEkQnqNARSpFKqTHJBWrNcbcARdE6QANqY8Bj48g",
                "outcome_root": "9MwPzDjanVv8oTk3Tb2wSDL8SfeG7UrUbFAUVWXg12i3",
                "prev_state_root": "B2ssTi9H76esSxSDKFyx5hxCnYBL3wXZP5dzMj9cNf3z",
                "timestamp": 1636079303784029966,
                "timestamp_nanosec": "1636079303784029966"
            },
            "inner_rest_hash": "7PV4ygtbfh81Vnou7qz3GhnZpNvf5eBMBK3aTKMWqrHa",
            "prev_block_hash": "7FMoS5Q7WQ8hEYz4EANoTxzhmWQ8ywfFiQgWiby6c5bx"
        },
        "block_proof": [],
        "outcome_proof": {
            "block_hash": "DqDHTSWjqfzoEnYRjbJw44rUcmjA7tq1YuoVYRxFoZQh",
            "id": "2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G",
            "outcome": {
                "executor_id": "alice.node1",
                "gas_burnt": 6484927719095,
                "logs": [],
                "metadata": {
                    "gas_profile": [
                        {
                            "cost": "BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "7148738997"
                        },
                        {
                            "cost": "CONTRACT_COMPILE_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "35445963"
                        },
                        {
                            "cost": "CONTRACT_COMPILE_BYTES",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "94777405500"
                        },
                        {
                            "cost": "READ_MEMORY_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "26098632000"
                        },
                        {
                            "cost": "READ_MEMORY_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "3930578322"
                        },
                        {
                            "cost": "WRITE_MEMORY_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "25234153749"
                        },
                        {
                            "cost": "WRITE_MEMORY_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "3037005780"
                        },
                        {
                            "cost": "READ_REGISTER_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "20137321488"
                        },
                        {
                            "cost": "READ_REGISTER_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "108319638"
                        },
                        {
                            "cost": "WRITE_REGISTER_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "28655224860"
                        },
                        {
                            "cost": "WRITE_REGISTER_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "6508277568"
                        },
                        {
                            "cost": "STORAGE_WRITE_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "192590208000"
                        },
                        {
                            "cost": "STORAGE_WRITE_KEY_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "1832554542"
                        },
                        {
                            "cost": "STORAGE_WRITE_VALUE_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "26520850845"
                        },
                        {
                            "cost": "STORAGE_WRITE_EVICTED_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "26817951345"
                        },
                        {
                            "cost": "STORAGE_READ_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "225427383000"
                        },
                        {
                            "cost": "STORAGE_READ_KEY_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "4735737549"
                        },
                        {
                            "cost": "STORAGE_READ_VALUE_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "2799891495"
                        },
                        {
                            "cost": "TOUCHING_TRIE_NODE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "2544109036308"
                        }
                    ],
                    "version": 1
                },
                "receipt_ids": [
                    "G4BqecLxdFY6jZBCQgToePR8bhtsiW1NaEUJLwAZ5aJg"
                ],
                "status": {
                    "SuccessValue": ""
                },
                "tokens_burnt": "648492771909500000000"
            },
            "proof": [
                {
                    "direction": "Right",
                    "hash": "DGBvNMANQH73V55xYSZ9eZtqKkGR9WaYqUpSssdGSjMW"
                },
                {
                    "direction": "Right",
                    "hash": "FDJGYqs9aCdGzJrkGHiLXgzPoFm7bcMiSrAfLcZuyVkB"
                }
            ]
        },
        "outcome_root_proof": [
            {
                "direction": "Right",
                "hash": "7tkzFg8RHBmMw1ncRJZCCZAizgq4rwCftTKYLce8RU8t"
            },
            {
                "direction": "Right",
                "hash": "4A9zZ1umpi36rXiuaKYJZgAjhUH9WoTrnSBXtA3wMdV2"
            }
        ]
    }`),
	"3QQeZHZxs8N4gVhoj8nAKrMQfN1L3n4jU7wSAULoww8y": []byte(`
    {
        "block_header_lite": {
            "inner_lite": {
                "block_merkle_root": "HZsF2tVjoVnVQBokGNYyUCdc8cvWbfe8bCy9R94fdCnq",
                "epoch_id": "FpxMgCMY533wxSQv7GRjre2qemdzBS6wHpfptRQCXFeb",
                "height": 70096077,
                "next_bp_hash": "DVNXiaDSyscYNXVJTtUrr7do83jfeo85GpYBoEVF2uyC",
                "next_epoch_id": "6b8WJEkQnqNARSpFKqTHJBWrNcbcARdE6QANqY8Bj48g",
                "outcome_root": "9MwPzDjanVv8oTk3Tb2wSDL8SfeG7UrUbFAUVWXg12i3",
                "prev_state_root": "B2ssTi9H76esSxSDKFyx5hxCnYBL3wXZP5dzMj9cNf3z",
                "timestamp": 1636079303784029966,
                "timestamp_nanosec": "1636079303784029966"
            },
            "inner_rest_hash": "7PV4ygtbfh81Vnou7qz3GhnZpNvf5eBMBK3aTKMWqrHa",
            "prev_block_hash": "7FMoS5Q7WQ8hEYz4EANoTxzhmWQ8ywfFiQgWiby6c5bx"
        },
        "block_proof": [],
        "outcome_proof": {
            "block_hash": "DqDHTSWjqfzoEnYRjbJw44rUcmjA7tq1YuoVYRxFoZQh",
            "id": "3QQeZHZxs8N4gVhoj8nAKrMQfN1L3n4jU7wSAULoww8y",
            "outcome": {
                "executor_id": "alice.node1",
                "gas_burnt": 4714430944655,
                "logs": [],
                "metadata": {
                    "gas_profile": [
                        {
                            "cost": "BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "7148738997"
                        },
                        {
                            "cost": "CONTRACT_COMPILE_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "35445963"
                        },
                        {
                            "cost": "CONTRACT_COMPILE_BYTES",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "94777405500"
                        },
                        {
                            "cost": "READ_MEMORY_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "26098632000"
                        },
                        {
                            "cost": "READ_MEMORY_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "3930578322"
                        },
                        {
                            "cost": "WRITE_MEMORY_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "25234153749"
                        },
                        {
                            "cost": "WRITE_MEMORY_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "3037005780"
                        },
                        {
                            "cost": "READ_REGISTER_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "20137321488"
                        },
                        {
                            "cost": "READ_REGISTER_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "108319638"
                        },
                        {
                            "cost": "WRITE_REGISTER_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "28655224860"
                        },
                        {
                            "cost": "WRITE_REGISTER_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "6584308848"
                        },
                        {
                            "cost": "STORAGE_WRITE_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "192590208000"
                        },
                        {
                            "cost": "STORAGE_WRITE_KEY_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "1832554542"
                        },
                        {
                            "cost": "STORAGE_WRITE_VALUE_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "26520850845"
                        },
                        {
                            "cost": "STORAGE_WRITE_EVICTED_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "27460297485"
                        },
                        {
                            "cost": "STORAGE_READ_BASE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "225427383000"
                        },
                        {
                            "cost": "STORAGE_READ_KEY_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "4735737549"
                        },
                        {
                            "cost": "STORAGE_READ_VALUE_BYTE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "2799891495"
                        },
                        {
                            "cost": "TOUCHING_TRIE_NODE",
                            "cost_category": "WASM_HOST_COST",
                            "gas_used": "772893884448"
                        }
                    ],
                    "version": 1
                },
                "receipt_ids": [
                    "Ft7WQS7xkBn2ZSSdkZDh2zaR5Py2m3RJq4mbf1Xusrhx"
                ],
                "status": {
                    "SuccessValue": ""
                },
                "tokens_burnt": "471443094465500000000"
            },
            "proof": [
                {
                    "direction": "Left",
                    "hash": "uE2tugECSK9CFJ8FDNt6LhyrABR3oNdzxkn6M1cuypL"
                },
                {
                    "direction": "Right",
                    "hash": "FDJGYqs9aCdGzJrkGHiLXgzPoFm7bcMiSrAfLcZuyVkB"
                }
            ]
        },
        "outcome_root_proof": [
            {
                "direction": "Right",
                "hash": "7tkzFg8RHBmMw1ncRJZCCZAizgq4rwCftTKYLce8RU8t"
            },
            {
                "direction": "Right",
                "hash": "4A9zZ1umpi36rXiuaKYJZgAjhUH9WoTrnSBXtA3wMdV2"
            }
        ]
    }`),
}

var MockcallContractFunctionData = map[string][]byte{

	"dev-20211206025826-24100687319598": []byte(`
    {
        "rx_seq": 0,
        "tx_seq": 1,
        "verifier": "dev-20211205172325-28827597417784",
        "relays": [
          {
            "account_id": "dev-20211205172426-13718608403006",
            "block_count": 0,
            "message_count": 0
          }
        ],
        "relay_index": 0,
        "rotate_height": 73935506,
        "rotate_term": 5,
        "delay_limit": 4,
        "rx_height_src": 1846537,
        "rx_height": 73935501,
        "block_interval_dst": 15000,
        "block_interval_src": 1500,
        "current_height": 73998396
      }`),

	"dev-20211205172325-28827597417784": []byte(`{
        "mta_height": 1846537,
        "mta_offset": 0,
        "last_height": 1846537
      }`),
}

var MockGetNonce = map[string]int64{

	"69c003c3b80ed12ea02f5c67c9e8167f0ce3b2e8020a0f43b1029c4d787b0d21": 28,
}

var MockTransactionResult = map[string][]byte{

	"6zgh2u9DqHHiXzdy9ouTP7oGky2T4nugqzqt9wJZwNFm": []byte(`
    {
        "receipts_outcome": [
            {
                "block_hash": "6K8SzhwtzZ6wrrTmbKcNCfvqVW5xuEze6yFZBMeWh3a7",
                "id": "GLRBG8eXbdn7UFQUm9QuBwgEVB772XmBAK4XXyg8Tykg",
                "outcome": {
                    "executor_id": "alice.node0",
                    "gas_burnt": 3280025539544,
                    "logs": [],
                    "metadata": {
                        "gas_profile": [],
                        "version": 1
                    },
                    "receipt_ids": [],
                    "status": {
                        "SuccessValue": ""
                    },
                    "tokens_burnt": "328002553954400000000"
                },
                "proof": [
                    {
                        "direction": "Left",
                        "hash": "98evkBr3E3o2PukJMp4eqkDT7ssEL77kMgBgEcWYQjtz"
                    }
                ]
            }
        ],
        "status": {
            "SuccessValue": ""
        },
        "transaction": {
            "actions": [
                {
                    "DeployContract": {
                        "code": "hctiB5FxZn0CHrTuOkJXOjw0y3whj0uv/6DCCtUw0EI="
                    }
                }
            ],
            "hash": "689ziDgHzcxCVdfeBvUimay3TGteJy6CbyFbT3i8t4At",
            "nonce": 66835160000040,
            "public_key": "ed25519:5rsVhAhEqJTYn1Nfzf1hZMBPgX4VBXRT3RuA9kui6M4n",
            "receiver_id": "alice.node0",
            "signature": "ed25519:2tw21Gh6zCFgrYs4hBVUHuRxDA4G4kLj3vxWMwCtqxbn5AU7Zq7jwd7C9QAAzr3WbkFfM4YaFzhwUaJwE2gKA592",
            "signer_id": "alice.node0"
        },
        "transaction_outcome": {
            "block_hash": "6K8SzhwtzZ6wrrTmbKcNCfvqVW5xuEze6yFZBMeWh3a7",
            "id": "689ziDgHzcxCVdfeBvUimay3TGteJy6CbyFbT3i8t4At",
            "outcome": {
                "executor_id": "alice.node0",
                "gas_burnt": 3280025539544,
                "logs": [],
                "metadata": {
                    "gas_profile": null,
                    "version": 1
                },
                "receipt_ids": [
                    "GLRBG8eXbdn7UFQUm9QuBwgEVB772XmBAK4XXyg8Tykg"
                ],
                "status": {
                    "SuccessReceiptId": "GLRBG8eXbdn7UFQUm9QuBwgEVB772XmBAK4XXyg8Tykg"
                },
                "tokens_burnt": "328002553954400000000"
            },
            "proof": [
                {
                    "direction": "Right",
                    "hash": "4iNBvPejsXuHbeacZv6kPWY91UxN7ft18cBtiVfCSB4h"
                }
            ]
        }
    }
    `),

	"CotTthJuKeUiEPuRG2SuZaEHw28adZGbkCJsSdnN1cMt": []byte(` {
        "receipts_outcome": [
            {
                "block_hash": "7iAm1t1A7s2bQrqqNH3mVLTwTE3dvA9MWoy1PQqHoKmh",
                "id": "6UZoXvuhNDZW4P1sTXJjzJ9E7HH7MyBufynVsrTfhkJU",
                "outcome": {
                    "executor_id": "alice.node0",
                    "gas_burnt": 6379401533341,
                    "logs": [],
                    "metadata": {
                        "gas_profile": null,
                        "version": 1
                    },
                    "receipt_ids": [
                        "CYnH1J9sHrAUpQuvvCN56kkavNxQtxYrrBm8oiTU9i3T"
                    ],
                    "status": {
                        "SuccessValue": ""
                    },
                    "tokens_burnt": "637940153334100000000"
                },
                "proof": [
                    {
                        "direction": "Left",
                        "hash": "7MBU3mrbam7WJuy9wbfVNFeYzPGYoDitLdqefPgRZNAk"
                    }
                ]
            },
            {
                "block_hash": "Hzm4pMPood5uJwqwUjutugMmzZCuk3iD3bmi87rP7fHG",
                "id": "CYnH1J9sHrAUpQuvvCN56kkavNxQtxYrrBm8oiTU9i3T",
                "outcome": {
                    "executor_id": "alice.node0",
                    "gas_burnt": 223182562500,
                    "logs": [],
                    "metadata": {
                        "gas_profile": null,
                        "version": 1
                    },
                    "receipt_ids": [],
                    "status": {
                        "SuccessValue": ""
                    },
                    "tokens_burnt": "0"
                },
                "proof": []
            }
        ],
        "status": {
            "SuccessValue": ""
        },
        "transaction": {
            "actions": [
                {
                    "FunctionCall": {
                        "args": "eyJsaW5rIjoiYnRwOi8vMHgxLmljb24vY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjViIn0=",
                        "deposit": "0",
                        "gas": 30000000000000,
                        "method_name": "add_link"
                    }
                }
            ],
            "hash": "CotTthJuKeUiEPuRG2SuZaEHw28adZGbkCJsSdnN1cMt",
            "nonce": 66835160000006,
            "public_key": "ed25519:5rsVhAhEqJTYn1Nfzf1hZMBPgX4VBXRT3RuA9kui6M4n",
            "receiver_id": "alice.node0",
            "signature": "ed25519:3rN9Zchf3GPUxYm2GQihppWTCX3drBeDBjmLL1P1Hg83x8Y4HnUxD3QvKHVNTKLysgFWiNDFy6PdJXgWBCUHKP7i",
            "signer_id": "alice.node0"
        },
        "transaction_outcome": {
            "block_hash": "7iAm1t1A7s2bQrqqNH3mVLTwTE3dvA9MWoy1PQqHoKmh",
            "id": "CotTthJuKeUiEPuRG2SuZaEHw28adZGbkCJsSdnN1cMt",
            "outcome": {
                "executor_id": "alice.node0",
                "gas_burnt": 2428090930984,
                "logs": [],
                "metadata": {
                    "gas_profile": null,
                    "version": 1
                },
                "receipt_ids": [
                    "6UZoXvuhNDZW4P1sTXJjzJ9E7HH7MyBufynVsrTfhkJU"
                ],
                "status": {
                    "SuccessReceiptId": "6UZoXvuhNDZW4P1sTXJjzJ9E7HH7MyBufynVsrTfhkJU"
                },
                "tokens_burnt": "242809093098400000000"
            },
            "proof": [
                {
                    "direction": "Right",
                    "hash": "7pS35PNmmbWqQKpXA3NPsrq7y1ASwPmVfVhhuqVbXfNV"
                }
            ]
        }
    }`),
}

var MockTransactionResultWithRecipts = map[string][]byte{

	"6zgh2u9DqHHiXzdy9ouTP7oGky2T4nugqzqt9wJZwNFm": []byte(`{
        "receipts": [],
        "receipts_outcome": [
            {
                "block_hash": "6K8SzhwtzZ6wrrTmbKcNCfvqVW5xuEze6yFZBMeWh3a7",
                "id": "GLRBG8eXbdn7UFQUm9QuBwgEVB772XmBAK4XXyg8Tykg",
                "outcome": {
                    "executor_id": "alice.node0",
                    "gas_burnt": 3280025539544,
                    "logs": [],
                    "metadata": {
                        "gas_profile": [],
                        "version": 1
                    },
                    "receipt_ids": [],
                    "status": {
                        "SuccessValue": ""
                    },
                    "tokens_burnt": "328002553954400000000"
                },
                "proof": [
                    {
                        "direction": "Left",
                        "hash": "98evkBr3E3o2PukJMp4eqkDT7ssEL77kMgBgEcWYQjtz"
                    }
                ]
            }
        ],
        "status": {
            "SuccessValue": ""
        },
        "transaction": {
            "actions": [
                {
                    "DeployContract": {
                        "code": "hctiB5FxZn0CHrTuOkJXOjw0y3whj0uv/6DCCtUw0EI="
                    }
                }
            ],
            "hash": "689ziDgHzcxCVdfeBvUimay3TGteJy6CbyFbT3i8t4At",
            "nonce": 66835160000040,
            "public_key": "ed25519:5rsVhAhEqJTYn1Nfzf1hZMBPgX4VBXRT3RuA9kui6M4n",
            "receiver_id": "alice.node0",
            "signature": "ed25519:2tw21Gh6zCFgrYs4hBVUHuRxDA4G4kLj3vxWMwCtqxbn5AU7Zq7jwd7C9QAAzr3WbkFfM4YaFzhwUaJwE2gKA592",
            "signer_id": "alice.node0"
        },
        "transaction_outcome": {
            "block_hash": "6K8SzhwtzZ6wrrTmbKcNCfvqVW5xuEze6yFZBMeWh3a7",
            "id": "689ziDgHzcxCVdfeBvUimay3TGteJy6CbyFbT3i8t4At",
            "outcome": {
                "executor_id": "alice.node0",
                "gas_burnt": 3280025539544,
                "logs": [],
                "metadata": {
                    "gas_profile": null,
                    "version": 1
                },
                "receipt_ids": [
                    "GLRBG8eXbdn7UFQUm9QuBwgEVB772XmBAK4XXyg8Tykg"
                ],
                "status": {
                    "SuccessReceiptId": "GLRBG8eXbdn7UFQUm9QuBwgEVB772XmBAK4XXyg8Tykg"
                },
                "tokens_burnt": "328002553954400000000"
            },
            "proof": [
                {
                    "direction": "Right",
                    "hash": "4iNBvPejsXuHbeacZv6kPWY91UxN7ft18cBtiVfCSB4h"
                }
            ]
        }
    }`),
}