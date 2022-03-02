package mock

type Response struct {
	Reponse interface{}
	Error   error
}

type Storage struct {
	LatestBlockHeight                Response
	LatestBlockHash                  Response
	BmcLinkStatusMap                 map[string]Response
	BmvStatusMap                     map[string]Response
	NonceMap                         map[string]Response
	BlockByHashMap                   map[string]Response
	ReceiptProofMap                  map[string]Response
	BlockByHeightMap                 map[int64]Response
	LightClientBlockMap              map[string]Response
	ContractStateChangeMap           map[int64]Response
	TransactionHash                  Response
	TransactionResultMap             map[string]Response
}