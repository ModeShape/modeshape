/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jdbc.delegate;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**

 */
public class LocalSession {
			
	private static LocalSession instance = new LocalSession();
		
	public static LocalSession getLocalSessionInstance() { return instance; }
	
	  private ThreadLocal<LocalSession> tlocal = new ThreadLocal<LocalSession>() {
		  	@Override
			protected LocalSession initialValue() {
		  		LocalSession ls = new LocalSession();
		  		LocalRepositoryDelegate.TRANSACTION_IDS.add(ls);
		  		return ls;
		    }
		  };


		private Session session;
	
	  	private void setSession(Session localSession) {
	  		session = localSession;
	  	}
	  	
	  	public LocalSession getLocalSession()  {
			return tlocal.get();
	  	}

	  	
	  	public LocalSession getLocalSession(final Repository repository, final ConnectionInfo connInfo) throws LoginException, NoSuchWorkspaceException, RepositoryException {
			LocalSession lsession = tlocal.get();
	  		
	  		Credentials credentials = connInfo.getCredentials();
			String workspaceName = connInfo.getWorkspaceName();
			Session session = null;
			if (workspaceName != null) {
				session = credentials != null ? repository.login(credentials,
						workspaceName) : repository.login(workspaceName);
			} else {
				session = credentials != null ? repository.login(credentials)
						: repository.login();
			}
			// this shouldn't happen, but in testing it did occur only because of
			// the repository not being setup correctly
			assert session != null;
			
			lsession.setSession(session);
	
			return lsession;
	  	}
	
	
	  	
	  	public  Session getSession() {
	  		return session;
	  	}
	  	
	  	public void remove() {
	  		tlocal.remove();
	  		LocalRepositoryDelegate.TRANSACTION_IDS.remove(this);
	  		session.logout();  	
	  	}
	  	   	
	  	@Override
		public String toString() {
	  		StringBuffer sb = new StringBuffer("Session:");
	  		sb.append(session.toString());    		
	  		return sb.toString();
	  	}

};
