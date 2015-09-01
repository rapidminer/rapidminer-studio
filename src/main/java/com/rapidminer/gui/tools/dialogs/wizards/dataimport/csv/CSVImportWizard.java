/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SimpleFileFilter;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizard;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.FileSelectionWizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.RepositoryLocationSelectionWizardStep;
import com.rapidminer.gui.wizards.AbstractConfigurationWizardCreator;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.io.CSVDataReader;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.io.Encoding;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;


/**
 * A wizard to import CSV files into the repository.
 * 
 * @author Tobias Malbrecht, Sebastian Loh
 */
public class CSVImportWizard extends DataImportWizard {

	private static final long serialVersionUID = -4308448171060612833L;

	private File file = null;

	private CSVDataReader reader = null;

	private Parameters parametersBackup = null;

	public CSVImportWizard(String i18nKey, Object... i18nArgs) {
		this(i18nKey, (ConfigurationListener) null, (File) null, true, (RepositoryLocation) null, i18nArgs);
	}

	public CSVImportWizard(String i18nKey, ConfigurationListener listener, final File preselectedFile,
			final boolean showStoreInRepositoryStep, final RepositoryLocation preselectedLocation, Object... i18nArgs) {
		super(i18nKey, i18nArgs);

		file = preselectedFile;
		if (listener != null) {
			reader = (CSVDataReader) listener;
		} else {
			try {
				reader = OperatorService.createOperator(com.rapidminer.operator.io.CSVDataReader.class);
			} catch (OperatorCreationException e) {
				// TODO fix this
				e.printStackTrace();
			}
		}

		parametersBackup = (Parameters) reader.getParameters().clone();

		addStep(new FileSelectionWizardStep(this, new SimpleFileFilter("CSV File (.csv)", ".csv")) {

			@Override
			protected boolean performEnteringAction(WizardStepDirection direction) {
				if (file != null && file.exists()) {
					this.fileChooser.setSelectedFile(file);
				}
				return true;
			}

			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				file = getSelectedFile();
				File oldFile = null;
				try {
					oldFile = reader.getParameterAsFile(CSVDataReader.PARAMETER_CSV_FILE);
				} catch (UndefinedParameterError e) {
					oldFile = null;
				} catch (UserError e) {
					oldFile = null;
				}
				if (oldFile == null || !oldFile.equals(file)) {
					reader.clearAllReaderSettings();
				}
				reader.setParameter(CSVDataReader.PARAMETER_CSV_FILE, file.getAbsolutePath());
				return true;
			}
		});
		addStep(new ParseFileWizardStep("specify_csv_parsing_options", reader) {

			@Override
			protected boolean performEnteringAction(WizardStepDirection direction) {
				reader.stopReading();
				if (reader.attributeNamesDefinedByUser()) {
					reader.loadMetaDataFromParameters();

					List<Object[]> dummyData = new LinkedList<Object[]>();
					setData(dummyData);

					new ProgressThread("load_csv_file") {

						@Override
						public void run() {
							final List<Object[]> data;
							try {
								data = reader.getPreviewAsList(getProgressListener(), true);
							} catch (OperatorException e) {
								// TODO fix this
								SwingTools.showVerySimpleErrorMessage(e.getMessage(), e);
								return;
							}
							SwingUtilities.invokeLater(new Runnable() {

								@Override
								public void run() {
									setData(data);
								}
							});
						}
					}.start();

				} else {
					settingsChanged();
				}
				return true;
			}

			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				reader.stopReading();
				return true;
			}

			@Override
			protected void settingsChanged() {
				reader.clearAllReaderSettings();
				reader.setParameter(Encoding.PARAMETER_ENCODING, getEncoding().displayName());
				reader.setParameter(CSVDataReader.PARAMETER_TRIM_LINES, Boolean.toString(trimLines()));
				reader.setParameter(CSVDataReader.PARAMETER_SKIP_COMMENTS, Boolean.toString(skipComments()));
				reader.setParameter(CSVDataReader.PARAMETER_COMMENT_CHARS, getCommentCharacters());
				reader.setParameter(CSVDataReader.PARAMETER_USE_FIRST_ROW_AS_ATTRIBUTE_NAMES,
						Boolean.toString(getUseFirstRowAsColumnNames()));
				reader.setParameter(CSVDataReader.PARAMETER_USE_QUOTES, Boolean.toString(useQuotes()));
				reader.setParameter(CSVDataReader.PARAMETER_QUOTES_CHARACTER, Character.toString(getQuotesCharacter()));
				reader.setParameter(CSVDataReader.PARAMETER_ESCAPE_CHARACTER, Character.toString(getEscapeCharacter()));
				reader.setParameter(CSVDataReader.PARAMETER_COLUMN_SEPARATORS, getSplitExpression());
				List<Object[]> dummyData = new LinkedList<Object[]>();
				setData(dummyData);

				new ProgressThread("load_csv_file") {

					@Override
					public void run() {
						final List<Object[]> data;
						try {
							data = reader.getPreviewAsList(getProgressListener(), true);
						} catch (OperatorException e) {
							// TODO fix this
							SwingTools.showVerySimpleErrorMessage(e.getMessage(), e);
							return;
						}
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								setData(data);
							}
						});
					}
				}.start();
			}

			@Override
			protected boolean canGoBack() {
				return true;
			}

			@Override
			protected boolean canProceed() {
				return true;
			}

		});
		addStep(new ParseValueTypesWizardStep("value_type_selection", reader) {

			@Override
			protected boolean canGoBack() {
				return true;
			}

			@Override
			protected boolean canProceed() {
				return true;
			}

			@Override
			protected boolean performEnteringAction(WizardStepDirection direction) {
				if (reader.attributeNamesDefinedByUser()) {
					reader.loadMetaDataFromParameters();
				}
				super.performEnteringAction(direction);
				return true;
			}

			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				// MetaData is saved to the ParameterType
				reader.stopReading();
				reader.writeMetaDataInParameter();
				return true;
			}
		});

		if (showStoreInRepositoryStep) {
			addStep(new RepositoryLocationSelectionWizardStep(this,
					preselectedLocation != null ? preselectedLocation.getAbsoluteLocation() : null, true, true) {

				@Override
				protected boolean performLeavingAction(WizardStepDirection direction) {
					return transferData(reader, getRepositoryLocation());
				}
			});
		}
		layoutDefault(HUGE);
	}

	@Override
	public void cancel() {
		reader.getParameters().setAll(parametersBackup);
		reader.stopReading();
		super.cancel();
	}

	@Override
	public void finish() {
		reader.stopReading();
		super.finish();
	}

	/**
	 * Creates a {@link CSVImportWizard}.
	 * 
	 * @author Sebastian Loh (06.05.2010)
	 * 
	 */
	public static class CSVDataReaderWizardCreator extends AbstractConfigurationWizardCreator {

		private static final long serialVersionUID = 1L;

		/*
		 * (non-Javadoc)
		 * 
		 * @seecom.rapidminer.gui.wizards.ConfigurationWizardCreator#
		 * createConfigurationWizard(com.rapidminer.parameter.ParameterType,
		 * com.rapidminer.gui.wizards.ConfigurationListener)
		 */
		@Override
		public void createConfigurationWizard(ParameterType type, ConfigurationListener listener) {
			// create wizard depending on the operator context
			String fileLocation = "";
			try {
				fileLocation = listener.getParameters().getParameter(CSVDataReader.PARAMETER_CSV_FILE);
				if (fileLocation == null) {
					throw new UndefinedParameterError("");
				}
				File file = new File(fileLocation);
				(new CSVImportWizard(getI18NKey(), listener, file, false, new Object[] { null })).setVisible(true);
			} catch (UndefinedParameterError e) {
				(new CSVImportWizard(getI18NKey(), listener, null, false, new Object[] { null })).setVisible(true);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.rapidminer.gui.wizards.ConfigurationWizardCreator#getI18NKey()
		 */
		@Override
		public String getI18NKey() {
			return "data_import_wizard";
		}
	}
}
