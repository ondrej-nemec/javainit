package common;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import common.env.AppMode;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class EnvTest {

	@Test(expected = IOException.class)
	public void testConstructorForFilesThrowIfNoFileInDir() throws FileNotFoundException, IOException {
		new Env(AppMode.AUTOLOAD, "/env/empty");
	}
	
	@Test
	@Parameters
	public void testConstructorForFilesWithAutoloadModeFindCorrectProperties(final AppMode mode, final String subDir)
			throws FileNotFoundException, IOException {
		Env e = new Env(AppMode.AUTOLOAD, "/env/" + subDir);
		assertEquals(mode, e.mode);
		assertEquals("value", e.getProperties().getProperty("key"));
	}
	
	public Collection<Object[]> parametersForTestConstructorForFilesWithAutoloadModeFindCorrectProperties() {
		return Arrays.asList(
				new Object[] {AppMode.PROD, "prod"},
				new Object[] {AppMode.DEV, "dev"},
				new Object[] {AppMode.TEST, "test"}
		);
	}
	
	@Test(expected=RuntimeException.class)
	public void testConstructorForCodeThrowsIfModeIsAutoload() {
		new Env(AppMode.AUTOLOAD, new Properties());
	}
	
	@Test
	@Ignore
	public void testCreateDbConfigReturnCorrectConfig() {
		// get env from dataprovider - file and code loading
	}
	
	@Test
	@Ignore
	public void testGetPropertyReturnPropertyOrNullIfNotExists() {
		// get env from dataprovider - file and code loading
	}
	
	@Test(expected=RuntimeException.class)
	@Ignore
	public void testGetPropertyOrThrowIfNotExistsThrowsIfPropertyNotExists() {
		// get env from dataprovider - file and code loading
	}
	
	@Test
	@Ignore
	public void testGetPropertyOrThrowIfNotExistsReturnProperty() {
		// get env from dataprovider - file and code loading
	}
}
