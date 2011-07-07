package org.opentox.pol.mysql;

public class DbException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3506275229841493058L;

    public DbException() {
    	super();
    }

    public DbException(String message) {
    	super(message);
    }

   
     public DbException(String message, Throwable cause) {
            super(message, cause);
     }

     public DbException(Throwable cause) {
            super(cause);
     }
}
