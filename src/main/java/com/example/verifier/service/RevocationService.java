package com.example.verifier.service;

import com.example.verifier.model.IpfsStatusList;
import com.example.verifier.repository.IpfsStatusListRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class RevocationService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IpfsStatusListRepository repository;

    private static final String CASCADE_FOLDER = "cascade";

    public RevocationService(IpfsStatusListRepository repository) {
        this.repository = repository;
    }

    public boolean isRevokedViaIpfs(String uri, int idx) {
        try {
            IpfsStatusList statusList = repository.findByUrl(uri)
                    .orElseGet(() -> fetchAndCacheStatusList(uri));

            long now = System.currentTimeMillis() / 1000;

            if (statusList.getExpiresAt() <= now) {
                statusList = fetchAndCacheStatusList(uri);
            }

            String cascadePath = CASCADE_FOLDER + "/" + statusList.getBinFilename();

            ProcessBuilder pb = new ProcessBuilder("python", CASCADE_FOLDER + "/cascade_cli.py", "check",
                    "--cascade", cascadePath,
                    "--id", String.valueOf(idx));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            reader.readLine();

            String jsonLine = reader.readLine();

            process.waitFor();

            if (jsonLine == null) {
                throw new RuntimeException("No JSON response from cascade_cli.py");
            }

            JsonNode result = mapper.readTree(jsonLine);

            if (result.has("exp")) {
                long exp = result.get("exp").asLong();
                if (exp > statusList.getExpiresAt()) {
                    statusList.setExpiresAt(exp);
                    repository.save(statusList);
                }
            }

            return result.has("revoked") && result.get("revoked").asBoolean();

        } catch (Exception e) {
            throw new RuntimeException("IPFS revocation check failed", e);
        }
    }

    private IpfsStatusList fetchAndCacheStatusList(String uri) {
        try {
            String filename = "cascade_" + System.currentTimeMillis() + ".bin";
            String fullPath = CASCADE_FOLDER + "/" + filename;

            downloadFile(uri, fullPath);

            IpfsStatusList statusList = repository.findByUrl(uri).orElse(new IpfsStatusList());
            statusList.setUrl(uri);
            statusList.setBinFilename(filename);
            statusList.setExpiresAt(0);

            return repository.save(statusList);

        } catch (IOException e) {
            throw new RuntimeException("Failed to download IPFS cascade.bin", e);
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
