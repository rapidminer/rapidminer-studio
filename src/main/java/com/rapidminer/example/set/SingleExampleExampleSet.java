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
package com.rapidminer.example.set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.operator.Annotations;

import java.util.Iterator;


/**
 * This view can be used to wrap a single example.
 *
 * @author Ingo Mierswa
 */
public class SingleExampleExampleSet extends AbstractExampleSet {

	private static final long serialVersionUID = -4817219403776439504L;

	/** This class is used as a data iterator for the single example example set. */
	private static class SingleExampleIterator extends AbstractExampleReader {

		private boolean nextCalled = false;

		private DataRow dataRow;

		private SingleExampleExampleSet exampleSet;

		public SingleExampleIterator(DataRow dataRow, SingleExampleExampleSet exampleSet) {
			this.dataRow = dataRow;
			this.exampleSet = exampleSet;
		}

		@Override
		public boolean hasNext() {
			return !nextCalled;
		}

		@Override
		public Example next() {
			if (!nextCalled) {
				nextCalled = true;
				return new Example(dataRow, exampleSet);
			} else {
				return null;
			}
		}
	}

	private ExampleSet parent;

	private Example example;

	public SingleExampleExampleSet(ExampleSet exampleSet, Example example) {
		this.parent = (ExampleSet) exampleSet.clone();
		this.example = example;
	}

	public SingleExampleExampleSet(SingleExampleExampleSet exampleSet) {
		this.parent = (ExampleSet) exampleSet.parent.clone();
		this.example = exampleSet.example;
	}

	@Override
	public Iterator<Example> iterator() {
		return new SingleExampleIterator(example.getDataRow(), this);
	}

	@Override
	public Example getExample(int index) {
		if (index == 0) {
			return new Example(example.getDataRow(), this);
		} else {
			return null;
		}
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public Example getExampleFromId(double id) {
		Attribute idAttribute = getAttributes().getId();
		if (idAttribute != null) {
			Example newExample = new Example(example.getDataRow(), this);
			if (newExample.getValue(idAttribute) == id) {
				return newExample;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public Attributes getAttributes() {
		return parent.getAttributes();
	}

	@Override
	public Annotations getAnnotations() {
		return parent.getAnnotations();
	}

	@Override
	public ExampleTable getExampleTable() {
		return parent.getExampleTable();
	}

	@Override
	public void cleanup() {
		parent.cleanup();
	}
}
