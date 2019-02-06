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

import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.swingx.border.DropShadowBorder;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.LoggedAbstractAction;
import com.rapidminer.gui.tools.ArrowButton;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.tools.FontTools;
import com.vlsolutions.swing.toolbars.VLToolBar;


/**
 * This is basically a {@link FancyDropDownButton} which looks fancier. Sadly the
 * {@link DropDownButton} is not very override friendly in terms of painting, so this class was
 * added which basically duplicates functionality from {@link DropDownButton}.
 *
 * @author Tobias Malbrecht, Marco Boeck
 */
public abstract class FancyDropDownButton extends JButton implements FancyConstants {

	private static final long serialVersionUID = 7512959784153390965L;

	private final class DefaultArrowAction extends LoggedAbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent ae) {
			JPopupMenu popup = getPopupMenu();
			popup.addPopupMenuListener(popupMenuListener);

			int popupPrefHeight = (int) popup.getPreferredSize().getHeight();
			int buttonY = mainButton.getLocationOnScreen().y;

			boolean showOnTop = false;

			GraphicsConfiguration graphicsConf = ApplicationFrame.getApplicationFrame().getGraphicsConfiguration();
			if (graphicsConf != null) {
				int windowHeight = (int) graphicsConf.getBounds().getHeight();
				showOnTop = buttonY + mainButton.getHeight() + popupPrefHeight > windowHeight;
			}

			if (showOnTop) {
				popup.show(mainButton, 0, -popupPrefHeight);
			} else {
				popup.show(mainButton, 0, mainButton.getHeight());
			}
		}
	}

	public static class DropDownArrowButton extends ArrowButton {

		private static final long serialVersionUID = -398619111521186260L;

		private float sizeFactor = 1;

		private JButton mainButton;

		public DropDownArrowButton(JButton mainButton) {
			super(SwingConstants.SOUTH);
			this.mainButton = mainButton;
		}

		@Override
		public void paintComponent(Graphics g) {
			if (mainButton != null && mainButton.getModel().isArmed() || getModel().isArmed()) {
				((Graphics2D) g).translate(1.1, 1.1);
			}
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			super.paintComponent(g);

			Graphics2D g2 = (Graphics2D) g.create();

			// fill background white
			g2.setColor(BACKGROUND_COLOR);
			g2.fillRect(3, 0, getWidth() - 9, getHeight() - 6);

			// draw border which complements the DropShadow
			g2.setColor(BORDER_LIGHTEST_GRAY);
			g2.drawLine(3, 0, 3, getHeight() - 6);
			g2.drawLine(getWidth() - 6, 0, getWidth() - 6, getHeight() - 6);
			g2.drawLine(3, getHeight() - 6, getWidth() - 6, getHeight() - 6);
			g2.drawLine(3, 0, getWidth() - 6, 0);

			// draw arrow
			GeneralPath arrow = new GeneralPath();
			int w, h;
			h = (int) (2 * sizeFactor);
			w = (int) (4 * sizeFactor);
			arrow.moveTo(getWidth() / 2 - w, getHeight() / 2);
			arrow.lineTo(getWidth() / 2 + w, getHeight() / 2);
			arrow.lineTo(getWidth() / 2, getHeight() / 2 + 2 * h);
			arrow.closePath();
			if (isEnabled()) {
				g2.setColor(HOVERED_TEXTCOLOR);
			} else {
				g2.setColor(BORDER_LIGHTEST_GRAY);
			}
			g2.fill(arrow);
			g2.dispose();
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
		public boolean isRolloverEnabled() {
			// force it to disable rollover because we have custom hover highlighting
			return false;
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension prefSize = super.getPreferredSize();
			prefSize.setSize(prefSize.getWidth() + 75, prefSize.getHeight() + 20);
			return prefSize;
		}

		@Override
		public Dimension getMinimumSize() {
			Dimension minSize = super.getMinimumSize();
			minSize.setSize(75, minSize.getHeight());
			return minSize;
		}
	}

	private final PopupMenuListener popupMenuListener = new PopupMenuListener() {

		@Override
		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
			popupVisible = true;
			arrowButton.getModel().setSelected(true);
		}

		@Override
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			popupVisible = false;

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
			arrowButton.repaint();
			mainButton.repaint();
		}
	};

	protected JButton mainButton = this;

	protected final DropDownArrowButton arrowButton = new DropDownArrowButton(this);

	protected boolean popupVisible = false;

	private final JToolBar arrowButtonPanel = new JToolBar();

	private final JPanel emptyPanel = new JPanel();

	/** flag indicating if the button is hovered */
	private boolean hovered;

	private MouseListener hoverListener = new MouseAdapter() {

		@Override
		public void mouseExited(MouseEvent e) {
			hovered = false;
			setBorderPainted(false);
			arrowButton.setBorderPainted(false);
			setForeground(NORMAL_TEXTCOLOR);
			repaint();
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			hovered = true;
			setBorderPainted(true);
			arrowButton.setBorderPainted(true);
			setForeground(HOVERED_TEXTCOLOR);
			repaint();
		}
	};

	private FocusListener focusListener = new FocusAdapter() {

		@Override
		public void focusLost(FocusEvent e) {
			if (e.getOppositeComponent() != arrowButton && e.getOppositeComponent() != getPopupMenu()) {
				hovered = false;
				setBorderPainted(false);
				arrowButton.setBorderPainted(false);
				setForeground(NORMAL_TEXTCOLOR);
				repaint();
			}
		}
	};

	public FancyDropDownButton(Action mainAction, Action arrowAction, boolean showText) {
		super(mainAction);
		if (!showText) {
			mainButton.setText(null);
		}
		mainButton.setOpaque(false);
		arrowButton.setOpaque(false);
		mainButton.setBorderPainted(false);
		mainButton.setMargin(new Insets(0, 0, 0, 0));
		mainButton.getModel().addChangeListener(changeListener);
		arrowButton.getModel().addChangeListener(changeListener);
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

		arrowButtonPanel.setLayout(new CardLayout());
		arrowButtonPanel.setFloatable(false);
		arrowButtonPanel.setBorder(null);
		arrowButton.setBorderPainted(true);
		emptyPanel.setOpaque(false);
		arrowButtonPanel.setOpaque(false);

		// setup fancy button things
		addMouseListener(hoverListener);
		addArrowButtonMouseListener(hoverListener);
		addFocusListener(focusListener);

		setBorderPainted(false);
		setBorder(new DropShadowBorder(SHADOW_COLOR, 5, 0.5f, 12, false, false, true, true));
		arrowButton.setBorder(new DropShadowBorder(SHADOW_COLOR, 5, 0.5f, 12, false, false, true, true));
		setFocusPainted(false);
		setContentAreaFilled(false);
		setHorizontalAlignment(SwingConstants.LEFT);
		setHorizontalTextPosition(SwingConstants.RIGHT);

		setForeground(NORMAL_TEXTCOLOR);
		setFont(FontTools.getFont("Open Sans Light", Font.PLAIN, 28));

		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		arrowButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	public FancyDropDownButton(Action action, boolean showText) {
		this(action, null, showText);
	}

	/**
	 * Shows no text on buttons.
	 */
	public FancyDropDownButton(Action action) {
		this(action, false);
	}

	public void addArrowButtonMouseListener(MouseListener l) {
		arrowButton.addMouseListener(l);
		emptyPanel.addMouseListener(l);
	}

	public void removeArrowButtonMouseListener(MouseListener l) {
		arrowButton.removeMouseListener(l);
		emptyPanel.removeMouseListener(l);
	}

	protected abstract JPopupMenu getPopupMenu();

	public void add(Action action) {
		getPopupMenu().add(action);
	}

	public void add(JMenuItem item) {
		getPopupMenu().add(item);
	}

	public JButton addToToolBar(JToolBar toolbar) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		toolbar.add(mainButton);
		toolbar.add(arrowButtonPanel);
		return mainButton;
	}

	public JButton addToToolBar(JToolBar toolbar, Object mainButtonConstraints, Object arrowButtonConstraints) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		toolbar.add(mainButton, mainButtonConstraints);
		toolbar.add(arrowButtonPanel, arrowButtonConstraints);
		return mainButton;
	}

	public JButton addToToolbar(JPanel toolbar, Object mainButtonConstraints, Object arrowButtonConstraints) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		toolbar.add(mainButton, mainButtonConstraints);
		toolbar.add(arrowButtonPanel, arrowButtonConstraints);
		return mainButton;
	}

	public JButton addToToolBar(VLToolBar toolbar) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		toolbar.add(mainButton);
		toolbar.add(arrowButtonPanel);
		return mainButton;
	}

	public JButton addToToolBar(ViewToolBar toolbar, int alignment) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		toolbar.add(mainButton, alignment);
		toolbar.add(arrowButtonPanel, alignment);
		return mainButton;
	}

	public JButton addToToolbar(JToolBar toolbar, Object mainButtonConstraints, Object arrowButtonConstraints) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		toolbar.add(mainButton, mainButtonConstraints);
		toolbar.add(arrowButtonPanel, arrowButtonConstraints);
		return mainButton;

	}

	public JButton addToFlowLayoutPanel(JPanel panel) {
		arrowButtonPanel.add(arrowButton);
		arrowButtonPanel.add(emptyPanel);
		panel.add(mainButton);
		panel.add(arrowButtonPanel);
		return mainButton;
	}

	public JButton addArrowToFlowLayoutPanel(JPanel panel) {
		// mainButton.setVisible(false);
		panel.add(mainButton);
		panel.add(arrowButton);
		return mainButton;
	}

	public void setArrowButtonVisible(boolean b) {
		CardLayout cl = (CardLayout) arrowButtonPanel.getLayout();
		if (b) {
			cl.first(arrowButtonPanel);
		} else {
			cl.last(arrowButtonPanel);
		}
	}

	public boolean isArrowButtonVisible() {
		return arrowButton.isVisible();
	}

	public void setArrowSizeFactor(float factor) {
		arrowButton.setSizeFactor(factor);
	}

	public float getArrowSizeFactor() {
		return arrowButton.getSizeFactor();
	}

	public boolean isPopupMenuVisible() {
		return popupVisible;
	}

	/**
	 * Returns <code>true</code> if the mouse currently hovers over this button.
	 *
	 * @return
	 */
	public boolean isHovered() {
		return hovered;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension prefSize = super.getPreferredSize();
		prefSize.setSize(prefSize.getWidth() + 75, prefSize.getHeight() + 20);
		return prefSize;
	}

	@Override
	public boolean isRolloverEnabled() {
		// force it to disable rollover because we have custom hover highlighting
		return false;
	}

	@Override
	public void paintComponent(Graphics g) {
		if (getModel().isArmed()) {
			((Graphics2D) g).translate(1.1, 1.1);
		}

		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Graphics2D g2 = (Graphics2D) g.create();
		if (hovered) {
			// fill background white
			g2.setColor(BACKGROUND_COLOR);
			g2.fillRect(0, 0, getWidth() - 6, getHeight() - 6);

			// draw border which complements the DropShadow
			g2.setColor(BORDER_LIGHTEST_GRAY);
			g2.drawLine(0, 0, 0, getHeight() - 6);
			g2.drawLine(0, getHeight() - 6, getWidth() - 6, getHeight() - 6);
			g2.drawLine(0, 0, getWidth() - 6, 0);
			g2.drawLine(getWidth() - 6, 0, getWidth() - 6, getHeight() - 6);

			// cut off arrow ends so the cut looks like an imaginary vertical line
			g2.setColor(BACKGROUND_COLOR);
			g2.fillRect((int) (getWidth() * 0.81), (int) (getHeight() * 0.29), 7, (int) (getHeight() * 0.5));
		}
		g2.dispose();

		super.paintComponent(g);
	}

	// factory methods
	public static FancyDropDownButton makeDropDownButton(Action mainAction, Action... actions) {
		final JPopupMenu menu = new JPopupMenu();
		for (Action action : actions) {
			menu.add(action);
		}
		return new FancyDropDownButton(mainAction) {

			private static final long serialVersionUID = -7359018188605409766L;

			@Override
			protected JPopupMenu getPopupMenu() {
				return menu;
			}
		};
	}

	public static FancyDropDownButton makeDropDownButton(Action action) {
		final JPopupMenu menu = new JPopupMenu();
		return new FancyDropDownButton(action) {

			private static final long serialVersionUID = -7359018188605409766L;

			@Override
			protected JPopupMenu getPopupMenu() {
				return menu;
			}
		};
	}

}
