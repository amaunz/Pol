package org.opentox.pol;

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

import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Serializer;

import org.opentox.pol.httpreturn.HttpReturn;
import org.opentox.pol.mysql.MySQL;
import org.opentox.pol.xml.ParsePolicy;
import org.opentox.pol.xml.Policy;


/**
 * A Singleton class that uses an in-memory map to keep 
 * the pol objects. This way, we maintain the state of the 
 * resources in memory. 
 * 
 */
@Path("/opensso-pol")
public class PolServiceImpl implements PolService {
	final static String msg_tokenmissing = "[Policy service] Missing token.\n\n";
	final static String msg_token_expired = "[Policy service] Token could not be resolved to a user id. Token expired?\n\n";
	final static String msg_dberror = "[Policy service] Exception in mysql backend. %s\n\n";
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
	
	final static String mime_text = "text/plain";
	
	private static PolServiceImpl instance = null;

	private PolServiceImpl() {
	}

	public synchronized static PolServiceImpl getInstance() {
		if(instance == null) {
			instance = new PolServiceImpl();
		}
		return instance;
	}
	public void log(String msg) {
		System.out.print(msg);
	}
	/**
	 * Create a Pol resource to respond to an HTTP request
	 * 
	 *     POST /opensso-pol
	 *         
	 *  @param uriInfo
	 *  @param Pol
	 *  @param subjectid
	 *  @return Response
	 */
	public Response createPol(@HeaderParam("subjectid") String subjectId, @Context UriInfo uriInfo, InputStream is) throws WebApplicationException {

		WebApplicationException exception=null;		// general purpose exception (thrown when status != null)

		File temp2 = null;							// Temp file without XML header 
		String token_user = null;					// user as encoded by subjectid (=token)

		MySQL s = new MySQL();						// database object to store rows of (policy name, user, resource).
		Iterator<Policy> it;						// runs policies found in the XML

		// READING 
		BufferedReader reader = null;
		InputStreamReader iss = null;
		FileOutputStream out2 = null;
		PrintStream p2 = null;

		// PARSE XML
		ParsePolicy pp = null;
		String polName = null;
		ArrayList<String> resNames = null;
		String[] resource_registered_name = new String[2];
		String db_user = null;
		String db_resource = null;

		boolean resources_untouched = true;			// Control conditions on uploaded policy
		boolean IP_check_ok = true;					//
		boolean policy_is_new = true;				//
		boolean nr_resources_below_max = true;		//
		final int max_nr_resources_per_pol = 2000;	//

		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();

		log("\nS: Create pol");

		try {

			// Find token user
			if(subjectId == null) {
				log(msg_tokenmissing);
				throw new WebApplicationException(Response.status(400).entity(msg_tokenmissing).type(mime_text).build());
			}
			Rest r = new Rest(); 
			HttpReturn ret = r.DoIdCall(subjectId);
			token_user = ret.data;
			if (token_user == null) {
				log(msg_token_expired);
				throw new WebApplicationException(Response.status(400).entity(msg_token_expired).type(mime_text).build());
			}
			log(String.format(msg_token_resolved,token_user));


			// Read XML
			if (is != null) {
				String line;
				try {
					iss = new InputStreamReader(is, "UTF-8");
					reader = new BufferedReader(iss);
					boolean body = false;
					while ((line = reader.readLine()) != null) {
						sb.append(line).append("\n");
						if (line.contains("<Policies>")) body = true;
						if (body) sb2.append(line).append("\n");
					}
					temp2 = File.createTempFile("opensso-policy2-",".xml");				
					out2 = new FileOutputStream(temp2);	
					p2 = new PrintStream (out2); 
					p2.println(sb2.toString());

					//temp2.deleteOnExit();
				}
				finally {
					p2.close();
					out2.close();
					reader.close();
					iss.close();
					is.close();
				}
			} 
			else {

				exception=new WebApplicationException(Response.status(400).entity(msg_missingxml).type(mime_text).build());
			}
		} catch(UnsupportedEncodingException e) {

			exception=new WebApplicationException(Response.status(400).entity(msg_unsupportedencoding).type(mime_text).build());
		} catch(IOException e) {
			exception=new WebApplicationException(Response.status(500).entity(String.format(msg_ioexception, e.getMessage())).type(mime_text).build());
		}
		//		finally {
		if (exception!=null) {
			temp2.delete();
			log(exception.getMessage());
			throw exception;
		}
		//		}

		else {
			
			pp = new ParsePolicy();
			ArrayList<Policy> polres = null;
			try {
				polres = pp.runParser(temp2.getAbsolutePath());
			}
			catch (Exception e) {
				e.printStackTrace();
				log(msg_malformedXML);
				throw new WebApplicationException(Response.status(400).entity(msg_malformedXML).type(mime_text).build());
			}
			finally {
				temp2.delete();
			}


			it = polres.iterator();

			while(it.hasNext()) {
				Policy p = it.next();
				polName=p.getName();
				log(String.format(msg_createpolicy,polName));
				resNames=p.getResources();
				// Proceed only for new policy name
				s.open();
				if (!(s.search_pol_name(polName))) {
					s.close();
					if (resNames.size() <= max_nr_resources_per_pol) {
						for (int i=0; i<resNames.size(); i++) {
							s.open();
							resource_registered_name = s.search_res(resNames.get(i));
							s.close();
							// check resource URI
							if (resource_registered_name[0] != null) {
								db_resource = resource_registered_name[0];
								db_user = resource_registered_name[1];
							}
							if (db_resource != null && !(db_user.equals(token_user))) {
								resources_untouched = false;
								break;
							}
						}
					}
					else {
						s.close();
						nr_resources_below_max = false;
					}
				} // end if (!(s.search_pol_name(polName)))
				else policy_is_new = false;

				if (!resources_untouched || !IP_check_ok || !nr_resources_below_max || !policy_is_new ) {
					break;
				}

			} // end while loop for policies.
 

			if (!resources_untouched) {

				
				log(String.format(msg_policyalreadyregistered,db_resource, db_user));		
				throw new WebApplicationException(Response.status(400).entity(String.format(msg_policyalreadyregistered,db_resource, db_user)).type(mime_text).build());
			}
			if (!policy_is_new) {

				log(String.format(msg_existingpolicy,polName));		
				throw new WebApplicationException(Response.status(400).entity(String.format(msg_existingpolicy,polName)).type(mime_text).build());
			}
			if (!nr_resources_below_max) {
				log(String.format(msg_maxresources,polName ,max_nr_resources_per_pol));		
				throw new WebApplicationException(Response.status(400).entity(String.format(msg_maxresources,polName ,max_nr_resources_per_pol)).type(mime_text).build());
			}

			// PROCESS REQUEST
			it = polres.iterator();
			while(it.hasNext()) {
				Policy p = it.next();
				resNames=p.getResources();

				// New: search through list of res names
				for (int i=0; i<resNames.size(); i++) {
					String resName=resNames.get(i);
					URL aURL = null;
					try {
						aURL = new URL(resName);
					} catch (MalformedURLException e) {

						throw new WebApplicationException(Response.status(400).entity(String.format(msg_invaliduri,resName)).type(mime_text).build());
					}
					String path = aURL.getPath();


					// Disallow global wildcards
					if (resName.indexOf("*") != -1) {
						boolean ok = false;
						if (path.indexOf("-*-") != -1) {
							if (path.contains("/dataset/-*-")) { ok=true; }
							if (path.contains("/feature/-*-")) { ok=true; }
							if (path.contains("/compound/-*-")) { ok=true; }
							if (path.contains("/metadata/-*-")) { ok=true; }
							if (path.contains("/conformer/-*-")) { ok=true; }
							if (path.contains("/model/-*-")) { ok=true; }
							if (path.contains("/algorithm/CDKPhysChem/-*-")) { ok=true; }
							if (path.contains("/algorithm/JOELIB2/-*-")) { ok=true; }
						}
						if (!ok) {

							throw new WebApplicationException(Response.status(400).entity(String.format(msg_invalidwildcard,resName)).type(mime_text).build());
						}
					}
				}
				// New: search through list of res names
			}


			String output = "";
			int status = 0;
			String output_short = "";
			OpenssoHelper opensso = new OpenssoHelper();
			try {
				ErrorInfo ei = opensso.doLogin();
				String token=null;
				if (ei != null) {
					if (ei.status == 200) {
						token=ei.output.substring(ei.output.lastIndexOf("token.id=")+9);
						log(token);
						ErrorInfo c = opensso.createPolicy(sb.toString(), token);
						output = htmlToString(c.output);
						status=c.status;
						output_short = GetSsoOutput(output);
					}
				}
				opensso.doLogout(token);
			} catch (IOException e) {
				throw new WebApplicationException(Response.status(500).entity(String.format(msg_ioexception, e.getMessage())).type(mime_text).build());
			}            
			if (output_short.length()==0 || status != 200) {
				throw new WebApplicationException(Response.status(400).entity(GetLastRow(output) + "\n\n").type(mime_text).build());            	
			}

			it = polres.iterator();
			while(it.hasNext()) {

				Policy p = it.next();
				polName=p.getName();
				resNames=p.getResources();

				try {	
					// Search through list of res names       	
					for (int i=0; i<resNames.size(); i++) {
						s.open();
						s.add(polName,token_user,resNames.get(i));
						s.close();
					}

				}
				catch (Exception e) {  // must roll back policy insertions
					it = polres.iterator();
					while(it.hasNext()) {
						Policy dp = it.next();
						polName=dp.getName();
						try {
							deletePolInternal(polName);
						}
						catch (Exception de){}
					}
					throw new WebApplicationException(Response.status(500).entity(String.format(msg_dberror,e.getMessage())).type(mime_text).build());
				}
				finally {
					s.close();
				}
			}
			log("E: Create pol");
			return Response.ok(output_short).type(mime_text).build();			
		}
	}


