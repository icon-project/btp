package pra

import "errors"

var (
	ErrBlockNotReady = errors.New("required result to be 32 bytes, but got 0")
)
