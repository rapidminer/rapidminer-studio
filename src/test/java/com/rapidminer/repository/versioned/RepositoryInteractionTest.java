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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.rapidminer.RapidMiner;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.connection.configuration.ConfigurationParameterImpl;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.connection.gui.dto.ConnectionInformationHolder;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.repository.resource.ResourceRepository;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.encryption.EncryptionProvider;
import com.rapidminer.tools.encryption.EncryptionProviderRegistry;
import com.rapidminer.tools.encryption.EncryptionType;


/**
 * Test to copy or move files between repositories
 */
public class RepositoryInteractionTest {

	private static final String ENCRYPTED_VALUE = "hello world";
	private static final String CONNECTION_NAME = "connection";

	private static FilesystemRepositoryAdapter filesystemRepository;
	private static FilesystemRepositoryAdapter filesystemEncryptedA;
	private static FilesystemRepositoryAdapter filesystemEncryptedB;
	private static LocalRepository legacyRepository;
	private static ResourceRepository resourceRepository;
	private static ResourceRepository resourceConnectionRepository;


	@BeforeClass
	public static void setup() throws Exception {
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.TEST);
		RepositoryManager.init();
		EncryptionProvider.initialize();
		EncryptionProviderRegistry.INSTANCE.registerKeysetForContext(KeysetHandle.generateNew(AeadKeyTemplates.AES256_GCM), "A", EncryptionType.SYMMETRIC, false);
		EncryptionProviderRegistry.INSTANCE.registerKeysetForContext(KeysetHandle.generateNew(AeadKeyTemplates.AES256_GCM), "B", EncryptionType.SYMMETRIC, false);
		ProductConstraintManager.INSTANCE.initialize(null, null);
		OperatorService.init();

		// set up repos:
		// legacy
		// resource
		// filesystem unencrypted
		// filesystem encrypted with context A
		// filesystem encrypted with context B

		Path tempDirectory1 = Files.createTempDirectory(UUID.randomUUID().toString());
		tempDirectory1.toFile().deleteOnExit();
		Path tempDirectory2 = Files.createTempDirectory(UUID.randomUUID().toString());
		tempDirectory2.toFile().deleteOnExit();
		Path tempDirectoryA = Files.createTempDirectory(UUID.randomUUID().toString());
		tempDirectoryA.toFile().deleteOnExit();
		Path tempDirectoryB = Files.createTempDirectory(UUID.randomUUID().toString());
		tempDirectoryB.toFile().deleteOnExit();

		filesystemRepository = (FilesystemRepositoryAdapter) FilesystemRepositoryFactory.createRepository("New FS repo", tempDirectory1, null);
		filesystemEncryptedA = (FilesystemRepositoryAdapter) FilesystemRepositoryFactory.createRepository("FS repo A", tempDirectoryA, "A");
		filesystemEncryptedA.ensureConnectionsFolder();
		filesystemEncryptedB = (FilesystemRepositoryAdapter) FilesystemRepositoryFactory.createRepository("FS repo B", tempDirectoryB, "B");
		filesystemEncryptedB.ensureConnectionsFolder();
		legacyRepository = new LocalRepository("Legacy", tempDirectory2.toFile()) {

			@Override
			public String getEncryptionContext() {
				return null;
			}
		};
		resourceRepository = new ResourceRepository("Resources", "samples") {

			@Override
			public String getEncryptionContext() {
				return null;
			}
		};
		resourceConnectionRepository = new ResourceRepository("Connection resources", "resourcerepositorytest", false, false){
			@Override
			public boolean supportsConnections() {
				return true;
			}
		};

		// make sure they are available
		resourceRepository.containsData("", DataEntry.class);
		resourceConnectionRepository.containsData("", DataEntry.class);

