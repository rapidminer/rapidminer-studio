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

import static com.rapidminer.storage.hdf5.ExampleSetHdf5Writer.ATTRIBUTE_LEGACY_ROLE;
import static com.rapidminer.storage.hdf5.ExampleSetHdf5Writer.ATTRIBUTE_LEGACY_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.hdf5.file.ColumnInfo;
import com.rapidminer.hdf5.file.ColumnInfo.ColumnRole;
import com.rapidminer.hdf5.file.NumericColumnInfo;
import com.rapidminer.hdf5.file.StringColumnInfo;
import com.rapidminer.hdf5.file.TableWriter;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.Ontology;


/**
 * Utility class for creating {@link ColumnInfo} from an {@link AttributeRole}.
 *
 * @author Jan Czogalla, Gisa Meier
 * @since 9.7.0
 */
enum ColumnInfoCreator {

	;//No-instance enum, only static methods

	/**
	 * Set of ontologies that have an associated belt type
	 */
	private static final Set<Integer> UNNECESSARY_ONTOLOGIES = new HashSet<>(Arrays.asList(Ontology.NOMINAL,
			Ontology.INTEGER, Ontology.REAL, Ontology.DATE_TIME));

	private static final Map<Integer, ColumnInfo.ColumnType> RM_TO_COLUMN_TYPE;

	static {
		Map<Integer, ColumnInfo.ColumnType> map = new HashMap<>();
		map.put(Ontology.NUMERICAL, ColumnInfo.ColumnType.REAL);
		map.put(Ontology.REAL, ColumnInfo.ColumnType.REAL);
		map.put(Ontology.INTEGER, ColumnInfo.ColumnType.INTEGER);
		map.put(Ontology.NOMINAL, ColumnInfo.ColumnType.NOMINAL);
		map.put(Ontology.BINOMINAL, ColumnInfo.ColumnType.NOMINAL);
		map.put(Ontology.POLYNOMINAL, ColumnInfo.ColumnType.NOMINAL);
		map.put(Ontology.STRING, ColumnInfo.ColumnType.NOMINAL);
		map.put(Ontology.FILE_PATH, ColumnInfo.ColumnType.NOMINAL);
		RM_TO_COLUMN_TYPE = Collections.unmodifiableMap(map);
	}

	/**
	 * Creates a column info for an attribute, either a {@link StringColumnInfo} or a {@link NumericColumnInfo}
	 * depending on the value type. If the value type or the role cannot be stored as one of the fixed allowed value
	 * for {@link TableWriter#ATTRIBUTE_TYPE} or {@link TableWriter#ATTRIBUTE_ROLE} they are stored using {@link
	 * ExampleSetHdf5Writer#ATTRIBUTE_LEGACY_TYPE} or {@link ExampleSetHdf5Writer#ATTRIBUTE_LEGACY_ROLE} respectively.
	 * For binominal attributes, the positive index {@code 2} is stored in the {@link
	 * TableWriter#ATTRIBUTE_POSITVE_INDEX} if a positive value exists, {@code -1} otherwise.
	 *
	 * @param attRole
	 * 		the attribute role containing the attribute
	 * @param statisticsProvider
	 * 		the source for statistics, can be {@code null} if no statistics should be written
	 * @return the associated column info
	 */
	static ColumnInfo create(AttributeRole attRole, ExampleSet statisticsProvider) {
		return create(attRole, statisticsProvider, false);
	}

	/**
	 * Creates a column info for an attribute, either a {@link StringColumnInfo} or a {@link NumericColumnInfo}
	 * depending on the value type. If the value type or the role cannot be stored as one of the fixed allowed value
	 * for {@link TableWriter#ATTRIBUTE_TYPE} or {@link TableWriter#ATTRIBUTE_ROLE} they are stored using {@link
	 * ExampleSetHdf5Writer#ATTRIBUTE_LEGACY_TYPE} or {@link ExampleSetHdf5Writer#ATTRIBUTE_LEGACY_ROLE} respectively.
	 * For binominal attributes, the positive index {@code 2} is stored in the {@link
	 * TableWriter#ATTRIBUTE_POSITVE_INDEX} if a positive value exists, {@code -1} otherwise.
	 *
	 * @param attRole
	 * 		the attribute role containing the attribute
	 * @param statisticsProvider
	 * 		the source for statistics, can be {@code null} if no statistics should be written
	 * @param shortenMD
	 * 		whether or not to shorten the metadata (i.e. store not all attributes and/or shorten the list of nominal values)
	 * @return the associated column info
	 */
	static ColumnInfo create(AttributeRole attRole, ExampleSet statisticsProvider, boolean shortenMD) {
		Attribute attribute = attRole.getAttribute();
		boolean isNom = attribute.isNominal();
		NominalMapping mapping = isNom ? attribute.getMapping() : null;
		List<String> values = isNom ? mapping.getValues() : null;
		boolean wasShortened = false;
		double mode = -1;
		if (shortenMD && isNom) {
			int maxNomValues = AttributeMetaData.getMaximumNumberOfNominalValues();
			if (maxNomValues < values.size()) {
				values = values.subList(0, maxNomValues);
				wasShortened = true;
				if (statisticsProvider != null) {
					mode = statisticsProvider.getStatistics(attribute, Statistics.MODE);
					if (mode >= values.size()) {
						String modeValue = mapping.mapIndex((int) mode);
						values = new ArrayList<>(values);
						values.add(modeValue);
					}
				}
			}
		}
		ColumnInfo info = create(attribute.getValueType(), attRole.getSpecialName(), attribute.getName(),
				attribute.isNumerical(), isNom, attribute.isDateTime(),
				values, isNom ? v -> mapping.getIndex(v) >= 0 : null);
		if (statisticsProvider != null) {
			StatisticsHandler.addStatistics(info, attribute, statisticsProvider);
		}
		SetRelation relation = isNom && shortenMD && wasShortened ? SetRelation.SUPERSET : SetRelation.EQUAL;
		info.addAdditionalAttribute(ExampleSetHdf5Writer.ATTRIBUTE_SET_RELATION, String.class, relation.toString());
		if (wasShortened) {
			info.addAdditionalAttribute(ExampleSetHdf5Writer.ATTRIBUTE_NOMINAL_SHRUNK, byte.class, (byte) 1);
			if (mode >= values.size()) {
				int mdMode = values.size();
				info.getAdditionalAttributes().computeIfPresent(StatisticsHandler.STATISTICS_MODE,
						(k, v) -> new ImmutablePair<>(int.class, mdMode));
			}
		}
		return info;
	}

