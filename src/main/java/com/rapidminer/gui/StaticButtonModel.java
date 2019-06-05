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
package com.rapidminer.gui;

import static java.awt.event.KeyEvent.VK_UNDEFINED;

import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.event.ChangeListener;

/**
 * A static {@link ButtonModel} that can be used on {@link javax.swing.JRadioButton JRadioButtons} to make them a
 * mere indicator.
 * <p>
 * Inspired by <a href="https://coderanch.com/t/346898/java/Making-radio-buttons-noneditable">
 *     https://coderanch.com/t/346898/java/Making-radio-buttons-noneditable</a>
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class StaticButtonModel implements ButtonModel {

	private boolean selected;

	public StaticButtonModel(boolean selected) {
		this.selected = selected;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getActionCommand() {
		return "static";
	}

	@Override
	public int getMnemonic() {
		return VK_UNDEFINED;
	}

	@Override
	public boolean isSelected() {
		return selected;
	}

	//region noop/default methods
	@Override
	public boolean isArmed() {
		return false;
	}

	@Override
	public boolean isPressed() {
		return false;
	}

	@Override
	public boolean isRollover() {
		return false;
	}

	@Override
	public void setArmed(boolean b) {
		// noop
	}

	@Override
	public void setSelected(boolean b) {
		// noop
	}

	@Override
	public void setEnabled(boolean b) {
		// noop
	}

	@Override
	public void setPressed(boolean b) {
		// noop
	}

	@Override
	public void setRollover(boolean b) {
		// noop
	}

	@Override
	public void setMnemonic(int key) {
		// noop
	}

	@Override
	public void setActionCommand(String s) {
		// noop
	}

	@Override
	public void setGroup(ButtonGroup group) {
		// noop
	}

	@Override
	public void addActionListener(ActionListener l) {
		// noop
	}

	@Override
	public void removeActionListener(ActionListener l) {
		// noop
	}

	@Override
	public Object[] getSelectedObjects() {
		return null;
	}

	@Override
	public void addItemListener(ItemListener l) {
		// noop
	}

	@Override
	public void removeItemListener(ItemListener l) {
		// noop
	}

	@Override
	public void addChangeListener(ChangeListener l) {
		// noop
	}

	@Override
	public void removeChangeListener(ChangeListener l) {
		// noop
	}
	//endregion
}
