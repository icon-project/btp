package mock

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"strconv"

	"github.com/icon-project/btp/chain/near/types"
)

const mockDataPath = "./testdata/mock/data"

func loadFiles(files []string, directory string) [][]byte {
	var fileBuffers = make([][]byte, 0)
	for _, f := range files {
		file, err := os.Open(directory + "/" + f + ".json")

		if err != nil {
			panic(fmt.Errorf("error [LoadFile]: %v", err))
		}

		buffer, err := ioutil.ReadAll(file)

		if err != nil {
			panic(fmt.Errorf("error [ReadFile]: %v", err))
		}

		fileBuffers = append(fileBuffers, buffer)
		defer file.Close()
	}
	return fileBuffers
}

func validateDirectory(directory string) {
	_, err := ioutil.ReadDir(directory)
	if err != nil {
		panic(fmt.Errorf("error [LoadBlock]: %v", err))
	}
}

func LoadBlockFromFile(names []string) (map[int64]Response, map[string]Response) {
	sectionDir := mockDataPath + "/blocks"
	validateDirectory(sectionDir)

	blockByHashMap := map[string]Response{}
	blockByHeightMap := map[int64]Response{}

	for _, buffer := range loadFiles(names, sectionDir) {
		var block types.Block
		err := json.Unmarshal(buffer, &block)
		if err != nil {
			panic(fmt.Errorf("error [LoadBlock][ParseJson]: %v", err))
		}

		blockByHashMap[block.Header.Hash.Base58Encode()] = Response{
			Reponse: buffer,
			Error:   nil,
		}

		blockByHeightMap[block.Header.Height] = Response{
			Reponse: buffer,
			Error:   nil,
		}
	}

	return blockByHeightMap, blockByHashMap
}

func LoadNonceFromFile(names []string) map[string]Response {
	sectionDir := mockDataPath + "/nonce"
	validateDirectory(sectionDir)

	var nonceMap = map[string]Response{}

	for index, buffer := range loadFiles(names, sectionDir) {
		var nonce types.NonceResponse

		err := json.Unmarshal(buffer, &nonce)
		if err != nil {
			panic(fmt.Errorf("error [LoadBlock][ParseJson]: %v", err))
		}
		nonceMap[names[index]] = Response{
			Reponse: nonce.Nonce,
			Error:   nil,
		}
	}

	return nonceMap
}

func LoadBmcStatusFromFile(names []string) map[string]Response {
	sectionDir := mockDataPath + "/contractsdata/bmc"
	validateDirectory(sectionDir)

	var bmcStatusMap = map[string]Response{}

	for index, buffer := range loadFiles(names, sectionDir) {
		var bmcstatus types.BmcStatus
		err := json.Unmarshal(buffer, &bmcstatus)
		if err != nil {
			panic(fmt.Errorf("error [LoadBlock][ParseJson]: %v", err))
		}

		bmcStatusMap[names[index]] = Response{
			Reponse: buffer,
			Error:   nil,
		}
	}

	return bmcStatusMap
}

func LoadBmvStatusFromFile(names []string) map[string]Response {
	sectionDir := mockDataPath + "/contractsdata/bmv"
	validateDirectory(sectionDir)

	var bmvStatusMap = map[string]Response{}

	for index, buffer := range loadFiles(names, sectionDir) {
		var bmvstatus types.BmvStatus
		err := json.Unmarshal(buffer, &bmvstatus)
		if err != nil {
			panic(fmt.Errorf("error [LoadBlock][ParseJson]: %v", err))
		}

		bmvStatusMap[names[index]] = Response{
			Reponse: buffer,
			Error:   nil,
		}
	}
	return bmvStatusMap
}

func LoadEventsFromFile(names []string) map[int64]Response {
	sectionDir := mockDataPath + "/events"
	validateDirectory(sectionDir)

	var getEventsMap = map[int64]Response{}
	for index, buffer := range loadFiles(names, sectionDir) {
		var contractStateChange types.ContractStateChange

		err := json.Unmarshal(buffer, &contractStateChange)
		if err != nil {
			panic(fmt.Errorf("error [LoadBlock][ParseJson]: %v", err))
		}

		blockHeight, err := strconv.Atoi(names[index])
		if err != nil {
			panic(fmt.Errorf("error [LoadBlock][ParseJson]: %v", err))
		}
		getEventsMap[int64(blockHeight)] = Response{
			Reponse: buffer,
			Error:   nil,
		}
	}
	return getEventsMap
}

func LoadReceiptsFromFile(names []string) map[string]Response {
	sectionDir := mockDataPath + "/receipts"
	validateDirectory(sectionDir)
	var receiptProofMap = map[string]Response{}
	for index, buffer := range loadFiles(names, sectionDir) {
		var receiptProofs types.ReceiptProof

		err := json.Unmarshal(buffer, &receiptProofs)
		if err != nil {
			panic(fmt.Errorf("error [LoadBlock][ParseJson]: %v", err))
		}

		receiptProofMap[names[index]] = Response{
			Reponse: buffer,
			Error:   nil,
		}
	}
	return receiptProofMap
}

func LoadTransactionResultFromFile(names []string) map[string]Response {
	sectionDir := mockDataPath + "/transaction"
	validateDirectory(sectionDir)

	var transactionResultMap = map[string]Response{}
	for index, buffer := range loadFiles(names, sectionDir) {
		var transactionResult types.TransactionResult

		err := json.Unmarshal(buffer, &transactionResult)
		if err != nil {
			panic(fmt.Errorf("error [LoadBlock][ParseJson]: %v", err))
		}

		transactionResultMap[names[index]] = Response{
			Reponse: buffer,
			Error:   nil,
		}
	}
	return transactionResultMap
}

func LoadLightClientBlocksFromFile(names []string) map[string]Response {
	sectionDir := mockDataPath + "/lightclient_blocks"
	validateDirectory(sectionDir)

	var LightClientBlockMap = map[string]Response{}
	for index, buffer := range loadFiles(names, sectionDir) {
		var LightClientBlock types.NextBlockProducers

		err := json.Unmarshal(buffer, &LightClientBlock)
		if err != nil {
			panic(fmt.Errorf("error [LoadLightClientBlock][ParseJson]: %v", err))
		}

		LightClientBlockMap[names[index]] = Response{
			Reponse: buffer,
			Error:   nil,
		}
	}
	return LightClientBlockMap
}