	/**
	 *  Return a Pol resource in response to an HTTP request
	 *  
	 *     GET /opensso-pol/{id}
	 *  
	 *  @param id
	 *  @return Response
	 */
	public Response getPolID(@HeaderParam("subjectid") String subjectId, @HeaderParam("id") String id, @HeaderParam("uri") String uri, @HeaderParam("polnames") String polnames)
	throws WebApplicationException {
		
		
		//moved upfront - first check if the token is here, before db access
		if(subjectId == null) {
			throw new WebApplicationException(Response.status(400).entity(msg_tokenmissing).type(mime_text).build());
		}
		
		log("\nS: Get pol ID\n");

		// If 'id' is *not* set, gather information about my URIs or a specific (but otherwise arbitrary) URI... 
		if (id == null) {
			Response r = getPol(subjectId, uri, polnames);
			log("E: Get pol ID\n");
			return r;
		}

		// ... else deliver a specific one of my policies.
		MySQL s = new MySQL();	// database object to store rows of (policy name, user, resource).
		String db_user = null;
		String token_user=null;

		// Get user who created policy "id"
		try {
			s.open();
			db_user = s.search_user_by_pol(id);
			
		}
		catch (Exception e) {
			throw new WebApplicationException(Response.status(500).entity(String.format(msg_dberror,e.getMessage())).type(mime_text).build());
		}
		finally {
			if (s!=null) s.close();
		}
		
		if (db_user == null) { //policy id not found, but better not to reveal this, just say not an owner
			throw new WebApplicationException(Response.status(401).entity(String.format(msg_notanowner,id,id)).type(mime_text).build());
		}
		log("id: '" + id + "'\n");
		log("db user: '" + db_user + "'\n");

		// Resolve user from token
		Rest r = new Rest(); 
		HttpReturn ret = r.DoIdCall(subjectId);
		token_user = ret.data;
		if (token_user == null) {
			throw new WebApplicationException(Response.status(400).entity(msg_token_expired).type(mime_text).build());
		}
		log("token user: '" + token_user + "'");

		// delete entries
		if (db_user != null && token_user != null && db_user.equals(token_user)) {

			OpenssoHelper opensso = new OpenssoHelper();
			String output = null;
			try {
				ErrorInfo ei = opensso.doLogin();
				String token=null;
				if (ei != null) {
					if (ei.status == 200) {
						token=ei.output.substring(ei.output.lastIndexOf("token.id=")+9);
						log(token);
						ErrorInfo c = opensso.listPolicy(id, token);
						output = htmlToString(c.output);
					}
				}
				opensso.doLogout(token);
				//output = (opensso.listPolicy("/", id)).toString();
			}
			catch (IOException e) {
				log("PolServiceImpl: " + e.getMessage());
				e.printStackTrace();
			}

			try {
				output = output.substring(output.indexOf("<Policies>"),output.length()); // incorporated XML formatting
			} catch (StringIndexOutOfBoundsException e) {
				
				throw new WebApplicationException(Response.status(500).entity(String.format(msg_xmlserializationfailed,e.getMessage(),output)).type(mime_text).build());
			}

			log("\n");

			try {
				output = format(output);
			} catch (ParsingException e) {
				throw new WebApplicationException(Response.status(500).entity(String.format(msg_xmlserializationfailed,e.getMessage(),output)).type(mime_text).build());
			} catch (IOException e) {
				throw new WebApplicationException(Response.status(500).entity(String.format(msg_xmlserializationfailed,e.getMessage(),output)).type(mime_text).build());
			}
			//System.out.println(output);	    	
			log("E: Get pol ID");
			return Response.ok(output).type("text/xml").build();

		}
		else {
			
			throw new WebApplicationException(Response.status(401).entity(String.format(msg_notanowner,id,id)).type(mime_text).build());
		}

	}

