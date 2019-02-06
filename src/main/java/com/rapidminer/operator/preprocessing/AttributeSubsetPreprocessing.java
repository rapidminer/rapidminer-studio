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
package com.rapidminer.operator.preprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.example.set.NonSpecialAttributesExampleSet;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeSubsetPassThroughRule;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.ProcessTools;


/**
 * <p>
 * This operator can be used to select one attribute (or a subset) by defining a regular expression
 * for the attribute name and applies its inner operators to the resulting subset. Please note that
 * this operator will also use special attributes which makes it necessary for all preprocessing
 * steps which should be performed on special attributes (and are normally not performed on special
 * attributes).
 * </p>
 * 
 * <p>
 * This operator is also able to deliver the additional results of the inner operator if desired.
 * </p>
 * 
 * <p>
 * Afterwards, the remaining original attributes are added to the resulting example set if the
 * parameter &quot;keep_subset_only&quot; is set to false (default).
 * </p>
 * 
 * <p>
 * Please note that this operator is very powerful and can be used to create new preprocessing
 * schemes by combining it with other preprocessing operators. However, there are two major
 * restrictions (among some others): first, since the inner result will be combined with the rest of
 * the input example set, the number of examples (data points) is not allowed to be changed inside
 * of the subset preprocessing. Second, attribute role changes will not be delivered to the outside
 * since internally all special attributes will be changed to regular for the inner operators and
 * role changes can afterwards not be delivered.
 * </p>
 * 
 * @author Ingo Mierswa, Shevek
 */
public class AttributeSubsetPreprocessing extends OperatorChain {

	/**
	 * The parameter name for &quot;Indicates if the additional results (other than example set) of
	 * the inner operator should also be returned.&quot;
	 */
	public static final String PARAMETER_DELIVER_INNER_RESULTS = "deliver_inner_results";

	/**
	 * The parameter name for &quot;Indicates if the attributes which did not match the regular
	 * expression should be removed by this operator.&quot;
	 */
	public static final String PARAMETER_KEEP_SUBSET_ONLY = "keep_subset_only";

	/** The parameter name for &quot;Indicates how to handle with doubling of Attributenames */
	public static final String PARAMETER_ROLE_CONFLICT_HANDLING = "role_conflict_handling";
	private static final String[] HANDLE_ROLE_CONFLICT_MODES = { "error", "keep new", "keep original" };
	private static final int HANDLE_ROLE_CONFLICT_ERROR = 0;
	private static final int HANDLE_ROLE_CONFLICT_KEEP_NEW = 1;
	private static final int HANDLE_ROLE_CONFLICT_KEEP_ORIGINAL = 2;

	public static final String PARAMETER_NAME_CONFLICT_HANDLING = "name_conflict_handling";
	private static final String[] HANDLE_NAME_CONFLICT_MODES = { "error", "keep new", "keep original" };
	private static final int HANDLE_NAME_CONFLICT_ERROR = 0;
	private static final int HANDLE_NAME_CONFLICT_KEEP_NEW = 1;
	private static final int HANDLE_NAME_CONFLICT_KEEP_ORIGINAL = 2;

	public static final String PARAMETER_REMOVE_ROLES = "remove_roles";

	private final InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private final OutputPort innerExampleSetSource = getSubprocess(0).getInnerSources().createPort("exampleSet");
	private final InputPort innerExampleSetSink = getSubprocess(0).getInnerSinks().createPort("example set",
			ExampleSet.class);
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private final PortPairExtender innerResultPorts = new PortPairExtender("through", getSubprocess(0).getInnerSinks(),
			getOutputPorts());

	private final AttributeSubsetSelector attributeSelector = new AttributeSubsetSelector(this, exampleSetInput);

