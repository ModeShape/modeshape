/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.modeshape.common.util.FileUtil;
import org.modeshape.web.server.Connector;
import org.modeshape.web.shared.RestoreParams;

/**
 *
 * @author kulikov
 */
public class BackupUploadServlet extends HttpServlet {

    private static final long serialVersionUID = 1505304380334878522L;
    private final static String CONTENT_PARAMETER = "Upload content";
    private final static String REPOSITORY_NAME_PARAMETER = "repository";
    private final static String INCLUDE_BINARY_PARAMETER = "Include binaries";
    private final static String REINDEX_ON_FINISH_PARAMETER = "Reindex on finish";
    private final static String REPOSITORY_CONNECTOR = "connector";
    private final static String DESTINATION_URL = "/tree/%s/";
    private ServletFileUpload upload;
    private File tempDir;

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<FileItem> items;
        // Parse the request
        try {
            items = upload.parseRequest(request);
        } catch (FileUploadException e) {
            throw new ServletException(e);
        }

        String repository = getParameter(items, REPOSITORY_NAME_PARAMETER);
        String incBinaries = getParameter(items, INCLUDE_BINARY_PARAMETER);
        String reindexOnFinish = getParameter(items, REINDEX_ON_FINISH_PARAMETER);

        RestoreParams params = new RestoreParams();
        params.setIncludeBinaries(Boolean.valueOf(incBinaries));
        params.setReindexOnFinish(Boolean.valueOf(reindexOnFinish));
        
        InputStream in = getStream(items);

        File dir = new File(tempDir.getAbsolutePath() + File.pathSeparator + 
                Long.toString(System.currentTimeMillis()));
        
        FileUtil.unzip(in, dir.getAbsolutePath());
        Connector connector = (Connector) request.getSession().getAttribute(REPOSITORY_CONNECTOR);

        try {
            connector.find(repository).restore(dir.getAbsolutePath(), params);
        } catch (Exception e) {
            throw new ServletException(e);
        }

        String uri = request.getContextPath()
                + String.format(DESTINATION_URL, repository);
        response.sendRedirect(uri);
    }

    /**
     * Extracts value of the parameter with given name.
     *
     * @param items
     * @param name
     * @return parameter value or null.
     */
    private String getParameter(List<FileItem> items, String name) {
        for (FileItem i : items) {
            if (i.isFormField() && i.getFieldName().equals(name)) {
                return i.getString();
            }
        }
        return null;
    }

    /**
     * Gets uploaded file as stream.
     *
     * @param items
     * @return the stream
     * @throws IOException
     */
    private InputStream getStream(List<FileItem> items) throws IOException {
        for (FileItem i : items) {
            if (!i.isFormField() && i.getFieldName().equals(CONTENT_PARAMETER)) {
                return i.getInputStream();
            }
        }
        return null;
    }

    @Override
    public void init() {
        DiskFileItemFactory diskFactory = new DiskFileItemFactory();

        // Configure a repository (to ensure a secure temp location is used)
        ServletContext servletContext = this.getServletConfig().getServletContext();

        tempDir = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
        diskFactory.setRepository(tempDir);

        // Create a new file upload handler
        upload = new ServletFileUpload(diskFactory);
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
