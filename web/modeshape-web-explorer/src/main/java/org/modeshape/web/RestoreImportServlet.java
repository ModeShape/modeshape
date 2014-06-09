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
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 *
 * @author kulikov
 */
public class RestoreImportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            PrintWriter out = response.getWriter();
        
                        response.setContentType("text/html");
                        response.setHeader("Pragma", "No-cache");
                        response.setDateHeader("Expires", 0);
                        response.setHeader("Cache-Control", "no-cache");
                        out = response.getWriter();
                        out.println("<html>");
                        out.println("<body>");
                        out.println("<script type=\"text/javascript\">");
                        out.println("if (parent.uploadComplete) parent.uploadComplete('zzz');");
                        out.println("</script>");
                        out.println("</body>");
                        out.println("</html>");
                        out.flush();
                        response.flushBuffer();
                        
                        if (true) {
                            return;
                        }
        
        if (ServletFileUpload.isMultipartContent(request)) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            out = response.getWriter();
            try {
                List<FileItem> items = upload.parseRequest(request);
                for (FileItem fileItem : items) {
                    if (fileItem.isFormField()) {
                        continue;
                    }
                    File uploadedFile = new File("zzz");

                    if (uploadedFile.createNewFile()) {
                        fileItem.write(uploadedFile);
                        response.setContentType("text/html");
                        response.setHeader("Pragma", "No-cache");
                        response.setDateHeader("Expires", 0);
                        response.setHeader("Cache-Control", "no-cache");
                        out = response.getWriter();
                        out.println("<html>");
                        out.println("<body>");
                        out.println("<script type=\"text/javascript\">");
                        out.println("if (parent.uploadComplete) parent.uploadComplete('zzz');");
                        out.println("</script>");
                        out.println("</body>");
                        out.println("</html>");
                        out.flush();
                        response.flushBuffer();
                    } else {
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.setContentType("text/html");
            }
        } else {
//            PrintWriter out;
            out = response.getWriter();
            response.setContentType("text/html");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
