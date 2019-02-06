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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.components.ToggleDropDownButton;


/**
 * An action that handles a boolean state which is toggled by the actionPerformed method.
 *
 * @author Tobias Malbrecht
 */
public abstract class ToggleAction extends ResourceAction {

	private static final long serialVersionUID = -4465114837957358373L;

	public interface ToggleActionListener {

		public void setSelected(boolean selected);
	}

	private class ToggleJCheckBoxMenuItem extends JCheckBoxMenuItem implements ToggleActionListener {

		private static final long serialVersionUID = 8604924475187496354L;

		private ToggleJCheckBoxMenuItem(Action action) {
			super(action);
			setSelected(ToggleAction.this.isSelected());
		}
	}

	private class ToggleJToggleButton extends JToggleButton implements ToggleActionListener {

		private static final long serialVersionUID = 8939204437291275737L;

		private ToggleJToggleButton(Action action) {
			super(action);
			setSelected(ToggleAction.this.isSelected());
		}
	}

	private abstract class ToggleJToggleDropDownButton extends ToggleDropDownButton implements ToggleActionListener {

		private static final long serialVersionUID = 1534764344656638939L;

		private ToggleJToggleDropDownButton(ToggleAction action) {
			super(action);
			setSelected(ToggleAction.this.isSelected());
		}
	}

	private boolean selected = false;

	private Collection<ToggleActionListener> listeners = new LinkedList<ToggleActionListener>();

	public ToggleAction(boolean smallIcon, String key, Object... args) {
		this(smallIcon, key, IconType.NORMAL, args);
	}

	public ToggleAction(boolean smallIcon, String key, IconType iconType, Object... args) {
		super(smallIcon, key, iconType, args);
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		setSelected(!isSelected());
		actionToggled(null);
	}

	public abstract void actionToggled(ActionEvent e);

	public void resetAction(boolean selected) {
		if (selected != isSelected()) {
			setSelected(!isSelected());
			actionToggled(null);
		}
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
		for (ToggleActionListener l : listeners) {
			l.setSelected(selected);
		}
	}

	public JCheckBoxMenuItem createMenuItem() {
		ToggleJCheckBoxMenuItem item = new ToggleJCheckBoxMenuItem(this);
		listeners.add(item);
		return item;
	}

	public JToggleButton createToggleButton() {
		ToggleJToggleButton button = new ToggleJToggleButton(this);
		listeners.add(button);
		return button;
	}

	public ToggleDropDownButton createDropDownToggleButton(final JPopupMenu popupMenu) {
		ToggleJToggleDropDownButton button = new ToggleJToggleDropDownButton(this) {

			private static final long serialVersionUID = 619422148555974973L;

			@Override
			protected JPopupMenu getPopupMenu() {
				return popupMenu;
			}
		};
		listeners.add(button);
		return button;
	}

	public void addToggleActionListener(ToggleActionListener l) {
		listeners.add(l);
	}

	public void removeToggleActionListener(ToggleActionListener l) {
		listeners.remove(l);
	}
}
