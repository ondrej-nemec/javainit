package database.support;

import java.sql.Connection;
import java.sql.SQLException;

import common.structures.ThrowingConsumer;

public interface ConnectionConsumer extends ThrowingConsumer<Connection, SQLException> {

	void accept(Connection connection) throws SQLException;
	
}