		RepositoryManager.getInstance(null).addRepository(legacyRepository);
		RepositoryManager.getInstance(null).addRepository(resourceRepository);
		RepositoryManager.getInstance(null).addRepository(filesystemRepository);
		RepositoryManager.getInstance(null).addRepository(filesystemEncryptedA);
		RepositoryManager.getInstance(null).addRepository(filesystemEncryptedB);
		RepositoryManager.getInstance(null).addSpecialRepository(resourceConnectionRepository);
	}

	@AfterClass
	public static void deleteTestRepos() {
		RepositoryManager.getInstance(null).removeRepository(filesystemRepository);
		RepositoryManager.getInstance(null).removeRepository(filesystemEncryptedA);
		RepositoryManager.getInstance(null).removeRepository(filesystemEncryptedB);
		RepositoryManager.getInstance(null).removeRepository(legacyRepository);
		RepositoryManager.getInstance(null).removeRepository(resourceRepository);
	}

	@Test
	public void testCopyingEncryptedConnectionFromAtoB() throws Exception {
		ConnectionInformation ci = createConnection();

		Folder connectionFolderA = filesystemEncryptedA.locateFolder(Folder.CONNECTION_FOLDER_NAME);
		ConnectionEntry conEntryA = connectionFolderA.createConnectionEntry(CONNECTION_NAME, ci);
		ConnectionInformationHolder holderA = ConnectionInformationHolder.from(conEntryA);
		ConnectionInformation loadedConnectionA = holderA.getConnectionInformation();

		String jsonOfConAEncrypted = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(loadedConnectionA.getConfiguration(), "A");
		Assert.assertFalse("jsonOfConAEncrypted did contain plaintext value even though it should have been encrypted!", jsonOfConAEncrypted.contains(ENCRYPTED_VALUE));
		String jsonOfConAPlain = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(loadedConnectionA.getConfiguration(), null);
		Assert.assertTrue("jsonOfConAPlain did not contain plaintext value even though it should NOT have been encrypted!", jsonOfConAPlain.contains(ENCRYPTED_VALUE));


		Folder connectionFolderB = filesystemEncryptedB.locateFolder(Folder.CONNECTION_FOLDER_NAME);
		RepositoryManager.getInstance(null).copy(conEntryA.getLocation(), connectionFolderB, null);
		ConnectionEntry conEntryB = (ConnectionEntry) connectionFolderB.getDataEntries().stream().filter(d -> CONNECTION_NAME.equals(d.getName())).findFirst().orElseThrow(() -> new RepositoryException("conEntryB not found in repoB"));
		ConnectionInformationHolder holderB = ConnectionInformationHolder.from(conEntryB);
		ConnectionInformation loadedConnectionB = holderB.getConnectionInformation();

		String jsonOfConBEncrypted = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(loadedConnectionB.getConfiguration(), "B");
		Assert.assertFalse("jsonOfConAEncrypted did contain plaintext value even though it should have been encrypted!", jsonOfConBEncrypted.contains(ENCRYPTED_VALUE));
		String jsonOfConBPlain = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(loadedConnectionB.getConfiguration(), null);
		Assert.assertTrue("jsonOfConAPlain did not contain plaintext value even though it should NOT have been encrypted!", jsonOfConBPlain.contains(ENCRYPTED_VALUE));

		Assert.assertNotEquals("Encrypted JSON was equal even though it should NOT have been!", jsonOfConAEncrypted, jsonOfConBEncrypted);
		Assert.assertEquals("Plaintext JSON was not equal even though it should have been!", jsonOfConAPlain, jsonOfConBPlain);
	}

	private ConnectionInformation createConnection() {
		ConnectionConfigurationBuilder config = new ConnectionConfigurationBuilder("Test connection", "whatever");
		config.withKeys("default", Collections.singletonList(new ConfigurationParameterImpl("encryptedKey", ENCRYPTED_VALUE, true)));
		ConnectionInformationBuilder builder = new ConnectionInformationBuilder(config.build());
		return builder.build();
	}

	@Test
	public void copyMoveBetweenRepositories() throws Exception {
		// copy from resource to legacy
		copyAndCheckContentsRecursive(resourceRepository, legacyRepository);
		// copy from legacy to fsrepo
		copyAndCheckContentsRecursive(legacyRepository, filesystemRepository);
		// also copy from connections resource repo
		copyAndCheckContentsRecursive(resourceConnectionRepository, filesystemRepository);

		// clean legacy
		for (Folder sub : legacyRepository.getSubfolders()) {
			if (!sub.isSpecialConnectionsFolder()) {
				sub.delete();
			}
		}
		for (DataEntry dataEntry : legacyRepository.getDataEntries()) {
			dataEntry.delete();
		}
		// copy from fsrepo to legacy
		copyAndCheckContentsRecursive(filesystemRepository, legacyRepository);
	}

	private void copyAndCheckContentsRecursive(Folder sourceFolder, Folder targetFolder) throws RepositoryException {
		for (Folder subfolder : sourceFolder.getSubfolders()) {
			Folder folder;
			if (!subfolder.isSpecialConnectionsFolder()
					&& !Folder.isConnectionsFolderName(subfolder.getName(), false)) {
				folder = targetFolder.createFolder(subfolder.getName());
			} else {
				folder = targetFolder.getSubfolders().stream()
						.filter(f -> f.getName().equalsIgnoreCase(subfolder.getName())).findFirst().orElseThrow(() -> new RepositoryException("Folder " + subfolder.getName() + " not found in targetFolder!"));
			}
			for (DataEntry dataEntry : subfolder.getDataEntries()) {
				// skip not allowed copy
				if (!(dataEntry instanceof BinaryEntry) || folder.getLocation().getRepository().isSupportingBinaryEntries()) {
					RepositoryManager.getInstance(null).copy(dataEntry.getLocation(), folder, null);
				}
			}
			Assert.assertEquals("After copying the amount of entries in source and target folder should be equal", subfolder.getDataEntries().size(), folder.getDataEntries().size());

			// only copy if target supports binary entries
			if (folder.isSupportingBinaryEntries()) {
				for (Folder subSubFolder : subfolder.getSubfolders()) {
					RepositoryManager.getInstance(null).copy(subSubFolder.getLocation(), folder, null);
					Optional<Folder> first = folder.getSubfolders().stream().filter(f -> f.getName().equals(subSubFolder.getName())).findFirst();
					Assert.assertTrue(first.isPresent());
					Assert.assertEquals("After copying from RepositoryManager, the amount of entries in source and target folder should be equal", subSubFolder.getDataEntries().size(), first.get().getDataEntries().size());
				}
				Assert.assertEquals("After copying the amount of entries in source and target folder should be equal", subfolder.getSubfolders().size(), folder.getSubfolders().size());
			}
		}
	}
}
