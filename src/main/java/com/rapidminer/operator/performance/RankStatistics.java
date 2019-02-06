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
package com.rapidminer.operator.performance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ConditionedExampleSet;
import com.rapidminer.example.set.NoMissingAttributesCondition;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.tools.ExpressionEvaluationException;


/**
 * Provides methods to compute ranks for a single attribute and rank correlations for two
 * attributes. When computing rank correlations, examples containing missing values for either
 * attribute are skipped. When computing ranks, missing values are given missing ranks. All methods
 * include an option to specify an imprecision tolerance when comparing values.
 *
 * @author Paul Rubin
 */
public class RankStatistics {

	/**
	 * Calculates the Spearman rank correlation between two attributes.
	 *
	 * @param eSet
	 *            the example set
	 * @param a
	 *            the first attribute to correlate
	 * @param b
	 *            the second attribute to correlate
	 * @param f
	 *            a fuzz factor (allowance for imprecision) when ranking
	 * @return the rank correlation
	 * @throws OperatorException
	 */
	public static double rho(ExampleSet eSet, Attribute a, Attribute b, double f) throws OperatorException {
		// create a new example set containing just attributes a and b
		ExampleSet e = extract(eSet, a, b);
		double[] ranka = rank(e, a, null, f);
		double[] rankb = rank(e, b, a, f);
		int nObs = ranka.length;
		double mu2 = nObs * (nObs + 1.0d) * (nObs + 1.0d) / 4.0d;
		// store rank products in a priority queue to sort them
		double prod = 0;
		double a2 = 0;
		double b2 = 0;
		for (int i = 0; i < nObs; i++) {
			prod += ranka[i] * rankb[i];
			a2 += ranka[i] * ranka[i];
			b2 += rankb[i] * rankb[i];
		}
		// return Spearman's rho
		double value = Math.sqrt((a2 - mu2) * (b2 - mu2));
		if (value != 0) {
			return (prod - mu2) / value;
		} else {
			return 0;
		}
	}

	/**
	 * Calculates the Spearman rank correlation between two attributes.
	 *
	 * @param eSet
	 *            the example set
	 * @param a
	 *            the first attribute to correlate
	 * @param b
	 *            the second attribute to correlate
	 * @return the rank correlation
	 * @throws OperatorException
	 */
	public static double rho(ExampleSet eSet, Attribute a, Attribute b) throws OperatorException {
		return rho(eSet, a, b, 0.0);
	}

	/**
	 * Calculates ranks for an attribute.
	 *
	 * Ranks are returned as double precision values, with 1 as the rank of the smallest value.
	 * Values within +/- fuzz of each other may be considered tied. Tied values receive identical
	 * ranks. Missing values receive rank NaN.
	 *
	 * Note that application of the "fuzz" factor is dependent on the order of the observations in
	 * the example set. For instance, if the first three values encountered are x, x+fuzz and
	 * x+2*fuzz, the first two will be considered tied but the third will not, since x+2*fuzz is not
	 * within +/- fuzz of x.
	 *
	 * @param eSet
	 *            the example set
	 * @param att
	 *            the attribute to rank
	 * @param fuzz
	 *            values within +/- fuzz may be considered tied
	 * @return a double precision array of ranks
	 */
	public static double[] rank(ExampleSet eSet, Attribute att, Attribute mappingAtt, double fuzz) {
		TreeMap<Double, ArrayList<Integer>> map;
		if (fuzz == 0.0) {
			map = new TreeMap<Double, ArrayList<Integer>>();
		} else {
			FuzzyComp fc = new FuzzyComp(fuzz);
			map = new TreeMap<Double, ArrayList<Integer>>(fc);
		}
		double[] rank = new double[eSet.size()];
		Iterator<Example> reader = eSet.iterator();
		int i = 0; // example index
		// iterate through the example set
		while (reader.hasNext()) {
			// get the attribute values from the next example
			Example e = reader.next();
			double x = e.getValue(att);
			if (att.isNominal() && mappingAtt != null) {
				String xString = att.getMapping().mapIndex((int) x);
				x = mappingAtt.getMapping().getIndex(xString);
			}
			// punt if either is missing
			if (Double.isNaN(x)) {
				rank[i++] = Double.NaN;
			} else {
				// insert x into the tree
				if (!map.containsKey(x)) {
					// new key -- create a new entry in the map
					map.put(x, new ArrayList<Integer>());
				}
				map.get(x).add(i++); // add the index to the list
			}
		}
		// convert the map to ranks
		double r = 0;
		for (double x : map.keySet()) {
			ArrayList<Integer> y = map.get(x);
			double v = r + (1.0 + y.size()) / 2.0;
			for (int j : y) {
				rank[j] = v;
			}
			r += y.size();
		}
		return rank;
	}

	/**
	 * Calculates ranks for an attribute.
	 *
	 * Ranks are returned as double precision values, with 1 as the rank of the smallest value. Tied
	 * values receive identical ranks. Missing values receive rank NaN.
	 *
	 * @param eSet
	 *            the example set
	 * @param att
	 *            the attribute to rank
	 * @param mappingAtt
	 *            the attribute which might be used for remapping the values
	 * @return a double precision array of ranks
	 */
	public static double[] rank(ExampleSet eSet, Attribute att, Attribute mappingAtt) {
		return rank(eSet, att, mappingAtt, 0.0);
	}

	/* Comparator for doubles using fuzz factor. */
	static class FuzzyComp implements Comparator<Double>, Serializable {

		private static final long serialVersionUID = -7752907616633799595L;

		private double fuzz; // comparison fuzz factor

