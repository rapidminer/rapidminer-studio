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
package com.rapidminer.studio.io.data.internal.file.csv;

import java.util.Map;
import javax.swing.JComponent;

import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.core.io.gui.WizardDirection;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.nio.model.CSVResultSet;
import com.rapidminer.studio.io.data.HeaderRowBehindStartRowException;
import com.rapidminer.studio.io.data.HeaderRowNotFoundException;
import com.rapidminer.studio.io.data.StartRowNotFoundException;
import com.rapidminer.studio.io.gui.internal.steps.AbstractWizardStep;


/**
 * Step that configures the format of the csv file that is imported.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
public class CSVFormatSpecificationWizardStep extends AbstractWizardStep {

	static final String CSV_FORMAT_SPECIFICATION_STEP_ID = "csv.format_specification";
	private final CSVFormatSpecificationPanel formatPanel;
	private final CSVDataSource csvDataSource;
	private final ImportWizard wizard;
	private Map<String, String> enteringConfiguration;
	private boolean calculateMetaData = true;
	private String lastGuessedCSVFile;

	CSVFormatSpecificationWizardStep(CSVDataSource csvDataSource, ImportWizard wizard) {
		this.wizard = wizard;
		this.formatPanel = new CSVFormatSpecificationPanel(csvDataSource.getResultSetConfiguration());
		this.formatPanel.addChangeListener(e -> fireStateChanged());
		this.csvDataSource = csvDataSource;
	}

	@Override
	public String getI18NKey() {
		return CSV_FORMAT_SPECIFICATION_STEP_ID;
	}

	@Override
	public JComponent getView() {
		return formatPanel;
	}

	@Override
	public void validate() throws InvalidConfigurationException {
		formatPanel.validateConfiguration();
	}

	@Override
	public void viewWillBecomeVisible(WizardDirection direction) throws InvalidConfigurationException {
		wizard.setProgress(40);

		formatPanel.startDataFetching();

		if (direction == WizardDirection.NEXT) {
			final String csvFile = csvDataSource.getResultSetConfiguration().getCsvFile();
			// guess separator if that has not been done for this file before
			if (csvFile != null && (lastGuessedCSVFile == null || !lastGuessedCSVFile.equals(csvFile))) {
				CSVResultSet.ColumnSplitter columnSplitter = CSVResultSet.guessColumnSplitter(csvFile);
				formatPanel.setColumnSeparator(columnSplitter);
				formatPanel.setTextQualifier(CSVResultSet.guessTextQualifier(csvFile));

				if (columnSplitter.equals(CSVResultSet.ColumnSplitter.COMMA)) {
					formatPanel.setDecimalCharacter(CSVResultSet.DecimalCharacter.PERIOD);
				} else {
					formatPanel.setDecimalCharacter(CSVResultSet.guessDecimalSeparator(csvFile));
				}

				lastGuessedCSVFile = csvFile;
			}
		}

		enteringConfiguration = csvDataSource.getConfiguration().getParameters();
	}

	@Override
	public void viewWillBecomeInvisible(WizardDirection direction) throws InvalidConfigurationException {
		formatPanel.killCurrentErrorBubbleWindow();
		if (direction == WizardDirection.NEXT) {
			Map<String, String> currentConfiguration = csvDataSource.getConfiguration().getParameters();
			if (calculateMetaData || !enteringConfiguration.equals(currentConfiguration)) {
				// only calculate meta data if the configuration has changed, it has not been
				// calculated before or the last calculation resulted in an error
				try {
					csvDataSource.createMetaData();
					calculateMetaData = false;
				} catch (HeaderRowNotFoundException e) {
					formatPanel.notifyHeaderRowNotFound();
					calculateMetaData = true;
					throw new InvalidConfigurationException();
				} catch (StartRowNotFoundException e) {
					formatPanel.notifyStartRowNotFound();
					calculateMetaData = true;
					throw new InvalidConfigurationException();
				} catch (HeaderRowBehindStartRowException e) {
					formatPanel.notifyHeaderRowBehindStartRow();
					calculateMetaData = true;
					throw new InvalidConfigurationException();
				} catch (DataSetException e) {
					SwingTools.showSimpleErrorMessage(wizard.getDialog(), "csv_format_specification.read_failure", e,
							e.getMessage());
					calculateMetaData = true;
					throw new InvalidConfigurationException();
				}
			}
		}
		formatPanel.stopDataFetching();
	}

	@Override
	public String getNextStepID() {
		return ImportWizard.CONFIGURE_DATA_STEP_ID;
	}

}
