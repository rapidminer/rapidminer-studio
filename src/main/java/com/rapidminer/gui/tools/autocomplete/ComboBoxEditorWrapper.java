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
package com.rapidminer.gui.tools.autocomplete;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.ComboBoxEditor;


/**
 * Wrapper class for ComboBoxEditor
 * <p>
 * Allows to use a custom styled ComboBoxEditor together with AutoComplete
 *
 * @since 8.1.2
 * @author Jonas Wilms-Pfau
 */
class ComboBoxEditorWrapper implements ComboBoxEditor, FocusListener {

	/** The wrapped ComboBoxEditor */
	private final ComboBoxEditor wrapped;

	ComboBoxEditorWrapper(ComboBoxEditor wrapped) {
		if (wrapped == null) {
			throw new IllegalArgumentException("wrapped ComboBoxEditor must not be null");
		}
		this.wrapped = wrapped;
	}

	@Override
	public Component getEditorComponent() {
		return wrapped.getEditorComponent();
	}

	@Override
	public void setItem(Object anObject) {
		wrapped.setItem(anObject);
	}

	@Override
	public Object getItem() {
		return wrapped.getItem();
	}

	@Override
	public void selectAll() {
		wrapped.selectAll();
	}

	@Override
	public void addActionListener(ActionListener l) {
		wrapped.addActionListener(l);
	}

	@Override
	public void removeActionListener(ActionListener l) {
		wrapped.removeActionListener(l);
	}

	@Override
	public void focusGained(FocusEvent e) {
		if (wrapped instanceof FocusListener) {
			((FocusListener) wrapped).focusGained(e);
		}
	}

	@Override
	public void focusLost(FocusEvent e) {
		if (wrapped instanceof FocusListener) {
			((FocusListener) wrapped).focusLost(e);
		}
	}
}
