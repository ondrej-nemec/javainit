package querybuilder.mysql;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.sql.Connection;

import org.junit.Test;

import querybuilder.AlterTableQueryBuilder;
import querybuilder.ColumnSetting;
import querybuilder.ColumnType;

public class MySqlAlterTableBuilderTest {
	
	@Test
	public void testBuilderViaSql() {
		Connection con = mock(Connection.class);
		AlterTableQueryBuilder builder = new MySqlAlterTableBuilder(con, "Table1")
				.addColumn("Column1", ColumnType.integer(), ColumnSetting.NOT_NULL)
				.addColumn("Column2", ColumnType.integer(), 1)
				.deleteColumn("Column3")
				.addForeingKey("Column", "Table2", "id")
				.deleteForeingKey("Column")
				.modifyColumnType("Column4", ColumnType.integer())
				.renameColumn("Column5", "Column6", ColumnType.integer());
		
		String expected = "ALTER TABLE Table1"
				+ " ADD Column1 INT NOT NULL,"
				+ " ADD Column2 INT DEFAULT 1,"
				+ " DROP COLUMN Column3,"
				+ " ADD FOREIGN KEY (Column) REFERENCES Table2(id),"
				+ " DROP FOREIGN KEY Column,"
				+ " MODIFY Column4 INT,"
				+ " CHANGE COLUMN Column5 Column6 INT";
		
		assertEquals(expected, builder.getSql());
		verifyNoMoreInteractions(con);
	}

}