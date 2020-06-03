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
package com.rapidminer.storage.hdf5;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.AttributeSelectionExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MDReal;
import com.rapidminer.operator.ports.metadata.MetaDataFactory;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.container.Range;


/**
 * Tests the meta data-only file format.
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public class WriteMDOnlyTest {

	private static ExampleSet exSet;
	private static ExampleSet exSetMod;
	private static ExampleSet exSetTooManyAtts;

	@BeforeClass
	public static void setup() {
		Map<Attribute, IntToDoubleFunction> attributes = new LinkedHashMap<>();
		RandomGenerator rng = RandomGenerator.getGlobalRandomGenerator();
		attributes.put(AttributeFactory.createAttribute("integer", Ontology.INTEGER), i -> rng.nextInt());
		attributes.put(AttributeFactory.createAttribute("real", Ontology.REAL), i -> rng.nextDouble());
		attributes.put(AttributeFactory.createAttribute("time", Ontology.TIME), i -> rng.nextInt(86400000));
		attributes.put(AttributeFactory.createAttribute("date", Ontology.DATE), i -> rng.nextInt() * 86400000L);
		attributes.put(AttributeFactory.createAttribute("date time", Ontology.DATE_TIME), i -> rng.nextLongInRange(-1L<<50, 1L<<50));
		Attribute smallNominal = AttributeFactory.createAttribute("small nominal", Ontology.NOMINAL);
		smallNominal.getMapping().mapString("positive");
		smallNominal.getMapping().mapString("negative");
		attributes.put(smallNominal, i -> rng.nextInt(smallNominal.getMapping().size()));
		Attribute nominal = AttributeFactory.createAttribute("nominal", Ontology.NOMINAL);
		for (int i = 0; i < 10; i++) {
			nominal.getMapping().mapString("value " + i);
		}
		attributes.put(nominal, i -> rng.nextInt(nominal.getMapping().size()));

		int maxNomValues = AttributeMetaData.getMaximumNumberOfNominalValues();
		int nValues = 2 * maxNomValues;
		int[] modeOffsets = {-maxNomValues/2, -1, 0, 1, 2, maxNomValues/2};
		for (int offset : modeOffsets) {
			Attribute tooManyNominal = AttributeFactory.createAttribute("too many nominal (offset " + offset + ")", Ontology.NOMINAL);
			for (int i = 0; i < nValues; i++) {
				tooManyNominal.getMapping().mapString("value " + i);
			}
			int mode = maxNomValues + offset - 1;
			attributes.put(tooManyNominal, i -> i < 100 ? mode : rng.nextInt(tooManyNominal.getMapping().size()));

		}

		Attribute tooLongNominal = AttributeFactory.createAttribute("too long nominal", Ontology.NOMINAL);
		tooLongNominal.getMapping().mapString("positive");
		tooLongNominal.getMapping().mapString("negative");
		tooLongNominal.getMapping().mapString(StringUtils.repeat("toolong", 100 / 7 + 1));
		attributes.put(tooLongNominal, i -> rng.nextInt(tooLongNominal.getMapping().size()));

		ExampleSetBuilder builder = ExampleSets.from(new ArrayList<>(attributes.keySet())).withBlankSize(1000);
		attributes.forEach(builder::withColumnFiller);

		exSet = builder.build();
		exSet.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, "test");

		boolean[] mask = new boolean[exSet.getAttributes().size()];
		for (int i = 0; i < mask.length - 1; i++) {
			mask[i] = true;
		}
		exSetMod = AttributeSelectionExampleSet.create((ExampleSet) exSet.clone(), mask);

		exSetTooManyAtts = ExampleSets.from(IntStream.range(0, ExampleSetMetaData.getMaximumNumberOfAttributes() + 10)
				.mapToObj(i -> AttributeFactory.createAttribute("att-" + i, Ontology.REAL))
				.collect(Collectors.toList())).withBlankSize(2000).build();
	}

	@Test
	public void testExSetAndMD() throws IOException {
		compare(exSet, false);
	}

	@Test
	public void testExSetAndMDNoLongNominals() throws IOException {
		compare(exSetMod, true);
	}

	@Test
	public void testExSetAndMDTooManyAttributes() throws IOException {
		compare(exSetTooManyAtts, true);
	}

	@Test
	public void testMD() throws IOException {
		compareMD(exSet, false);
	}

	@Test
	public void testMDNoLongNominals() throws IOException {
		compareMD(exSetMod, false);
		compareMD(exSetMod, true);
	}

	@Test
	public void testMDTooManyAtts() throws IOException {
		compareMD(exSetTooManyAtts, false);
		compareMD(exSetTooManyAtts, true);
	}

	@Test
	public void testExampleCount() throws IOException {
		ExampleSetMetaData md = new ExampleSetMetaData();
		AttributeMetaData amd = new AttributeMetaData("attribute", null, Ontology.REAL, new Range(-10, 10));
		amd.setNumberOfMissingValues(new MDInteger(4));
		amd.setMean(new MDReal(4d));
		md.addAttribute(amd);

		// add unknown amount ("at least" case)
		ExampleSetMetaData testMD = md.clone();
		testMD.getNumberOfExamples().increaseByUnknownAmount();
		compare(testMD);

		// now subtract unknown amount ("unknown" case)
		testMD.getNumberOfExamples().reduceByUnknownAmount();
		compare(testMD);

		// new copy and only subtract unknown amount ("at most" case)
		testMD = md.clone();
		md.getNumberOfExamples().reduceByUnknownAmount();
		compare(testMD);
	}

	@AfterClass
	public static void cleanup() {
		exSet = exSetMod = exSetTooManyAtts = null;
	}

	/**
	 * Writes both the example set and its metadata to hdf5 in the metadata-only format, then loads them
	 * as {@link ExampleSetMetaData} and compares these two (via Java serialization).
	 */
	private void compare(ExampleSet exSet, boolean shortenMD) throws IOException {
		exSet.recalculateAllAttributeStatistics();
		ExampleSetMetaData md = (ExampleSetMetaData) MetaDataFactory.getInstance().createMetaDataforIOObject(exSet, shortenMD);
		File a = File.createTempFile("hdf md a", ".hdf5");
		File b = File.createTempFile("hdf md b", ".hdf5");
		a.deleteOnExit();
		b.deleteOnExit();
		new ExampleSetHdf5Writer(exSet, true, true).write(a.toPath());
		new ExampleSetHdf5Writer(md, true).write(b.toPath());
		ExampleSetMetaData mdA = Hdf5ExampleSetReader.readMetaData(a.toPath());
		ExampleSetMetaData mdB = Hdf5ExampleSetReader.readMetaData(b.toPath());
		File mdAFile = File.createTempFile("md a", ".md");
		File mdBFile = File.createTempFile("md b", ".md");
		mdAFile.deleteOnExit();
		mdBFile.deleteOnExit();
		try (ObjectOutputStream mdAOut = new ObjectOutputStream(new FileOutputStream(mdAFile));
			 ObjectOutputStream mdBOut = new ObjectOutputStream(new FileOutputStream(mdBFile))) {
			mdAOut.writeObject(mdA);
			mdBOut.writeObject(mdB);
		}

		assertTrue(FileUtils.contentEquals(mdAFile, mdBFile));
	}

	/**
	 * Creates meta data from the given exampleset, writes it to hdf metadata-only format, reads it as metadata
	 * and compares it to the original (via Java serialization for each attribute and on some other fields).
	 */
	private void compareMD(ExampleSet exSet, boolean shortenMD) throws IOException {
		exSet.recalculateAllAttributeStatistics();
		ExampleSetMetaData md = (ExampleSetMetaData) MetaDataFactory.getInstance().createMetaDataforIOObject(exSet, shortenMD);
		compare(md);
	}

	private void compare(ExampleSetMetaData md) throws IOException {
		File hdf = File.createTempFile("hdf md", ".hdf");
		hdf.deleteOnExit();
		new ExampleSetHdf5Writer(md, false).write(hdf.toPath());
		ExampleSetMetaData readMD = Hdf5ExampleSetReader.readMetaData(hdf.toPath());
		assertEquals(md.getAttributeSetRelation(), readMD.getAttributeSetRelation());
		assertEquals(md.getNumberOfExamples().getNumber(), readMD.getNumberOfExamples().getNumber());
		assertEquals(md.getNumberOfExamples().getRelation(), readMD.getNumberOfExamples().getRelation());
		assertEquals(md.getAnnotations(), readMD.getAnnotations());

		for (AttributeMetaData amdO : md.getAllAttributes()) {
			AttributeMetaData amdRead = readMD.getAttributeByName(amdO.getName());
			boolean readShrunk = amdRead.valueSetWasShrunk();
			boolean shrunk = amdO.valueSetWasShrunk();
			amdO = amdO.clone();
			amdO.valueSetIsShrunk(shrunk);
			amdRead = amdRead.clone();
			amdRead.valueSetIsShrunk(readShrunk);
			File mdAFile = File.createTempFile("amd a", ".md");
			File mdBFile = File.createTempFile("amd b", ".md");
			mdAFile.deleteOnExit();
			mdBFile.deleteOnExit();
			try (ObjectOutputStream mdAOut = new ObjectOutputStream(new FileOutputStream(mdAFile));
				 ObjectOutputStream mdBOut = new ObjectOutputStream(new FileOutputStream(mdBFile))) {
				mdAOut.writeObject(amdO);
				mdBOut.writeObject(amdRead);
			}
			assertTrue("attribute " + amdO.getName() + " not equal", FileUtils.contentEquals(mdAFile, mdBFile));

		}
	}

}
