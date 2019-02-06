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

import com.rapidminer.gui.look.borders.Borders;
import com.rapidminer.gui.look.borders.DummyBorder;
import com.rapidminer.gui.look.borders.PopupBorder;
import com.rapidminer.gui.look.borders.ShadowedPopupMenuBorder;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;

import javax.swing.JApplet;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JRootPane;
import javax.swing.Popup;
import javax.swing.SwingUtilities;


/**
 * A popup object in form of a rounded rectangle.
 *
 * @author Ingo Mierswa
 */
public final class RoundedRectanglePopup extends Popup {

	private static final Rectangle rectangle = new Rectangle();

	private BufferedImage topRight;
	private BufferedImage topLeft;
	private BufferedImage bottomRight;
	private BufferedImage bottomLeft;
	private BufferedImage bottom;
	private BufferedImage right;

	private Dimension size;

	private Component contents;

	private int x, y;

	private Popup popup;

	private Robot robot;

	private Component owner;

	private static final Point point = new Point();

	private static Rectangle bufferRectangle = new Rectangle();

	static Popup getInstance(Component owner, Component contents, int x, int y, Popup delegate) {
		RoundedRectanglePopup popup;

		synchronized (RoundedRectanglePopup.class) {
			popup = new RoundedRectanglePopup();
		}

		popup.initPopup(owner, contents, x, y, delegate);
		return popup;
	}

	@Override
	public void hide() {
		this.popup.hide();
		((JComponent) this.contents).putClientProperty(RoundedPopupFactory.BOTTOM_LEFT_PIC, null);
		((JComponent) this.contents).putClientProperty(RoundedPopupFactory.BOTTOM_RIGHT_PIC, null);
		((JComponent) this.contents).putClientProperty(RoundedPopupFactory.TOP_LEFT_PIC, null);
		((JComponent) this.contents).putClientProperty(RoundedPopupFactory.TOP_RIGHT_PIC, null);
		((JComponent) this.contents).putClientProperty(RoundedPopupFactory.RIGHT_PIC, null);
		((JComponent) this.contents).putClientProperty(RoundedPopupFactory.BOTTOM_PIC, null);

		this.contents = null;
		this.popup = null;
	}

	@Override
	public void show() {
		if ((((JComponent) this.contents).getBorder() instanceof PopupBorder)
				|| (((JComponent) this.contents).getBorder() instanceof ShadowedPopupMenuBorder)) {
			updatePics();
		}
		this.popup.show();
	}

	private void initPopup(Component owner, Component contents, int x, int y, Popup popup) {
		this.owner = owner;
		this.contents = contents;
		this.popup = popup;
		this.x = x;
		this.y = y;

		boolean mac = false;
		try {
			mac = System.getProperty("os.name").toLowerCase().startsWith("mac");
		} catch (SecurityException e) {
			// do nothing
		}
		if (mac) {
			((JComponent) contents).setBorder(Borders.getPopupMenuBorder());
		} else if (((JComponent) contents).getBorder() instanceof DummyBorder) {
			if ((owner != null) //
					&& (((owner instanceof JMenu) && ((JMenu) owner).isTopLevelMenu()) //
					|| ((owner.getParent() != null) && (owner.getParent() instanceof javax.swing.JToolBar)) //
					|| (owner instanceof javax.swing.JComboBox))) {
				((JComponent) contents).setBorder(Borders.getPopupBorder());
			} else {
				((JComponent) contents).setBorder(Borders.getShadowedPopupMenuBorder());
			}
		}
	}

