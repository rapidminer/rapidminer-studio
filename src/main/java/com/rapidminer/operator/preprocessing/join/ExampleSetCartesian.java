/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.operator.preprocessing.join;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;

import java.util.Iterator;
import java.util.List;


/**
 * <p>
 * Build the cartesian product of two example sets. In contrast to the {@link ExampleSetJoin}
 * operator, this operator does not depend on Id attributes. The result example set will consist of
 * the union set or the union list (depending on parameter setting double attributes will be removed
 * or renamed) of both feature sets. In case of removing double attribute the attribute values must
 * be the same for the examples of both example set, otherwise an exception will be thrown.
 * </p>
 * 
 * <p>
 * Please note that this check for double attributes will only be applied for regular attributes.
 * Special attributes of the second input example set which do not exist in the first example set
 * will simply be added. If they already exist they are simply skipped.
 * </p>
 * 
 * @author Peter B. Volk
 */
public class ExampleSetCartesian extends AbstractExampleSetJoin {

	public ExampleSetCartesian(OperatorDescription description) {
		super(description);
		getLeftInput().addPrecondition(new ExampleSetPrecondition(getLeftInput()));
		getRightInput().addPrecondition(new ExampleSetPrecondition(getRightInput()));
	}

	/**
	 * Joins the data WITHOUT a WHERE criteria.
	 * 
	 * @param es1
	 * @param es2
	 * @param originalAttributeSources
	 * @param unionAttributeList
	 * @return the table with the joined data
	 * @throws OperatorException
	 */
	@Override
	protected MemoryExampleTable joinData(ExampleSet es1, ExampleSet es2, List<AttributeSource> originalAttributeSources,
			List<Attribute> unionAttributeList) throws OperatorException {
		MemoryExampleTable unionTable = new MemoryExampleTable(unionAttributeList);
		Iterator<Example> reader = es1.iterator();
		while (reader.hasNext()) {
			Example example1 = reader.next();
			Iterator reader2 = es2.iterator();
			while (reader2.hasNext()) {
				Example example2 = (Example) reader2.next();
				double[] unionDataRow = new double[unionAttributeList.size()];
				Iterator<AttributeSource> a = originalAttributeSources.iterator();
				int index = 0;
				while (a.hasNext()) {
					AttributeSource source = a.next();
					if (source.getSource() == AttributeSource.FIRST_SOURCE) {
						unionDataRow[index] = example1.getValue(source.getAttribute());
					} else if (source.getSource() == AttributeSource.SECOND_SOURCE) {
						unionDataRow[index] = example2.getValue(source.getAttribute());
					}
					index++;
				}

				unionTable.addDataRow(new DoubleArrayDataRow(unionDataRow));
				checkForStop();
			}
		}

		return unionTable;
	}

	@Override
	protected boolean isIdNeeded() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPorts().getPortByIndex(0),
				ExampleSetCartesian.class, null);
	}
}