		/* Constructor */
		FuzzyComp(double f) {
			fuzz = Math.abs(f);
		}

		@Override
		public int compare(Double x, Double y) {
			return x > y + fuzz ? 1 : x < y - fuzz ? -1 : 0;
		}
	}

	/*
	 * Extracts an example set containing just the two specified attributes and no missing values.
	 *
	 * @param eSet the source example set
	 *
	 * @param a the first attribute to extract
	 *
	 * @param b the second attribute to extract
	 *
	 * @return the reduced example set
	 */
	private static ExampleSet extract(ExampleSet eSet, Attribute a, Attribute b) {
		// create a new example set containing just attributes a and b
		ExampleSet e = (ExampleSet) eSet.clone();
		e.getAttributes().clearRegular();
		e.getAttributes().clearSpecial();
		e.getAttributes().addRegular(a);
		e.getAttributes().addRegular(b);
		try {
			return new ConditionedExampleSet(e, new NoMissingAttributesCondition(e, null));
		} catch (ExpressionEvaluationException e1) {
			// this should not happen for the NoMissingAttributesCondition
			return eSet;
		}
	}

	/**
	 * Computes Kendall's tau-b rank correlation statistic, ignoring examples containing missing
	 * values.
	 *
	 * @param eSet
	 *            the example set
	 * @param a
	 *            the first attribute to correlate
	 * @param b
	 *            the second attribute to correlate
	 * @return Kendall's tau-b rank correlation
	 * @throws OperatorException
	 */
	public static double tau_b(ExampleSet eSet, Attribute a, Attribute b) throws OperatorException {
		ExampleSet e = extract(eSet, a, b); // reduced example set
		long c = 0; // concordant pairs
		long d = 0; // discordant pairs
		long ta = 0; // pairs tied on a (only)
		long tb = 0; // pairs tied on b (only)
		int n = 0; // number of times iterator i is bumped
		Iterator<Example> i = e.iterator();
		while (i.hasNext()) {
			// iterate through all possible pairs
			Example z1 = i.next();
			n++;
			double x = z1.getValue(a);
			double y = z1.getValue(b);
			if (b.isNominal() && a != null) {
				String yString = b.getMapping().mapIndex((int) y);
				y = a.getMapping().getIndex(yString);
			}
			Iterator<Example> j = e.iterator();
			for (int k = 0; k < n; k++) {
				j.next(); // increment j to match i
			}
			while (j.hasNext()) {
				// move on to subsequent examples
				Example z2 = j.next();
				double xx = z2.getValue(a);
				double yy = z2.getValue(b);
				if (b.isNominal() && a != null) {
					String yyString = b.getMapping().mapIndex((int) yy);
					yy = a.getMapping().getIndex(yyString);
				}
				if (x == xx) {
					if (y == yy) {
						// tied on both attributes - noop
					} else {
						ta++; // tied only on a
					}
				} else if (y == yy) {
					tb++; // tied only on b
				} else if (x > xx && y > yy || x < xx && y < yy) {
					c++;
					// concordant pair
				} else {
					d++; // discordant pair
				}
			}
		}
		double num = c - d;
		double f1 = c + d + ta;
		double f2 = c + d + tb;
		double den = Math.sqrt(f1 * f2);
		if (den != 0) {
			return num / den;
		} else {
			return 0;
		}
	}

	/**
	 * Computes Kendall's tau-b rank correlation statistic, ignoring examples containing missing
	 * values, with approximate comparisons.
	 *
	 * @param eSet
	 *            the example set
	 * @param a
	 *            the first attribute to correlate
	 * @param b
	 *            the second attribute to correlate
	 * @param fuzz
	 *            values within +/- fuzz may be considered tied
	 * @return Kendall's tau-b rank correlation
	 * @throws OperatorException
	 */
	public static double tau_b(ExampleSet eSet, Attribute a, Attribute b, double fuzz) throws OperatorException {
		ExampleSet e = extract(eSet, a, b); // reduced example set
		FuzzyComp fc = new FuzzyComp(fuzz);
		int c = 0; // concordant pairs
		int d = 0; // discordant pairs
		int ta = 0; // pairs tied on a (only)
		int tb = 0; // pairs tied on b (only)
		int n = 0; // number of times iterator i is bumped
		Iterator<Example> i = e.iterator();
		while (i.hasNext()) {
			// iterate through all possible pairs
			Example z1 = i.next();
			n++;
			double x = z1.getValue(a);
			double y = z1.getValue(b);
			if (b.isNominal() && a != null) {
				String yString = b.getMapping().mapIndex((int) y);
				y = a.getMapping().getIndex(yString);
			}
			Iterator<Example> j = e.iterator();
			for (int k = 0; k < n; k++) {
				j.next(); // increment j to match i
			}
			while (j.hasNext()) {
				// move on to subsequent examples
				Example z2 = j.next();
				double xx = z2.getValue(a);
				double yy = z2.getValue(b);
				if (b.isNominal() && a != null) {
					String yyString = b.getMapping().mapIndex((int) yy);
					yy = a.getMapping().getIndex(yyString);
				}
				int xc = fc.compare(x, xx);
				int yc = fc.compare(y, yy);
				if (xc == 0) {
					if (yc == 0) {
						// tied on both attributes - noop
					} else {
						ta++; // tied only on a
					}
				} else if (yc == 0) {
					tb++; // tied only on b
				} else if (xc == yc) {
					c++; // concordant pair
				} else {
					d++; // discordant pair
				}
			}
		}
		double num = c - d;
		double den = Math.sqrt((c + d + ta) * (c + d + tb));
		if (den != 0) {
			return num / den;
		} else {
			return 0;
		}
	}
}
