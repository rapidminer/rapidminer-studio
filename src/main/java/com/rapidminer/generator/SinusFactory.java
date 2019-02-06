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
package com.rapidminer.generator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.math.BinaryPeakFinder;
import com.rapidminer.tools.math.Complex;
import com.rapidminer.tools.math.FastFourierTransform;
import com.rapidminer.tools.math.Peak;
import com.rapidminer.tools.math.PeakFinder;
import com.rapidminer.tools.math.SpectrumFilter;
import com.rapidminer.tools.math.WindowFunction;


/**
 * Factory class to produce new attributes based on the fourier synthesis of the label mapped on an
 * attribute dimension.
 *
 * @author Ingo Mierswa
 */
public class SinusFactory {

	/** Indicates the min evidence factor. */
	private static final double MIN_EVIDENCE = 0.2d;

	public static final String[] ADAPTION_TYPES = { "uniformly", "uniformly_without_nu", "gaussian" };

	public static final int UNIFORMLY = 0;

	public static final int UNIFORMLY_WITHOUT_NU = 1;

	public static final int GAUSSIAN = 2;

	/**
	 * Generates this number of peaks in a range of <code>epsilon * frequency</code>. Necessary
	 * because the FT does not deliver the correct frequency (aliasing, leakage) in all cases. In
	 * later releases this should be replaced by a gradient search or a evolutionary search for the
	 * correct value.
	 */
	private int attributesPerPeak = 3;

	/**
	 * Generates this <code>peaksPerPeak</code> peaks in the range of
	 * <code>epsilon * frequency</code>. Necessary because the FT does not deliver the correct
	 * frequency (aliasing, leakage) in all cases. In later releases this should be replaced by a
	 * gradient search or a evolutionary search for the correct value.
	 */
	private double epsilon = 0.1;

	/** Indicates the type of frequency adaption. */
	private int adaptionType = UNIFORMLY;

	/**
	 * The maximal number of generated attributes for each possible attribute. Corresponds to the
	 * highest peaks in the frequency spectrum of the label in the source attribute's space.
	 */
	private int maxPeaks = 5;

	/** The fast fourier transformation calculator. */
	private FastFourierTransform fft = null;

	/**
	 * The spectrum filter type which should be applied on the spectrum after the fourier
	 * transformation.
	 */
	private SpectrumFilter filter = null;

	/** The algorithm to find the peaks in the frequency spectrum. */
	private PeakFinder peakFinder = null;

	/**
	 * Creates a new sinus factory which creates <code>maxPeaks</code> new peaks. Uses
	 * Blackman-Harris window function and no spectrum filter as default. The adaption type is
	 * gaussian with an epsilon of 0.1. The factory produces three attributes for each highest peak
	 * as default.
	 */
	public SinusFactory(int maxPeaks) {
		this.maxPeaks = maxPeaks;
		this.fft = new FastFourierTransform(WindowFunction.BLACKMAN_HARRIS);
		this.filter = new SpectrumFilter(SpectrumFilter.NONE);
		this.peakFinder = new BinaryPeakFinder();
	}

	public void setAdaptionType(int type) {
		this.adaptionType = type;
	}

	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	/** Must be bigger than 2! */
	public void setAttributePerPeak(int attributesPerPeak) {
		this.attributesPerPeak = attributesPerPeak;
	}

