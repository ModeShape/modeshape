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
import java.util.StringTokenizer;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.jboss.logging.Logger;
import sun.misc.BASE64Decoder;

/**
 *
 * @author kulikov
 */
public class AuthFilter implements Filter {

    private final static Logger logger = Logger.getLogger(AuthFilter.class);
    
    @Override
    public void init(FilterConfig fc) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String authHeader = ((HttpServletRequest)request).getHeader("Authorization");
        logger.debug("Filter is activated");
        if (authHeader != null) {
            logger.debug("--------------Catch authentication header");
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens()) {
                String basic = st.nextToken();
                if (basic.equalsIgnoreCase("Basic")) {
                    BASE64Decoder decoder = new BASE64Decoder();
                    String userPass = new String(decoder.decodeBuffer(st.nextToken()));
                    int p = userPass.indexOf(":");
                    if (p != -1) {
                        String userID = userPass.substring(0, p);
                        String pass = userPass.substring(p + 1);
                        
                        HttpSession session = ((HttpServletRequest)request).getSession(true);
                        session.setAttribute("uname", userID); 
                        session.setAttribute("password", pass); 
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
