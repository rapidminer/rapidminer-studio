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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.resource.TestIOObject;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * Testing creation of filesystem repositories
 *
 * @author Andreas Timm, Marco Boeck
 * @since 9.7
 */
public class FilesystemRepositoryFactoryTest {

    private static final String PROCESS_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><process version=\"9.5.000-SNAPSHOT\">\n" +
            "  <context>\n    <input/>\n    <output/>\n    <macros/>\n  </context></process>";
    private static final String PROCESS_ENTRY_NAME = "testprocess";
    private static final String ES_ENTRY_NAME = "testexampleset";
    private static final String BINARY_ENTRY_NAME_1 = "testbinary.py";
    private static final String BINARY_ENTRY_NAME_2 = "testbinary.txt";
    private static final String PYTHON_CONTENT = "Hello Python!";
    private static final String TEXT_CONTENT = "Hello Text!";

    private Path tempDirectory;
    private Repository testRepository;


	@BeforeClass
	public static void setup() {
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.TEST);
		RepositoryManager.init();
	}

    @Before
    public void createTestRepo() throws IOException, RepositoryException {
        tempDirectory = Files.createTempDirectory(UUID.randomUUID().toString());
        FilesystemRepositoryFactory.createRepository("Test Local", tempDirectory, EncryptionProvider.DEFAULT_CONTEXT);
        testRepository = RepositoryManager.getInstance(null).getRepository("Test Local");
    }

    @After
    public void deleteTestRepo() {
        RepositoryManager.getInstance(null).removeRepository(testRepository);
        FileUtils.deleteQuietly(tempDirectory.toFile());
    }


    @Test
    public void createFilesystemRepository() throws RepositoryException {
        assertTrue(testRepository instanceof FilesystemRepositoryAdapter);
        assertFalse(((FilesystemRepositoryAdapter) testRepository).getGeneralRepository().isVersioned());
        assertTrue(testRepository.isConfigurable());
        assertTrue(testRepository.isSupportingBinaryEntries());
		try {
			BlobEntry no_name = testRepository.createBlobEntry("no name");
			fail("Created blob but that should not be possible");
		} catch (RepositoryException re) {
			assertEquals("Can only store blobs in old legacy repositories!", re.getMessage());
		}
		assertFalse(testRepository.isSpecialConnectionsFolder());
		assertFalse(testRepository.move(null));
		assertFalse(testRepository.move(null, null));
		assertEquals(testRepository.getLocation().getAbsoluteLocation(), "//Test Local");
		assertFalse(testRepository.isReadOnly());
    }

    @Test
    public void testConnectionsFolderIsCreatedInLocal() throws RepositoryException {
        Folder connFolder = RepositoryManager.getInstance(null).locateFolder(testRepository, Folder.CONNECTION_FOLDER_NAME, false);
        assertNotNull("Connections folder was null", connFolder);
        assertTrue("Connections folder was not really a Folder entry", Folder.class.isAssignableFrom(connFolder.getClass()));
        assertTrue("Connections folder did report it is not a connections folder", connFolder.isSpecialConnectionsFolder());

        try {
            connFolder.createFolder("subfolder");
            fail("Could create subfolder in Connections folder");
        } catch (RepositoryException e) {
            // expected
        }
    }

    @Test
    public void testParent() throws RepositoryException {
        Folder myFolder = testRepository.createFolder("myFolder");
        Folder nestedFolder = myFolder.createFolder("nestedFolder");
        ProcessEntry myProcess = nestedFolder.createProcessEntry("myProcess", PROCESS_XML);
        ProcessEntry rootProcess = testRepository.createProcessEntry("rootProcess", PROCESS_XML);
        assertNotNull("myFolder was null", myFolder);
        assertNotNull("nestedFolder was null", nestedFolder);
        assertNotNull("myProcess was null", myProcess);
        assertNotNull("rootProcess was null", rootProcess);

        assertEquals(myFolder, nestedFolder.getContainingFolder());
        assertEquals(nestedFolder, myProcess.getContainingFolder());
        // entry in root must give repository back in old repository framework, that is a feature that is used throughout the codebase
        assertEquals(testRepository, rootProcess.getContainingFolder());
        // subfolder of root must give repository back in old repository framework, that is a feature that is used throughout the codebase
        assertEquals(testRepository, myFolder.getContainingFolder());
    }

    @Test
    public void testDeletion() throws RepositoryException {
        Folder myFolder = testRepository.createFolder("myFolder");
        Folder nestedFolder = myFolder.createFolder("nestedFolder");
        ProcessEntry myProcess = nestedFolder.createProcessEntry("myProcess", PROCESS_XML);
        ProcessEntry myProcess2 = nestedFolder.createProcessEntry("myProcess2", PROCESS_XML);
        assertNotNull("myFolder was null", myFolder);
        assertNotNull("nestedFolder was null", nestedFolder);
        assertNotNull("myProcess was null", myProcess);
        assertNotNull("myProcess2 was null", myProcess2);

        assertEquals(1, myFolder.getSubfolders().size());
        assertEquals(0, myFolder.getDataEntries().size());
        assertEquals(0, nestedFolder.getSubfolders().size());

        assertEquals(2, nestedFolder.getDataEntries().size());
        myProcess2.delete();
        assertEquals(1, nestedFolder.getDataEntries().size());

        nestedFolder.delete();
        assertEquals(0, myFolder.getSubfolders().size());

        // conn folder is always there, so we expect 2 now
        assertEquals(2, testRepository.getSubfolders().size());
        myFolder.delete();
        assertEquals(1, testRepository.getSubfolders().size());
    }

    @Test
    public void testDuplicateNamePrevention() throws RepositoryException {
        // on repo level (root)
        ensureDuplicationPrevention(testRepository);
        Folder subfolder = testRepository.createFolder("subfolder");
        // on subfolder level
        ensureDuplicationPrevention(subfolder);
    }

    private void ensureDuplicationPrevention(Folder folder) throws RepositoryException {
        folder.createProcessEntry("process", PROCESS_XML);
        assertTrue("test repo did not contain process entry", folder.containsData("process", ProcessEntry.class));
        assertTrue("test repo did not contain process entry", folder.containsData("process" + ProcessEntry.RMP_SUFFIX, ProcessEntry.class));

        // this should work since 9.7
        folder.createFolder("process");

        try {
            folder.createFolder("process" + ProcessEntry.RMP_SUFFIX);
            fail("Could create 'process" + ProcessEntry.RMP_SUFFIX + "' folder despite having created a 'process' process entry in " + folder.getName());
        } catch (RepositoryException e) {
            // expected
        }
        String duplicateFolderName = "duplicateFolder";
        folder.createFolder(duplicateFolderName);
        try {
            folder.createFolder(duplicateFolderName);
            fail("Could create 'duplicateFolder' folder despite having created a 'duplicateFolder' folder already in " + folder.getName());
        } catch (RepositoryException e) {
            // expected
        }

        // this should work
        folder.createProcessEntry(duplicateFolderName, PROCESS_XML);
        // this should fail
        try {
            folder.createProcessEntry(duplicateFolderName, PROCESS_XML);
            fail("Could create 'duplicateFolder' process entry despite having created a 'duplicateFolder' process entry already in " + folder.getName());
        } catch (RepositoryException e) {
            // expected
        }

        // this should work since 9.7
        folder.createIOObjectEntry(duplicateFolderName, new TestIOObject(), null, null);
        // this should fail
        try {
            folder.createIOObjectEntry(duplicateFolderName, new TestIOObject(), null, null);
            fail("Could create 'duplicateFolder' ioobject entry despite having created a 'duplicateFolder' ioobject entry already in " + folder.getName());
        } catch (RepositoryException e) {
            // expected
        }

        // this should still fail even after 9.7 because a folder and a binary entry cannot have the exact same name
        // as binary entries do not have a hidden suffix like process and IOObject above
        try {
            folder.createBinaryEntry(duplicateFolderName);
            fail("Could create 'duplicateFolder' binary entry despite having created a 'duplicateFolder' folder entry already in " + folder.getName());
        } catch (RepositoryException e) {
            // expected
        }
    }
}
