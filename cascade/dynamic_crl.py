from web3 import Web3
from dotenv import load_dotenv
import base64
import os
import json

load_dotenv()
RPC_URL = os.getenv("RPC_URL")
PRIVATE_KEY = os.getenv("PRIVATE_KEY")

def get_crl(contract_address: str, abi_json: str, issuer_address: str):
    if not RPC_URL or not PRIVATE_KEY:
        raise Exception("Missing RPC_URL or PRIVATE_KEY in .env")

    w3 = Web3(Web3.HTTPProvider(RPC_URL))
    if not w3.is_connected():
        raise Exception("Could not connect to the Holesky RPC")

    abi = json.loads(abi_json)
    contract = w3.eth.contract(address=Web3.to_checksum_address(contract_address), abi=abi)

    crl_abi = next((item for item in abi if item.get("name") == "getCRL" and item.get("type") == "function"), None)
    if not crl_abi:
        raise Exception("Function 'getCRL' not found in ABI")

    output_fields = [o["name"] for o in crl_abi["outputs"]]

    try:
        result = contract.functions.getCRL(issuer_address).call()
        return dict(zip(output_fields, result))
    except Exception as e:
        raise Exception(f"Error while calling getCRL: {e}")

if __name__ == "__main__":
    import sys

    if len(sys.argv) != 4:
        print("Usage: python dynamic_crl.py <contract_address> <abi_json> <issuer_address>")
        sys.exit(1)

    contract_address = sys.argv[1]
    
    abi_b64 = sys.argv[2]
    abi_json = base64.b64decode(abi_b64).decode('utf-8')
    abi = json.loads(abi_json)

    issuer_address = sys.argv[3]

    try:
        crl_data = get_crl(contract_address, abi_json, issuer_address)
        print(json.dumps(crl_data, indent=2))
    except Exception as err:
        print(str(err))
