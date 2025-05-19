package com.example.verifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class RevocationService {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final String CASCADE_FOLDER = "cascade";
    private static final String CASCADE_PATH = CASCADE_FOLDER + "/cascade.bin";
    private static final String CLI_PATH = CASCADE_FOLDER + "/cascade_cli.py";


    public boolean isRevokedViaIpfs(String uri, int idx) {
        try {
            //downloadFile(uri, CASCADE_PATH);

            ProcessBuilder pb = new ProcessBuilder("python", CLI_PATH, "check",
                    "--cascade", CASCADE_PATH,
                    "--id", String.valueOf(idx));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            process.waitFor();

            JsonNode result = mapper.readTree(output.toString());
            return result.has("revoked") && result.get("revoked").asBoolean();

        } catch (Exception e) {
            throw new RuntimeException("IPFS revocation check failed", e);
        }
    }

    private void downloadFile(String fileUrl, String destinationPath) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(destinationPath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        connection.disconnect();
    }
}
