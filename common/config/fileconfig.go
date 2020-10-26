package config

import (
	"encoding/json"
	"io/ioutil"
	"os"
	"path"
	"path/filepath"
)

type FileConfig struct {
	BaseDir  string `json:"base_dir,omitempty"`
	FilePath string `json:"-"` //absolute path
}

func (c *FileConfig) ResolveAbsolute(targetPath string) string {
	if filepath.IsAbs(targetPath) {
		return targetPath
	}
	if c.FilePath == "" {
		r, _ := filepath.Abs(targetPath)
		return r
	}
	return filepath.Clean(path.Join(filepath.Dir(c.FilePath), targetPath))
}

func (c *FileConfig) ResolveRelative(targetPath string) string {
	absPath, _ := filepath.Abs(targetPath)
	base := filepath.Dir(c.FilePath)
	base, _ = filepath.Abs(base)
	r, _ := filepath.Rel(base, absPath)
	return r
}

func (c *FileConfig) AbsBaseDir() string {
	return c.ResolveAbsolute(c.BaseDir)
}

func (c *FileConfig) SetFilePath(targetPath string) {
	c.FilePath = targetPath
}


type fileConfig interface {
	ResolveAbsolute(targetPath string) string
	SetFilePath(targetPath string)
}

func Save(cfg interface{}, filename string) error {
	targetPath := filename
	c, ok := cfg.(fileConfig)
	if ok && targetPath != ""{
		targetPath = c.ResolveAbsolute(targetPath)
	}

	f, err := os.OpenFile(targetPath, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0644)
	if err != nil {
		return err
	}
	defer func() {
		_ = f.Close()
	}()
	enc := json.NewEncoder(f)
	enc.SetIndent("", "  ")
	if err := enc.Encode(cfg); err != nil {
		return err
	}
	if ok {
		c.SetFilePath(targetPath)
	}
	return nil
}

func Load(cfg interface{}, filename string) error {
	targetPath := filename
	c, ok := cfg.(fileConfig)
	if ok && targetPath != ""{
		targetPath = c.ResolveAbsolute(targetPath)
	}

	b, err := ioutil.ReadFile(targetPath)
	if err != nil {
		return err
	}
	if err = json.Unmarshal(b, cfg); err != nil {
		return err
	}
	if ok {
		c.SetFilePath(targetPath)
	}
	return nil
}