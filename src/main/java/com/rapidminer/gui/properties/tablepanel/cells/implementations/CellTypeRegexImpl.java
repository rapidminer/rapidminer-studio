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
package com.rapidminer.gui.properties.tablepanel.cells.implementations;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.rapidminer.gui.properties.RegexpPropertyDialog;
import com.rapidminer.gui.properties.tablepanel.TablePanel;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellType;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeRegex;
import com.rapidminer.gui.properties.tablepanel.model.TablePanelModel;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.tools.I18N;


/**
 * GUI component for the {@link TablePanel} for {@link CellTypeRegex}.
 *
 * @author Marco Boeck
 *
 */
public class CellTypeRegexImpl extends JPanel implements CellTypeRegex {

	private static final long serialVersionUID = -2006834470031594342L;

	/**
	 * Creates a panel for regex cells. Adds a regex config dialog button next to the field. Does
	 * not validate the model, so make sure this call works!
	 *
	 * @param model
	 * @param rowIndex
	 * @param columnIndex
	 * @param cellClass
	 */
	public CellTypeRegexImpl(final TablePanelModel model, final int rowIndex, final int columnIndex,
			final Class<? extends CellType> cellClass) {
		super();

		final JFormattedTextField field = CellTypeImplHelper.createFormattedTextField(model, rowIndex, columnIndex);
		setLayout(new BorderLayout());
		add(field, BorderLayout.CENTER);

		// add regex dialog button
		final StringBuilder searchTextBuilder = new StringBuilder();
		Collection<String> valueCollection = model.getPossibleValuesForCellOrNull(rowIndex, columnIndex);
		if (valueCollection != null && !valueCollection.isEmpty()) {
			int counter = 0;
			for (String value : valueCollection) {
				counter++;
				searchTextBuilder.append(value);
				searchTextBuilder.append(' ');
				if (counter >= 100) {
					// for preview purposes and to reduce load, only display 100 items
					break;
				}
			}
			searchTextBuilder.deleteCharAt(searchTextBuilder.length() - 1);
		} else {
			// create empty collection to avoid NPEs later
			valueCollection = new LinkedList<>();
		}
		final Collection<String> values = valueCollection;
		final JButton regexButton = new JButton(new ResourceAction(true, "regexp") {

			private static final long serialVersionUID = 3989811306286704326L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				RegexpPropertyDialog dialog = new RegexpPropertyDialog(values, field.getText(),
						I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.action.regex_description.label"));
				dialog.setSearchFieldText(searchTextBuilder.toString());
				dialog.setVisible(true);
				if (dialog.wasConfirmed()) {
					field.setText(dialog.getRegexp());
				}
			}
		});
		add(regexButton, BorderLayout.EAST);

		// add ctrl+space shortcut for regex dialog
		Action caAction = new AbstractAction() {

			private static final long serialVersionUID = 5092311623220201432L;

			@Override
			public void actionPerformed(ActionEvent e) {
				regexButton.doClick();
			}
		};
		field.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				"contentAssistAction");
		field.getActionMap().put("contentAssistAction", caAction);

		// set text to model value
		String text = String.valueOf(model.getValueAt(rowIndex, columnIndex));
		field.setText(text);

		// set size so panels don't grow larger when they get the chance
		setPreferredSize(new Dimension(300, 20));
		setMinimumSize(new Dimension(100, 15));
		setMaximumSize(new Dimension(1600, 30));
	}

}
