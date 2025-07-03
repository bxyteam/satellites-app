package com.browxy.satellites.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.browxy.satellites.domain.config.Config;
import com.browxy.satellites.domain.github.GitHubBranchSyncChecker;

public class DailyScriptScheduler {
  private static Logger logger = LoggerFactory.getLogger(DailyScriptScheduler.class);

  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  public void startSchedule() {
    int hour = this.getScheduleRunHour();
    int minute = this.getScheduleRunMinute();
    LocalTime targetTime = LocalTime.of(hour, minute);

    scheduleDailyTask(targetTime);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      scheduler.shutdown();
    }));
  }

  private int getScheduleRunHour() {
    if(System.getenv("SCHEDULE_RUN_HOUR") != null) {
      try {
        int h = Integer.parseInt(System.getenv("SCHEDULE_RUN_HOUR").trim());
        return h;
      } catch (NumberFormatException e) {
        logger.error("Error parse schedule run hour",e); 
        return Config.getInstance().getIntValue("schedule.scriptRunHour", 1);
      }
    } else {  
        return Config.getInstance().getIntValue("schedule.scriptRunHour", 1);
      }
  }

  private int getScheduleRunMinute() {
    if(System.getenv("SCHEDULE_RUN_MINUTE") != null) {
      try {
        int m = Integer.parseInt(System.getenv("SCHEDULE_RUN_MINUTE").trim());
        return m;
      } catch (NumberFormatException e) {
        logger.error("Error parse schedule run minute",e); 
        return Config.getInstance().getIntValue("schedule.scriptRunMinute", 30);
      }
    } else {  
        return Config.getInstance().getIntValue("schedule.scriptRunMinute", 30);
      }
  }
  
  private void scheduleDailyTask(LocalTime targetTime) {
    long initialDelay = calculateInitialDelay(targetTime);
    long period = TimeUnit.DAYS.toSeconds(1);
    
    scheduler.scheduleAtFixedRate(() -> {
      runKepsUdaterScript();
      runPasosUdaterScript();
    }, initialDelay, period, TimeUnit.SECONDS);

    logger.info("Script scheduled to run daily at " + targetTime);
  }

  private long calculateInitialDelay(LocalTime targetTime) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime nextRun = now.with(targetTime);

    if (now.isAfter(nextRun)) {
      nextRun = nextRun.plusDays(1);
    }

    return now.until(nextRun, ChronoUnit.SECONDS);
  }

  private void runKepsUdaterScript() {
    String scriptKepsUpdaterFilePath =
        Application.getInstance().getSatelliteGithubConfig().getLocalRepoPath()
            + "/scripts/run_keps_updater.sh";
    String scriptCopyFilesFilePath =
        Application.getInstance().getSatelliteGithubConfig().getLocalRepoPath()
            + "/scripts/copy_files.sh";
    try {
      String spaceTrackIdentity = System.getenv("SPACE_TRACK_IDENTITY");
      String spaceTrackPassword = System.getenv("SPACE_TRACK_PASSWORD");
      if(spaceTrackIdentity != null && !spaceTrackIdentity.isEmpty() 
          && spaceTrackPassword != null && !spaceTrackPassword.isEmpty()) {
        executeScript(scriptKepsUpdaterFilePath);
      }
      syncGithub();
      executeScript(scriptCopyFilesFilePath);
    } catch (Exception e) {
      logger.error("Error executing script: " + scriptKepsUpdaterFilePath, e);
    }
  }

  private void runPasosUdaterScript() {
    String scriptPasosUpdaterFilePath =
        Application.getInstance().getSatelliteGithubConfig().getLocalRepoPath()
            + "/scripts/run_pasos_updater.sh";
    try {
      executeScript(scriptPasosUpdaterFilePath);
    } catch (Exception e) {
      logger.error("Error executing script: " + scriptPasosUpdaterFilePath, e);
    }
  }

  public void syncGithub() {

    if (Application.getInstance().getSatelliteGithubConfig().getGithubToken() == null
        || Application.getInstance().getSatelliteGithubConfig().getGithubToken().isEmpty()) {
      logger.error("Please set GITHUB_TOKEN environment variable");
      return;
    }

    GitHubBranchSyncChecker checker =
        new GitHubBranchSyncChecker(Application.getInstance().getSatelliteGithubConfig());

    checker.checkAndSync();

  }

  private void executeScript(String scriptPath) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder("bash", scriptPath);
    pb.redirectErrorStream(true);

    Process process = pb.start();

    // Read the output
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        logger.info("Script output: " + line);
      }
    }

    int exitCode = process.waitFor();
    if (exitCode == 0) {
      logger.info("Script executed successfully at " + LocalDateTime.now());
    } else {
      logger.error("Script failed with exit code: " + exitCode);
    }
  }

}