	/**
	 * Creates a column info for an {@link AttributeMetaData}, either a {@link StringColumnInfo} or a
	 * {@link NumericColumnInfo} depending on the value type. If the value type or the role cannot be stored as one
	 * of the fixed allowed value for {@link TableWriter#ATTRIBUTE_TYPE} or {@link TableWriter#ATTRIBUTE_ROLE} they are
	 * stored using {@link ExampleSetHdf5Writer#ATTRIBUTE_LEGACY_TYPE} or {@link ExampleSetHdf5Writer#ATTRIBUTE_LEGACY_ROLE}
	 * respectively. For binominal attributes, the positive index {@code 2} is stored in the
	 * {@link TableWriter#ATTRIBUTE_POSITVE_INDEX} if a positive value exists, {@code -1} otherwise.
	 *
	 * @param amd
	 * 		the attribute meta data
	 * @param writeStatistics
	 * 		whether statistics should be written
	 * @return the associated column info
	 */
	static ColumnInfo create(AttributeMetaData amd, boolean writeStatistics) {
		return create(amd, writeStatistics, false);
	}

	/**
	 * Creates a column info for an {@link AttributeMetaData}, either a {@link StringColumnInfo} or a
	 * {@link NumericColumnInfo} depending on the value type. If the value type or the role cannot be stored as one
	 * of the fixed allowed value for {@link TableWriter#ATTRIBUTE_TYPE} or {@link TableWriter#ATTRIBUTE_ROLE} they are
	 * stored using {@link ExampleSetHdf5Writer#ATTRIBUTE_LEGACY_TYPE} or {@link ExampleSetHdf5Writer#ATTRIBUTE_LEGACY_ROLE}
	 * respectively. For binominal attributes, the positive index {@code 2} is stored in the
	 * {@link TableWriter#ATTRIBUTE_POSITVE_INDEX} if a positive value exists, {@code -1} otherwise.
	 *
	 * @param amd
	 * 		the attribute meta data
	 * @param writeStatistics
	 * 		whether statistics should be written
	 * @param shortenMD
	 * 		whether or not to shorten the metadata (i.e. store not all attributes and/or shorten the list of nominal values)
	 * @return the associated column info
	 */
	static ColumnInfo create(AttributeMetaData amd, boolean writeStatistics, boolean shortenMD) {
		boolean isNom = amd.isNominal();
		List<String> values = null;
		String mode = amd.getMode();
		SetRelation relation = amd.getValueSetRelation();
		if (isNom) {
			if (relation == SetRelation.UNKNOWN) {
				values = new ArrayList<>();
			} else {
				Stream<String> valueStream = amd.getValueSet().stream();
				if (shortenMD) {
					valueStream = valueStream.limit(AttributeMetaData.getMaximumNumberOfNominalValues());
				}
				values = valueStream.collect(Collectors.toList());
				if (values.size() < amd.getValueSet().size()) {
					relation = relation.merge(SetRelation.SUPERSET);
				}
			}
		}
		if (isNom && mode != null && !values.contains(mode)) {
			values.add(mode);
		}
		ColumnInfo info = create(amd.getValueType(), amd.getRole(), amd.getName(),
				amd.isNumerical(), isNom, amd.isDateTime(), values, isNom ? values::contains : null);
		if (writeStatistics) {
			StatisticsHandler.addStatistics(info, amd);
		}
		if (relation != SetRelation.UNKNOWN) {
			info.addAdditionalAttribute(ExampleSetHdf5Writer.ATTRIBUTE_SET_RELATION, String.class, relation.toString());
		}
		if (isNom && amd.valueSetWasShrunk()) {
			info.addAdditionalAttribute(ExampleSetHdf5Writer.ATTRIBUTE_NOMINAL_SHRUNK, byte.class, (byte) 1);
		}
		return info;
	}

