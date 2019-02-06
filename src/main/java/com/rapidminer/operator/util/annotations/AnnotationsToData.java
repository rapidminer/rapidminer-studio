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
package com.rapidminer.operator.util.annotations;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.tools.Ontology;


/**
 * @author Marius Helf
 *
 */
public class AnnotationsToData extends Operator {

	private static final String ANNOTATION_ATTRIBUTE = "annotation";
	private static final String VALUE_ATTRIBUTE = "value";

	private InputPort inputPort = getInputPorts().createPort("object", IOObject.class);
	private OutputPort annotationsOutputPort = getOutputPorts().createPort("annotations");
	private OutputPort outputPort = getOutputPorts().createPort("object through");

	/**
	 * @param description
	 */
	public AnnotationsToData(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(inputPort, outputPort);
		getTransformer().addRule(new MDTransformationRule() {

			@Override
			public void transformMD() {
				ExampleSetMetaData metaData = new ExampleSetMetaData();
				metaData.addAttribute(new AttributeMetaData(ANNOTATION_ATTRIBUTE, Ontology.POLYNOMINAL, Attributes.ID_NAME));
				metaData.addAttribute(new AttributeMetaData(VALUE_ATTRIBUTE, Ontology.POLYNOMINAL));
				annotationsOutputPort.deliverMD(metaData);
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		IOObject data = inputPort.getData(IOObject.class);
		Annotations annotations = data.getAnnotations();
		Attribute annotationAttr = AttributeFactory.createAttribute(ANNOTATION_ATTRIBUTE, Ontology.POLYNOMINAL);
		Attribute valueAttr = AttributeFactory.createAttribute(VALUE_ATTRIBUTE, Ontology.POLYNOMINAL);

		ExampleSetBuilder builder = ExampleSets.from(annotationAttr, valueAttr).withExpectedSize(annotations.size());

		for (String annotation : annotations.getDefinedAnnotationNames()) {
			double[] rowData = new double[2];
			rowData[0] = annotationAttr.getMapping().mapString(annotation);
			rowData[1] = valueAttr.getMapping().mapString(annotations.getAnnotation(annotation));
			builder.addRow(rowData);
		}

		ExampleSet exampleSet = builder.build();
		exampleSet.getAttributes().setSpecialAttribute(annotationAttr, Attributes.ID_NAME);
		outputPort.deliver(data);
		annotationsOutputPort.deliver(exampleSet);
	}
}