	private void updatePics() {
		try {
			this.contents.requestFocus();
			this.robot = new Robot();
			this.size = this.contents.getPreferredSize();
			int w = this.size.width;
			int h = this.size.height;

			rectangle.setBounds(this.x, this.y, 5, 5);
			this.topLeft = this.robot.createScreenCapture(rectangle);
			rectangle.setBounds(this.x + w - 5, this.y, 5, 5);
			this.topRight = this.robot.createScreenCapture(rectangle);
			rectangle.setBounds(this.x, this.y + h - 5, 5, 5);
			this.bottomLeft = this.robot.createScreenCapture(rectangle);
			rectangle.setBounds(this.x + w - 5, this.y + h - 5, 5, 5);
			this.bottomRight = this.robot.createScreenCapture(rectangle);

			rectangle.setBounds(this.x + w - 1, this.y, 1, h - 5);
			this.right = this.robot.createScreenCapture(rectangle);
			rectangle.setBounds(this.x + 5, this.y + h - 1, w - 10, 1);
			this.bottom = this.robot.createScreenCapture(rectangle);

			Component layeredPane = getLayeredPane();
			if (layeredPane == null) {
				return;
			}

			point.x = this.x;
			point.y = this.y;
			SwingUtilities.convertPointFromScreen(point, layeredPane);

			bufferRectangle = new Rectangle(point.x, point.y, 5, 5);
			drawRemaining(bufferRectangle, layeredPane, this.topLeft);
			bufferRectangle = new Rectangle(point.x + w - 5, point.y, 5, 5);
			drawRemaining(bufferRectangle, layeredPane, this.topRight);
			bufferRectangle = new Rectangle(point.x, point.y + h - 5, 5, 5);
			drawRemaining(bufferRectangle, layeredPane, this.bottomLeft);
			bufferRectangle = new Rectangle(point.x + w - 5, point.y + h - 5, 5, 5);
			drawRemaining(bufferRectangle, layeredPane, this.bottomRight);

			bufferRectangle = new Rectangle(point.x + w - 1, point.y, 1, h - 5);
			drawRemaining(bufferRectangle, layeredPane, this.right);
			bufferRectangle = new Rectangle(point.x + 5, point.y + h - 1, w - 10, 1);
			drawRemaining(bufferRectangle, layeredPane, this.bottom);

			((JComponent) this.contents).putClientProperty(RoundedPopupFactory.BOTTOM_LEFT_PIC, this.bottomLeft);
			((JComponent) this.contents).putClientProperty(RoundedPopupFactory.BOTTOM_RIGHT_PIC, this.bottomRight);
			((JComponent) this.contents).putClientProperty(RoundedPopupFactory.TOP_LEFT_PIC, this.topLeft);
			((JComponent) this.contents).putClientProperty(RoundedPopupFactory.TOP_RIGHT_PIC, this.topRight);
			((JComponent) this.contents).putClientProperty(RoundedPopupFactory.RIGHT_PIC, this.right);
			((JComponent) this.contents).putClientProperty(RoundedPopupFactory.BOTTOM_PIC, this.bottom);
		} catch (Exception e) {
			// do nothing
		}
	}

	private void drawRemaining(Rectangle rectangle, Component lp, BufferedImage pic) {
		if ((rectangle.x + rectangle.width) > lp.getWidth()) {
			rectangle.width = lp.getWidth() - rectangle.x;
		}
		if ((rectangle.y + rectangle.height) > lp.getHeight()) {
			rectangle.height = lp.getHeight() - rectangle.y;
		}
		if (!rectangle.isEmpty()) {
			Graphics g = pic.createGraphics();
			g.translate(-rectangle.x, -rectangle.y);
			g.setClip(rectangle);
			boolean doubleBuffered = lp.isDoubleBuffered();
			if (lp instanceof JComponent) {
				((JComponent) lp).setDoubleBuffered(false);
				lp.paint(g);
				((JComponent) lp).setDoubleBuffered(doubleBuffered);
			} else {
				lp.paint(g);
			}
			g.dispose();
		}
	}

	private Component getLayeredPane() {
		Container parent = null;
		if (this.owner != null) {
			parent = this.owner instanceof Container ? (Container) this.owner : this.owner.getParent();
		}
		for (Container p = parent; p != null; p = p.getParent()) {
			if (p instanceof JRootPane) {
				if (p.getParent() instanceof JInternalFrame) {
					continue;
				}
				parent = ((JRootPane) p).getLayeredPane();
			} else if (p instanceof Window) {
				if (parent == null) {
					parent = p;
				}
				break;
			} else if (p instanceof JApplet) {
				break;
			}
		}
		return parent;
	}
}
