package org.opentox.pol.mysql.pool;

import javax.sql.DataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DataSourceC3P0 {
	protected volatile ComboPooledDataSource datasource;

	public DataSourceC3P0(String connectURI)  throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        datasource = new ComboPooledDataSource(); 
     	datasource.setJdbcUrl(connectURI);
     	datasource.setMaxPoolSize(512); 
      

	}
	public void close() throws Exception {
		
		if (datasource!= null)	datasource.close();		

	}
	@Override
	protected void finalize() throws Throwable {
		try {
		close();
		} catch (Exception x) {
			
		}
		super.finalize();
	}

	public DataSource getDatasource() {
		return datasource;
	}
}

