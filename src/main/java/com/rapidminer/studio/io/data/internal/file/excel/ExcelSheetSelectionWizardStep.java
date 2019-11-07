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
package com.rapidminer.studio.io.data.internal.file.excel;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

import javax.swing.JComponent;

import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.core.io.gui.WizardDirection;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.operator.nio.model.xlsx.XlsxUtilities.XlsxCellCoordinates;
import com.rapidminer.studio.io.data.HeaderRowNotFoundException;
import com.rapidminer.studio.io.data.StartRowNotFoundException;
import com.rapidminer.studio.io.gui.internal.steps.AbstractWizardStep;


/**
 * An {@link com.rapidminer.core.io.gui.WizardStep ImportWizardStep} which allows to select the Excel sheet and cell range to import.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
final class ExcelSheetSelectionWizardStep extends AbstractWizardStep {

	static final String EXCEL_SHEET_SELECTION_STEP_ID = "excel.sheet_selection";

	private final ExcelSheetSelectionPanel workbookSelectionPanel;
	private final ExcelDataSource excelDataSource;

	private Map<String, String> enteringConfiguration;
	private String enteringPath;
	private boolean calculateMetaData = true;

	private final ImportWizard wizard;

	ExcelSheetSelectionWizardStep(ExcelDataSource excelDataSource, ImportWizard wizard) {
		this.excelDataSource = excelDataSource;
		this.wizard = wizard;
		this.workbookSelectionPanel = new ExcelSheetSelectionPanel(excelDataSource);
		this.workbookSelectionPanel.addChangeListener(e -> fireStateChanged());
		wizard.getDialog().addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				workbookSelectionPanel.tearDown();
			}
		});
	}

	@Override
	public String getI18NKey() {
		return EXCEL_SHEET_SELECTION_STEP_ID;
	}

	@Override
	public JComponent getView() {
		return workbookSelectionPanel;
	}

	@Override
	public void validate() throws InvalidConfigurationException {
		if (workbookSelectionPanel.isSheetEmpty() || workbookSelectionPanel.isSelectionEmpty()
				|| workbookSelectionPanel.isUpdatingUI()) {
			throw new InvalidConfigurationException();
		}

		// check whether header row is included in the data
		int headerRowIndex = workbookSelectionPanel.getHeaderRowIndex();
		if (headerRowIndex > workbookSelectionPanel.getSelection().getRowIndexEnd()) {
			workbookSelectionPanel.notifyHeaderRowBehindStartRow();
			throw new InvalidConfigurationException();
		}

		// in case only a column is specified we use the first row as start row
		int rowIndexStart = workbookSelectionPanel.getSelection().getRowIndexStart();
		if (rowIndexStart == XlsxCellCoordinates.NO_ROW_NUMBER) {
			rowIndexStart = 0;
		}

		// check whether the header is behind or equal to the first data row
		if (headerRowIndex > rowIndexStart) {
			workbookSelectionPanel.notifyHeaderRowBehindStartRow();
			throw new InvalidConfigurationException();
		}
	}

	@Override
	public void viewWillBecomeVisible(WizardDirection direction) throws InvalidConfigurationException {

		wizard.setProgress(40);

		/*
		 * If the users proceeds to this step from the file selection and changed the file we need
		 * to clear the cache. If he returns from the column configuration we can still use the old
		 * cache.
		 */
		if (direction == WizardDirection.NEXT && enteringPath != null
				&& !enteringPath.equals(excelDataSource.getLocation().toString())) {
			workbookSelectionPanel.clearCache();
		}

		// we need to update the model (and thus the UI) in any case though
		workbookSelectionPanel.configureSheetSelectionModel(excelDataSource);

		// remember configuration of the data source
		enteringConfiguration = excelDataSource.getConfiguration().getParameters();
		enteringPath = excelDataSource.getLocation().toString();
	}

	@Override
	public void viewWillBecomeInvisible(WizardDirection direction) throws InvalidConfigurationException {


		// only configure meta data in case the user proceeds to the next step
		if (direction == WizardDirection.NEXT) {

			// update configuration with content selection
			CellRangeSelection selection = workbookSelectionPanel.getSelection();

			ExcelResultSetConfiguration configuration = excelDataSource.getResultSetConfiguration();

			// update the selected sheet
			configuration.setSheet(workbookSelectionPanel.getSheetIndex());
			configuration.setSheetSelectionMode(ExcelResultSetConfiguration.SheetSelectionMode.BY_INDEX);

			// update the cell range selection
			configuration.setColumnOffset(selection.getColumnIndexStart());
			configuration.setColumnLast(selection.getColumnIndexEnd());

			configuration.setRowOffset(selection.getRowIndexStart());
			configuration.setRowLast(selection.getRowIndexEnd());

			// update header row
			excelDataSource.setHeaderRowIndex(workbookSelectionPanel.getHeaderRowIndex());

			Map<String, String> currentConfiguration = excelDataSource.getConfiguration().getParameters();
			if (calculateMetaData || !enteringConfiguration.equals(currentConfiguration)) {
				// only calculate meta data if the configuration has changed, it has not been
				// calculated before or the last calculation resulted in an error
				try {
					excelDataSource.createMetaData();
					calculateMetaData = false;
				} catch (HeaderRowNotFoundException e) {
					calculateMetaData = true;
					workbookSelectionPanel.notifyHeaderRowBehindStartRow();
					throw new InvalidConfigurationException();
				} catch (StartRowNotFoundException e) {
					calculateMetaData = true;
					workbookSelectionPanel.notifyNoRowsLeft();
					throw new InvalidConfigurationException();
				} catch (DataSetException e) {
					calculateMetaData = true;
					SwingTools.showSimpleErrorMessage(wizard.getDialog(),
							"io.dataimport.step.excel.sheet_selection.read_failure", e, e.getMessage());
					throw new InvalidConfigurationException();
				}
			}
		}
		workbookSelectionPanel.tearDown();
	}

	@Override
	public String getNextStepID() {
		return ImportWizard.CONFIGURE_DATA_STEP_ID;
	}

}
