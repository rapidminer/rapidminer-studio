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
package com.rapidminer.repository.versioned;

import static com.rapidminer.repository.versioned.IOObjectFileTypeHandler.DATA_TABLE_FILE_ENDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.performance.AbsoluteError;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.repository.local.SimpleDataEntry;
import com.rapidminer.test_utils.RapidAssert;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * Tests reading and writing {@link ExampleSet}s into the file system repository and copying/moving to/from the legacy
 * repository.
 *
 * @author Gisa Meier
 * @since 9.7
 */
public class ExampleSetsInRepoTest {

	private static final Random RANDOM = new Random();

	private Path tempDirectoryNew;
	private Repository newTestRepository;
	private Path tempDirectoryLegacy;
	private Repository legacyTestRepository;


	@BeforeClass
	public static void setup() {
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.TEST);
		RepositoryManager.init();
		RapidMiner.initAsserters();
	}

	@Before
	public void createTestRepos() throws IOException, RepositoryException {
		tempDirectoryNew = Files.createTempDirectory(UUID.randomUUID().toString());
		FilesystemRepositoryFactory.createRepository("Test Local", tempDirectoryNew, EncryptionProvider.DEFAULT_CONTEXT);
		newTestRepository = RepositoryManager.getInstance(null).getRepository("Test Local");
		tempDirectoryLegacy = Files.createTempDirectory(UUID.randomUUID().toString());
		RepositoryManager.getInstance(null).addRepository(new LocalRepository("Test Local Old",
				tempDirectoryLegacy.toFile()));
		legacyTestRepository = RepositoryManager.getInstance(null).getRepository("Test Local Old");
	}

	@After
	public void deleteTestRepos() {
		RepositoryManager.getInstance(null).removeRepository(newTestRepository);
		FileUtils.deleteQuietly(tempDirectoryNew.toFile());
		RepositoryManager.getInstance(null).removeRepository(legacyTestRepository);
		FileUtils.deleteQuietly(tempDirectoryLegacy.toFile());
	}

	@Test
	public void testFileEnding() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		ExampleSet exampleSet = createDataSet(10, 20);
		ExampleSet exampleSet2 = createDataSet(100, 5);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstES", exampleSet, null, null);
		IOObjectEntry entry2 = myFolder.createIOObjectEntry("secondES", exampleSet2, null, null);
		assertEquals(DATA_TABLE_FILE_ENDING, ((BasicEntry) entry1).getSuffix());
		assertEquals(DATA_TABLE_FILE_ENDING, ((BasicEntry) entry2).getSuffix());
		myFolder.delete();
	}


	@Test
	public void testDeletion() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		ExampleSet exampleSet = createDataSet(10, 20);
		ExampleSet exampleSet2 = createDataSet(100, 5);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstES", exampleSet, null, null);
		IOObjectEntry entry2 = nestedFolder.createIOObjectEntry("secondES", exampleSet2, null, null);
		assertNotNull("myFolder was null", myFolder);
		assertNotNull("nestedFolder was null", nestedFolder);
		assertNotNull("entry1 was null", entry1);
		assertNotNull("entry2 was null", entry2);

		assertEquals(1, myFolder.getSubfolders().size());
		assertEquals(0, myFolder.getDataEntries().size());
		assertEquals(0, nestedFolder.getSubfolders().size());

		assertEquals(2, nestedFolder.getDataEntries().size());
		entry1.delete();
		assertEquals(1, nestedFolder.getDataEntries().size());

		nestedFolder.delete();
		assertEquals(0, myFolder.getSubfolders().size());

		// conn folder is always there, so we expect 2 now
		assertEquals(2, newTestRepository.getSubfolders().size());
		myFolder.delete();
		assertEquals(1, newTestRepository.getSubfolders().size());
	}

	@Test
	public void testCopy() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		ExampleSet exampleSet = createDataSet(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstES", exampleSet, null, null);
		RepositoryManager.getInstance(null).copy(entry1.getLocation(), myFolder, "secondES", null);
		assertTrue(myFolder.containsData("secondES", BasicExampleSetEntry.class));
		DataEntry secondEntry =
				myFolder.getDataEntries().stream().filter(d -> "secondES".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(exampleSet, data);
		myFolder.delete();
		assertEquals(1, newTestRepository.getSubfolders().size());
	}

	@Test
	public void testCopyToOld() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		ExampleSet exampleSet = createDataSet(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstES", exampleSet, null, null);

		Folder newFolder = legacyTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).copy(entry1.getLocation(), newFolder, "secondES", null);
		assertTrue(newFolder.containsData("secondES", BasicExampleSetEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "secondES".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(IOObjectEntry.IOO_SUFFIX, ((SimpleDataEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(exampleSet, data);
		myFolder.delete();
		newFolder.delete();
	}

	@Test
	public void testCopyFromOld() throws RepositoryException {
		Folder myFolder = legacyTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		ExampleSet exampleSet = createDataSet(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstES", exampleSet, null, null);

		Folder newFolder = newTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).copy(entry1.getLocation(), newFolder, "secondES", null);
		assertTrue(newFolder.containsData("secondES", BasicExampleSetEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "secondES".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(DATA_TABLE_FILE_ENDING, ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(exampleSet, data);
		myFolder.delete();
		newFolder.delete();
	}

	@Test
	public void testMove() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		ExampleSet exampleSet = createDataSet(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstES", exampleSet, null, null);
		RepositoryManager.getInstance(null).move(entry1.getLocation(), myFolder, "secondES", null);
		assertTrue(myFolder.containsData("secondES", BasicExampleSetEntry.class));
		DataEntry secondEntry =
				myFolder.getDataEntries().stream().filter(d -> "secondES".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(exampleSet, data);
		myFolder.delete();
		assertEquals(1, newTestRepository.getSubfolders().size());
	}

	@Test
	public void testMoveToOld() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		ExampleSet exampleSet = createDataSet(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstES", exampleSet, null, null);

		Folder newFolder = legacyTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).move(entry1.getLocation(), newFolder, "secondES", null);
		assertTrue(newFolder.containsData("secondES", BasicExampleSetEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "secondES".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(IOObjectEntry.IOO_SUFFIX, ((SimpleDataEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(exampleSet, data);
		myFolder.delete();
		newFolder.delete();
	}

	@Test
	public void testMoveFromOld() throws RepositoryException {
		Folder myFolder = legacyTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		ExampleSet exampleSet = createDataSet(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstES", exampleSet, null, null);

		Folder newFolder = newTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).move(entry1.getLocation(), newFolder, "secondES", null);
		assertTrue(newFolder.containsData("secondES", BasicExampleSetEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "secondES".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(DATA_TABLE_FILE_ENDING, ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(exampleSet, data);
		myFolder.delete();
		newFolder.delete();
	}

	@Test
	public void testOverwriteWithIOO() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		ExampleSet exampleSet = createDataSet(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstES", exampleSet, null, null);

		IOObject dummy = new AbsoluteError();
		RepositoryManager.getInstance(null).store(dummy, entry1.getLocation(), null, null);

		DataEntry secondEntry =
				nestedFolder.getDataEntries().stream().filter(d -> "firstES".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(IOObjectEntry.IOO_SUFFIX.replace(".",""), ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(dummy, data);
		myFolder.delete();
		nestedFolder.delete();
	}

	@Test
	public void testOverwriteIOO() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		IOObject dummy = new AbsoluteError();
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", dummy, null, null);

		ExampleSet exampleSet = createDataSet(10, 20);
		RepositoryManager.getInstance(null).store(exampleSet, entry1.getLocation(), null, null);

		DataEntry secondEntry =
				nestedFolder.getDataEntries().stream().filter(d -> "first".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(DATA_TABLE_FILE_ENDING, ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(exampleSet, data);
		myFolder.delete();
		nestedFolder.delete();
	}

	private static ExampleSet createDataSet(int rows, int columns) {
		int shift = RANDOM.nextInt(10);
		IntUnaryOperator indexToOntology = i -> {
			switch ((i + shift) % 10) {
				case 0:
				case 8:
					return Ontology.REAL;
				case 1:
					return Ontology.NOMINAL;
				case 2:
					return Ontology.DATE_TIME;
				case 3:
					return Ontology.INTEGER;
				case 4:
					return Ontology.BINOMINAL;
				case 5:
					return Ontology.DATE;
				case 7:
					return Ontology.STRING;
				case 6:
				case 9:
				default:
					return Ontology.NUMERICAL;
			}
		};
		BiFunction<Integer, Attribute, IntToDoubleFunction> indexToFiller = (j, att) -> {
			switch ((j + shift) % 10) {
				case 0:
				case 8:
					return i -> RANDOM.nextDouble();
				case 1:
					return i -> att.getMapping().mapString("value" + RANDOM.nextInt(15));
				case 2:
					return i -> (double) (1573554664271L + RANDOM.nextInt() * 10001);
				case 3:
					return i -> Math.round(RANDOM.nextDouble() * 10000);
				case 4:
					return i -> RANDOM.nextDouble() > 0.7 ? Double.NaN :
							att.getMapping().mapString("val" + RANDOM.nextInt(2));
				case 5:
					return i -> (1573735477L + RANDOM.nextInt(1000000)) / 1000 * 1000;
				case 7:
					return i -> att.getMapping().mapString("some longer longer value" + RANDOM.nextInt(1000));
				case 6:
				case 9:
				default:
					return i -> RANDOM.nextDouble() * 1000;
			}
		};

		List<Attribute> attributes = IntStream.range(0, columns)
				.mapToObj(i -> AttributeFactory.createAttribute("att-" + i, indexToOntology.applyAsInt(i))).collect(Collectors.toList());
		ExampleSetBuilder builder = ExampleSets.from(attributes).withBlankSize(rows);
		for (int i = 0; i < attributes.size(); i++) {
			Attribute attribute = attributes.get(i);
			builder.withColumnFiller(attribute, indexToFiller.apply(i, attribute));
		}
		ExampleSet build = builder.build();
		build.recalculateAllAttributeStatistics();
		return build;
	}
}
