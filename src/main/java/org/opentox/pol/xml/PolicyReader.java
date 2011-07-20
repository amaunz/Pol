package org.opentox.pol.xml;

import java.io.IOException;

import nu.xom.ParsingException;

import org.opentox.pol.ErrorInfo;
import org.opentox.pol.OpenssoHelper;
import org.opentox.pol.Rest;
import org.opentox.pol.RestException;
import org.opentox.pol.httpreturn.HttpReturn;
import org.opentox.pol.mysql.DbException;
import org.opentox.pol.mysql.MySQL;

public class PolicyReader extends PolicyHelper {

	
	public String getPolID(String subjectId, String id, String uri, String polnames) throws DbException, RestException {	
		//moved upfront - first check if the token is here, before db access
		if(subjectId == null) throw new RestException(400,msg_tokenmissing);

		log("\nS: Get pol ID\n");

		// If 'id' is *not* set, gather information about my URIs or a specific (but otherwise arbitrary) URI... 
		if (id == null) return getPol(subjectId, uri, polnames);


		// ... else deliver a specific one of my policies.
		MySQL s = new MySQL();	// database object to store rows of (policy name, user, resource).
		// Get user who created policy "id"
		String db_user = s.getDbUser(id);
		if (db_user == null)  //policy id not found, but better not to reveal this, just say not an owner
			throw new RestException(401,String.format(msg_notanowner,id,id));

		String token_user=null;		
		log("id: '" + id + "'\n");
		log("db user: '" + db_user + "'\n");

		// Resolve user from token
		Rest r = new Rest(); 
		HttpReturn ret = r.DoIdCall(subjectId);
		token_user = ret.data;
		if (token_user == null) throw new RestException(400,msg_token_expired);
		
		log("token user: '" + token_user + "'\n");

		String token=null;
		if (db_user != null && token_user != null && db_user.equals(token_user)) {

			OpenssoHelper opensso = new OpenssoHelper();
			String output = null;
			try {
				ErrorInfo ei = opensso.doLogin();
				if (ei != null) {
					if (ei.status == 200) {
						token=ei.output.substring(ei.output.lastIndexOf("token.id=")+9);
						log(token);
						ErrorInfo c = opensso.listPolicy(id, token);
						output = htmlToString(c.output);
					}
				}
			} catch (IOException e) {
				throw new RestException(502,e.getMessage());
			} finally {
				try {opensso.doLogout(token);} catch (Exception x) {}	
			}

			try {
				output = output.substring(output.indexOf("<Policies>"),output.length()); // incorporated XML formatting
			} catch (StringIndexOutOfBoundsException e) {
				throw new RestException(500,String.format(msg_xmlserializationfailed,e.getMessage(),output));
			}

			log("\n");

			try {
				output = format(output);
			} catch (ParsingException e) {
				throw new RestException(500,String.format(msg_xmlserializationfailed,e.getMessage(),output));
			} catch (IOException e) {
				throw new RestException(500,String.format(msg_xmlserializationfailed,e.getMessage(),output));
			}
			//System.out.println(output);	    	
			log("E: Get pol ID");
			return output;
		}
		else throw new RestException(401,String.format(msg_notanowner,id,id));

	}
}
