package bsc

import (
	"bytes"
	"fmt"
	"math/big"
	"testing"

	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/crypto"
	"github.com/ethereum/go-ethereum/ethdb/memorydb"
	"github.com/ethereum/go-ethereum/light"
	"github.com/ethereum/go-ethereum/rlp"
	"github.com/ethereum/go-ethereum/trie"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/bsc/binding"

	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
)

// LogProof contains everything that's necessary
// to verify the inclusion of a log in a block's
// receipts trie, on chain.
type LogProof struct {
	Value    []byte `json:"value"`
	Proof    []byte `json:"proof"`
	Key      []byte `json:"key"`
	Header   []byte `json:"header"`
	LogIndex uint   `json:"logIndex"`
}

func GenerateReceiptProof_(height int64) ([]*chain.ReceiptProof, error) {
	client := NewClient("http://localhost:8545", log.New())

	rps := make([]*chain.ReceiptProof, 0)

	trieDB := trie.NewDatabase(memorydb.New())
	trieObj, _ := trie.New(common.Hash{}, trieDB) // empty trie

	block, err := client.GetBlockByHeight(big.NewInt(height))
	if err != nil {
		return nil, err
	}

	receipts, err := client.GetBlockReceipts(block)
	if err != nil {
		return nil, err
	}

	srcContractAddress := HexToAddress("0xAaFc8EeaEE8d9C8bD3262CCE3D73E56DeE3FB776")

	var idx uint

	for _, receipt := range receipts {
		rp := &chain.ReceiptProof{}

		i, _ := codec.RLP.MarshalToBytes(receipt.TransactionIndex)
		proof, err := codec.RLP.MarshalToBytes(*MakeReceipt(receipt))

		if err != nil {
			return nil, err
		}

		trieObj.Update(i, proof)

		for _, eventLog := range receipt.Logs {
			if eventLog.Address != srcContractAddress {
				continue
			}
			if bmcMsg, err := binding.UnpackEventLog(eventLog.Data); err == nil {
				rp.Events = append(rp.Events, &chain.Event{
					Message:  bmcMsg.Msg,
					Next:     chain.BtpAddress(bmcMsg.Next),
					Sequence: bmcMsg.Seq,
				})
			}
			proof, err := codec.RLP.MarshalToBytes(*MakeLog(eventLog))
			if err != nil {
				return nil, err
			}
			rp.EventProofs = append(rp.EventProofs, &chain.EventProof{
				Index: int(eventLog.Index),
				Proof: proof,
			})
		}

		if len(rp.Events) > 0 {
			//fmt.Println("newReceiptProofs:", rp.Events[0].Message)
			rp.Index = int(receipt.TransactionIndex)
			rp.Proof, err = codec.RLP.MarshalToBytes(*MakeReceipt(receipt))
			if err != nil {
				return nil, err
			}
			rps = append(rps, rp)
			idx = receipt.TransactionIndex
		}

	}

	_, err = trieObj.Commit(nil)
	if err != nil {
		return nil, err
	}

	path := []byte{byte(idx)}
	receiptProof := Proof(trieObj, path[:])
	fmt.Println(receiptProof)

	return rps, nil
}

func GenerateReceiptProof1(height int64) error {
	client := NewClient("http://localhost:8545", log.New())

	trieDB := trie.NewDatabase(memorydb.New())
	trieObj, _ := trie.New(common.Hash{}, trieDB) // empty trie

	block, err := client.GetBlockByHeight(big.NewInt(height))
	if err != nil {
		return err
	}

	receipts, err := client.GetBlockReceipts(block)
	if err != nil {
		return err
	}

	for _, receipt := range receipts {
		i, _ := codec.RLP.MarshalToBytes(receipt.TransactionIndex)
		proof, err := codec.RLP.MarshalToBytes(*MakeReceipt(receipt))

		if err != nil {
			return err
		}
		trieObj.Update(i, proof)
	}

	_, err = trieObj.Commit(nil)
	if err != nil {
		return err
	}

	path := []byte{byte(1)}
	receiptProof := Proof(trieObj, path[:])
	fmt.Println(receiptProof)

	return nil
}

