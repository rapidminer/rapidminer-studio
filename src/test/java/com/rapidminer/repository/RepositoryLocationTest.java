/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.repository.versioned.FilesystemRepositoryFactory;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * Unit tests for the {@link RepositoryLocation}
 *
 * @author Andreas Timm
 */
public class RepositoryLocationTest {
	private static final int THREAD_COUNT = 200;
	private String TEST_REPO_NAME = "Test Local";

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
		FilesystemRepositoryFactory.createRepository(TEST_REPO_NAME, tempDirectory, true, false, EncryptionProvider.DEFAULT_CONTEXT);
		testRepository = RepositoryManager.getInstance(null).getRepository(TEST_REPO_NAME);
	}

	@After
	public void deleteTestRepo() {
		RepositoryManager.getInstance(null).removeRepository(testRepository);
		FileUtils.deleteQuietly(tempDirectory.toFile());
	}

	@Test
	public void createFoldersRecursively() throws MalformedRepositoryLocationException, ExecutionException, InterruptedException {
		RepositoryLocation test = new RepositoryLocationBuilder().withLocationType(RepositoryLocationType.FOLDER).buildFromPathComponents(TEST_REPO_NAME, new String[]{"1", "2", "3", "1", "2", "3"});
		ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
		int runs = THREAD_COUNT * 1000;
		List<Future<Void>> futures = new ArrayList<>(runs);
		CountDownLatch startSignal = new CountDownLatch(1);
		for (int i = 0; i < runs; i++) {
			futures.add(executorService.submit(() -> {
				startSignal.await();
				test.createFoldersRecursively();
				return null;
			}));
		}
		startSignal.countDown();
		for (Future<Void> future : futures) {
			future.get();
		}
		// if no errors happenend, the test was successful
	}
}