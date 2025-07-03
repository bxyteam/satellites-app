package com.browxy.satellites.domain;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.browxy.satellites.domain.github.GithubRepoDownloader;

public class UpdateKepsManager {
  private Logger logger = LoggerFactory.getLogger(UpdateKepsManager.class);

  public UpdateKepsManager() {
    logger.info("Starting Satellites Keps Updater.");
    
    DailyScriptScheduler dailyScriptScheduler = new DailyScriptScheduler();
    
    if (Application.getInstance().getSatelliteGithubConfig().getGithubToken() == null
        || Application.getInstance().getSatelliteGithubConfig().getGithubToken().isEmpty()) {
      GithubRepoDownloader githubRepoDownloader = new GithubRepoDownloader();
      try {
        githubRepoDownloader.download();
      } catch (IOException | InterruptedException e) {
        logger.error("Error download and unzip github project",e);
      }
      
    } else {
      dailyScriptScheduler.syncGithub();
    }
    
    dailyScriptScheduler.startSchedule();
  }


}