	/**
	 * Calculates the fourier transformation from the first attribute on the second and delivers the
	 * <code>maxPeaks</code> highest peaks. Returns a list with the highest attribute peaks.
	 */
	public List<AttributePeak> getAttributePeaks(ExampleSet exampleSet, Attribute first, Attribute second)
			throws OperatorException {
		exampleSet.recalculateAllAttributeStatistics();
		Complex[] result = fft.getFourierTransform(exampleSet, first, second);
		Peak[] spectrum = filter.filter(result, exampleSet.size());
		double average = 0.0d;
		for (int k = 0; k < spectrum.length; k++) {
			average += spectrum[k].getMagnitude();
		}
		average /= spectrum.length;
		List<Peak> peaks = peakFinder.getPeaks(spectrum);
		Collections.sort(peaks);
		if (maxPeaks < peaks.size()) {
			peaks = peaks.subList(0, maxPeaks);
		}

		// remember highest peaks
		double inputDeviation = Math.sqrt(exampleSet.getStatistics(second, Statistics.VARIANCE))
				/ (exampleSet.getStatistics(second, Statistics.MAXIMUM)
						- exampleSet.getStatistics(second, Statistics.MINIMUM));
		double maxEvidence = Double.NaN;
		List<AttributePeak> attributes = new LinkedList<AttributePeak>();
		for (Peak peak : peaks) {
			double evidence = peak.getMagnitude() / average * (1.0d / inputDeviation);
			if (Double.isNaN(maxEvidence)) {
				maxEvidence = evidence;
			}
			if (evidence > MIN_EVIDENCE * maxEvidence) {
				attributes.add(new AttributePeak(second, peak.getIndex(), evidence));
			}
		}
		return attributes;
	}

	/**
	 * Generates a new sinus function attribute for all given attribute peaks. Since the frequency
	 * cannot be calculated exactly (leakage, aliasing), several new attribute may be added for each
	 * peak. These additional attributes are randomly chosen (uniformly in epsilon range, uniformly
	 * without nu, gaussian with epsilon as standard deviation)
	 */
	public void generateSinusFunctions(ExampleSet exampleSet, List<AttributePeak> attributes, Random random)
			throws GenerationException {
		if (attributes.isEmpty()) {
			return;
		}
		Collections.sort(attributes);
		double totalMaxEvidence = attributes.get(0).getEvidence();

		for (AttributePeak ae : attributes) {
			if (ae.getEvidence() > MIN_EVIDENCE * totalMaxEvidence) {
				for (int i = 0; i < attributesPerPeak; i++) {
					double frequency = ae.getFrequency();
					switch (adaptionType) {
						case UNIFORMLY:
							if (attributesPerPeak != 1) {
								frequency = (double) i / (double) (attributesPerPeak - 1) * 2.0d * epsilon * frequency
										+ (frequency - epsilon * frequency);
							}
							break;
						case UNIFORMLY_WITHOUT_NU:
							if (attributesPerPeak != 1) {
								frequency = (double) i / (double) (attributesPerPeak - 1) * 2.0d * epsilon
										+ (frequency - epsilon);
							}
							break;
						case GAUSSIAN:
							frequency = random.nextGaussian() * epsilon + frequency;
							break;
					}

					// frequency constant
					List<Attribute> frequencyResult = generateAttribute(exampleSet, new ConstantGenerator(frequency));

					// scaling with frequency
					FeatureGenerator scale = new BasicArithmeticOperationGenerator(
							BasicArithmeticOperationGenerator.PRODUCT);
					scale.setArguments(new Attribute[] { frequencyResult.get(0), ae.getAttribute() });
					List<Attribute> scaleResult = generateAttribute(exampleSet, scale);

					// calc sin
					FeatureGenerator sin = new TrigonometricFunctionGenerator(TrigonometricFunctionGenerator.SINUS);
					sin.setArguments(new Attribute[] { scaleResult.get(0) });
					List<Attribute> sinResult = generateAttribute(exampleSet, sin);
					for (Attribute attribute : sinResult) {
						exampleSet.getAttributes().addRegular(attribute);
					}
				}
			}
		}
	}

	private List<Attribute> generateAttribute(ExampleSet exampleSet, FeatureGenerator generator) throws GenerationException {
		List<FeatureGenerator> generators = new LinkedList<FeatureGenerator>();
		generators.add(generator);
		return FeatureGenerator.generateAll(exampleSet.getExampleTable(), generators);
	}
}
