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
package com.rapidminer.gui.look;

import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 * The editor for combo boxes.
 *
 * @author Ingo Mierswa
 */
public class RapidLookComboBoxEditor extends BasicComboBoxEditor {

	private static String PROMPT = I18N.getMessage(I18N.getGUIBundle(), "gui.input_combobox.label");

	private JTextField textField;

	public static class UIResource extends RapidLookComboBoxEditor implements javax.swing.plaf.UIResource {
	}

	public void putClientProperty(Object key, Object val) {
		this.textField.putClientProperty(key, val);
	}

	public void setEnable(boolean val) {
		this.editor.setEnabled(val);
		if (val) {
			SwingTools.setPrompt(PROMPT, textField);
		} else {
			SwingTools.setPrompt("", textField);
		}
	}

	public RapidLookComboBoxEditor() {
		this.editor.removeFocusListener(this);
		this.textField = new JTextField("", 9) {

			private static final long serialVersionUID = 1L;

			@Override
			public void setText(String s) {
				if (getText().equals(s)) {
					return;
				}
				super.setText(s);
			}
		};
		if (editor.isEnabled()) {
			setEnable(true);
		}
		textField.setBorder(null);
		this.editor = this.textField;
	}
}
