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
package com.rapidminer.operator.preprocessing.filter.attributes;

import java.text.Collator;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.features.selection.AbstractFeatureSelection;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttributeOrderingRules;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Tools;


/**
 * This Operator is capable of sorting attributes of an {@link ExampleSet}. Either alphabetically or
 * by user specified ordering roles.
 *
 * @author Nils Woehler
 *
 */
public class AttributeOrderingOperator extends AbstractFeatureSelection {

	private abstract class FilterConditon {

		public abstract boolean match(String rule, String value);
	}

	private static final String REFERENCE_DATA_PORT_NAME = "reference_data";

	// --------------------- Order method ---------------------------------
	public static final String PARAMETER_ORDER_MODE = "sort_mode";

	public static final String USER_SPECIFIED_RULES_MODE = "user specified";
	public static final String ALPHABETICALLY_MODE = "alphabetically";
	public static final String REFERENCE_DATA = "reference data";

	public static final String[] SORT_MODES = new String[] { USER_SPECIFIED_RULES_MODE, ALPHABETICALLY_MODE,
			REFERENCE_DATA };
	public static final int USER_SPECIFIED_RULES_MODE_INDEX = 0;
	public static final int ALPHABETICALLY_MODE_INDEX = 1;
	public static final int REFERENCE_DATA_INDEX = 2;

	// --------------------- Sort direction -------------------------------
	public static final String PARAMETER_SORT_DIRECTION = "sort_direction";

	public static final String DIRECTION_ASCENDING = "ascending";
	public static final String DIRECTION_DESCENDING = "descending";
	public static final String DIRECTION_NONE = "none";

	public static final String[] SORT_DIRECTIONS = new String[] { DIRECTION_ASCENDING, DIRECTION_DESCENDING,
			DIRECTION_NONE };
	public static final int DIRECTION_ASCENDING_INDEX = 0;
	public static final int DIRECTION_DESCENDING_INDEX = 1;
	public static final int DIRECTION_NONE_INDEX = 2;

	// --------------------- Others ---------------------------------------

	public static final String PARAMETER_ORDER_RULES = "attribute_ordering";
	public static final String PARAMETER_USE_REGEXP = "use_regular_expressions";

	public static final String PARAMETER_HANDLE_UNMATCHED_ATTRIBUTES = "handle_unmatched";

	public static final String REMOVE_UNMATCHED_MODE = "remove";
	public static final String PREPEND_UNMATCHED_MODE = "prepend";
	public static final String APPEND_UNMATCHED_MODE = "append";

	public static final String[] HANDLE_UNMATCHED_MODES = { REMOVE_UNMATCHED_MODE, PREPEND_UNMATCHED_MODE,
			APPEND_UNMATCHED_MODE };

	public static final int REMOVE_UNMATCHED_MODE_INDEX = 0;
	public static final int PREPEND_UNMATCHED_MODE_INDEX = 1;
	public static final int APPEND_UNMATCHED_MODE_INDEX = 2;

	private final InputPort referenceDataPort = getInputPorts().createPort(REFERENCE_DATA_PORT_NAME);

