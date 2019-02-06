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
package com.rapidminer.tools.metadata;

import com.rapidminer.example.Attributes;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;


/**
 * Tools class for handling meta data. Should contain analogs to the Tools class for handling
 * exampleSet.
 * 
 * @author Sebastian Land
 */
public class MetaDataTools {

	/**
	 * This method is the analogon to the checkAndCreateIds of the Tools class for meta data. It
	 * creates an integer id attribute with as much informations as available.
	 */
	public static void checkAndCreateIds(ExampleSetMetaData emd) {
		if (emd.getSpecial(Attributes.ID_NAME) == null) {
			AttributeMetaData idMD = new AttributeMetaData(Attributes.ID_NAME, Ontology.INTEGER, Attributes.ID_NAME);
			if (emd.getNumberOfExamples().isKnown()) {
				if (emd.getNumberOfExamples().getValue().doubleValue() > 1) {
					idMD.setValueRange(new Range(0, emd.getNumberOfExamples().getValue().doubleValue() - 1),
							SetRelation.EQUAL);
				} else {
					idMD.setValueRange(new Range(), SetRelation.EQUAL);
				}
			}
			emd.addAttribute(idMD);
		}
	}
}
