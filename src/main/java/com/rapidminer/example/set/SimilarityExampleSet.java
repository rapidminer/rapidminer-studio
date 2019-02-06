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
import com.rapidminer.example.SimpleAttributes;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.WrapperOperatorRuntimeException;
import com.rapidminer.operator.similarity.ExampleSet2SimilarityExampleSet;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;


/**
 * This similarity based example set is used for the operator
 * {@link ExampleSet2SimilarityExampleSet}.
 * 
 * @author Ingo Mierswa
 */
public class SimilarityExampleSet extends AbstractExampleSet {

	/**
	 * {@link InvocationHandler} for {@link ExampleTable} that can take care of
	 * {@link com.rapidminer.operator.execution.FlowCleaner} calls. Will throw a {@link UserError}
	 * for all other calls.
	 *
	 * @author Jan Czogalla
	 * @since 8.2
	 */
	private static class SimilarityHandler implements InvocationHandler {

		private static final String ATTRIBUTE_COUNT_METHOD_NAME = "getAttributeCount";

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals(ATTRIBUTE_COUNT_METHOD_NAME)) {
				return 0;
			}
			throw new WrapperOperatorRuntimeException(new UserError(null, "similarity_example_set_not_extendable"));
		}

	}

	private static final long serialVersionUID = 4757975818441794105L;

	private static class IndexExampleReader extends AbstractExampleReader {

		private int index = 0;

		private ExampleSet exampleSet;

		public IndexExampleReader(ExampleSet exampleSet) {
			this.exampleSet = exampleSet;
		}

		@Override
		public boolean hasNext() {
			return index < exampleSet.size() - 1;
		}

		@Override
		public Example next() {
			Example example = exampleSet.getExample(index);
			index++;
			return example;
		}
	}

	private ExampleSet parent;

	private Attribute parentIdAttribute;

	private Attributes attributes;

	private DistanceMeasure measure;

	public SimilarityExampleSet(ExampleSet parent, DistanceMeasure measure) {
		this.parent = parent;

		this.parentIdAttribute = parent.getAttributes().getId();
		this.attributes = new SimpleAttributes();

		Attribute firstIdAttribute = null;
		Attribute secondIdAttribute = null;
		firstIdAttribute = AttributeFactory.createAttribute("FIRST_ID", this.parentIdAttribute.getValueType());
		secondIdAttribute = AttributeFactory.createAttribute("SECOND_ID", this.parentIdAttribute.getValueType());

		this.attributes.addRegular(firstIdAttribute);
		this.attributes.addRegular(secondIdAttribute);
		firstIdAttribute.setTableIndex(0);
		secondIdAttribute.setTableIndex(1);

		// copying mapping of original id attribute
		if (parentIdAttribute.isNominal()) {
			NominalMapping mapping = parentIdAttribute.getMapping();
			firstIdAttribute.setMapping((NominalMapping) mapping.clone());
			secondIdAttribute.setMapping((NominalMapping) mapping.clone());
		}

		String name = "SIMILARITY";
		if (measure.isDistance()) {
			name = "DISTANCE";
		}

		Attribute similarityAttribute = AttributeFactory.createAttribute(name, Ontology.REAL);
		this.attributes.addRegular(similarityAttribute);
		similarityAttribute.setTableIndex(2);

		this.measure = measure;
	}

	/** Clone constructor. */
	public SimilarityExampleSet(SimilarityExampleSet exampleSet) {
		this.parent = (ExampleSet) exampleSet.parent.clone();
		this.parentIdAttribute = (Attribute) exampleSet.parentIdAttribute.clone();
		this.attributes = (Attributes) exampleSet.attributes.clone();
		this.measure = exampleSet.measure;
	}

	@Override
	public boolean equals(Object o) {
		if (!super.equals(o)) {
			return false;
		}
		if (!(o instanceof SimilarityExampleSet)) {
			return false;
		}

		SimilarityExampleSet other = (SimilarityExampleSet) o;
		if (!this.measure.getClass().equals(other.measure.getClass())) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ this.measure.getClass().hashCode();
	}

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}

	@Override
	public Annotations getAnnotations() {
		return parent.getAnnotations();
	}

	@Override
	public Example getExample(int index) {
		int firstIndex = index / this.parent.size();
		int secondIndex = index % this.parent.size();

		Example firstExample = this.parent.getExample(firstIndex);
		Example secondExample = this.parent.getExample(secondIndex);

		double[] data = new double[3];
		data[0] = firstExample.getValue(parentIdAttribute);
		data[1] = secondExample.getValue(parentIdAttribute);

		if (measure.isDistance()) {
			data[2] = measure.calculateDistance(firstExample, secondExample);
		} else {
			data[2] = measure.calculateSimilarity(firstExample, secondExample);
		}

		return new Example(new DoubleArrayDataRow(data), this);
	}

	@Override
	public Iterator<Example> iterator() {
		return new IndexExampleReader(this);
	}

	@Override
	public ExampleTable getExampleTable() {
		// return a proxy object so the flow cleaner is happy
		return (ExampleTable) Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class<?>[]{ExampleTable.class}, new SimilarityHandler());
	}

	@Override
	public int size() {
		return this.parent.size() * this.parent.size();
	}

	@Override
	public void cleanup() {
		parent.cleanup();
	}
}
