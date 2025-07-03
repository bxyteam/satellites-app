package com.browxy.satellites.domain.github;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.browxy.satellites.domain.config.SatelliteGithubConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GitHubBranchSyncChecker {
  private Logger logger = LoggerFactory.getLogger(GitHubBranchSyncChecker.class);

  private static final String GITHUB_API_BASE = "https://api.github.com";
  private final String token;
  private final String repoOwner;
  private final String repoName;
  private final String localRepoPath;

  public GitHubBranchSyncChecker(SatelliteGithubConfig amsatGithubConfig) {
    this.token = amsatGithubConfig.getGithubToken();
    this.repoOwner = amsatGithubConfig.getGithubOwner();
    this.repoName = amsatGithubConfig.getGithubRepo();
    this.localRepoPath = amsatGithubConfig.getLocalRepoPath();
  }

  public void checkAndSync() {
    try {

      if (!isLocalRepoExists()) {
        logger.info("Local repository not found. Cloning from remote...");
        cloneRepository();
        return;
      }

      String localCommitSha = getLocalMainCommitSha();
      String remoteCommitSha = getRemoteMainCommitSha();

      logger.info("Local main commit: " + localCommitSha);
      logger.info("Remote main commit: " + remoteCommitSha);

      if (localCommitSha.equals(remoteCommitSha)) {
        logger.info("Local and remote main branches are in sync. Nothing to do.");
      } else {
        logger.info("Local and remote main branches differ. Fetching new content...");
        fetchNewContent();
      }

    } catch (Exception e) {
      logger.error("Error checking branch sync: " + e.getMessage(), e);
    }
  }

  private boolean isLocalRepoExists() {
    File repoDir = new File(localRepoPath);
    File gitDir = new File(repoDir, ".git");

    return repoDir.exists() && gitDir.exists() && gitDir.isDirectory();
  }

  private void cloneRepository() throws IOException, InterruptedException {
    String repoUrl = String.format("https://github.com/%s/%s.git", repoOwner, repoName);

    File repoDir = new File(localRepoPath);
    File parentDir = repoDir.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      boolean created = parentDir.mkdirs();
      if (!created) {
        throw new RuntimeException(
            "Failed to create parent directory: " + parentDir.getAbsolutePath());
      }
    }

    logger.info("Cloning repository from: " + repoUrl);
    logger.info("To: " + localRepoPath);

    ProcessBuilder pb = new ProcessBuilder("git", "clone", repoUrl, localRepoPath);
    pb.redirectErrorStream(true);

    Process process = pb.start();

    // Log clone output
    try (Scanner scanner = new Scanner(process.getInputStream())) {
      while (scanner.hasNextLine()) {
        logger.info(scanner.nextLine());
      }
    }

    int exitCode = process.waitFor();
    if (exitCode == 0) {
      logger.info("Successfully cloned repository!");
      logger.info("Repository is now available at: " + localRepoPath);
    } else {
      throw new RuntimeException("Git clone failed with exit code: " + exitCode);
    }
  }

  private String getLocalMainCommitSha() throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "main");
    pb.directory(new File(localRepoPath));
    pb.redirectErrorStream(true);

    Process process = pb.start();

    String output = "";
    try (Scanner scanner = new Scanner(process.getInputStream())) {
      if (scanner.hasNextLine()) {
        output = scanner.nextLine().trim();
      }
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException("Failed to get local commit SHA. Exit code: " + exitCode);
    }

    return output;
  }

  private String getRemoteMainCommitSha() throws IOException {
    String apiUrl =
        String.format("%s/repos/%s/%s/branches/main", GITHUB_API_BASE, repoOwner, repoName);

    URL url = new URL(apiUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    connection.setRequestMethod("GET");
    connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
    connection.setRequestProperty("Authorization", "Bearer " + token);
    connection.setRequestProperty("User-Agent", "GitHubBranchSyncChecker");

    int responseCode = connection.getResponseCode();
    if (responseCode != 200) {
      throw new RuntimeException("GitHub API request failed with code: " + responseCode);
    }

    StringBuilder response = new StringBuilder();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
    }

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(response.toString());
    JsonNode commitNode = rootNode.path("commit");
    JsonNode shaNode = commitNode.path("sha");

    if (shaNode.isMissingNode()) {
      throw new RuntimeException("Could not find commit SHA in API response");
    }

    return shaNode.asText();
  }

  private void fetchNewContent() throws IOException, InterruptedException {
    logger.info("Executing git fetch...");
    ProcessBuilder pb = new ProcessBuilder("git", "fetch", "origin", "main");
    pb.directory(new File(localRepoPath));
    pb.redirectErrorStream(true);

    Process process = pb.start();

    // Log fetch output
    try (Scanner scanner = new Scanner(process.getInputStream())) {
      while (scanner.hasNextLine()) {
        logger.info(scanner.nextLine());
      }
    }

    int exitCode = process.waitFor();
    if (exitCode == 0) {
      logger.info("Successfully fetched new content from remote repository.");
      // Show what changed before merging
      showFetchSummary();

      // Update local main branch with remote changes
      updateLocalBranch();

    } else {
      logger.error("Git fetch failed with exit code: " + exitCode);
    }
  }

  private void updateLocalBranch() throws IOException, InterruptedException {
    // Check if we're currently on main branch
    String currentBranch = getCurrentBranch();

    if (!"main".equals(currentBranch)) {
      logger.info("Current branch is '" + currentBranch + "', switching to main...");
      checkoutBranch("main");
    }

    // Check for uncommitted changes
    if (hasUncommittedChanges()) {
      logger.info("Warning: You have uncommitted changes. Reset...");
      forceResetToRemote();
    } else {
      // Safe to merge
      mergeRemoteMain();
    }
  }

  private String getCurrentBranch() throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder("git", "branch", "--show-current");
    pb.directory(new File(localRepoPath));
    pb.redirectErrorStream(true);

    Process process = pb.start();

    String branch = "";
    try (Scanner scanner = new Scanner(process.getInputStream())) {
      if (scanner.hasNextLine()) {
        branch = scanner.nextLine().trim();
      }
    }

    process.waitFor();
    return branch;
  }

  private void checkoutBranch(String branchName) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder("git", "checkout", branchName);
    pb.directory(new File(localRepoPath));
    pb.redirectErrorStream(true);

    Process process = pb.start();

    try (Scanner scanner = new Scanner(process.getInputStream())) {
      while (scanner.hasNextLine()) {
        logger.info(scanner.nextLine());
      }
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException("Failed to checkout branch: " + branchName);
    }
  }

  private boolean hasUncommittedChanges() throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
    pb.directory(new File(localRepoPath));
    pb.redirectErrorStream(true);

    Process process = pb.start();

    boolean hasChanges = false;
    try (Scanner scanner = new Scanner(process.getInputStream())) {
      hasChanges = scanner.hasNextLine();
    }

    process.waitFor();
    return hasChanges;
  }

  /*
  private void stashChanges() throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder("git", "stash", "push", "-m", "Auto-stash before sync");
    pb.directory(new File(localRepoPath));
    pb.redirectErrorStream(true);

    Process process = pb.start();

    try (Scanner scanner = new Scanner(process.getInputStream())) {
      while (scanner.hasNextLine()) {
        logger.info(scanner.nextLine());
      }
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      logger.error("Warning: Failed to stash changes");
    }
  }

  private void popStashedChanges() throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder("git", "stash", "pop");
    pb.directory(new File(localRepoPath));
    pb.redirectErrorStream(true);

    Process process = pb.start();

    try (Scanner scanner = new Scanner(process.getInputStream())) {
      while (scanner.hasNextLine()) {
        logger.info(scanner.nextLine());
      }
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      logger.error(
          "Warning: Failed to restore stashed changes. You may need to manually run 'git stash pop'");
    }
  }
*/

  private void mergeRemoteMain() throws IOException, InterruptedException {
    logger.info("Merging remote changes into local main branch...");

    ProcessBuilder pb = new ProcessBuilder("git", "merge", "-X", "theirs", "origin/main");
    pb.directory(new File(localRepoPath));
    pb.redirectErrorStream(true);

    Process process = pb.start();
    try (Scanner scanner = new Scanner(process.getInputStream())) {
      while (scanner.hasNextLine()) {
        logger.info(scanner.nextLine());
      }
    }

    int exitCode = process.waitFor();
    if (exitCode == 0) {
      logger.info("Successfully updated local main branch with remote changes.");
      logger.info("Your local files have been updated!");
    } else {
      logger.error("Merge failed with exit code: " + exitCode);
      logger.error("Falling back to hard reset (replacing local content with remote)...");
      forceResetToRemote();
    }
  }

  private void forceResetToRemote() throws IOException, InterruptedException {
    logger.warn("WARNING: Performing a hard reset. All local changes will be lost!");

    ProcessBuilder resetPb = new ProcessBuilder("git", "reset", "--hard", "origin/main");
    resetPb.directory(new File(localRepoPath));
    resetPb.redirectErrorStream(true);

    Process resetProcess = resetPb.start();
    try (Scanner scanner = new Scanner(resetProcess.getInputStream())) {
      while (scanner.hasNextLine()) {
        logger.info(scanner.nextLine());
      }
    }

    int resetExitCode = resetProcess.waitFor();
    if (resetExitCode == 0) {
      logger.info("Hard reset completed. Local repository now matches remote.");
    } else {
      logger.error("Hard reset failed with exit code: " + resetExitCode);
      throw new RuntimeException("Hard reset failed. Manual intervention may be required.");
    }
  }

 /*
  private String getCurrentCommitHash() throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
    pb.directory(new File(localRepoPath));

    Process process = pb.start();
    try (Scanner scanner = new Scanner(process.getInputStream())) {
      return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }
  }
*/
 
  private void showFetchSummary() throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder("git", "log", "--oneline", "main..origin/main");
    pb.directory(new File(localRepoPath));
    pb.redirectErrorStream(true);

    Process process = pb.start();

    StringBuilder output = new StringBuilder();
    try (Scanner scanner = new Scanner(process.getInputStream())) {
      while (scanner.hasNextLine()) {
        output.append(scanner.nextLine()).append("\n");
      }
    }

    process.waitFor();

    if (output.length() > 0) {
      logger.info("\nNew commits available:");
      logger.info(output.toString());
    }
  }
}


