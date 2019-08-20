package testing;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;

import database.Database;
import logging.LoggerFactory;
import testing.entities.Table;

public abstract class DatabaseTestCase extends TestCase {

	private final DatabaseMock database;

	public DatabaseTestCase(final Properties properties) {
		super(properties);
		this.database = new DatabaseMock(
				env.createDbConfig(),
				getDataSet(),
				new LoggerFactory(env.createLogConfig()).getLogger(DatabaseMock.class)
		);
	}

	public DatabaseTestCase(final String propertiesPath) {
		super(propertiesPath);
		this.database = new DatabaseMock(
				env.createDbConfig(),
				getDataSet(), 
				new LoggerFactory(env.createLogConfig()).getLogger(DatabaseMock.class)
		);
	}	

	protected abstract List<Table> getDataSet();
	
	@Before
	public void before() throws SQLException {
		database.createDbAndMigrate();
		database.prepare();
	}
	
	@After
	public void after() throws SQLException {
		database.clean();
	}
	
	protected Database getDatabase() {
		return database;
	}

	protected DatabaseMock getDbMock() {
		return database;
	}

	protected Database getNestedDatabase() {
		return database.getNestedDatabase();
	}

}