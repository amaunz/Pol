package org.opentox.pol.xml;

import java.io.UnsupportedEncodingException;
import java.net.*;

public class UrlEncoder {

	public static String Encode(String s) {
		String res = "";

		// separete input by spaces ( URLs don't have spaces )
		String [] parts = s.split("\"");
		// Attempt to convert each item into an URL.   
		for( String item : parts ) try {
			if (res.length() != 0) {
				res = res + "\"";
			}
			URL url = new URL(item);
			// If possible then replace with anchor...
			try {
				res = res + URLEncoder.encode(url.toString(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// ignore
			}    
		} catch (MalformedURLException e) {
			// If there was an URL that was not it!...
			res = res + item;
		}
		return res;
	}

}
