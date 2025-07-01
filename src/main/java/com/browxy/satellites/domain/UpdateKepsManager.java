package com.browxy.satellites.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateKepsManager {
  private Logger logger = LoggerFactory.getLogger(UpdateKepsManager.class);

  public UpdateKepsManager() {
    logger.info("Starting Satellites Keps Updater.");
    DailyScriptScheduler dailyScriptScheduler = new DailyScriptScheduler();
    dailyScriptScheduler.syncGithub();
    dailyScriptScheduler.startSchedule();
  }


}


