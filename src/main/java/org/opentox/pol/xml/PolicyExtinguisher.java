package org.opentox.pol.xml;

import java.util.List;

import org.opentox.pol.Rest;
import org.opentox.pol.RestException;
import org.opentox.pol.httpreturn.HttpReturn;
import org.opentox.pol.mysql.DbException;
import org.opentox.pol.mysql.MySQL;

public class PolicyExtinguisher extends PolicyHelper {


	public String deletePol(String subjectId, String id) throws RestException, DbException { 
		/**
		 * Do nothing if no subjectid
		 */
		if(subjectId == null) throw new RestException(400,msg_tokenmissing);

		MySQL s = new MySQL();	

		try {
			/**
			 * Retrieve the username of the policy owner
			 */
			String db_user = s.getDbUser(id);
			if	(db_user == null) throw new RestException(401,msg_notanowner);
			/**
			 * Resolve user from token
			 * Match token and db user
			 */
			String token_user = null;
			Rest r = new Rest(); 
			HttpReturn ret = r.DoIdCall(subjectId);
			token_user = ret.data;
			if (token_user == null) 
				throw new RestException(400,msg_token_expired);
			
			/**
			 * Now delete entries 
			 */
			if (db_user != null && token_user != null && db_user.equals(token_user)) 
				return deletePolicy(s,id);
			else 
				throw new RestException(401,String.format(msg_notanowner,id,id));
		} catch (RestException x) {
			throw x;
		} catch (DbException x) {
			throw x;
		} catch (Exception x) {
			throw new RestException(500,x.getMessage());
			
		}
	}
	
	

    //	move to MySQL, make use of real roll back 
	protected String deletePolicy(MySQL s, String id) throws RestException, DbException {
		String msg = "";
		// delete db entry
		boolean db_entry_deleted = true;
		List<String> res = null;
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

		String dr = null;
		if (db_entry_deleted) 
		try {  	
			dr = deletePolInternal(id);
			return dr;
		} catch (Exception x) {
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
			
			log("E: Del pol");
			if (x instanceof RestException) throw (RestException) x;
			else if (x instanceof DbException) throw (DbException) x;
			else throw new RestException(500,x);
		}
		else throw new DbException(String.format(msg_dberror,msg));
		
	}
	

}
