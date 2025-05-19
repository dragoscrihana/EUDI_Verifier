import math
import hashlib
from pickle import dumps
from secrets import randbits
from rbloom import Bloom
import struct
import secrets


class Cascade:

    def __init__(self):
        pass

    def build_cascade(self, R, S):
        """
        Build the cascade of RBloomFilters.
        
        :param R: Set of valid credential IDs (strings).
        :param S: Set of revoked credential IDs (strings).
        :param r_hat: Padding size for valid IDs (|R| ≤ r_hat, |S| ≤ 2*r_hat).
        """

        self.r_hat, self.s_hat = self.calculate_daily_crl_sizes(len(R))

        # Initialize the filters list
        self.filters = []
        # Generate 256-bit salt as a hex string
        self.salt = format(secrets.randbits(256), "064x")

        self.s_hat = 2 * self.r_hat

        # Generate padding IDs
        Pr = set()
        while len(Pr) < self.r_hat - len(R):
            new_id = format(randbits(256), '064x')
            if new_id not in R and new_id not in S:
                Pr.add(new_id)

        Ps = set()
        while len(Ps) < self.s_hat - len(S):
            new_id = format(randbits(256), '064x')
            if new_id not in R and new_id not in S and new_id not in Pr:
                Ps.add(new_id)

        # Add the padding
        R_hat = R | Pr
        S_hat = S | Ps

        p = 0.5
        p0 = math.sqrt(p) / 2

        # Generate the working sets for included and excluded IDs
        Win = R_hat.copy()
        Wex = S_hat.copy()

        # Iterate until we eliminate all false positives
        level = 0
        while len(Win) > 0:
            false_positive_rate = p0 if level == 0 else p

            # Create a new filter

            filter = Bloom(expected_items=len(Win), false_positive_rate=false_positive_rate, hash_func=self._hash_func)


            W_salted = set()
            for id in Win:
                salted_id = self._get_seasoned_id(id, level)
                W_salted.add(salted_id)

            for salted_id in W_salted:
                filter.add(salted_id)

            # Store filter
            self.filters.append({'level': level, 'filter': filter})

            W_false_positives = set()
            for id in Wex:
                salted_id = self._get_seasoned_id(id, level)
                if salted_id in filter:
                    W_false_positives.add(id)  # Collect original id

            Wex = Win.copy()
            Win = W_false_positives.copy()

            level += 1

    def is_revoked(self, id):
        """Check if an ID is revoked."""
        for filter_data in self.filters:
            level = filter_data['level']
            filter = filter_data['filter']
            salted_id = self._get_seasoned_id(id, level)
            if salted_id not in filter:
                return level % 2 == 1  # Even: valid, Odd: revoked
        if len(self.filters) % 2 == 0:
            return False
        else:
            return True

        
    def calculate_daily_crl_sizes(self, current_valid_certs : int, daily_revocation_rate : float = 0.01, safety_factor : float = 1.2):
        """
        Calculate appropriate sizes for R and S sets for a daily CRL.
        
        Parameters:
        current_valid_certs (int): Number of currently valid certificates that could be revoked
        daily_revocation_rate (float): Expected maximum daily revocation rate (e.g., 0.01 for 1%)
        safety_factor (float): Multiplier to add extra capacity for unexpected spikes
        
        Returns:
        tuple: (r_hat, s_hat) - The padded sizes for sets R and S
        """

        expected_revocations = math.ceil(current_valid_certs * daily_revocation_rate * safety_factor)
        
        r_hat = current_valid_certs - expected_revocations
        
        s_hat = 2 * expected_revocations
        
        return r_hat, s_hat

    def serialize_cascade(self):
        """
        Serialize the cascade filter into a binary format.
        
        Returns:
            bytes: Serialized cascade data
        """
        data = []
        
        # Add salt
        salt_bytes = bytes.fromhex(self.salt)
        data.append(salt_bytes)
        
        # Add number of filters
        num_filters = len(self.filters)
        data.append(struct.pack('>I', num_filters))
        
        # Add each filter
        for filter_data in self.filters:
            filter = filter_data['filter']
            bits = filter.save_bytes()
            
            # Add length of filter data
            length = len(bits)
            data.append(struct.pack('>I', length))
            
            # Add filter data
            data.append(bits)
        
        # Combine all parts
        return b''.join(data)

    def deserialize_cascade(self, data):
        """
        Deserialize binary data into a cascade filter.
        
        Args:
            data: Binary data or hex string representation
        """
        # Handle hex string input
        if isinstance(data, str):
            if data.startswith("0x"):
                data = data[2:]
            try:
                data = bytes.fromhex(data)
            except ValueError as e:
                raise ValueError(f"Invalid hex string: {e}")
        
        # Extract salt (first 32 bytes)
        offset = 0
        self.salt = data[offset:offset + 32].hex()
        offset += 32
        
        # Extract number of filters (4 bytes)
        num_filters = struct.unpack_from('>I', data, offset)[0]
        offset += 4
        
        # Reconstruct filters
        self.filters = []
        for i in range(num_filters):
            # Extract filter data length (4 bytes)
            length = struct.unpack_from('>I', data, offset)[0]
            offset += 4
            
            # Extract filter data
            bits = data[offset:offset + length]
            offset += length
            
            # Reconstruct filter
            filter = Bloom.load_bytes(bytes(bits), self._hash_func)
            
            # Store filter
            self.filters.append({'level': i, 'filter': filter})

    def _get_seasoned_id(self, id, level):
        """
        Returns the salted ID using SHA-256 hashing.
        """
        #return hashlib.sha3_256((id + str(level) + self.salt).encode()).hexdigest()
        return (id + str(level) + self.salt).encode()
    
    def _hash_func(self, obj):
        h = hashlib.sha3_256(dumps(obj)).digest()
        return int.from_bytes(h[:16], "big", signed=True)