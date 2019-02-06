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
package com.rapidminer.operator.learner.associations;

import com.rapidminer.tools.Tools;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;


/**
 * <p>
 * An association rule which can be created from a frequent item set.
 * </p>
 * 
 * <p>
 * Note: this class has a natural ordering that is inconsistent with equals.
 * </p>
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class AssociationRule implements Serializable, Comparable<AssociationRule> {

	private static final long serialVersionUID = -4788528227281876533L;

	private double confidence, totalSupport, lift, laplace, gain, ps, conviction;

	private Collection<Item> premise;

	private Collection<Item> conclusion;

	public AssociationRule(Collection<Item> premise, Collection<Item> conclusion, double totalSupport) {
		this.premise = premise;
		this.conclusion = conclusion;
		this.totalSupport = totalSupport;
	}

	public double getGain() {
		return gain;
	}

	public void setGain(double gain) {
		this.gain = gain;
	}

	public double getConviction() {
		return conviction;
	}

	public void setConviction(double conviction) {
		this.conviction = conviction;
	}

	public double getLaplace() {
		return laplace;
	}

	public void setLaplace(double laplace) {
		this.laplace = laplace;
	}

	public double getLift() {
		return lift;
	}

	public void setLift(double lift) {
		this.lift = lift;
	}

	public double getPs() {
		return ps;
	}

	public void setPs(double ps) {
		this.ps = ps;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	public double getConfidence() {
		return this.confidence;
	}

	public double getTotalSupport() {
		return this.totalSupport;
	}

	public Iterator<Item> getPremiseItems() {
		return premise.iterator();
	}

	public Iterator<Item> getConclusionItems() {
		return conclusion.iterator();
	}

	public String toPremiseString() {
		return premise.toString();
	}

	public String toConclusionString() {
		return conclusion.toString();
	}

	@Override
	public int compareTo(AssociationRule o) {
		return Double.compare(this.confidence, o.confidence);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AssociationRule)) {
			return false;
		}
		AssociationRule other = (AssociationRule) o;
		return premise.toString().equals(other.premise.toString())
				&& conclusion.toString().equals(other.conclusion.toString()) && this.confidence == other.confidence;
	}

	@Override
	public int hashCode() {
		return premise.toString().hashCode() ^ conclusion.toString().hashCode() ^ Double.valueOf(this.confidence).hashCode();
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(premise.toString());
		buffer.append(" --> ");
		buffer.append(conclusion.toString());
		buffer.append(" (confidence: ");
		buffer.append(Tools.formatNumber(confidence));
		buffer.append(")");
		return buffer.toString();
	}
}
