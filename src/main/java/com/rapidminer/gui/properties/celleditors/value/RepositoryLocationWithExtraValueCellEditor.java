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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;

/**
 * Abstract repository location cell editor that is specialized for a special {@link Entry} type and adds a second button
 * that allows to interact with the selected entry if it is of the specified type.
 *
 * @see #getExtraActionKey()
 * @see #doExtraAction(RepositoryLocation)
 * @see #getExpectedEntryClass()
 *
 * @author Marcel Seifert, Nils Woehler, Jan Czogalla
 * @since 9.3
 */
public abstract class RepositoryLocationWithExtraValueCellEditor extends RepositoryLocationValueCellEditor {
	private final JPanel surroundingPanel = new JPanel(new GridBagLayout());
	private final JButton extraButton = new JButton(new ResourceAction(true, getExtraActionKey()) {

		private static final long serialVersionUID = 1L;

		{
			putValue(NAME, null);
		}

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			RepositoryLocation repositoryLocation;
			try {
				if (getTextField() != null) {
					repositoryLocation = RepositoryLocation.getRepositoryLocation(getTextFieldText(), getOperator());
					if (repositoryLocation != null) {
						doExtraAction(repositoryLocation);
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
			checkExtraButtonEnabled();
		}

		@Override
		public void editingCanceled(ChangeEvent e) {
			// do nothing
		}
	};

	public RepositoryLocationWithExtraValueCellEditor(ParameterTypeRepositoryLocation type) {
		super(type);
		extraButton.setEnabled(false);

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		surroundingPanel.add(getPanel(), gbc);

		gbc.gridx += 1;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = new Insets(0, 2, 0, 0);
		surroundingPanel.add(extraButton, gbc);
		addCellEditorListener(listener);

	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
		// ensure text field is filled with correct values
		super.getTableCellEditorComponent(table, value, isSelected, row, col);
		checkExtraButtonEnabled();
		return surroundingPanel;
	}

	@Override
	public void activate() {
		if (extraButton.isEnabled()) {
			extraButton.doClick();
		} else {
			super.activate();
		}
	}

	/** The i18n key for the extra action (i.e. second button). Only the icon will be used. */
	protected abstract String getExtraActionKey();

	/**
	 * The action behind the second button.
	 *
	 * @param repositoryLocation
	 * 		the location to execute the action on; will never be called with {@code null}
	 */
	protected abstract void doExtraAction(RepositoryLocation repositoryLocation);

	/** Returns the expected (super) class of allowed entries. */
	protected abstract Class<? extends Entry> getExpectedEntryClass();

	/**
	 * Checks whether the provided repository location is valid and is of the correct type.
	 *
	 * @see #getExpectedEntryClass()
	 */
	private void checkExtraButtonEnabled() {
		final String location = getTextFieldText();
		ProgressThread t = new ProgressThread("check_process_location_available", false, location) {

			@Override
			public void run() {
				boolean enabled = true;
				try {
					// check whether the lcoation can be found and is of correct type
					enabled = getExpectedEntryClass().isInstance(RepositoryLocation.getRepositoryLocation(location, getOperator()).locateEntry());
				} catch (UserError | RepositoryException e) {
					enabled = false;
				}
				final boolean enable = enabled;
				SwingUtilities.invokeLater(() -> extraButton.setEnabled(enable));
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
