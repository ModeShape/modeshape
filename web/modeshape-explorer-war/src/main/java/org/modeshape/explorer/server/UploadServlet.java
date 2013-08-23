/**
Copyright (c) <2009> <Pete Boysen>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package org.modeshape.explorer.server;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * 
 * @author Pete Boysen (pboysen@iastate.edu)
 * @since May 9, 2009
 * @version May 9, 2009
 */
public class UploadServlet extends HttpServlet {
	private static final long serialVersionUID = 8220537012536612058L;
	
	private static Log log = LogFactory.getLog(UploadServlet.class);

	public UploadServlet() {
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		process(request, response);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		process(request, response);
	}

	private void process(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			if (ServletFileUpload.isMultipartContent(request)) {
				processFiles(request, response);
			} else {
				processQuery(request, response);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void processQuery(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			// process the parameters and provide a response based on your local
			// code

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}

	}

	private void processFiles(HttpServletRequest request,
			HttpServletResponse response) {
		HashMap<String, String> args = new HashMap<String, String>();
		try {
			if (log.isDebugEnabled())
				log.debug(request.getParameterMap());
			ServletFileUpload upload = new ServletFileUpload();
			FileItemIterator iter = upload.getItemIterator(request);
			// pick up parameters first and note actual FileItem
			while (iter.hasNext()) {
				FileItemStream item = iter.next();
				String name = item.getFieldName();
				if (item.isFormField()) {
					args.put(name, Streams.asString(item.openStream()));
				} else {
					args.put("contentType", item.getContentType());
					String fileName = item.getName();
					int slash = fileName.lastIndexOf("/");
					if (slash < 0)
						slash = fileName.lastIndexOf("\\");
					if (slash > 0)
						fileName = fileName.substring(slash + 1);
					args.put("fileName", fileName);

					if (log.isDebugEnabled())
						log.debug(args);
					
					InputStream in = null;
					try {
						in = item.openStream();
						writeToFile(request.getSession().getId() + "/" +fileName, in, true, request.getSession().getServletContext().getRealPath("/"));
					} catch (Exception e) {
//						e.printStackTrace();
						log.error("Fail to upload " + fileName);
						
						response.setContentType("text/html");
						response.setHeader("Pragma", "No-cache");
						response.setDateHeader("Expires", 0);
						response.setHeader("Cache-Control", "no-cache");
						PrintWriter out = response.getWriter();
						out.println("<html>");
						out.println("<body>");
						out.println("<script type=\"text/javascript\">");
						out.println("if (parent.uploadFailed) parent.uploadFailed('" + e.getLocalizedMessage().replaceAll("\'|\"", "") + "');");
						out.println("</script>");
						out.println("</body>");
						out.println("</html>");
						out.flush();
						return;
					} finally {
						if (in != null)
							try {
								in.close();
							} catch (Exception e) {
							}
					}

				}
			}
			response.setContentType("text/html");
			response.setHeader("Pragma", "No-cache");
			response.setDateHeader("Expires", 0);
			response.setHeader("Cache-Control", "no-cache");
			PrintWriter out = response.getWriter();
			out.println("<html>");
			out.println("<body>");
			out.println("<script type=\"text/javascript\">");
			out.println("if (parent.uploadComplete) parent.uploadComplete('" + args.get("fileName") + "');");
			out.println("</script>");
			out.println("</body>");
			out.println("</html>");
			out.flush();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static void writeToFile(String fileName, InputStream iStream, boolean createDir, String servletRealPath) throws IOException {
		String me = "FileUtils.WriteToFile";
		if (fileName == null) {
			throw new IOException(me + ": filename is null");
		}
		if (iStream == null) {
			throw new IOException(me + ": InputStream is null");
		}

		File theFile = new File(servletRealPath + "temp_files/" + fileName);

		// Check if a file exists.
		if (theFile.exists()) {
			String msg = theFile.isDirectory() ? "directory" : (!theFile
					.canWrite() ? "not writable" : null);
			if (msg != null) {
				throw new IOException(me + ": file '" + fileName + "' is "
						+ msg);
			}
		}

		// Create directory for the file, if requested.
		if (createDir && theFile.getParentFile() != null) {
			theFile.getParentFile().mkdirs();
		}

		// Save InputStream to the file.
		BufferedOutputStream fOut = null;
		try {
			fOut = new BufferedOutputStream(new FileOutputStream(theFile));
			byte[] buffer = new byte[32 * 1024];
			int bytesRead = 0;
			while ((bytesRead = iStream.read(buffer)) != -1) {
				fOut.write(buffer, 0, bytesRead);
			}
		} catch (Exception e) {
			throw new IOException(me + " failed, got: " + e.toString());
		} finally {
			close(iStream, fOut);
		}
	}

	protected static void close(InputStream iStream, OutputStream oStream)
			throws IOException {
		try {
			if (iStream != null) {
				iStream.close();
			}
		} finally {
			if (oStream != null) {
				oStream.close();
			}
		}
	}

}