	/**
	 * @param description
	 */
	public AttributeOrderingOperator(OperatorDescription description) {
		super(description);

		referenceDataPort.addPrecondition(new SimplePrecondition(referenceDataPort, new MetaData(ExampleSet.class), false));

		getTransformer().addRule(new MDTransformationRule() {

			@Override
			public void transformMD() {
				MetaData md1 = getInputPorts().getPortByIndex(0).getMetaData();
				OutputPort outputPort = getOutputPorts().getPortByIndex(0);

				try {
					if (getParameterAsString(PARAMETER_ORDER_MODE).equals(ALPHABETICALLY_MODE)) {
						outputPort.deliverMD(md1); // no attributes will be removed, just deliver
						// old MD
						return;
					}

					FilterConditon condition = null;
					List<String> rules = new LinkedList<>();

					if (getParameterAsString(PARAMETER_ORDER_MODE).equals(REFERENCE_DATA)) {
						MetaData refMD = getInputPorts().getPortByName(REFERENCE_DATA_PORT_NAME).getMetaData();
						if (refMD == null || !(refMD instanceof ExampleSetMetaData)) {
							outputPort.deliverMD(new ExampleSetMetaData());
							return;
						}
						ExampleSetMetaData refEsMD = (ExampleSetMetaData) refMD;
						List<AttributeMetaData> allReferenceAttributes = new LinkedList<>(refEsMD.getAllAttributes());

						// add all attributes that are not special to rules list
						for (AttributeMetaData amd : allReferenceAttributes) {
							if (!amd.isSpecial()) {
								rules.add(amd.getName());
							}
						}

						condition = new FilterConditon() {

							@Override
							public boolean match(String rule, String value) {
								return value.equals(rule);
							}
						};

					}

					if (getParameterAsString(PARAMETER_ORDER_MODE).equals(USER_SPECIFIED_RULES_MODE)) {
						String combinedMaskedRules = getParameterAsString(PARAMETER_ORDER_RULES);

						// if parameter is empty just return old meta data
						if (combinedMaskedRules == null || combinedMaskedRules.length() == 0) {
							outputPort.deliverMD(new ExampleSetMetaData());
							return;
						}

						String[] splittedRules = combinedMaskedRules.split("\\|");

						// unmask rules
						for (int i = 0; i < splittedRules.length; i++) {
							rules.add(Tools.unmask('|', splittedRules[i]));
						}

						condition = new FilterConditon() {

							@Override
							public boolean match(String rule, String value) {
								try {
									return getParameterAsBoolean(PARAMETER_USE_REGEXP) ? value.matches(rule)
											: value.equals(rule);
								} catch (PatternSyntaxException e) {
									return false;
								}
							}
						};

					}

					// calculate new meta data
					ExampleSetMetaData sortedEmd = applyRulesOnMetaData(rules, md1, condition);

					outputPort.deliverMD(sortedEmd);

				} catch (UndefinedParameterError e) {
					outputPort.deliverMD(new ExampleSetMetaData());
				}
			}

		});
	}

	private ExampleSetMetaData applyRulesOnMetaData(List<String> rules, MetaData metaData, FilterConditon condition)
			throws UndefinedParameterError {
		if (metaData == null || !(metaData instanceof ExampleSetMetaData) || condition == null) {
			return new ExampleSetMetaData();
		}
		ExampleSetMetaData sortedMetaData = new ExampleSetMetaData();
		ExampleSetMetaData originalMetaData = (ExampleSetMetaData) metaData;
		Collection<AttributeMetaData> allAttributes = originalMetaData.getAllAttributes();

		// iterate over all rules
		for (String currentRule : rules) {

			// iterate over all original attributes and check if rule applies
			Iterator<AttributeMetaData> iterator = allAttributes.iterator();
			while (iterator.hasNext()) {
				AttributeMetaData attrMD = iterator.next();

				// skip special attributes
				if (attrMD.isSpecial()) {
					continue;
				}

				// if rule applies, remove attribute from unmachted list and add it to rules matched
				// list
				if (condition.match(currentRule, attrMD.getName())) {
					iterator.remove();
					sortedMetaData.addAttribute(attrMD);
				}
			}

		}

		if (!getParameterAsString(PARAMETER_HANDLE_UNMATCHED_ATTRIBUTES).equals(REMOVE_UNMATCHED_MODE)) {
			sortedMetaData.addAllAttributes(allAttributes);
		}

		return sortedMetaData;
	}

