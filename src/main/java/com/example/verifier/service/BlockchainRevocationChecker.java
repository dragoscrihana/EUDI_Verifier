package com.example.verifier.service;
import java.io.*;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class BlockchainRevocationChecker {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IpfsService ipfsService;
    private static final String CASCADE_FOLDER = "cascade";

    public BlockchainRevocationChecker(IpfsService ipfsService) {
        this.ipfsService = ipfsService;
    }

    public boolean checkRevocationViaBlockchain(String contractAddress, String abiJson, String issuerAddress, int index, String issuerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    CASCADE_FOLDER + "/dynamic_crl.py",
                    contractAddress,
                    abiJson,
                    issuerAddress
            );

            System.out.println(abiJson);

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            process.waitFor();

            Map<String, Object> crl = mapper.readValue(output.toString(), Map.class);

            if (crl.containsKey("error")) {
                System.err.println("Python error: " + crl.get("error"));
                return false;
            }

            String ipfsHash = crl.get("ipfsHash").toString();
            String ipfsUrl = "https://gateway.pinata.cloud/ipfs/" + ipfsHash;

            return ipfsService.isRevokedViaIpfs(ipfsUrl, index, issuerName);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
