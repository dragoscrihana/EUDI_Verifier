#!/usr/bin/env python3
from web3 import Web3
from dotenv import load_dotenv
import base64
import os
import json

load_dotenv()
RPC_URL = os.getenv("RPC_URL")
PRIVATE_KEY = os.getenv("PRIVATE_KEY")

# ✅ Hardcoded values from your input
CONTRACT_ADDRESS = "0x84a83892cb0608c5dfDa5EBD7398d2c3EfF0988d"
ISSUER_ADDRESS = "0xC887f232c81c4609CF98857c6Fe55FDE8d24f418"
SAVE_METHOD = 0

# Paste the ABI JSON directly below
ABI_JSON = json.dumps([
    {
        "anonymous": False,
        "inputs": [
            {"indexed": True, "internalType": "address", "name": "issuer", "type": "address"},
            {"indexed": True, "internalType": "enum CRLRegistry.SaveMethod", "name": "saveMethod", "type": "uint8"},
            {"indexed": False, "internalType": "string", "name": "pointerHash", "type": "string"},
            {"indexed": False, "internalType": "uint256", "name": "expiresAt", "type": "uint256"},
            {"indexed": False, "internalType": "uint256", "name": "version", "type": "uint256"}
        ],
        "name": "CRLPublished",
        "type": "event"
    },
    {
        "inputs": [],
        "name": "NAME",
        "outputs": [{"internalType": "string", "name": "", "type": "string"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [],
        "name": "VERSION",
        "outputs": [{"internalType": "string", "name": "", "type": "string"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [
            {"internalType": "address", "name": "", "type": "address"},
            {"internalType": "enum CRLRegistry.SaveMethod", "name": "", "type": "uint8"}
        ],
        "name": "crls",
        "outputs": [
            {"internalType": "enum CRLRegistry.SaveMethod", "name": "saveMethod", "type": "uint8"},
            {"internalType": "string", "name": "pointerHash", "type": "string"},
            {"internalType": "uint256", "name": "expiresAt", "type": "uint256"},
            {"internalType": "uint256", "name": "version", "type": "uint256"}
        ],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [
            {"internalType": "address", "name": "issuer", "type": "address"},
            {"internalType": "enum CRLRegistry.SaveMethod", "name": "saveMethod", "type": "uint8"}
        ],
        "name": "getCRL",
        "outputs": [
            {"internalType": "string", "name": "pointerHash", "type": "string"},
            {"internalType": "uint256", "name": "expiresAt", "type": "uint256"},
            {"internalType": "uint256", "name": "version", "type": "uint256"},
            {"internalType": "bool", "name": "isValid", "type": "bool"}
        ],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [{"internalType": "address", "name": "", "type": "address"}],
        "name": "issuers",
        "outputs": [{"internalType": "bool", "name": "", "type": "bool"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [
            {"internalType": "enum CRLRegistry.SaveMethod", "name": "saveMethod", "type": "uint8"},
            {"internalType": "string", "name": "pointerHash", "type": "string"},
            {"internalType": "uint256", "name": "validityHours", "type": "uint256"}
        ],
        "name": "publishCRL",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function"
    }
])

def get_crl():
    if not RPC_URL or not PRIVATE_KEY:
        raise Exception("Missing RPC_URL or PRIVATE_KEY in .env")

    w3 = Web3(Web3.HTTPProvider(RPC_URL))
    if not w3.is_connected():
        raise Exception("Could not connect to the Holesky RPC")

    contract = w3.eth.contract(address=Web3.to_checksum_address(CONTRACT_ADDRESS), abi=json.loads(ABI_JSON))

    try:
        result = contract.functions.getCRL(ISSUER_ADDRESS, SAVE_METHOD).call()
        return {
            "pointerHash": result[0],
            "expiresAt": result[1],
            "version": result[2],
            "isValid": result[3]
        }
    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    crl_data = get_crl()
    print(json.dumps(crl_data, indent=2))
