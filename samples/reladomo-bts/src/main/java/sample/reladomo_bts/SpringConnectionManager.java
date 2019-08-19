package sample.reladomo_bts;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.TimeZone;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gs.fw.common.mithra.bulkloader.BulkLoader;
import com.gs.fw.common.mithra.bulkloader.BulkLoaderException;
import com.gs.fw.common.mithra.connectionmanager.SourcelessConnectionManager;
import com.gs.fw.common.mithra.databasetype.DatabaseType;
import com.gs.fw.common.mithra.databasetype.H2DatabaseType;

@Component
public class SpringConnectionManager implements SourcelessConnectionManager, InitializingBean {

	private static SpringConnectionManager instance;

	@Autowired
	DataSource dataSource;

	@Value("${spring.datasource.name:testdb}")
	String name;

	@Value("${spring.datasource.driverClassName}")
	String driverClassName;

	@Override
	public BulkLoader createBulkLoader() throws BulkLoaderException {
		throw new BulkLoaderException("bulk loading not supported");
	}

	@Override
	public Connection getConnection() {
		try {
			return dataSource.getConnection();
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public DatabaseType getDatabaseType() {
		if ("org.h2.Driver".equals(driverClassName)) {
			return H2DatabaseType.getInstance();
		}
		throw new IllegalStateException("spring.datasource.driverClassName '" + driverClassName + "' is not supported");
	}

	@Override
	public TimeZone getDatabaseTimeZone() {
		return TimeZone.getTimeZone("America/New_York");
	}

	@Override
	public String getDatabaseIdentifier() {
		return name;
	}

	@Override
	public void afterPropertiesSet() {
		instance = this;
	}

	public static SpringConnectionManager getInstance() {
		return instance;
	}
}