	/**
	 * Creates the actual {@link ColumnInfo} from the extracted information from either {@link AttributeRole} or
	 * {@link AttributeMetaData}.
	 *
	 * @param valueType
	 * 		the attributes value type
	 * @param specialName
	 * 		the special role; might be {@code null}
	 * @param attName
	 * 		the attribute name
	 * @param isNum
	 * 		if the attribute is numerical
	 * @param isNom
	 * 		if the attribute is nominal
	 * @param isDateTime
	 * 		if the attribute is a date/time
	 * @param dictionary
	 * 		the nominal values of the attribute if it is nominal; {@code null} otherwise
	 * @param containedInDictionary
	 * 		predicate to decide if a given nominal value is actually in the dictionary; {@code null},
	 * 		if the attribute is not nominal
	 * @return the column info
	 */
	private static ColumnInfo create(int valueType, String specialName, String attName,
									 boolean isNum, boolean isNom, boolean isDateTime,
									 List<String> dictionary, Predicate<String> containedInDictionary) {
		ColumnInfo info;
		ColumnInfo.ColumnType columnType = fromOntology(valueType);
		Pair<ColumnRole, String> columnRole = toColumnRole(specialName);
		if (isNum) {
			info = new NumericColumnInfo(attName, columnType, columnRole.getLeft());
		} else {
			if (isNom) {
				// use -1 as number of rows to prevent nominal columns being stored without category indices
				info = new StringColumnInfo(attName, columnType, columnRole.getLeft(),
						dictionary, containedInDictionary, -1);
			} else {
				if (isDateTime) {
					info = NumericColumnInfo.createDateTime(attName, columnRole.getLeft(),
							valueType != Ontology.DATE);
				} else {
					throw new AssertionError();
				}
			}
		}

		if (!UNNECESSARY_ONTOLOGIES.contains(valueType)) {
			info.addAdditionalAttribute(ATTRIBUTE_LEGACY_TYPE, byte.class, (byte) valueType);
		}

		String legacyRole = columnRole.getRight();
		if (legacyRole != null) {
			info.addAdditionalAttribute(ATTRIBUTE_LEGACY_ROLE, String.class, legacyRole);
		}

		if (valueType == Ontology.BINOMINAL) {
			int size = dictionary.size();
			if (size == 2) {
				//for binominal, the first value is negative and the second positive, plus shift by 1
				info.addAdditionalAttribute(TableWriter.ATTRIBUTE_POSITVE_INDEX, byte.class, (byte) 2);
			} else if (size < 2) {
				//if no second value exists, there is no positive index
				info.addAdditionalAttribute(TableWriter.ATTRIBUTE_POSITVE_INDEX, byte.class, (byte) -1);
			}
		}
		return info;
	}

	/**
	 * Converts ontology to column type using the {@link #RM_TO_COLUMN_TYPE} map.
	 */
	private static ColumnInfo.ColumnType fromOntology(int ontology) {
		return RM_TO_COLUMN_TYPE.getOrDefault(ontology, ColumnInfo.ColumnType.REAL);
	}

	/**
	 * Converts the role to a pair that contains the column role and, if that is not specific enough, the role itself
	 * again.
	 *
	 * @param role
	 * 		the role to convert
	 * @return a pair of a column role and optional the input role again
	 */
	private static Pair<ColumnInfo.ColumnRole, String> toColumnRole(String role) {
		if (role == null) {
			return new ImmutablePair<>(null, null);
		}
		switch (role) {
			case Attributes.LABEL_NAME:
				return new ImmutablePair<>(ColumnInfo.ColumnRole.LABEL, null);
			case Attributes.ID_NAME:
				return new ImmutablePair<>(ColumnInfo.ColumnRole.ID, null);
			case Attributes.PREDICTION_NAME:
				return new ImmutablePair<>(ColumnInfo.ColumnRole.PREDICTION, null);
			case Attributes.CONFIDENCE_NAME:
				return new ImmutablePair<>(ColumnInfo.ColumnRole.SCORE, null);
			case Attributes.CLUSTER_NAME:
				return new ImmutablePair<>(ColumnInfo.ColumnRole.CLUSTER, null);
			case Attributes.OUTLIER_NAME:
				return new ImmutablePair<>(ColumnInfo.ColumnRole.OUTLIER, null);
			case Attributes.WEIGHT_NAME:
				return new ImmutablePair<>(ColumnInfo.ColumnRole.WEIGHT, null);
			case Attributes.BATCH_NAME:
				return new ImmutablePair<>(ColumnInfo.ColumnRole.BATCH, null);
			default:
				if (role.startsWith(Attributes.CONFIDENCE_NAME)) {
					return new ImmutablePair<>(ColumnInfo.ColumnRole.SCORE, role);
				}
				return new ImmutablePair<>(ColumnInfo.ColumnRole.METADATA, role);
		}

	}

}
