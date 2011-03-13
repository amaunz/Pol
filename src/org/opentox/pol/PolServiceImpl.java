package org.opentox.pol;

/**
 * @author Gabriel Mateescu gabriel@vt.edu
 */


//
// Annotations 
//

// Designate this class as a JAX-RS resource/service
import javax.ws.rs.HeaderParam;
import nu.xom.*;

import java.net.*;
import java.io.*;

import javax.ws.rs.Path;
//import javax.ws.rs.core.Response.Status;

import java.util.*;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.net.URLEncoder;


import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

// Exceptions
import javax.ws.rs.WebApplicationException;

// Utilities
import org.opentox.pol.xml.*;
import org.opentox.pol.httpreturn.*;
//import org.opentox.pol.sqlitedb.*;
import org.opentox.mysql.*;
import org.opentox.pol.xml.ParsePolicy;

//import java.sql.*;




/**
 * A Singleton class that uses an in-memory map to keep 
 * the pol objects. This way, we maintain the state of the 
 * resources in memory. 
 * 
 */
@Path("/opensso-pol")
public class PolServiceImpl implements PolService {

	//@Context private javax.servlet.http.HttpServletRequest hsr;


	private static PolServiceImpl instance = null;

	private PolServiceImpl() {
		// Prevent instantiation by clients
	}



