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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Predicate;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryLocationChooser;


/**
 * Cell editor that allows to select a repository entry by pressing a button.
 *
 * @author Simon Fischer
 *
 */
public class RepositoryLocationValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = 1L;

	private final JPanel panel = new JPanel();

	private final JTextField textField = new JTextField(12);

	private Operator operator;

	private final JButton button;

	public RepositoryLocationValueCellEditor(final ParameterTypeRepositoryLocation type) {
		GridBagLayout gridBagLayout = new GridBagLayout();
		panel.setLayout(gridBagLayout);
		panel.setToolTipText(type.getDescription());
		textField.setToolTipText(type.getDescription());
		textField.addActionListener(e -> fireEditingStopped());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = GridBagConstraints.RELATIVE;
		panel.add(textField, c);

		button = new JButton(new ResourceAction(true, "repository_select_location") {

			private static final long serialVersionUID = 1L;
			{
				putValue(NAME, null);
			}

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				com.rapidminer.Process process = RepositoryLocationValueCellEditor.this.operator != null ? RepositoryLocationValueCellEditor.this.operator
						.getProcess() : null;
				RepositoryLocation processLocation = null;
				if (process != null) {
					processLocation = process.getRepositoryLocation();
					if (processLocation != null) {
						processLocation = processLocation.parent();
					}
				}

				String locationName = RepositoryLocationChooser.selectLocation(processLocation, textField.getText(),
						panel, type.isAllowEntries(), type.isAllowFolders(), false,
						type.isEnforceValidRepositoryEntryName(), type.isOnlyWriteableLocations(), getRepositoryFilter());
				if (locationName != null) {
					textField.setText(locationName);
				}
				fireEditingStopped();
			}
		});
		button.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				// fire only if the focus didn't move to the textField. If this check
				// would not be included, fireEditingStopped() would remove the
				// table from this RepositoryLocationValeCellEditor's listenerList,
				// and thus the call to fireEditingStopped() in the event handler of
				// the textField would be without effect, and thus the user's choice
				// would be dismissed.
				// Additionally, the event is only fired if the focus loss is permamently,
				// i.e. it is not fired if the user e.g. just switched to another window.
				// Otherwise any changes made after switching back to rapidminer would
				// not be saved for the same reasons as stated above.
				if (e.getOppositeComponent() != textField && !e.isTemporary()) {
					fireEditingStopped();
				}
			}
		});
		button.setMargin(new Insets(0, 0, 0, 0));
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		c.insets = new Insets(0, 5, 0, 0);
		panel.add(button, c);

		textField.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				// fire only if the focus didn't move to the button. If this check
				// would not be included, fireEditingStopped() would remove the
				// table from this RepositoryLocationValeCellEditor's listenerList,
				// and thus the call to fireEditingStopped() in the event handler of
				// the button would be without effect, and thus the user's choice
				// in the RepositoryBrowser dialog would be dismissed.
				// Additionally, the event is only fired if the focus loss is permamently,
				// i.e. it is not fired if the user e.g. just switched to another window.
				// Otherwise any changes made after switching back to rapidminer would
				// not be saved for the same reasons as stated above.
				if (e.getOppositeComponent() != button && !e.isTemporary()) {
					fireEditingStopped();
				}
			}
		});

	}

	/**
	 * If the items in a RepositoryLocationChooserDialog, containing a {@link com.rapidminer.repository.gui.RepositoryTree},
	 * should show only a subset of the whole tree, provide a {@link Predicate<Entry>} to accept those. Defaults to null,
	 * meaning everything is visible.
	 *
	 * @return the {@link Predicate<Entry>} that accepts {@link Entry Entries} that should be visualized in the {@link com.rapidminer.repository.gui.RepositoryTree}
	 * @since 9.4
	 */
	protected Predicate<Entry> getRepositoryFilter() {
		return null;
	}

	@Override
	public Object getCellEditorValue() {
		return textField.getText().trim().length() == 0 ? null : textField.getText().trim();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
		textField.setText(value == null ? "" : value.toString());
		return panel;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	@Override
	public void activate() {
		button.doClick();
	}

	/**
	 * @return the panel storing the cell editor text field and button
	 */
	protected JPanel getPanel() {
		return panel;
	}

	/**
	 * @return the text field storing the process location
	 */
	protected JTextField getTextField() {
		return textField;
	}

	/**
	 * @return the operator for this renderer
	 */
	protected Operator getOperator() {
		return operator;
	}
}
