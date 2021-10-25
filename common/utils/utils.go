package module

import (
	"os"
	"path"

	"golang.org/x/mod/module"
)

func GetModulePath(name, version string) (string, error) {
	cache, ok := os.LookupEnv("GOMODCACHE")
	if !ok {
		cache = path.Join(os.Getenv("GOPATH"), "pkg", "mod")
	}

	escapedPath, err := module.EscapePath(name)
	if err != nil {
		return "", err
	}

	escapedVersion, err := module.EscapeVersion(version)
	if err != nil {
		return "", err
	}

	return path.Join(cache, escapedPath+"@"+escapedVersion), nil
}
