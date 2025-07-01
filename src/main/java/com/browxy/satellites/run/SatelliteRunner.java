package com.browxy.satellites.run;

import java.io.File;
import java.io.FileNotFoundException;
//import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
//import java.util.Map;
import org.slf4j.LoggerFactory;
import com.browxy.satellites.domain.UpdateKepsManager;
import com.browxy.satellites.domain.config.Config;
import com.browxy.satellites.domain.config.ConfigResource;
import com.browxy.satellites.server.SatelliteWebServer;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;


public class SatelliteRunner {

  public static void main(String[] args) {
    if (args.length == 0) {
      throw new RuntimeException("please specify the properties identification, example: dev");
    }
    String environment = args[0];
    String resourceFile = "resource." + environment + ".properties";
    Config.getInstance().addConfigResource(new ConfigResource("compilerTemplate",
        ConfigResource.Type.Resource, "resource.template.properties", true, "UTF-8"));
    Config.getInstance().addConfigResource(new ConfigResource("compilerProduction",
        ConfigResource.Type.Resource, resourceFile, true, "UTF-8"));
//    updateLog4jConfiguration(SatelliteRunner.class
//        .getClassLoader()
//        .getResource("logback." + environment + ".xml").getFile());
    updateLog4jConfiguration("logback." + environment + ".xml");
   
    new UpdateKepsManager();
    
    int port = getPort();  
    new SatelliteWebServer(port);
    
  }

  public static int getPort() {
    String portStr = System.getenv("SERVER_PORT") == null  || System.getenv("SERVER_PORT").isEmpty() ? null : System.getenv("SERVER_PORT");
    return portStr != null ? Integer.parseInt(System.getenv("SERVER_PORT")) : Config.getInstance().getIntValue("port", 8090);  
  }
  
  public static void updateLog4jConfiguration(String logConfigPath) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    URL logConfigUrl = null;
    try {
        File file = new File(SatelliteRunner.class.getClassLoader().getResource(logConfigPath).getFile());
        if (file.exists()) {
          logConfigUrl = file.toURI().toURL();
        } else {
            logConfigUrl = SatelliteRunner.class.getClassLoader().getResource(logConfigPath);
            if (logConfigUrl == null) {
              file = new File("/home/satellite/application/" + logConfigPath);
              if(file.exists()) {
                logConfigUrl = file.toURI().toURL();
              } else {
                 throw new FileNotFoundException("Could not find logback config at path: " + logConfigPath);
              }
           }
        }

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        loggerContext.reset();
        configurator.doConfigure(logConfigUrl);
    } catch (JoranException | FileNotFoundException | MalformedURLException e) {
        throw new RuntimeException("Failed to configure logging from: " + logConfigPath, e);
    }

    StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
}

//  public static void updateLog4jConfiguration(String logFile) {
//    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
//    URL url;
//    try {
//      url = new File(logFile).toURI().toURL();
//    } catch (MalformedURLException e) {
//      throw new RuntimeException(e);
//    }
//    try {
//      JoranConfigurator configurator = new JoranConfigurator();
//      configurator.setContext(loggerContext);
//      loggerContext.reset();
//      configurator.doConfigure(url);
//    } catch (JoranException je) {
//      // StatusPrinter will handle this
//    }
//    StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
//  }

}