	public synchronized static PolServiceImpl getInstance() {
		if(instance == null) {
			instance = new PolServiceImpl();
		}
		return instance;
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
	public Response createPol(@HeaderParam("subjectid") String subjectId, @Context UriInfo uriInfo, InputStream is) 
	throws WebApplicationException {

		//		Status status = null;			// ssoadm return code 
		WebApplicationException exception=null;		// general purpose exception (thrown when status != null)

		File temp2 = null;				// Temp file without XML header 
		String token_user = null;				// user as encoded by subjectid (=token)

		MySQL s = new MySQL();	// database object to store rows of (policy name, user, resource).
		Iterator<Policy> it;			// runs policies found in the XML

		BufferedReader reader = null;	//	
		FileOutputStream out2 = null;	//

		boolean resources_untouched = true;			// Control conditions on uploaded policy
		boolean IP_check_ok = true;					//
		boolean policy_is_new = true;				//
		boolean nr_resources_below_max = true;		//
		final int max_nr_resources_per_pol = 2000;	//

		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();

		System.out.println("\nS: Create pol");

		try {

			// Find token user
			if(subjectId == null) {
				System.out.println("Missing token.");
				throw new WebApplicationException(Response.status(400).entity("Missing token.\n\n").type("text/plain").build());
			}
			Rest r = new Rest(); 
			HttpReturn ret = r.DoIdCall(subjectId);
			token_user = ret.data;
			if (token_user == null) {
				System.out.println("Token could not be resolved to a user id. Token expired?");
				throw new WebApplicationException(Response.status(400).entity("Token could not be resolved to a user id. Token expired?.\n\n").type("text/plain").build());
			}
			System.out.println("Resolved to user: '" + token_user + "'");


			// Read XML
			if (is != null) {
				String line;
				try {
					reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
					boolean body = false;
					while ((line = reader.readLine()) != null) {
						sb.append(line).append("\n");
						if (line.contains("<Policies>")) body = true;
						if (body) sb2.append(line).append("\n");
					}
					temp2 = File.createTempFile("opensso-policy2-",".xml");	out2 = new FileOutputStream(temp2);	PrintStream p2 = new PrintStream (out2); p2.println(sb2.toString());
					//temp2.deleteOnExit();
				}
				finally {
					reader.close();
					is.close();
					out2.close();
				}
			} 
			else {       
				exception=new WebApplicationException(Response.status(400).entity("Missing XML file.\n\n").type("text/plain").build());
			}
		} catch(UnsupportedEncodingException e) {
			exception=new WebApplicationException(Response.status(400).entity("Unsupport Coding. Please use UTF-8 encoded XML files.\n\n").type("text/plain").build());
		} catch(IOException e) {
			exception=new WebApplicationException(Response.status(500).entity("IOException. Please contact administrator.\n\n").type("text/plain").build());
		}

		
		if (exception!=null) {
			temp2.delete();
			System.out.println(exception.getMessage());
			throw exception;
		}

		else {
			// Parse XML
			ParsePolicy pp = new ParsePolicy();
			ArrayList<Policy> polres = null;
			try {
				polres = pp.runParser(temp2.getAbsolutePath());
			}
			catch (Exception e) {
				e.printStackTrace();
				System.out.println("Malformed XML.");
				throw new WebApplicationException(Response.status(400).entity("Malformed XML.\n\n").type("text/plain").build());
			}
			finally {
				temp2.delete();
			}
			String polName = null;
			ArrayList<String> resNames = null;
			String[] resource_registered_name = new String[2];
			String db_user = null;
			String db_resource = null;

			it = polres.iterator();
			try {

				while(it.hasNext()) {
					Policy p = it.next();
					polName=p.getName();
					System.out.println("Create Policy '" + polName + "'");
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


			} finally {

			}

			if (!resources_untouched) {
				System.out.println("Resource '" +  db_resource + "' already registered by user '" + db_user + "'.");		
				throw new WebApplicationException(Response.status(400).entity("Resource '" +  db_resource + "' already registered by user '" + db_user + "'.\n\n").type("text/plain").build());
			}
			if (!policy_is_new) {
				System.out.println("Policy '" +  polName + "' already exists.");		
				throw new WebApplicationException(Response.status(400).entity("Policy '" +  polName + "' already exists.\n\n").type("text/plain").build());
			}
			if (!nr_resources_below_max) {
				System.out.println("Policy '" +  polName + "' contains more than the current maximum of " + max_nr_resources_per_pol + " resources. Please consider splitting your policy.");		
				throw new WebApplicationException(Response.status(400).entity("Policy '" +  polName + "' contains more than the current maximum of " + max_nr_resources_per_pol + " resources. Please consider splitting your policy.\n\n").type("text/plain").build());
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
						throw new WebApplicationException(Response.status(400).entity("Resource '" + resName + "' is not a valid URI.\n\n").type("text/plain").build());
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
							throw new WebApplicationException(Response.status(400).entity("Resource '" + resName + "' has illegal wildcards.\n\n").type("text/plain").build());
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
						System.out.println(token);
						ErrorInfo c = opensso.createPolicy(sb.toString(), token);
						output = htmlToString(c.output);
						status=c.status;
						output_short = GetSsoOutput(output);
					}
				}
				opensso.doLogout(token);
			} catch (IOException e) {
				throw new WebApplicationException(Response.status(500).entity("IOException. Please contact administrator.\n\n").type("text/plain").build());
			}            
			if (output_short.length()==0 || status != 200) {
				throw new WebApplicationException(Response.status(400).entity(GetLastRow(output) + "\n\n").type("text/plain").build());            	
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
					throw new WebApplicationException(Response.status(500).entity("Exception in mysql backend.\n\n").type("text/plain").build());
				}
			}
			System.out.println("E: Create pol");
			return Response.ok(output_short).type("text/plain").build();			
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

		System.out.println("\nS: Get pol ID");

		// If 'id' is *not* set, gather information about my URIs or a specific (but otherwise arbitrary) URI... 
		if (id == null) {
			Response r = getPol(subjectId, uri, polnames);
			System.out.println("E: Get pol ID");
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
			s.close();
		}
		catch (Exception e) {
			throw new WebApplicationException(Response.status(500).entity("Exception in mysql backend.\n\n").type("text/plain").build());
		}
		System.out.println("id: '" + id + "'");
		System.out.println("db user: '" + db_user + "'");

		// Resolve user from token
		if(subjectId == null) {
			throw new WebApplicationException(Response.status(400).entity("Missing token.\n\n").type("text/plain").build());
		}
		Rest r = new Rest(); 
		HttpReturn ret = r.DoIdCall(subjectId);
		token_user = ret.data;
		if (token_user == null) {
			throw new WebApplicationException(Response.status(400).entity("Token could not be resolved to a user id. Token expired?.\n\n").type("text/plain").build());
		}
		System.out.println("token user: '" + token_user + "'");

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
						System.out.println(token);
						ErrorInfo c = opensso.listPolicy(id, token);
						output = htmlToString(c.output);
					}
				}
				opensso.doLogout(token);
				//output = (opensso.listPolicy("/", id)).toString();
			}
			catch (IOException e) {
				System.out.println("PolServiceImpl: " + e.getMessage());
				e.printStackTrace();
			}

			try {
				output = output.substring(output.indexOf("<Policies>"),output.length()); // incorporated XML formatting
			} catch (StringIndexOutOfBoundsException e) {
				throw new WebApplicationException(Response.status(500).entity("XML serialization failed.\n\n" + output + "\n\n").type("text/plain").build());
			}

			System.out.println();

			try {
				output = format(output);
			} catch (ParsingException e) {
				throw new WebApplicationException(Response.status(500).entity("XML serialization failed.\n\n").type("text/plain").build());
			} catch (IOException e) {
				throw new WebApplicationException(Response.status(500).entity("XML serialization failed.\n\n").type("text/plain").build());
			}
			//System.out.println(output);	    	
			System.out.println("E: Get pol ID");
			return Response.ok(output).type("text/xml").build();

		}
		else {
			throw new WebApplicationException(Response.status(401).entity("Not the owner of policy '"+id+"' or policy '"+id+"' does not exist.\n\n").type("text/plain").build());
		}

	}

	// Get
	public Response getPol(String subjectId, String uri, String polnames) {

		String res = null;
		String token_user = null;

		System.out.println("   S: Get pol (i)");
		// Resolve user from token
		if(subjectId == null) {
			throw new WebApplicationException(Response.status(400).entity("Missing token.\n\n").type("text/plain").build());
		}
		Rest r = new Rest(); 
		HttpReturn ret = r.DoIdCall(subjectId);
		token_user = ret.data;
		if (token_user == null) {
			throw new WebApplicationException(Response.status(400).entity("Token could not be resolved to a user id. Token expired?.\n\n").type("text/plain").build());
		}
		System.out.println("   Token user: '" + token_user + "'");

		// URI is given: get owner of URI...
		if (uri != null) {
			System.out.println("   Searching owner of uri '" + uri + "'.");
			// get owner of URI
			try {
				MySQL s = new MySQL();
				s.open();
				String[] res_arr;
				if (polnames == null) {
					res_arr = s.search_res(uri);
					res = res_arr[1];
				}
				else {
					System.out.println("   => with pol names.");
					res_arr = s.search_res_pol(uri);
					res = res_arr[1];
					res += res_arr[2];
				}
				s.close();
			}
			catch (Exception e) {
				throw new WebApplicationException(Response.status(500).entity("Exception in mysql backend.\n\n").type("text/plain").build());
			}
		}

		// ... or URI is *not* given: get all my policies. 
		else {


			System.out.println("   Searching policies of token user '" + token_user + "'.");			
			// get all policies owned by token user
			try {
				MySQL s = new MySQL();
				s.open();
				res = s.search_users_pols(token_user);
				s.close();
			}
			catch (Exception e) {
				throw new WebApplicationException(Response.status(500).entity("Exception in mysql backend.\n\n").type("text/plain").build());
			}
		}

		System.out.println("   E: Get pol (i)");
		return Response.ok(res + "\n").type("text/plain").build();

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
			s.close();
		}
		catch (Exception e) {
			throw new WebApplicationException(Response.status(500).entity("Exception in mysql backend.\n\n").type("text/plain").build());
		}
		System.out.println("db user: '" + db_user + "'");


		// Resolve user from token
		if(subjectId == null) {
			throw new WebApplicationException(Response.status(400).entity("Missing token.\n\n").type("text/plain").build());
		}
		Rest r = new Rest(); 
		HttpReturn ret = r.DoIdCall(subjectId);
		token_user = ret.data;
		if (token_user == null) {
			throw new WebApplicationException(Response.status(400).entity("Token could not be resolved to a user id. Token expired?.\n\n").type("text/plain").build());
		}
		System.out.println("token user: '" + token_user + "'");


		// delete entries
		if (db_user != null && token_user != null && db_user.equals(token_user)) {

			// delete db entry
			boolean db_entry_deleted = true;
			try {
				s.open();
				res = s.search_pol(id);
				s.delete_pol(id);
				s.close();
			}
			catch (Exception e) {
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
				System.out.println("E: Del pol");
				return dr;
			}
			else {
				return Response.status(500).entity("Exception in mysql backend.\n\n").type("text/plain").build();
			}

		}

		else {
			throw new WebApplicationException(Response.status(401).entity("Not the owner of policy '"+id+"' or policy '"+id+"' does not exist.\n\n").type("text/plain").build());
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
					System.out.println(token);
					ErrorInfo c = opensso.deletePolicy(id, token);
					output = htmlToString(c.output);
					status = c.status;
				}
			}
			opensso.doLogout(token);
		} catch (IOException e1) {
			System.out.println("PolServiceImpl: " + e1.getMessage());
			e1.printStackTrace();
		}

		String output_short = "";
		try {
			output_short = GetSsoOutput(output);
		} catch (IOException e) {
			throw new WebApplicationException(Response.status(500).entity("IOException. Please contact administrator.\n\n").type("text/plain").build());
		}  
		if (output_short.length()==0) {
			throw new WebApplicationException(Response.status(400).entity(GetLastRow(output) + "\n\n").type("text/plain").build());            	
		}
		if (status == 200) return Response.ok(output_short).type("text/plain").build();
		else return Response.serverError().type("text/plain").build();
	}


	public String format(String xml) throws ParsingException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Serializer serializer = new Serializer(out);
		serializer.setIndent(4);  // or whatever you like
		serializer.write(new Builder().build(xml, ""));
		return out.toString("UTF-8");
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
		BufferedReader reader = new BufferedReader(new StringReader(str));
		boolean found=false;
		while ((str2 = reader.readLine()) != null) {
			if (found) return str2 + "\n\n";
			if (str2.contains("OpenSSOBack to main page")) {
				found = true;
			}
		}
		return ("");
	}

	public String htmlToString (String html) {
		StringReader in = new StringReader(html);
		Html2Text parser = new Html2Text();
		try {
			parser.parse(in);
		} catch (IOException e) {
			System.out.println("OpenssoHelper: " + e.getMessage());
			e.printStackTrace();
		}
		in.close();
		return (parser.getText());
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




