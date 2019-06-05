/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.resource;

import static com.rapidminer.tools.FunctionWithThrowable.suppress;

import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.connection.configuration.ConnectionConfigurationImpl;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.local.SimpleConnectionEntry;


/**
 * A few tests for the {@link ResourceFolder}
 */
public class ResourceFolderTest {

	public static final String TEST_CON_NAME = "one";
	private static final String TEST_CON_TYPE = "cli";
	private static final String TEST_CON_ID = "1543493610184";
	private static final String TEST_CON_LIB_FILE = "mariadb-java-client-1.3.6.jar";
	private static final String TEST_CON_OTHER_FILE = "README.txt";

	private Folder resourceFolder = new ResourceFolder(null, "test", null, null);

	private static RepositoryManager repositoryManager;

	/**
	 * <strong>Helper Method</strong> Create the {@link ConnectionInformation} object needed for this test class.
	 * This only needs to be executed if something significant changed in the way {@link ConnectionInformation} works.
	 */
	public static void main(String[] args) throws Exception {
		Path libJDBCPath = Paths.get("lib", "jdbc", TEST_CON_LIB_FILE);
		if (!Files.exists(libJDBCPath)) {
			throw new IllegalArgumentException("Lib file does not exist");
		}
		Optional<URL> resource = Optional.ofNullable(ResourceFolderTest.class.getResource(TEST_CON_OTHER_FILE));
		Path readmePath = resource.map(suppress(URL::toURI)).map(Paths::get).orElse(null);
		if (readmePath == null || !Files.exists(readmePath)) {
			throw new IllegalArgumentException("Readme file does not exist");
		}
		Path junitPath = Paths.get("rapidminer-studio-core", "src", "test", "resources",
				"com", "rapidminer", "resources", "resourcerepositorytest", Folder.CONNECTION_FOLDER_NAME);
		if (!Files.exists(junitPath)) {
			throw new IllegalArgumentException("Junit test path does not exist");
		}
		ConnectionConfiguration config = new ConnectionConfigurationBuilder(TEST_CON_NAME, TEST_CON_TYPE).build();
		Method setId = ConnectionConfigurationImpl.class.getDeclaredMethod("setId", String.class);
		setId.setAccessible(true);
		setId.invoke(config, TEST_CON_ID);
		ConnectionInformation connection = new ConnectionInformationBuilder(config).withLibraryFiles(Collections.singletonList(libJDBCPath))
				.withOtherFiles(Collections.singletonList(readmePath)).build();
		ConnectionInformationSerializer.LOCAL.serialize(connection, Files.newOutputStream(junitPath.resolve(TEST_CON_NAME + SimpleConnectionEntry.CON_SUFFIX)));
	}

	@BeforeClass
	public static void setup() throws RepositoryException {
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.TEST);
		repositoryManager = RepositoryManager.getInstance(null);
		ResourceRepository resourceRepo = new ResourceRepository("test", "resourcerepositorytest") {
			@Override
			public boolean supportsConnections() {
				return true;
			}
		};
		repositoryManager.addSpecialRepository(resourceRepo);
	}

	@Test
	public void readResourceRepository() throws RepositoryException {
		final Repository testRepo = repositoryManager.getRepository("test");
		final Entry entry = testRepo.locate(Folder.CONNECTION_FOLDER_NAME + '/' + TEST_CON_NAME);
		Assert.assertNotNull(entry);
		Assert.assertTrue(entry instanceof ResourceConnectionEntry);
		ConnectionInformation ci = ((ConnectionInformationContainerIOObject) ((ResourceConnectionEntry) entry).retrieveData(null)).getConnectionInformation();
		Assert.assertNotNull(ci);
		Assert.assertEquals("The type entry of the JSON configuration in the ConnectionInformation object should be '" + TEST_CON_TYPE + "'",
				TEST_CON_TYPE, ci.getConfiguration().getType());
		Assert.assertEquals("The expected ID of the JSON configuration in the ConnectionInformation object 'one.conninfo' does not match.",
				TEST_CON_ID, ci.getConfiguration().getId());
		Assert.assertEquals("The name entry of the JSON configuration in the ConnectionInformation object should be '" + TEST_CON_NAME + "'",
				TEST_CON_NAME, ci.getConfiguration().getName());
		Assert.assertEquals("There should be one library file in the ConnectionInformation object", 1, ci.getLibraryFiles().size());
		Assert.assertTrue(ci.getLibraryFiles().get(0).endsWith(TEST_CON_LIB_FILE));
		Assert.assertEquals("There should be one other file in the ConnectionInformation object", 1, ci.getOtherFiles().size());
		Assert.assertTrue(ci.getOtherFiles().get(0).endsWith(TEST_CON_OTHER_FILE));
	}

	@Test
	public void createBlobEntry() {
		try {
			resourceFolder.createBlobEntry(null);
			Assert.fail("Resource Repository should be read only");
		} catch (RepositoryException re) {
			Assert.assertEquals("This is a read-only sample repository. Cannot create new entries.", re.getMessage());
		}
	}

	@Test
	public void createFolder() {
		try {
			resourceFolder.createFolder(null);
			Assert.fail("Resource Repository should be read only");
		} catch (RepositoryException re) {
			Assert.assertEquals("This is a read-only sample repository. Cannot create new entries.", re.getMessage());
		}
	}

	@Test
	public void createIOObjectEntry() {
		try {
			resourceFolder.createIOObjectEntry(null, null, null, null);
			Assert.fail("Resource Repository should be read only");
		} catch (RepositoryException re) {
			Assert.assertEquals("This is a read-only sample repository. Cannot create new entries.", re.getMessage());
		}
	}

	@Test
	public void createProcessEntry() {
		try {
			resourceFolder.createProcessEntry(null, null);
			Assert.fail("Resource Repository should be read only");
		} catch (RepositoryException re) {
			Assert.assertEquals("This is a read-only sample repository. Cannot create new entries.", re.getMessage());
		}
	}

	@Test
	public void createConnectionEntry() {
		try {
			resourceFolder.createConnectionEntry(null, null);
			Assert.fail("Resource Repository should be read only");
		} catch (RepositoryException re) {
			Assert.assertEquals("This is a read-only sample repository. Cannot create new entries.", re.getMessage());
		}
	}

}
