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
package com.rapidminer.gui.tools;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.Vector;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;


/**
 * A simple {@link JList} that can ignore the menu shortcut key when clicking.
 * Ignoring that key might implicate a constraint to be single selection only.
 *
 * @since 8.2
 * @author Jan Czogalla
 */
public class MenuShortcutJList<E> extends JList<E> {

	private boolean controlEnabled = true;

	public MenuShortcutJList() {
		super();
	}

	public MenuShortcutJList(boolean controlEnabled) {
		super();
		setControlEnabled(controlEnabled);
	}

	public MenuShortcutJList(E[] listData) {
		super(listData);
	}

	public MenuShortcutJList(E[] listData, boolean controlEnabled) {
		super(listData);
		setControlEnabled(controlEnabled);
	}

	public MenuShortcutJList(Vector<? extends E> listData) {
		super(listData);
	}

	public MenuShortcutJList(Vector<? extends E> listData, boolean controlEnabled) {
		super(listData);
		setControlEnabled(controlEnabled);
	}

	public MenuShortcutJList(ListModel<E> dataModel) {
		super(dataModel);
	}

	public MenuShortcutJList(ListModel<E> dataModel, boolean controlEnabled) {
		super(dataModel);
		setControlEnabled(controlEnabled);
	}

	@Override
	public void processMouseEvent(MouseEvent e) {
		if (!controlEnabled && SwingTools.isControlOrMetaDown(e)) {
			// remove menu shortcut key mask from event
			int modifiers = e.getModifiers();
			modifiers &= ~Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
			e = new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(), modifiers,
					e.getX(), e.getY(), e.getClickCount(), e.isPopupTrigger());
		}
		super.processMouseEvent(e);
	}

	/**
	 * @return whether the menu shortcut key is taken into account.
	 */
	public boolean isControlEnabled() {
		return controlEnabled;
	}

	/**
	 * Sets whether the menu shortcut key should be taken into account. If it should be ignored,
	 * i.e. {@code controlEnabled} is {@code false}, the selection mode will be automatically
	 * set to {@link ListSelectionModel#SINGLE_SELECTION}.
	 *
	 * @param controlEnabled
	 * 		whether the menu shortcut key should be taken into account
	 */
	public void setControlEnabled(boolean controlEnabled) {
		this.controlEnabled = controlEnabled;
		if (!controlEnabled) {
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
	}
}
