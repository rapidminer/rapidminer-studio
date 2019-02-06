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
package com.rapidminer.gui.properties;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.rapidminer.example.Attribute;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExampleResolver;
import com.rapidminer.tools.expression.FunctionDescription;
import com.rapidminer.tools.expression.FunctionInput;
import com.rapidminer.tools.expression.FunctionInput.Category;


/**
 * This class is the model for the actual {@link Attribute}s, macros and constants in the
 * {@link ExpressionPropertyDialog}.
 *
 * @author Sabrina Kirstein
 *
 */
public class FunctionInputsModel extends AbstractObservable<FunctionInputPanel> {

	/** contains the list with ALL inputs */
	private LinkedHashMap<String, List<FunctionInput>> modelMap = new LinkedHashMap<>();

	/** contains the filtered list of inputs */
	private LinkedHashMap<String, List<FunctionInput>> filteredModelMap = new LinkedHashMap<>();

	/** the function name filter {@link String} */
	private String filterNameString;

	private boolean nominalFilter = false;
	private boolean numericFilter = false;
	private boolean dateTimeFilter = false;

	/**
	 * Sorts the incoming {@link FunctionInput}s by category first
	 */
	private static final Comparator<FunctionInput> FUNCTION_INPUT_COMPARATOR = new Comparator<FunctionInput>() {

		@Override
		public int compare(FunctionInput o1, FunctionInput o2) {
			if (o1 == null && o2 == null) {
				return 0;
			} else if (o1 != null && o2 == null) {
				return 1;
			} else if (o1 == null && o2 != null) {
				return -1;
			} else {
				if (o1.getCategory() == Category.DYNAMIC && o2.getCategory() == Category.DYNAMIC) {

					if (o1.getCategoryName().equals(o2.getCategoryName())) {
						return o1.getName().compareToIgnoreCase(o2.getName());
					} else if (o1.getCategoryName().equals(ExampleResolver.KEY_ATTRIBUTES)) {
						return -1;
					} else if (o2.getCategoryName().equals(ExampleResolver.KEY_ATTRIBUTES)) {
						return 1;
					} else if (o1.getCategoryName().equals(ExampleResolver.KEY_SPECIAL_ATTRIBUTES)) {
						return -1;
					} else if (o1.getCategoryName().equals(ExampleResolver.KEY_SPECIAL_ATTRIBUTES)) {
						return 1;
					} else {
						return 0;
					}
				} else if (o1.getCategory() == Category.SCOPE && o2.getCategory() == Category.SCOPE) {
					return o1.useCustomIcon() ? -1 : 1;

				} else if (o1.getCategory().ordinal() < o2.getCategory().ordinal()) {
					return 1;

				} else if (o2.getCategory().ordinal() < o1.getCategory().ordinal()) {
					return -1;
				} else {
					return 0;
				}
			}
		}
	};

	/**
	 * Creates a model for the possible {@link FunctionDescription} inputs
	 */
	public FunctionInputsModel() {
		clearContent();
	}

	/**
	 * Clears all model content.
	 */
	public void clearContent() {
		modelMap = new LinkedHashMap<>();
		filteredModelMap = new LinkedHashMap<>();
		filterNameString = "";
		nominalFilter = false;
		numericFilter = false;
		dateTimeFilter = false;
	}

	/**
	 * Add the given inputs for the key (type of input)
	 *
	 * @param key
	 * @param inputs
	 */
	public void addContent(List<FunctionInput> inputs) {
		Collections.sort(inputs, FUNCTION_INPUT_COMPARATOR);
		for (FunctionInput input : inputs) {
			if (input.isVisible()) {
				if (modelMap.containsKey(input.getCategoryName())) {
					modelMap.get(input.getCategoryName()).add(input);
				} else {
					modelMap.put(input.getCategoryName(), new LinkedList<FunctionInput>());
					modelMap.get(input.getCategoryName()).add(input);
				}
			}
		}
		applyFilter();
	}

	/**
	 * Returns the filtered {@link Map} of {@link List}s of Strings.
	 *
	 * @return
	 */
	public Map<String, List<FunctionInput>> getFilteredModel() {
		return filteredModelMap;
	}

	/**
	 * returns the filtered map of Strings for one specific input type (defined by the type name)
	 *
	 * @param type
	 * @return
	 */
	public List<FunctionInput> getFilteredModel(String type) {
		return filteredModelMap.get(type);
	}

	/**
	 * returns the filter name
	 *
	 * @return
	 */
	public String getFilterNameString() {
		return filterNameString;
	}

