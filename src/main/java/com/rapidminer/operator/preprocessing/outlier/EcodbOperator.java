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
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;


/**
 * <p>
 * This operator performs a Class Outlier Factor (COF) search. COF outliers (or Class Outliers
 * method) search for observations (objects) those that arouse suspicions, taking into account the
 * class labels according to the definition of Class Outlier by Hewaihi and Saad in
 * "A comparative Study of Outlier Mining and Class Outlier Mining", CS Letters, Vol 1, No 1
 * (2009)", and "Class Outliers Mining: Distance-Based Approach
 * ", International Journal of Intelligent Systems and Technologies, Vol. 2, No. 1, pp 55-68, 2007".
 * </p>
 * <p>
 * It detects rare / exceptional / suspicious cases with respect group of similar cases. The main
 * key factors of computing COF are the probability of the instance's class among its neighbors's
 * classes, the deviation of the instance from the instances of the same class, and the distance
 * between the instance and its k nearest neighbors.
 * </p>
 * 
 * <p>
 * The main concept of ECODB (Enhanced Class Outlier - Distance Based) algorithm is to rank each
 * instance in the dataset D given the parameters N (top N class outliers), and K (the number of
 * nearest neighbors. The Rank finds out the rank of each instance using the formula (COF = PCL(T,K)
 * - norm(deviation(T)) + norm(kDist(T))). where PCL(T,K) is the Probability of the class label of
 * the instance T with respect to the class labels of its K Nearest Neighbors. and
 * norm(Deviation(T)) and norm(KDist(T)) are the normalized value of Deviation(T) and KDist(T)
 * respectively and their value fall into the range [0 - 1]. Deviation(T) is how much the instance T
 * deviates from instances of the same class, and computed by summing the distances between the
 * instance T and every instance belong to the same class of the instance. KDist(T) is the summation
 * of distances between the instance T and its K nearest neighbors.
 * </p>
 * 
 * <p>
 * The ECODB algorithm maintains a list of only the instances of the top N class outliers. The less
 * is the value of COF of an instance, the higher is the priority of the instance to be a class
 * outlier.
 * </p>
 * 
 * <p>
 * The operator supports mixed euclidian distance. The Operator takes an example set and passes it
 * on with an boolean top-n COF outlier status in a new boolean-valued special outlier attribute
 * indicating true (outlier) and false (no outlier), and another special attribute "COF Factor"
 * which measures the degree of being Class Outlier for an object.
 * </p>
 * 
 * @author Motaz K. Saad, Marius Helf
 */
public class EcodbOperator extends AbstractOutlierDetection {

	/**
	 * The parameter name for &quot;Specifies the k value for the k-th nearest neighbours to be the
	 * analyzed.&quot;
	 */
	public static final String PARAMETER_NUMBER_OF_NEIGHBORS = "number_of_neighbors";

	/** The parameter name for &quot;The number of top-n Class Outliers to be looked for.&quot; */
	public static final String PARAMETER_NUMBER_OF_Class_OUTLIERS = "number_of_class_outliers";

	public final static OperatorVersion VERSION_LOWERCASE_ATTRIBUTE_NAME = new OperatorVersion(5, 2, 6);

	private static final String COF_FACTOR_NAME = "COF Factor";

	public EcodbOperator(OperatorDescription description) {
		super(description);
	}

