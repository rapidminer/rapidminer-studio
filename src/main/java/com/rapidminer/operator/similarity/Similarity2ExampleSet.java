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
package com.rapidminer.operator.similarity;

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasures;
import com.rapidminer.tools.metadata.MetaDataTools;


/**
 * <p>
 * This operator creates an example set from a given similarity measure. It can either produce a
 * long table format, i.e. something like<br />
 * <br />
 * id1 id2 sim<br />
 * id1 id3 sim<br />
 * id1 id4 sim<br />
 * ...<br />
 * id2 id1 sim<br />
 * ...<br />
 * <br />
 * or a matrix format like here<br />
 * <br />
 * id id1 id2 id3 ...<br />
 * id1 sim sim sim...<br />
 * ... <br />
 * </p>
 *
 * @author Ingo Mierswa
 */
public class Similarity2ExampleSet extends Operator {

	private final InputPort similarityInput = getInputPorts().createPort("similarity", SimilarityMeasureObject.class);
	private final InputPort exampleSetInput = getInputPorts().createPort("exampleSet", ExampleSet.class);
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("exampleSet");

	public static final String PARAMETER_TABLE_TYPE = "table_type";

	public static final String[] TABLE_TYPES = { "long_table", "matrix" };

	public static final int TABLE_TYPE_LONG_TABLE = 0;

	public static final int TABLE_TYPE_MATRIX = 1;

