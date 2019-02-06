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

import com.rapidminer.tools.math.similarity.DistanceMeasure;

import java.util.ArrayList;
import java.util.PriorityQueue;


/**
 * The COF class creates COF Objects which handle the representation of objects from the test data
 * set in the core of the class outlier operators. Such an object is able to store all relevant
 * values, coordinates, dimensions, etc. for an object (e.g. from an Example from a RapidMiner
 * ExampleSet) as well as perform various operations, such as computeCOF, and ComputeDistance
 * 
 * @author Motaz K. Saad
 */
public class COFObject implements Comparable<COFObject> {

	double[] values;
	double cof;
	int id;
	double pcl;
	double deviation;
	double kDist;
	double label;

	public COFObject() {
		this.cof = Double.POSITIVE_INFINITY;
		this.id = -1;
	}

	public COFObject(double[] vals, double label, double cof, int id) {
		this.cof = cof;
		this.id = id;
		this.values = vals;
		this.label = label;
	}

	/**
	 * @return the values
	 */
	public double[] getValues() {
		return values;
	}

	/**
	 * @param values
	 *            the values to set
	 */
	public void setValues(double[] values) {
		this.values = values;
	}

	/**
	 * @return the value[dim]
	 */
	public double getValMember(int dim) {
		return this.values[dim];
	}

	/**
	 * Provides the (integer) number of dimensions of the Object. Remark: some methods actually use
	 * the this.dimensions reference which is used by this, but this method would be able to provide
	 * the dimensions externally.
	 */
	public int getDimensions() {
		return (this.values.length);
	}

	/**
	 * @return the cof
	 */
	public double getCOF() {
		return cof;
	}

	/**
	 * @param cof
	 *            the cof to set
	 */
	public void setCOF(double cof) {
		this.cof = cof;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the this.id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the deviation
	 */
	public double getDeviation() {
		return deviation;
	}

	/**
	 * @param deviation
	 *            the deviation to set
	 */
	public void setDeviation(double deviation) {
		this.deviation = deviation;
	}

	/**
	 * @return the kDist
	 */
	public double getKDist() {
		return kDist;
	}

	/**
	 * @param dist
	 *            the kDist to set
	 */
	public void setKDist(double dist) {
		kDist = dist;
	}

	/**
	 * @return the label
	 */
	public double getLabel() {
		return label;
	}

	/**
	 * @param label
	 *            the label to set
	 */
	public void setLabel(double label) {
		this.label = label;
	}

	/**
	 * @return the pcl
	 */
	public double getPcl() {
		return pcl;
	}

	/**
	 * @param pcl
	 *            the pcl to set
	 */
	public void setPcl(double pcl) {
		this.pcl = pcl;
	}

	@Override
	public int compareTo(COFObject arg0) {

		if (this.cof < arg0.getCOF()) {
			return 1;
		}
		if (this.cof > arg0.getCOF()) {
			return -1;
		} else {
			return 0;
		}
	}

	public void recomputeCOF(double minDev, double maxDev, double minkDist, double maxkDist) {
		cof = pcl - ((deviation - minDev) / (maxDev - minDev)) + ((kDist - minkDist) / (maxkDist - minkDist));
	}

	public void computeCOF(ArrayList<COFObject> cofobjectList, int k, DistanceMeasure measure) {

		// define a list of knn for each cof object
		PriorityQueue<COFKnn> knnList = new PriorityQueue<COFKnn>();

		// reset pcl, kDist, and deviation
		double pcl = 0.0;
		double kDist = 0.0;
		double deviation = 0.0;

		for (COFObject cofobject : cofobjectList) {// for all objects in the dataset
			double distance = Double.POSITIVE_INFINITY;
			// compute the distance to current object
			distance = measure.calculateDistance(this.getValues(), cofobject.getValues());
			COFKnn cOFKnn = new COFKnn(cofobject, distance);
			// determine if cofobject is on of the nearest neighbors to current object
			if (knnList.size() < k) {
				knnList.offer(cOFKnn);
			} else if (distance < knnList.peek().getDistance()) {
				knnList.remove();
				knnList.offer(cOFKnn);
			}
			// if the cofobject has the same class label, add its distance to deviation
			if (this.getLabel() == cofobject.getLabel()) {
				deviation += distance;
			}

		}
		this.setDeviation(deviation); // save deviation

		// compute pcl to current object
		for (COFKnn cofKnn : knnList) {
			kDist += measure.calculateDistance(getValues(), cofKnn.getCofobject().getValues());
			if (this.getLabel() == cofKnn.getCofobject().getLabel()) {
				pcl++;
			}
		}

		this.setPcl(pcl); // save pcl
		this.setCOF(pcl); // save the initial cof based on pcl
		this.setKDist(kDist); // save kDist

	}
}
