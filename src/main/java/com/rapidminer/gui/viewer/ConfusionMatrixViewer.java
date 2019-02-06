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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.report.Tableable;
import com.rapidminer.tools.I18N;


/**
 * This viewer class can be used to display performance criteria based on a multi class confusion
 * matrix. The viewer consists of two parts, first a part containing the general performance info
 * string and second a table with the complete confusion matrix.
 *
 * @author Ingo Mierswa
 */
public class ConfusionMatrixViewer extends JPanel implements Tableable, PrintableComponent {

	private static final long serialVersionUID = 3448880915145528006L;

	private ConfusionMatrixViewerTable table;

	private JComponent plotter;

	private String performanceName;

	public ConfusionMatrixViewer(String performanceName, String performanceString, String[] classNames, double[][] counter) {
		this.performanceName = performanceName;
		setLayout(new BorderLayout());

		final JPanel mainPanel = new JPanel();
		mainPanel.setOpaque(true);
		mainPanel.setBackground(Colors.WHITE);
		final CardLayout cardLayout = new CardLayout();
		mainPanel.setLayout(cardLayout);
		add(mainPanel, BorderLayout.CENTER);

		// *** table panel ***
		JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.setOpaque(true);
		tablePanel.setBackground(Colors.WHITE);
		tablePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

		// info string
		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		infoPanel.setOpaque(true);
		infoPanel.setBackground(Colors.WHITE);
		JTextPane infoText = new JTextPane();
		infoText.setEditable(false);
		infoText.setBackground(infoPanel.getBackground());
		infoText.setFont(infoText.getFont().deriveFont(Font.BOLD));
		infoText.setText(performanceString);
		infoPanel.add(infoText);
		tablePanel.add(infoPanel, BorderLayout.NORTH);

		// table
		table = new ConfusionMatrixViewerTable(classNames, counter);
		table.setBorder(BorderFactory.createLineBorder(Colors.TABLE_CELL_BORDER));
		JScrollPane scrollPane = new ExtendedJScrollPane(table);
		scrollPane.setBorder(null);
		scrollPane.setBackground(Colors.WHITE);
		scrollPane.getViewport().setBackground(Colors.WHITE);
		tablePanel.add(scrollPane, BorderLayout.CENTER);
		table.setTableHeader(null);

		// *** plot panel ***
		SimpleDataTable dataTable = new SimpleDataTable("Confusion Matrix", new String[] { "True Class", "Predicted Class",
		"Confusion Matrix (x: true class,  y: pred. class,  z: counters)" });
		for (int row = 0; row < classNames.length; row++) {
			for (int column = 0; column < classNames.length; column++) {
				dataTable.add(new SimpleDataTableRow(new double[] { row, column, counter[row][column] }));
			}
		}

		PlotterConfigurationModel settings = new PlotterConfigurationModel(PlotterConfigurationModel.STICK_CHART_3D,
				dataTable);
		settings.setAxis(0, 0);
		settings.setAxis(1, 1);
		settings.enablePlotColumn(2);

		mainPanel.add(tablePanel, "table");
		plotter = settings.getPlotter().getPlotter();
		mainPanel.add(plotter, "plot");

		// toggle radio button for views
		final JRadioButton metaDataButton = new JRadioButton("Table View", true);
		metaDataButton.setToolTipText("Changes to a table showing the confusion matrix.");
		metaDataButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (metaDataButton.isSelected()) {
					cardLayout.show(mainPanel, "table");
				}
			}
		});

		final JRadioButton plotButton = new JRadioButton("Plot View", false);
		plotButton.setToolTipText("Changes to a plot view of the confusion matrix.");
		plotButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (plotButton.isSelected()) {
					cardLayout.show(mainPanel, "plot");
				}
			}
		});

		ButtonGroup group = new ButtonGroup();
		group.add(metaDataButton);
		group.add(plotButton);
		JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		togglePanel.setOpaque(true);
		togglePanel.setBackground(Colors.WHITE);
		togglePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
		togglePanel.add(metaDataButton);
		togglePanel.add(plotButton);

		add(togglePanel, BorderLayout.NORTH);
	}

	@Override
	public void prepareReporting() {
		table.prepareReporting();
	}

	@Override
	public void finishReporting() {
		table.finishReporting();
	}

	@Override
	public boolean isFirstLineHeader() {
		return true;
	}

	@Override
	public boolean isFirstColumnHeader() {
		return true;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return table.getColumnName(columnIndex);
	}

	@Override
	public String getCell(int row, int column) {
		return table.getCell(row, column);
	}

	@Override
	public int getColumnNumber() {
		return table.getColumnNumber();
	}

	@Override
	public int getRowNumber() {
		return table.getRowNumber();
	}

	@Override
	public Component getExportComponent() {
		return plotter;
	}

	@Override
	public String getExportName() {
		return I18N.getGUIMessage("gui.cards.result_view.confusion_matrix.title");
	}

	@Override
	public String getIdentifier() {
		return performanceName;
	}

	@Override
	public String getExportIconName() {
		return I18N.getGUIMessage("gui.cards.result_view.confusion_matrix.icon");
	}
}
