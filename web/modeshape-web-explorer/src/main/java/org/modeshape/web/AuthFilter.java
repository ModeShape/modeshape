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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.modeshape.common.util.Base64;

/**
 *
 * @author kulikov
 */
public class AuthFilter implements Filter {
    private final static Logger logger = Logger.getLogger("AuthFilter");
    
    @Override
    public void init(FilterConfig fc) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String authHeader = ((HttpServletRequest)request).getHeader("Authorization");
        HttpSession session = ((HttpServletRequest)request).getSession(true);
        
        String userID = null;
        String pass = null;
        
        if (authHeader != null) {
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens()) {
                String basic = st.nextToken();
                if (basic.equalsIgnoreCase("Basic")) {
                    String userPass = new String(Base64.decode(st.nextToken()));
                    int p = userPass.indexOf(":");
                    if (p != -1) {
                        userID = userPass.substring(0, p);
                        pass = userPass.substring(p + 1);
                        
                    }
                }
            }
        } else {
            //for based?
/*            String params = Stream2String(request.getInputStream());
            logger.info("Params=" + params);
            String[] credentials = params.split("&");
            
            if (credentials.length != 2) {
                throw new ServletException("Unknown authentication method");
            }
            
            userID = credentials[0].split("=")[0];
            pass = credentials[1].split("=")[0];
            */ 
        } 

        session.setAttribute("uname", userID); 
        session.setAttribute("password", pass); 
        chain.doFilter(request, response);
    }

    private String Stream2String(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            bout.write((byte)b);
        }
        return bout.toString();
    }
    
    @Override
    public void destroy() {
    }
}
