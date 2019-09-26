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

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.table.JTableHeader;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.tools.CellColorProvider;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.viewer.metadata.model.AbstractAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.DateTimeAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.NominalAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.NumericalAttributeStatisticsModel;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * Can be used to display (parts of) the data by means of a JTable. Used to display
 * {@link ExampleSet} results in the {@link DataViewer}.
 *
 * @author Ingo Mierswa
 */
public class DataViewerTable extends ExtendedJTable {

	private static final int MAXIMAL_CONTENT_LENGTH = 200;
	
	protected static final int MAX_ROW_HEIGHT = 30;
	protected static final int MIN_ROW_HEIGHT = 25;

	private static final long serialVersionUID = 5535239693801265693L;

	/** Maximal length for displaying nominal values so that the html parsing does not take forever. */
	private static final int MAX_VALUE_LENGTH = 350;

	private int[] dateColumns;

	private Map<String, Color> mappingAttributeNamesToColor;

	private DataViewerTableModel dvTableModel;

	private Map<String, AbstractAttributeStatisticsModel> mappingAttributeNamesToAttributeStatisticsModel = new HashMap<>();

	private boolean statsInitState = true;

	private WeakReference<ExampleSet> exampleSetReference;

	private Map<Integer, String> toolTipMessagesMap = new HashMap<>();

	public DataViewerTable() {
		this.mappingAttributeNamesToColor = new HashMap<>();
		setAutoResizeMode(AUTO_RESIZE_OFF);
		setFixFirstColumnForRearranging(true);
		installToolTip();

		// handles the highlighting of the currently hovered row
		setRowHighlighting(true);
	}

