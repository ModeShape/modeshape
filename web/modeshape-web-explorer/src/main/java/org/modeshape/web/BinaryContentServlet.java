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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.modeshape.web.server.Connector;

/**
 *
 * @author kulikov
 */
public class BinaryContentServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger("Binary");
    
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
            log.severe("Binary content has been requested");
        
        String repository = request.getParameter("repository");
        String workspace = request.getParameter("workspace");
        String path = request.getParameter("path");
        String property = request.getParameter("property");

        Connector connector = (Connector) request.getSession().getAttribute("connector");
        try {
            Session session = connector.find(repository).session(workspace);
            Node node = session.getNode(path);

            Property p;
            try {
                p = node.getProperty(property);
            } catch (PathNotFoundException e) {
                response.setContentType("text/html");
                PrintWriter writer = response.getWriter();
                writer.write("<html>");
                writer.write("<p>Content not found or recently removed</p>");
                writer.write("</html>");
                return;
            }
            
            Binary binary = p.getBinary();


            InputStream in = binary.getStream();
            String contentType = node.getProperty("jcr:mimeType").getString();

            response.setContentType(contentType);

            int b;
            while ((b = in.read()) != -1) {
                response.getOutputStream().write(b);
            }

            log.severe("Sent binary content");
            binary.dispose();

        } catch (Exception e) {
            throw new ServletException(e);
        }
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
    }// </editor-fold>
}
