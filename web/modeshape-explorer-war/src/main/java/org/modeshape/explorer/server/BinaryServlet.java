package org.modeshape.explorer.server;

//import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.jcr.Item;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletOutputStream;
import javax.servlet.SingleThreadModel;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.modeshape.explorer.client.domain.LoginDetails;
//import com.sun.xml.messaging.saaj.util.ByteOutputStream;

/**
 * This Servlet is used for returing binary property values.
 */
public class BinaryServlet extends HttpServlet implements SingleThreadModel {
	private static final long serialVersionUID = 1L;

	private Log log = LogFactory.getLog(this.getClass());
	
//	private Session session;
//    private ClassPathXmlApplicationContext context 
//    = new ClassPathXmlApplicationContext(new String[] {"applicationContext.xml"});
//
//    private SessionService sessionService = (SessionService) context.getBean("sessionService");
//
//	public SessionService getSessionService() {
//		return sessionService;
//	}
//
//	public void setSessionService(SessionService sessionService) {
//		this.sessionService = sessionService;
//	}
	private LoginDetails adminLoginDetails = new LoginDetails();
	
	public void init() {
		try {
			String userName = getServletContext().getInitParameter("adminUserName");
			String password = getServletContext().getInitParameter("adminPassword");
			adminLoginDetails = new LoginDetails();
			adminLoginDetails.setUserName(userName);
			adminLoginDetails.setPassword(password);
		} catch (Exception e) {
			log.info("Failed fetching admin login details from web descriptor. " + e.getMessage());
		}
	}
	
	private Session login(String rmiUrl, String workSpace, String userName, String password){
		try {
			try {
				Repository repository = null;//new URLRemoteRepository(rmiUrl);
				SimpleCredentials creds = new SimpleCredentials(userName, password.toCharArray());
				return repository.login(creds, workSpace);
			} catch (Exception e) {
				e.printStackTrace();
				throw new Exception(e.getMessage());
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		Session session = null;
		try {
			String binaryPath = request.getParameter("path");
			log.info("Binary Content Path: "+ binaryPath);
			
			//Get default admin user name and password from web.xml if the query strings are empty
			if (null == request.getParameter("userName") || null == request.getParameter("password")) {
				session = login(request.getParameter("rmiUrl"), request.getParameter("workSpace"), 
						adminLoginDetails.getUserName(), adminLoginDetails.getPassword());
			} else {
				session = login(request.getParameter("rmiUrl"), request.getParameter("workSpace"), 
						request.getParameter("userName"), request.getParameter("password"));
			}
			Item it = session.getItem(binaryPath);
			if (it instanceof Property) {
				Property property = (Property) it;
				InputStream inputStream = property.getStream();
				ServletOutputStream servletOutputStream = response.getOutputStream();
				ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
				//byteOutputStream.write(inputStream);
				if (null != request.getParameter("mimeType") && !request.getParameter("mimeType").equals("")) {
					//response.setHeader("Content-Disposition", "attachment; filename=test.pdf");
					response.setContentType(request.getParameter("mimeType"));
					response.setHeader("Content-Disposition", " filename=" + getFileName(binaryPath) );
				}
				servletOutputStream.write(byteOutputStream.toByteArray());
				servletOutputStream.flush();
				return;
			}

		} catch (Exception e) {
			log.info("Failed to return binary stream! " + e);
		} finally {
			try {
				session.logout();
			} catch (Exception e) {
				log.info("problems while logout from repository " + e);
			}
		}
	}
	
	private String getFileName(String binaryPath) {
		String[] pathArray = binaryPath.split("/");
		return pathArray[pathArray.length-3];
	}

}
