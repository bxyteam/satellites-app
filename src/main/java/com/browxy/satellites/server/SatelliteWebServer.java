package com.browxy.satellites.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.browxy.satellites.run.SessionCleanupListener;
import com.browxy.satellites.server.servlets.AdminGithubServlet;
import com.browxy.satellites.server.servlets.SendStaticFileServlet;
import com.browxy.satellites.server.servlets.api.admin.v1.GithubSaveRepoAdminConfServlet;
import com.browxy.satellites.server.servlets.api.v1.FileReaderServlet;
import com.browxy.satellites.server.servlets.api.v1.GetAliasServlet;
import com.browxy.satellites.server.servlets.api.v1.GetAssetServlet;
import com.browxy.satellites.server.servlets.api.v1.GetSessionServlet;

public class SatelliteWebServer {
  private final Logger logger = LoggerFactory.getLogger(SatelliteWebServer.class);

  public SatelliteWebServer(int port) {
    Thread jettyThread = new Thread(() -> startJettyServer(port));
    jettyThread.start();

  }

  private void startJettyServer(int port) {
    try {

      Server jettyServer = new Server(port);
      ServletContextHandler servletContextHandler = getServletHandler();

      HandlerList handlers = new HandlerList();
      handlers.addHandler(servletContextHandler);

      jettyServer.setHandler(handlers);

      jettyServer.start();

      jettyServer.join();
    } catch (Exception e) {
      logger.error("Error starting Jetty server", e);
    }
  }

  private ServletContextHandler getServletHandler() {

    ServletContextHandler servletContextHandler =
        new ServletContextHandler(ServletContextHandler.SESSIONS);

    SessionHandler sessionHandler = servletContextHandler.getSessionHandler();
    sessionHandler.setMaxInactiveInterval(30 * 60);
    sessionHandler.addEventListener(new SessionCleanupListener());

    servletContextHandler.addServlet(new ServletHolder(new SendStaticFileServlet()), "/*");
    servletContextHandler.addServlet(new ServletHolder(new GetAssetServlet()), "/api/v1/getAsset");
    servletContextHandler.addServlet(new ServletHolder(new FileReaderServlet()),
        "/api/v1/readFile");
    servletContextHandler.addServlet(new ServletHolder(new GetSessionServlet()),
        "/api/v1/getSession");
    servletContextHandler.addServlet(new ServletHolder(new GetAliasServlet()), "/api/v1/getAlias");
    servletContextHandler.addServlet(new ServletHolder(new AdminGithubServlet()), "/admin/github");
    servletContextHandler.addServlet(new ServletHolder(new GithubSaveRepoAdminConfServlet()), "/api/admin/v1/saveGithubConf");
    servletContextHandler.setContextPath("/");
    return servletContextHandler;
  }
}
