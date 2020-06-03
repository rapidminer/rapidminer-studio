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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.hdf5.CustomDataOutput;
import com.rapidminer.hdf5.file.ColumnInfo;
import com.rapidminer.hdf5.file.NumericColumnInfo;
import com.rapidminer.hdf5.file.StringColumnInfo;
import com.rapidminer.hdf5.file.TableWriter;
import com.rapidminer.hdf5.message.data.DataType;
import com.rapidminer.hdf5.message.data.DefaultDataType;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.test_utils.RapidAssert;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.RandomGenerator;


/**
 * Tests the different possibilities to write example sets using the {@link TableWriter} and read them with the
 * {@link Hdf5ExampleSetReader}.
 *
 * @author Gisa Meier
 */
@RunWith(Enclosed.class)
public class WriteReadTest {

	private static final RandomGenerator rng = new RandomGenerator(RandomGenerator.DEFAULT_SEED);

	@RunWith(Parameterized.class)
	public static class ReadWrittenByWriter {

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"real and int", createExampleSetNum(50, 10000, true)});
			sets.add(new Object[]{"datetime", createExampleSetDatetime(50, 1000, true)});
			sets.add(new Object[]{"long utf8 values", createDataSet(1000, 100, 5)});
			sets.add(new Object[]{"binominal", createExampleSetNom(5, 100, 2, true, true, false)});
			sets.add(new Object[]{"few varlength values", createExampleSetNom(5, 100, 5, false, true, false)});
			sets.add(new Object[]{"few fixlength values", createExampleSetNom(5, 100, 5, true, true, false)});
			sets.add(new Object[]{"many varlength values", createExampleSetNom(5, 300, 300, false, true, false)});
			sets.add(new Object[]{"many fixlength values", createExampleSetNom(5, 300, 300, true, true, false)});
			sets.add(new Object[]{"many varlength values ending on null", createExampleSetNom(5, 300, 300, false,
					false, true)});
			sets.add(new Object[]{"all types", createAllTypes()});
			sets.add(new Object[]{"all roles", createDifferentRoles()});
			return sets;
		}

		@Test
		public void testWriteAndRead() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new ExampleSetHdf5Writer(set).write(f.toPath());
			ExampleSet read = Hdf5ExampleSetReader.read(f.toPath());
			RapidAssert.assertEquals(set, read);
			assertEquals(set.getAnnotations(), read.getAnnotations());
		}

		@Test
		public void testWriteAndReadMD() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new ExampleSetHdf5Writer(set).write(f.toPath());
			ExampleSetMetaData read = Hdf5ExampleSetReader.readMetaData(f.toPath());
			ExampleSetMetaData expected = new ExampleSetMetaData(set);
			assertEquals(expected.getNumberOfExamples().getNumber(), read.getNumberOfExamples().getNumber());
			Iterator<AttributeMetaData> readIterator = read.getAllAttributes().iterator();
			Iterator<AttributeMetaData> expectedIterator = expected.getAllAttributes().iterator();
			while(expectedIterator.hasNext()){
				AttributeMetaData readAtt = readIterator.next();
				AttributeMetaData expectedAtt = expectedIterator.next();
				// if min max statistics is not calculated, it is this, but it is stored in studio (bug?)
				if (expectedAtt.getValueRange().getLower() == Double.POSITIVE_INFINITY
						&& expectedAtt.getValueRange().getUpper() == Double.NEGATIVE_INFINITY) {
					assertEquals(expectedAtt.getValueType(), readAtt.getValueType());
					assertEquals(expectedAtt.getNumberOfMissingValues().getNumber(),
							readAtt.getNumberOfMissingValues().getNumber());
					assertTrue(readAtt.getValueSetRelation().equals(SetRelation.EQUAL)
							|| readAtt.getValueSetRelation().equals(SetRelation.UNKNOWN));
					assertEquals(new AttributeMetaData("", 0).getValueRange(), readAtt.getValueRange());
					assertEquals(new AttributeMetaData("", 0).getMean().getNumber(), readAtt.getMean().getNumber());
				} else {
					assertEquals(expectedAtt.getDescription(), readAtt.getDescription());
				}
				assertEquals(expectedAtt.getValueSet(), readAtt.getValueSet());
			}
		}

		@Test
		public void testReadingNonExistentMD() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new ExampleSetHdf5Writer(set).write(f.toPath(), false);
			ExampleSetMetaData read = Hdf5ExampleSetReader.readMetaData(f.toPath());
			assertNull(read);
		}

	}

	@RunWith(Parameterized.class)
	public static class ReadStringRawFormats {

		public class RawStringsWriter extends ExampleSetHdf5Writer {

			private final ExampleSet exampleSet;

			public RawStringsWriter(ExampleSet exampleSet) {
				super(exampleSet);
				this.exampleSet = exampleSet;
			}

			public void writeRaw(Path f) throws IOException {
				Attributes attributes = exampleSet.getAttributes();
				int allAttributeCount = attributes.allSize();
				int rowCount = exampleSet.size();
				Iterator<AttributeRole> iterator = attributes.allAttributeRoles();
				ColumnInfo[] columnInfos = new ColumnInfo[allAttributeCount];
				for (int i = 0; i < columnInfos.length; i++) {
					AttributeRole next = iterator.next();
					Attribute attribute = next.getAttribute();
					NominalMapping mapping = attribute.getMapping();
					StringColumnInfo stringColumnInfo = new StringColumnInfo(attribute.getName(),
							ColumnInfo.ColumnType.NOMINAL, null,
							mapping.getValues(), v -> mapping.getIndex(v) >= 0, ColumnInfo.StorageType.STRING_RAW,
							i % 2 == 0);
					stringColumnInfo.addAdditionalAttribute(ATTRIBUTE_LEGACY_TYPE, byte.class,
							(byte) attribute.getValueType());
					columnInfos[i] = stringColumnInfo;
				}
				write(columnInfos, exampleSet.getAnnotations(), rowCount, null, f);
			}
		}

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"long utf8 values", createDataSet(1000, 100, 5)});
			sets.add(new Object[]{"binominal", createExampleSetNom(5, 100, 2, true, true, false)});
			sets.add(new Object[]{"few varlength values", createExampleSetNom(5, 100, 5, false, true, false)});
			sets.add(new Object[]{"few fixlength values", createExampleSetNom(5, 100, 5, true, true, false)});
			sets.add(new Object[]{"many varlength values", createExampleSetNom(5, 300, 300, false, true, false)});
			sets.add(new Object[]{"many fixlength values", createExampleSetNom(5, 300, 300, true, true, false)});
			return sets;
		}


		@Test
		public void testReadRawStringColumns() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new RawStringsWriter(set).writeRaw(f.toPath());
			ExampleSet read = Hdf5ExampleSetReader.read(f.toPath());
			RapidAssert.assertEquals(set, read);
			assertEquals(set.getAnnotations(), read.getAnnotations());
		}
	}


	@RunWith(Parameterized.class)
	public static class ReadIntCategoricalFormats {

		public class IntCategoriesWriter extends ExampleSetHdf5Writer {

			private final ExampleSet exampleSet;

			public IntCategoriesWriter(ExampleSet exampleSet) {
				super(exampleSet);
				this.exampleSet = exampleSet;
			}

			public void writeInts(Path f) throws IOException {
				Attributes attributes = exampleSet.getAttributes();
				int allAttributeCount = attributes.allSize();
				int rowCount = exampleSet.size();
				Iterator<AttributeRole> iterator = attributes.allAttributeRoles();
				ColumnInfo[] columnInfos = new ColumnInfo[allAttributeCount];
				for (int i = 0; i < columnInfos.length; i++) {
					AttributeRole next = iterator.next();
					Attribute attribute = next.getAttribute();
					NominalMapping mapping = attribute.getMapping();
					StringColumnInfo stringColumnInfo = new StringColumnInfo(attribute.getName(),
							ColumnInfo.ColumnType.NOMINAL, null,
							mapping.getValues(), v -> mapping.getIndex(v) >= 0,
							ColumnInfo.StorageType.STRING_DICTIONARY,
							false) {

						@Override
						public DataType getDataType() {
							return DefaultDataType.FIXED32;
						}
					};
					stringColumnInfo.addAdditionalAttribute(ATTRIBUTE_LEGACY_TYPE, byte.class,
							(byte) attribute.getValueType());
					columnInfos[i] = stringColumnInfo;
				}
				write(columnInfos, exampleSet.getAnnotations(), rowCount, null, f);
			}
		}


		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"long utf8 values", createDataSet(1000, 100, 5)});
			sets.add(new Object[]{"binominal", createExampleSetNom(5, 100, 2, true, true, false)});
			sets.add(new Object[]{"few varlength values", createExampleSetNom(5, 100, 5, false, true, false)});
			sets.add(new Object[]{"few fixlength values", createExampleSetNom(5, 100, 5, true, true, false)});
			sets.add(new Object[]{"many varlength values", createExampleSetNom(5, 300, 300, false, true, false)});
			sets.add(new Object[]{"many fixlength values", createExampleSetNom(5, 300, 300, true, true, false)});
			return sets;
		}


		@Test
		public void testReadCategoricalColumns() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new IntCategoriesWriter(set).writeInts(f.toPath());
			ExampleSet read = Hdf5ExampleSetReader.read(f.toPath());
			RapidAssert.assertEquals(set, read);
			assertEquals(set.getAnnotations(), read.getAnnotations());
		}
	}

	@RunWith(Parameterized.class)
	public static class ReadFloatFormats {

		public class FloatWriter extends ExampleSetHdf5Writer {

			private final ExampleSet exampleSet;

			public FloatWriter(ExampleSet exampleSet) {
				super(exampleSet);
				this.exampleSet = exampleSet;
			}

			public void writeFloats(Path f) throws IOException {
				Attributes attributes = exampleSet.getAttributes();
				int allAttributeCount = attributes.allSize();
				int rowCount = exampleSet.size();
				Iterator<AttributeRole> iterator = attributes.allAttributeRoles();
				ColumnInfo[] columnInfos = new ColumnInfo[allAttributeCount];
				for (int i = 0; i < columnInfos.length; i++) {
					AttributeRole next = iterator.next();
					Attribute attribute = next.getAttribute();
					NumericColumnInfo columnInfo = new NumericColumnInfo(attribute.getName(),
							attribute.getValueType() == Ontology.INTEGER ? ColumnInfo.ColumnType.INTEGER :
									ColumnInfo.ColumnType.REAL, null) {

						@Override
						public DataType getDataType() {
							//create float type
							return DataType.createGeneric(DataType.FLOAT_TYPE, new byte[]{0x20, 0x1f, 0x00}, 4,
									new byte[]{0x00, 0x00, 0x20, 0x00, 0x17, 0x08, 0x00, 0x17, 0x7f, 0x00, 0x00,
											0x00});
						}
					};

					columnInfo.addAdditionalAttribute(ATTRIBUTE_LEGACY_TYPE, byte.class,
							(byte) attribute.getValueType());
					columnInfos[i] = columnInfo;
				}
				write(columnInfos, exampleSet.getAnnotations(), rowCount, null, f);
			}

			@Override
			public void writeDoubleData(ColumnInfo columnInfo, CustomDataOutput channel) throws IOException {
				Attribute att = exampleSet.getAttributes().get(columnInfo.getName());
				for (Example example : exampleSet) {
					channel.writeFloat((float) example.getValue(att));
				}
			}
		}

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"real and int", createExampleSetNumFloat(50, 10000, true)});
			return sets;
		}


		@Test
		public void testReadFloatColumns() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new FloatWriter(set).writeFloats(f.toPath());
			ExampleSet read = Hdf5ExampleSetReader.read(f.toPath());
			RapidAssert.assertEquals(set, read);
			assertEquals(set.getAnnotations(), read.getAnnotations());
		}
	}

	@RunWith(Parameterized.class)
	public static class ReadLongFormats {

		public class LongWriter extends ExampleSetHdf5Writer {

			private final ExampleSet exampleSet;

			public LongWriter(ExampleSet exampleSet) {
				super(exampleSet);
				this.exampleSet = exampleSet;
			}

			public void writeLongs(Path f) throws IOException {
				Attributes attributes = exampleSet.getAttributes();
				int allAttributeCount = attributes.allSize();
				int rowCount = exampleSet.size();
				Iterator<AttributeRole> iterator = attributes.allAttributeRoles();
				ColumnInfo[] columnInfos = new ColumnInfo[allAttributeCount];
				for (int i = 0; i < columnInfos.length; i++) {
					AttributeRole next = iterator.next();
					Attribute attribute = next.getAttribute();
					NumericColumnInfo columnInfo = new NumericColumnInfo(attribute.getName(),
							attribute.getValueType() == Ontology.INTEGER ? ColumnInfo.ColumnType.TIME :
									ColumnInfo.ColumnType.REAL, null) {

						@Override
						public DataType getDataType() {
							//create float type
							return DefaultDataType.FIXED64;
						}
					};

					columnInfos[i] = columnInfo;
				}
				write(columnInfos, exampleSet.getAnnotations(), rowCount, null, f);
			}

			@Override
			protected void writeLongData(ColumnInfo columnInfo, CustomDataOutput channel) throws IOException {
				Attribute att = exampleSet.getAttributes().get(columnInfo.getName());
				for (Example example : exampleSet) {
					channel.writeLong((long) example.getValue(att));
				}
			}
		}

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"real and int", createExampleSetNumNoFractions(50, 10000, true)});
			return sets;
		}


		@Test
		public void testReadFloatColumns() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new LongWriter(set).writeLongs(f.toPath());
			ExampleSet read = Hdf5ExampleSetReader.read(f.toPath());
			RapidAssert.assertEquals(set, read);
			assertEquals(set.getAnnotations(), read.getAnnotations());
		}
	}

	@RunWith(Parameterized.class)
	public static class ReadIntegerFormats {

		public class IntegerWriter extends ExampleSetHdf5Writer {

			private final ExampleSet exampleSet;

			public IntegerWriter(ExampleSet exampleSet) {
				super(exampleSet);
				this.exampleSet = exampleSet;
			}

			public void writeInts(Path f) throws IOException {
				Attributes attributes = exampleSet.getAttributes();
				int allAttributeCount = attributes.allSize();
				int rowCount = exampleSet.size();
				Iterator<AttributeRole> iterator = attributes.allAttributeRoles();
				ColumnInfo[] columnInfos = new ColumnInfo[allAttributeCount];
				for (int i = 0; i < columnInfos.length; i++) {
					AttributeRole next = iterator.next();
					Attribute attribute = next.getAttribute();
					NumericColumnInfo columnInfo = new NumericColumnInfo(attribute.getName(),
							attribute.getValueType() == Ontology.INTEGER ? ColumnInfo.ColumnType.TIME :
									ColumnInfo.ColumnType.REAL, null) {

						@Override
						public DataType getDataType() {
							//create float type
							return DefaultDataType.FIXED32;
						}
					};

					columnInfos[i] = columnInfo;
				}
				write(columnInfos, exampleSet.getAnnotations(), rowCount, null, f);
			}

			@Override
			public void writeDoubleData(ColumnInfo columnInfo, CustomDataOutput channel) throws IOException {
				Attribute att = exampleSet.getAttributes().get(columnInfo.getName());
				for (Example example : exampleSet) {
					channel.writeInt((int) example.getValue(att));
				}
			}
		}

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"real and int", createExampleSetNumNoFractions(50, 10000, true)});
			return sets;
		}


		@Test
		public void testReadFloatColumns() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new IntegerWriter(set).writeInts(f.toPath());
			ExampleSet read = Hdf5ExampleSetReader.read(f.toPath());
			RapidAssert.assertEquals(set, read);
			assertEquals(set.getAnnotations(), read.getAnnotations());
		}
	}

	@RunWith(Parameterized.class)
	public static class ReadModeStatistics {

		public class StringsWriter extends ExampleSetHdf5Writer {

			private final ExampleSet exampleSet;

			public StringsWriter(ExampleSet exampleSet) {
				super(exampleSet);
				this.exampleSet = exampleSet;
			}

			public void writeDifferent(Path f) throws IOException {
				Attributes attributes = exampleSet.getAttributes();
				int allAttributeCount = attributes.allSize();
				int rowCount = exampleSet.size();
				Iterator<AttributeRole> iterator = attributes.allAttributeRoles();
				ColumnInfo[] columnInfos = new ColumnInfo[allAttributeCount];
				for (int i = 0; i < columnInfos.length; i++) {
					AttributeRole next = iterator.next();
					Attribute attribute = next.getAttribute();
					NominalMapping mapping = attribute.getMapping();
					StringColumnInfo stringColumnInfo = new StringColumnInfo(attribute.getName(),
							ColumnInfo.ColumnType.NOMINAL, null,
							mapping.getValues(), v -> mapping.getIndex(v) >= 0, i % 3 == 0 ?
							ColumnInfo.StorageType.STRING_RAW :
							i % 3 != 1 || i % 2 != 0 ? ColumnInfo.StorageType.STRING_DICTIONARY
									: ColumnInfo.StorageType.STRING_TINY_DICTIONARY,
							i % 2 == 0);
					stringColumnInfo.addAdditionalAttribute(ATTRIBUTE_LEGACY_TYPE, byte.class,
							(byte) attribute.getValueType());
					StatisticsHandler.addStatistics(stringColumnInfo, attribute, exampleSet);
					columnInfos[i] = stringColumnInfo;
				}
				write(columnInfos, exampleSet.getAnnotations(), rowCount,
						Collections.singletonMap(ATTRIBUTE_HAS_STATISTICS,
						new ImmutablePair<>(byte.class, (byte) 1)), f);
			}
		}

		@BeforeClass
		public static void setup() {
			RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.TEST);
			ParameterService.init();
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			ExampleSet exampleSet = createDataSetNom(400, 300);
			sets.add(new Object[]{"mode first", exampleSet});
			ExampleSet mockedExampleSet0 = Mockito.spy(exampleSet);
			doReturn(42.0).when(mockedExampleSet0).getStatistics(any(), any());
			sets.add(new Object[]{"mode first part", mockedExampleSet0});
			ExampleSet mockedExampleset = Mockito.spy(exampleSet);
			doReturn(142.0).when(mockedExampleset).getStatistics(any(), any());
			sets.add(new Object[]{"mode last part", mockedExampleset});
			ExampleSet mockedExampleSet = Mockito.spy(exampleSet);
			doReturn((double)mockedExampleSet.size()-1).when(mockedExampleSet).getStatistics(any(), any());
			sets.add(new Object[]{"mode last", mockedExampleSet});
			ExampleSet mockedExampleSet2 = Mockito.spy(exampleSet);
			doReturn((double)AttributeMetaData.getMaximumNumberOfNominalValues()).when(mockedExampleSet2).getStatistics(any(), any());
			sets.add(new Object[]{"mode limit", mockedExampleSet2});
			ExampleSet mockedExampleSet3 = Mockito.spy(exampleSet);
			doReturn((double)AttributeMetaData.getMaximumNumberOfNominalValues()-1).when(mockedExampleSet3).getStatistics(any(), any());
			sets.add(new Object[]{"mode limit-1", mockedExampleSet3});
			ExampleSet mockedExampleSet4 = Mockito.spy(exampleSet);
			doReturn((double)AttributeMetaData.getMaximumNumberOfNominalValues()+1).when(mockedExampleSet4).getStatistics(any(), any());
			sets.add(new Object[]{"mode limit+1", mockedExampleSet4});
			return sets;
		}


		@Test
		public void testWriteAndReadMD() throws IOException {
			File f = File.createTempFile("test", ".hdf5");
			f.deleteOnExit();
			new StringsWriter(set).writeDifferent(f.toPath());
			ExampleSetMetaData read = Hdf5ExampleSetReader.readMetaData(f.toPath());
			ExampleSetMetaData expected = new ExampleSetMetaData(set);
			assertEquals(expected.getNumberOfExamples().getNumber(), read.getNumberOfExamples().getNumber());
			Iterator<AttributeMetaData> readIterator = read.getAllAttributes().iterator();
			Iterator<AttributeMetaData> expectedIterator = expected.getAllAttributes().iterator();
			while (expectedIterator.hasNext()) {
				AttributeMetaData readAtt = readIterator.next();
				AttributeMetaData expectedAtt = expectedIterator.next();
				assertEquals(expectedAtt.getDescription(), readAtt.getDescription());
				assertEquals(expectedAtt.getValueSet(), readAtt.getValueSet());
				assertEquals(expectedAtt.getMode(), readAtt.getMode());
			}
		}

	}

	private static ExampleSet createExampleSetNom(int columns, int rows, int values, boolean fixedLength,
												  boolean perColumn, boolean endOnNull) {
		int valueType = values <= 2 ? Ontology.BINOMINAL : values < rows ? Ontology.NOMINAL : Ontology.STRING;
		List<Attribute> attributes = IntStream.range(0, columns).mapToObj(i -> "att-" + i)
				.map(name -> AttributeFactory.createAttribute(name, valueType))
				.collect(Collectors.toList());
		ExampleSetBuilder builder = ExampleSets.from(attributes).withBlankSize(rows);
		IntToDoubleFunction intFiller;
		if (valueType == Ontology.STRING) {
			intFiller = i -> i;
		} else {
			intFiller = i -> {
				int index = rng.nextInt(values + 1);
				return index == values ? Double.NaN : index;
			};
		}
		attributes.forEach(att -> {
			NominalMapping mapping = att.getMapping();
			int limit = values;
			if (!fixedLength) {
				limit--;
			}
			for (int i = 0; i < limit; i++) {
				mapping.mapString((perColumn ? att.getName() + "-" : "") + "value-" + i);
			}
			if (!fixedLength) {
				mapping.mapString((perColumn ? att.getName() + "-" : "") + "value-" + limit + StringUtils.repeat(
						"var", "|", 50) + (endOnNull ? "\u0000" : ""));
			}
			builder.withColumnFiller(att, intFiller);
		});
		ExampleSet build = builder.build();
		build.recalculateAllAttributeStatistics();
		return build;
	}

	private static ExampleSet createExampleSetNum(int columns, int rows, boolean allowIntegers) {
		List<Attribute> attributes =
				IntStream.range(0, columns).mapToObj(i -> "att-" + i + "-" + (allowIntegers && i % 2 == 1))
						.map(name -> AttributeFactory.createAttribute(name, name.endsWith("true") ? Ontology.INTEGER :
								Ontology.REAL))
						.collect(Collectors.toList());
		ExampleSetBuilder builder = ExampleSets.from(attributes).withBlankSize(rows);
		IntToDoubleFunction doubleFiller = i -> rng.nextDouble();
		IntToDoubleFunction intFiller = i -> rng.nextInt();
		attributes.forEach(att -> builder.withColumnFiller(att, att.getValueType() == Ontology.REAL ? doubleFiller :
				intFiller));
		ExampleSet build = builder.build();
		build.getAnnotations().put("bla", "blup");
		build.getAnnotations().put("blabla", "blupblup");
		build.recalculateAllAttributeStatistics();
		return build;
	}

	private static ExampleSet createExampleSetNumFloat(int columns, int rows, boolean allowIntegers) {
		List<Attribute> attributes =
				IntStream.range(0, columns).mapToObj(i -> "att-" + i + "-" + (allowIntegers && i % 2 == 1))
						.map(name -> AttributeFactory.createAttribute(name, name.endsWith("true") ? Ontology.INTEGER :
								Ontology.REAL))
						.collect(Collectors.toList());
		ExampleSetBuilder builder = ExampleSets.from(attributes).withBlankSize(rows);
		IntToDoubleFunction doubleFiller = i -> (float) rng.nextDouble();
		IntToDoubleFunction intFiller = i -> (float)rng.nextInt();
		attributes.forEach(att -> builder.withColumnFiller(att, att.getValueType() == Ontology.REAL ? doubleFiller :
				intFiller));
		ExampleSet build = builder.build();
		build.getAnnotations().put("bla", "blup");
		build.getAnnotations().put("blabla", "blupblup");
		build.recalculateAllAttributeStatistics();
		return build;
	}

	private static ExampleSet createExampleSetNumNoFractions(int columns, int rows, boolean allowIntegers) {
		List<Attribute> attributes =
				IntStream.range(0, columns).mapToObj(i -> "att-" + i + "-" + (allowIntegers && i % 2 == 1))
						.map(name -> AttributeFactory.createAttribute(name, name.endsWith("true") ? Ontology.INTEGER :
								Ontology.REAL))
						.collect(Collectors.toList());
		ExampleSetBuilder builder = ExampleSets.from(attributes).withBlankSize(rows);
		IntToDoubleFunction doubleFiller = i -> (int) (rng.nextDouble() * 1000);
		IntToDoubleFunction intFiller = i -> rng.nextInt();
		attributes.forEach(att -> builder.withColumnFiller(att, att.getValueType() == Ontology.REAL ? doubleFiller :
				intFiller));
		ExampleSet build = builder.build();
		build.getAnnotations().put("bla", "blup");
		build.getAnnotations().put("blabla", "blupblup");
		build.recalculateAllAttributeStatistics();
		return build;
	}

	private static ExampleSet createExampleSetDatetime(int columns, int rows, boolean date) {
		List<Attribute> attributes = IntStream.range(0, columns).mapToObj(i -> "att-" + i + "-" + (date && i % 2 == 1))
				.map(name -> AttributeFactory.createAttribute(name, name.endsWith("true") ? Ontology.DATE :
						Ontology.DATE_TIME))
				.collect(Collectors.toList());
		ExampleSetBuilder builder = ExampleSets.from(attributes).withBlankSize(rows);
		IntToDoubleFunction datetimeFiller = i -> rng.nextInt() * 1000001;
		IntToDoubleFunction dateFiller = i -> (rng.nextInt() * 1000000) / 1000 * 1000;
		attributes.forEach(att -> builder.withColumnFiller(att, att.getValueType() == Ontology.DATE_TIME ?
				datetimeFiller : dateFiller));
		ExampleSet build = builder.build();
		build.getAnnotations().put("bla", "blup");
		build.recalculateAllAttributeStatistics();
		return build;
	}

	private static ExampleSet createDataSet(int rows, int columns, int numberOfValues) {
		List<Attribute> attributes = IntStream.range(1, columns + 1).mapToObj(i -> "att-" + i)
				.map(name -> AttributeFactory.createAttribute(name, Ontology.NOMINAL)).collect(Collectors.toList());
		ExampleSetBuilder builder = ExampleSets.from(attributes).withBlankSize(rows);
		attributes.forEach(att -> builder.withColumnFiller(att,
				(i -> att.getMapping().mapString(att.getName() +
						RandomStringUtils.random(100 + rng.nextInt(100)) + rng.nextInt(numberOfValues)))));
		ExampleSet build = builder.build();
		Annotations annotations = build.getAnnotations();
		for (int i = 0; i < columns; i++) {
			annotations.put("column " + i, " is important");
		}
		build.recalculateAllAttributeStatistics();
		return build;
	}

	private static ExampleSet createDataSetNom(int rows, int columns) {
		List<Attribute> attributes = IntStream.range(1, columns + 1).mapToObj(i -> "att-" + i)
				.map(name -> AttributeFactory.createAttribute(name, Ontology.NOMINAL)).collect(Collectors.toList());
		ExampleSetBuilder builder = ExampleSets.from(attributes).withBlankSize(rows);
		for (int i = 0; i < columns; i++) {
			Attribute att = attributes.get(i);
			if (i % 3 == 0) { //for the one test need the raw string data belong to unique values
				builder.withColumnFiller(att,
						(j -> att.getMapping().mapString(att.getName() + "-val" + j)));
			} else {
				builder.withColumnFiller(att,
						(j -> att.getMapping().mapString(att.getName() + "-val" + rng.nextInt(300))));
			}
		}
		ExampleSet build = builder.build();
		Annotations annotations = build.getAnnotations();
		for (int i = 0; i < columns; i++) {
			annotations.put("column " + i, " is important");
		}
		build.recalculateAllAttributeStatistics();
		return build;
	}

	private static ExampleSet createAllTypes() {
		List<Attribute> attributes = IntStream.range(1, Ontology.VALUE_TYPE_NAMES.length)
				.mapToObj(i -> AttributeFactory.createAttribute("att-" + i, i)).collect(Collectors.toList());
		ExampleSetBuilder builder = ExampleSets.from(attributes);
		double[] row = new double[attributes.size()];
		Arrays.fill(row, Double.NaN);
		builder.addRow(row);
		ExampleSet build = builder.build();
		build.recalculateAllAttributeStatistics();
		return build;
	}

	private static ExampleSet createDifferentRoles() {
		List<Attribute> attributes = IntStream.range(1, 12)
				.mapToObj(i -> AttributeFactory.createAttribute("att-" + i, Ontology.REAL)).collect(Collectors.toList());
		ExampleSetBuilder builder = ExampleSets.from(attributes);
		builder.withRole(attributes.get(0), Attributes.LABEL_NAME);
		builder.withRole(attributes.get(1), Attributes.BATCH_NAME);
		builder.withRole(attributes.get(2), Attributes.CLASSIFICATION_COST);
		builder.withRole(attributes.get(3), Attributes.CLUSTER_NAME);
		builder.withRole(attributes.get(4), Attributes.CONFIDENCE_NAME + "_" + "Yes");
		builder.withRole(attributes.get(5), Attributes.ID_NAME);
		builder.withRole(attributes.get(6), Attributes.OUTLIER_NAME);
		builder.withRole(attributes.get(7), Attributes.PREDICTION_NAME);
		builder.withRole(attributes.get(8), Attributes.WEIGHT_NAME);
		builder.withRole(attributes.get(9), "some Random String");
		builder.withRole(attributes.get(10), "some Random String longer and with utf8 ääµµüÖÖÖ%}          ");
		ExampleSet build = builder.build();
		build.recalculateAllAttributeStatistics();
		return build;
	}

}
