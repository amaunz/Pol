package org.opentox.pol;

import java.net.*;
import java.io.*;
import java.util.*;

public class OpenssoHelper {

	
	private String ssoadm_create = "/ssoadm.jsp?cmd=create-policies";
	private String ssoadm_delete = "/ssoadm.jsp?cmd=delete-policies";
	private String ssoadm_list= "/ssoadm.jsp?cmd=list-policies";
	private String authenticate = "/identity/authenticate";
	private String logout = "/identity/authenticate/logout";
	private String url = "";
	private String user = "";
	private String pw = "";
	Rest r = null;

	public OpenssoHelper(){
		r = new Rest();
		InputStream fis = null;
		String propfile = "org/opentox/pol/admin.properties";
		fis = OpenssoHelper.class.getClassLoader().getResourceAsStream(propfile);
		Properties config = new Properties();
		try {
			config.load(fis);
			user = config.getProperty("user"); 
			pw = config.getProperty("pw");
			url = config.getProperty("host");
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


	public ErrorInfo doLogin() throws IOException {

		String data = null;
		ErrorInfo ei = null;
		InputStreamReader iss = null;
		BufferedReader br = null;

		try {
			data = "username=" + URLEncoder.encode(user,"UTF-8") + "&password=" + URLEncoder.encode(pw,"UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("OpenssoHelper: " + e.getMessage());
			e.printStackTrace();
		}

		if (data != null) {
			try {
				r.Connect(new URL(url+authenticate));
			}
			catch (MalformedURLException e) {
				System.out.println("OpenssoHelper: " + e.getMessage());
				e.printStackTrace();
			}

			HttpURLConnection urlc = (HttpURLConnection) r.c;
			r.Send(urlc, data);

			String answer = null;
			int status=0;
			
			iss = new InputStreamReader(urlc.getInputStream());
			br = new BufferedReader(iss);
			answer=BrToString(br);
			status = urlc.getResponseCode();
			if (answer != null) {
				ei = new ErrorInfo(answer, status);
			}
			iss.close();
			br.close();
		}

		return ei;

	}
	
	public ErrorInfo doLogout(String token) throws IOException {

		String data = null;
		ErrorInfo ei = null;
		InputStreamReader iss = null;
		BufferedReader br = null;

		try {
			data = "subjectid=" + URLEncoder.encode(token,"UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("OpenssoHelper: " + e.getMessage());
			e.printStackTrace();
		}

		if (data != null) {
			try {
				r.Connect(new URL(url+logout));
			}
			catch (MalformedURLException e) {
				System.out.println("OpenssoHelper: " + e.getMessage());
				e.printStackTrace();
			}

			HttpURLConnection urlc = (HttpURLConnection) r.c;
			r.Send(urlc, data);

			String answer = null;
			int status=0;
			iss = new InputStreamReader(urlc.getInputStream());
			br = new BufferedReader(iss);
			answer=BrToString(br);
			status = urlc.getResponseCode();
			if (answer != null) {
				ei = new ErrorInfo(answer, status);
			}
			iss.close();
			br.close();
		}

		return ei;

	}


	public ErrorInfo createPolicy(String xml, String token) throws IOException {

		String realm="/";
		String data = null;
		ErrorInfo ei = null;
		InputStreamReader iss = null;
		BufferedReader br = null;

		try {
			data = "realm=" + URLEncoder.encode(realm,"UTF-8") + 
			"&xmlfile=" + URLEncoder.encode(xml,"UTF-8") + 
			"&submit=" + URLEncoder.encode("Submit","UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("OpenssoHelper: " + e.getMessage());
			e.printStackTrace();
		}

		if (data != null) {
			try {
				r.Connect(new URL(url+ssoadm_create));
			}
			catch (MalformedURLException e) {
				System.out.println("OpenssoHelper: " + e.getMessage());
				e.printStackTrace();
			}

			HttpURLConnection urlc = (HttpURLConnection) r.c;
			urlc.addRequestProperty("Cookie","iPlanetDirectoryPro=\"" + token + "\"");
			r.Send(urlc, data);

			String answer = null;
			int status=0;
			iss = new InputStreamReader(urlc.getInputStream());
			br = new BufferedReader(iss);
			answer = BrToString(br);
			status = urlc.getResponseCode();
			if (answer != null) {
				ei = new ErrorInfo(answer, status);
			}
			iss.close();
			br.close();
		}

		return ei;

	}

	public ErrorInfo deletePolicy(String polname, String token) throws IOException {

		String realm="/";
		String data = null;
		ErrorInfo ei = null;
		InputStreamReader iss = null;
		BufferedReader br = null;
		
		try {
			data = "policynames=" + URLEncoder.encode(polname,"UTF-8") + 
			"&realm=" + URLEncoder.encode(realm,"UTF-8") + 
			"&submit=" + URLEncoder.encode("Submit","UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("OpenssoHelper: " + e.getMessage());
			e.printStackTrace();
		}

		if (data != null) {
			try {
				r.Connect(new URL(url+ssoadm_delete));
			}
			catch (MalformedURLException e) {
				System.out.println("OpenssoHelper: " + e.getMessage());
				e.printStackTrace();
			}

			HttpURLConnection urlc = (HttpURLConnection) r.c;
			urlc.addRequestProperty("Cookie","iPlanetDirectoryPro=\"" + token + "\"");
			r.Send(urlc, data);

			String answer = null;
			int status=0;
			iss = new InputStreamReader(urlc.getInputStream());
			br = new BufferedReader(iss);
			answer = BrToString(br);
			status = urlc.getResponseCode();
			if (answer != null) {
				ei = new ErrorInfo(answer, status);
			}
			iss.close();
			br.close();
		}

		return ei;

	}

	public ErrorInfo listPolicy(String polname, String token) throws IOException {

		String realm="/";
		String data = null;
		ErrorInfo ei = null;
		InputStreamReader iss = null;
		BufferedReader br = null;

		try {
			data = "policynames=" + URLEncoder.encode(polname,"UTF-8") + 
			"&realm=" + URLEncoder.encode(realm,"UTF-8") + 
			"&submit=" + URLEncoder.encode("Submit","UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("OpenssoHelper: " + e.getMessage());
			e.printStackTrace();
		}

		if (data != null) {
			try {
				r.Connect(new URL(url+ssoadm_list));
			}
			catch (MalformedURLException e) {
				System.out.println("OpenssoHelper: " + e.getMessage());
				e.printStackTrace();
			}

			HttpURLConnection urlc = (HttpURLConnection) r.c;
			urlc.addRequestProperty("Cookie","iPlanetDirectoryPro=\"" + token + "\"");
			r.Send(urlc, data);

			String answer = null;
			int status=0;
			iss = new InputStreamReader(urlc.getInputStream());
			br = new BufferedReader(iss);
			answer = BrToString(br);
			status = urlc.getResponseCode();
			if (answer != null) {
				ei = new ErrorInfo(answer, status);
			}
			iss.close();
			br.close();
		}

		return ei;

	}


	/*
	public String readFile() throws IOException {
		File file = new File("/home/am/aa/Pol-REST/andi.xml");
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		String fcont = "";

		try {
			fis = new FileInputStream(file);
			bis = new BufferedInputStream(fis);
			fcont = BrToString(new BufferedReader(new InputStreamReader(bis)));
			fis.close();
			bis.close();

		} catch (FileNotFoundException e) {
			System.out.println("OpenssoHelper: " + e.getMessage());
			e.printStackTrace();
		} 
		return fcont;
	}
	*/


	public String BrToString (BufferedReader br) {
		String line = null;
		String contents="";
		try {
			while ((line=br.readLine()) != null) {
				contents += line;
			}
			br.close();
		}
		catch (IOException e) {
			System.out.println("OpenssoHelper: " + e.getMessage());
			e.printStackTrace();
		}
		return contents;
	}


	//	  public static void main(String[] args) throws Exception {
	//
	//	    OpenssoHelper h = new OpenssoHelper();
	//	    ErrorInfo ei = h.doLogin();
	//	    String token=null;
	//	    if (ei != null) {
	//	      if (ei.status == 200) {
	//	        token=ei.output.substring(ei.output.lastIndexOf("token.id=")+9);
	//	        System.out.println(token);
	//
	//	        ErrorInfo d = h.deletePolicy("andi",token);
	//	        System.out.println(d.output);
	//	        System.out.println(d.status);
	//
	//	        
	//	        String xml = h.readFile();
	//	        ErrorInfo c = h.createPolicy(xml,token);
	//	        System.out.println(c.output);
	//	        System.out.println(c.status);
	//
	//
	//	        ErrorInfo l = h.listPolicy("andi",token);
	//	        System.out.println(l.output);
	//	        System.out.println(l.status);
	//
	//
	//	      }
	//	    }
	//
	//
	//	  }

}