	/**
	 * This method implements the main functionality of the Operator but can be considered as a sort
	 * of wrapper to pass the RapidMiner operator chain data deeper into the search space class, so
	 * do not expect a lot of things happening here.
	 */
	@Override
	public ExampleSet apply(ExampleSet eSet) throws OperatorException {
		// check if the label attribute exists and is nominal
		Tools.hasNominalLabels(eSet, getOperatorClassName());

		// declaration and initializing the necessary fields from input
		int k = this.getParameterAsInt(PARAMETER_NUMBER_OF_NEIGHBORS);
		int n = this.getParameterAsInt(PARAMETER_NUMBER_OF_Class_OUTLIERS);

		// initialize distance measure
		DistanceMeasure measure = DistanceMeasures.createMeasure(this);
		measure.init(eSet);

		// create a new special attribute for the exampleSet
		String outlierAttributeName = Attributes.OUTLIER_NAME;
		if (getCompatibilityLevel().isAtMost(VERSION_LOWERCASE_ATTRIBUTE_NAME)) {
			// in oder version the attribute name started with an uppercase "O"
			outlierAttributeName = "Outlier";
		}
		Attribute outlierAttribute = AttributeFactory.createAttribute(outlierAttributeName, Ontology.BINOMINAL);

		// class outlier flag (true or false)
		outlierAttribute.getMapping().mapString("false");
		outlierAttribute.getMapping().mapString("true");
		eSet.getExampleTable().addAttribute(outlierAttribute);

		// class outlier factor (COF) attribute
		Attribute COFoutlierAttribute = AttributeFactory.createAttribute(COF_FACTOR_NAME, Ontology.REAL);
		eSet.getExampleTable().addAttribute(COFoutlierAttribute);

		// add these special attributes (outlier flag and class outlier factor attributes) to the
		// example set
		eSet.getAttributes().setOutlier(outlierAttribute);
		eSet.getAttributes().setSpecialAttribute(COFoutlierAttribute, COF_FACTOR_NAME);

		// reset all examples to positive infinity COF and class outlier flag to false
		Iterator<Example> reader = eSet.iterator();
		while (reader.hasNext()) {
			Example example = reader.next(); // read the next example & create a search object
			example.setValue(outlierAttribute, outlierAttribute.getMapping().mapString("false"));
			example.setValue(COFoutlierAttribute, Double.POSITIVE_INFINITY);
		}

		// finding attributes names
		ArrayList<String> sampleAttributeNames;

		Attributes attributes = eSet.getAttributes();
		sampleAttributeNames = new ArrayList<String>(attributes.size());
		for (Attribute attribute : attributes) {
			sampleAttributeNames.add(attribute.getName());
		}

		ArrayList<Attribute> sampleAttributes = new ArrayList<Attribute>(sampleAttributeNames.size());

		for (String attributeName : sampleAttributeNames) {
			sampleAttributes.add(attributes.get(attributeName));
		}

		// array list of COF objects that hold all dataset examples represented by double[] values
		// array
		ArrayList<COFObject> cofobjectList = new ArrayList<COFObject>();

		int counter = 0;

		// perform data transformation to double[] values
		// get double[] values for each example in the example set
		for (Example example : eSet) {
			double[] values = new double[sampleAttributes.size()];
			// reading values
			int i = 0;
			for (Attribute attribute : sampleAttributes) {
				values[i] = example.getValue(attribute);
				i++;
			}
			double label = example.getLabel();// get the label value
			// insert the cof object initialization in the list
			cofobjectList.add(new COFObject(values, label, Double.POSITIVE_INFINITY, counter++));
		}

		// define variables to hold max and min for Dev and kDist
		double maxDev, minDev;
		double maxkDist, minkDist;

		// initialize max and min for Dev and kDist
		maxkDist = Double.NEGATIVE_INFINITY;
		minkDist = Double.POSITIVE_INFINITY;
		maxDev = Double.NEGATIVE_INFINITY;
		minDev = Double.POSITIVE_INFINITY;

		// phase 1: compute cof value for all examples based on PCL
		for (COFObject cofobject : cofobjectList) {
			cofobject.computeCOF(cofobjectList, k, measure);

			// specify max and min for dev and Kdist
			double tempKdist = cofobject.getKDist();
			if (tempKdist > maxkDist) {
				maxkDist = tempKdist;
			}
			if (tempKdist < minkDist) {
				minkDist = tempKdist;
			}

			double tempDev = cofobject.getDeviation();
			if (tempDev > maxDev) {
				maxDev = tempDev;
			}
			if (tempDev < minDev) {
				minDev = tempDev;
			}

		}

		// priority queue of top n cof outliers
		PriorityQueue<COFObject> topCOFList = new PriorityQueue<COFObject>();
		for (COFObject cofobject : cofobjectList) {
			double cof = cofobject.getCOF();
			if (topCOFList.size() < n) {
				topCOFList.offer(cofobject);
			} else if (cof < topCOFList.peek().getCOF()) {
				topCOFList.remove();
				topCOFList.offer(cofobject);
			}
		}

		// phase 2: recompute COF based on PCL, normalized Dev, and normalized kDist
		for (COFObject cofobject : topCOFList) {
			cofobject.recomputeCOF(minDev, maxDev, minkDist, maxkDist);
		}

		// set outlier status and cof values in the example set
		for (COFObject cofobject : topCOFList) {
			Example example = eSet.getExample(cofobject.getId());
			example.setValue(outlierAttribute, outlierAttribute.getMapping().mapString("true"));
			example.setValue(COFoutlierAttribute, cofobject.getCOF());
		}

		return eSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(
				PARAMETER_NUMBER_OF_NEIGHBORS,
				"Specifies the k value for the k-th nearest neighbours to be the analyzed. (default value is 10, minimum 1 and max is set to 1 million)",
				1, Integer.MAX_VALUE, 7, false));
		types.add(new ParameterTypeInt(
				PARAMETER_NUMBER_OF_Class_OUTLIERS,
				"The number of top-n Class Outliers to be looked for.(default value is 10, minimum 2 (internal reasons) and max is set to 1 million)",
				1, Integer.MAX_VALUE, 10, false));
		types.addAll(DistanceMeasures.getParameterTypes(this));
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
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), EcodbOperator.class, null);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] oldChanges = super.getIncompatibleVersionChanges();
		OperatorVersion[] newChanges = { VERSION_LOWERCASE_ATTRIBUTE_NAME };
		OperatorVersion[] changes = new OperatorVersion[oldChanges.length + newChanges.length];

		int i = 0;
		for (OperatorVersion v : oldChanges) {
			changes[i] = v;
			++i;
		}
		for (OperatorVersion v : newChanges) {
			changes[i] = v;
			++i;
		}

		return changes;
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		metaData = (ExampleSetMetaData) super.modifyMetaData(metaData);

		// add COF Factor
		AttributeMetaData amd = new AttributeMetaData(COF_FACTOR_NAME, Ontology.REAL, COF_FACTOR_NAME);
		metaData.addAttribute(amd);
		return metaData;
	}
}
