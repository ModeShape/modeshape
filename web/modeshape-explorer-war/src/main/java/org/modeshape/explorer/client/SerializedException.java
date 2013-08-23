package org.modeshape.explorer.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SerializedException extends Exception implements IsSerializable {
	
	public SerializedException() {
		super();
	}

	public SerializedException(String e) {
		super(e);
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = -6553300137997976774L;

}
