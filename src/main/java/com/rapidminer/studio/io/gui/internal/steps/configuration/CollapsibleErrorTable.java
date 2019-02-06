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
package com.rapidminer.studio.io.gui.internal.steps.configuration;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXCollapsiblePane.Direction;

import com.rapidminer.gui.tools.Ionicon;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 * A collapsible error table that shows the content of an {@link AbstractErrorWarningTableModel}. A
 * label shows whether the content of the table consists of errors or warnings or the table is
 * empty.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
public class CollapsibleErrorTable extends JPanel {

	private static final long serialVersionUID = 1L;

	/** image indicating errors */
	private static final ImageIcon ICON_ERRORS_FOUND = SwingTools.createIcon("16/error.png");

	/** image indicating no errors or warnings */
	private static final ImageIcon ICON_NO_ERRORS_FOUND = SwingTools.createIcon("16/ok.png");

	private static final ImageIcon ICON_WARNINGS_FOUND = SwingTools.createIcon("16/sign_warning.png");

	/** the text for opening the error table */
	private static final String SHOW_ERROR_TABLE = "<html><span style=\"text-decoration: underline;\">"
			+ I18N.getGUILabel("csv_format_specification.view_error_details") + " " + Ionicon.CHEVRON_UP.getHtml()
			+ "</span>";

	/** the text for closing the error table */
	private static final String HIDE_ERROR_TABLE = "<html><span style=\"text-decoration: underline;\">"
			+ I18N.getGUILabel("csv_format_specification.hide_error_details") + " " + Ionicon.CHEVRON_DOWN.getHtml()
			+ "</span>";

	/** the label text when there are errors */
	private static final String ERRORS_TEXT = " " + I18N.getGUILabel("csv_format_specification.label_errors");

	/** the label text when there are errors and warnings */
	private static final String ERRORS_AND_TEXT = " " + I18N.getGUILabel("csv_format_specification.label_errors_and") + " ";

	/** the label text when there are warnings */
	private static final String WARNINGS_TEXT = " " + I18N.getGUILabel("csv_format_specification.label_warnings");

	/** the label text when there are no errors */
	private static final String NO_ERRORS_TEXT = I18N.getGUILabel("csv_format_specification.label_no_problems");

	/** buffer for the table height */
	private static final int ERROR_TABLE_HEIGHT_BUFFER = 10;

	/** maximal height of the component */
	private static final int MAXIMAL_HEIGHT = 200;

	private final AbstractErrorWarningTableModel errorWarningTableModel;
	private final JTable errorTable;
	private final JScrollPane errorScrollPane;
	private final JLabel errorLabel = new JLabel();
	private final JLabel openErrorLabel = new JLabel();
	private boolean errorPanelCollapsed = true;
	final JXCollapsiblePane collapsePane = new JXCollapsiblePane(Direction.UP);

	public CollapsibleErrorTable(AbstractErrorWarningTableModel errorWarningTableModel) {
		this.errorWarningTableModel = errorWarningTableModel;
		errorTable = new JTable(errorWarningTableModel) {

			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				// add tooltip for last column
				JComponent component = (JComponent) super.prepareRenderer(renderer, row, column);
				if (column == getColumnCount() - 1) {
					component.setToolTipText(Objects.toString(getValueAt(row, column), ""));
				}
				return component;
			}

		};
		errorScrollPane = new JScrollPane(errorTable);
		errorWarningTableModel.addTableModelListener(e -> update());
		setupGUI();
	}

	/**
	 * @return the error panel which can be extended to show the {@link #errorTable} if there are
	 *         errors
	 */
	private void setupGUI() {
		errorScrollPane.setPreferredSize(new Dimension(errorScrollPane.getPreferredSize().width, MAXIMAL_HEIGHT));
		errorScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

		collapsePane.add(errorScrollPane);
		collapsePane.setCollapsed(true);

		this.setLayout(new BorderLayout());
		this.add(collapsePane, BorderLayout.NORTH);

		JPanel errorContainerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		errorContainerPanel.add(errorLabel);

		openErrorLabel.setText(SHOW_ERROR_TABLE);
		openErrorLabel.setForeground(SwingTools.RAPIDMINER_ORANGE);
		openErrorLabel.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				errorPanelCollapsed = !errorPanelCollapsed;
				if (errorPanelCollapsed) {
					openErrorLabel.setText(SHOW_ERROR_TABLE);
				} else {
					openErrorLabel.setText(HIDE_ERROR_TABLE);
				}
				collapsePane.setCollapsed(errorPanelCollapsed);
			}
		});
		openErrorLabel.setVisible(false);

		errorContainerPanel.add(openErrorLabel);
		this.add(errorContainerPanel, BorderLayout.CENTER);
	}

	/**
	 * Updates the content of the {@link #errorTable} and the label that is used to show it.
	 */
	public void update() {
		// calculate the size of the table
		int rowCount = errorWarningTableModel.getRowCount();
		int tableHeight = rowCount * errorTable.getRowHeight() + errorTable.getTableHeader().getHeight();
		int height = Math.min(MAXIMAL_HEIGHT, tableHeight + ERROR_TABLE_HEIGHT_BUFFER);
		errorScrollPane.setPreferredSize(new Dimension(errorScrollPane.getPreferredSize().width, height));

		// Update errors summary
		if (rowCount == 0) {
			errorLabel.setText(NO_ERRORS_TEXT);
			errorLabel.setIcon(ICON_NO_ERRORS_FOUND);
			// show no error table and no link to open it
			openErrorLabel.setVisible(false);
			errorPanelCollapsed = true;
			openErrorLabel.setText(SHOW_ERROR_TABLE);
			collapsePane.setCollapsed(errorPanelCollapsed);
		} else {
			int errorCount = errorWarningTableModel.getErrorCount();
			int warningCount = errorWarningTableModel.getWarningCount();
			if (errorCount > 0) {
				errorLabel.setIcon(ICON_ERRORS_FOUND);
				if (warningCount > 0) {
					errorLabel.setText(errorCount + ERRORS_AND_TEXT + warningCount + WARNINGS_TEXT);
				} else {
					errorLabel.setText(rowCount + ERRORS_TEXT);
				}
			} else {
				errorLabel.setIcon(ICON_WARNINGS_FOUND);
				errorLabel.setText(warningCount + WARNINGS_TEXT);
			}
			openErrorLabel.setVisible(true);
		}
	}

	/**
	 * @return the underlying table
	 */
	public JTable getTable() {
		return errorTable;
	}

}
