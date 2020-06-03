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
package com.rapidminer.storage.hdf5;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleFunction;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.hdf5.CustomDataOutput;
import com.rapidminer.hdf5.file.ColumnInfo;
import com.rapidminer.hdf5.file.NumericColumnInfo;
import com.rapidminer.hdf5.file.TableWriter;
import com.rapidminer.hdf5.metadata.GlobalHeap;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MDNumber;
import com.rapidminer.operator.ports.metadata.MDNumber.Relation;
import com.rapidminer.operator.ports.metadata.SetRelation;


/**
 * Writes {@link ExampleSet}s and {@link ExampleSetMetaData} to hdf5 using the {@link TableWriter}.
 * Additionally writes statistics if they are present, see {@link StatisticsHandler}.
 * <p>
 * The writer can be configured to write either the example set or only the metadata. When writing metadata,
 * the writer can also be configured to only write a shortened version of the metadata. If so configured,
 * only the first {@link ExampleSetMetaData#getMaximumNumberOfAttributes()} columns are written to the file.
 * If a column is nominal, only the first {@link AttributeMetaData#getMaximumNumberOfNominalValues()} and the
 * mode value (if present) are written.
 *
 * @author Jan Czogalla, Gisa Meier
 * @since 9.7.0
 */
public class ExampleSetHdf5Writer extends TableWriter {

	/**
	 * Hdf5 attribute name to store type information that cannot be stored in {@link TableWriter#ATTRIBUTE_TYPE}
	 */
	public static final String ATTRIBUTE_LEGACY_TYPE = "legacy_type";

	/**
	 * Hdf5 attribute name to store role information that cannot be stored in {@link TableWriter#ATTRIBUTE_ROLE}
	 */
	public static final String ATTRIBUTE_LEGACY_ROLE = "legacy_role";

	/**
	 * Hdf5 attribute name to indicate whether statistics information are present
	 */
	public static final String ATTRIBUTE_HAS_STATISTICS = "has_statistics";

	/**
	 * Hdf5 attribute name to indicate whether the file is a meta data file
	 */
	public static final String ATTRIBUTE_IS_METADATA = "is_md";

	/**
	 * Hdf5 attribute name to indicate whether a column's nominal values were shrunk
	 */
	public static final String ATTRIBUTE_NOMINAL_SHRUNK = "nominal_data_shrunk";

	/**
	 * Hdf5 attribute name to indicate the {@link SetRelation} of a metadata file or its columns
	 */
	public static final String ATTRIBUTE_SET_RELATION = "set_relation";

	/**
	 * Hdf5 attribute name to indicate the {@link Relation} of a metadata file's row count.
	 */
	public static final String ATTRIBUTE_ROW_RELATION = "row_relation";

	/**
	 * Number of milli-seconds in a second
	 */
	static final long MILLISECONDS_PER_SECOND = 1_000;

	/**
	 * Number of nano-seconds in a milli-second
	 */
	static final long NANOS_PER_MILLISECOND = 1_000_000;

	private final ExampleSet exampleSet;
	private final ExampleSetMetaData md;
	private final boolean shortenMD;

	/**
	 * Creates a new writer for the {@link ExampleSet}.
	 *
	 * @param exampleSet
	 * 		an example set with up to date statistics already calculated
	 */
	public ExampleSetHdf5Writer(ExampleSet exampleSet) {
		this(exampleSet, false, false);
	}

	/**
	 * Creates a new writer for the {@link ExampleSet}.
	 *
	 * @param exampleSet
	 * 		an example set with up to date statistics already calculated
	 * @param asMetaData
	 * 		whether this writer should only write a metadata file
	 */
	public ExampleSetHdf5Writer(ExampleSet exampleSet, boolean asMetaData) {
		this(exampleSet, asMetaData, false);
	}

	/**
	 * Creates a new writer for the example set.
	 *
	 * @param exampleSet
	 * 		an example set with up to date statistics already calculated
	 * @param asMetaData
	 * 		whether this writer should only write a metadata file
	 * @param shortenMD
	 * 		whether to shorten the metadata
	 */
	public ExampleSetHdf5Writer(ExampleSet exampleSet, boolean asMetaData, boolean shortenMD) {
		super(asMetaData);
		this.exampleSet = exampleSet;
		this.md = null;
		this.shortenMD = asMetaData && shortenMD;
	}

	/**
	 * Creates a new writer for the given {@link ExampleSetMetaData}. Can/will only write a metadata file.
	 *
	 * @param md
	 * 		a set of metadata for an example set with all statistics up to date
	 */
	public ExampleSetHdf5Writer(ExampleSetMetaData md) {
		this(md, false);
	}

	/**
	 * Creates a new writer for the given {@link ExampleSetMetaData}. Can/will only write a metadata file.
	 *
	 * @param md
	 * 		a set of metadata for an example set with all statistics up to date
	 * @param shortenMD
	 * 		whether to shorten the metadata
	 */
	public ExampleSetHdf5Writer(ExampleSetMetaData md, boolean shortenMD) {
		super(true);
		this.exampleSet = null;
		this.md = md.clone();
		this.shortenMD = shortenMD;
	}

