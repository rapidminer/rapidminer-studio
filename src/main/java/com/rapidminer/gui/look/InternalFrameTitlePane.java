/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
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
			int x = (leftToRight) ? 2 + 6 : w - 16 - 2 - 6;
			if (this.pane.menuBar != null) {
				this.pane.menuBar.setBounds(x, (h - iconHeight) / 2, 16, 16);
			}

			x = (leftToRight) ? w - 16 - 2 - 6 : 2 + 6;

			if (this.pane.frame.isClosable()) {
				this.pane.closeButton.setBounds(x, (h - buttonHeight) / 2, 16, 14);
				x += (leftToRight) ? -(16 + 2) : 16 + 2;
			}

			if (this.pane.frame.isMaximizable()) {
				this.pane.maxButton.setBounds(x, (h - buttonHeight) / 2, 16, 14);
				x += (leftToRight) ? -(16 + 2) : 16 + 2;
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
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][0]);
			g.drawLine(8, 0, w - 9, 0);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][1]);
			g.drawLine(10, 0, w - 11, 0);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][2]);
			g.drawLine(6, 1, w - 7, 1);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][3]);
			g.drawLine(7, 1, w - 8, 1);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][4]);
			g.drawLine(8, 1, w - 9, 1);

			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][5]);
			g.drawLine(5, 2, w - 6, 2);
			g.drawLine(4, 3, w - 5, 3);
			g.drawLine(3, 4, w - 4, 4);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][6]);
			g.drawLine(1, 5, w - 2, 5);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][7]);
			g.drawLine(2, 5, w - 3, 5);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][8]);
			g.drawLine(1, 6, w - 2, 6);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][9]);
			g.drawLine(2, 6, w - 3, 6);
			g.fillRect(1, 7, w - 2, h - 1);

			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][10]);
			g.drawLine(2, 4, 4, 2);
			g.drawLine(w - 3, 4, w - 5, 2);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][11]);
			g.drawLine(0, 7, 0, 7);
			g.drawLine(w - 1, 7, w - 1, 7);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][12]);
			g.drawLine(0, 8, 0, 8);
			g.drawLine(w - 1, 8, w - 1, 8);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[0][13]);
			g.drawLine(0, 9, 0, h - 1);
			g.drawLine(w - 1, 9, w - 1, h - 1);
		} else {
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][0]);
			g.drawLine(8, 0, w - 9, 0);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][1]);
			g.drawLine(10, 0, w - 11, 0);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][2]);
			g.drawLine(6, 1, w - 7, 1);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][3]);
			g.drawLine(7, 1, w - 8, 1);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][4]);
			g.drawLine(8, 1, w - 9, 1);

			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][5]);
			g.drawLine(5, 2, w - 6, 2);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][6]);
			g.drawLine(4, 3, w - 5, 3);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][7]);
			g.drawLine(3, 4, w - 4, 4);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][8]);
			g.drawLine(1, 5, w - 2, 5);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][9]);
			g.drawLine(2, 5, w - 3, 5);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][10]);
			g.drawLine(1, 6, w - 2, 6);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][11]);
			g.drawLine(2, 6, w - 3, 6);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][12]);
			g.drawLine(1, 7, w - 2, 7);

			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][13]);
			g.drawLine(1, 8, w - 2, 8);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][14]);
			g.drawLine(1, 9, w - 2, 9);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][15]);
			g.drawLine(1, 10, w - 2, 10);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][16]);
			g.drawLine(1, 11, w - 2, 11);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][17]);
			g.drawLine(1, 12, w - 2, 12);
			g.fillRect(1, 12, w - 2, h - 20);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][18]);
			g.drawLine(1, h - 11, w - 2, h - 11);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][19]);
			g.drawLine(1, h - 10, w - 2, h - 10);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][20]);
			g.drawLine(1, h - 9, w - 2, h - 9);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][21]);
			g.drawLine(1, h - 8, w - 2, h - 8);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][22]);
			g.drawLine(1, h - 7, w - 2, h - 7);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][23]);
			g.drawLine(1, h - 6, w - 2, h - 6);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][24]);
			g.drawLine(1, h - 5, w - 2, h - 5);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][25]);
			g.drawLine(1, h - 4, w - 2, h - 4);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][26]);
			g.drawLine(1, h - 3, w - 2, h - 3);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][27]);
			g.drawLine(1, h - 2, w - 2, h - 2);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][28]);
			g.drawLine(1, h - 1, w - 2, h - 1);

			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][29]);
			g.drawLine(2, 4, 4, 2);
			g.drawLine(w - 3, 4, w - 5, 2);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][30]);
			g.drawLine(0, 7, 0, 7);
			g.drawLine(w - 1, 7, w - 1, 7);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][31]);
			g.drawLine(0, 8, 0, 8);
			g.drawLine(w - 1, 8, w - 1, 8);
			g.setColor(RapidLookTools.getColors().getInternalFrameTitlePaneColors()[1][32]);
			g.drawLine(0, 9, 0, h - 1);
			g.drawLine(w - 1, 9, w - 1, h - 1);
		}
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
