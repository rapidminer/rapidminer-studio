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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterTypeProcessLocation;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;


/**
 * Cell editor that allows to select a repository entry by pressing a button.
 *
 * @author Marcel Seifert, Nils Woehler
 *
 */
public class ProcessLocationValueCellEditor extends RepositoryLocationValueCellEditor {

	private static final long serialVersionUID = 1L;

	private final JPanel surroundingPanel = new JPanel(new BorderLayout());

	private final JButton openProcessButton = new JButton(new ResourceAction(true, "execute_process.open_process") {

		private static final long serialVersionUID = 1L;
		{
			putValue(NAME, null);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			RepositoryLocation repositoryLocation;
			RepositoryProcessLocation repositoryProcessLocation = null;
			try {
				if (getTextField() != null) {
					repositoryLocation = RepositoryLocation.getRepositoryLocation(getTextFieldText(), getOperator());

					if (repositoryLocation != null) {
						repositoryProcessLocation = new RepositoryProcessLocation(repositoryLocation);
						if (repositoryProcessLocation != null && RapidMinerGUI.getMainFrame().close()) {
							OpenAction.open(repositoryProcessLocation, true);
						}
					}
				}
			} catch (UserError e1) {
				SwingTools.showVerySimpleErrorMessage("malformed_repository_location", getTextField().getText());
			}
		}

	});

	private final CellEditorListener listener = new CellEditorListener() {

		@Override
		public void editingStopped(ChangeEvent e) {
			checkOpenProcessButtonEnabled();
		}

		@Override
		public void editingCanceled(ChangeEvent e) {
			// do nothing
		}
	};

	public ProcessLocationValueCellEditor(final ParameterTypeProcessLocation type) {
		super(type);
		openProcessButton.setEnabled(false);
		surroundingPanel.add(getPanel(), BorderLayout.CENTER);
		surroundingPanel.add(openProcessButton, BorderLayout.EAST);
		addCellEditorListener(listener);

	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
		// ensure text field is filled with correct values
		super.getTableCellEditorComponent(table, value, isSelected, row, col);
		checkOpenProcessButtonEnabled();
		return surroundingPanel;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}

	/**
	 * Checks whether the provided repository location is valid and is a process.
	 */
	private void checkOpenProcessButtonEnabled() {
		final String location = getTextFieldText();
		ProgressThread t = new ProgressThread("check_process_location_available", false, location) {

			@Override
			public void run() {
				boolean enabled = true;
				try {
					// check whether the process can be found
					enabled = RepositoryLocation.getRepositoryLocation(location, getOperator()).locateEntry() instanceof ProcessEntry;
				} catch (UserError | RepositoryException e) {
					enabled = false;
				}
				final boolean enable = enabled;
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						openProcessButton.setEnabled(enable);
					}
				});
			}
		};
		t.setIndeterminate(true);
		t.start();

	}

	/**
	 * @return the text of the text field which cannot be null
	 */
	private String getTextFieldText() {
		return getTextField().getText() != null ? getTextField().getText() : "";
	}
}
