package com.browxy.satellites.server.servlets.api.v1;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.browxy.satellites.domain.Application;
import com.browxy.satellites.util.FileManager;
import com.browxy.satellites.util.ResponseMessageUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class FileReaderServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(FileReaderServlet.class);

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("application/json;charset=UTF-8");
    try {
      String file = request.getParameter("file");
      if (file == null || file.trim().isEmpty()) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter()
            .write(ResponseMessageUtil.getStatusMessage("Request does not contain file", 404));
      }

      String path = request.getParameter("path");
      String fullPath = path == null || path.trim().isEmpty()
          ? Application.getInstance().getSatelliteWebConfig().getBasePath() + File.separator + file
          : Application.getInstance().getSatelliteWebConfig().getBasePath() + File.separator + path
              + File.separator + file;

      if (!(new File(fullPath).exists())) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write(ResponseMessageUtil.getStatusMessage("File not found", 404));
        return;
      }

      String content = FileManager.readFile(fullPath, "UTF-8");

      JsonObject json = new JsonObject();
      json.addProperty("statusCode", 200);
      json.addProperty("content", content);

      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write(new Gson().toJson(json));

    } catch (Exception e) {
      logger.error("FileReader error ", e);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write(ResponseMessageUtil.getStatusMessage("Error reading file", 400));
    } finally {
      response.flushBuffer();
      response.getWriter().close();
    }
  }

}

