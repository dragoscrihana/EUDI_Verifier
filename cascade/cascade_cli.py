#!/usr/bin/env python3
import argparse
import json
import base64
import sys
import requests
import contextlib
import io
from bloom_cascade import Cascade  # or use your own Cascade class

def load_cascade_from_ipfs(cascade_data_b64: str):
    serialized = base64.b64decode(cascade_data_b64)
    csd = Cascade()
    exp = csd.deserialize_cascade(serialized)
    return csd, exp

def load_cascade_from_blob(tx_hash: str):
    tx_url = f"https://api.holesky.blobscan.com/transactions/0x{tx_hash}"
    response = requests.get(tx_url)

    if response.status_code != 200:
        raise Exception(f"Transaction not found: HTTP {response.status_code} - {response.text}")

    tx_data = response.json()
    if not tx_data.get("blobs"):
        raise Exception("No blob versioned hashes found in transaction")

    blob_hash = tx_data['blobs'][0]['versionedHash']
    blobs_url = f"https://api.holesky.blobscan.com/blobs/{blob_hash}/data"
    blobs_response = requests.get(blobs_url)

    if blobs_response.status_code != 200:
        raise Exception(f"Could not retrieve blob: HTTP {blobs_response.status_code} - {blobs_response.text}")

    blobs_data = blobs_response.json()
    blobs_data_bytes = bytes.fromhex(blobs_data[2:])

    csd = Cascade()
    exp = csd.deserialize_cascade_blob(blobs_data_bytes)

    bytes1 = csd.serialize_cascade()

    b64_blob = base64.b64encode(bytes1).decode("utf-8")

    return csd, exp, b64_blob

def main():
    parser = argparse.ArgumentParser(description='Cascade CRL CLI')
    parser.add_argument('command', choices=['check'], help='Only supported command')
    parser.add_argument('--cascade_data', help='Base64-encoded cascade string (IPFS mode)')
    parser.add_argument('--pointer_hash', help='Transaction hash to lookup blob (Blob mode)')
    parser.add_argument('--id', required=True, help='Credential ID to check')

    args = parser.parse_args()

    if args.command == 'check':
        try:
            if args.cascade_data:
                cascade, exp = load_cascade_from_ipfs(args.cascade_data)
                result = {
                    "exp": exp,
                    "id": args.id,
                    "revoked": cascade.is_revoked(args.id)
                }

            elif args.pointer_hash:
                cascade, exp, blob_b64 = load_cascade_from_blob(args.pointer_hash)
                result = {
                    "exp": exp,
                    "id": args.id,
                    "revoked": cascade.is_revoked(args.id),
                    "b64_blob": blob_b64
                }

            else:
                print("Error: Provide --cascade_data or --pointer_hash")
                sys.exit(2)

            print(json.dumps(result))
            sys.exit(0 if not result["revoked"] else 1)

        except Exception as e:
            print(json.dumps({"error": str(e)}))
            sys.exit(3)

if __name__ == "__main__":
    sys.exit(main())
