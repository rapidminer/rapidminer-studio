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
package com.rapidminer.operator.nio;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.operator.nio.ExcelWorkbookPane.ExcelWorkbookSelection;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;


/**
 * This step allows to select a sheet of an excel workbook to import it.
 *
 * It handles the handshake with the generic following {@link AnnotationDeclarationWizardStep}.
 *
 * @author Sebastian Land
 */
class ExcelSheetSelectionWizardStep extends WizardStep {

	private ExcelResultSetConfiguration configuration;
	private final ExcelWorkbookPane workbookSelectionPanel;

	private final JLabel errorLabel = new JLabel("");

	public ExcelSheetSelectionWizardStep(ExcelResultSetConfiguration configuration) {
		super("importwizard.excel_data_selection");

		this.configuration = configuration;
		this.workbookSelectionPanel = new ExcelWorkbookPane(configuration);
		workbookSelectionPanel.getSheetTabbedPane().addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				fireStateChanged();
			}
		});
	}

	@Override
	protected boolean canGoBack() {
		return true;
	}

	@Override
	protected boolean canProceed() {
		return workbookSelectionPanel.canProceed();
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		if (direction == WizardStepDirection.FORWARD) {
			workbookSelectionPanel.loadWorkbook();
		}
		return true;
	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		if (direction == WizardStepDirection.FORWARD || direction == WizardStepDirection.FINISH) {
			ExcelWorkbookSelection selection = workbookSelectionPanel.getSelection();

			// empty workbooks (i.e. without any column) cannot leave
			if (!canProceed()) {
				return false;
			}

			configuration.setSheet(selection.getSheetIndex());
			configuration.setSheetSelectionMode(ExcelResultSetConfiguration.SheetSelectionMode.BY_INDEX);
			configuration.setColumnOffset(selection.getColumnIndexStart());
			configuration.setColumnLast(selection.getColumnIndexEnd());
			configuration.setRowOffset(selection.getRowIndexStart());
			configuration.setRowLast(selection.getRowIndexEnd());
		} else {
			configuration.closeWorkbook();
		}

		return true;
	}

	@Override
	protected JComponent getComponent() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(workbookSelectionPanel, BorderLayout.CENTER);
		panel.add(errorLabel, BorderLayout.SOUTH);
		return panel;
	}
}
