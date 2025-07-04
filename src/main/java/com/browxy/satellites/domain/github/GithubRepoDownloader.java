package com.browxy.satellites.domain.github;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.browxy.satellites.domain.Application;

public class GithubRepoDownloader {
  private static Logger logger = LoggerFactory.getLogger(GithubRepoDownloader.class);
  
  public void download() throws IOException, InterruptedException  {
    
    if (Application.getInstance().getSatelliteGithubConfig().getDownloadUrl() == null
        || Application.getInstance().getSatelliteGithubConfig().getDownloadUrl().isEmpty()) {
      return;
    }
    
    String script = 
        "set -e\n" +
        "TMP_DIR=\"/var/satellite/data/tmp\"\n" +
        "TARGET_DIR=\"/var/satellite/data\"\n" +
        "ZIP_FILE=\"$TMP_DIR/main.zip\"\n" +
        "mkdir -p \"$TMP_DIR\"\n" +
        "wget -O \"$ZIP_FILE\" \"$GITHUB_DOWNLOAD_URL\"\n" +
        "unzip -o \"$ZIP_FILE\" -d \"$TMP_DIR\"\n" +
        "UNZIPPED_DIR=$(find \"$TMP_DIR\" -maxdepth 1 -type d -name \"*-main\")\n" +
        "if [ -d \"$UNZIPPED_DIR\" ]; then\n" +
        "    mv \"$UNZIPPED_DIR\" \"$TMP_DIR/github\"\n" +
        "    mv \"$TMP_DIR/github\" \"$TARGET_DIR/\"\n" +
        "else\n" +
        "    echo \"Error: Unzipped directory not found.\" >&2\n" +
        "    exit 1\n" +
        "fi\n" +
        "rm -f \"$ZIP_FILE\"\n" +
        "echo \"Done: Folder 'github' is now in $TARGET_DIR\"\n" +
        "WEB_DIR=\"/var/satellite/data/web\"\n" + 
        "GITHUB_DIR=\"/var/satellite/data/github\"\n" + 
        "\n" + 
        "# Copy templates if source directory exists and has files\n" + 
        "if [ -d \"${GITHUB_DIR}/frontend/templates\" ] && [ \"$(ls -A ${GITHUB_DIR}/frontend/templates)\" ]; then\n" + 
        "    cp ${GITHUB_DIR}/frontend/templates/* ${WEB_DIR}/templates\n" + 
        "else\n" + 
        "    echo \"Templates directory is missing or empty: ${GITHUB_DIR}/frontend/templates\"\n" + 
        "fi\n" + 
        "\n" + 
        "# Copy satellite assets if source directory exists and has files\n" + 
        "if [ -d \"${GITHUB_DIR}/frontend/sats\" ] && [ \"$(ls -A ${GITHUB_DIR}/frontend/sats)\" ]; then\n" + 
        "    cp ${GITHUB_DIR}/frontend/sats/* ${WEB_DIR}/share/assets\n" + 
        "else\n" + 
        "    echo \"Sats directory is missing or empty: ${GITHUB_DIR}/frontend/sats\"\n" + 
        "fi\n" + 
        "\n" + 
        "# Copy html assets if source directory exists and has files\n" + 
        "if [ -d \"${GITHUB_DIR}/frontend/html\" ] && [ \"$(ls -A ${GITHUB_DIR}/frontend/html)\" ]; then\n" + 
        "    cp ${GITHUB_DIR}/frontend/html/* ${WEB_DIR}/share/assets\n" + 
        "else\n" + 
        "    echo \"Sats directory is missing or empty: ${GITHUB_DIR}/frontend/html\"\n" + 
        "fi\n";

 
    ProcessBuilder builder = new ProcessBuilder("bash", "-s");
    builder.redirectErrorStream(true); 
    
    // Start the process
    Process process = builder.start();

    // Write the script to the process's stdin
    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
        writer.write(script);
    }

    // Read and print script output
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
            logger.info(line);
        }
    }

    // Wait for completion
    int exitCode = process.waitFor();
    if (exitCode != 0) {
        logger.error("Script failed with exit code: " + exitCode);
    } else {
        logger.info("GitHub repo downloaded and moved successfully.");
    }
  }
}
