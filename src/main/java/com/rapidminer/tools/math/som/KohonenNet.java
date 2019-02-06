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
package com.rapidminer.tools.math.som;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessStoppedException;


/**
 * This class can be used to train a Kohonen net.
 *
 * @author Sebastian Land
 */
public class KohonenNet implements Serializable {

	private static final long serialVersionUID = -5445606750204819559L;

	private long randomSeed = 19091982;

	private int netDimension;

	private int[] netDimensions;

	private int phase;

	private int trainingSteps = 80;

	private List<ProgressListener> progressListener = new ArrayList<>();

	private KohonenNode[] nodes;

	private DistanceFunction distanceFunction;

	private AdaptationFunction adaptationFunction;

	private KohonenTrainingsData data;

	private Random randomGenerator = new Random(randomSeed);

	private int cubeNodeCounter = 0;

	private int cubeEdgeLength = 0;

	private int[] cubeEdgeLengths;

	private int[] cubeOffset;

	private volatile boolean stopTraining = false;

	public KohonenNet(KohonenTrainingsData data) {
		this.distanceFunction = new EuclideanDistance();
		this.adaptationFunction = new RitterAdaptation();
		this.data = data;
	}

	public void init(int dataDimension, int[] netDimensions, boolean hexagonal) {
		// TODO
		// this.dataDimension = dataDimension;
		// if (netDimensions.length == 2) {
		// this.hexagonal = hexagonal;
		// } else if (netDimensions.length > 2) {
		// this.hexagonal = false;
		// }
		updateProgressListener(0);
		this.netDimension = netDimensions.length;
		this.netDimensions = netDimensions;

		// Calculats needed number of nodes
		int nodeNumber = 1;
		for (int i = 0; i < netDimension; i++) {
			nodeNumber *= netDimensions[i];
		}
		this.nodes = new KohonenNode[nodeNumber];

		// Generates nodes with random values
		double[] randomTupel = new double[dataDimension];
		for (int i = 0; i < nodes.length; i++) {
			for (int j = 0; j < dataDimension; j++) {
				randomTupel[j] = randomGenerator.nextDouble();
			}
			nodes[i] = new KohonenNode(randomTupel);
		}
		// Initiation was successfull
		phase = 1;
		updateProgressListener(10);
	}

	/**
	 * trains the KohonenNet
	 */
	public void train() {
		try {
			this.train(null);
		} catch (ProcessStoppedException e) {
			// this block is unreachable because we don't provide an Operator in this method
		}
	}

	/**
	 * Performs exactly the same action as {@link KohonenNet}.train() with the difference that this
	 * method checks whether the process was stopped.
	 *
	 * @param executingOperator
	 *            the Operator which executes this method
	 * @throws ProcessStoppedException
	 *             if the currently running Process was stopped
	 */
	public void train(Operator executingOperator) throws ProcessStoppedException {
		if (phase == 1) {
			this.stopTraining = false;
			data.setRandomGenerator(this.randomGenerator);
			for (int step = 1; step <= this.trainingSteps; step++) {
				updateProgressListener(10 + (step - 1) * 80 / trainingSteps);
				data.reset();
				int fittingNode = 0;
				// training over all examples
				// double[] exampleWeights = new double[0]; Unused. Shevek
				for (int example = 0; example < data.countData(); example++) {
					double[] exampleWeights = data.getNext();
					// getting coordinates in NodeNet of best fitting node
					fittingNode = getBestFittingNode(exampleWeights);
					int[] stimulusCoords = getCoordinatesOfIndex(fittingNode);
					// adapting every node in range to stimulus
					int range = 2 * (int) Math.round(adaptationFunction.getAdaptationRadius(null, step, trainingSteps));
					cube(range, stimulusCoords);
					while (cubeHasNext()) {
						// running over the number of nodes in the hypercube
						int currentNode = cubeNext();
						// calculating distance in net to stimulus
						double currentDistance = distanceFunction.getDistance(stimulusCoords,
								getCoordinatesOfIndex(currentNode), netDimensions);
						// adjusting weight of node
						nodes[currentNode].setWeights(adaptationFunction.adapt(exampleWeights,
								nodes[currentNode].getWeights(), currentDistance, step, trainingSteps));
					}
					if (executingOperator != null) {
						executingOperator.checkForStop();
					} else if (this.stopTraining) {
						throw new ProcessStoppedException();
					}
				}
			}
			// Training has been successful: data not needed anymore
			data = null;
			phase = 2;
			updateProgressListener(90);
			informProgressExit();
		}
	}

	private boolean cubeHasNext() {
		if (cubeNodeCounter < Math.pow(cubeEdgeLength, netDimension)) {
			return true;
		} else {
			return false;
		}

	}

