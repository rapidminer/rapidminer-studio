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
package com.rapidminer.gui.tools.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.rapidminer.gui.LoggedAbstractAction;
import com.rapidminer.gui.tools.ArrowButton;
import com.rapidminer.gui.tools.ViewToolBar;
import com.vlsolutions.swing.toolbars.VLToolBar;


/**
 *
 * @author Tobias Malbrecht
 * @deprecated use {@link DropDownPopupButton} instead
 */
@Deprecated
public abstract class DropDownButton extends JButton {

	private static final long serialVersionUID = -5987392204641149649L;

	private final PopupMenuListener popupMenuListener = new PopupMenuListener() {

		@Override
		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
			popupVisible = true;
			mainButton.getModel().setRollover(true);
			arrowButton.getModel().setSelected(true);
		}

		@Override
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			popupVisible = false;

			mainButton.getModel().setRollover(false);
			arrowButton.getModel().setSelected(false);
			((JPopupMenu) e.getSource()).removePopupMenuListener(this);
		}

		@Override
		public void popupMenuCanceled(PopupMenuEvent e) {
			popupVisible = false;
		}
	};

	private final ChangeListener changeListener = new ChangeListener() {

		@Override
		public void stateChanged(ChangeEvent e) {
			if (e.getSource() == mainButton.getModel()) {
				if (popupVisible && !mainButton.getModel().isRollover()) {
					mainButton.getModel().setRollover(true);
					return;
				}
				arrowButton.getModel().setRollover(mainButton.getModel().isRollover());
				arrowButton.setSelected(mainButton.getModel().isArmed() && mainButton.getModel().isPressed());
			} else {
				if (popupVisible && !arrowButton.getModel().isSelected()) {
					arrowButton.getModel().setSelected(true);
					return;
				}
				mainButton.getModel().setRollover(arrowButton.getModel().isRollover());
			}
		}
	};

	private final class DefaultArrowAction extends LoggedAbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent ae) {
			JPopupMenu popup = getPopupMenu();
			popup.addPopupMenuListener(popupMenuListener);
			popup.show(mainButton,
					isRightAlign() ? -popup.getPreferredSize().width + mainButton.getWidth() + arrowButton.getWidth() : 0,
					mainButton.getHeight());
		}
	}

	protected JButton mainButton = this;

	private boolean rightAlign = false;

	@Deprecated
	public static class DropDownArrowButton extends ArrowButton {

		private static final long serialVersionUID = -398619111521186260L;

		private float sizeFactor = 1;

		private DropDownButton attachedButton;

		public DropDownArrowButton(DropDownButton attachedButton) {
			super(SwingConstants.SOUTH);
			this.attachedButton = attachedButton;
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			GeneralPath arrow = new GeneralPath();
			int w, h;
			h = (int) (2 * sizeFactor);
			w = (int) (4 * sizeFactor);
			arrow.moveTo(getWidth() / 2 - w, getHeight() / 2);
			arrow.lineTo(getWidth() / 2 + w, getHeight() / 2);
			arrow.lineTo(getWidth() / 2, getHeight() / 2 + 2 * h);
			arrow.closePath();
			if (isEnabled()) {
				g.setColor(Color.BLACK);
			} else {
				g.setColor(Color.GRAY);
			}
			((Graphics2D) g).fill(arrow);
		}

		/**
		 *
		 * This method gets the factor for enlargement or reduction of the arrow, if it should be
		 * displayed in a non-standard size.
		 *
		 */
		public float getSizeFactor() {
			return sizeFactor;
		}

		/**
		 *
		 * This method determines the factor for enlargement or reduction of the arrow, if it should
		 * be displayed in a non-standard size. Standard is a width of 4 px and a height of 8 px.
		 *
		 * @param sizeFactor
		 */
		public void setSizeFactor(float sizeFactor) {
			this.sizeFactor = sizeFactor;
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension dim = new Dimension((int) super.getPreferredSize().getWidth(),
					(int) attachedButton.getPreferredSize().getHeight());
			return dim;
		}

		@Override
		public Dimension getSize() {
			Dimension dim = new Dimension((int) super.getSize().getWidth(), (int) attachedButton.getSize().getHeight());
			return dim;
		}

		@Override
		public Dimension getMaximumSize() {
			Dimension dim = new Dimension((int) super.getMaximumSize().getWidth(),
					(int) attachedButton.getMaximumSize().getHeight());
			return dim;
		}

		@Override
		public Dimension getMinimumSize() {
			Dimension dim = new Dimension((int) super.getMinimumSize().getWidth(),
					(int) attachedButton.getMinimumSize().getHeight());
			return dim;
		}
	}

	protected final DropDownArrowButton arrowButton = new DropDownArrowButton(this);

	protected boolean popupVisible = false;

	public DropDownButton(Action mainAction, Action arrowAction, boolean showButton) {
		super(mainAction != null ? mainAction : arrowAction);	// if main action is null, use
		// dropdown action for it as well so
		// user is not confused
		mainButton.setText(null);
		mainButton.setOpaque(showButton);
		mainButton.setBorderPainted(showButton);
		mainButton.setMargin(new Insets(0, 0, 0, 0));
		mainButton.getModel().addChangeListener(changeListener);
		arrowButton.getModel().addChangeListener(changeListener);
		arrowButton.setFocusable(false);
		if (arrowAction != null) {
			arrowButton.addActionListener(arrowAction);
		} else {
			arrowButton.addActionListener(new DefaultArrowAction());
		}
		arrowButton.setMargin(new Insets(0, 0, 0, 0));
		mainButton.addPropertyChangeListener("enabled", new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				arrowButton.setEnabled(mainButton.isEnabled());
			}
		});
	}

	public DropDownButton(Action action, boolean showText) {
		this(action, null, showText);
	}

	/**
	 * Shows no text on buttons.
	 */
	public DropDownButton(Action action) {
		this(action, false);
	}

	public void addArrowButtonMouseListener(MouseListener l) {
		arrowButton.addMouseListener(l);
	}

	public void removeArrowButtonMouseListener(MouseListener l) {
		arrowButton.removeMouseListener(l);
	}

	protected abstract JPopupMenu getPopupMenu();

	public void add(Action action) {
		getPopupMenu().add(action);
	}

	public void add(JMenuItem item) {
		getPopupMenu().add(item);
	}

	public JButton addToToolBar(JToolBar toolbar) {
		toolbar.add(mainButton);
		toolbar.add(arrowButton);
		return mainButton;
	}

	public JButton addToToolBar(JToolBar toolbar, Object mainButtonConstraints, Object arrowButtonConstraints) {
		toolbar.add(mainButton, mainButtonConstraints);
		toolbar.add(arrowButton, arrowButtonConstraints);
		return mainButton;
	}

	public JButton addToToolbar(JPanel toolbar, Object mainButtonConstraints, Object arrowButtonConstraints) {
		toolbar.add(mainButton, mainButtonConstraints);
		toolbar.add(arrowButton, arrowButtonConstraints);
		return mainButton;
	}

	public JButton addToToolBar(VLToolBar toolbar) {
		toolbar.add(mainButton);
		toolbar.add(arrowButton);
		return mainButton;
	}

	public JButton addToToolBar(ViewToolBar toolbar, int alignment) {
		toolbar.add(mainButton, alignment);
		toolbar.add(arrowButton, alignment);
		return mainButton;
	}

	public JButton addToToolbar(JToolBar toolbar, Object mainButtonConstraints, Object arrowButtonConstraints) {
		toolbar.add(mainButton, mainButtonConstraints);
		toolbar.add(arrowButton, arrowButtonConstraints);
		return mainButton;

	}

	public JButton addToFlowLayoutPanel(JPanel panel) {
		panel.add(mainButton);
		panel.add(arrowButton);
		return mainButton;
	}

	public JButton addArrowToFlowLayoutPanel(JPanel panel) {
		panel.add(mainButton);
		panel.add(arrowButton);
		return mainButton;
	}

	public JPanel getArrowButtonBorderPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints(0, 0, GridBagConstraints.RELATIVE, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
		panel.add(mainButton, gbc); // , BorderLayout.CENTER);
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0;
		panel.add(arrowButton, gbc); // , BorderLayout.EAST);
		mainButton.setBorder(null);
		arrowButton.setBorder(null);
		panel.setBackground(UIManager.getColor("Button.background"));
		return panel;
	}

	// factory methods
	public static DropDownButton makeDropDownButton(Action mainAction, Action... actions) {
		return makeDropDownButton(mainAction, false, actions);
	}

	public static DropDownButton makeDropDownButton(Action mainAction, boolean showButton, Action... actions) {
		final JPopupMenu menu = new JPopupMenu();
		for (Action action : actions) {
			menu.add(action);
		}
		return new DropDownButton(mainAction, showButton) {

			private static final long serialVersionUID = -7359018188605409766L;

			@Override
			protected JPopupMenu getPopupMenu() {
				return menu;
			}
		};
	}

	public boolean isArrowButtonVisible() {
		return arrowButton.isVisible();
	}

	public void setArrowSizeFactor(float factor) {
		arrowButton.setSizeFactor(factor);
	}

	/**
	 * After this is called, ckicks on the main button act like clicking on the arrow button. <br/>
	 * This is useful if the main action is only used to set icon/tooltip, but not a real action.
	 */
	public void setUsePopupActionOnMainButton() {
		mainButton.addActionListener(new DefaultArrowAction());
	}

	public float getArrowSizeFactor() {
		return arrowButton.getSizeFactor();
	}

	public boolean isPopupMenuVisible() {
		return popupVisible;
	}

	public static DropDownButton makeDropDownButton(Action action) {
		final JPopupMenu menu = new JPopupMenu();
		return new DropDownButton(action) {

			private static final long serialVersionUID = -7359018188605409766L;

			@Override
			protected JPopupMenu getPopupMenu() {
				return menu;
			}
		};
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		if (arrowButton != null) {
			arrowButton.setEnabled(b);
		}
	}

	public boolean isRightAlign() {
		return rightAlign;
	}

	public void setRightAlign(boolean rightAlign) {
		this.rightAlign = rightAlign;
	}

}
