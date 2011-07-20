package org.opentox.pol.mysql;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.idea.modbcum.c.DatasourceFactory;

import org.opentox.pol.OpenssoHelper;
import org.opentox.pol.RestException;

public class MySQL {
	final static String msg_dberror = "[Policy service] Exception in mysql backend. %s\n\n";
	final static String res_field = "res";
	final static String user_field = "user";
	final static String pol_field = "pol";
	
	private Connection conn;
	
	

	public void open() throws DbException , RestException {
		try {
			InputStream fis = null;
			String pw = "";
			String propfile = "org/opentox/pol/admin.properties";
			fis = OpenssoHelper.class.getClassLoader().getResourceAsStream(propfile);
			if (fis==null) throw new RestException(500,"Cant't load "+propfile);
			Properties config = new Properties();
			try {
				config.load(fis);
				pw = config.getProperty("pw");
			} catch (IOException e) {
				throw new RestException(500,e);
			}
			finally {
				try {
					fis.close();
				} catch (IOException e) {
					// ignore
				}
			}
			conn = getConnection("root", pw);		   
			
		} catch (SQLException e) {
			throw new DbException(String.format("SQLException: %s\nSQLState: %s\nVendorError: %d", e.getMessage(),e.getSQLState(),e.getErrorCode()));
		} catch (Exception x) {
			throw new DbException(x.getMessage(),x);
		}

	}
	
	protected Connection getConnection(String user,String pw) throws Exception  {
		String uri = String.format("jdbc:mysql://localhost/Pol?user=%s&password=%s",user,pw);
		return DatasourceFactory.getConnection(uri);
		//return DriverManager.getConnection(String.format("jdbc:mysql://localhost/Pol?user=%s&password=%s",user,pw));
	}

	public void close() {
		try {
			if (conn != null) conn.close();
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
		}
	}

