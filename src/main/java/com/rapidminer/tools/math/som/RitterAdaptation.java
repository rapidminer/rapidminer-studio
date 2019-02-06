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

/**
 * The RitterAdaptation provides an implementation of the AdaptationFunction interface for
 * calculation the adaption of a node to an input stimulus.
 * 
 * @author Sebastian Land
 */
public class RitterAdaptation implements AdaptationFunction {

	private static final long serialVersionUID = 254565250431806677L;

	private double learnRateStart = 0.8;

	private double learnRateEnd = 0.01;

	private double adaptationRadiusStart = 5;

	private double adaptationRadiusEnd = 1;

	private int lastTime = -1;

	private double learnRateCurrent;

	private double adaptationRadiusCurrent;

	@Override
	public double[] adapt(double[] stimulus, double[] nodeValue, double distanceFromImpact, int time, int maxtime) {
		// calculating time dependent variables only if time has changed
		if (lastTime != time) {
			lastTime = time;
			learnRateCurrent = learnRateStart
					* Math.pow((learnRateEnd / learnRateStart), (((double) time) / ((double) maxtime)));
			adaptationRadiusCurrent = getAdaptationRadius(time, maxtime);
		}
		double distanceWeightCurrent = Math.exp((-distanceFromImpact * distanceFromImpact)
				/ (2 * adaptationRadiusCurrent * adaptationRadiusCurrent));
		double weightNew[] = nodeValue.clone();
		if (distanceWeightCurrent > 0.5) {
			for (int i = 0; i < weightNew.length; i++) {
				if (!Double.isNaN(stimulus[i])) {
					weightNew[i] += learnRateCurrent * distanceWeightCurrent * (stimulus[i] - nodeValue[i]);
					if (weightNew[i] > 10) {
						weightNew[i] = weightNew[i];
					}
				}
			}
		}
		return weightNew;
	}

	@Override
	public double getAdaptationRadius(double[] stimulus, int time, int maxtime) {
		return getAdaptationRadius(time, maxtime);
	}

	private double getAdaptationRadius(int time, int maxtime) {
		return adaptationRadiusStart
				* Math.pow((adaptationRadiusEnd / adaptationRadiusStart), (((double) time) / ((double) maxtime)));
	}

	public void setAdaptationRadiusStart(double start) {
		this.adaptationRadiusStart = start;
	}

	public void setAdaptationRadiusEnd(double end) {
		this.adaptationRadiusEnd = end;
	}

	public void setLearnRateStart(double start) {
		this.learnRateStart = start;
	}

	public void setLearnRateEnd(double end) {
		this.learnRateEnd = end;
	}
}
