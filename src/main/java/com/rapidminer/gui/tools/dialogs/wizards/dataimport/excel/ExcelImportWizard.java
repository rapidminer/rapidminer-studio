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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel;

import com.rapidminer.gui.tools.SimpleFileFilter;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizard;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.FileSelectionWizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.MetaDataDeclerationWizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.RepositoryLocationSelectionWizardStep;
import com.rapidminer.gui.wizards.AbstractConfigurationWizardCreator;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.io.ExcelExampleSource;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.OperatorService;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * @author Tobias Malbrecht, Sebastian Loh
 */
public class ExcelImportWizard extends DataImportWizard {

	private static final long serialVersionUID = -4308448171060612833L;

	private File file = null;

	// the operator which is setup during the wizard
	private ExcelExampleSource reader = null;

	private Parameters parametersBackup;

	private final WizardStep STEP_FILE_SELECTION = new FileSelectionWizardStep(this, new SimpleFileFilter(
			"Excel File (.xls)", ".xls")) {

		@Override
		protected boolean performEnteringAction(WizardStepDirection direction) {
			if (file != null && file.exists()) {
				this.fileChooser.setSelectedFile(file);
			}
			return true;
		}

		@Override
		protected boolean performLeavingAction(WizardStepDirection direction) {
			// deleting annotations if a second step has been performed earlier
			ExcelImportWizard.this.reader.setParameter(ExcelExampleSource.PARAMETER_ANNOTATIONS, null);

			// setting parameter
			file = getSelectedFile();
			File oldFile = null;
			try {
				oldFile = reader.getParameterAsFile(ExcelExampleSource.PARAMETER_EXCEL_FILE);
			} catch (UndefinedParameterError e) {
				oldFile = null;
			} catch (UserError e) {
				oldFile = null;
			}
			if (oldFile == null || !oldFile.equals(file)) {
				reader.clearAllReaderSettings();
			}
			reader.setParameter(ExcelExampleSource.PARAMETER_EXCEL_FILE, file.getAbsolutePath());
			// reader.resetWorkbook();
			return true;
		}
	};

	private static class ExcelWorkSheetSelection extends WizardStep {

		private final ExcelWorkbookPane workbookSelectionPanel;

		private final JLabel errorLabel = new JLabel("");

		ExcelExampleSource reader;

		public ExcelWorkSheetSelection(ExcelExampleSource reader) {
			super("excel_data_selection");
			this.reader = reader;
			this.workbookSelectionPanel = new ExcelWorkbookPane(this, reader);
		}

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
			reader.stopReading();
			reader.setParameter(ExcelExampleSource.PARAMETER_FIRST_ROW_AS_NAMES, Boolean.FALSE.toString());
			reader.skipNameAnnotationRow(false);
			boolean flag = reader.attributeNamesDefinedByUser();
			workbookSelectionPanel.loadWorkbook();
			// write flag back because laodWorkbook invokes a reader.clearReaderSetting()
			reader.setAttributeNamesDefinedByUser(flag);

			return true;
		}