	public void add(String pol, String user, String res) throws DbException  {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(String.format("INSERT INTO pol VALUES ('%s','%s','%s',null,null);",pol,user,res));
		}
		catch (SQLException e){
			throw new DbException(String.format("SQLException: %s\nSQLState: %s\nVendorError: %d", e.getMessage(),e.getSQLState(),e.getErrorCode()));
		}
		finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) { } // ignore
				stmt = null;
			}
		}

	}


	public void delete_pol(String pol) throws DbException {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			String query = String.format("DELETE FROM pol WHERE pol='%s'",pol);
			stmt.executeUpdate(query);
		}
		catch (SQLException e){
			throw new DbException(String.format("SQLException: %s\nSQLState: %s\nVendorError: %d", e.getMessage(),e.getSQLState(),e.getErrorCode()));
		}
		finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) { } // ignore
				stmt = null;
			}
		}

	}


	public String search_user_by_pol(String pol) throws DbException {
		String resres = null;
		ResultSet rs = null;
		Statement stat = null;
		try {
			stat = conn.createStatement();
			rs = stat.executeQuery(String.format("SELECT user FROM pol WHERE pol='%s'",pol));
			while (rs.next()) {
				resres = rs.getString(user_field);
				break;
			}
		} catch (SQLException e) {
			throw new DbException(String.format("SQLException: %s\nSQLState: %s\nVendorError: %d", e.getMessage(),e.getSQLState(),e.getErrorCode()));
		}
		finally {
			try {
				if (rs != null) rs.close();
				if (stat !=null) stat.close();
			} catch (SQLException sqlEx) { } // ignore
			finally {
				rs = null;
				stat = null;
			}
		}
		return resres;
	}

	// returns true if policy name exists in DB
	public boolean search_pol_name(String pol) throws DbException {
		boolean res = false;
		ResultSet rs = null;
		Statement stat = null;
		try {
			stat = conn.createStatement();
			rs = stat.executeQuery(String.format("SELECT pol FROM pol WHERE pol='%s' LIMIT 1",pol));   
			while (rs.next()) {
				res = true;
				break;
			}
		} catch (SQLException e) {
			throw new DbException(String.format("SQLException: %s\nSQLState: %s\nVendorError: %d", e.getMessage(),e.getSQLState(),e.getErrorCode()));
		}
		finally {
			try {
				rs.close();
				stat.close();
			} catch (SQLException sqlEx) { } // ignore
			finally {
				rs = null;
				stat = null;
			}
		}
		return(res);
	}

	/**
	 * Retrieve policyid, user, and resource , given a policy id
	 * @param pol
	 * @return
	 * @throws Exception
	 */
	public List<String> search_pol(String pol) throws Exception {
		Statement stat = null;
		ResultSet rs = null;
		List<String> res = new ArrayList<String>();

		try {
			stat = conn.createStatement();
			rs = stat.executeQuery(String.format("SELECT pol,user,res FROM pol WHERE pol='%s'",pol));
			while (rs.next()) {
				res.add(String.format("%s;%s;%s",
						rs.getString(pol_field),rs.getString(user_field),rs.getString(res_field)
						));
			}
		} catch (SQLException e) {
			throw new DbException(String.format("SQLException: %s\nSQLState: %s\nVendorError: %d", e.getMessage(),e.getSQLState(),e.getErrorCode()));
		}
		finally {
			try {
				rs.close();
				stat.close();
			} catch (SQLException sqlEx) { } // ignore
			finally {
				rs = null;
				stat = null;
			}
		}

		return res;

	}

	/**
	 * Retrieves policy ids, given user name. 
	 * Strings delimited by \n
	 * @param user
	 * @return
	 */
	public String search_users_pols(String user) throws DbException {
		final String polfield = "pol";
		final String newline = "\n";

		Statement stat = null;
		ResultSet rs = null;
		StringBuilder b = new StringBuilder();
		
		try {
			stat = conn.createStatement();
			rs = stat.executeQuery(String.format("SELECT pol FROM pol WHERE user='%s'",user));
			while (rs.next()) {
				String s = rs.getString(polfield);
				if (s!=null) {
					b.append(s);
					b.append(newline);
				}
			}

		} catch (SQLException e) {
			throw new DbException(String.format("SQLException: %s\nSQLState: %s\nVendorError: %d", e.getMessage(),e.getSQLState(),e.getErrorCode()));
		}
		finally {
			try {
				rs.close();
				stat.close();
			} catch (SQLException sqlEx) { } // ignore
			finally {
				rs = null;
				stat = null;
			}
		}
		return b.toString();
	}

	/**
	 * Retrieves resource owner (user name), given by resource URI
	 * @param res
	 * @return
	 */
	public String[] search_res(String res) throws DbException {

		String[] resres = new String[3];
		Statement stat = null;
		ResultSet rs = null;

		try {
			stat = conn.createStatement();
			String query = String.format("SELECT res,user FROM pol WHERE res='%s' LIMIT 1",res);
			rs = stat.executeQuery(query);
			resres[2]="";
			while (rs.next()) {
				resres[0] = rs.getString(res_field);
				resres[1] = rs.getString(user_field);
				break; // Assumes the same user for a resource name in all records
			}

		} catch (SQLException e) {
			throw new DbException(String.format("SQLException: %s\nSQLState: %s\nVendorError: %d", e.getMessage(),e.getSQLState(),e.getErrorCode()));
		}
		finally {
			try {
				rs.close();
				stat.close();
			} catch (SQLException sqlEx) { } // ignore
			finally {
				rs = null;
				stat = null;
			}
		}
		return resres;
	}


	/**
	 * get user who created res and all policy names associated with it
	 */
	public String[] search_res_pol(String res) throws DbException {
		String[] resres = new String[3];
		Statement stat = null;
		ResultSet rs = null;

		try {
			stat = conn.createStatement();
			String query = String.format("SELECT res,user,pol FROM pol WHERE res='%s'",res);
			rs = stat.executeQuery(query);
			int i=0;
			StringBuilder b = new StringBuilder();
			resres[2]="";
			while (rs.next()) {
				if (i==0) { 
					resres[0] = rs.getString(res_field);
					resres[1] = rs.getString(user_field);
				}
				b.append("\n");
				b.append(rs.getString(pol_field));
				i++;
			}
			resres[2] = b.toString();

		} catch (SQLException e) {
			throw new DbException(String.format("SQLException: %s\nSQLState: %s\nVendorError: %d", e.getMessage(),e.getSQLState(),e.getErrorCode()));
		}
		finally {
			try {
				rs.close();
				stat.close();
			} catch (SQLException sqlEx) { } // ignore
			finally {
				rs = null;
				stat = null;
			}
		}
		return resres;
	}
	
	//added from {@link PolServiceImpl}
	
	/**
	 * 
	 * @param id policy id
	 * @return username of the owner
	 * @throws Exception
	 */
	public String getDbUser(String id) throws DbException {
		
		String db_user = null;
		// Get user who created policy "id"
		try {
			open();
			db_user = search_user_by_pol(id);
		} catch (Exception e) {
			throw new DbException(String.format(msg_dberror,e.getMessage()),e);
		}
		finally {
			close();
		}
		return db_user;
	}
	
	

	public String getUriOwner(String uri,String polnames) throws DbException {
		StringBuilder res = new StringBuilder();
		try {
			open();
			String[] res_arr;
			if (polnames == null) {
				res_arr = search_res(uri);
				res.append(res_arr[1]);
			}
			else {
				//log("   => with pol names.");
				res_arr = search_res_pol(uri);
				res.append(res_arr[1]);
				res.append(res_arr[2]);
			}

		}
		catch (Exception e) {
			throw new DbException(String.format(msg_dberror,e.getMessage()),e);
		}
		finally {
			close();
		}
		return res.toString();
	}
	
	/**
	 * get all policies owned by token user
	 * @param token_user
	 * @return
	 * @throws DbException
	 */
	public String getPoliciesByUser(String token_user) throws DbException {
		
		try {
			open();
			return search_users_pols(token_user);
		}
		catch (Exception e) {
			throw new DbException(String.format(msg_dberror,e.getMessage()));
		}
		finally {
			try {close();} catch (Exception x) {}
		}
	}
	
}