	// Get
	public Response getPol(String subjectId, String uri, String polnames) {

		String res = null;
		String token_user = null;
		MySQL s = null;

		log("   S: Get pol (i)");
		// Resolve user from token
		if(subjectId == null) {
			throw new WebApplicationException(Response.status(400).entity(msg_tokenmissing).type(mime_text).build());
		}
		Rest r = new Rest(); 
		HttpReturn ret = r.DoIdCall(subjectId);
		token_user = ret.data;
		if (token_user == null) {
			throw new WebApplicationException(Response.status(400).entity(msg_token_expired).type(mime_text).build());
		}
		log("   Token user: '" + token_user + "'");

		// URI is given: get owner of URI...
		if (uri != null) {
			log("   Searching owner of uri '" + uri + "'.");
			// get owner of URI
			try {
				s = new MySQL();
				s.open();
				String[] res_arr;
				if (polnames == null) {
					res_arr = s.search_res(uri);
					res = res_arr[1];
				}
				else {
					log("   => with pol names.");
					res_arr = s.search_res_pol(uri);
					res = res_arr[1];
					res += res_arr[2];
				}

			}
			catch (Exception e) {
				throw new WebApplicationException(Response.status(500).entity(String.format(msg_dberror,e.getMessage())).type(mime_text).build());
			}
			finally {
				s.close();
			}
		}

		// ... or URI is *not* given: get all my policies. 
		else {


			log(String.format(msg_searchingpolicies,token_user));			
			// get all policies owned by token user
			try {
				s = new MySQL();
				s.open();
				res = s.search_users_pols(token_user);
			}
			catch (Exception e) {
				throw new WebApplicationException(Response.status(500).entity(String.format(msg_dberror,e.getMessage())).type(mime_text).build());
			}
			finally {
				s.close();
			}
		}

		log("   E: Get pol (i)");
		return Response.ok(res + "\n").type(mime_text).build();

	}








