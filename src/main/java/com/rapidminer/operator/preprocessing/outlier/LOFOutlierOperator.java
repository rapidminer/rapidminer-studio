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
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.math.container.Range;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * <p>
 * This operator performs a LOF outlier search. LOF outliers or outliers with a local outlier factor
 * per object are density based outliers according to Breuning, Kriegel, et al.
 * </p>
 * 
 * <p>
 * The approach to find those outliers is based on measuring the density of objects and its relation
 * to each other (referred to as local reachability density). Based on the average ratio of the
 * local reachability density of an object and its k-nearest neighbours (e.g. the objects in its
 * k-distance neighbourhood), a local outlier factor (LOF) is computed. The approach takes a
 * parameter MinPts (actually specifying the "k") and it uses the maximum LOFs for objects in a
 * MinPts range (lower bound and upper bound to MinPts).
 * </p>
 * 
 * <p>
 * Currently, the operator supports cosine, sine or squared distances in addition to the usual
 * euclidian distance which can be specified by the corresponding parameter. In the first step, the
 * objects are grouped into containers. For each object, using a radius screening of all other
 * objects, all the available distances between that object and another object (or group of objects)
 * on the (same) radius given by the distance are associated with a container. That container than
 * has the distance information as well as the list of objects within that distance (usually only a
 * few) and the information, how many objects are in the container.
 * </p>
 * 
 * <p>
 * In the second step, three things are done: (1) The containers for each object are counted in
 * acending order according to the cardinality of the object list within the container (= that
 * distance) to find the k-distances for each object and the objects in that k-distance (all objects
 * in all the subsequent containers with a smaller distance). (2) Using this information, the local
 * reachability densities are computed by using the maximum of the actual distance and the
 * k-distance for each object pair (object and objects in k-distance) and averaging it by the
 * cardinality of the k-neighbourhood and than taking the reciprocal value. (3) The LOF is computed
 * for each MinPts value in the range (actually for all up to upper bound) by averaging the ratio
 * between the MinPts-local reachability-density of all objects in the k-neighbourhood and the
 * object itself. The maximum LOF in the MinPts range is passed as final LOF to each object.
 * </p>
 * 
 * <p>
 * Afterwards LOFs are added as values for a special real-valued outlier attribute in the example
 * set which the operator will return.
 * </p>
 * 
 * @author Stephan Deutsch, Ingo Mierswa
 */
public class LOFOutlierOperator extends AbstractOutlierDetection {

	/** The parameter name for &quot;The lower bound for MinPts for the Outlier test &quot; */
	public static final String PARAMETER_MINIMAL_POINTS_LOWER_BOUND = "minimal_points_lower_bound";

	/** The parameter name for &quot;The upper bound for MinPts for the Outlier test &quot; */
	public static final String PARAMETER_MINIMAL_POINTS_UPPER_BOUND = "minimal_points_upper_bound";

	/**
	 * The parameter name for &quot;choose which distance function will be used for calculating
	 * &quot;
	 */
	public static final String PARAMETER_DISTANCE_FUNCTION = "distance_function";
	private static final String[] distanceFunctionList = { "euclidian distance", "squared distance", "cosine distance",
			"inverted cosine distance", "angle" };

	public LOFOutlierOperator(OperatorDescription description) {
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
		int minPtsLowerBound = 0;
		int minPtsUpperBound = 0;
		int minPtsLB = this.getParameterAsInt(PARAMETER_MINIMAL_POINTS_LOWER_BOUND);
		int minPtsUB = this.getParameterAsInt(PARAMETER_MINIMAL_POINTS_UPPER_BOUND);
		int kindOfDistance = this.getParameterAsInt(PARAMETER_DISTANCE_FUNCTION);

		// check for the sanity of entered parameters:
		if (minPtsLB <= minPtsUB) { // if lower bound is smaller or equal upper bound, pass them on
			minPtsLowerBound = minPtsLB;
			minPtsUpperBound = minPtsUB;
		} else { // else change both to have a sensible set of parameters ;-)
			minPtsLowerBound = minPtsUB;
			minPtsUpperBound = minPtsLB;
		}

		// create a new SearchSpace for the LOF-Outlier search
		Iterator<Example> reader = eSet.iterator();
		int searchSpaceDimension = eSet.getAttributes().size();
		SearchSpace sr = new SearchSpace(searchSpaceDimension, minPtsLowerBound, minPtsUpperBound + 1);
		Attribute[] regularAttributes = eSet.getAttributes().createRegularAttributeArray();

		// now read through the Examples of the ExampleSet
		int counter = 0;
		while (reader.hasNext()) {
			Example example = reader.next(); // read the next example & create a search object
			SearchObject so = new SearchObject(searchSpaceDimension, "object" + counter, minPtsLowerBound, minPtsUpperBound);
			// for now, give so an id like label and add the MinPts ranges, so that arrays are
			// initialized
			counter++;
			int i = 0;
			for (Attribute attribute : regularAttributes) {
				so.setVektor(i++, example.getValue(attribute));
			}
			sr.addObject(so); // finally add the search object to the search room
			checkForStop();
		}
		// set all Outlier Factors to ZERO to be sure
		sr.resetOutlierStatus();

		// find all Containers for the LOF first
		sr.findAllKdContainers(kindOfDistance, this);

		// perform the LOF-Outlier search
		sr.computeLOF(minPtsLowerBound, minPtsUpperBound, this);

		Attribute outlierAttribute = AttributeFactory.createAttribute(Attributes.OUTLIER_NAME, Ontology.REAL);
		eSet.getExampleTable().addAttribute(outlierAttribute);
		eSet.getAttributes().setOutlier(outlierAttribute);

		counter = 0; // reset counter to zero
		Iterator<Example> reader2 = eSet.iterator();
		while (reader2.hasNext()) {
			Example example = reader2.next(); // read the next example
			SearchObject sobj = sr.getSearchObjects().elementAt(counter);
			example.setValue(outlierAttribute, sobj.getOutlierFactor());
			counter++;
		}

		return eSet;
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		AttributeMetaData amd = new AttributeMetaData(Attributes.OUTLIER_NAME, Ontology.REAL, Attributes.OUTLIER_NAME);
		amd.setValueRange(new Range(0, 1), SetRelation.EQUAL);
		metaData.addAttribute(amd);
		return metaData;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeInt(PARAMETER_MINIMAL_POINTS_LOWER_BOUND,
				"The lower bound for MinPts for the Outlier test " + "(default value is 10)", 0, Integer.MAX_VALUE, 10);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_MINIMAL_POINTS_UPPER_BOUND, "The upper bound for MinPts for the Outlier test "
				+ "(default value is 20)", 0, Integer.MAX_VALUE, 20);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeCategory(PARAMETER_DISTANCE_FUNCTION,
				"choose which distance function will be used for calculating " + "the distance between two objects",
				distanceFunctionList, 0);
		type.setExpert(false);
		types.add(type);
		return types;
	}

	@Override
	/**
	 * Isn't called because super method of modifyMetaData is overridden.
	 */
	protected Set<String> getOutlierValues() {
		HashSet<String> set = new HashSet<>();
		set.add("true");
		set.add("false");
		return set;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), LOFOutlierOperator.class,
				null);
	}
}
