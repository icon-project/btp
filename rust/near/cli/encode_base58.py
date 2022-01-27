import base58
import sys

input = sys.argv[1]
input = input.split("ed25519:")[1].split(" ")[0]
print(base58.b58decode(input).hex())