def chain_repositories():
    native.local_repository(
        name = "near",
        path = "./rust/near",
    )

    native.local_repository(
        name = "icon",
        path = "./javascore/icon",
    )