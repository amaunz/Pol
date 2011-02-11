package org.opentox.pol;

public class ErrorInfo {
	
	public String output = "";   // output of command
	public int status = 0;       // status code
	
	public ErrorInfo(String o, int s) {
		output=o;
		status=s;
	}

}
