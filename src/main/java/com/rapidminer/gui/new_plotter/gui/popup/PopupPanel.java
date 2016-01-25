/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.gui.new_plotter.gui.popup;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Popup;
import javax.swing.SwingUtilities;


/**
 * A JPanel that contains the component that should be shown when a popup action is triggered.
 * 
 * @author Nils Woehler
 * 
 */
public class PopupPanel extends JPanel implements PropertyChangeListener {

	private static final long serialVersionUID = 1L;

	private final List<PopupComponentListener> listenerList = new LinkedList<PopupComponentListener>();

	private static final String PERMANENT_FOCUS_OWNER = "permanentFocusOwner";

	private Window containingWindow;

	public PopupPanel(Component comp) {
		this.setLayout(new GridBagLayout());

		GridBagConstraints itemConstraint = new GridBagConstraints();

		itemConstraint = new GridBagConstraints();
		itemConstraint.fill = GridBagConstraints.BOTH;
		itemConstraint.weightx = 1.0;
		itemConstraint.weighty = 1.0;
		itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
		itemConstraint.insets = new Insets(5, 5, 5, 5);
		this.setBorder(new JPopupMenu().getBorder());
		this.add(comp, itemConstraint);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		Component newValue = (Component) evt.getNewValue();
		if (newValue == null) {
			return;
		}
		if (!isFocusInside(newValue)) {
			fireFocusLost();
		}
	}

	public void addListener(PopupComponentListener l) {
		listenerList.add(l);
	}

	public void removeListener(PopupComponentListener l) {
		listenerList.remove(l);
	}

	/**
	 * Starts tracking of focus change.
	 * 
	 * @param containingWindow
	 *            the window that contains the popup
	 * @param actionSource
	 */
	public void startTracking(Window containingWindow, Component actionSource) {
		this.containingWindow = containingWindow;
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(PERMANENT_FOCUS_OWNER, this);
	}

	public void stopTracking() {
		this.containingWindow = null;
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(PERMANENT_FOCUS_OWNER, this);
	}

	private void fireFocusLost() {
		for (PopupComponentListener l : listenerList) {
			l.focusLost();
		}
		stopTracking();
	}

	/**
	 * Checks if the focus is still on this component or its child components.
	 */
	private boolean isFocusInside(Object newFocusedComp) {
		if (newFocusedComp instanceof Popup) {
			return true;
		}
		if (newFocusedComp instanceof Component && !SwingUtilities.isDescendingFrom((Component) newFocusedComp, this)) {
			// Check if focus is on other window
			if (containingWindow == null) {
				return false;
			}

			Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();

			// if focus is on other window return true
			if (containingWindow == focusedWindow) {
				return false;
			}
		}
		return true;
	}
}
