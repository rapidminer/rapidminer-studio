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
package com.rapidminer.operator.features.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.AbstractModel;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Ontology;


/**
 * This class is completely unnecessary and is only kept for compatibility reasons. The class
 * hierarchy is complete nonsense and will be dropped with one of the next versions. So if you
 * implement using this class, please implement this little code fragment below again or build a
 * more fitting class hierarchy.
 *
 * The model for the generic dimensionality reducer.
 *
 * @author Ingo Mierswa
 */
@Deprecated
public class DimensionalityReducerModel extends AbstractModel {

	private static final long serialVersionUID = 1036161585615738268L;

	private int dimensions;

	private double[][] p;

	protected DimensionalityReducerModel(ExampleSet exampleSet, double[][] p, int dimensions) {
		super(exampleSet);
		this.p = p;
		this.dimensions = dimensions;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		List<Attribute> attributes = new ArrayList<Attribute>();
		for (int i = 0; i < dimensions; i++) {
			Attribute att = AttributeFactory.createAttribute("d" + i, Ontology.REAL);
			attributes.add(att);
		}

		Map<String, Attribute> newSpecialAttributes = new HashMap<String, Attribute>();
		Map<String, Attribute> oldSpecialAttributes = new HashMap<String, Attribute>();
		Iterator<AttributeRole> s = exampleSet.getAttributes().specialAttributes();
		while (s.hasNext()) {
			AttributeRole role = s.next();
			Attribute att = AttributeFactory.createAttribute(role.getAttribute());
			newSpecialAttributes.put(role.getSpecialName(), att);
			oldSpecialAttributes.put(role.getSpecialName(), role.getAttribute());
			attributes.add(att);
		}
		ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(exampleSet.size());

		// Apply build the instances
		int i = 0;
		for (Example oldExample : exampleSet) {
			DataRow row = new DoubleArrayDataRow(new double[attributes.size()]);
			for (int j = 0; j < dimensions; j++) {
				row.set(attributes.get(j), p[i][j]);
			}

			for (String specialAttributeRole : newSpecialAttributes.keySet()) {
				Attribute attribute = newSpecialAttributes.get(specialAttributeRole);
				row.set(attribute, oldExample.getValue(oldSpecialAttributes.get(specialAttributeRole)));
			}

			builder.addDataRow(row);
			i++;
		}

		ExampleSet resultSet = builder.build();
		// set special roles
		Attributes newAttributes = resultSet.getAttributes();
		for (Entry<String, Attribute> specialEntry : newSpecialAttributes.entrySet()) {
			newAttributes.setSpecialAttribute(specialEntry.getValue(), specialEntry.getKey());
		}

		return resultSet;
	}

	@Override
	public String getName() {
		return "Dimensionality Reduction";
	}

	@Override
	public String toString() {
		return "This model reduces the number of dimensions to " + dimensions + ".";
	}
}