	public AttributeSubsetPreprocessing(OperatorDescription description) {
		super(description, "Subset Process");
		getTransformer().addRule(new AttributeSubsetPassThroughRule(exampleSetInput, innerExampleSetSource, this, false) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				// checking if condition creation works
				try {
					AttributeSubsetSelector.createCondition(
							operator.getParameterAsString(AttributeSubsetSelector.PARAMETER_FILTER_TYPE), operator);
				} catch (UndefinedParameterError e) {
					// a standard error is already thrown
				} catch (ConditionCreationException e) {
					try {
						operator.addError(new SimpleProcessSetupError(Severity.ERROR, operator.getPortOwner(),
								"attribute_filter_condition_error", operator
										.getParameterAsString(AttributeSubsetSelector.PARAMETER_FILTER_TYPE)));
					} catch (UndefinedParameterError e1) {
						// a standard error is already thrown
					}
				}

				ExampleSetMetaData result;
				if (!AttributeSubsetPreprocessing.this.isRemoveRolesSet()) {
					List<AttributeMetaData> specialAttributes = this.seperateSpecialAttributeMetaData(metaData);
					result = selector.getMetaDataSubset(metaData, keepSpecialIfNotIncluded);
					this.restoreSpecialRoles(specialAttributes, result);
				} else {
					result = selector.getMetaDataSubset(metaData, keepSpecialIfNotIncluded);
				}

				return result;
			}

			/**
			 * Separates all AttributeMetaData-Objects with a special role
			 * 
			 * @param metaData
			 *            ExampleSetMetaData to separate from
			 * @return a List with all AttributeMetaData with a special role
			 */
			private List<AttributeMetaData> seperateSpecialAttributeMetaData(ExampleSetMetaData metaData) {
				ArrayList<AttributeMetaData> specialStuff = new ArrayList<AttributeMetaData>();
				for (AttributeMetaData att : metaData.getAllAttributes()) {
					if (att.isSpecial()) {
						specialStuff.add(att);
					}
				}
				return specialStuff;
			}

			/**
			 * changes the role of the AttributeAttribute in exempleSet(with the same name as a
			 * Attribute in specialAttributes) to the role of the AttributeMetaData in
			 * specialAttributes
			 * 
			 * @param specialAttributes
			 *            list of AttributeMetaData with special roles
			 * @param exampleSet
			 *            ExampleSetMetaData with will be modified
			 */
			private void restoreSpecialRoles(List<AttributeMetaData> specialAttributes, ExampleSetMetaData exampleSet) {
				for (AttributeMetaData specialAttribute : specialAttributes) {
					AttributeMetaData attributeToChange = exampleSet.getAttributeByName(specialAttribute.getName());
					if (attributeToChange != null) {
						attributeToChange.setRole(specialAttribute.getRole());
					}
				}
			}
		});
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(new ExampleSetPassThroughRule(innerExampleSetSink, exampleSetOutput, SetRelation.UNKNOWN) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData inputMetaData) {
				if (getParameterAsBoolean(PARAMETER_KEEP_SUBSET_ONLY)) {
					return inputMetaData;
				} else {
					MetaData metaData = exampleSetInput.getMetaData();
					if (metaData instanceof ExampleSetMetaData) {
						inputMetaData = (ExampleSetMetaData) metaData;
						ExampleSetMetaData subsetAmd = attributeSelector.getMetaDataSubset(inputMetaData, false);
						// restore special Attributes roles if wanted
						if (!getParameterAsBoolean(PARAMETER_REMOVE_ROLES)) {
							for (AttributeMetaData attribute : subsetAmd.getAllAttributes()) {
								AttributeMetaData inputAttribute = inputMetaData.getAttributeByName(attribute.getName());
								if (inputAttribute.isSpecial()) {
									attribute.setRole(inputAttribute.getRole());
								}
							}
						}

						// storing unused attributes
						List<AttributeMetaData> unusedAttributes = new LinkedList<AttributeMetaData>();
						Iterator<AttributeMetaData> iterator = inputMetaData.getAllAttributes().iterator();
						while (iterator.hasNext()) {
							AttributeMetaData amd = iterator.next();
							if (!(subsetAmd.containsAttributeName(amd.getName()) == MetaDataInfo.YES)) {
								unusedAttributes.add(amd);
							}
						}

						// retrieving result
						if (innerExampleSetSink.getMetaData() instanceof ExampleSetMetaData) {
							ExampleSetMetaData resultMetaData = (ExampleSetMetaData) innerExampleSetSink.getMetaData()
									.clone();

							// merge and add unused completely
							Iterator<AttributeMetaData> iter = unusedAttributes.iterator();
							int nameConflict = 0;
							int roleConflict = 0;
							try {
								nameConflict = getParameterAsInt(PARAMETER_NAME_CONFLICT_HANDLING);
							} catch (UndefinedParameterError e) {
							}
							try {
								roleConflict = getParameterAsInt(PARAMETER_ROLE_CONFLICT_HANDLING);
							} catch (UndefinedParameterError e) {
							}
							while (iter.hasNext()) {
								AttributeMetaData unusedControl = iter.next();
								if (unusedControl.getRole() != null
										&& resultMetaData.getSpecial(unusedControl.getRole()) != null) {
									// use-cases
									switch (roleConflict) {
										case HANDLE_ROLE_CONFLICT_ERROR:
											innerExampleSetSink.addError(new SimpleMetaDataError(Severity.ERROR,
													innerExampleSetSink, "work_on_subset.new_special_role_exist",
													new Object[] { unusedControl.getRole(),
															resultMetaData.getSpecial(unusedControl.getRole()).getName() }));
											break;
										case HANDLE_ROLE_CONFLICT_KEEP_ORIGINAL:
											// remove special attribute
											AttributeMetaData toRemove = resultMetaData.getSpecial(unusedControl.getRole());
											resultMetaData.removeAttribute(toRemove);
											// throw error if name of original attribute exists at
											// another point in the resultSet
											if (resultMetaData.getAttributeByName(unusedControl.getName()) != null) {
												innerExampleSetSink.addError(new SimpleMetaDataError(Severity.ERROR,
														innerExampleSetSink, "work_on_subset.role_and_name_conflict",
														new Object[] { unusedControl.getName() }));
											} else {
												// insert the new one
												resultMetaData.addAttribute(unusedControl);
											}
											break;
										case HANDLE_ROLE_CONFLICT_KEEP_NEW:
											// throw error if the name of the special attribute
											// exists already, else we don't do anything
											String SpecialResultName = resultMetaData.getSpecial(unusedControl.getRole())
													.getName();
											if (!(unusedControl.getName().equals(SpecialResultName))
													&& inputMetaData.getAttributeByName(SpecialResultName) != null) {
												// throw error because is case isn't defined
												innerExampleSetSink.addError(new SimpleMetaDataError(Severity.ERROR,
														innerExampleSetSink, "work_on_subset.role_and_name_conflict",
														new Object[] { SpecialResultName }));
											}
											break;
										default:
											// don't do anything, we keep the new one
											break;
									}
								} else {
									// test for name conflict
									if (resultMetaData.getAttributeByName(unusedControl.getName()) != null) {
										if (unusedControl.getRole() != null) {
											innerExampleSetSink.addError(new SimpleMetaDataError(Severity.ERROR,
													innerExampleSetSink, "work_on_subset.role_and_name_conflict",
													new Object[] { unusedControl.getName() }));
										} else {
											// we have a regular attribute
											switch (nameConflict) {
												case HANDLE_NAME_CONFLICT_ERROR:
													innerExampleSetSink.addError(new SimpleMetaDataError(Severity.ERROR,
															innerExampleSetSink, "work_on_subset.new_attribute_exist",
															new Object[] { unusedControl.getName() }));
													break;
												case HANDLE_NAME_CONFLICT_KEEP_ORIGINAL:
													// tests whether a attribute with special role
													// and same name exists
													AttributeMetaData toRemove = resultMetaData
															.getAttributeByName(unusedControl.getName());
													if (toRemove.isSpecial()) {
														innerExampleSetSink.addError(new SimpleMetaDataError(Severity.ERROR,
																innerExampleSetSink,
																"work_on_subset.role_and_name_conflict",
																new Object[] { unusedControl.getName() }));
													} else {
														// act as defined
														resultMetaData.removeAttribute(toRemove);
														resultMetaData.addAttribute(unusedControl);
													}
													break;
												case HANDLE_NAME_CONFLICT_KEEP_NEW:
													// throws error if the attribute with this name
													// isn't regular an could be removed later in
													// this
													// loop
													AttributeMetaData toKeep = resultMetaData
															.getAttributeByName(unusedControl.getName());
													if (toKeep.isSpecial()) {
														innerExampleSetSink.addError(new SimpleMetaDataError(Severity.ERROR,
																innerExampleSetSink,
																"work_on_subset.role_and_name_conflict",
																new Object[] { unusedControl.getName() }));
													}
												default:
													// don't do anything, we keep the new one
													break;
											}
										}
									} else {
										// there is no conflict for the attribute
										resultMetaData.addAttribute(unusedControl);
									}// end if
								}
							}// end while
							return resultMetaData;
						}

					}
				}
				return inputMetaData;
			}

		});
		getTransformer().addRule(innerResultPorts.makePassThroughRule());
		innerResultPorts.start();
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet inputSet = exampleSetInput.getData(ExampleSet.class);
		ExampleSet workingExampleSet = (ExampleSet) inputSet.clone();
		Set<Attribute> selectedAttributes = attributeSelector.getAttributeSubset(workingExampleSet, false);

		List<Attribute> unusedAttributes = new LinkedList<Attribute>();
		Iterator<Attribute> iterator = workingExampleSet.getAttributes().allAttributes();
		while (iterator.hasNext()) {
			Attribute attribute = iterator.next();
			if (!selectedAttributes.contains(attribute)) {
				unusedAttributes.add(attribute);
				iterator.remove();
			}
		}
		if (getParameterAsBoolean(PARAMETER_REMOVE_ROLES)) {
			// converting special to normal
			workingExampleSet = NonSpecialAttributesExampleSet.create(workingExampleSet);
		}

		// perform inner operators
		innerExampleSetSource.deliver(workingExampleSet);
		getSubprocess(0).execute();

		// retrieve transformed example set
		ExampleSet resultSet = innerExampleSetSink.getData(ExampleSet.class);

		// add old attributes if desired
		if (!getParameterAsBoolean(PARAMETER_KEEP_SUBSET_ONLY)) {
			if (resultSet.size() != inputSet.size()) {
				throw new UserError(this, 127,
						"changing the size of the example set is not allowed if the non-processed attributes should be kept.");
			}
			mergeSets(resultSet, inputSet, unusedAttributes, resultSet.getExampleTable().equals(inputSet.getExampleTable()),
					getParameterAsInt(PARAMETER_ROLE_CONFLICT_HANDLING), getParameterAsInt(PARAMETER_NAME_CONFLICT_HANDLING));
		}

		// add all other results if desired
		innerResultPorts.passDataThrough();

		// deliver example set
		exampleSetOutput.deliver(resultSet);
	}

	/**
	 * 
	 * @param resultSet
	 *            = ExampleSet which will be returned
	 * @param inputSet
	 *            = original ExampleSet
	 * @param unusedAttributes
	 *            = list of unused Attributes of the original ExampleSet
	 * @param identicalExampleTables
	 *            = boolean Value which indicates whether the ExampleSets are the same
	 * @param roleConflictHandling
	 *            = integer Value for Role Conflict Handling
	 * @param nameConflictHandling
	 *            = integer Value for Name Conflict Handling
	 * @throws UserError
	 */
	private void mergeSets(ExampleSet resultSet, ExampleSet inputSet, List<Attribute> unusedAttributes,
			boolean identicalExampleTables, int roleConflictHandling, int nameConflictHandling) throws UserError {

		// Check whether the underlying example table has been change
		if (identicalExampleTables) {
			// if Attribute names are duplicated it throws an Exception or decide whether the new or
			// old Attribute should be kept
			for (Attribute attribute : unusedAttributes) {
				AttributeRole role = inputSet.getAttributes().getRole(attribute);
				if (resultSet.getAttributes().getSpecial(role.getSpecialName()) != null) {
					switch (roleConflictHandling) {
						case HANDLE_ROLE_CONFLICT_ERROR:
							throw new UserError(this, "attribute_subset_preprocessing.role_conflict", role.getAttribute()
									.getName(), resultSet.getAttributes().getSpecial(role.getSpecialName()).getName());
						case HANDLE_ROLE_CONFLICT_KEEP_ORIGINAL:
							// remove special attribute
							resultSet.getAttributes().remove(attribute);
							// throw error if name of original attribute exists at another point in
							// the resultSet
							if (resultSet.getAttributes().get(attribute.getName()) != null) {
								throw new UserError(this, "attribute_subset_preprocessing.role_name_conflict", role
										.getAttribute().getName());
							}
							// insert the new one
							resultSet.getAttributes().add(role);
							break;
						case HANDLE_ROLE_CONFLICT_KEEP_NEW:
							// throw error if the name of the special attribute exists already, else
							// we don't do anything
							String SpecialResultName = resultSet.getAttributes().getSpecial(role.getSpecialName()).getName();
							if (inputSet.getAttributes().get(SpecialResultName) != null
									&& !(attribute.getName().equals(SpecialResultName))) {
								// throw error because is case isn't defined
								throw new UserError(this, "attribute_subset_preprocessing.role_name_conflict",
										attribute.getName());
							}
							break;
						default:
							// don't do anything, we keep the new one
							break;
					}
				} else {
					// we have a regular attribute or the special attribute isn't part of the result
					// set until now
					if (resultSet.getAttributes().get(attribute.getName()) != null) {
						if (role.isSpecial()) {
							throw new UserError(this, "attribute_subset_preprocessing.role_name_conflict",
									attribute.getName());
						}
						// we have a regular attribute
						switch (nameConflictHandling) {
							case HANDLE_NAME_CONFLICT_ERROR:
								throw new UserError(this, "attribute_subset_preprocessing.name_conflict", role
										.getAttribute().getName());
							case HANDLE_NAME_CONFLICT_KEEP_ORIGINAL:
								// tests whether a attribute with special role and same name exists
								AttributeRole control = resultSet.getAttributes().getRole(attribute.getName());
								if (control.isSpecial()) {
									throw new UserError(this, "attribute_subset_preprocessing.role_name_conflict",
											attribute.getName());
								}
								resultSet.getAttributes().remove(attribute);
								resultSet.getAttributes().add(role);
								break;
							case HANDLE_NAME_CONFLICT_KEEP_NEW:
								AttributeRole Keep = resultSet.getAttributes().getRole(attribute.getName());
								if (Keep.isSpecial()) {
									throw new UserError(this, "attribute_subset_preprocessing.role_name_conflict",
											attribute.getName());
								}
							default:
								// don't do anything, we keep the new one
								break;
						}
					} else {
						// there is no conflict for the attribute
						resultSet.getAttributes().add(role);
					}
				}
			}
		} else {
			// we have two different ExampleSets
			logWarning("Underlying example table has changed: data copy into new table is necessary in order to keep non-processed attributes.");
			for (Attribute oldAttribute : unusedAttributes) {
				AttributeRole oldRole = inputSet.getAttributes().getRole(oldAttribute);

				if (resultSet.getAttributes().getSpecial(oldRole.getSpecialName()) != null) {
					switch (roleConflictHandling) {
						case HANDLE_ROLE_CONFLICT_ERROR:
							String targetRole = oldRole.getSpecialName();
							throw new UserError(this, "attribute_subset_preprocessing.role_conflict", new Object[] {
									targetRole, resultSet.getAttributes().getSpecial(targetRole).getName() });
						case HANDLE_ROLE_CONFLICT_KEEP_ORIGINAL:
							// remove the special attribute in resultSet and copy the original to
							// the resulSet
							resultSet.getAttributes().remove(oldAttribute);
							// throw error if name of original attribute exists at another point in
							// the resultSet
							if (resultSet.getAttributes().get(oldAttribute.getName()) != null) {
								throw new UserError(this, "attribute_subset_preprocessing.role_name_conflict",
										oldAttribute.getName());
							}

							// create and add copy of unused attributes from input set
							Attribute newAttribute = (Attribute) oldAttribute.clone();
							resultSet.getExampleTable().addAttribute(newAttribute);
							AttributeRole newRole = new AttributeRole(newAttribute);
							if (oldRole.isSpecial()) {
								newRole.setSpecial(oldRole.getSpecialName());
							}

							// add to result set
							resultSet.getAttributes().add(newRole);

							// copy data for the new attribute
							Iterator<Example> oldIterator = inputSet.iterator();
							Iterator<Example> newIterator = resultSet.iterator();
							while (oldIterator.hasNext()) {
								Example oldExample = oldIterator.next();
								Example newExample = newIterator.next();
								newExample.setValue(newAttribute, oldExample.getValue(oldAttribute));
							}
							break;
						case HANDLE_ROLE_CONFLICT_KEEP_NEW:
							// if a name-conflict with other attributes exists we throw an error
							String SpecialResultName = resultSet.getAttributes().getSpecial(oldRole.getSpecialName())
									.getName();
							if (inputSet.getAttributes().get(SpecialResultName) != null
									&& !(oldAttribute.getName().equals(SpecialResultName))) {
								// throw error because this case is not defined
								throw new UserError(this, "attribute_subset_preprocessing.role_name_conflict",
										oldAttribute.getName());
							}
							break;
						default:
							// don't do anything, we keep the new one
							break;
					}
				} else {
					if (resultSet.getAttributes().get(oldAttribute.getName()) != null) {
						// we have a regular attribute or the special attribute isn't part of the
						// result set until now
						if (oldRole.isSpecial()) {
							throw new UserError(this, "attribute_subset_preprocessing.role_name_conflict",
									oldAttribute.getName());
						}
						// we have a regular attribute
						switch (nameConflictHandling) {
							case HANDLE_NAME_CONFLICT_ERROR:
								throw new UserError(this, "attribute_subset_preprocessing.name_conflict",
										oldAttribute.getName());
							case HANDLE_NAME_CONFLICT_KEEP_ORIGINAL:
								// tests whether a attribute with special role and same name exists
								AttributeRole control = resultSet.getAttributes().getRole(oldAttribute.getName());
								if (control.isSpecial()) {
									throw new UserError(this, "attribute_subset_preprocessing.role_name_conflict",
											oldAttribute.getName());
								}
								// remove the attribute in result and copy the original to the
								// resulSet
								resultSet.getAttributes().remove(oldAttribute);
								// create and add copy of unused attributes from input set
								Attribute newAttribute = (Attribute) oldAttribute.clone();
								resultSet.getExampleTable().addAttribute(newAttribute);
								AttributeRole newRole = new AttributeRole(newAttribute);
								if (oldRole.isSpecial()) {
									newRole.setSpecial(oldRole.getSpecialName());
								}

								// add to result set
								resultSet.getAttributes().add(newRole);

								// copy data for the new attribute
								Iterator<Example> oldIterator = inputSet.iterator();
								Iterator<Example> newIterator = resultSet.iterator();
								while (oldIterator.hasNext()) {
									Example oldExample = oldIterator.next();
									Example newExample = newIterator.next();
									newExample.setValue(newAttribute, oldExample.getValue(oldAttribute));
								}
								break;
							case HANDLE_NAME_CONFLICT_KEEP_NEW:
								AttributeRole toKeep = resultSet.getAttributes().getRole(oldAttribute.getName());
								if (toKeep.isSpecial()) {
									throw new UserError(this, "attribute_subset_preprocessing.role_name_conflict",
											oldAttribute.getName());
								}
							default:
								// don't do anything, we keep the new one
								break;
						}
					} else {
						// there is no conflict of name or role
						// create and add copy of unused attributes from input set
						Attribute newAttribute = (Attribute) oldAttribute.clone();
						resultSet.getExampleTable().addAttribute(newAttribute);
						AttributeRole newRole = new AttributeRole(newAttribute);
						if (oldRole.isSpecial()) {
							newRole.setSpecial(oldRole.getSpecialName());
						}

						// add to result set
						resultSet.getAttributes().add(newRole);

						// copy data for the new attribute
						Iterator<Example> oldIterator = inputSet.iterator();
						Iterator<Example> newIterator = resultSet.iterator();
						while (oldIterator.hasNext()) {
							Example oldExample = oldIterator.next();
							Example newExample = newIterator.next();
							newExample.setValue(newAttribute, oldExample.getValue(oldAttribute));
						}// end while

					}// end else
				}
			}
		}

	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(ProcessTools.setSubsetSelectorPrimaryParameter(attributeSelector.getParameterTypes(), true));

		ParameterType type = new ParameterTypeCategory(PARAMETER_NAME_CONFLICT_HANDLING,
				"Decides how to deal with duplicate attribute names.", HANDLE_NAME_CONFLICT_MODES, 0);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeCategory(PARAMETER_ROLE_CONFLICT_HANDLING,
				"Decides how to deal with duplicate attribute roles.", HANDLE_ROLE_CONFLICT_MODES, 0);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(
				PARAMETER_KEEP_SUBSET_ONLY,
				"Indicates if the attributes which did not match the regular expression should be removed by this operator.",
				false));
		types.add(new ParameterTypeBoolean(
				PARAMETER_DELIVER_INNER_RESULTS,
				"Indicates if the additional results (other than example set) of the inner operator should also be returned.",
				false));
		types.add(new ParameterTypeBoolean(
				PARAMETER_REMOVE_ROLES,
				"Indicates whether the role of the attributes in the subset will be removed by entering the Operator or not.",
				false));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPorts().getPortByIndex(0),
				AttributeSubsetPreprocessing.class, attributeSelector);
	}

	private boolean isRemoveRolesSet() {
		return getParameterAsBoolean(PARAMETER_REMOVE_ROLES);
	}
}
