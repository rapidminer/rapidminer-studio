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
package com.rapidminer.tools.math;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.report.Readable;
import com.rapidminer.tools.Tools;


/**
 * Superclass for all objects which can be averaged. Averagable objects can be stored in a average
 * vector.
 * 
 * @author Ingo Mierswa
 */
public abstract class Averagable extends ResultObjectAdapter implements Cloneable, Readable {

	private static final long serialVersionUID = 3193522429555690641L;

	/** The averages are summed up each time buildAverage is called. */
	private double meanSum;

	/** The squared averages are summed up each time buildAverage is called. */
	private double meanSquaredSum;

	/** Counts the number of times, build average was executed. */
	private int averageCount;

	public Averagable() {
		this.meanSum = Double.NaN;
		this.meanSquaredSum = Double.NaN;
		this.averageCount = 0;
	}

	public Averagable(Averagable o) {
		this.meanSum = o.meanSum;
		this.meanSquaredSum = o.meanSquaredSum;
		this.averageCount = o.averageCount;
	}

	/**
	 * Returns the name of this averagable. The returned string should only contain lowercase
	 * letters and underscore (RapidMiner parameter format) since the names will be automatically
	 * used for GUI purposes.
	 */
	@Override
	public abstract String getName();

	/**
	 * Returns the (current) value of the averagable (the average itself). If the method
	 * {@link #buildSingleAverage(Averagable)} was used, this method must return the micro average
	 * from both (or more) criteria. This is usually achieved by correctly implementing
	 * {@link #buildSingleAverage(Averagable)}.
	 */
	public abstract double getMikroAverage();

	/**
	 * Returns the variance of the averagable. The returned value must not be negative. If the
	 * averagable does not define a variance this method should return Double.NaN.
	 */
	public abstract double getMikroVariance();

	/**
	 * Must be implemented by subclasses such that it copies all values of <code>other</code> to
	 * <code>this</code>. When this method is called, it is guaranteed, that <code>other</code> is a
	 * subclass of the class of the object it is called on.
	 * 
	 * @deprecated Please use copy constructors instead
	 */
	@Deprecated
	protected final void cloneAveragable(Averagable other) {}

	/**
	 * This method should build the average of this and another averagable of the same type. The
	 * next invocation of {@link #getMikroAverage()} should return the average of this and the given
	 * averagable. Hence, this method is used to build the actual micro average value of two
	 * criteria. Please refer to {@link com.rapidminer.operator.performance.SimpleCriterion} for a
	 * simple implementation example.
	 */
	protected abstract void buildSingleAverage(Averagable averagable);

	// ================================================================================

	/**
	 * This method builds the macro average of two averagables of the same type. First this method
	 * checks if the classes of <code>this</code> and <code>performance</code> are the same and if
	 * the {@link #getName()} methods return the same String. Otherwise a RuntimeException is
	 * thrown. <br>
	 * The value of <code>averagable.</code>{@link #getMikroAverage()} is added to {@link #meanSum},
	 * its square is added to {@link #meanSquaredSum} and {@link #averageCount} is increased by one.
	 * These values are used in the {@link #getMakroAverage()} and {@link #getMakroVariance()}
	 * methods. <br>
	 * Subclasses should implement the method buildSingleAverage() to build the micro (weighted)
	 * average of <code>this</code> averagable and the given <code>averagable</code>. They must be
	 * weighted by the number of examples used for calculating the averagables.
	 */
	public final void buildAverage(Averagable averagable) {
		if (!averagable.getClass().equals(this.getClass())) {
			throw new RuntimeException("Cannot build average of different averagable types (" + this.getClass().getName()
					+ "/" + averagable.getClass().getName() + ").");
		}
		if (!averagable.getName().equals(this.getName())) {
			throw new RuntimeException("Cannot build average of different averagable types (" + this.getName() + "/"
					+ averagable.getName() + ").");
		}

		if (averageCount == 0) { // count yourself
			double value = this.getMikroAverage();
			meanSum = value;
			meanSquaredSum = value * value;
			averageCount = 1;
		}
		double value = averagable.getMikroAverage();
		meanSum += value;
		meanSquaredSum += value * value;
		averageCount++;

		buildSingleAverage(averagable);
	}

	/**
	 * This method returns the macro average if it was defined and the micro average (the current
	 * value) otherwise. This method should be used instead of {@link #getMikroAverage()} for
	 * optimization purposes, i.e. by methods like <code>getFitness()</code> of performance
	 * criteria.
	 */
	public final double getAverage() {
		double average = Double.NaN;
		if (averageCount > 0) {
			average = getMakroAverage();
		}
		if (Double.isNaN(average)) {
			average = getMikroAverage();
		}
		return average;
	}