		@Override
		protected boolean performLeavingAction(WizardStepDirection direction) {
			if (reader.attributeNamesDefinedByUser()) {
				reader.loadMetaDataFromParameters();
			}
			reader.stopReading();
			reader.setParameter(ExcelExampleSource.PARAMETER_SHEET_NUMBER,
					Integer.toString(workbookSelectionPanel.getSelection().getSheetIndex() + 1));
			List<String[]> annotationParameter = new LinkedList<String[]>();

			boolean nameAnnotationFound = false;
			for (Map.Entry<Integer, String> entry : workbookSelectionPanel.getSelection().getAnnotationMap().entrySet()) {
				annotationParameter.add(new String[] { entry.getKey().toString(), entry.getValue() });
				if (entry.getValue().equals(Annotations.ANNOTATION_NAME)) {
					nameAnnotationFound = true;
				}
			}
			reader.setParameter(ExcelExampleSource.PARAMETER_ANNOTATIONS,
					ParameterTypeList.transformList2String(annotationParameter));

			if (nameAnnotationFound) {
				reader.setAttributeNamesDefinedByUser(false);
				reader.skipNameAnnotationRow(false); // should be already false
			} else {
				reader.skipNameAnnotationRow(true);
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

	@Override
	public void cancel() {
		reader.getParameters().setAll(parametersBackup);
		reader.stopReading();
		// TODO reader.resetWorkbook();
		super.cancel();
	}

	@Override
	public void finish() {
		reader.stopReading();
		super.finish();
	}

	public ExcelImportWizard(String i18nKey, ConfigurationListener listener, File preselectedFile,
			final boolean showStoreInRepositoryStep, RepositoryLocation preselectedLocation, Object... i18nArgs) {
		super(i18nKey, i18nArgs);
		file = preselectedFile;
		if (listener != null) {
			reader = (ExcelExampleSource) listener;
		} else {
			try {
				reader = OperatorService.createOperator(com.rapidminer.operator.io.ExcelExampleSource.class);
			} catch (OperatorCreationException e) {
				throw new RuntimeException("Failed to create excel reader: " + e, e);
			}
		}

		parametersBackup = (Parameters) reader.getParameters().clone();

		addStep(STEP_FILE_SELECTION);

		// reader.keepWorkbookOpen();

		addStep(new ExcelWorkSheetSelection(reader));

		addStep(new MetaDataDeclerationWizardStep("select_attributes", reader) {

			@Override
			protected JComponent getComponent() {
				JPanel typeDetection = new JPanel(ButtonDialog.createGridLayout(1, 2));
				typeDetection.setBorder(ButtonDialog.createTitledBorder("Value Type Detection"));
				typeDetection.add(new JLabel("Guess the value types of all attributes"));
				typeDetection.add(guessingButtonsPanel);

				Component[] superComponents = super.getComponent().getComponents();

				JPanel upperPanel = new JPanel(new BorderLayout());// new
				// JPanel(ButtonDialog.createGridLayout(2,
				// 1));
				upperPanel.add(typeDetection, BorderLayout.NORTH);
				upperPanel.add(superComponents[0], BorderLayout.CENTER);

				JPanel panel = new JPanel(new BorderLayout(0, ButtonDialog.GAP));
				panel.add(upperPanel, BorderLayout.NORTH);
				panel.add(superComponents[1], BorderLayout.CENTER);

				return panel;
			}

			@Override
			protected void doAfterEnteringAction() {
				reader.setAttributeNamesDefinedByUser(true);
				((ExcelExampleSource) reader).skipNameAnnotationRow(true);
			}

			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				reader.stopReading();
				reader.writeMetaDataInParameter();
				// TODO if (ExcelImportWizard.this.isComplete()) {
				// ((ExcelExampleSource) reader).resetWorkbook();
				// }
				return true;
			}

		});

		if (showStoreInRepositoryStep) {
			addStep(new RepositoryLocationSelectionWizardStep(this,
					preselectedLocation != null ? preselectedLocation.getAbsoluteLocation() : null, true, true) {

				@Override
				protected boolean performLeavingAction(WizardStepDirection direction) {
					synchronized (reader) {
						boolean flag = transferData(reader, getRepositoryLocation());
						// TODO (reader).resetWorkbook();
						return flag;
					}
				}
			});
		}

		layoutDefault(HUGE);
	}

	public ExcelImportWizard(String i18nKey, Object... i18nArgs) {
		this(i18nKey, (ConfigurationListener) null, (File) null, true, (RepositoryLocation) null, i18nArgs);
	}

	public ExcelImportWizard(String i18nKey, File preselectedFile, RepositoryLocation preselectedLocation,
			Object... i18nArgs) {
		this(i18nKey, preselectedFile, true, preselectedLocation, i18nArgs);
	}

	public ExcelImportWizard(String i18nKey, File preselectedFile, ConfigurationListener listener, Object... i18nArgs) {
		this(i18nKey, preselectedFile, false, null, i18nArgs);
	}

	// RapidLab constructor?
	public ExcelImportWizard(String i18nKey, ExcelExampleSource reader, Object... i18nArgs) {
		super(i18nKey, i18nArgs);
		this.reader = reader;
		addStep(STEP_FILE_SELECTION);
		addStep(new ExcelWorkSheetSelection(reader));
		// addStep(STEP_EXCEL_DATA_SELECTION);
		layoutDefault(LARGE);
	}

	/**
	 * Creates a {@link ExcelImportWizard}.
	 * 
	 * @author Sebastian Loh (06.05.2010)
	 * 
	 */
	public static class ExcelExampleSourceConfigurationWizardCreator extends AbstractConfigurationWizardCreator {

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
				fileLocation = listener.getParameters().getParameter(ExcelExampleSource.PARAMETER_EXCEL_FILE);
				if (fileLocation == null) {
					throw new UndefinedParameterError("");
				}
				File file = new File(fileLocation);
				(new ExcelImportWizard(getI18NKey(), listener, file, false, new Object[] { null })).setVisible(true);
			} catch (UndefinedParameterError e) {
				(new ExcelImportWizard(getI18NKey(), listener, null, false, new Object[] { null })).setVisible(true);
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
