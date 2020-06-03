/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.repository.versioned;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryConnectionsFolderImmutableException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositoryNotConnectionsFolderException;
import com.rapidminer.repository.RepositoryStoreOtherInConnectionsFolderException;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.repository.resource.TestIOObject;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * Tests for the FilesystemRepositoryAdapter making sure the Connections folder behaves properly and files and folders
 * can be moved.
 *
 * @author Andreas Timm
 */
public class FilesystemRepositoryAdapterTest {

	private static final String PROCESS_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><process version=\"9.5.000-SNAPSHOT\">\n" +
			"  <context>\n    <input/>\n    <output/>\n    <macros/>\n  </context></process>";
	private static final String PROCESS_ENTRY_NAME_1 = "testprocess1";
	private static final String PROCESS_ENTRY_NAME_2 = "testprocess2";
	private static final String ES_ENTRY_NAME = "testexampleset";
	private static final String BINARY_ENTRY_NAME_1 = "testbinary.py";
	private static final String BINARY_ENTRY_NAME_2 = "testbinary.txt";
	private static final String PYTHON_CONTENT = "Hello Python!";
	private static final String TEXT_CONTENT = "Hello Text!";
	private static final String NEW_FOLDER = "newfolder";
	private static final String DATA_FOLDER = "data";
	private static final String OLD_DATA_FOLDER = "olddata";

	private static Repository testRepository;


	@BeforeClass
	public static void setup() throws Exception {
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.TEST);
		RepositoryManager.init();
		Path tempDirectory = Files.createTempDirectory(UUID.randomUUID().toString());
		FilesystemRepositoryFactory.createRepository("Test Local", tempDirectory, EncryptionProvider.DEFAULT_CONTEXT);
		testRepository = RepositoryManager.getInstance(null).getRepository("Test Local");
	}

	@Test
	public void moveFolder() throws Exception {
		Folder newfolder = testRepository.createFolder(NEW_FOLDER);
		assertEquals(2, testRepository.getSubfolders().size());
		Folder datafolder = testRepository.createFolder(DATA_FOLDER);
		try {
			newfolder.rename(DATA_FOLDER);
			fail("It should not be possible to rename a folder if another entry with the name exists");
		} catch (RepositoryException re) {
			assertEquals("Folder with name '" + DATA_FOLDER + "' already exists", re.getMessage());
		}
		assertTrue(newfolder.move(datafolder));
		assertEquals(1, datafolder.getSubfolders().size());
		assertEquals(datafolder, newfolder.getContainingFolder());
		assertTrue(newfolder.move(testRepository, OLD_DATA_FOLDER));
		assertEquals(OLD_DATA_FOLDER, newfolder.getName());
		assertEquals(3, testRepository.getSubfolders().size());
		newfolder.delete();
		assertEquals(2, testRepository.getSubfolders().size());
		datafolder.delete();
	}


	@Test
	public void moveFile() throws Exception {
		Folder newfolder = testRepository.createFolder(NEW_FOLDER);
		ProcessEntry processEntry = newfolder.createProcessEntry(PROCESS_ENTRY_NAME_1, "");
		assertEquals(1, newfolder.getDataEntries().size());
		processEntry.rename(PROCESS_ENTRY_NAME_2);
		assertEquals(PROCESS_ENTRY_NAME_2, processEntry.getName());
		assertEquals("", processEntry.retrieveXML());
		processEntry.storeXML(PROCESS_XML);
		processEntry.move(testRepository);
		assertEquals(0, newfolder.getDataEntries().size());
		assertEquals(0, newfolder.getSubfolders().size());
		assertEquals(2, testRepository.getSubfolders().size());
		assertEquals(PROCESS_XML, processEntry.retrieveXML());
		processEntry.delete();
		assertEquals(0, testRepository.getDataEntries().size());
		newfolder.delete();
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
	public void createConnectionIOObjectEntryOutside() {
		try {
			ConnectionInformation ci = new ConnectionInformationBuilder(new ConnectionConfigurationBuilder("conf", "conf").build()).build();
			testRepository.createConnectionEntry("connection", ci);
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
	public void createBinaryEntryInSpecial() {
		try {
			getConnectionsFolder().createBinaryEntry("foo.bar");
			fail("Storing of non-connections in connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		}
	}

	@Test
	public void moveSpecial() throws RepositoryException {
		Folder testRepositoryFolder = testRepository.createFolder("bla");
		try {
			getConnectionsFolder().move(testRepositoryFolder);
			fail("Moving of connection folder should be forbidden");
		} catch (RepositoryConnectionsFolderImmutableException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER_CHANGE, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		} finally {
			testRepositoryFolder.delete();
		}
	}

	@Test
	public void moveAndRenameSpecial() {
		try {
			getConnectionsFolder().move(testRepository, "blablup");
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
	public void moveIntoSpecial() throws RepositoryException {
		Folder testRepositoryFolder = testRepository.createFolder("bla");
		try {
			testRepositoryFolder.move(getConnectionsFolder());
			fail("Moving into connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		} finally {
			testRepositoryFolder.delete();
		}
	}

	@Test
	public void moveIntoSpecialAndRename() throws RepositoryException {
		Folder testRepositoryFolder = testRepository.createFolder("bla");
		try {
			testRepositoryFolder.move(getConnectionsFolder(), "blablup");
			fail("Moving into connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		} finally {
			testRepositoryFolder.delete();
		}
	}

	@Test
	public void moveElementIntoSpecial() throws RepositoryException {
		IOObjectEntry test = testRepository.createIOObjectEntry("test", new TestIOObject(), null, null);
		try {
			test.move(getConnectionsFolder());
			fail("Moving into connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		} finally {
			test.delete();
		}
	}

	@Test
	public void moveElementIntoSpecialAndRename() throws RepositoryException {
		BinaryEntry binaryEntry = testRepository.createBinaryEntry("foo.bar");
		try {
			binaryEntry.move(getConnectionsFolder(), "bla.blup");
			fail("Moving into connection folder should be forbidden");
		} catch (RepositoryStoreOtherInConnectionsFolderException e) {
			assertEquals(Folder.MESSAGE_CONNECTION_FOLDER, e.getMessage());
		} catch (RepositoryException e) {
			fail("Expected more specific exception when trying to store other objects in Connections folder");
		} finally {
			binaryEntry.delete();
		}
	}

	private Folder getConnectionsFolder() throws RepositoryException {
		return RepositoryTools.getConnectionFolder(testRepository);
	}

}
