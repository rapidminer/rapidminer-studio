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
package com.rapidminer.operator.preprocessing.weighting;

import com.rapidminer.example.Attributes;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;


/**
 * Abstract superclass of operators adding a weight attribute.
 * 
 * @author Simon Fischer
 * 
 */
public abstract class AbstractExampleWeighting extends AbstractDataProcessing {

	public AbstractExampleWeighting(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		AttributeMetaData weightAttribute = new AttributeMetaData(Attributes.WEIGHT_NAME, Ontology.REAL,
				Attributes.WEIGHT_NAME);
		weightAttribute.setValueRange(getWeightAttributeRange(), getWeightAttributeValueRelation());
		metaData.addAttribute(weightAttribute);
		return metaData;
	}

	protected Range getWeightAttributeRange() {
		return new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	protected SetRelation getWeightAttributeValueRelation() {
		return SetRelation.UNKNOWN;
	}

}
