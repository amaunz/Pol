/* DatasourceFactory.java
 * Author: Nina Jeliazkova
 * Date: Apr 13, 2008 
 * Revision: 0.1 
 * 
 * Copyright (C) 2005-2008  Nina Jeliazkova
 * 
 * Contact: nina@acad.bg
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package org.opentox.pol.mysql.pool;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public class DatasourceFactory {
    protected static final String slash="/";
    protected static final String qmark="?";
    protected static final String colon=":";
    protected static final String eqmark="=";
    protected static final String amark="&";
    protected DataSourceC3P0 datasource;
    
    private DatasourceFactory() {
        super();
    }
    private static class DatasourceFactoryHolder { 
        private final static DatasourceFactory instance = new DatasourceFactory();
      }
    
    public static synchronized DatasourceFactory getInstance() {
        return DatasourceFactoryHolder.instance;
    }
    
    public static synchronized DataSource getDataSource(String connectURI) throws Exception {
        if (connectURI == null) throw new Exception("Connection URI not specified!");
        
        DataSourceC3P0 ds = getInstance().datasource;
        if (ds == null) {
        	ds = setupDataSource(connectURI);
        	getInstance().datasource = ds;
        }
        if (ds!= null) return ds.getDatasource();
        else return null;

    }

    
    public static Connection getConnection(String connectURI) throws SQLException, Exception {
            Connection connection = getDataSource(connectURI).getConnection();
            if (connection.isClosed()) 
            	return getDataSource(connectURI).getConnection();
            else
            	return connection;

    }    
    public static synchronized DataSourceC3P0 setupDataSource(String connectURI) throws Exception {
        	return new DataSourceC3P0(connectURI);

    }
    /**
     * Assembles connection URI
     * @param scheme
     * @param hostname
     * @param port
     * @param database
     * @param user
     * @param password
     * @return    scheme://{Hostname}:{Port}/{Database}?user={user}&password={password}
     */
    public static String getConnectionURI(String scheme,String hostname,String port,
                String database,String user,String password) {
        
        StringBuilder b = new StringBuilder();
        b.append(scheme).append(colon).
        append(slash).append(slash);
        
        if (hostname==null) b.append("localhost");
        else b.append(hostname);
        if (port != null) b.append(colon).append(port);
        b.append(slash).
        append(database);
        String q = qmark;
        if (user != null) {
            b.append(q); q = amark;
            b.append("user").append(eqmark).append(user);
        }
        if (password != null) {
            b.append(q); q = amark;            
            b.append("password").append(eqmark).append(password);
        }
        b.append(amark);
        b.append("&useUnicode=true&characterEncoding=UTF8&characterSetResults=UTF-8");
       // b.append("[validationQuery]=[SELECT 1]");
        return b.toString();
    }
    /**
     * 
     * @param scheme
     * @param hostname
     * @param port
     * @param database
     * @return    scheme://{Hostname}:{Port}/{Database}
     */
    public static String getConnectionURI(String scheme,String hostname,String port,
            String database) {
        return getConnectionURI(scheme, hostname, port, database,null,null);
    }    
    public static String getConnectionURI(String hostname,String port,
            String database) {
        return getConnectionURI("jdbc:mysql", hostname, port, database,null,null);
    }

}




