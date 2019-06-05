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

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.configuration.ConnectionConfigurationImpl;
import com.rapidminer.operator.IOObject;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.repository.local.LocalRepositoryFolderTest;
import com.rapidminer.repository.local.SimpleFolder;
import com.rapidminer.tools.Tools;


/**
 * Tests for concurrent repository access for different {@link Folder} implementations.
 *
 * Each test spawns a number of threads with mixed operations and checks if the folder is in a
 * consistent state.
 *
 * @author Peter Csaszar, Marcel Michel
 */
public class ConcurrentRepositoryTest {

	/**
	 * maximum wait time for threads in milliseconds
	 */
	private static final int THREAD_WAIT_THRESHOLD = 1000;

	/**
	 * number of exceutor threads
	 */
	private static final int THREAD_COUNT = 100;

	/**
	 * all operations / refresh operations ratio
	 */
	private static final double REFRESH_CALL_RATIO = .4;

	private static final String FOLDER_NAME_PREFIX = "folder_";
	private static final String PROCESS_NAME_PREFIX = "process_";
	private static final String IOOBJECT_NAME_PREFIX = "ioobject_";
	private static final String BLOBENTRY_NAME_PREFIX = "blobentry_";
	private static final String CONNECTIONENTRY_NAME_PREFIX = "conninfo_";

	private static final String TEST_PROCESS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";

	private static final String[] EXPECTED_RESOURCE_DATA_ENTRIES = new String[]{"Deals", "Deals-Testset", "Golf",
			"Golf-Testset", "Iris", "Labor-Negotiations", "Market-Data", "Polynomial", "Products", "Purchases", "Ripley-Set",
			"Sonar", "Titanic", "Titanic Training", "Titanic Unlabeled", "Transactions", "Weighting"};

	private Random random = new Random();
	private CountDownLatch startSignal;

	private ResourceFolder getTestResourceFolder(String name) {
		ResourceRepository repository = new ResourceRepository("test", "samples");
		return new ResourceFolder(repository, name, "/" + name, repository);
	}

	private SimpleFolder getTestResourceFolderAsSimpleFolder() {
		File root;
		LocalRepository folder = null;
		try {
			root = new File(Tools.getResource("samples/data").toURI());
			root.deleteOnExit();
			folder = new LocalRepository("test", root);
		} catch (URISyntaxException | RepositoryException e) {
			e.printStackTrace();
			Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
		}
		return folder;
	}

	@Test
	public void resourceFolder_DataEntries() throws Exception {
		Folder reference = getTestResourceFolder("data");
		Folder test = getTestResourceFolder("data");
		testLoadWithRefresh(test, reference.getDataEntries().size(), reference.getSubfolders().size(),
				EXPECTED_RESOURCE_DATA_ENTRIES);
	}

	@Test
	public void simpleFolder_DataEntries() throws Exception {
		Folder reference = getTestResourceFolderAsSimpleFolder();
		Folder test = getTestResourceFolderAsSimpleFolder();
		testLoadWithRefresh(test, reference.getDataEntries().size(), reference.getSubfolders().size());
	}

	@Test
	public void simpleFolder_CreateItems() throws Exception {
		File root = Files.createTempDirectory("testfolder_").toFile();
		root.deleteOnExit();

		LocalRepository repository = new LocalRepository("test", root);

		testCreateEntries(repository, 100, 100, 100, 100, 10);
		LocalRepositoryFolderTest.purgeDirectory(root);
	}


