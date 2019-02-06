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

import com.rapidminer.gui.tools.CellColorProviderAlternating;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.model.WizardState;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;


/**
 * This Wizard Step might be used to select several rows as annotation rows having special meaning.
 * 
 * @author Sebastian Land
 */
public class AnnotationDeclarationWizardStep extends WizardStep {

	private final JPanel panel = new JPanel(new BorderLayout());
	private final WizardState state;
	private JTable table;

	private final LoadingContentPane loadingContentPane = new LoadingContentPane("loading_data", panel);

	public AnnotationDeclarationWizardStep(WizardState state) {
		super("importwizard.annotations");
		this.state = state;
		ExtendedJTable extendedTable = new ExtendedJTable(false, false, false);
		extendedTable.setCellColorProvider(new CellColorProviderAlternating() {

			@Override
			public Color getCellColor(int row, int column) {
				if (column == 0) {
					return row % 2 == 0 ? Color.WHITE : SwingTools.LIGHTEST_YELLOW;
				} else {
					return super.getCellColor(row, column);
				}
			}
		});
		table = extendedTable;
		panel.add(new ExtendedJScrollPane(table), BorderLayout.CENTER);
	}

	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		if (direction == WizardStepDirection.FORWARD) {
			ProgressThread thread = new ProgressThread("loading_data") {

				@Override
				public void run() {
					getProgressListener().setTotal(100);
					getProgressListener().setCompleted(10);
					try {
						final TableModel wrappedModel = state.getDataResultSetFactory().makePreviewTableModel(
								getProgressListener());
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								table.setModel(new AnnotationTableModel(wrappedModel, state.getTranslationConfiguration()
										.getAnnotationsMap()));
								table.getColumnModel().getColumn(0).setCellEditor(new AnnotationCellEditor());
							}
						});
					} catch (Exception e) {
						ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e.toString(),
								e);
					} finally {
						getProgressListener().complete();
					}
				}
			};
			loadingContentPane.init(thread);
			thread.start();
		}
		return true;

	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		if (direction == WizardStepDirection.BACKWARD || direction == WizardStepDirection.FINISH) {
			if (state.getTranslator() != null) {
				try {
					state.getTranslator().close();
				} catch (OperatorException e) {
					ImportWizardUtils.showErrorMessage(state.getDataResultSetFactory().getResourceName(), e.toString(), e);
				}
			}
		}
		return true;
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
	protected JComponent getComponent() {
		return loadingContentPane;
	}
}
