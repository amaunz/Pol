package org.opentox.pol.xml;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.UriInfo;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Serializer;

import org.opentox.pol.ErrorInfo;
import org.opentox.pol.Html2Text;
import org.opentox.pol.OpenssoHelper;
import org.opentox.pol.Rest;
import org.opentox.pol.RestException;
import org.opentox.pol.httpreturn.HttpReturn;
import org.opentox.pol.mysql.DbException;
import org.opentox.pol.mysql.MySQL;

public class PolicyHelper {
	final static String msg_dberror = "[Policy service] Exception in mysql backend. %s\n\n";
	final static String msg_tokenmissing = "[Policy service] Missing token.\n\n";
	final static String msg_token_expired = "[Policy service] Token could not be resolved to a user id. Token expired?\n\n";

	final static String msg_notanowner = "[Policy service] Not the owner of policy '%s' or policy '%s' does not exist.\n\n";
	final static String msg_ioexception = "[Policy service] IOException %s. Please contact the administrator.\n\n";
	final static String msg_token_resolved = "[Policy service] Resolved to user: '%s'";
	final static String msg_malformedXML = "[Policy service] Malformed XML\n\n";
	final static String msg_createpolicy = "[Policy service] Create Policy '%s'";
	final static String msg_policyalreadyregistered =  "Resource '%s' already registered by user '%s'.\n\n";
	final static String msg_invaliduri = "Resource '%s' is not a valid URI.\n\n";
	final static String msg_existingpolicy = "Policy '%s' already exists.\n\n";
	final static String msg_maxresources = "Policy '%s' contains more than the current maximum of %d resources. Please consider splitting your policy.";
	final static String msg_missingxml = "Missing XML file.\n\n";
	final static String msg_unsupportedencoding = "Unsupported encoding. Please use UTF-8 encoded XML files.\n\n";
	final static String msg_invalidwildcard = "Resource '%s' has illegal wildcards.\n\n";
	final static String msg_xmlserializationfailed = "XML serialization failed. %s\n\n%s\n\n";
	final static String msg_searchingpolicies = "   Searching policies of token user '%s'.";
	
	protected void log(String msg) {
		System.out.print(msg);
	}	



	protected String format(String xml) throws ParsingException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Serializer serializer = new Serializer(out);
		serializer.setIndent(4);  // or whatever you like
		Builder b = new Builder();
		Document d = b.build(xml, "");
		serializer.write(d);
		String s=out.toString("UTF-8");
		out.close();
		return s;
	}
	
	/**
	 *  Delete a Pol resource internally
	 *    
	 *  @param id
	 */
	protected String deletePolInternal(String id) throws RestException {

		OpenssoHelper opensso = new OpenssoHelper();
		String output = null;
		int status = 0;
		String token=null;
		try {
			ErrorInfo ei = opensso.doLogin();
			
			if (ei != null) {
				if (ei.status == 200) {
					token=ei.output.substring(ei.output.lastIndexOf("token.id=")+9);
					ErrorInfo c = opensso.deletePolicy(id, token);
					output = htmlToString(c.output);
					status = c.status;
				}
			}
			
		} catch (IOException e1) {
			log(getClass().getName() + ": " + e1.getMessage());
			e1.printStackTrace();
		} finally {
			try { opensso.doLogout(token);} catch (Exception x) {}
		}

		String output_short = "";
		try {
			output_short = GetSsoOutput(output);
		} catch (IOException e) {
			throw new RestException(500,String.format(msg_ioexception, e.getMessage()));
		}  
		if (output_short.length()==0) {
			throw new RestException(400,GetLastRow(output) + "\n\n");            	
		}
		if (status == 200) return output_short;
		else throw new RestException(status);
	}
	

	protected final String GetSsoOutput (String str) throws IOException { 
		String str2;
		StringReader stringReader = new StringReader(str);
		BufferedReader reader = new BufferedReader(stringReader);
		boolean found=false;
		while ((str2 = reader.readLine()) != null) {
			if (found) return str2 + "\n\n";
			if (str2.contains("OpenSSOBack to main page")) {
				found = true;
			}
		}
		reader.close();
		stringReader.close();
		return ("");
	}

	protected String htmlToString (String html) {
		StringReader in = new StringReader(html);
		Html2Text parser = new Html2Text();
		try {
			parser.parse(in);
		} catch (IOException e) {
			log("OpenssoHelper: " + e.getMessage());
			e.printStackTrace();
		}
		in.close();
		String result = parser.getText(); 
		return (result);
	}

	/**
	 * Returns last row after removing whitespaces from the end.
	 */
	protected final String GetLastRow (String str) { 
		str = str.trim();
		String[] fields = str.split("\n");
		return (fields[fields.length-1]);
	}



	/**
	 * Get policy
	 * @param subjectId
	 * @param uri
	 * @param polnames
	 * @return
	 * @throws RestException
	 * @throws DbException
	 */
	protected String getPol(String subjectId, String uri, String polnames) throws RestException, DbException {

		String res = null;
		String token_user = null;
	

		log("   S: Get pol (i)");
		// Resolve user from token
		if(subjectId == null) 
			throw new RestException(400,msg_tokenmissing);
		
		Rest r = new Rest(); 
		HttpReturn ret = r.DoIdCall(subjectId);
		token_user = ret.data;
		if (token_user == null) {
			throw new RestException(400,msg_token_expired);
		}
		log("   Token user: '" + token_user + "'");

		MySQL s = new MySQL();
		/*
		 *  URI is given: get owner of URI...
		 */
		if (uri != null) {
			log("   Searching owner of uri '" + uri + "'.");
			// get owner of URI
			res = s.getUriOwner(uri,polnames); //throws {@link DbException}
		} else {
		/*
		 *  ... or URI is *not* given: get all my policies. 
		 */
			log(String.format(msg_searchingpolicies,token_user));			
			// get all policies owned by token user
			res = s.getPoliciesByUser(token_user);

		}
		log("   E: Get pol (i)");

		return String.format("%s\n", res);
	}

	
}
