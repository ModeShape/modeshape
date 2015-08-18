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
package org.modeshape.web.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 *
 * @author kulikov
 */
public class RemoteException extends Exception implements IsSerializable {
    public final static int SECURITY_ERROR = 1;
    
    private static final long serialVersionUID = 1L;
    private int code;
    private String msg;
    
    public RemoteException() {
        super();
    }

    public RemoteException(String e) {
        super(e);
    }
    
    public RemoteException(int code, String e) {
        super(e);
        this.code = code;
    }
    
    public RemoteException(Exception e) {
        msg = e.getMessage();
        if (msg == null || msg.length() == 0) {
            msg = e.getClass().getName();
        }
    }
    
    @Override
    public String getMessage() {
        return msg != null ? msg : super.getMessage();
    }
    
    public int code() {
        return code;
    }
}
