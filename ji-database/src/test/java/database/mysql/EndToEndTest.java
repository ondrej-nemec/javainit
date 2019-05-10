package database.mysql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import common.Logger;
import database.MySql;
import database.support.DatabaseRow;
import querybuilder.Join;
import querybuilder.QueryBuilder;
import querybuilder.SelectQueryBuilder;
import utils.env.DatabaseConfig;

public class EndToEndTest {
	
	private QueryBuilder builder;
	
	private MySql database;

	@Before
	public void before() {
		DatabaseConfig config = new DatabaseConfig(
				"mysql",
				"//localhost",
				true,
				"javainit_database_test",
				"root",
				"",
				"migrations"
		);
		
		Logger logger = mock(Logger.class);		
		this.database = new MySql(config, logger);
		database.createDbAndMigrate();
		
		this.builder = database.getQueryBuilder();
	}
	
	@After
	public void after() throws SQLException {
		database.applyQuery((conn)->{
			Statement stat = conn.createStatement();
			
			stat.executeUpdate(
				"DROP TABLE update_table,"
				+ " delete_table,"
				+ " insert_table,"
				+ " select_table,"
				+ " joined_table,"
				+ " flyway_schema_history"
			);
		});
	}
	
	@Test
	public void testQueryBuilderInstance() {
		assertTrue(builder instanceof MySqlQueryBuilder);
	}	
	
	@Test
	public void testExecuteUpdate() throws SQLException {
		int code = builder.update("update_table")
			   .set("name='setted name'")
			   .where("id > 1")
			   .andWhere("name='set it'")
			   .orWhere("name='this too'")
			   .execute();
		assertEquals(code, 2);
		
		database.applyQuery((conn)->{
			ResultSet res = conn.createStatement().executeQuery("SELECT * FROM update_table");
			
			int id = 1;
			while(res.next()) {
				switch(id) {
					case 1: assertRow(res, id, "set it");break;
					case 2: assertRow(res, id, "setted name");break;
					case 3: assertRow(res, id, "setted name");break;
					case 4: assertRow(res, id, "this not");break;
					default: throw new RuntimeException("Upgrade your tests!");
				}
				id++;
			}
		});
	}
	
	private void assertRow(ResultSet res, int id, String name) throws SQLException {
		assertEquals(id, res.getInt("id"));
		assertEquals(name, res.getString("name"));
	}

	@Test
	public void testExecuteDelete() throws SQLException {
		int code = builder.delete("delete_table")
			   .where("id > 1")
			   .andWhere("name='delete this'")
			   .orWhere("name='this too'")
			   .execute();
		assertEquals(code, 2);
		
		database.applyQuery((conn)->{
			ResultSet res = conn.createStatement().executeQuery("SELECT * FROM delete_table");
			
			int id = 1;
			while(res.next()) {
				switch(id) {
					case 1: assertRow(res, id, "delete this");break;
					case 2: assertRow(res, 4, "this not");break;
					default: throw new RuntimeException("Upgrade your tests!");
				}
				id++;
			}
		});
	}
	
	@Test
	public void testExecuteInsert() throws SQLException {
		int code = builder.insert("insert_table")
			   .addColumns("id", "name")
			   .values("1", "'column_name'")
			   .execute();
		assertEquals(code, 1);
		
		database.applyQuery((conn)->{
			ResultSet res = conn.createStatement().executeQuery("SELECT * FROM insert_table");
			
			int id = 1;
			while(res.next()) {
				switch(id) {
					case 1: assertRow(res, id, "column_name");break;
					default: throw new RuntimeException("Upgrade your tests!");
				}
				id++;
			}
		});
	}
	
	@Test
	public void testExecuteSelect() throws SQLException {
		SelectQueryBuilder res = builder.select("a.id a_id, b.id b_id, a.a_name, b.b_name")
			   .from("select_table a")
			   .join("joined_table b", Join.INNER_JOIN, "a.id = b.a_id")
			   .where("a.id > 1")
			   .andWhere("b.name = 'name_b'")
			   .orWhere("a.name = 'name_a'")
			   .groupBy("a.id")
			   .having("a.id < 6")
			   .orderBy("a.id DESC")
			   .limit(2)
			   .offset(0);
		
		String expectedSingle = "2";
		DatabaseRow expectedRow = new DatabaseRow();
		expectedRow.addValue("a_id", "2");
		expectedRow.addValue("b_id", "3");
		expectedRow.addValue("a_name", "name_2");
		expectedRow.addValue("b_name", "name_b");
		

		DatabaseRow expectedRow2 = new DatabaseRow();
		expectedRow2.addValue("a_id", "4");
		expectedRow2.addValue("b_id", "5");
		expectedRow2.addValue("a_name", "name a");
		expectedRow2.addValue("b_name", "name 5");
		
		assertEquals(expectedSingle, res.fetchSingle());
		assertEquals(expectedRow, res.fetchRow());
		assertEquals(
				Arrays.asList(expectedRow, expectedRow2),
				res.fetchAll()
		);
	}
	
}
