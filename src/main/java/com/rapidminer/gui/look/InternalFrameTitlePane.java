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
package com.rapidminer.gui.look;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;

import javax.swing.Icon;
import javax.swing.JInternalFrame;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;


/**
 * The title pane used for internal frames.
 *
 * @author Ingo Mierswa
 */
public class InternalFrameTitlePane extends BasicInternalFrameTitlePane {

	private static final long serialVersionUID = -2918905049450451804L;

	private class RapidLookTitlePaneLayout extends BasicInternalFrameTitlePane.TitlePaneLayout {

		private final InternalFrameTitlePane pane;

		RapidLookTitlePaneLayout(InternalFrameTitlePane pane) {
			this.pane = pane;
		}

		@Override
		public void addLayoutComponent(String name, Component c) {}

		@Override
		public void removeLayoutComponent(Component c) {}

		@Override
		public Dimension preferredLayoutSize(Container c) {
			return minimumLayoutSize(c);
		}

		@Override
		public void layoutContainer(Container c) {
			boolean leftToRight = true; // TODO: properly support right to left
			int buttonHeight = this.pane.closeButton.getIcon().getIconHeight();

			Icon icon = this.pane.frame.getFrameIcon();
			int iconHeight = 0;
			if (icon != null) {
				iconHeight = icon.getIconHeight();
			}

			int w = this.pane.getWidth();
			int h = this.pane.getHeight();
			int x = leftToRight ? 2 + 6 : w - 16 - 2 - 6;
			if (this.pane.menuBar != null) {
				this.pane.menuBar.setBounds(x, (h - iconHeight) / 2, 16, 16);
			}

			x = leftToRight ? w - 16 - 2 - 6 : 2 + 6;

			if (this.pane.frame.isClosable()) {
				this.pane.closeButton.setBounds(x, (h - buttonHeight) / 2, 16, 14);
				x += leftToRight ? -(16 + 2) : 16 + 2;
			}

			if (this.pane.frame.isMaximizable()) {
				this.pane.maxButton.setBounds(x, (h - buttonHeight) / 2, 16, 14);
				x += leftToRight ? -(16 + 2) : 16 + 2;
			}

			if (this.pane.frame.isIconifiable()) {
				this.pane.iconButton.setBounds(x, (h - buttonHeight) / 2, 16, 14);
			}
		}
	}

	private Icon rolloverCloseIcon;

	private Icon rolloverIconifyIcon;

	private Icon rolloverMinimizeIcon;

	private Icon rolloverMaximumIcon;

	public InternalFrameTitlePane(JInternalFrame f) {
		super(f);
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
		this.rolloverCloseIcon = UIManager.getIcon("InternalFrame.rolloverCloseIcon");
		this.rolloverIconifyIcon = UIManager.getIcon("InternalFrame.rolloverIconifyIcon");
		this.rolloverMinimizeIcon = UIManager.getIcon("InternalFrame.rolloverMinimizeIcon");
		this.rolloverMaximumIcon = UIManager.getIcon("InternalFrame.rolloverMaximizeIcon");
	}

	@Override
	public void uninstallListeners() {
		super.uninstallListeners();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}

	@Override
	protected void paintTitleBackground(Graphics g) {
		int w = getWidth();
		int h = getHeight();
		if (!this.frame.isSelected() && !this.frame.isIcon()) {
			g.setColor(Colors.TAB_BACKGROUND);
		} else {
			g.setColor(Colors.TAB_BACKGROUND_SELECTED);
		}
		g.fillRect(0, 0, w, h);
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize();
		d.height = Math.max(25, d.height);
		return d;
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension d = super.getMinimumSize();
		d.height = Math.max(25, d.height);
		return d;
	}

	@Override
	protected void createButtons() {
		super.createButtons();
		this.iconButton.setContentAreaFilled(false);
		this.maxButton.setContentAreaFilled(false);
		this.closeButton.setContentAreaFilled(false);
		this.iconButton.setRolloverEnabled(true);
		this.maxButton.setRolloverEnabled(true);
		this.closeButton.setRolloverEnabled(true);
		setButtonIcons();
	}

	@Override
	protected void setButtonIcons() {
		super.setButtonIcons();
		if (this.frame.isIcon()) {
			this.iconButton.setIcon(this.minIcon);
			this.iconButton.setRolloverIcon(this.rolloverMinimizeIcon);
			this.maxButton.setIcon(this.maxIcon);
			this.maxButton.setRolloverIcon(this.rolloverMaximumIcon);
		} else if (this.frame.isMaximum()) {
			this.iconButton.setIcon(this.iconIcon);
			this.iconButton.setRolloverIcon(this.rolloverIconifyIcon);
			this.maxButton.setIcon(this.minIcon);
			this.maxButton.setRolloverIcon(this.rolloverMinimizeIcon);
		} else {
			this.iconButton.setIcon(this.iconIcon);
			this.iconButton.setRolloverIcon(this.rolloverIconifyIcon);
			this.maxButton.setIcon(this.maxIcon);
			this.maxButton.setRolloverIcon(this.rolloverMaximumIcon);
		}
		this.closeButton.setIcon(this.closeIcon);
		this.closeButton.setRolloverIcon(this.rolloverCloseIcon);
	}

	@Override
	protected LayoutManager createLayout() {
		return new RapidLookTitlePaneLayout(this);
	}
}