	/**
	 * Loads the folder meanwhile refreshing it randomly and checks if expected entries are present.
	 */
	private void testLoadWithRefresh(final Folder folder, Integer expectedDataEntryCount, Integer expectedSubFolderCount,
									 String... expectedEntries) throws InterruptedException, ExecutionException {
		int threadCount = 50;
		startSignal = new CountDownLatch(1);

		List<Future<Integer>> getDataEntriesCalls = new ArrayList<>();
		List<Future<Integer>> getSubfolderEntriesCalls = new ArrayList<>();
		Map<String, Future<Boolean>> containsEntryCalls = new HashMap<>();

		ExecutorService executorService = Executors.newFixedThreadPool(threadCount * 2);

		for (int i = 0; i < threadCount; i++) {

			// load threads
			getDataEntriesCalls.add(executorService.submit(folder_getDataEntries(folder)));
			getSubfolderEntriesCalls.add(executorService.submit(folder_getSubfolders(folder)));

			// refresh threads
			if (random.nextDouble() < REFRESH_CALL_RATIO) {
				executorService.submit(folder_refresh(folder));
			}

			// check if expected entries present
			if (expectedEntries != null) {
				for (final String entryName : expectedEntries) {
					containsEntryCalls.put(entryName + " " + i,
							executorService.submit(folder_containsEntry(folder, entryName)));
				}
			}
		}

		startSignal.countDown();
		for (Future<Integer> test : getDataEntriesCalls) {
			Assert.assertEquals("data entry count mismatch", expectedDataEntryCount, test.get());
		}

		for (Entry<String, Future<Boolean>> entry : containsEntryCalls.entrySet()) {
			Assert.assertTrue("expected entry not found: " + entry.getKey(), entry.getValue().get());
		}

		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.SECONDS);
	}

	/**
	 * Creates a number of subfolders and data entries parallel meanwhile refreshing the folder
	 * randomly.
	 */
	private void testCreateEntries(final Repository repository, int folderCount, int processCount, int ioobjectCount,
								   int blobEntryCount, int connectionEntryCount) throws Exception {
		Folder connectionsFolder = RepositoryTools.getConnectionFolder(repository);
		int allOperations = folderCount + processCount + ioobjectCount + blobEntryCount;
		List<Callable<Void>> operations = new ArrayList<>(allOperations);

		// refresh
		for (int i = 0; i < allOperations * REFRESH_CALL_RATIO; i++) {
			operations.add(folder_refresh(repository));
		}
		// folders
		for (int i = 0; i < folderCount; i++) {
			operations.add(folder_createFolder(repository, FOLDER_NAME_PREFIX + i));
		}
		// processes
		for (int i = 0; i < processCount; i++) {
			operations.add(folder_createProcessEntry(repository, PROCESS_NAME_PREFIX + i));
		}
		// ioobjects
		for (int i = 0; i < ioobjectCount; i++) {
			operations.add(folder_createIOObjectEntry(repository, IOOBJECT_NAME_PREFIX + i, new TestIOObject()));
		}
		// blob entries
		for (int i = 0; i < blobEntryCount; i++) {
			operations.add(folder_createBlobEntry(repository, BLOBENTRY_NAME_PREFIX + i));
		}
		// connection entries
		for (int i = 0; i < connectionEntryCount; i++) {
			operations.add(folder_createConnectionEntry(connectionsFolder, CONNECTIONENTRY_NAME_PREFIX + i));
		}
		executeOperations(operations);

		try {
			// check subfolders, take Connections folder into account
			Assert.assertEquals("subfolder count mismatch", folderCount+1, repository.getSubfolders().size());
			// check data entries (ioobjects + processes + blobs)
			Assert.assertEquals("data entry count mismatch", ioobjectCount + processCount + blobEntryCount,
					repository.getDataEntries().size());
			// check processes
			for (int i = 0; i < processCount; i++) {
				String name = PROCESS_NAME_PREFIX + i;
				Assert.assertTrue(name + " not found", repository.containsEntry(name));
			}
			// check ioobjects
			for (int i = 0; i < processCount; i++) {
				String name = IOOBJECT_NAME_PREFIX + i;
				Assert.assertTrue(name + " not found", repository.containsEntry(name));
			}
			// check blob entries
			for (int i = 0; i < blobEntryCount; i++) {
				String name = BLOBENTRY_NAME_PREFIX + i;
				Assert.assertTrue(name + " not found", repository.containsEntry(name));
			}

			// check data entries in connections folder
			Assert.assertEquals("data entry count mismatch in connections folder", connectionEntryCount,
					connectionsFolder.getDataEntries().size());

			// gather all names of connection entries
			Map<String, ConnectionInformation> foundConnectionInformations = new HashMap<>();
			for (DataEntry dataEntry : connectionsFolder.getDataEntries()) {
				if (dataEntry instanceof ConnectionEntry) {
					foundConnectionInformations.put(dataEntry.getName(), ((ConnectionInformationContainerIOObject) ((ConnectionEntry) dataEntry).retrieveData(null)).getConnectionInformation());
				}
			}
			// check connection entries
			for (int i = 0; i < connectionEntryCount; i++) {
				String name = CONNECTIONENTRY_NAME_PREFIX + i;
				Assert.assertTrue(name + " not found", connectionsFolder.containsEntry(name));
				// read the content, was it stored like that?
				Assert.assertTrue(name + " not in the list of connection information entries", foundConnectionInformations.containsKey(name));
				final ConnectionInformation connectionInformation = foundConnectionInformations.get(name);
				Assert.assertNotNull(connectionInformation);
				Assert.assertNotNull(connectionInformation.getConfiguration());
				Assert.assertNotNull(connectionInformation.getLibraryFiles());
				connectionInformation.getLibraryFiles().forEach(path -> Assert.assertTrue(path.toString().contains("lib_" + name)));
				Assert.assertNotNull(connectionInformation.getOtherFiles());
				connectionInformation.getOtherFiles().forEach(path -> Assert.assertTrue(path.toString().contains("other_" + name)));
			}
		} catch (RepositoryException e) {
			Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
		}
	}

	private Callable<Void> folder_createConnectionEntry(Folder folder, String name) {
		return () -> {
			try {
				startSignal.await();
				Thread.sleep(random.nextInt(THREAD_WAIT_THRESHOLD));
				List<Path> libfiles = new ArrayList<>();
				Path tempLibFile = Files.createTempFile("lib_" + name, ".jar");
				tempLibFile.toFile().deleteOnExit();
				libfiles.add(tempLibFile);
				List<Path> otherFiles = new ArrayList<>();
				Path tempOtherFile = Files.createTempFile("other_" + name, ".cfg");
				tempOtherFile.toFile().deleteOnExit();
				otherFiles.add(tempOtherFile);
				ConnectionInformationBuilder connectionInformationBuilder = new ConnectionInformationBuilder(new ConnectionConfigurationImpl(name + " config", "test")).withLibraryFiles(libfiles).withOtherFiles(otherFiles);
				ConnectionInformation ci = connectionInformationBuilder.build();

				folder.createConnectionEntry(name, ci);
			} catch (RepositoryException | InterruptedException e) {
				e.printStackTrace();
				Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
			}
			return null;
		};
	}

	private List<Future<Void>> executeOperations(List<Callable<Void>> operations) throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
		Collections.shuffle(operations);

		startSignal = new CountDownLatch(1);
		List<Future<Void>> futures = new ArrayList<Future<Void>>(operations.size());
		for (Callable<Void> operation : operations) {
			futures.add(executorService.submit(operation));
		}
		startSignal.countDown();

		for (Future<Void> future : futures) {
			future.get();
		}

		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.SECONDS); // probably not needed

		return futures;
	}

	/**
	 * Returns a Callable that calls {@link Folder#refresh()}
	 */
	private Callable<Void> folder_refresh(final Folder folder) {
		return new Callable<Void>() {

			@Override
			public Void call() {
				try {
					startSignal.await();
					Thread.sleep(random.nextInt(THREAD_WAIT_THRESHOLD));
					// System.out.println(Thread.currentThread().getName() + " RERESH");
					folder.refresh();
				} catch (RepositoryException | InterruptedException e) {
					Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
				}
				return null;
			}
		};
	}

	/**
	 * Returns a Callable that calls {@link Folder#getSubfolders()}
	 */
	private Callable<Integer> folder_getSubfolders(final Folder folder) {
		return new Callable<Integer>() {

			@Override
			public Integer call() {
				try {
					startSignal.await();
					Thread.sleep(random.nextInt(THREAD_WAIT_THRESHOLD));
					// System.out.println(Thread.currentThread().getName() + " GET SUBFOLDERS");
					return folder.getSubfolders().size();
				} catch (RepositoryException | InterruptedException e) {
					Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
					return null;
				}
			}
		};
	}

	/**
	 * Returns a Callable that calls {@link Folder#getDataEntries()}
	 */
	private Callable<Integer> folder_getDataEntries(final Folder folder) {
		return new Callable<Integer>() {

			@Override
			public Integer call() {
				try {
					startSignal.await();
					Thread.sleep(random.nextInt(THREAD_WAIT_THRESHOLD));
					// System.out.println(Thread.currentThread().getName() + " GET DATA ENTRIES");
					return folder.getDataEntries().size();
				} catch (RepositoryException | InterruptedException e) {
					Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
					return -1;
				}
			}
		};
	}

	/**
	 * Returns a Callable that calls {@link Folder#createFolder(String)}
	 */
	private Callable<Void> folder_createFolder(final Folder folder, final String subFolderName) {
		return new Callable<Void>() {

			@Override
			public Void call() {
				try {
					startSignal.await();
					Thread.sleep(random.nextInt(THREAD_WAIT_THRESHOLD));
					// System.out.println(Thread.currentThread().getName() + " CREATE FOLDER " +
					// subFolderName);
					folder.createFolder(subFolderName);
				} catch (RepositoryException | InterruptedException e) {
					Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
				}
				return null;
			}
		};
	}

	/**
	 * Returns a Callable that calls {@link Folder#createBlobEntry(String)}
	 */
	private Callable<Void> folder_createBlobEntry(final Folder folder, final String blobEntryName) {
		return new Callable<Void>() {

			@Override
			public Void call() {
				try {
					startSignal.await();
					Thread.sleep(random.nextInt(THREAD_WAIT_THRESHOLD));
					// System.out.println(Thread.currentThread().getName() + " CREATE BLOB " +
					// blobEntryName);
					folder.createBlobEntry(blobEntryName);
				} catch (RepositoryException | InterruptedException e) {
					Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
				}
				return null;
			}
		};
	}

	/**
	 * Returns a Callable that calls {@link Folder#createProcessEntry(String, String)}
	 */
	private Callable<Void> folder_createProcessEntry(final Folder folder, final String processName) {
		return new Callable<Void>() {

			@Override
			public Void call() {
				try {
					startSignal.await();
					Thread.sleep(random.nextInt(THREAD_WAIT_THRESHOLD));
					// System.out.println(Thread.currentThread().getName() + " CREATE PROCESS " +
					// processName);
					folder.createProcessEntry(processName, TEST_PROCESS);
				} catch (RepositoryException | InterruptedException e) {
					Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
				}
				return null;
			}
		};
	}

	/**
	 * Returns a Callable that calls
	 * {@link Folder#createIOObjectEntry(String, IOObject, com.rapidminer.operator.Operator, com.rapidminer.tools.ProgressListener)}
	 */
	private Callable<Void> folder_createIOObjectEntry(final Folder folder, final String ioobjectName,
													  final IOObject ioobject) {
		return new Callable<Void>() {

			@Override
			public Void call() {
				try {
					startSignal.await();
					Thread.sleep(random.nextInt(THREAD_WAIT_THRESHOLD));
					// System.out.println(Thread.currentThread().getName() + " CREATE IOOBJECT " +
					// ioobjectName);
					folder.createIOObjectEntry(ioobjectName, ioobject, null, null);
				} catch (RepositoryException | InterruptedException e) {
					Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
				}
				return null;
			}
		};
	}

	/**
	 * Returns a Callable that calls {@link Folder#containsEntry(String)}
	 */
	private Callable<Boolean> folder_containsEntry(final Folder folder, final String entryName) {
		return new Callable<Boolean>() {

			@Override
			public Boolean call() {
				try {
					startSignal.await();
					Thread.sleep(random.nextInt(THREAD_WAIT_THRESHOLD));
					// System.out.println(Thread.currentThread().getName() + " CONTAINS " +
					// entryName);
					folder.containsEntry(entryName);
					return folder.containsEntry(entryName);
				} catch (InterruptedException | RepositoryException e) {
					Assert.fail(Thread.currentThread().getName() + " " + e.getMessage());
					return false;
				}
			}
		};
	}
}
