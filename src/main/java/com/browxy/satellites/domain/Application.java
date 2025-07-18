package com.browxy.satellites.domain;

import com.browxy.satellites.domain.config.SatelliteGithubConfig;
import com.browxy.satellites.domain.config.SatelliteWebConfig;
import com.browxy.satellites.domain.config.Config;
import com.browxy.satellites.util.JSONUtils;


public class Application {
  private static Application instance;
  private SatelliteGithubConfig satelliteGithubConfig;
  private SatelliteWebConfig satelliteWebConfig;

  public Application() {
    this.satelliteGithubConfig = this.getGithubConfigFromEnv();
    this.satelliteWebConfig = this.getWebConfigFromJson();
  }

  public static Application getInstance() {
    if (instance == null) {
      synchronized (Application.class) {
        if (instance == null) {
          instance = new Application();
        }
      }
    }
    return instance;
  }

  public SatelliteGithubConfig getSatelliteGithubConfig() {
    return satelliteGithubConfig;
  }

  public void setSatelliteGithubConfig(SatelliteGithubConfig satelliteGithubConfig) {
    this.satelliteGithubConfig = satelliteGithubConfig;
  }

  public SatelliteWebConfig getSatelliteWebConfig() {
    return satelliteWebConfig;
  }

  private SatelliteGithubConfig getGithubConfigFromEnv() {
     return new SatelliteGithubConfig();
  }

  private SatelliteWebConfig getWebConfigFromJson() {
    String json = Config.getInstance().getStringValue("web.config");
    try {
      SatelliteWebConfig conf = JSONUtils.fromJson(json, SatelliteWebConfig.class);
      String entryPoint = System.getenv("ENTRY_POINT") != null && !System.getenv("ENTRY_POINT").trim().isEmpty() ? System.getenv("ENTRY_POINT").trim() : "pass";  
      String token = System.getenv("TOKEN") != null ? System.getenv("TOKEN").trim() : "";  
      conf.setEntryPoint(entryPoint);
      conf.setToken(token);
      return conf;
    } catch (Exception e) {
      return new SatelliteWebConfig();
    }
  }
}
