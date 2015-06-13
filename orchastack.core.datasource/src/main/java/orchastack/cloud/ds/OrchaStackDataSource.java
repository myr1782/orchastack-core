package orchastack.cloud.ds;

import java.sql.SQLException;
import java.util.Dictionary;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Updated;
import org.apache.felix.ipojo.annotations.Validate;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.xa.DruidXADataSource;

//@Component(managedservice = "orchastack.core.ds.params", immediate = true, propagation = true)
//@Provides(specifications = { javax.sql.DataSource.class })
//@Instantiate(name = "defaultDataSource")
public class OrchaStackDataSource extends DruidXADataSource {

	private static Logger log = Logger.getLogger(OrchaStackDataSource.class
			.getName());

//	@Validate
	public void init() {
		try {
			super.init();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Error initialize Datasource!!!", e);
		}
	}

//	@Invalidate
	public void close() {
		super.close();
	}

//	@ServiceProperty(name = "osgi.jndi.service.name", value = "jdbc/xaSoiDS")
	private String m_jndiName;

//	@Updated
	public void update(Dictionary dict) {
		try {
			if (dict != null) {

				String jndiName = (String) dict.get("jndiName");
				if (jndiName != null)
					this.m_jndiName = jndiName;

				String url = (String) dict.get("url");
				if (url != null)
					setUrl(url);

				String username = (String) dict.get("username");
				if (username != null)
					setUsername(username);

				String password = (String) dict.get("password");
				if (password != null)
					setPassword(password);

				String size = (String) dict.get("initialSize");
				if (size != null)
					setInitialSize(Integer.parseInt(size));

				String minIdle = (String) dict.get("minIdle");
				if (minIdle != null)
					setMinIdle(Integer.parseInt(minIdle));

				String maxActive = (String) dict.get("maxActive");
				if (maxActive != null)
					setMaxActive(Integer.parseInt(maxActive));

				String maxWait = (String) dict.get("maxWait");
				if (maxWait != null)
					setMaxWait(Integer.parseInt(maxWait));

				String timeBERM = (String) dict
						.get("timeBetweenEvictionRunsMillis");
				if (timeBERM != null)
					setTimeBetweenEvictionRunsMillis(Long.parseLong(timeBERM));

				String minEITM = (String) dict
						.get("minEvictableIdleTimeMillis");
				if (minEITM != null)
					setMinEvictableIdleTimeMillis(Long.parseLong(minEITM));

				String testWhileIdle = (String) dict.get("testWhileIdle");
				if (testWhileIdle != null)
					setTestWhileIdle(Boolean.parseBoolean(testWhileIdle));

				String testOnBorrow = (String) dict.get("testOnBorrow");
				if (testOnBorrow != null)
					setTestOnBorrow(Boolean.parseBoolean(testOnBorrow));

				String testOnReturn = (String) dict.get("testOnReturn");
				if (testOnReturn != null)
					setTestOnReturn(Boolean.parseBoolean(testOnReturn));

				String pool = (String) dict.get("poolPreparedStatements");
				if (pool != null)
					setPoolPreparedStatements(Boolean.parseBoolean(pool));

				String poolSize = (String) dict
						.get("maxPoolPreparedStatementPerConnectionSize");
				if (poolSize != null)
					setMaxPoolPreparedStatementPerConnectionSize(Integer
							.parseInt(poolSize));

				String sql = (String) dict.get("validationQuery");
				if (sql != null)
					setValidationQuery(sql);

				String filters = (String) dict.get("filters");
				if (filters != null)
					try {
						setFilters(filters);
					} catch (SQLException e) {
						log.log(Level.SEVERE,
								"Error update Datasource property - filters !",
								e);
					}

				String props = (String) dict.get("connectionProperties");
				if (props != null)
					setConnectionProperties(props);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error update properties !!!", e);
		}
	}

}
