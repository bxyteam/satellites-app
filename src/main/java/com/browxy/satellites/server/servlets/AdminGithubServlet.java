package com.browxy.satellites.server.servlets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.browxy.satellites.domain.Application;
import com.browxy.satellites.util.MimeTypeUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AdminGithubServlet extends HttpServlet {
  
  private static final long serialVersionUID = 1L;
  private final Logger logger = LoggerFactory.getLogger(AdminGithubServlet.class);

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      String token = req.getParameter("token");
      if(!Application.getInstance().getAmsatWebConfig().getToken().equals(token)) {
        resp.setContentType("text/html");
        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED - Bad Credentials");
      }
      String filePath =  Application.getInstance().getAmsatWebConfig().getStaticPath() + File.separator
              + "adminGithub.html";
      File file = new File(filePath);
      String mimeType = MimeTypeUtil.getMimeTypeByFileName(file.getName());
      if (file.exists()) {
        String content = buildHtmlMetadata(file);
        resp.setContentType(mimeType);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(content);
      } else {
        resp.setContentType("text/html");
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found!");
      }
    } catch (Exception e) {
      logger.error("unable to send admin github content", e);
      resp.setContentType("text/html");
      resp.sendError(HttpServletResponse.SC_NOT_FOUND, "unable to send html content");
    } finally {
      resp.flushBuffer();
      resp.getWriter().close();
    }
  }

  private String buildHtmlMetadata(File file) throws IOException {
    String content = new String(Files.readAllBytes(file.toPath()));
    Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
    String conf = gson.toJson(Application.getInstance().getAmsatGithubConfig());
    return content.replace("%%GITHUB_DATA%%;", conf);
  }
}
