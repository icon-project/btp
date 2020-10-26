package common

import (
	"regexp"
	"strings"
)

func StrLeft(n int, s string) string {
	if len(s) > n {
		return string([]byte(s)[0:n])
	}
	return s
}

var (
	firstCap = regexp.MustCompile("(.)([A-Z][a-z]+)")
	allCap   = regexp.MustCompile("([a-z0-9])([A-Z])")
	underCap = regexp.MustCompile("(^[A-Za-z])|_([A-Za-z])")
)

func StrToSnakeCase(str string) string {
	snake := firstCap.ReplaceAllString(str, "${1}_${2}")
	snake = allCap.ReplaceAllString(snake, "${1}_${2}")
	return strings.ToLower(snake)
}

func StrToCamelCase(str string) string {
	return underCap.ReplaceAllStringFunc(str, func(s string) string {
		return strings.ToUpper(strings.Replace(s, "_", "", -1))
	})
}