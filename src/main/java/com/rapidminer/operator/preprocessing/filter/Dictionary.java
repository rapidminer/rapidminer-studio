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
package com.rapidminer.operator.preprocessing.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.table.ViewAttribute;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.studio.internal.ProcessStoppedRuntimeException;
import com.rapidminer.tools.Tools;


/**
 * Replaces strings by interpreting the second example set as a dictionary. The
 *
 * @author Simon Fischer
 */
public class Dictionary extends PreprocessingModel {

	private static final long serialVersionUID = 1441613108993813785L;

	private static final int OPERATOR_PROGRESS_STEPS = 100;

	private List<String[]> replacements;
	private String[] affectedAttributeNames;
	private ExampleSet exampleSet;

	private transient Map<NominalMapping, Map<Double, Double>> viewReplacements;

	private boolean regexp = false;
	private boolean toLowerCase = false;

	private boolean stopAfterFirstMatch = false;

	public Dictionary(ExampleSet exampleSet, Set<Attribute> attributesAffected, List<String[]> replacements, boolean regexp,
			boolean toLowerCase, boolean stopAfterFistMatch) {
		super(exampleSet);
		this.exampleSet = exampleSet;
		this.stopAfterFirstMatch = stopAfterFistMatch;
		this.regexp = regexp;
		this.replacements = replacements;
		this.toLowerCase = toLowerCase;
		affectedAttributeNames = new String[attributesAffected.size()];
		int i = 0;
		for (Attribute attribute : attributesAffected) {
			affectedAttributeNames[i] = attribute.getName();
			i++;
		}
	}

	/**
	 * Remaps the attributes in the exampleSet or writes into the exampleSet if a remapping is not
	 * possible.
	 */
	private void remap(ExampleSet exampleSet) throws ProcessStoppedException {
		Attributes attributes = exampleSet.getAttributes();
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(affectedAttributeNames.length);
		}
		int progressCounter = 0;

		for (String attributeName : affectedAttributeNames) {
			Attribute attr = attributes.get(attributeName);
			if (attr.isNominal()) {
				NominalMapping mapping = attr.getMapping();
				List<String> mappingValues = mapping.getValues();

				for (String string : mappingValues) {
					String replacement = replace(string);
					int oldRepr = mapping.getIndex(string);
					// Nothing to replace
					if (replacement.equals(string)) {
						continue;
					}
					// Replacement already present in mapping -> Replace in example set
					else if (mappingValues.contains(replacement)) {
						for (Example example : exampleSet) {
							double oldValue = example.getValue(attr);
							if (Tools.isEqual(oldRepr, oldValue)) {
								int newRepr = mapping.getIndex(replacement);
								example.setValue(attr, newRepr);
							}
						}
					}
					// Replacement not present in mapping -> Replace in mapping
					else {
						mapping.setMapping(replacement, oldRepr);
					}
				}

			}
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}
	}

	/**
	 * Gives the attributes a new mapping if a remapping is possible or stores the value change in
	 * {@link #viewReplacements} and changes to {@link ViewAttribute}.
	 */
	private void remap(Attributes attributes) throws ProcessStoppedException {
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(affectedAttributeNames.length);
		}
		int progressCounter = 0;

		for (String attributeName : affectedAttributeNames) {
			Attribute attr = attributes.get(attributeName);
			if (attr.isNominal()) {
				NominalMapping mapping = attr.getMapping();
				List<String> mappingValues = mapping.getValues();
				boolean cloned = false;
				boolean putReplacement = false;

				for (String string : mappingValues) {
					String replacement = replace(string);
					// Nothing to replace
					if (replacement.equals(string)) {
						continue;
					}

					if (!cloned) {
						// mapping is not cloned on attribute clone so we have to clone it before we
						// change anything
						attr.setMapping((NominalMapping) mapping.clone());
						// get mapping from attributes again because it might be cloned when setting
						mapping = attr.getMapping();
						cloned = true;
					}

					int oldRepr = mapping.getIndex(string);

					// Replacement already present in mapping -> Replace value on the fly
					if (mappingValues.contains(replacement)) {
						if (viewReplacements == null) {
							viewReplacements = new HashMap<>();
						}

						Map<Double, Double> viewReplacer = viewReplacements.get(mapping);
						if (viewReplacer == null) {
							viewReplacer = new HashMap<>();
							viewReplacements.put(mapping, viewReplacer);
							putReplacement = true;
						}
						int newRepr = mapping.getIndex(replacement);
						viewReplacer.put((double) oldRepr, (double) newRepr);
					}
					// Replacement not present in mapping -> Replace in mapping
					else {
						mapping.setMapping(replacement, oldRepr);
					}
				}

				if (putReplacement) {
					// add view attribute to allow replacing on the fly via viewReplacements
					attributes.replace(attr, new ViewAttribute(this, attr, attr.getName(), attr.getValueType(), mapping));
				}

			}
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}
	}

	private String replace(String string) {
		if (toLowerCase) {
			string = string.toLowerCase();
		}
		for (String[] replacement : replacements) {
			if (regexp) {
				if (stopAfterFirstMatch) {
					if (string.matches(replacement[0])) {
						String newString = string.replaceAll(replacement[0], replacement[1]);
						return newString;
					}
				} else {
					string = string.replaceAll(replacement[0], replacement[1]);
				}
			} else {
				boolean foundMatch = false;
				StringBuilder soFar = new StringBuilder("");
				String remainder = string;
				while (true) {
					int pos = remainder.indexOf(replacement[0]);
					if (pos == -1) {
						break;
					}
					foundMatch = true;
					soFar.append(remainder.substring(0, pos));
					soFar.append(replacement[1]);
					remainder = remainder.substring(pos + replacement[0].length());
				}
				soFar.append(remainder);
				string = soFar.toString();
				if (foundMatch && stopAfterFirstMatch) {
					return string;
				}
			}
		}
		return string;
	}

	@Override
	public ExampleSet applyOnData(ExampleSet exampleSet) throws OperatorException {
		remap(exampleSet);
		return exampleSet;
	}

	@Override
	public Attributes getTargetAttributes(ExampleSet viewParent) {
		Attributes attributes = (Attributes) viewParent.getAttributes().clone();
		try {
			remap(attributes);
		} catch (ProcessStoppedException e) {
			throw new ProcessStoppedRuntimeException();
		}
		return attributes;
	}

	@Override
	public double getValue(Attribute targetAttribute, double value) {
		if (viewReplacements != null) {
			Map<Double, Double> replacers = viewReplacements.get(targetAttribute.getMapping());
			if (replacers != null) {
				Double replacement = replacers.get(value);
				if (replacement != null) {
					value = replacement;
				}
			}
		}
		return value;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (String[] replacement : replacements) {
			b.append(replacement[0]);
			b.append(" -> ");
			b.append(replacement[1]);
			b.append("\n");
		}
		return b.toString();
	}

	public List<String[]> getReplacements() {
		return replacements;
	}

	public String[] getAffectedAttributeNames() {
		return affectedAttributeNames;
	}

	public boolean isRegexp() {
		return regexp;
	}

	public boolean isToLowerCase() {
		return toLowerCase;
	}

	public boolean shouldStopAfterFirstMatch() {
		return stopAfterFirstMatch;
	}

	public ExampleSet getExampleSet() {
		return exampleSet;
	}

	@Override
	protected boolean needsRemapping() {
		return false;
	}

	@Override
	protected boolean writesIntoExistingData() {
		return true;
	}
}
