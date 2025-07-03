package com.browxy.satellites.domain.github;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GithubRepoDownloader {
  private static Logger logger = LoggerFactory.getLogger(GithubRepoDownloader.class);
  
  public void download() throws IOException, InterruptedException  {
    
    //"export GITHUB_DOWNLOAD_URL=\"https://github.com/bxyteam/satellite-test/archive/refs/heads/main.zip\"\n" +
    String script = 
        "set -e\n" +
        "TMP_DIR=\"/var/compiler/satellite/data/tmp\"\n" +
        "TARGET_DIR=\"/var/compiler/satellite/data\"\n" +
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
        "echo \"Done: Folder 'github' is now in $TARGET_DIR\"\n";

 
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