	@Override
	protected void performAdditionalChecks() {
		super.performAdditionalChecks();
		try {
			InputPort referenceDataPort = getInputPorts().getPortByName(REFERENCE_DATA_PORT_NAME);
			String orderMode = getParameterAsString(PARAMETER_ORDER_MODE);
			if (orderMode.equals(REFERENCE_DATA) && !referenceDataPort.isConnected()) {
				addError(new SimpleMetaDataError(Severity.ERROR, referenceDataPort, "input_missing",
						REFERENCE_DATA_PORT_NAME));
			}
			if (!orderMode.equals(REFERENCE_DATA) && referenceDataPort.isConnected()) {
				addError(new SimpleMetaDataError(Severity.WARNING, referenceDataPort, "port_connected_but_parameter_not_set",
						REFERENCE_DATA_PORT_NAME, PARAMETER_ORDER_MODE, orderMode));
			}
		} catch (UndefinedParameterError e) {
			// nothing to do here
		}
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {

		if (exampleSet == null) {
			throw new UserError(this, 149, getInputPorts().getPortByIndex(0).getName());
		}

		// get unmachted attributes
		Attributes attributes = exampleSet.getAttributes();
		List<Attribute> unmachtedAttributes = getAttributeList(attributes);

		if (getParameterAsString(PARAMETER_ORDER_MODE).equals(ALPHABETICALLY_MODE)) {

			if (getParameterAsString(PARAMETER_SORT_DIRECTION).equals(DIRECTION_NONE)) {
				return exampleSet;
			}

			// sort attributes
			sortAttributeListAlphabetically(unmachtedAttributes);

			// apply sorted attributes
			applySortedAttributes(unmachtedAttributes, null, attributes);

		} else if (getParameterAsString(PARAMETER_ORDER_MODE).equals(USER_SPECIFIED_RULES_MODE)
				|| getParameterAsString(PARAMETER_ORDER_MODE).equals(REFERENCE_DATA)) {
			List<Attribute> sortedAttributes = new LinkedList<>();

			if (getParameterAsString(PARAMETER_ORDER_MODE).equals(REFERENCE_DATA)) {
				InputPort referencePort = getInputPorts().getPortByName(REFERENCE_DATA_PORT_NAME);
				ExampleSet referenceSet = referencePort.getData(ExampleSet.class);

				if (referenceSet == null) {
					throw new UserError(this, 149, referencePort.getName());
				}

				// iterate over reference attributes and order unmachted attributes accordingly
				for (Attribute refAttr : referenceSet.getAttributes()) {
					// System.out.println("Check attribute " + refAttr.getName());
					Iterator<Attribute> iterator = unmachtedAttributes.iterator();
					while (iterator.hasNext()) {
						Attribute unmachtedAttr = iterator.next();
						if (refAttr.getName().equals(unmachtedAttr.getName())) {

							// only pairwise matching is possible -> directly add attribute to
							// sorted list
							sortedAttributes.add(unmachtedAttr);
							// System.out.println("Added unmachted attribute to list: " +
							// unmachtedAttr.getName());

							// remove attribute from unmachted attributes
							iterator.remove();
						}
					}
				}
			} else {
				boolean useRegexp = getParameterAsBoolean(PARAMETER_USE_REGEXP);
				String combinedMaskedRules = getParameterAsString(PARAMETER_ORDER_RULES);
				if (combinedMaskedRules == null || combinedMaskedRules.length() == 0) {
					throw new UndefinedParameterError(PARAMETER_ORDER_RULES, this);
				}

				// iterate over all rules
				for (String maskedRule : combinedMaskedRules.split("\\|")) {
					String rule = Tools.unmask('|', maskedRule); // unmask them to allow regexp
					List<Attribute> matchedAttributes = new LinkedList<>();

					// iterate over all attributes and check if rules apply
					Iterator<Attribute> iterator = unmachtedAttributes.iterator();
					while (iterator.hasNext()) {
						Attribute attr = iterator.next();
						boolean match = false;
						if (useRegexp) {
							try {
								if (attr.getName().matches(rule)) {
									match = true;
								}
							} catch (PatternSyntaxException e) {
								throw new UserError(this, 206, rule, e.getMessage());
							}
						} else {
							if (attr.getName().equals(rule)) {
								match = true;
							}
						}

						// if rule applies remove attribute from unmachted list and add it to rules
						// matched list
						if (match) {
							iterator.remove();
							matchedAttributes.add(attr);
						}
					}

					// sort matched attributes according to sort direction if more then one match
					// has been found
					if (matchedAttributes.size() > 1) {
						sortAttributeListAlphabetically(matchedAttributes);
					}

					// add matched attributes to sorted attribute list
					sortedAttributes.addAll(matchedAttributes);

				}
			}

			/*
			 * UNMACHTED Handling
			 */

			if (!getParameterAsString(PARAMETER_HANDLE_UNMATCHED_ATTRIBUTES).equals(REMOVE_UNMATCHED_MODE)) {
				// sort unmachted attributes according to sort direction
				sortAttributeListAlphabetically(unmachtedAttributes);

				if (getParameterAsString(PARAMETER_HANDLE_UNMATCHED_ATTRIBUTES).equals(PREPEND_UNMATCHED_MODE)) {
					// prepend attributes to ordered attributes list
					sortedAttributes.addAll(0, unmachtedAttributes);
				} else {
					// append attributes to ordered attributes list
					sortedAttributes.addAll(unmachtedAttributes);
				}

				applySortedAttributes(sortedAttributes, null, attributes);

			} else {
				applySortedAttributes(sortedAttributes, unmachtedAttributes, attributes);
			}

		} else {
			throw new IllegalArgumentException(
					"Order mode " + getParameterAsString(PARAMETER_ORDER_MODE) + " is not implemented!");
		}
		return exampleSet;
	}

	private List<Attribute> getAttributeList(Attributes attributes) {
		List<Attribute> attributeList = new LinkedList<>();
		for (Attribute attr : attributes) {
			attributeList.add(attr);
		}
		return attributeList;
	}

	/**
	 * Applies the sorted and unmachted attribute list to the provided {@link Attributes}. All
	 * unmachted attributes are removed from attributes and all {@link Attribute}s from the sorted
	 * list are added in correct order.
	 *
	 * @param sortedAttributeList
	 *            attributes that will be removed first and added in correct order afterwards.
	 * @param unmachtedAttributes
	 *            attributes that should be removed. May be <code>null</code> if no attributes
	 *            should be removed.
	 */
	private void applySortedAttributes(List<Attribute> sortedAttributeList, List<Attribute> unmachtedAttributes,
			Attributes attributes) {
		if (unmachtedAttributes != null) {
			for (Attribute unmachted : unmachtedAttributes) {
				attributes.remove(unmachted);
			}
		}

		for (Attribute attribute : sortedAttributeList) {
			AttributeRole role = attributes.getRole(attribute);
			attributes.remove(attribute);

			if (role.isSpecial()) {
				attributes.setSpecialAttribute(attribute, role.getSpecialName());
			} else { // regular
				attributes.addRegular(attribute);
			}
		}
	}

	/**
	 * Sorts a list of attributes alphabetically according to the desired sort direction. CAUTION:
	 * The provided list 'unsortedAttributeList' will be changed internally.
	 */
	private void sortAttributeListAlphabetically(List<Attribute> unsortedAttributeList) throws UndefinedParameterError {

		// sort direction none -> just return
		if (getParameterAsString(PARAMETER_SORT_DIRECTION).equals(DIRECTION_NONE)) {
			return;
		}

		// sort attributes
		Collections.sort(unsortedAttributeList, new Comparator<Attribute>() {

			@Override
			public int compare(Attribute o1, Attribute o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}

		});

		// if descending, reverse sort
		if (getParameterAsString(PARAMETER_SORT_DIRECTION).equals(DIRECTION_DESCENDING)) {
			Collections.reverse(unsortedAttributeList);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> parameterTypes = super.getParameterTypes();

		ParameterType type = new ParameterTypeCategory(PARAMETER_ORDER_MODE, "Ordering method that should be applied.",
				SORT_MODES, USER_SPECIFIED_RULES_MODE_INDEX, false);
		parameterTypes.add(type);

		// --------------------------- USER SPECIFIED -------------------------

		type = new ParameterTypeAttributeOrderingRules(PARAMETER_ORDER_RULES, "Rules to order attributes.",
				getInputPorts().getPortByIndex(0), true);
		type.setExpert(false);
		type.setPrimary(true);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_ORDER_MODE, SORT_MODES, true, USER_SPECIFIED_RULES_MODE_INDEX));
		parameterTypes.add(type);

		type = new ParameterTypeBoolean(PARAMETER_USE_REGEXP,
				"If checked attribute orders will be evaluated as regular expressions.", false, true);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_ORDER_MODE, SORT_MODES, true, USER_SPECIFIED_RULES_MODE_INDEX));
		parameterTypes.add(type);

		type = new ParameterTypeCategory(PARAMETER_HANDLE_UNMATCHED_ATTRIBUTES,
				"Defines the behavior for unmatched attributes.", HANDLE_UNMATCHED_MODES, APPEND_UNMATCHED_MODE_INDEX,
				false);
		type.setOptional(true);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_ORDER_MODE, SORT_MODES, false,
				USER_SPECIFIED_RULES_MODE_INDEX, REFERENCE_DATA_INDEX));
		parameterTypes.add(type);

		type = new ParameterTypeCategory(PARAMETER_SORT_DIRECTION, "Sort direction for attribute names.", SORT_DIRECTIONS,
				DIRECTION_ASCENDING_INDEX, false);
		parameterTypes.add(type);

		return parameterTypes;
	}

	@Override
	public boolean shouldAutoConnect(InputPort inputPort) {
		if (inputPort == referenceDataPort) {
			return false;
		}
		return super.shouldAutoConnect(inputPort);
	}
}
