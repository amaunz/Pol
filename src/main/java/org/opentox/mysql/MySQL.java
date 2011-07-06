package org.opentox.mysql;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.opentox.pol.OpenssoHelper;

public class MySQL {
	final static String res_field = "res";
	final static String user_field = "user";
	final static String pol_field = "pol";
	
	private Connection conn;

	public void open() {
		try {
			InputStream fis = null;
			String pw = "";
			String propfile = "org/opentox/pol/admin.properties";
			fis = OpenssoHelper.class.getClassLoader().getResourceAsStream(propfile);
			Properties config = new Properties();
			try {
				config.load(fis);
				pw = config.getProperty("pw");
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
			conn = DriverManager.getConnection("jdbc:mysql://localhost/Pol?" +
					"user=root&password=" + pw);		   
		} catch (SQLException e) {
			// handle any errors
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
		} 
	}

	public void close() {
		try {
			conn.close();
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
		}
	}

	public void add(String pol, String user, String res) {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(String.format("INSERT INTO pol VALUES ('%s','%s','%s');",pol,user,res));
		}
		catch (SQLException ex){
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
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


	public void delete_pol(String pol) {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			String query = String.format("DELETE FROM pol WHERE pol='%s'",pol);
			stmt.executeUpdate(query);
		}
		catch (SQLException ex){
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
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


	public String search_user_by_pol(String pol) {
		String resres = null;
		ResultSet rs = null;
		Statement stat = null;
		try {
			stat = conn.createStatement();
			rs = stat.executeQuery(String.format("SELECT user FROM pol WHERE pol='%s'",pol));
			while (rs.next()) {
				//System.out.println("search_user_by_pol: pol = " + rs.getString("pol"y));
				//System.out.println("search_user_by_pol: user = " + rs.getString("user"));
				//System.out.println("search_user_by_pol: res = " + rs.getString("res"));
				resres = rs.getString(user_field);
				break;
			}
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
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

	// returns true if policy name exists in DB
	public boolean search_pol_name(String pol)  {
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
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
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
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
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
	public String search_users_pols(String user) {
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
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
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
	public String[] search_res(String res) {

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
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
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
	public String[] search_res_pol(String res) {
		String[] resres = new String[3];
		Statement stat = null;
		ResultSet rs = null;

		try {
			stat = conn.createStatement();
			String query = String.format("SELECT res,user,pol FROM pol WHERE res='%s'",res);
			//System.out.println(query);
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
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
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

}
