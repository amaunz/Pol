package org.opentox.pol.sqlitedb;
import java.sql.*;
import java.util.*;

/**
 * Implements a table in a sqlite DB
 * table has  cols for policy name (pol), user name (user), and resource name aka URI (res).
 * 
 * @author am
 *
 */

public class SQLiteDB {
	
	private Connection conn;
	
  public void open() throws SQLException {
    try { Class.forName("org.sqlite.JDBC"); }
    catch (Exception e) {}
    conn = DriverManager.getConnection("jdbc:sqlite:pol.db");
    Statement stat = conn.createStatement();
    stat.executeUpdate("create table if not exists pol (pol, user, res);");
  }

  public void close() throws SQLException {
	    conn.close();
  }
  
  /**
   * Add a new entry.
   * 
   * @param pol
   * @param user
   * @param res
   * @throws Exception
   */
  public void add(String pol, String user, String res) throws Exception {
    PreparedStatement prep = conn.prepareStatement("INSERT INTO POL VALUES (?, ?, ?);");
    prep.setString(1, pol);
    prep.setString(2, user);
    prep.setString(3, res);
    prep.addBatch();
    conn.setAutoCommit(false);
    prep.executeBatch();
    conn.setAutoCommit(true);
  }

  public String search_user_by_pol(String pol) throws Exception {
	  	String resres = null;
	    Statement stat = conn.createStatement();
	    String query = "SELECT * FROM pol WHERE pol='"+pol+"';";
	    //System.out.println(query);
	    ResultSet rs = stat.executeQuery(query);
	    while (rs.next()) {
	    	//System.out.println("search_user_by_pol: pol = " + rs.getString("pol"));
	    	//System.out.println("search_user_by_pol: user = " + rs.getString("user"));
	    	//System.out.println("search_user_by_pol: res = " + rs.getString("res"));
			resres = rs.getString("user");
			break;
		}
	    return resres;
  }
  
  // returns true if policy name exists in DB
  public boolean search_pol_name(String pol) throws SQLException {
	boolean res = false;
    Statement stat = conn.createStatement();
    String query = "SELECT * FROM pol WHERE pol='"+pol+"';";
    //System.out.println(query);
    ResultSet rs = stat.executeQuery(query);   
    while (rs.next()) {
    	res = true;
    	break;
	}
    return(res);
  }
  
  public List<String> search_pol(String pol) throws Exception {
	Statement stat = null;
	ResultSet rs = null;
	List<String> res = new ArrayList<String>();
	
	stat = conn.createStatement();
	rs = stat.executeQuery("SELECT * FROM pol WHERE pol='"+pol+"'");
	while (rs.next()) {
		res.add(rs.getString("pol") + ":::" + rs.getString("user") + ":::" + rs.getString("res"));
	}
	
	return res;
	
  }
  
  
  public String search_users_pols(String user) throws Exception {
	String res = "";
	Iterator<String> it = null;
	
    Statement stat = conn.createStatement();
    String query = "SELECT * FROM pol WHERE user='"+user+"';";
    //System.out.println(query);
   	ResultSet rs = stat.executeQuery(query);
    HashSet<String> hs = new HashSet<String>();
	while (rs.next()) {
		String s = rs.getString("pol");
		if (s!=null) hs.add(s);
	}
	it = hs.iterator();
	while (it.hasNext()) {
		String s = it.next();
		if (s != null) res += s + "\n";	
	}
	return res;
  }
  
  // get user who created res
  public String[] search_res(String res) throws SQLException {  
	String[] resres = new String[3];
    Statement stat = conn.createStatement();
    String query = "SELECT * FROM pol WHERE res='"+res+"';";
   	ResultSet rs = stat.executeQuery(query);
   	resres[2]="";
    while (rs.next()) {
    	resres[0] = rs.getString("res");
		resres[1] = rs.getString("user");
		break; // Assumes the same user for a resource name in all records
	}
    return resres;
  }

  // get user who created res and all policy names associated with it
  public String[] search_res_pol(String res) throws SQLException {  
		String[] resres = new String[3];
	    Statement stat = conn.createStatement();
	    String query = "SELECT * FROM pol WHERE res='"+res+"';";
	    //System.out.println(query);
	   	ResultSet rs = stat.executeQuery(query);
	   	int i=0;
	   	resres[2]="";
	    while (rs.next()) {
			if (i==0) { 
				resres[0] = rs.getString("res");
				resres[1] = rs.getString("user");
			}
			resres[2] += "\n" + rs.getString("pol");
			i++;
		}
	    return resres;
	  }
  
//  public ResultSet search() throws Exception {
//    Statement stat = conn.createStatement();
//    String query = "SELECT * FROM pol;";
//    //System.out.println(query);
//    return stat.executeQuery(query);
//  }

  public void delete_pol(String pol) throws Exception {
    Statement stat = conn.createStatement();
    String query = "DELETE FROM pol WHERE pol='"+pol+"';";
    //System.out.println(query);
    stat.executeUpdate(query);
  }

}