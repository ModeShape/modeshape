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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;

/**
 *
 * @author kulikov
 */
public class BackupExportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private File tempDir;
    
    private Logger logger = Logger.getLogger("Servlet");
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String fname = request.getParameter("file");
        String qs = request.getQueryString();
        
        logger.debug("--------- Query string=" + qs);
        
        File file = new File(tempDir.getAbsoluteFile() + File.separator + fname);
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }

        File zipFile = new File(tempDir.getAbsolutePath() + File.separator + "bak.zip");
        
        FileUtil.zipDir(file.getAbsolutePath(), zipFile.getAbsolutePath());
        
        response.setHeader("Content-Type", "application/zip");
        response.setHeader("Content-Length", String.valueOf(zipFile.length()));
        response.setHeader("Content-disposition", "attachment;filename=\"bak.zip\"");

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(zipFile));
        BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
        
        IoUtil.write(bis, bos);

        bos.flush();
        bos.close();
        bis.close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        doGet(request, response);
    }
    
    @Override
    public void init() {
        // Configure a repository (to ensure a secure temp location is used)
        ServletContext servletContext = this.getServletConfig().getServletContext();
        tempDir = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
    }
    
}