	/**
	 * Writes the {@link ExampleSet} or {@link ExampleSetMetaData} to the given path. Depending on the configuration,
	 * of this writer, writes it as a full example set or as a meta data file.
	 *
	 * @param path
	 * 		the path to write to
	 * @throws IOException
	 * 		if writing fails
	 */
	public void write(Path path) throws IOException {
		write(path, hasStatistics());
	}

	/**
	 * Writes the {@link ExampleSet} or {@link ExampleSetMetaData} to the given path. Depending on the configuration
	 * of this writer, writes it as a full example set or as a meta data file.
	 *
	 * @param path
	 * 		the path to write to
	 * @throws IOException
	 * 		if writing fails
	 */
	public void write(Path path, boolean writeStatistics) throws IOException {
		if (exampleSet != null) {
			writeFromExampleSet(path, writeStatistics);
			return;
		}
		writeFromMetadata(path, writeStatistics);
	}

	/**
	 * Write example set or metadata file from {@link ExampleSet} as a source
	 */
	private void writeFromExampleSet(Path path, boolean writeStatistics) throws IOException {
		Attributes attributes = exampleSet.getAttributes();
		int allAttributeCount = attributes.allSize();
		int rowCount = exampleSet.size();
		boolean isMetaData = isNullDataSpace();
		Iterator<AttributeRole> iterator = attributes.allAttributeRoles();
		boolean wasShortened = false;
		if (shortenMD) {
			int maxAtt = ExampleSetMetaData.getMaximumNumberOfAttributes();
			if (maxAtt < allAttributeCount) {
				wasShortened = true;
				allAttributeCount = maxAtt;
			}
		}
		ColumnInfo[] columnInfos = new ColumnInfo[allAttributeCount];
		for (int i = 0; i < columnInfos.length; i++) {
			AttributeRole next = iterator.next();
			columnInfos[i] = ColumnInfoCreator.create(next, writeStatistics ? exampleSet : null, isMetaData && shortenMD);
			if (!isMetaData) {
				columnInfos[i].getAdditionalAttributes().remove(ATTRIBUTE_SET_RELATION);
			}
		}
		Map<String, Pair<Class<?>, Object>> additionalRootAttributes = new LinkedHashMap<>();
		additionalRootAttributes.put(ATTRIBUTE_HAS_STATISTICS, new ImmutablePair<>(byte.class, writeStatistics ? (byte) 1 : (byte) 0));
		if (isMetaData) {
			additionalRootAttributes.put(ATTRIBUTE_IS_METADATA, new ImmutablePair<>(byte.class, (byte) 1));
			additionalRootAttributes.put(ATTRIBUTE_SET_RELATION, new ImmutablePair<>(String.class,
					(wasShortened ? SetRelation.SUPERSET : SetRelation.EQUAL).toString()));
			additionalRootAttributes.put(ATTRIBUTE_ROW_RELATION, new ImmutablePair<>(String.class, Relation.EQUAL.getRepresentation()));
		}
		write(columnInfos, exampleSet.getAnnotations(), rowCount, additionalRootAttributes, path);
	}

	/**
	 * Write metadata file from {@link ExampleSetMetaData} as a source
	 */
	private void writeFromMetadata(Path path, boolean writeStatistics) throws IOException {
		Collection<AttributeMetaData> allAttributes = md.getAllAttributes();
		int allAttributeCount = allAttributes.size();
		SetRelation attSetRelation = md.getAttributeSetRelation();
		MDInteger nEx = md.getNumberOfExamples();
		int rowCount = Optional.ofNullable(nEx.getNumber()).orElse(0);

		if (shortenMD) {
			int maxAtt = ExampleSetMetaData.getMaximumNumberOfAttributes();
			if (maxAtt < allAttributeCount) {
				allAttributeCount = maxAtt;
				attSetRelation = attSetRelation.merge(SetRelation.SUPERSET);
			}
		}
		ColumnInfo[] columnInfos = new ColumnInfo[allAttributeCount];
		Iterator<AttributeMetaData> iterator = allAttributes.iterator();
		for (int i = 0; i < columnInfos.length; i++) {
			AttributeMetaData next = iterator.next();
			columnInfos[i] = ColumnInfoCreator.create(next, writeStatistics, shortenMD);
		}
		Map<String, Pair<Class<?>, Object>> additionalRootAttributes = new LinkedHashMap<>();
		additionalRootAttributes.put(ATTRIBUTE_HAS_STATISTICS, new ImmutablePair<>(byte.class, writeStatistics ? (byte) 1 : (byte) 0));
		additionalRootAttributes.put(ATTRIBUTE_IS_METADATA, new ImmutablePair<>(byte.class, (byte) 1));
		if (attSetRelation != SetRelation.UNKNOWN) {
			additionalRootAttributes.put(ATTRIBUTE_SET_RELATION, new ImmutablePair<>(String.class, attSetRelation.toString()));
		}
		additionalRootAttributes.put(ATTRIBUTE_ROW_RELATION, new ImmutablePair<>(String.class, nEx.getRelation().getRepresentation()));
		write(columnInfos, md.getAnnotations(), rowCount, additionalRootAttributes, path);
	}

