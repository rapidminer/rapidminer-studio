/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryConnectionsFolderImmutableException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositoryNotConnectionsFolderException;
import com.rapidminer.repository.RepositoryStoreOtherInConnectionsFolderException;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.tools.FunctionWithThrowable;


/**
 * A few tests for the {@link LocalRepository} with respect to special Connections folders.
 */
public class LocalRepositoryFolderTest {

	private static final String TEST = "forTest";
	private static final String TEST_WITH_EXISTING = "forTestExisting";
	private static LocalRepository folder;
	private static LocalRepository testInsideConnectionsFolder;

	private static RepositoryManager repositoryManager;

	@BeforeClass
	public static void setup() throws RepositoryException, IOException {
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.TEST);
		repositoryManager = RepositoryManager.getInstance(null);
		File root = Files.createTempDirectory("testRepo_").toFile();
		root.deleteOnExit();
		folder = new LocalRepository(TEST, root);
		repositoryManager.addRepository(folder);

		File rootWithExistingConnectionFolder = Files.createTempDirectory("testRepoWithConnectionFolder_").toFile();
		File connFolderChild = new File(new File(rootWithExistingConnectionFolder, Folder.CONNECTION_FOLDER_NAME), "Test");
		connFolderChild.mkdirs();
		rootWithExistingConnectionFolder.deleteOnExit();
		testInsideConnectionsFolder = new LocalRepository(TEST_WITH_EXISTING, rootWithExistingConnectionFolder);
		repositoryManager.addRepository(testInsideConnectionsFolder);
	}

	@AfterClass
	public static void teardown() throws RepositoryException {
		purgeDirectory(folder.getRoot());
	}

	@Test
	public void createFolderInSpecial() {
		try {
			getConnectionsFolder().createFolder(null);
			fail("Storing of non-connections in connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	private Folder getConnectionsFolder() throws RepositoryException {
		return RepositoryTools.getConnectionFolder(repositoryManager.getRepository(TEST));
	}

	private Folder getSubFolderOfConnectionsFolder() throws RepositoryException {
		return RepositoryTools.getConnectionFolder(repositoryManager.getRepository(TEST_WITH_EXISTING)).getSubfolders().get(0);
	}

	@Test
	public void createIOObjectEntryInSpecial() {
		try {
			getConnectionsFolder().createIOObjectEntry(null, null, null, null);
			fail("Storing of non-connections in connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	@Test
	public void createIOObjectEntryInExistingFolderInsideSpecial() {
		try {
			getSubFolderOfConnectionsFolder().createIOObjectEntry(null, null, null, null);
			fail("Storing of non-connections in subfolder of existing connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	@Test
	public void createConnectionIOObjectEntryInExistingFolderInsideSpecial() {
		try {
			ConnectionInformationContainerIOObject ioObject = new ConnectionInformationContainerIOObject(null);
			getSubFolderOfConnectionsFolder().createIOObjectEntry(null, ioObject, null, null);
			fail("Storing of connections in subfolder of existing connection folder should be forbidden");
		} catch (RepositoryNotConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_CREATION, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store connection in subfolder of existing Connections folder");
		}
	}

	@Test
	public void createConnectionIOObjectEntryOutside() {
		try {
			ConnectionInformationContainerIOObject ioObject = new ConnectionInformationContainerIOObject(null);
			folder.createIOObjectEntry(null, ioObject, null, null);
			fail("Storing of connections outside connection folder should be forbidden");
		} catch (RepositoryNotConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_CREATION, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when storing connections outside connection folder");
		}
	}

	@Test
	public void createProcessEntryInSpecial() {
		try {
			getConnectionsFolder().createProcessEntry(null, null);
			fail("Storing of non-connections in connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	@Test
	public void createConnectionEntryOutside() {
		try {
			folder.createConnectionEntry(null, null);
			fail("Storing connections outside connection folder should be forbidden");
		} catch (RepositoryNotConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_CREATION, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when storing connections outside connection folder");
		}
	}

	@Test
	public void createBlobEntryInSpecial() {
		try {
			getConnectionsFolder().createBlobEntry(null);
			fail("Storing of non-connections in connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	@Test
	public void moveSpecial() {
		try {
			getConnectionsFolder().move(new SimpleFolder("bla", folder, folder));
			fail("Moving of connection folder should be forbidden");
		} catch (RepositoryConnectionsFolderImmutableException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER_CHANGE, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	@Test
	public void moveAndRenameSpecial() {
		try {
			getConnectionsFolder().move(folder, "blablup");
			fail("Moving of connection folder should be forbidden");
		} catch (RepositoryConnectionsFolderImmutableException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER_CHANGE, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	@Test
	public void renameSpecial() {
		try {
			getConnectionsFolder().rename("blablup");
			fail("Renaming of connection folder should be forbidden");
		} catch (RepositoryConnectionsFolderImmutableException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER_CHANGE, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	@Test
	public void deleteSpecial() {
		try {
			getConnectionsFolder().delete();
			fail("Deleting of connection folder should be forbidden");
		} catch (RepositoryConnectionsFolderImmutableException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER_CHANGE, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	@Test
	public void moveIntoSpecial() {
		try {
			SimpleFolder test = new SimpleFolder("bla", folder, folder);
			test.move(getConnectionsFolder());
			fail("Moving into connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	@Test
	public void moveIntoSpecialAndRename() {
		try {
			SimpleFolder test = new SimpleFolder("bla", folder, folder);
			test.move(getConnectionsFolder(), "blablup");
			fail("Moving into connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	@Test
	public void moveElementIntoSpecial() {
		try {
			SimpleEntry test = new SimpleIOObjectEntry("test", folder, folder);
			test.move(getConnectionsFolder());
			fail("Moving into connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	@Test
	public void moveElementIntoSpecialAndRename() {
		try {
			SimpleEntry test = new SimpleIOObjectEntry("test", folder, folder);
			test.move(getConnectionsFolder(), "blablup");
			fail("Moving into connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	@Test
	public void getConnections() throws RepositoryException {
		List<ConnectionConfiguration> expected = new ArrayList<>(2);
		Folder folder = getConnectionsFolder();
		ConnectionInformation conInformation =
				new ConnectionInformationBuilder(new ConnectionConfigurationBuilder("bla", "blup").build()).build();
		ConnectionInformationContainerIOObject ioobject = new ConnectionInformationContainerIOObject(conInformation);
		expected.add(conInformation.getConfiguration());
		folder.createIOObjectEntry("test1", ioobject, null, null);
		ConnectionInformation connectionInformation =
				new ConnectionInformationBuilder(new ConnectionConfigurationBuilder("dings", "bums").build()).build();
		folder.createConnectionEntry("test2", connectionInformation);
		expected.add(connectionInformation.getConfiguration());

		List<ConnectionConfiguration> actual = RepositoryTools.getConnections(repositoryManager.getRepository(TEST)).stream()
				.map(FunctionWithThrowable.wrap(e ->((ConnectionInformationContainerIOObject) e.retrieveData(null))
						.getConnectionInformation().getConfiguration()))
				.collect(Collectors.toList());
		assertEquals(expected, actual);
	}

	/**
	 * Recursively deletes all files in the directory.
	 *
	 * @param dir
	 * 		the directory to purge
	 */
	public static void purgeDirectory(File dir) {
		for (File file: dir.listFiles()) {
			if (file.isDirectory()) {
				purgeDirectory(file);
			}
			file.delete();
		}
	}
}
