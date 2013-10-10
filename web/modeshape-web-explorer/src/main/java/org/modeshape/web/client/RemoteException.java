/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.client;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 *
 * @author kulikov
 */
public class RemoteException extends Exception implements IsSerializable {

    public RemoteException() {
        super();
    }

    public RemoteException(String e) {
        super(e);
    }
}