	/**
	 * This method returns the macro variance if it was defined and the micro variance otherwise.
	 */
	public final double getVariance() {
		double variance = Double.NaN;
		if (averageCount > 0) {
			variance = getMakroVariance();
		}
		if (Double.isNaN(variance)) {
			variance = getMikroVariance();
		}
		return variance;
	}

	/**
	 * This method returns the macro standard deviation if it was defined and the micro standard
	 * deviation otherwise.
	 */
	public final double getStandardDeviation() {
		double sd = Double.NaN;
		if (averageCount > 0) {
			sd = getMakroStandardDeviation();
		}
		if (Double.isNaN(sd)) {
			sd = getMikroStandardDeviation();
		}
		return sd;
	}

	/** Returns the standard deviation of the performance criterion. */
	public final double getMikroStandardDeviation() {
		double variance = getMikroVariance();
		if (Double.isNaN(variance)) {
			return Double.NaN;
		} else if (variance < 0.0d) {
			return 0.0d;
		} else {
			return Math.sqrt(variance);
		}
	}

	/**
	 * Returns the average value of all performance criteria average by using the
	 * {@link #buildAverage(Averagable)} method.
	 */
	public final double getMakroAverage() {
		return meanSum / averageCount;
	}

	/**
	 * Returns the variance of all performance criteria average by using the
	 * {@link #buildAverage(Averagable)} method.
	 */
	public final double getMakroVariance() {
		if (averageCount < 2) {
			return Double.NaN;
		}
		double besselsCorrection = averageCount / (averageCount - 1.0d);
		double biasedVariance = meanSquaredSum / averageCount - Math.pow(getMakroAverage(), 2);
		double unbiasedVariance = besselsCorrection * biasedVariance;
		if (unbiasedVariance < 0.0d) {
			return 0.0d;
		}
		return unbiasedVariance;
	}

	/**
	 * Returns the standard deviation of all performance criteria average by using the
	 * {@link #buildAverage(Averagable)} method.
	 */
	public final double getMakroStandardDeviation() {
		return Math.sqrt(getMakroVariance());
	}

	/** Returns the number of averagables used to create this averagable. */
	public int getAverageCount() {
		return this.averageCount;
	}

	/** Returns a (deep) clone of this averagable. */
	@Override
	public Object clone() throws CloneNotSupportedException {
		try {
			Class<? extends Averagable> clazz = this.getClass();
			Constructor<? extends Averagable> cloneConstructor = clazz.getConstructor(clazz);
			Averagable result = cloneConstructor.newInstance(this);
			return result;
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
			throw new CloneNotSupportedException("Cannot clone averagable: " + e.getMessage());
		}
	}

	// ================================================================================

	/**
	 * Indicates wether or not percentage format should be used in the {@link #toString} method. The
	 * default implementation returns false.
	 */
	public boolean formatPercent() {
		return false;
	}

	/** Formats the value for the {@link #toString()} method. */
	private final String formatValue(double value) {
		if (Double.isNaN(value)) {
			return "unknown";
		} else {
			if (formatPercent()) {
				return Tools.formatPercent(value);
			} else {
				return Tools.formatNumber(value);
			}
		}
	}

	/** Formats the standard deviation for the {@link #toString()} method. */
	private final String formatDeviation(double dev) {
		if (formatPercent()) {
			return Tools.formatPercent(dev);
		} else {
			return Tools.formatNumber(dev);
		}
	}

	public String getExtension() {
		return "avg";
	}

	public String getFileDescription() {
		return "averagable";
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(getName() + ": ");
		boolean makroUsable = false;
		if (averageCount > 0) {
			double makroAverage = getMakroAverage();
			if (!Double.isNaN(makroAverage)) {
				makroUsable = true;
				result.append(formatValue(makroAverage));
				double sd = getMakroStandardDeviation();
				if (!Double.isNaN(sd)) {
					result.append(" +/- " + formatDeviation(sd));
				}
			}
		}

		if (makroUsable) {
			result.append(" (micro average: ");
		}

		result.append(formatValue(getMikroAverage()));
		double sd = getMikroStandardDeviation();
		if (!Double.isNaN(sd)) {
			result.append(" +/- " + formatDeviation(sd));
		}

		if (makroUsable) {
			result.append(")");
		}

		return result.toString();
	}

	@Override
	public boolean isInTargetEncoding() {
		return false;
	}

	public void setAverageCount(int averageCount) {
		this.averageCount = averageCount;
	}
}