	public void setExampleSet(ExampleSet exampleSet) {
		this.dvTableModel = new DataViewerTableModel(exampleSet);
		this.exampleSetReference = new WeakReference<>(exampleSet);
		setModel(dvTableModel);

		dateColumns = new int[exampleSet.getAttributes().allSize() + 1];
		dateColumns[0] = NO_DATE_FORMAT;
		int index = 1;
		List<AttributeRole> specialAttributes = new ArrayList<>(exampleSet.getAttributes().specialSize());
		Iterator<AttributeRole> s = exampleSet.getAttributes().specialAttributes();
		while (s.hasNext()) {
			specialAttributes.add(s.next());
		}
		Collections.sort(specialAttributes, ExampleSetUtilities.SPECIAL_ATTRIBUTES_ROLE_COMPARATOR);
		for (AttributeRole attributeRole : specialAttributes) {
			Attribute attribute = attributeRole.getAttribute();
			String attributeRoleLabel = exampleSet.getAttributes().getRole(attribute).getSpecialName();
			Color specialColor = AttributeGuiTools.getColorForAttributeRole(attributeRoleLabel);
			mappingAttributeNamesToColor.put(attribute.getName(), specialColor);
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE)) {
				dateColumns[index] = DATE_FORMAT;
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.TIME)) {
				dateColumns[index] = TIME_FORMAT;
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
				dateColumns[index] = DATE_TIME_FORMAT;
			} else {
				dateColumns[index] = NO_DATE_FORMAT;
			}
			index++;
		}

		for (Attribute attribute : exampleSet.getAttributes()) {
			mappingAttributeNamesToColor.put(attribute.getName(), Colors.WHITE);
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE)) {
				dateColumns[index] = DATE_FORMAT;
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.TIME)) {
				dateColumns[index] = TIME_FORMAT;
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
				dateColumns[index] = DATE_TIME_FORMAT;
			} else {
				dateColumns[index] = NO_DATE_FORMAT;
			}
			index++;
		}

		setCellColorProvider(new CellColorProvider() {

			@Override
			public Color getCellColor(int row, int column) {
				int col = convertColumnIndexToModel(column);
				Color returnCol;
				if (dvTableModel != null && dvTableModel.getColumnAttribute(col) != null) {
					Color color = mappingAttributeNamesToColor.get(dvTableModel.getColumnAttribute(col).getName());
					returnCol = color;
				} else {
					returnCol = Colors.WHITE;
				}

				return returnCol;
			}

		});

		setGridColor(Colors.TABLE_CELL_BORDER);

		setCutOnLineBreak(true);
		setMaximalTextLength(MAXIMAL_CONTENT_LENGTH);

		final int size = exampleSet.size();
		setRowHeight(calcRowHeight(MAX_ROW_HEIGHT, MIN_ROW_HEIGHT, size));

		ProgressThread createStatsThread = new ProgressThread("update_result_statistics") {

			@Override
			public void run() {
				statsInitState = true;
				toolTipMessagesMap.clear();
				// iterate over all attributes, create models for them
				ExampleSet exampleSet = DataViewerTable.this.exampleSetReference.get();
				if (exampleSet != null) {
					Iterator<Attribute> attributeIterator = exampleSet.getAttributes().allAttributes();
					while (attributeIterator.hasNext()) {
						Attribute att = attributeIterator.next();
						AbstractAttributeStatisticsModel statModel;
						if (att.isNumerical()) {
							statModel = new NumericalAttributeStatisticsModel(exampleSet, att);
						} else if (att.isNominal()) {
							statModel = new NominalAttributeStatisticsModel(exampleSet, att);
						} else {
							statModel = new DateTimeAttributeStatisticsModel(exampleSet, att);
						}
						statModel.updateStatistics(exampleSet);
						mappingAttributeNamesToAttributeStatisticsModel.put(att.getName(), statModel);
					}
				}
				statsInitState = false;
			}

		};
		createStatsThread.start();

	}

	private static int calcRowHeight(int maxRowHeight, int minRowHeight, int count) {
		if (count == 0) {
			return maxRowHeight;
		}
		final int f = Integer.MAX_VALUE / count;
		final int m = Math.max(minRowHeight, f);
		return Math.min(maxRowHeight, m);
	}

	/**
	 * This method ensures that the correct tool tip for the current column is delivered.
	 */
	@Override
	protected JTableHeader createDefaultTableHeader() {
		JTableHeader header = new JTableHeader(columnModel) {

			private static final long serialVersionUID = 1L;

			@Override
			public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int index = columnModel.getColumnIndexAtX(p.x);
				int realColumnIndex = convertColumnIndexToModel(index);
				return DataViewerTable.this.getHeaderToolTipText(realColumnIndex);
			}
		};
		header.putClientProperty(RapidLookTools.PROPERTY_TABLE_HEADER_BACKGROUND, Colors.WHITE);
		return header;
	}

	@Override
	public int getDateFormat(int row, int column) {
		return dateColumns[column];
	}

	private String getHeaderToolTipText(int realColumnIndex) {
		if (realColumnIndex == 0) {
			// tooltip text for the column containing the example index
			return I18N.getMessage(I18N.getGUIBundle(), "gui.label.data_view.example_index.tooltip");
		}
		if (statsInitState) {
			// tooltip text for when the statistics are not yet calculated
			return I18N.getMessage(I18N.getGUIBundle(), "gui.label.data_view.calc_stats.tooltip");
		}
		if (toolTipMessagesMap.containsKey(realColumnIndex)) {
			// if the tooltip string was already constructed, use the cached string
			return toolTipMessagesMap.get(realColumnIndex);
		}

		AbstractAttributeStatisticsModel statsModel = mappingAttributeNamesToAttributeStatisticsModel.get(getModel()
				.getColumnName(realColumnIndex));
		if (statsModel != null && statsModel.getExampleSetOrNull() != null) {
			statsModel.updateStatistics(statsModel.getExampleSetOrNull());
			double missingValues = statsModel.getNumberOfMissingValues();

			if (Double.isNaN(missingValues)) {
				// tooltip text for when the statistics are not yet calculated
				return I18N.getMessage(I18N.getGUIBundle(), "gui.label.data_view.calc_stats.tooltip");
			}

			// construct the tooltip text when the statistics have been calculated
			String attRole = statsModel.getExampleSetOrNull().getAttributes().getRole(statsModel.getAttribute())
					.getSpecialName();

			String valueTypeString = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(statsModel.getAttribute().getValueType());
			valueTypeString = valueTypeString.replaceAll("_", " ");
			valueTypeString = String.valueOf(valueTypeString.charAt(0)).toUpperCase() + valueTypeString.substring(1);

			String I18NStatsKey = "gui.label.attribute_statistics.statistics.";

			String toolTipText = "<html>";
			toolTipText += "<b>" + getModel().getColumnName(realColumnIndex) + "</b>";
			if (attRole != null) {
				toolTipText += "<font color=\"#666666\"> (" + attRole + ")</font>";
			}
			toolTipText += "<br><i>" + valueTypeString + "</i><br><br style=\"font-size:3px;\">";

			toolTipText += I18N.getMessage(I18N.getGUIBundle(), I18NStatsKey + "values_missing") + ": "
					+ Tools.formatIntegerIfPossible(missingValues) + "<br>";
			if (statsModel instanceof NumericalAttributeStatisticsModel) {
				NumericalAttributeStatisticsModel numStatsModel = (NumericalAttributeStatisticsModel) statsModel;
				toolTipText += I18N.getMessage(I18N.getGUIBundle(), I18NStatsKey + "min.label") + ": "
						+ Tools.formatIntegerIfPossible(numStatsModel.getMinimum()) + "<br>";
				toolTipText += I18N.getMessage(I18N.getGUIBundle(), I18NStatsKey + "max.label") + ": "
						+ Tools.formatIntegerIfPossible(numStatsModel.getMaximum()) + "<br>";
				toolTipText += I18N.getMessage(I18N.getGUIBundle(), I18NStatsKey + "avg.label") + ": "
						+ Tools.formatIntegerIfPossible(numStatsModel.getAverage()) + "<br>";
			} else if (statsModel instanceof NominalAttributeStatisticsModel) {
				NominalAttributeStatisticsModel nomStatsModel = (NominalAttributeStatisticsModel) statsModel;
				toolTipText += I18N.getMessage(I18N.getGUIBundle(), I18NStatsKey + "least.label") + ": "
						+ SwingTools.getShortenedDisplayName(nomStatsModel.getLeast(), MAX_VALUE_LENGTH) + "<br>";
				toolTipText += I18N.getMessage(I18N.getGUIBundle(), I18NStatsKey + "most.label") + ": "
						+ SwingTools.getShortenedDisplayName(nomStatsModel.getMost(), MAX_VALUE_LENGTH) + "<br>";
			} else if (statsModel instanceof DateTimeAttributeStatisticsModel) {
				DateTimeAttributeStatisticsModel dateStatsModel = (DateTimeAttributeStatisticsModel) statsModel;
				toolTipText += I18N.getMessage(I18N.getGUIBundle(), I18NStatsKey + "duration.label") + ": "
						+ dateStatsModel.getDuration() + "<br>";
				toolTipText += I18N.getMessage(I18N.getGUIBundle(), I18NStatsKey + "from.label") + ": "
						+ dateStatsModel.getFrom() + "<br>";
				toolTipText += I18N.getMessage(I18N.getGUIBundle(), I18NStatsKey + "until.label") + ": "
						+ dateStatsModel.getUntil() + "<br>";
			}

			toolTipMessagesMap.put(realColumnIndex, toolTipText);
			return toolTipText;
		}

		return null;
	}
}
