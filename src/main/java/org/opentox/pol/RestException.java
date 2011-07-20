package org.opentox.pol;

public class RestException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = -4255914900958587831L;
	protected int httpcode = -1;
	
	public int getHttpcode() {
		return httpcode;
	}


	public void setHttpcode(int httpcode) {
		this.httpcode = httpcode;
	}


	public RestException(int code) {
    	super();
    	this.httpcode = code;
    }


    public RestException(int code,String message) {
    	super(message);
    	this.httpcode = code;
    }

  
    public RestException(int code,String message, Throwable cause) {
        super(message, cause);
        this.httpcode = code;
    }

    public RestException(int code,Throwable cause) {
        super(cause);
        this.httpcode = code;
    }
    @Override
    public String toString() {
    	return String.format("[%d] %s",super.toString());
    }
}
