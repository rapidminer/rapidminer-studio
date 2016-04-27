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
package com.rapidminer.gui.tools;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import com.rapidminer.gui.look.Colors;


/**
 * This {@link JPopupMenu} extension displays its contents in a scrollpane. The maximum height can
 * be set as well as a custom width of this popup menu. Furthermore, focus traversal via TAB works
 * for any {@link Component} added to this popupmenu, not only for {@link JMenuItem}s.
 *
 * @author Marco Boeck
 *
 */
public class ScrollableJPopupMenu extends JPopupMenu {

	private static final long serialVersionUID = 7440641917394853639L;

	private static final int INSETS = 5;

	public static final int SIZE_TINY = 100;
	public static final int SIZE_SMALL = 200;
	public static final int SIZE_NORMAL = 400;
	public static final int SIZE_LARGE = 600;
	public static final int SIZE_HUGE = 800;

	/** the scrollpane which allows scrolling */
	private JScrollPane scrollPane;

	/** the inner panel which houses all components */
	private JPanel innerPanel;

	/** the title text for the menu, displayed above scrollpane */
	private String title;

	/** the max height in pixel for the scrollpane */
	private int maxHeight;

	/** the max width in pixel for the scrollpane */
	private int maxWidth;

	/** if not null, will be used to determine the width of the scrollpane */
	private Integer customWidth;

	/**
	 * Creates a new {@link ScrollJPopupMenu} instance with the default max height.
	 *
	 */
	public ScrollableJPopupMenu() {
		this(null, SIZE_NORMAL);
	}

	/**
	 * Creates a new {@link ScrollJPopupMenu} instance with the specified max height.
	 *
	 * @param maxHeight
	 */
	public ScrollableJPopupMenu(int maxHeight) {
		this(null, maxHeight);
	}

	/**
	 * Creates a new {@link ScrollJPopupMenu} instance with the specified title.
	 *
	 * @param title
	 */
	public ScrollableJPopupMenu(String title) {
		this(title, SIZE_NORMAL);
	}

	/**
	 * Creates a new {@link ScrollJPopupMenu} instance with the specified max height and title.
	 *
	 * @param title
	 * @param maxHeight
	 */
	public ScrollableJPopupMenu(String title, int maxHeight) {
		super();
		if (maxHeight < SIZE_TINY) {
			throw new IllegalArgumentException("size must not be smaller than " + SIZE_TINY);
		}
		this.title = title;
		this.maxHeight = maxHeight;
		this.maxWidth = 800;

		initGUI();
	}

	/**
	 * Initializes the GUI.
	 */
	private void initGUI() {
		innerPanel = new JPanel();
		innerPanel.setBackground(Colors.MENU_ITEM_BACKGROUND);
		innerPanel.setLayout(new BoxLayout(this.innerPanel, BoxLayout.Y_AXIS));
		scrollPane = new ExtendedJScrollPane(innerPanel);
		scrollPane.setBorder(null);
		scrollPane.setMaximumSize(new Dimension(maxWidth, maxHeight));

		// allows closing of popup via Escape (which consumes the event)
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false),
				"close");
		getActionMap().put("close", new AbstractAction() {

			private static final long serialVersionUID = 6744591919263755414L;

			@Override
			public void actionPerformed(ActionEvent e) {
				ScrollableJPopupMenu.this.setVisible(false);
			}
		});

		// add optional title
		if (title != null) {
			super.add(new JLabel(title));
		}

		// add scrollpane
		super.add(scrollPane);

		// set focus policy so we only cycle through components in our scrollpane
		setFocusTraversalPolicyProvider(true);
		setFocusTraversalPolicy(new FocusTraversalPolicy() {

			@Override
			public Component getLastComponent(Container aContainer) {
				if (innerPanel.getComponentCount() <= 0) {
					return null;
				}
				return innerPanel.getComponent(innerPanel.getComponentCount() - 1);
			}

			@Override
			public Component getFirstComponent(Container aContainer) {
				if (innerPanel.getComponentCount() <= 0) {
					return null;
				}
				return innerPanel.getComponent(0);
			}

			@Override
			public Component getDefaultComponent(Container aContainer) {
				return getFirstComponent(aContainer);
			}

			@Override
			public Component getComponentBefore(Container aContainer, Component aComponent) {
				if (innerPanel.getComponentCount() <= 0) {
					return null;
				}
				for (int i = 0; i < innerPanel.getComponentCount(); i++) {
					if (innerPanel.getComponent(i) == aComponent) {
						if (i == 0) {
							return getLastComponent(aContainer);
						}
						Component previousComp = innerPanel.getComponent(i - 1);
						// needed so focus cyling does not stop at a separator
						if (previousComp instanceof Separator) {
							return getComponentBefore(aContainer, previousComp);
						} else {
							return previousComp;
						}
					}
				}
				return getFirstComponent(aContainer);
			}

			@Override
			public Component getComponentAfter(Container aContainer, Component aComponent) {
				if (innerPanel.getComponentCount() <= 0) {
					return null;
				}
				for (int i = 0; i < innerPanel.getComponentCount(); i++) {
					if (innerPanel.getComponent(i) == aComponent) {
						if (i == innerPanel.getComponentCount() - 1) {
							return getFirstComponent(aContainer);
						}
						Component nextComp = innerPanel.getComponent(i + 1);
						// needed so focus cyling does not stop at a separator
						if (nextComp instanceof Separator) {
							return getComponentAfter(aContainer, nextComp);
						} else {
							return nextComp;
						}
					}
				}
				return getLastComponent(aContainer);
			}
		});
	}

	@Override
	public Component add(final Component comp) {
		innerPanel.add(comp);
		resizeScrollPane();

		return comp;
	}

	@Override
	public void remove(Component comp) {
		innerPanel.remove(comp);
		resizeScrollPane();
	}

	/**
	 * Updates the preferred size of the scrollpane depending on the components of this popup menu.
	 */
	private void resizeScrollPane() {
		int width = customWidth == null ? innerPanel.getPreferredSize().width
				+ scrollPane.getVerticalScrollBar().getPreferredSize().width + INSETS : customWidth - INSETS;
		scrollPane
		.setPreferredSize(new Dimension(width, Math.min(maxHeight, innerPanel.getPreferredSize().height + INSETS)));
	}

	/**
	 * Sets the fixed custom width. If set to <code>null</code>, will not use a fixed width.
	 *
	 * @param customWidth
	 */
	public void setCustomWidth(Integer customWidth) {
		this.customWidth = customWidth;
		resizeScrollPane();
	}

	/**
	 * Returns all {@link Component}s inside the scrollpane.
	 *
	 * @return
	 */
	public Component[] getComponentsInsideScrollpane() {
		return innerPanel.getComponents();
	}

	/**
	 * Requets the focus on the first component inside the scrollpane. Does nothing if no components
	 * exist.
	 *
	 * @return {@link Component#requestFocusInWindow()}
	 */
	private boolean requestFocusForFirstComponent() {
		if (innerPanel.getComponentCount() > 0) {
			return innerPanel.getComponent(0).requestFocusInWindow();
		}
		return false;
	}

	@Override
	public boolean requestFocusInWindow() {
		return requestFocusForFirstComponent();
	}
}
