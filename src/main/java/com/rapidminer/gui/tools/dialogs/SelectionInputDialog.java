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
package com.rapidminer.gui.tools.dialogs;

import java.awt.Window;
import java.util.Collection;

import javax.swing.JComboBox;

import com.rapidminer.gui.ApplicationFrame;


/**
 *
 * @author Tobias Malbrecht
 */
public class SelectionInputDialog extends ButtonDialog {

	private static final long serialVersionUID = -5825873580778775409L;

	private final JComboBox<Object> comboBox = new JComboBox<>();

	/**
	 * Create a SelectionInputDIalog.
	 *
	 * @param key
	 *            i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @deprecated use {@link #SelectionInputDialog(Window, String, Object[], Object)}
	 */
	@Deprecated
	public SelectionInputDialog(String key, Object[] selectionValues, Object initialSelectionValue) {
		this(ApplicationFrame.getApplicationFrame(), key, selectionValues, initialSelectionValue);
	}

	/**
	 * Create a SelectionInputDIalog.
	 *
	 * @param key
	 *            the i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @deprecated use {@link #SelectionInputDialog(Window, String, Object[], Object, Object...)}
	 *             instead
	 */
	@Deprecated
	public SelectionInputDialog(String key, Object[] selectionValues, Object initialSelectionValue, Object... keyArguments) {
		this(ApplicationFrame.getApplicationFrame(), key, selectionValues, initialSelectionValue, keyArguments);
	}

	/**
	 * Create a SelectionInputDIalog whose Combobox can be editable.
	 *
	 * @param key
	 *            the i18n key
	 * @param editable
	 *            if the selection should be editable
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @deprecated use
	 *             {@link #SelectionInputDialog(Window, String, boolean, Collection, Object, Object...)}
	 *             instead
	 */
	@Deprecated
	public SelectionInputDialog(String key, boolean editable, Collection<?> selectionValues, Object initialSelectionValue,
			Object... keyArguments) {
		this(ApplicationFrame.getApplicationFrame(), key, editable, selectionValues, initialSelectionValue, keyArguments);
	}

	/**
	 * Create a SelectionInputDIalog whose Combobox can be editable.
	 *
	 * @param key
	 *            the i18n key
	 * @param editable
	 *            if the selection should be editable
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @deprecated use
	 *             {@link #SelectionInputDialog(Window, String, boolean, Object[], Object, Object...)}
	 *             instead
	 */
	@Deprecated
	public SelectionInputDialog(String key, boolean editable, Object[] selectionValues, Object initialSelectionValue,
			Object... keyArguments) {
		this(ApplicationFrame.getApplicationFrame(), key, editable, selectionValues, initialSelectionValue, keyArguments);
	}

	/**
	 * Create a SelectionInputDIalog.
	 *
	 * @param key
	 *            the i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @deprecated use {@link #SelectionInputDialog(Window, String, Collection, Object, Object...)}
	 *             instead
	 */
	@Deprecated
	public SelectionInputDialog(String key, Collection<?> selectionValues, Object initialSelectionValue,
			Object... keyArguments) {
		this(ApplicationFrame.getApplicationFrame(), key, selectionValues, initialSelectionValue, keyArguments);
	}

	/**
	 * Create a SelectionInputDIalog.
	 *
	 * @param key
	 *            i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @since 6.5.0
	 */
	public SelectionInputDialog(Window owner, String key, Object[] selectionValues, Object initialSelectionValue) {
		this(owner, key, selectionValues, initialSelectionValue, new Object[] {});
	}

	/**
	 * Create a SelectionInputDIalog.
	 *
	 * @param key
	 *            the i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @since 6.5.0
	 */
	public SelectionInputDialog(Window owner, String key, Object[] selectionValues, Object initialSelectionValue,
			Object... keyArguments) {
		super(owner, "input." + key, ModalityType.APPLICATION_MODAL,
				keyArguments);
		for (Object selectionValue : selectionValues) {
			comboBox.addItem(selectionValue);
		}
		comboBox.setSelectedItem(initialSelectionValue);
		layoutDefault(comboBox, makeOkButton(), makeCancelButton());
	}

	/**
	 * Create a SelectionInputDIalog whose Combobox can be editable.
	 *
	 * @param key
	 *            the i18n key
	 * @param editable
	 *            if the selection should be editable
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @since 6.5.0
	 */
	public SelectionInputDialog(Window owner, String key, boolean editable, Collection<?> selectionValues,
			Object initialSelectionValue, Object... keyArguments) {
		this(owner, key, selectionValues, initialSelectionValue, keyArguments);
		comboBox.setEditable(editable);
	}

	/**
	 * Create a SelectionInputDIalog whose Combobox can be editable.
	 *
	 * @param key
	 *            the i18n key
	 * @param editable
	 *            if the selection should be editable
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @since 6.5.0
	 */
	public SelectionInputDialog(Window owner, String key, boolean editable, Object[] selectionValues,
			Object initialSelectionValue, Object... keyArguments) {
		this(owner, key, selectionValues, initialSelectionValue, keyArguments);
		comboBox.setEditable(editable);
	}

	/**
	 * Create a SelectionInputDIalog.
	 *
	 * @param key
	 *            the i18n key
	 * @param selectionValues
	 *            the available selection values
	 * @param initialSelectionValue
	 *            the initially selected value
	 * @param keyArguments
	 *            additional i18n arguments
	 * @since 6.5.0
	 */
	public SelectionInputDialog(Window owner, String key, Collection<?> selectionValues, Object initialSelectionValue,
			Object... keyArguments) {
		super(owner, "input." + key, ModalityType.APPLICATION_MODAL,
				keyArguments);
		for (Object selectionValue : selectionValues) {
			comboBox.addItem(selectionValue);
		}
		comboBox.setSelectedItem(initialSelectionValue);
		layoutDefault(comboBox, makeOkButton(), makeCancelButton());
	}

	public Object getInputSelection() {
		return wasConfirmed() ? comboBox.getSelectedItem() : null;
	}
}
