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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ActionMapUIResource;


/**
 * This is the tri state check box.
 * 
 * @author Santhosh Kumar, Ingo Mierswa
 */
public class ExtendedTriStateCheckBox extends JCheckBox {

	private static final long serialVersionUID = 8924026691487760529L;

	private final TristateDecorator model;

	public ExtendedTriStateCheckBox(String text, Icon icon, Boolean initial) {
		super(text, icon);
		// Add a listener for when the mouse is pressed
		super.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				grabFocus();
				model.nextState();
			}
		});
		// Reset the keyboard action map
		ActionMap map = new ActionMapUIResource();
		map.put("pressed", new AbstractAction() { // NOI18N

					private static final long serialVersionUID = 0L;

					@Override
					public void actionPerformed(ActionEvent e) {
						grabFocus();
						model.nextState();
					}
				});
		map.put("released", null); // NOI18N
		SwingUtilities.replaceUIActionMap(this, map);
		// set the model to the adapted model
		model = new TristateDecorator(getModel());
		setModel(model);
		setState(initial);
	}

	public ExtendedTriStateCheckBox(String text, Boolean initial) {
		this(text, null, initial);
	}

	public ExtendedTriStateCheckBox(String text) {
		this(text, null);
	}

	public ExtendedTriStateCheckBox() {
		this(null);
	}

	/** No one may add mouse listeners, not even Swing! */
	@Override
	public void addMouseListener(MouseListener l) {}

	/**
	 * Set the new state to either SELECTED, NOT_SELECTED or DONT_CARE. If state == null, it is
	 * treated as DONT_CARE.
	 */
	public void setState(Boolean state) {
		model.setState(state);
	}

	/**
	 * Return the current state, which is determined by the selection status of the model.
	 */
	public Boolean getState() {
		return model.getState();
	}

	/**
	 * Exactly which Design Pattern is this? Is it an Adapter, a Proxy or a Decorator? In this case,
	 * my vote lies with the Decorator, because we are extending functionality and "decorating" the
	 * original model with a more powerful model.
	 */
	private static class TristateDecorator implements ButtonModel {

		private final ButtonModel other;

		private TristateDecorator(ButtonModel other) {
			this.other = other;
		}

		private void setState(Boolean state) {
			if (state == null) {
				other.setArmed(true);
				setPressed(true);
				setSelected(false);
			} else if (state.booleanValue() == Boolean.FALSE) {
				other.setArmed(false);
				setPressed(false);
				setSelected(false);
			} else if (state.booleanValue() == Boolean.TRUE) {
				other.setArmed(false);
				setPressed(false);
				setSelected(true);
			}
		}

		/**
		 * The current state is embedded in the selection / armed state of the model.
		 * 
		 * We return the SELECTED state when the checkbox is selected but not armed, DONT_CARE state
		 * when the checkbox is selected and armed (grey) and NOT_SELECTED when the checkbox is
		 * deselected.
		 */
		private Boolean getState() {
			if (isSelected() && !isArmed()) {
				// normal black tick
				return Boolean.TRUE;
			} else if (isSelected() && isArmed()) {
				// don't care grey tick
				return null;
			} else {
				// normal deselected
				return Boolean.FALSE;
			}
		}

		/** We rotate between NOT_SELECTED, SELECTED and DONT_CARE. */
		private void nextState() {
			Boolean current = getState();
			if (current == Boolean.FALSE) {
				setState(Boolean.TRUE);
			} else if (current == Boolean.TRUE) {
				setState(null);
			} else if (current == null) {
				setState(Boolean.FALSE);
			}
		}

		/** Filter: No one may change the armed status except us. */
		@Override
		public void setArmed(boolean b) {}

		/**
		 * We disable focusing on the component when it is not enabled.
		 */
		@Override
		public void setEnabled(boolean b) {
			// setFocusable(b);
			other.setEnabled(b);
		}

		/**
		 * All these methods simply delegate to the "other" model that is being decorated.
		 */
		@Override
		public boolean isArmed() {
			return other.isArmed();
		}

		@Override
		public boolean isSelected() {
			return other.isSelected();
		}

		@Override
		public boolean isEnabled() {
			return other.isEnabled();
		}

		@Override
		public boolean isPressed() {
			return other.isPressed();
		}

		@Override
		public boolean isRollover() {
			return other.isRollover();
		}

		@Override
		public void setSelected(boolean b) {
			other.setSelected(b);
		}

		@Override
		public void setPressed(boolean b) {
			other.setPressed(b);
		}

		@Override
		public void setRollover(boolean b) {
			other.setRollover(b);
		}

		@Override
		public void setMnemonic(int key) {
			other.setMnemonic(key);
		}

		@Override
		public int getMnemonic() {
			return other.getMnemonic();
		}

		@Override
		public void setActionCommand(String s) {
			other.setActionCommand(s);
		}

		@Override
		public String getActionCommand() {
			return other.getActionCommand();
		}

		@Override
		public void setGroup(ButtonGroup group) {
			other.setGroup(group);
		}

		@Override
		public void addActionListener(ActionListener l) {
			other.addActionListener(l);
		}

		@Override
		public void removeActionListener(ActionListener l) {
			other.removeActionListener(l);
		}

		@Override
		public void addItemListener(ItemListener l) {
			other.addItemListener(l);
		}

		@Override
		public void removeItemListener(ItemListener l) {
			other.removeItemListener(l);
		}

		@Override
		public void addChangeListener(ChangeListener l) {
			other.addChangeListener(l);
		}

		@Override
		public void removeChangeListener(ChangeListener l) {
			other.removeChangeListener(l);
		}

		@Override
		public Object[] getSelectedObjects() {
			return other.getSelectedObjects();
		}
	}
}
