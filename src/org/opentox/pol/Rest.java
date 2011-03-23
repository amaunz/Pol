package org.opentox.pol;

import java.io.*;
import java.net.*;
import org.opentox.pol.httpreturn.*;
import java.net.URLEncoder;
import java.util.Properties;


public class Rest {

	private String sso_url = "";
	public URLConnection c;
	public Rest(){
		c=null;
		InputStream fis = null;
		String propfile = "org/opentox/pol/admin.properties";
		fis = OpenssoHelper.class.getClassLoader().getResourceAsStream(propfile);
		Properties config = new Properties();
		try {
			config.load(fis);
			sso_url = config.getProperty("host");
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				fis.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public void Connect(URL u) {
		try {
			c = u.openConnection();
		}
		catch (IOException e) {
			System.out.println("Rest: " + e.getMessage());
		}
	}

	public void Send(HttpURLConnection urlc, String data) {
		urlc.setDoOutput(true);
		urlc.setAllowUserInteraction(false);
		PrintStream ps = null;
		OutputStream outputStream = null;
		try {
			outputStream = urlc.getOutputStream();
			ps = new PrintStream(outputStream);
			ps.print(data);
			outputStream.flush();
		}
		catch (IOException e){
			System.out.println("Rest: Could not open output stream: " + e.getMessage());
		}
		finally {
			ps.close();
			try {
				outputStream.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public HttpReturn DoIdCall(String subjectid) {

		int status = 0;
		String string = null;
		PrintStream ps = null;
		BufferedReader br = null;
		InputStreamReader iss = null;
		HttpURLConnection urlc = null;
		OutputStream outputStream = null;
		InputStream inputStream = null;

		try {

			//set data
			String data = "subjectid=" + URLEncoder.encode(subjectid.toString(),"UTF-8");
			data += "&attributes_names=uid";

			//make connection
			URL url = new URL(sso_url + "/identity/attributes");
			Connect(url);
			urlc = (HttpURLConnection) c;

			//use post mode
			urlc.setDoOutput(true);
			urlc.setAllowUserInteraction(false);

			//send query
			outputStream = urlc.getOutputStream();
			ps = new PrintStream(outputStream);
			ps.print(data);
			outputStream.flush();


			//get result
			inputStream = urlc.getInputStream();
			iss = new InputStreamReader(inputStream);
			br = new BufferedReader(iss);
			status = urlc.getResponseCode();
			String l = "";
			boolean found=false;
			while ((l=br.readLine())!=null) {
				if (l.indexOf("userdetails.attribute.name=uid")!=-1) {
					found=true;
					break;
				}
			}
			if (found) {
				l=br.readLine();
				string=l.substring(l.indexOf('=')+1);
			}
			if (string == null) System.out.println("NAME IS NULL");
		} 
		catch (IOException e) {
		}
		finally {
			try {
				inputStream.close();
				outputStream.close();
				ps.close();
				urlc.disconnect();
				br.close();
				iss.close();
			} catch (IOException e) {
				// ignore
			}
		}

		return new HttpReturn(status, string);

	}

	public HttpReturn LogOut(String subjectid) {   	
		int status = 0;
		PrintStream ps=null;
		HttpURLConnection urlc=null;
		OutputStream outputStream = null;
		
		try {
			//set data
			String data = "subjectid=" + URLEncoder.encode(subjectid.toString(),"UTF-8");

			//make connection
			URL url = new URL(sso_url + "/identity/logout");
			Connect(url);
			urlc = (HttpURLConnection) c;

			//use post mode
			urlc.setDoOutput(true);
			urlc.setAllowUserInteraction(false);

			//send query
			outputStream = urlc.getOutputStream();
			ps = new PrintStream(outputStream);
			ps.print(data);

			//get result
			status = urlc.getResponseCode();

		} 
		catch (IOException e) {
		}
		finally {
			ps.close();
			try {
				outputStream.close();
			} catch (IOException e) {
				// ignore
			}
			urlc.disconnect();
		}
		return new HttpReturn(status, "");  
	}
}

