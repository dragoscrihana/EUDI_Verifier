#!/usr/bin/env python3
import argparse
import json
import sys
from cascade import Cascade  # Import your Cascade class

R = set() # Valid creds
S = set() # Revoked creds

def read_state_list(REVOKED_LIST_FILENAME):

    with open(REVOKED_LIST_FILENAME) as f:
        id_status_list = [line.strip() for line in f if line.strip()]

    for item in id_status_list:
        hex_hash, status = item.split(':')
        if status == '1':
            R.add(hex_hash)
        else:
            S.add(hex_hash)

def main():
    parser = argparse.ArgumentParser(description='Cascade CRL CLI')
    subparsers = parser.add_subparsers(dest='command', help='Command to execute')
    
    # Build command
    build_parser = subparsers.add_parser('build', help='Build a Cascade CRL')
    build_parser.add_argument('--status_list', required=True, help='File containg the status list')
    build_parser.add_argument('--output', required=True, help='Output file for serialized cascade')
    
    # Check command
    check_parser = subparsers.add_parser('check', help='Check if an ID is revoked')
    check_parser.add_argument('--cascade', required=True, help='Serialized cascade file')
    check_parser.add_argument('--id', required=True, help='Credential ID to check')
    
    # Parse arguments
    args = parser.parse_args()
    
    # Execute command
    if args.command == 'build':

        read_state_list(args.status_list)
        
        # Build cascade
        cascade = Cascade()
        cascade.build_cascade(R, S)
        
        # Serialize and save
        serialized = cascade.serialize_cascade()
        with open(args.output, 'wb') as f:
            f.write(serialized)
                
    elif args.command == 'check':

        # Load cascade
        with open(args.cascade, 'rb') as f:
            serialized = f.read()
            
        cascade = Cascade()
        cascade.deserialize_cascade(serialized)
        
        # Check ID
        is_revoked = cascade.is_revoked(args.id)
        result = {"id": args.id, "revoked": is_revoked}
        
        # Print result as JSON
        print(json.dumps(result))
        return 0 if not is_revoked else 1

if __name__ == "__main__":
    sys.exit(main())