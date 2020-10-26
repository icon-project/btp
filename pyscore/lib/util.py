def with_def_val(l: list, idx: int, def_val):
    if len(l) > idx:
        return l[idx]
    else:
        return def_val
