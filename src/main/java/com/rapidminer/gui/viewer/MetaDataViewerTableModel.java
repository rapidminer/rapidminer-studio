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
package com.rapidminer.gui.viewer;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.Tools;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.operator.Annotations;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;


/**
 * The model for the {@link com.rapidminer.gui.viewer.MetaDataViewerTable}.
 * 
 * @author Ingo Mierswa
 */
public class MetaDataViewerTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -1598719681189990076L;

	public static final int DEFAULT_MAX_NUMBER_OF_ROWS_FOR_STATISTICS = 100000;

	public static final int DEFAULT_MAX_DISPLAYED_VALUES = 50;

	public static final int TYPE = 0;
	public static final int INDEX = 1;
	public static final int NAME = 2;
	public static final int SOURCE = 3;
	public static final int VALUE_TYPE = 4;
	public static final int BLOCK_TYPE = 5;
	public static final int STATISTICS_AVERAGE = 6;
	public static final int STATISTICS_RANGE = 7;
	public static final int STATISTICS_SUM = 8;
	public static final int STATISTICS_UNKNOWN = 9;
	public static final int UNIT = 10;
	public static final int COMMENT = 11;

	public static final String[] COLUMN_NAMES = new String[] { "Role", "Table Index", "Name", "Construction", "Type",
			"Block", "Statistics", "Range", "Sum", "Missings", Annotations.KEY_UNIT, Annotations.KEY_COMMENT };

	public static final String[] COLUMN_TOOL_TIPS = new String[] {
			"The type of the attribute (regular or one of the special types).",
			"The index of the attribute in the example table backing up this example set (view).",
			"The name of the attribute.", "The construction source of the attribute, i.e. how it was generated.",
			"The value type of this attribute, e.g. if the attribute is nominal or numerical.",
			"The block type of this attribute, e.g. if the attribute is a single attribute or part of a series.",
			"Basic statistics about the data set values with respect to this attribute.",
			"The range about the data set values with respect to this attribute (only numerical attributes).",
			"The sum of all values in the data set for this attribute.",
			"The number of unknown values in the data set for this attribute", "The unit annotation.",
			"The comment annotation" };

	public static final Class<?>[] COLUMN_CLASSES = new Class[] { String.class, Double.class, String.class, String.class,
			String.class, String.class, String.class, String.class, Double.class, Double.class, String.class, String.class };

	private int[] currentMapping = { TYPE, NAME, VALUE_TYPE, STATISTICS_AVERAGE, STATISTICS_RANGE, STATISTICS_UNKNOWN };

	private ExampleSet exampleSet;

	private Attribute[] regularAttributes = new Attribute[0];

	private Attribute[] specialAttributes = new Attribute[0];

	private String[] specialAttributeNames = new String[0];

	public MetaDataViewerTableModel(ExampleSet exampleSet) {
		this.exampleSet = exampleSet;
		if (this.exampleSet != null) {
			this.regularAttributes = Tools.createRegularAttributeArray(exampleSet);
			this.specialAttributes = new Attribute[exampleSet.getAttributes().specialSize()];
			this.specialAttributeNames = new String[exampleSet.getAttributes().specialSize()];
			Iterator<AttributeRole> i = exampleSet.getAttributes().specialAttributes();
			int counter = 0;
			while (i.hasNext()) {
				AttributeRole role = i.next();
				this.specialAttributeNames[counter] = role.getSpecialName();
				this.specialAttributes[counter] = role.getAttribute();
				counter++;
			}

			// calculate statistics
			int maxNumberForStatistics = DEFAULT_MAX_NUMBER_OF_ROWS_FOR_STATISTICS;
			String maxString = ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_MAX_STATISTICS_ROWS);
			if (maxString != null) {
				try {
					maxNumberForStatistics = Integer.parseInt(maxString);
				} catch (NumberFormatException e) {
					// do nothing
				}
			}

			if (exampleSet.size() < maxNumberForStatistics) {
				calculateStatistics();
			} else {
				setShowColumn(MetaDataViewerTableModel.STATISTICS_AVERAGE, false);
				setShowColumn(MetaDataViewerTableModel.STATISTICS_RANGE, false);
				setShowColumn(MetaDataViewerTableModel.STATISTICS_SUM, false);
				setShowColumn(MetaDataViewerTableModel.STATISTICS_UNKNOWN, false);
			}
		}
	}

	public void calculateStatistics() {
		exampleSet.recalculateAllAttributeStatistics();

		// make sure this is called from the EDT
		if (SwingUtilities.isEventDispatchThread()) {
			setShowColumn(MetaDataViewerTableModel.STATISTICS_AVERAGE, true);
			setShowColumn(MetaDataViewerTableModel.STATISTICS_RANGE, true);
			setShowColumn(MetaDataViewerTableModel.STATISTICS_UNKNOWN, true);
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {

					@Override
					public void run() {
						setShowColumn(MetaDataViewerTableModel.STATISTICS_AVERAGE, true);
						setShowColumn(MetaDataViewerTableModel.STATISTICS_RANGE, true);
						setShowColumn(MetaDataViewerTableModel.STATISTICS_UNKNOWN, true);
					}
				});
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}

	}

	public void setShowColumn(int index, boolean show) {
		List<Integer> result = new LinkedList<Integer>();
		for (int i = 0; i < COLUMN_NAMES.length; i++) {
			if (i == index) {
				if (show) {
					result.add(i);
				}
			} else {
				if (getShowColumn(i)) {
					result.add(i);
				}
			}
		}
		this.currentMapping = new int[result.size()];
		Iterator<Integer> i = result.iterator();
		int counter = 0;
		while (i.hasNext()) {
			this.currentMapping[counter++] = i.next();
		}
		fireTableStructureChanged();
	}

	public boolean getShowColumn(int index) {
		for (int element : currentMapping) {
			if (element == index) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getRowCount() {
		if (this.exampleSet != null) {
			return exampleSet.getAttributes().specialSize() + exampleSet.getAttributes().size();
		} else {
			return 0;
		}
	}

	/**
	 * Returns up to 9 for the following eight columns (depending on the current column selection): <br>
	 * 0: type<br>
	 * 1: index<br>
	 * 2: name<br>
	 * 3: construction<br>
	 * 4: value type<br>
	 * 5: block type<br>
	 * 6: basic statistics<br>
	 * 7: range statistics<br>
	 * 8: sum statistics<br>
	 * 9: unknown statistics<br>
	 */
	@Override
	public int getColumnCount() {
		if (this.exampleSet != null) {
			return currentMapping.length;
		} else {
			return 0;
		}
	}

	@Override
	public Object getValueAt(int row, int col) {
		Attribute attribute = null;
		String type = "regular";
		if (row < specialAttributes.length) {
			attribute = specialAttributes[row];
			type = specialAttributeNames[row];
		} else {
			attribute = this.regularAttributes[row - specialAttributes.length];
		}
		int actualColumn = currentMapping[col];
		switch (actualColumn) {
			case TYPE:
				return type;
			case INDEX:
				return attribute.getTableIndex();
			case NAME:
				return attribute.getName();
			case SOURCE:
				return attribute.getConstruction();
			case VALUE_TYPE:
				return Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attribute.getValueType());
			case BLOCK_TYPE:
				return Ontology.ATTRIBUTE_BLOCK_TYPE.mapIndex(attribute.getBlockType());
			case STATISTICS_AVERAGE:
				// DATE
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
					long minMilliseconds = (long) exampleSet.getStatistics(attribute, Statistics.MINIMUM);
					long maxMilliseconds = (long) exampleSet.getStatistics(attribute, Statistics.MAXIMUM);
					long difference = maxMilliseconds - minMilliseconds;
					String duration = "length = ";
					if (attribute.getValueType() == Ontology.DATE) {
						// days
						duration += com.rapidminer.tools.Tools.formatIntegerIfPossible(Math.round(difference
								/ (24.0d * 60.0d * 60.0d * 1000.0d)))
								+ " days";
					} else if (attribute.getValueType() == Ontology.TIME) {
						// hours
						duration += com.rapidminer.tools.Tools.formatIntegerIfPossible(Math.round(difference
								/ (60.0d * 60.0d * 1000.0d)))
								+ " hours";
					} else if (attribute.getValueType() == Ontology.DATE_TIME) {
						// days
						duration += com.rapidminer.tools.Tools.formatIntegerIfPossible(Math.round(difference
								/ (24.0d * 60.0d * 60.0d * 1000.0d)))
								+ " days";
					}
					return duration;
				} else if (attribute.isNominal()) {
					// NOMINAL
					int modeIndex = (int) exampleSet.getStatistics(attribute, Statistics.MODE);
					String mode = null;
					try {
						if (modeIndex != -1) {
							mode = attribute.getMapping().mapIndex(modeIndex);
						}
					} catch (AttributeTypeException e) {
						// do nothing
					}

					int leastIndex = (int) exampleSet.getStatistics(attribute, Statistics.LEAST);
					String least = null;
					try {
						if (leastIndex != -1) {
							least = attribute.getMapping().mapIndex(leastIndex);
						}
					} catch (AttributeTypeException e) {
						// do nothing
					}

					StringBuffer result = new StringBuffer();
					if (mode != null) {
						result.append("mode = "
								+ mode
								+ " ("
								+ com.rapidminer.tools.Tools.formatIntegerIfPossible(exampleSet.getStatistics(attribute,
										Statistics.COUNT, mode)) + ")");
					} else {
						return result.append("mode = unknown");
					}
					if (least != null) {
						result.append(", least = "
								+ least
								+ " ("
								+ com.rapidminer.tools.Tools.formatIntegerIfPossible(exampleSet.getStatistics(attribute,
										Statistics.COUNT, least)) + ")");
					} else {
						return result.append(", least = unknown");
					}
					return result.toString();
				} else {
					// NUMERICAL
					double average = exampleSet.getStatistics(attribute, Statistics.AVERAGE);
					double variance = Math.sqrt(exampleSet.getStatistics(attribute, Statistics.VARIANCE));
					return "avg = " + com.rapidminer.tools.Tools.formatIntegerIfPossible(average) + " +/- "
							+ com.rapidminer.tools.Tools.formatIntegerIfPossible(variance);
				}
			case STATISTICS_RANGE:
				// DATE
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
					long minMilliseconds = (long) exampleSet.getStatistics(attribute, Statistics.MINIMUM);
					long maxMilliseconds = (long) exampleSet.getStatistics(attribute, Statistics.MAXIMUM);
					String minResult = null;
					String maxResult = null;
					if (attribute.getValueType() == Ontology.DATE) {
						minResult = com.rapidminer.tools.Tools.formatDate(new Date(minMilliseconds));
						maxResult = com.rapidminer.tools.Tools.formatDate(new Date(maxMilliseconds));
					} else if (attribute.getValueType() == Ontology.TIME) {
						minResult = com.rapidminer.tools.Tools.formatTime(new Date(minMilliseconds));
						maxResult = com.rapidminer.tools.Tools.formatTime(new Date(maxMilliseconds));
					} else if (attribute.getValueType() == Ontology.DATE_TIME) {
						minResult = com.rapidminer.tools.Tools.formatDateTime(new Date(minMilliseconds));
						maxResult = com.rapidminer.tools.Tools.formatDateTime(new Date(maxMilliseconds));
					}
					return "[" + minResult + " ; " + maxResult + "]";
				} else if (attribute.isNominal()) {
					// NOMINAL
					int maxDisplayValues = DEFAULT_MAX_DISPLAYED_VALUES / 2;
					String maxString = ParameterService
							.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_MAX_DISPLAYED_VALUES);
					if (maxString != null) {
						try {
							maxDisplayValues = Integer.parseInt(maxString) / 2;
						} catch (NumberFormatException e) {
							// do nothing
						}
					}

					StringBuffer str = new StringBuffer();

					int totalNumberOfValues = attribute.getMapping().size();
					if (totalNumberOfValues > 2 * maxDisplayValues) {
						class ValueAndCount implements Comparable<ValueAndCount> {

							String value;
							int count;

							ValueAndCount(String value, int count) {
								this.value = value;
								this.count = count;
							}

							@Override
							public int compareTo(ValueAndCount o) {
								int result = -1 * Double.compare(this.count, o.count);
								if (result == 0) {
									if (this.value == null) {
										if (o.value == null) {
											return 0;
										} else {
											return -1;
										}
									} else {
										return this.value.compareTo(o.value);
									}
								} else {
									return result;
								}
							}

							@Override
							public int hashCode() {
								final int prime = 31;
								int result = 1;
								result = prime * result + count;
								result = prime * result + (value == null ? 0 : value.hashCode());
								return result;
							}

							@Override
							public boolean equals(Object obj) {
								if (this == obj) {
									return true;
								}
								if (obj == null) {
									return false;
								}
								if (getClass() != obj.getClass()) {
									return false;
								}
								ValueAndCount other = (ValueAndCount) obj;
								if (count != other.count) {
									return false;
								}
								if (value == null) {
									if (other.value != null) {
										return false;
									}
								} else if (!value.equals(other.value)) {
									return false;
								}
								return true;
							}
						}

						List<ValueAndCount> valuesAndCounts = new ArrayList<ValueAndCount>();
						for (String value : attribute.getMapping().getValues()) {
							valuesAndCounts.add(new ValueAndCount(value,
									(int) exampleSet.getStatistics(attribute, Statistics.COUNT, value)));
						}

						Collections.sort(valuesAndCounts);

						int n = 0;
						for (ValueAndCount valueAndCount : valuesAndCounts) {
							if (n > maxDisplayValues) {
								break;
							}
							if (n > 0) {
								str.append(", ");
							}
							n++;

							str.append(valueAndCount.value);
							str.append(" (" + valueAndCount.count + ")");
						}

						str.append(", ... and " + (valuesAndCounts.size() - 2 * maxDisplayValues) + " more ... ");

						Iterator<ValueAndCount> l = valuesAndCounts
								.listIterator(valuesAndCounts.size() - 1 - maxDisplayValues);
						while (l.hasNext()) {
							str.append(", ");
							ValueAndCount valueAndCount = l.next();
							str.append(valueAndCount.value);
							str.append(" (" + valueAndCount.count + ")");
						}
					} else {
						int n = 0;
						for (String value : attribute.getMapping().getValues()) {
							if (n > 0) {
								str.append(", ");
							}
							n++;

							int count = (int) exampleSet.getStatistics(attribute, Statistics.COUNT, value);
							str.append(value + " (" + count + ")");
						}
					}

					return str.toString();
				} else {
					// NUMERICAL
					return "["
							+ com.rapidminer.tools.Tools.formatNumber(exampleSet
									.getStatistics(attribute, Statistics.MINIMUM))
							+ " ; "
							+ com.rapidminer.tools.Tools.formatNumber(exampleSet
									.getStatistics(attribute, Statistics.MAXIMUM)) + "]";
				}
			case STATISTICS_SUM:
				return exampleSet.getStatistics(attribute, Statistics.SUM);
			case STATISTICS_UNKNOWN:
				return exampleSet.getStatistics(attribute, Statistics.UNKNOWN);
			case COMMENT:
				return attribute.getAnnotations().getAnnotation(Annotations.KEY_COMMENT);
			case UNIT:
				return attribute.getAnnotations().getAnnotation(Annotations.KEY_UNIT);
			default:
				return "unknown";
		}
	}

	/**
	 * Returns one of the following nine column names:<br>
	 * 0: type<br>
	 * 1: index<br>
	 * 2: name<br>
	 * 3: construction<br>
	 * 4: type<br>
	 * 5: block type<br>
	 * 6: basic statistics<br>
	 * 7: range statistics<br>
	 * 8: sum statistics<br>
	 * 9: unknown statistics<br>
	 * 10: unit annotation<br>
	 * 11: comment annotation
	 */
	@Override
	public String getColumnName(int col) {
		return COLUMN_NAMES[currentMapping[col]];
	}

	/** Returns the classes of the columns. */
	@Override
	public Class<?> getColumnClass(int col) {
		return COLUMN_CLASSES[currentMapping[col]];
	}

	/** Returns the tool tip text for the specified column. */
	public String getColumnToolTip(int column) {
		if (column < 0 || column >= currentMapping.length) {
			return "";
		} else {
			return COLUMN_TOOL_TIPS[currentMapping[column]];
		}
	}
}