	/**
	 *  Delete a Pol resource in response to an HTTP request
	 *  
	 *     DELETE /opensso-pol/{id}
	 *    
	 *  @param id
	 */
	public Response deletePol(@HeaderParam("subjectid") String subjectId, @HeaderParam("id") String id) 
	throws WebApplicationException {

		MySQL s = new MySQL();	// database object to store rows of (policy name, user, resource).
		String db_user = null;
		String token_user = null;
		List<String> res = null;

		System.out.println("\nS: Del pol");
		System.out.println("Policy '" + id + "'");
		// Get user who created policy "id"
		try {
			s.open();
			db_user = s.search_user_by_pol(id);
		}
		catch (Exception e) {
			throw new WebApplicationException(Response.status(500).entity(String.format(msg_dberror,e.getMessage())).type(mime_text).build());
		}
		finally {
			s.close();
		}
		System.out.println("db user: '" + db_user + "'");


		// Resolve user from token
		if(subjectId == null) {
			throw new WebApplicationException(Response.status(400).entity(msg_tokenmissing).type(mime_text).build());
		}
		Rest r = new Rest(); 
		HttpReturn ret = r.DoIdCall(subjectId);
		token_user = ret.data;
		if (token_user == null) {
			throw new WebApplicationException(Response.status(400).entity(msg_token_expired).type(mime_text).build());
		}
		log("token user: '" + token_user + "'");


		// delete entries
		if (db_user != null && token_user != null && db_user.equals(token_user)) {

			String msg = "";
			// delete db entry
			boolean db_entry_deleted = true;
			try {
				s.open();
				res = s.search_pol(id);
				s.delete_pol(id);
				s.close();
			}
			catch (Exception e) {
				msg = e.getMessage();
				db_entry_deleted = false;  	
			}

			Response dr = null;
			if (db_entry_deleted) {  	
				dr = deletePolInternal(id);
				if (dr.getStatus() != 200) {
					// roll back db entry
					try {
						for (int i = 0; i < res.size(); i++) {
							String[] temp = res.get(i).split(":::");
							String pol = temp[0];
							String user = temp[1];
							String resource = temp[2];
							s.open();
							s.add(pol,user,resource);
							s.close();
						}
					}
					catch (Exception e) {
					}
				}
				log("E: Del pol");
				return dr;
			}
			else {
				return Response.status(500).entity(String.format(msg_dberror,msg)).type(mime_text).build();
			}

		}

		else {
			throw new WebApplicationException(Response.status(401).entity(
					String.format(msg_notanowner,id,id)).type(mime_text).build());
		}

	}