func GenerateReceiptProof(height int64) (*LogProof, error) {
	client := NewClient("http://localhost:8545", log.New())

	block, err := client.GetBlockByHeight(big.NewInt(height))
	if err != nil {
		return nil, err
	}
	receipts, err := client.GetBlockReceipts(block)
	if err != nil {
		return nil, err
	}

	receiptTrie, err := trieFromReceipts(receipts)
	if err != nil {
		return nil, err
	}

	fmt.Println(receiptTrie.Hash())
	fmt.Println(block.ReceiptHash())

	// Generate MTP.
	proof := memorydb.New()

	key, err := rlp.EncodeToBytes(*big.NewInt(0))
	if err != nil {
		fmt.Println(err)
	}

	//buf := new(bytes.Buffer)
	//err = rlp.Encode(buf, uint(0))
	//fmt.Println(buf.Bytes(), key)

	nodes := light.NewNodeSet()
	err = receiptTrie.Prove(key, 0, nodes)

	//nodeList := nodes.NodeList()
	verifyProof, err := trie.VerifyProof(block.ReceiptHash(), key, nodes)
	if err != nil {
		fmt.Println(err)
	}
	fmt.Println("verify: ", verifyProof)
	var receipt types.Receipt
	if err := rlp.DecodeBytes(verifyProof, &receipt); err != nil {
		fmt.Println(err)
	}

	/*keyBuf.Reset()
	err = rlp.Encode(keyBuf, uint(txIndex))
	if err != nil {
		return nil, 0, fmt.Errorf("rlp encode returns an error: %v", err)
	}
	Logger.Println("Start proving receipt trie...")
	err = receiptTrie.Prove(keyBuf.Bytes(), 0, proof)
	if err != nil {
		return nil, 0, err
	}
	Logger.Println("Finish proving receipt trie.")*/

	proofs := make([]interface{}, 0)
	if it := trie.NewIterator(receiptTrie.NodeIterator(key)); it.Next() && bytes.Equal(key, it.Key) {
		for _, p := range it.Prove() {
			err := proof.Put(crypto.Keccak256(p), p)
			if err != nil {
				return nil, err
			}
			var decoded interface{}
			if err := rlp.DecodeBytes(p, &decoded); err != nil {
				return nil, err
			}

			proofs = append(proofs, decoded)
		}
	}

	proofBytes, err := rlp.EncodeToBytes(proofs)
	if err != nil {
		return nil, err
	}

	receiptBytes, err := rlp.EncodeToBytes(receipts[0])
	if err != nil {
		return nil, err
	}

	headerRaw, err := rlp.EncodeToBytes(block.Header())

	logProof := &LogProof{
		Value:    receiptBytes,
		Proof:    proofBytes,
		Key:      key,
		Header:   headerRaw,
		LogIndex: uint(0), // TODO: Fix log index (l.Index returns weird number)
	}

	return logProof, nil
}

func VerifyLogProof(p *LogProof) (bool, error) {
	var header types.Header
	if err := rlp.DecodeBytes(p.Header, &header); err != nil {
		return false, err
	}

	var proofNodes [][]byte
	//proofs := make([]interface{}, 0)
	if err := rlp.DecodeBytes(p.Proof, &proofNodes); err != nil {
		return false, err
	}

	proof := memorydb.New()
	for _, n := range proofNodes {
		err := proof.Put(crypto.Keccak256(n), n)
		if err != nil {
			return false, err
		}
	}
	// Recover rlp encoded receipt from proof and receipt
	// trie root.
	receiptRaw, err := trie.VerifyProof(header.ReceiptHash, p.Key, proof)
	lRaw, err := logFromReceipt(receiptRaw, p.LogIndex)
	if err != nil {
		return false, err
	}

	if !bytes.Equal(lRaw, p.Value) {
		fmt.Printf("invalid proof: computed leaf doesn't match\n")
		return false, nil
	}

	return true, nil
}

func logFromReceipt(r []byte, i uint) ([]byte, error) {
	var receipt types.Receipt
	if err := rlp.DecodeBytes(r, &receipt); err != nil {
		return nil, err
	}

	// Extract log from receipt.
	l := receipt.Logs[i]
	lRawBuf := new(bytes.Buffer)
	if err := l.EncodeRLP(lRawBuf); err != nil {
		return nil, err
	}

	return lRawBuf.Bytes(), nil
}

func TestReceiver_GetReceiptProof(t *testing.T) {
	GenerateReceiptProof(772)

	//fmt.Println(VerifyLogProof(proof))
}

func encodeReceipt(r *types.Receipt) ([]byte, error) {
	buf := new(bytes.Buffer)
	if err := r.EncodeRLP(buf); err != nil {
		return nil, err
	}

	return buf.Bytes(), nil
}

// Proof creates an array of the proof pathj ordered
func Proof(trie *trie.Trie, path []byte) []byte {
	proof := generateProof(trie, path)
	proofRLP, err := rlp.EncodeToBytes(proof)
	if err != nil {
		log.Fatal("ERROR encoding proof: ", err)
	}
	return proofRLP
}

func generateProof(trie *trie.Trie, path []byte) []interface{} {
	proof := memorydb.New()
	err := trie.Prove(path, 0, proof)
	if err != nil {
		log.Fatal("ERROR failed to create proof")
	}

	var proofArr []interface{}
	for nodeIt := trie.NodeIterator(nil); nodeIt.Next(true); {
		if val, err := proof.Get(nodeIt.Hash().Bytes()); val != nil && err == nil {
			var decodedVal interface{}
			err = rlp.DecodeBytes(val, &decodedVal)
			if err != nil {
				log.Fatalf("ERROR(%s) failed decoding RLP: 0x%0x\n", err, val)
			}
			proofArr = append(proofArr, decodedVal)
		}
	}
	return proofArr
}
