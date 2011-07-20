package org.opentox.pol.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import javax.ws.rs.core.UriInfo;

import org.opentox.pol.ErrorInfo;
import org.opentox.pol.OpenssoHelper;
import org.opentox.pol.Rest;
import org.opentox.pol.RestException;
import org.opentox.pol.httpreturn.HttpReturn;
import org.opentox.pol.mysql.DbException;
import org.opentox.pol.mysql.MySQL;

public class PolicyCreator extends PolicyHelper {

	public String createPolicy(String subjectId,  UriInfo uriInfo, InputStream is) throws RestException, DbException {
		

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
		RestException exception = null;
		try {
			/**
			 * No token, do nothing
			 */
			if(subjectId == null) throw new RestException(400,msg_tokenmissing);
			/**
			 * Find token user
			 */
			Rest r = new Rest(); 
			HttpReturn ret = r.DoIdCall(subjectId);
			token_user = ret.data;
			if (token_user == null) throw new RestException(400,msg_token_expired);
			
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

				}
				finally {
					p2.close();
					out2.close();
					reader.close();
					iss.close();
					is.close();
				}
			} 
			else 
				exception=new RestException(400,msg_missingxml);
			
		} catch (RestException x) {
			exception=x;
		} catch(UnsupportedEncodingException e) {
			exception=new RestException(400,msg_unsupportedencoding);
		} catch(IOException e) {
			exception=  new RestException(500,msg_ioexception);
		}

		//hm, not so elegant
		if (exception!=null) {
			temp2.delete();
			log(exception.getMessage());
			throw exception;
		} else {
			
			pp = new ParsePolicy();
			ArrayList<Policy> polres = null;
			try {
				polres = pp.runParser(temp2.getAbsolutePath());
			} catch (Exception e) {
				e.printStackTrace();
				log(msg_malformedXML);
				throw new RestException(400,msg_malformedXML);
			} finally {
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
 
			if (!resources_untouched) throw new RestException(400,String.format(msg_policyalreadyregistered,db_resource, db_user));
			
			if (!policy_is_new) throw new RestException(400,String.format(msg_existingpolicy,polName));
			
			if (!nr_resources_below_max) throw new RestException(400,String.format(msg_maxresources,polName ,max_nr_resources_per_pol));

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

						throw new RestException(400,String.format(msg_invaliduri,resName));
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
						if (!ok) 
							throw new RestException(400,String.format(msg_invalidwildcard,resName));
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
				throw new RestException(500,String.format(msg_ioexception, e.getMessage()));
			}            
			if (output_short.length()==0 || status != 200) 
				throw new RestException(400,GetLastRow(output) + "\n\n");            	

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
					throw new RestException(500,String.format(msg_dberror,e.getMessage()));
				}
				finally {
					s.close();
				}
			}
			log("E: Create pol\n");
			return output_short;			
		}
	}

}