	private int cubeNext() {
		if (cubeNodeCounter < Math.pow(cubeEdgeLength, netDimension)) {
			// Calculating relative position of node in hypercube
			int[] coordModifier = getCoordinatesOfIndex(cubeNodeCounter, cubeEdgeLengths);
			// shifting Hypercube, so that it's centered on the stimulus
			coordModifier = addArray(coordModifier, -cubeEdgeLength / 2);
			// adding relative Cube coordinates to absolut position of stimulus
			int[] currentCoord = addArrays(coordModifier, cubeOffset);
			// getting node index in array from absolute coords.
			cubeNodeCounter++;
			return getIndexOfCoordinates(currentCoord);
		} else {
			return -1;
		}

	}

	private void cube(int cubeEdgeLength, int[] offset) {
		cubeEdgeLengths = setArray(new int[netDimension], cubeEdgeLength);
		this.cubeEdgeLength = cubeEdgeLength;
		cubeOffset = offset;
		cubeNodeCounter = 0;
	}

	public int[] apply(double[] data) {
		if (phase == 2) {
			int bestNode = getBestFittingNode(data);
			return getCoordinatesOfIndex(bestNode);
		} else {
			return new int[] {};
		}
	}

	public void setRandomSeed(long seed) {
		if (phase == 0) {
			randomSeed = seed;
		}
	}

	public void setDistanceFunction(DistanceFunction function) {
		if (phase == 0) {
			this.distanceFunction = function;
		}
	}

	public void setAdaptationFunction(AdaptationFunction function) {
		if (phase == 0) {
			this.adaptationFunction = function;
		}
	}

	public void setTrainingRounds(int rounds) {
		this.trainingSteps = Math.max(rounds, 1);
	}

	public double getDistance(double[] point1, double[] point2) {
		return distanceFunction.getDistance(point1, point2);
	}

	public double[] getNodeWeights(int[] coords) {
		return nodes[getIndexOfCoordinates(coords)].getWeights();
	}

	public double getNodeDistance(int nodeIndex) {
		cube(3, getCoordinatesOfIndex(nodeIndex));
		double distance = 0;
		while (cubeHasNext()) {
			distance += distanceFunction.getDistance(nodes[nodeIndex].getWeights(), nodes[cubeNext()].getWeights());
		}
		return distance;
	}

	private int getBestFittingNode(double[] dataVector) {
		// initialising values
		double bestDistance = Double.POSITIVE_INFINITY;
		int best = -1;

		// searching for best fitting node
		for (int i = 0; i < nodes.length; i++) {
			double currentDistance = distanceFunction.getDistance(dataVector, nodes[i].getWeights());
			if (currentDistance < bestDistance) {
				best = i;
				bestDistance = currentDistance;
			}
		}
		return best;
	}

	private int[] getCoordinatesOfIndex(int index, int[] dimensions) {
		int[] coordinate = new int[dimensions.length];
		for (int i = 0; i < dimensions.length; i++) {
			coordinate[i] = index % dimensions[i];
			index = index / dimensions[i];
		}
		return coordinate;
	}

	private int[] getCoordinatesOfIndex(int index) {
		return getCoordinatesOfIndex(index, netDimensions);
	}

	public int getIndexOfCoordinates(int[] coordinates) {
		return getIndexOfCoordinates(coordinates, netDimensions);
	}

	private int getIndexOfCoordinates(int[] coordinates, int[] dimensions) {
		int index = 0;
		for (int i = dimensions.length - 1; i >= 0; i--) {
			if (coordinates[i] < 0) {
				coordinates[i] = dimensions[i] + coordinates[i];
			}
			index *= dimensions[i];
			index += Math.abs(coordinates[i] % dimensions[i]);
		}
		return index;
	}

	private int[] addArrays(int[] array, int[] adder) {
		if (array.length == adder.length) {
			for (int i = 0; i < array.length; i++) {
				array[i] += adder[i];
			}
		}
		return array;
	}

	private int[] addArray(int[] array, int adder) {
		for (int i = 0; i < array.length; i++) {
			array[i] += adder;
		}
		return array;
	}

	private int[] setArray(int[] array, int value) {
		for (int i = 0; i < array.length; i++) {
			array[i] = value;
		}
		return array;
	}

	public void addProgressListener(ProgressListener listener) {
		progressListener.add(listener);
	}

	public void removeProgressListener(ProgressListener listener) {
		progressListener.remove(listener);
	}

	public void updateProgressListener(int value) {
		Iterator<ProgressListener> iterator = progressListener.iterator();
		while (iterator.hasNext()) {
			iterator.next().setProgress(value);
		}
	}

	public void informProgressExit() {
		// we need to make a copy of the list, because progressFinsihed
		// may remove the listener from the progressListener list
		Iterator<ProgressListener> iterator = new ArrayList<>(progressListener).iterator();
		while (iterator.hasNext()) {
			iterator.next().progressFinished();
		}
	}

	/**
	 * If this Method is called the {@link KohonenNet} will stop the training as far as possible.
	 *
	 * NOTE: you can only use this method if train() is performed in another thread. Otherwise this
	 * method will have no effect.
	 */
	public void stopTrain() {
		this.stopTraining = true;
	}
}
