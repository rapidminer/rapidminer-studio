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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JPanel;
import javax.swing.text.DefaultFormatterFactory;

import com.rapidminer.gui.properties.tablepanel.TablePanel;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellType;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldDefault;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldInteger;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldNumerical;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldTime;
import com.rapidminer.gui.properties.tablepanel.model.TablePanelModel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 * GUI component for the {@link TablePanel} for e.g. {@link CellTypeTextFieldDefault}.
 * 
 * @author Marco Boeck
 * 
 */
public class CellTypeTextFieldDefaultImpl extends JPanel implements CellTypeTextFieldDefault, CellTypeTextFieldInteger,
		CellTypeTextFieldNumerical, CellTypeTextFieldTime {

	private static final long serialVersionUID = 5923158263372081013L;

	/**
	 * Creates a {@link JFormattedTextField} for the specified cell. If a formatter is given, will
	 * apply it to the field. Does not validate the model, so make sure this call works!
	 * 
	 * @param model
	 * @param rowIndex
	 * @param columnIndex
	 * @param cellClass
	 * @param formatter
	 *            the formatter or <code>null</code> if none is required
	 * @param hideUnavailableContentAssist
	 * @return
	 */
	public CellTypeTextFieldDefaultImpl(final TablePanelModel model, final int rowIndex, final int columnIndex,
			final Class<? extends CellType> cellClass, AbstractFormatter formatter, boolean hideUnavailableContentAssist) {
		super();

		final JFormattedTextField field = CellTypeImplHelper.createFormattedTextField(model, rowIndex, columnIndex);
		setLayout(new BorderLayout());
		add(field, BorderLayout.CENTER);

		// otherwise 'null' would be restored
		Object value = model.getValueAt(rowIndex, columnIndex);
		String text = value != null ? String.valueOf(value) : "";

		// specical handling when formatter is given
		if (formatter != null) {
			field.setFormatterFactory(new DefaultFormatterFactory(formatter));
		}
		field.setText(text);

		// set syntax assist if available
		String syntaxHelp = model.getSyntaxHelpAt(rowIndex, columnIndex);
		if (syntaxHelp != null && !"".equals(syntaxHelp.trim())) {
			SwingTools.setPrompt(syntaxHelp, field);
		}

		// see if content assist is possible for this field, if so add it
		ImageIcon icon = SwingTools.createIcon("16/"
				+ I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.action.content_assist.icon"));
		JButton contentAssistButton = new JButton();
		contentAssistButton.setIcon(icon);
		if (field.isEnabled() && model.isContentAssistPossibleForCell(rowIndex, columnIndex)) {
			contentAssistButton.setToolTipText(I18N.getMessageOrNull(I18N.getGUIBundle(),
					"gui.action.content_assist_enabled.tip"));
			CellTypeImplHelper.addContentAssist(model, rowIndex, columnIndex, field, contentAssistButton, cellClass);
		} else {
			contentAssistButton.setToolTipText(I18N.getMessageOrNull(I18N.getGUIBundle(),
					"gui.action.content_assist_disabled.tip"));
			contentAssistButton.setEnabled(false);
		}
		if (contentAssistButton.isEnabled() || (!contentAssistButton.isEnabled() && !hideUnavailableContentAssist)) {
			add(contentAssistButton, BorderLayout.EAST);
		}

		// set size so panels don't grow larger when they get the chance
		setPreferredSize(new Dimension(300, 20));
		setMinimumSize(new Dimension(100, 15));
		setMaximumSize(new Dimension(1600, 30));
	}

}