	public Similarity2ExampleSet(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				AttributeMetaData idAttribute = metaData.getSpecial(Attributes.ID_NAME);
				try {
					if (getParameterAsInt(PARAMETER_TABLE_TYPE) == TABLE_TYPE_LONG_TABLE) {
						if (idAttribute == null) {
							MetaDataTools.checkAndCreateIds(metaData);
						}
						idAttribute = metaData.getSpecial(Attributes.ID_NAME);

						ExampleSetMetaData newSet = new ExampleSetMetaData();
						AttributeMetaData firstId = idAttribute.copy();
						AttributeMetaData secondId = idAttribute.copy();
						firstId.setName("FIRST_ID");
						firstId.setRole(Attributes.ATTRIBUTE_NAME);
						secondId.setName("SECOND_ID");
						secondId.setRole(Attributes.ATTRIBUTE_NAME);

						// determining if its distance or similarity
						DistanceMeasure measure;
						String name = "SIMILARITY";
						try {
							measure = DistanceMeasures.createMeasure(Similarity2ExampleSet.this);
							if (measure.isDistance()) {
								name = "DISTANCE";
							}
						} catch (UndefinedParameterError e) {
						} catch (OperatorException e) {
						}

						AttributeMetaData distanceAttribute = new AttributeMetaData(name, Ontology.REAL,
								Attributes.ATTRIBUTE_NAME);
						newSet.addAttribute(firstId);
						newSet.addAttribute(secondId);
						newSet.addAttribute(distanceAttribute);

						// calculating size
						if (metaData.getNumberOfExamples().isKnown()) {
							newSet.setNumberOfExamples(metaData.getNumberOfExamples().getValue().intValue()
									* (metaData.getNumberOfExamples().getValue().intValue() - 1));
						}
						return newSet;
					} else {
						ExampleSetMetaData newSet = new ExampleSetMetaData();
						if (metaData.getSpecial(Attributes.ID_NAME) == null && metaData.getNumberOfExamples().isKnown()) {
							// then exact reproduction is possible
							AttributeMetaData firstId = new AttributeMetaData("ID", Ontology.INTEGER, Attributes.ID_NAME);
							newSet.addAttribute(firstId);
							for (int i = 1; i <= metaData.getNumberOfExamples().getValue().intValue(); i++) {
								AttributeMetaData attr = new AttributeMetaData("" + i, Ontology.REAL);
								attr.setValueRange(new Range(0, Double.POSITIVE_INFINITY), SetRelation.SUBSET);
								attr.setValueSetRelation(SetRelation.SUBSET);
								newSet.addAttribute(attr);
							}
							newSet.setNumberOfExamples(metaData.getNumberOfExamples().getValue().intValue());
						} else {
							AttributeMetaData firstId = metaData.getSpecial(Attributes.ID_NAME).copy();
							firstId.setName("ID");
							newSet.addAttribute(firstId);
							newSet.attributesAreSubset();
						}
						return newSet;
					}
				} catch (UndefinedParameterError e) {
				}
				return metaData;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		SimilarityMeasureObject measureObject = similarityInput.getData(SimilarityMeasureObject.class);

		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Tools.checkAndCreateIds(exampleSet);

		DistanceMeasure measure = measureObject.getDistanceMeasure();
		Attribute id = exampleSet.getAttributes().getId();
		if (id == null) {
			throw new UserError(this, 129);
		}

		ExampleSet result = null;
		if (getParameterAsInt(PARAMETER_TABLE_TYPE) == TABLE_TYPE_LONG_TABLE) {
			List<Attribute> attributes = new ArrayList<Attribute>(3);
			Attribute firstIdAttribute = AttributeFactory.createAttribute("FIRST_ID", id.getValueType());
			attributes.add(firstIdAttribute);
			Attribute secondIdAttribute = AttributeFactory.createAttribute("SECOND_ID", id.getValueType());
			attributes.add(secondIdAttribute);
			String name = "SIMILARITY";
			if (measure.isDistance()) {
				name = "DISTANCE";
			}
			Attribute similarityAttribute = AttributeFactory.createAttribute(name, Ontology.REAL);
			attributes.add(similarityAttribute);

			ExampleSetBuilder builder = ExampleSets.from(attributes);

			int i = 0;
			for (Example example : exampleSet) {
				int j = 0;
				for (Example compExample : exampleSet) {
					if (j != i) {
						double[] data = new double[3];
						if (id.isNominal()) {
							data[0] = firstIdAttribute.getMapping().mapString(
									id.getMapping().mapIndex((int) example.getValue(id)));
							data[1] = secondIdAttribute.getMapping().mapString(
									id.getMapping().mapIndex((int) compExample.getValue(id)));
						} else {
							data[0] = example.getValue(id);
							data[1] = compExample.getValue(id);
						}
						if (measure.isDistance()) {
							data[2] = measure.calculateDistance(example, compExample);
						} else {
							data[2] = measure.calculateSimilarity(example, compExample);
						}
						builder.addRow(data);
					}
					j++;
				}
				i++;
			}

			result = builder.build();

		} else {
			int numberOfExamples = exampleSet.size();
			List<Attribute> attributes = new ArrayList<Attribute>(numberOfExamples + 1);
			Attribute newIdAttribute = AttributeFactory.createAttribute("ID", id.getValueType());
			attributes.add(newIdAttribute);
			for (Example example : exampleSet) {
				Attribute attribute;
				if (id.getValueType() != Ontology.INTEGER) {
					attribute = AttributeFactory.createAttribute(example.getValueAsString(id), Ontology.REAL);
				} else {
					attribute = AttributeFactory.createAttribute("" + (int) example.getValue(id), Ontology.REAL);
				}

				attributes.add(attribute);
			}

			ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(exampleSet.size());

			for (Example example : exampleSet) {
				double[] data = new double[numberOfExamples + 1];
				if (id.isNominal()) {
					data[0] = newIdAttribute.getMapping().mapString(id.getMapping().mapIndex((int) example.getValue(id)));
				} else {
					data[0] = example.getValue(id);
				}
				int index = 1;
				for (Example compExample : exampleSet) {
					if (measure.isDistance()) {
						data[index++] = measure.calculateDistance(example, compExample);
					} else {
						data[index++] = measure.calculateSimilarity(example, compExample);
					}
				}
				builder.addRow(data);
			}

			result = builder.withRole(newIdAttribute, Attributes.ID_NAME).build();
		}

		exampleSetOutput.deliver(result);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_TABLE_TYPE,
				"Indicates if the resulting table should have a matrix format or a long table format.", TABLE_TYPES,
				TABLE_TYPE_LONG_TABLE);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
