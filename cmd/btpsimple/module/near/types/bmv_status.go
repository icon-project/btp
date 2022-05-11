package types

type BmvStatus struct {
	Height     int64 `json:"mta_height"`
	Offset     int64 `json:"mta_offset"`
	LastHeight int64 `json:"last_height"`
}