package migration.endToEndModule2;

import java.sql.SQLException;

import migration.Migration;
import querybuilder.ColumnType;
import querybuilder.QueryBuilder;

public class V1__AddedFirstTable implements Migration {

	@Override
	public void migrate(QueryBuilder builder) throws SQLException {
		builder.createTable("First_table_2").addColumn("id", ColumnType.integer()).execute();
	}

	@Override
	public void revert(QueryBuilder builder) throws SQLException {
		builder.deleteTable("First_table_2").execute();
	}

}
