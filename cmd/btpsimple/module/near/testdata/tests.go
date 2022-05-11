package testdata

import (
	"fmt"
	"testing"

	"github.com/icon-project/btp/cmd/btpsimple/module/near/testdata/mock"
	"github.com/stretchr/testify/assert"
)

type Test interface {
	Description() string
	TestDatas() []TestData
}

type TestData struct {
	Description string
	Input       interface{}
	Expected    struct {
		Success interface{}
		Fail    interface{}
	}
	MockStorage mock.Storage
}

var Tests = map[string]Test{}

func RegisterTest(module string, test Test) {
	Tests[module] = test
}

func GetTest(module string, t *testing.T) (Test, error) {
	err := fmt.Errorf("not supported test:%s", module)
	if test := Tests[module]; test != nil {
		return test, nil
	}

	assert.NoError(t, err)
	return nil, err
}