	@Override
	public void writeDoubleData(ColumnInfo columnInfo, CustomDataOutput channel) throws IOException {
		Attribute att = exampleSet.getAttributes().get(columnInfo.getName());
		for (Example example : exampleSet) {
			channel.writeDouble(example.getValue(att));
		}
	}

	@Override
	protected void writeLongData(ColumnInfo columnInfo, CustomDataOutput channel) throws IOException {
		Attribute att = exampleSet.getAttributes().get(columnInfo.getName());
		writeDateData(channel, att);
	}

	/**
	 * Writes the second part of date-time data.
	 */
	private void writeDateData(CustomDataOutput channel, Attribute att) throws IOException {
		for (Example example : exampleSet) {
			double value = example.getValue(att);
			if (Double.isNaN(value)) {
				channel.writeLong(Long.MAX_VALUE);
			} else {
				channel.writeLong(Math.floorDiv((long) value, MILLISECONDS_PER_SECOND));
			}
		}
	}

	@Override
	protected void writeAdditionalData(NumericColumnInfo columnInfo, CustomDataOutput channel) throws IOException {
		Attribute att = exampleSet.getAttributes().get(columnInfo.getName());
		writeDateNanos(channel, att);
	}

	/**
	 * Writes the sub-second part of date-time data as nanoseconds.
	 */
	private void writeDateNanos(CustomDataOutput channel, Attribute att) throws IOException {
		for (Example example : exampleSet) {
			double value = example.getValue(att);
			if (Double.isNaN(value)) {
				channel.writeInt(0);
			} else {
				channel.writeInt((int) (Math.floorMod((long) value, MILLISECONDS_PER_SECOND) * NANOS_PER_MILLISECOND));
			}
		}
	}

	@Override
	public void writeCategoryData(ColumnInfo columnInfo, CustomDataOutput channel) throws IOException {
		Attribute att = exampleSet.getAttributes().get(columnInfo.getName());
		int nBytes = columnInfo.getDataType().width();
		int mapSize = att.getMapping().getValues().size();
		if (mapSize == 0) {
			channel.writeNulls(nBytes * exampleSet.size());
		} else {
			for (Example example : exampleSet) {
				double d = example.getValue(att);
				channel.writeLong(Double.isNaN(d) || d < 0 || d >= mapSize ? 0L : (long) (d + 1), nBytes);
			}
		}
	}

	@Override
	public void writeFixedLengthStrings(ColumnInfo columnInfo, CustomDataOutput channel) throws IOException {
		Attribute att = exampleSet.getAttributes().get(columnInfo.getName());
		DoubleFunction<String> lookup = createLookup(columnInfo, att);
		int maxLength = columnInfo.getMaxStringLength();
		for (Example example : exampleSet) {
			double d = example.getValue(att);
			String applied = lookup.apply(d);
			writeFixedLengthString(applied, maxLength, channel);
		}

	}

	@Override
	public void writeVarLengthStrings(ColumnInfo columnInfo, GlobalHeap<String> globalHeap, CustomDataOutput channel)
			throws IOException {
		Attribute att = exampleSet.getAttributes().get(columnInfo.getName());
		DoubleFunction<String> lookup = createLookup(columnInfo, att);
		for (Example example : exampleSet) {
			double d = example.getValue(att);
			String applied = lookup.apply(d);
			writeVarLengthString(applied, globalHeap, channel);
		}

	}

	/**
	 * Creates a double to String function that converts a double value of a nominal column to a String.
	 */
	private DoubleFunction<String> createLookup(ColumnInfo columnInfo, Attribute att) {
		List<String> values = att.getMapping().getValues();
		String missingRepresentation = columnInfo.getMissingRepresentation();
		DoubleFunction<String> lookup;
		if (values.isEmpty()) {
			lookup = d -> missingRepresentation;
		} else {
			lookup = d -> Double.isNaN(d) || d < 0 || d >= values.size() ? missingRepresentation : values.get((int) d);
		}
		return lookup;
	}

	/**
	 * Checks if it is worth to write statistics because they were most likely calculated.
	 */
	private boolean hasStatistics() {
		if (md != null) {
			return md.getAllAttributes().stream().map(AttributeMetaData::getNumberOfMissingValues)
					.map(MDNumber::getRelation).noneMatch(rel -> rel == Relation.UNKNOWN);
		}
		Iterator<Attribute> attributeIterator = exampleSet.getAttributes().allAttributes();
		if (attributeIterator.hasNext()) {
			double statistics = exampleSet.getStatistics(attributeIterator.next(), Statistics.UNKNOWN);
			//if ExampleSet#recalculateAllAttributeStatistics has not been called, this value is NaN
			return !Double.isNaN(statistics);
		}
		return false;
	}

	
}
