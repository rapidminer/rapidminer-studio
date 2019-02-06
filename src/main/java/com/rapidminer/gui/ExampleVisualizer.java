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
package com.rapidminer.gui;

import java.awt.Dimension;
import java.util.Date;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.rapidminer.ObjectVisualizer;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.ButtonDialog.ButtonDialogBuilder;
import com.rapidminer.gui.tools.dialogs.ButtonDialog.ButtonDialogBuilder.DefaultButtons;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * A visualizer which shows the attribute values of an example. This is the most simple visualizer
 * which should work in all cases.
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class ExampleVisualizer implements ObjectVisualizer {

	private final ExampleSet exampleSet;

	private final Attribute idAttribute;

	private boolean isExampleSetRemapped = false;

	public ExampleVisualizer(ExampleSet exampleSet) {
		this.exampleSet = exampleSet;
		this.idAttribute = exampleSet.getAttributes().getId();
	}

	@Override
	public void startVisualization(final Object objId) {
		remapIds();

		JComponent main;
		int dialogSize = ButtonDialog.MESSAGE;
		if (idAttribute != null) {
			final double idValue;
			if (idAttribute.isNominal()) {
				idValue = objId instanceof String ? idAttribute.getMapping().mapString((String) objId) : (Double) objId;
			} else {
				idValue = objId instanceof String ? Double.parseDouble((String) objId) : (Double) objId;
			}
			Example example = exampleSet.getExampleFromId(idValue);
			if (example != null) {
				main = makeMainVisualizationComponent(example);
				dialogSize = ButtonDialog.NARROW;
			} else {
				main = new JLabel("No information available for object '" + objId + "'.");
			}
		} else {
			main = new JLabel("No information available for object '" + objId + "' because no ID attribute exists.");
		}

		ButtonDialogBuilder builder = new ButtonDialogBuilder("example_visualizer_dialog");
		JDialog dialog = builder.setI18nArguments(objId).setContent(main, dialogSize)
				.setButtons(DefaultButtons.CLOSE_BUTTON).setOwner(ApplicationFrame.getApplicationFrame()).build();
		dialog.setVisible(true);
	}

	private String getFormattedValue(Example example, Attribute attribute) {
		double value = example.getValue(attribute);
		if (Double.isNaN(value)) {
			return "?";
		}
		if (attribute.isNominal()) {
			return attribute.getMapping().mapIndex((int) value);
		} else {
			switch (attribute.getValueType()) {
				case Ontology.INTEGER:
					return Tools.formatIntegerIfPossible(value);
				case Ontology.REAL:
					return Tools.formatNumber(value);
				case Ontology.NUMERICAL:
					return Tools.formatNumber(value);
				case Ontology.DATE:
					return Tools.formatDate(new Date((long) value));
				case Ontology.TIME:
					return Tools.formatTime(new Date((long) value));
				case Ontology.DATE_TIME:
					return Tools.formatDateTime(new Date((long) value));
			}
		}
		;
		return "";
	}

	protected JComponent makeMainVisualizationComponent(Example example) {
		JComponent main;
		String[] columnNames = new String[] { "Attribute", "Value" };
		String[][] data = new String[exampleSet.getAttributes().allSize()][2];
		Iterator<Attribute> a = exampleSet.getAttributes().allAttributes();
		int counter = 0;
		while (a.hasNext()) {
			Attribute attribute = a.next();
			data[counter][0] = attribute.getName();
			data[counter][1] = getFormattedValue(example, attribute);
			counter++;
		}
		ExtendedJTable table = new ExtendedJTable();
		table.setDefaultEditor(Object.class, null);
		TableModel tableModel = new DefaultTableModel(data, columnNames);
		table.setRowHighlighting(true);
		table.setRowHeight(PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
		table.setModel(tableModel);
		main = new ExtendedJScrollPane(table);
		main.setBorder(null);
		int tableHeight = (int) (table.getPreferredSize().getHeight() + table.getTableHeader().getPreferredSize().getHeight()
				+ 5); // 5 for border
		if (tableHeight < main.getPreferredSize().getHeight()) {
			main.setPreferredSize(new Dimension((int) main.getPreferredSize().getWidth(), tableHeight));
		}
		return main;
	};

	@Override
	public String getDetailData(Object objId, String fieldName) {
		remapIds();
		double idValue = Double.NaN;

		if (idAttribute.isNominal()) {
			idValue = objId instanceof String ? idAttribute.getMapping().mapString((String) objId) : (Double) objId;
		} else {
			idValue = objId instanceof String ? Double.parseDouble((String) objId) : (Double) objId;
		}

		Example example = exampleSet.getExampleFromId(idValue);

		Attribute attribute = exampleSet.getAttributes().get(fieldName);
		if (attribute != null) {
			return example.getValueAsString(attribute);
		} else {
			return null;
		}
	}

	@Override
	public String[] getFieldNames(Object id) {
		return com.rapidminer.example.Tools.getAllAttributeNames(exampleSet);
	}

	/** Does nothing. */
	@Override
	public void stopVisualization(Object objId) {}

	@Override
	public String getTitle(Object objId) {
		return objId instanceof String ? (String) objId : ((Double) objId).toString();
	}

	@Override
	public boolean isCapableToVisualize(Object id) {
		remapIds();
		if (idAttribute.isNominal()) {
			double idValue = id instanceof String ? idAttribute.getMapping().mapString((String) id) : (Double) id;
			return exampleSet.getExampleFromId(idValue) != null;
		} else {
			double idValue = id instanceof String ? Double.parseDouble((String) id) : (Double) id;
			return exampleSet.getExampleFromId(idValue) != null;
		}
	}

	private void remapIds() {
		if (!isExampleSetRemapped) {
			this.exampleSet.remapIds();
			this.isExampleSetRemapped = true;
		}
	}
}
