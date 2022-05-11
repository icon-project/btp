package mock

const (
	LatestBlockHeight = 377840
	LatestBlockHash   = "CE3NVJbb5tStSPXNhuNEJc1UkHbuGdeL9UBFFcBrxHx3"
)

var Blocks = []string{"377825"}
var Nonce = []string{"94a5a3fc9bc948a7f4b1c6210518b4afe1744ebe33188eb91d17c863dfe200a8"}
var BmcLinkStatus = []string{"0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5"}
var BmvLinkStatus = []string{"dev-20211205172325-28827597417784"}
var GetEvents = []string{"377825", "377826"}
var GetReceiptsProof = []string{"2VWWEfyg5BzyDBVRsHRXApQJdZ37Bdtj8GtkH7UvNm7G"}
var TransactionResult = []string{"6zgh2u9DqHHiXzdy9ouTP7oGky2T4nugqzqt9wJZwNFm"}

func Default() Storage {
	blockByHeightMap, blockByHashMap := LoadBlockFromFile(Blocks)
	nonceMap := LoadNonceFromFile(Nonce)
	bmcLinkStatusMap := LoadBmcStatusFromFile(BmcLinkStatus)
	bmvLinkStatusMap := LoadBmvStatusFromFile(BmvLinkStatus)
	contractStateChangeMap := LoadEventsFromFile(GetEvents)
	receiptProofMap := LoadReceiptsFromFile(GetReceiptsProof)
	transactionHashMap := Response{
		Reponse: "6zgh2u9DqHHiXzdy9ouTP7oGky2T4nugqzqt9wJZwNFm",
		Error:   nil,
	}
	transactionResultMap := LoadTransactionResultFromFile(TransactionResult)

	return Storage{
		LatestBlockHeight: Response{
			Reponse: LatestBlockHeight,
			Error:   nil,
		},
		LatestBlockHash: Response{
			Reponse: LatestBlockHash,
			Error:   nil,
		},
		BlockByHeightMap:       blockByHeightMap,
		BlockByHashMap:         blockByHashMap,
		NonceMap:               nonceMap,
		BmcLinkStatusMap:       bmcLinkStatusMap,
		BmvStatusMap:           bmvLinkStatusMap,
		ContractStateChangeMap: contractStateChangeMap,
		ReceiptProofMap:        receiptProofMap,
		TransactionHash:        transactionHashMap,
		TransactionResultMap:   transactionResultMap,
	}
}
