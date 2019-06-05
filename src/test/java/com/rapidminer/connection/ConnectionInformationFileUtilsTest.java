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
package com.rapidminer.connection;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.connection.configuration.ConnectionConfigurationImpl;
import com.rapidminer.connection.configuration.ConnectionResources;
import com.rapidminer.tools.FileSystemService;


/**
 * @author Andreas Timm
 * @since 9.3
 */
public class ConnectionInformationFileUtilsTest {

	private static List<Path> tmpFiles = new ArrayList<>();

	@BeforeClass
	public static void init() {

	}

	@AfterClass
	public static void teardown() {
		tmpFiles.forEach(f -> {
			try {
				Path cacheLocation = getCacheLocation();
				if (f.startsWith(cacheLocation)) {
					while (!f.equals(cacheLocation)) {
						File file = f.toFile();
						FileDeleteStrategy.FORCE.delete(file);
						f = f.getParent();
					}
				} else {
					// delete actual file
					File file = f.toFile();
					FileDeleteStrategy.FORCE.delete(file);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}


	@Test
	public void getOrCreateCacheFiles() throws IOException {
		final List<Path> cacheFiles = ConnectionInformationFileUtils.getOrCreateCacheFiles(ConnectionResources.RESOURCE_PATH);
		assertEquals(ConnectionResources.RESOURCE_PATH.toFile()
				.listFiles(pathname -> !pathname.toPath().endsWith(ConnectionInformationSerializer.MD5_SUFFIX)).length,
				cacheFiles.size());
		tmpFiles.addAll(cacheFiles);
	}

	@Test
	public void save() {
		// case 1 save(null, null)
		try {
			ConnectionInformationFileUtils.save(null, null);
			fail("Saving null as ConnectionInformation to file null was not supposed to work");
		} catch (IOException ioe) {
			assertEquals("Unexpected message received from the IOException", "Target file was not set", ioe.getMessage());
		}

	}

	@Test
	public void saveNullToFile() {
		// case 2 save(null, tmpFile)
		File tmpFile = null;
		try {
			tmpFile = createTempfile();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			fail(ioe.getMessage());
		}
		assertTrue(tmpFile.exists());
		assertTrue(tmpFile.canWrite());
		assertTrue(tmpFile.delete());
		try {
			ConnectionInformationFileUtils.save(null, tmpFile.toPath());
			fail("Saving null as ConnectionInformation was not supposed to work");
		} catch (IOException ioe) {
			assertEquals("Unexpected message received from the IOException", "Object connection information is null", ioe.getMessage());
		}

	}

	@Test
	public void saveToFile() throws IOException {
		// case 3 save(ci, tmpFile)
		ConnectionInformation ci = new ConnectionInformationBuilder(new ConnectionConfigurationImpl("test name 1", "test type")).build();
		assertNotNull(ci);
		final Path zipFile = createTempfile().toPath();
		try {
			ConnectionInformationFileUtils.save(ci, zipFile);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			fail(ioe.getMessage());
		}
		try {
			ConnectionInformation reloadedCI = ConnectionInformationFileUtils.loadFromZipFile(zipFile);
			assertEquals("The loaded Connection Information is not equal to the created one!", ci, reloadedCI);
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void saveWithLibfilesToFile() throws IOException, URISyntaxException {
		// case 4 save(ci with libfiles, tmpFile)
		List<Path> libfiles = new ArrayList<>();
		libfiles.add(new File(getClass().getResource("empty.jar").toURI()).toPath());
		ConnectionInformation ci = new ConnectionInformationBuilder(new ConnectionConfigurationImpl("test name 1", "test type")).withLibraryFiles(libfiles).build();
		assertNotNull(ci);
		final Path tmpFile = createTempfile().toPath();
		try {
			ConnectionInformationFileUtils.save(ci, tmpFile);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			fail(ioe.getMessage());
		}
		try {
			ConnectionInformation reloadedCI = ConnectionInformationFileUtils.loadFromZipFile(tmpFile);
			assertEquals("The loaded Connection Information is not equal to the created one!", ci.getConfiguration(), reloadedCI.getConfiguration());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void saveToNull() {
		// case 5 save(ci, null)
		try {
			ConnectionInformation ci = new ConnectionInformationBuilder(new ConnectionConfigurationImpl("test name 1", "test type")).build();
			ConnectionInformationFileUtils.save(ci, null);
			fail("Saving a ConnectionInformation to a null file was not supposed to work");
		} catch (IOException ioe) {
			assertEquals("Unexpected message received from the IOException", "Target file was not set", ioe.getMessage());
		}

	}

	@Test
	public void saveCorruptCI() {
		// case 6 save(corrupt-ci, tmpfile)
		try {
			ArrayList<Path> libfiles = new ArrayList<>();
			libfiles.add(ConnectionResources.RESOURCE_PATH.resolve("missing.file"));
			ConnectionInformation ci = new ConnectionInformationBuilder(new ConnectionConfigurationImpl("test name 1", "test type")).withLibraryFiles(libfiles).build();
			fail("Creating a ConnectionInformation with missing library files was not supposed to work");
		} catch (IllegalArgumentException iae) {
			assertTrue(iae.getMessage().startsWith("Non-existing paths found"));
			assertTrue(iae.getMessage().contains("missing.file"));
		}
	}

	@Test
	public void moveTo() throws IOException {
		final Path source = createTempfile().toPath();

		try (FileWriter writer = new FileWriter(source.toFile())) {
			IOUtils.copy(ConnectionResources.ENCODING_TEST_RESOURCE.openStream(), writer, StandardCharsets.UTF_8);
		}
		Path target = createTempfile().toPath();
		ConnectionInformationFileUtils.moveTo(source, target);
		assertTrue("Source file should not exist anymore after moving it but still exists: " + source.toFile().getAbsolutePath(), !source.toFile().exists());
		assertTrue("Target file does not exist after move operation but should: " + target.toFile().getAbsolutePath(), target.toFile().exists());

		final String sourceString = IOUtils.toString(ConnectionResources.ENCODING_TEST_RESOURCE, StandardCharsets.UTF_8);
		final String movedString;
		try (final FileInputStream input = new FileInputStream(target.toFile())) {
			movedString = IOUtils.toString(input, StandardCharsets.UTF_8);
		}

		assertEquals(sourceString, movedString);
	}

	@Test
	public void copyTo() throws IOException {
		File original = createTempfile();
		File target = createTempfile();
		assertTrue(original.exists());
		assertTrue(target.delete());
		ConnectionInformationFileUtils.copyTo(original.toPath(), target.toPath());
		assertTrue("The source file to copy vanished!! Original location: " + original.getAbsolutePath(), original.exists());
		assertTrue("Did not properly copy the file to the target location " + target.getAbsolutePath(), target.exists());
	}

	@Test
	public void copyFilesToZipNullNullNull() {
		try {
			ConnectionInformationFileUtils.copyFilesToZip(null, null, null);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void copyFilesToZipTempNullNull() throws IOException {
		ConnectionInformationFileUtils.copyFilesToZip(createTempDirectory(), null, null);
		ConnectionInformationFileUtils.copyFilesToZip(createTempDirectory(), new ArrayList<>(), null);
	}

	@Test
	public void copyFilesToZipTempFiles() throws IOException {
		List<Path> files = new ArrayList<>();
		for (File file : Objects.requireNonNull(ConnectionResources.RESOURCE_PATH.toFile().listFiles())) {
			if (file.isFile()) {
				files.add(file.toPath());
			}
		}
		final Path tempDirectory = createTempDirectory();
		ConnectionInformationFileUtils.copyFilesToZip(tempDirectory, files, null);
		try {
			ConnectionInformationFileUtils.copyFilesToZip(tempDirectory, files, "");
		} catch (IOException ioe) {
			assertEquals(tempDirectory.toAbsolutePath().toString(), ioe.getMessage());
		}
		final String newDirName = "new";
		final Path newDir = tempDirectory.resolve(newDirName);
		tmpFiles.add(newDir);
		ConnectionInformationFileUtils.copyFilesToZip(tempDirectory, files, newDirName);
		// there will be an md5 entry for every copied file
		final File[] newDirFiles = newDir.toFile().listFiles();
		assertEquals(files.size() * 2, Objects.requireNonNull(newDirFiles).length);
		Set<String> newDirFilenames = Arrays.stream(newDirFiles).map(File::getName).collect(Collectors.toSet());
		for (Path file : files) {
			assertTrue(newDirFilenames.contains(file.toFile().getName()));
		}
	}

	@Test
	public void addFileInternally() throws IOException {
		String name = "conninfo-test";
		final Path subdirInCache = getCacheLocation().resolve(name);
		tmpFiles.add(subdirInCache);
		ConnectionInformationFileUtils.addFileInternally(name, ConnectionResources.EMPTY_JAR.openStream(), null);

		assertEquals(1, Objects.requireNonNull(subdirInCache.toFile().listFiles()).length);
	}

	@Test
	public void addFileInternallyWithWrongMD5() {
		String name = "conninfo-test";
		final Path subdirInCache = getCacheLocation().resolve(name);
		tmpFiles.add(subdirInCache);
		try {
			ConnectionInformationFileUtils.addFileInternally(name, ConnectionResources.EMPTY_JAR.openStream(), "");
			fail("File was added despite wrong md5 hash");
		} catch (IOException e) {

		}
		assertFalse("Sub directory was falsely created", Files.exists(subdirInCache));
	}

	private File createTempfile() throws IOException {
		final File tempFile = File.createTempFile("file", "zip");
		tmpFiles.add(tempFile.toPath());
		return tempFile;
	}

	private static Path getCacheLocation() {
		return FileSystemService.getUserRapidMinerDir().toPath().resolve(FileSystemService.RAPIDMINER_INTERNAL_CACHE_CONNECTION_FULL);
	}

	private Path createTempDirectory() throws IOException {
		final Path tempDirectory = Files.createTempDirectory("cifu-temp");
		tmpFiles.add(tempDirectory);
		return tempDirectory;
	}
}
