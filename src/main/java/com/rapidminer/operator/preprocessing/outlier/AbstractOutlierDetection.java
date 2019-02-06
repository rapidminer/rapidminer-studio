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
package com.rapidminer.operator.preprocessing.outlier;

import com.rapidminer.example.Attributes;
import com.rapidminer.operator.AbstractExampleSetProcessing;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.Ontology;

import java.util.Set;


/**
 * Abstract superclass of outlier detection operators.
 * 
 * TODO: All subclasses generate outlier indicator attribute. Double-check this and add this to meta
 * data.
 * 
 * @author Simon Fischer
 */
public abstract class AbstractOutlierDetection extends AbstractExampleSetProcessing {

	public AbstractOutlierDetection(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		AttributeMetaData amd = new AttributeMetaData(Attributes.OUTLIER_NAME, Ontology.BINOMINAL, Attributes.OUTLIER_NAME);
		amd.setValueSet(getOutlierValues(), SetRelation.EQUAL);
		metaData.addAttribute(amd);
		return metaData;
	}

	protected abstract Set<String> getOutlierValues();

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}
}
