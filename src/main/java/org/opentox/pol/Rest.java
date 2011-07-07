package org.opentox.pol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Properties;

import org.opentox.pol.httpreturn.HttpReturn;


public class Rest {

	private String sso_url = "";
	public URLConnection c;
	
	public Rest() throws RestException {
		c=null;
		InputStream fis = null;
		String propfile = "org/opentox/pol/admin.properties";
		fis = OpenssoHelper.class.getClassLoader().getResourceAsStream(propfile);
		if (fis==null) throw new RestException(500,"Can't load "+propfile);
		Properties config = new Properties();
		try {
			config.load(fis);
			sso_url = config.getProperty("host");
		} catch (IOException e) {
			throw new RestException(500,e);
		}
		finally {
			try {
				if (fis!=null) fis.close();
			} catch (IOException e) {}
		}
	}

	public void Connect(URL u) throws IOException {
		c = u.openConnection();
	}

	public void Send(HttpURLConnection urlc, String data) throws RestException {
		urlc.setDoOutput(true);
		urlc.setAllowUserInteraction(false);
		PrintStream ps = null;
		OutputStream outputStream = null;
		int code = -1;
		try {
			outputStream = urlc.getOutputStream();
			ps = new PrintStream(outputStream);
			ps.print(data);
			outputStream.flush();
			if (urlc.getResponseCode()!=200)
				throw new RestException(code,urlc.getResponseMessage());
		}
		catch (RestException e) {
			throw e;
		}
		catch (Exception e){
			
			try { code = urlc.getResponseCode(); } catch (Exception x) {}
			throw new RestException(code,e);
		}
		finally {
			if (ps!= null) ps.close();
			try {
				if (outputStream!=null) outputStream.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public HttpReturn DoIdCall(String subjectid) throws RestException  {

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
			String data = String.format("subjectid=%s&attributes_names=uid", URLEncoder.encode(subjectid.toString(),"UTF-8"));

			//make connection
			URL url = new URL(String.format("%s/identity/attributes",sso_url));
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
			
			if (status == 200) {
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
			} else 
				throw new RestException(status,urlc.getResponseMessage());
			
		} 
		catch (org.apache.wink.common.RestException e) {
			throw e;
		} catch (Exception e) {
			int code = -1;
			try { code = urlc.getResponseCode(); } catch (Exception x) {}
			throw new RestException(code,e);
		}
		finally {
			try {
				if (inputStream !=null) inputStream.close();
				if (outputStream != null) outputStream.close();
				if (ps != null) ps.close();

				if (br != null) br.close();
				if (iss != null) iss.close();
			} catch (IOException e) {
				// ignore
				e.printStackTrace();
			}
			try {
				
				if (urlc != null) urlc.disconnect();

			} catch (Exception e) {
				// ignore
				e.printStackTrace();
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
		catch (Exception e) {
			try {status = urlc.getResponseCode();} catch (Exception x) {}

		}
		finally {
			if (ps != null) ps.close();
			try {
				if (outputStream != null) outputStream.close();
			} catch (IOException e) {
				// ignore
			}
			try { 
				if (urlc!=null) urlc.disconnect();
			
			} catch (Exception x) {}
		}
		return new HttpReturn(status, "");  
	}
}

