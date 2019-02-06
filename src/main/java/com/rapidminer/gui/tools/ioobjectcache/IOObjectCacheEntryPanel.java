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
package com.rapidminer.gui.tools.ioobjectcache;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.operator.IOObjectMap;


/**
 * A {@link JPanel} representing a single entry of an {@link IOObjectMap}. The panel layout assumes
 * that it is used as part of a vertical list, i.e., that it represents a single row.
 *
 * @author Michael Knopf
 */
public class IOObjectCacheEntryPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Layout constraints for the entry's icon (first columns). */
	public static final GridBagConstraints ICON_CONSTRAINTS = new GridBagConstraints();
	static {
		ICON_CONSTRAINTS.anchor = GridBagConstraints.CENTER;
		ICON_CONSTRAINTS.insets = new Insets(1, 1, 1, 1);
	}

	/** CĹayout constraints for the entry's object type (second column). */
	public static final GridBagConstraints TYPE_CONSTRAINTS = new GridBagConstraints();
	static {
		TYPE_CONSTRAINTS.anchor = GridBagConstraints.WEST;
		TYPE_CONSTRAINTS.insets = new Insets(1, 8, 1, 1);
	}

	/** Layout constraints for the entry's key name (third columnd). */
	public static final GridBagConstraints KEY_CONSTRAINTS = new GridBagConstraints();
	static {
		KEY_CONSTRAINTS.anchor = GridBagConstraints.WEST;
		KEY_CONSTRAINTS.insets = new Insets(1, 1, 1, 1);
	}

	/** Layout constraints for the entry's remove button (last column). */
	public static final GridBagConstraints REMOVE_BUTTON_CONSTRAINTS = new GridBagConstraints();
	static {
		REMOVE_BUTTON_CONSTRAINTS.anchor = GridBagConstraints.EAST;
		REMOVE_BUTTON_CONSTRAINTS.insets = new Insets(1, 1, 1, 1);
	}

	/** The layout used for the panel. */
	public static final GridBagLayout ENTRY_LAYOUT = new GridBagLayout();
	private static final int MAX_TYPE_WIDTH = 128;
	static {
		ENTRY_LAYOUT.columnWidths = new int[] { 32, MAX_TYPE_WIDTH + 9, 0, 0 };
		ENTRY_LAYOUT.columnWeights = new double[] { 0.0, 0.0, 1.0, 0.0 };
	}

	/** Dimensions of the removal button. */
	private static final Dimension buttonSize = new Dimension(24, 24);

	/** Background color when highlighted. */
	private static final Color COLOR_HIGHLIGHT = Colors.MENU_ITEM_BACKGROUND_SELECTED;

	/** Default background color, may be modified to create alternating rows. */
	private Color defaultBackground;

	private Action openAction;

	/** Mouse movement adapter (required for hover effect). */
	private MouseListener hoverMouseListener = new MouseAdapter() {

		@Override
		public void mouseExited(MouseEvent e) {
			if (!SwingTools.isMouseEventExitedToChildComponents(IOObjectCacheEntryPanel.this, e)) {
				highlight(false);
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			highlight(true);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e)) {
				if (e.getClickCount() == 2) {
					openAction.actionPerformed(new ActionEvent(IOObjectCacheEntryPanel.this, ActionEvent.ACTION_PERFORMED,
							null));
				}
			}
		}
	};

	/** Mouse dispatching listener (required for hover effect). */
	private MouseListener dispatchMouseListener = new MouseListener() {

		@Override
		public void mouseClicked(MouseEvent e) {
			IOObjectCacheEntryPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e,
					IOObjectCacheEntryPanel.this));
		}

		@Override
		public void mousePressed(MouseEvent e) {
			IOObjectCacheEntryPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e,
					IOObjectCacheEntryPanel.this));
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			IOObjectCacheEntryPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e,
					IOObjectCacheEntryPanel.this));
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			IOObjectCacheEntryPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e,
					IOObjectCacheEntryPanel.this));
		}

		@Override
		public void mouseExited(MouseEvent e) {
			IOObjectCacheEntryPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e,
					IOObjectCacheEntryPanel.this));
		}

	};

	/**
	 * Creates a new {@link IOObjectCacheEntryPanel}.
	 *
	 * @param icon
	 *            The {@link Icon} associated with the entry's type.
	 * @param entryType
	 *            Human readable representation of the entry's type (e.g., 'Data Table').
	 * @param openAction
	 *            The action to be performed when clicking in the entry.
	 * @param removeAction
	 *            An action triggering the removal of the entry.
	 */
	public IOObjectCacheEntryPanel(Icon icon, String entryType, Action openAction, Action removeAction) {
		super(ENTRY_LAYOUT);

		this.openAction = openAction;

		// add icon label
		JLabel iconLabel = new JLabel(icon);
		add(iconLabel, ICON_CONSTRAINTS);

		// add object type label
		JLabel typeLabel = new JLabel(entryType);
		typeLabel.setMaximumSize(new Dimension(MAX_TYPE_WIDTH, 24));
		typeLabel.setPreferredSize(typeLabel.getMaximumSize());
		typeLabel.setToolTipText(entryType);
		add(typeLabel, TYPE_CONSTRAINTS);

		// add link button performing the specified action, the label displays the entry's key name
		LinkLocalButton openButton = new LinkLocalButton(openAction);
		openButton.setMargin(new Insets(0, 0, 0, 0));
		add(openButton, KEY_CONSTRAINTS);

		// add removal button
		JButton removeButton = new JButton(removeAction);
		removeButton.setBorderPainted(false);
		removeButton.setOpaque(false);
		removeButton.setMinimumSize(buttonSize);
		removeButton.setPreferredSize(buttonSize);
		removeButton.setContentAreaFilled(false);
		removeButton.setText(null);
		add(removeButton, REMOVE_BUTTON_CONSTRAINTS);

		// register mouse listeners
		addMouseListener(hoverMouseListener);
		iconLabel.addMouseListener(dispatchMouseListener);
		typeLabel.addMouseListener(dispatchMouseListener);
		openButton.addMouseListener(dispatchMouseListener);
		removeButton.addMouseListener(dispatchMouseListener);
	}

	/**
	 * @param color
	 *            The background {@link Color} use if the panel is not highlighted.
	 */
	public void setDefaultBackground(Color color) {
		this.defaultBackground = color;
		this.setBackground(color);
	}

	/**
	 * Enables or disables the highlighting of this panel.
	 *
	 * @param highlighted
	 *            Iff true, the background color of the panel is highlighted.
	 */
	private void highlight(boolean highlighted) {
		if (highlighted) {
			this.setBackground(COLOR_HIGHLIGHT);
		} else {
			this.setBackground(defaultBackground);
		}
	}
}