	/**
	 * Filters the list of inputs using the filterNameString.
	 *
	 * @param filterNameString
	 */
	public synchronized void setFilterNameString(String filterNameString) {
		// do nothing on equal filter name
		if (filterNameString.equals(this.filterNameString)) {
			return;
		}

		this.filterNameString = filterNameString;
		applyFilter();
		fireUpdate();
	}

	/**
	 * set the filter to show the nominal attributes and constants
	 *
	 * @param filterToggled
	 */
	public void setNominalFilter(boolean filterToggled) {
		nominalFilter = filterToggled;
		applyFilter();
		fireUpdate();
	}

	/**
	 * get the nominal filter state
	 *
	 * @return if the nominal filter is toggled
	 */
	public boolean isNominalFilterToggled() {
		return nominalFilter;
	}

	/**
	 * set the filter to show the numeric attributes and constants
	 *
	 * @param filterToggled
	 */
	public void setNumericFilter(boolean filterToggled) {
		numericFilter = filterToggled;
		applyFilter();
		fireUpdate();
	}

	/**
	 * get the numeric filter state
	 *
	 * @return if the numeric filter is toggled
	 */
	public boolean isNumericFilterToggled() {
		return numericFilter;
	}

	/**
	 * set the filter to show the date time attributes and constants
	 *
	 * @param filterToggled
	 */
	public void setDateTimeFilter(boolean filterToggled) {
		dateTimeFilter = filterToggled;
		applyFilter();
		fireUpdate();
	}

	/**
	 * get the date time filter state
	 *
	 * @return if the date time filter is toggled
	 */
	public boolean isDateTimeFilterToggled() {
		return dateTimeFilter;
	}

	/**
	 * Applies the current filters.
	 */
	private synchronized void applyFilter() {

		filteredModelMap = new LinkedHashMap<>();
		for (Entry<String, List<FunctionInput>> entry : modelMap.entrySet()) {
			List<FunctionInput> list = entry.getValue();
			List<FunctionInput> newList = new LinkedList<>();
			for (FunctionInput inputValue : list) {
				newList.add(inputValue);
			}
			filteredModelMap.put(entry.getKey(), newList);
		}
		boolean anyFilterToggled = isNominalFilterToggled() || isNumericFilterToggled() || isDateTimeFilterToggled();

		// apply filter on non empty string
		for (String key : filteredModelMap.keySet()) {
			List<FunctionInput> list = filteredModelMap.get(key);
			// if the function group name already matches the search string, keep all group
			// inputs
			Iterator<FunctionInput> entryIterator = list.iterator();
			// remove the inputs that do not fit the search string
			while (entryIterator.hasNext()) {

				boolean alreadyRemoved = false;
				FunctionInput entry = entryIterator.next();
				String entryName = entry.getName();
				// check whether the input entry with the given type
				if (anyFilterToggled) {
					// should be shown
					if (!showInputEntry(entry.getType())) {
						entryIterator.remove();
						alreadyRemoved = true;
					}
				}
				if (!getFilterNameString().isEmpty()) {
					if (!entryName.toLowerCase(Locale.ENGLISH).contains(filterNameString.toLowerCase(Locale.ENGLISH))
							&& !alreadyRemoved) {
						if (key.toLowerCase(Locale.ENGLISH).contains(filterNameString.toLowerCase(Locale.ENGLISH))) {
							continue;
						}
						entryIterator.remove();
					}
				}
			}
		}
		// if a function group has no fitting inputs, remove the input group
		for (String key : modelMap.keySet()) {
			List<FunctionInput> list = filteredModelMap.get(key);
			if (list.isEmpty()) {
				filteredModelMap.remove(key);
			}
		}
	}

	/**
	 * Checks and returns whether an input entry is shown based on the entryType and the toggled
	 * filters
	 *
	 * @param entryType
	 * @param anyFilterToggled
	 * @return if the entry should be shown
	 */
	private boolean showInputEntry(int entryType) {
		switch (entryType) {
			case Ontology.INTEGER:
			case Ontology.REAL:
			case Ontology.NUMERICAL:
				if (!isNumericFilterToggled()) {
					return false;
				} else {
					return true;
				}
			case Ontology.DATE_TIME:
			case Ontology.DATE:
			case Ontology.TIME:
				if (!isDateTimeFilterToggled()) {
					return false;
				} else {
					return true;
				}
			case Ontology.FILE_PATH:
			case Ontology.STRING:
			case Ontology.POLYNOMINAL:
			case Ontology.BINOMINAL:
			case Ontology.NOMINAL:
				if (!isNominalFilterToggled()) {
					return false;
				} else {
					return true;
				}
			default:
				return false;
		}
	}
}