	/**
	 *  Delete a Pol resource internally
	 *    
	 *  @param id
	 */
	public Response deletePolInternal(String id) 
	throws WebApplicationException {

		OpenssoHelper opensso = new OpenssoHelper();
		String output = null;
		int status = 0;

		try {
			ErrorInfo ei = opensso.doLogin();
			String token=null;
			if (ei != null) {
				if (ei.status == 200) {
					token=ei.output.substring(ei.output.lastIndexOf("token.id=")+9);
					log(token);
					ErrorInfo c = opensso.deletePolicy(id, token);
					output = htmlToString(c.output);
					status = c.status;
				}
			}
			opensso.doLogout(token);
		} catch (IOException e1) {
			log(getClass().getName() + ": " + e1.getMessage());
			e1.printStackTrace();
		}

		String output_short = "";
		try {
			output_short = GetSsoOutput(output);
		} catch (IOException e) {
			throw new WebApplicationException(Response.status(500).entity(String.format(msg_ioexception, e.getMessage())).type(mime_text).build());
		}  
		if (output_short.length()==0) {
			throw new WebApplicationException(Response.status(400).entity(GetLastRow(output) + "\n\n").type(mime_text).build());            	
		}
		if (status == 200) return Response.ok(output_short).type(mime_text).build();
		else return Response.serverError().type(mime_text).build();
	}


	public String format(String xml) throws ParsingException, IOException {
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

	public final boolean ValidateIPAddress(String ipAddress) {
		String[] parts = ipAddress.split( "\\." );
		if ( parts.length != 3 ) return false;
		for ( String s : parts ) {
			try { 
				int i = Integer.parseInt( s );
				if ((i < 0) || (i>255)) return false;
			}
			catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	public final String GetSsoOutput (String str) throws IOException { 
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

	public String htmlToString (String html) {
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
	public final String GetLastRow (String str) { 
		str = str.trim();
		String[] fields = str.split("\n");
		return (fields[fields.length-1]);
	}

}




