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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * <p>
 * This operator is a DB outlier detection algorithm which calculates the DB(p,D)-outliers for an
 * ExampleSet passed to the operator. DB(p,D)-outliers are Distance based outliers according to
 * Knorr and Ng. A DB(p,D)-outlier is an object to which at least a proportion of p of all objects
 * are farer away than distance D. It implements a global homogenous outlier search.
 * </p>
 *
 * <p>
 * Currently, the operator supports cosine, sine or squared distances in addition to the usual
 * euclidian distance which can be specified by the corresponding parameter. The operator takes two
 * other real-valued parameters p and D. Depending on these parameters, search objects will be
 * created from the examples in the ExampleSet passed to the operator. These search objects will be
 * added to a search space which will perform the outlier search according to the DB(p,D) scheme.
 * </p>
 *
 * <p>
 * The Outlier status (boolean in its nature) is written to a new special attribute
 * &quot;Outlier&quot; and is passed on with the example set.
 * </p>
 *
 * @author Stephan Deutsch, Ingo Mierswa
 */
public class DBOutlierOperator extends AbstractOutlierDetection {

	/** The parameter name for &quot;The distance for objects.&quot; */
	public static final String PARAMETER_DISTANCE = "distance";

	/** The parameter name for &quot;The proportion of objects related to D.&quot; */
	public static final String PARAMETER_PROPORTION = "proportion";

	/**
	 * The parameter name for &quot;Indicates which distance function will be used for calculating
	 * the distance between two objects&quot;
	 */
	public static final String PARAMETER_DISTANCE_FUNCTION = "distance_function";
	private static final String[] distanceFunctionList = { "euclidian distance", "squared distance", "cosine distance",
			"inverted cosine distance", "angle" };

	public DBOutlierOperator(OperatorDescription description) {
		super(description);
	}

	/**
	 * This method implements the main functionality of the Operator but can be considered as a sort
	 * of wrapper to pass the RapidMiner operator chain data deeper into the SearchSpace class, so
	 * do not expect a lot of things happening here.
	 */
	@Override
	public ExampleSet apply(ExampleSet eSet) throws OperatorException {
		// declaration and initializing the necessary fields from input
		double d = this.getParameterAsDouble(PARAMETER_DISTANCE);
		double p = this.getParameterAsDouble(PARAMETER_PROPORTION);
		int kindOfDistance = this.getParameterAsInt(PARAMETER_DISTANCE_FUNCTION);

		// create a new SearchSpace for the DB(p,D)-Outlier search
		Iterator<Example> reader = eSet.iterator();
		int searchSpaceDimension = eSet.getAttributes().size();
		SearchSpace sr = new SearchSpace(searchSpaceDimension);
		Attribute[] regularAttributes = eSet.getAttributes().createRegularAttributeArray();

		// now read through the Examples of the ExampleSet
		int counter = 0;
		while (reader.hasNext()) {
			Example example = reader.next();
			SearchObject so = new SearchObject(searchSpaceDimension, "object" + counter);
			counter++;
			int i = 0;
			for (Attribute attribute : regularAttributes) {
				so.setVektor(i++, example.getValue(attribute));
			}
			sr.addObject(so);
		}

		log("Searching d=" + sr.getDimensions() + " dimensions with D=" + d + " distance and p=" + p + " .");

		// set all Outlier Status to ZERO to be sure
		sr.resetOutlierStatus();

		// perform the DB(p,d)-Outlier search
		sr.allRadiusSearch(d, p, kindOfDistance);

		// create a new special attribute for the exampleSet
		Attribute outlierAttribute = AttributeFactory.createAttribute(Attributes.OUTLIER_NAME, Ontology.BINOMINAL);
		outlierAttribute.getMapping().mapString("false");
		outlierAttribute.getMapping().mapString("true");
		eSet.getExampleTable().addAttribute(outlierAttribute);
		eSet.getAttributes().setOutlier(outlierAttribute);

		counter = 0; // reset counter to zero
		Iterator<Example> reader2 = eSet.iterator();
		while (reader2.hasNext()) {
			Example example = reader2.next();
			if (sr.getSearchObjectOutlierStatus(counter) == true) {
				example.setValue(outlierAttribute, outlierAttribute.getMapping().mapString("true"));
			} else {
				example.setValue(outlierAttribute, outlierAttribute.getMapping().mapString("false"));
			}
			counter++;
		}

		return eSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeDouble(PARAMETER_DISTANCE, "The distance for objects.", 0,
				Double.POSITIVE_INFINITY);
		type.setOptional(false);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_PROPORTION, "The proportion of objects related to D.", 0, 1);
		type.setOptional(false);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeCategory(PARAMETER_DISTANCE_FUNCTION,
				"Indicates which distance function will be used for calculating the distance between two objects",
				distanceFunctionList, 0, false));
		return types;
	}

	@Override
	protected Set<String> getOutlierValues() {
		HashSet<String> set = new HashSet<String>();
		set.add("true");
		set.add("false");
		return set;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), DBOutlierOperator.class,
				null);
	}
}
