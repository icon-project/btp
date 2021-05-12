// SPDX-License-Identifier: Apache-2.0

/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pragma solidity >=0.6.0 <=0.8.5;
pragma abicoder v2;

contract MerkleTreeAccumulator {

  struct Node {
    State state;
    NodeType nodeType;
    bytes data;
    bytes32 hash;
    bytes32 leftHash;
    bytes32 rightHash;
  }

  enum State {
    NIL,
    DIRTY,
    HASHED,
    FLUSHED
  }

  enum NodeType {
    DATA,
    BRANCH
  }

  struct Witness {
    Direction direction;
    bytes32 hashValue;
  }

  enum Direction {
    LEFT,
    RIGHT
  }

  Node[] roots;
  mapping(bytes32=>Node) left;
  mapping(bytes32=>Node) right;
  uint256 length;
  mapping(bytes32 => Witness) witnessList;

  function addData(bytes calldata data) public returns (Witness[] memory) {
    Node memory node = Node(State.DIRTY, NodeType.DATA, data, 0, 0, 0);
    Witness[] memory w = addNode(node);
    return w;
  }

  function addNode(Node memory node) internal returns (Witness[] memory) {
    return _addNode(0, node, new Witness[](roots.length));
  }

  function _addNode(uint256 h, Node memory node, Witness[] memory w) internal returns(Witness[] memory) {
    if (h >= roots.length) {
      roots.push(node);
      length += 1;
      return w;
    }

    bytes32 rh;

    if (roots[h].state != State.NIL) {
       rh = nodeHash(roots[h]);
    }

    if(rh == 0){
      roots[h] = node;
      length += 1;
      return w;
    } else {
      Witness[] memory _w = new Witness[](w.length+1);
      for(uint i=0; i < w.length; i++) {
        _w[i] = w[i];
      }
      _w[w.length] = Witness(Direction.LEFT, rh);
      Node memory root = roots[h];
      delete roots[h];
      Node memory branch = Node(State.DIRTY, NodeType.BRANCH, new bytes(0), 0, root.hash, node.hash);
      left[root.hash] = root;
      right[node.hash] = node;
      //nodeHash( right[node.hash] );
      return _addNode(h+1, branch, _w);
    }
  }

  function nodeHash(Node storage node) internal returns(bytes32) {
    if (node.state == State.NIL){
      return 0;
    }
    if (node.nodeType == NodeType.DATA && node.state != State.HASHED){
      node.state = State.HASHED;
      node.hash = keccak256(abi.encodePacked(node.data));
      return node.hash;
    }
    if (node.state != State.HASHED) {
      Node storage leftNode = left[node.leftHash];
      Node storage rightNode = right[node.rightHash];
      node.leftHash = nodeHash(leftNode);
      node.rightHash = nodeHash(rightNode);
      right[node.rightHash] = rightNode;
       //right[node.rightHash] =
      node.hash = keccak256(abi.encodePacked(node.leftHash,  node.rightHash));
      node.state = State.HASHED;
      return node.hash;
    }
    return node.hash;
  }

  function getRoot() public view returns (bytes32) {
    return roots[0].hash;
  }

  function getNode(uint256 idx) public view returns (Node memory) {
    require(idx <= length, "Invalid index");
    uint256 offset = roots.length;

    while(offset > 0){
      offset -= 1;
      if(roots[offset].state == State.NIL){
        continue;
      }
      uint256 inbound = 1 << uint(offset);
      if(idx < inbound) {
         return getNode(roots[offset], offset, idx);
      }
      idx -= inbound;
    }
    return left[0];
  }

  function getNode(Node memory node, uint256 depth, uint256 idx) public view returns (Node memory){
     if (node.nodeType == NodeType.BRANCH){
          if (depth < 1){
            return node;
          }
          uint256 bound = 1 << uint(depth-1);
          if(idx < bound){
            return getNode(left[node.leftHash], depth-1, idx);
          } else {
            return getNode(right[node.rightHash], depth-1, idx-bound);
          }
        }
      if (node.nodeType == NodeType.DATA) {
        require(depth <= 0, "Invalid depth");
        return node;
      }
  }

  function getLength() public view returns (uint256) {
    return length;
  }

}
