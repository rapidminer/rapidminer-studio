/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.gui.look.ui;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.GenericArrowButton;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.RapidLookTools;
import com.vlsolutions.swing.docking.DockTabbedPane;
import com.vlsolutions.swing.docking.DockViewAsTab.TabHeader;


/**
 * The UI for tabbed panes.
 *
 * @author Ingo Mierswa, Marco Boeck
 */
public class TabbedPaneUI extends BasicTabbedPaneUI {

	private class TabbedPaneMouseListener implements MouseMotionListener, MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {
			updateMouseOver(e.getPoint());
		}

		@Override
		public void mouseExited(MouseEvent e) {
			updateMouseOver(e.getPoint());
		}

		@Override
		public void mousePressed(MouseEvent e) {
			updateMouseOver(e.getPoint());
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			updateMouseOver(e.getPoint());
		}

		@Override
		public void mouseDragged(MouseEvent e) {}

		@Override
		public void mouseMoved(MouseEvent e) {
			updateMouseOver(e.getPoint());
		}
	}

	private TabbedPaneMouseListener mouseListener = new TabbedPaneMouseListener();
	private ChangeListener tabSelListener = new ChangeListener() {

		@Override
		public void stateChanged(ChangeEvent e) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					// the content border is not repainted for the SettingsDialog unless this is
					// done
					TabbedPaneUI.this.tabPane.repaint();
				}
			});
		}
	};

	private int rolloveredTabIndex = -1;

	public static ComponentUI createUI(JComponent c) {
		return new TabbedPaneUI();
	}

	@Override
	protected JButton createScrollButton(int direction) {
		if (direction != SOUTH && direction != NORTH && direction != EAST && direction != WEST) {
			throw new IllegalArgumentException("Direction must be one of: " + "SOUTH, NORTH, EAST or WEST");
		}
		return new GenericArrowButton(direction, 17, 17);
	}

	@Override
	protected void installListeners() {
		super.installListeners();
		this.tabPane.addMouseListener(this.mouseListener);
		this.tabPane.addMouseMotionListener(this.mouseListener);
		this.tabPane.addChangeListener(this.tabSelListener);
	}

	@Override
	protected void uninstallListeners() {
		super.uninstallListeners();
		this.tabPane.removeMouseListener(this.mouseListener);
		this.tabPane.removeMouseMotionListener(this.mouseListener);
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
	}

	@Override
	protected FontMetrics getFontMetrics() {
		Font font = tabPane.getFont();
		// selected tab is bold, to avoid resizing always assume bold and make tabs bigger
		font = font.deriveFont(Font.BOLD);
		return tabPane.getFontMetrics(font);
	}

	@Override
	protected MouseListener createMouseListener() {
		return new MouseHandler();
	}

	@Override
	protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
		if (isDockingFrameworkTab()) {
			return super.calculateTabWidth(tabPlacement, tabIndex, metrics) - 5;
		}

		return super.calculateTabWidth(tabPlacement, tabIndex, metrics) + 5;
	}

	@Override
	protected LayoutManager createLayoutManager() {
		if (tabPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT) {
			// can't override because private class..
			return super.createLayoutManager();
		} else {
			// override for docking framework spacing fix!
			return new BasicTabbedPaneUI.TabbedPaneLayout() {

				@Override
				protected void calculateTabRects(int tabPlacement, int tabCount) {
					final int spacer = -5;
					final int indent = 0;

					super.calculateTabRects(tabPlacement, tabCount);

					for (int i = 1; i < rects.length; i++) {
						// hack to get the tabs closer together. Don't shift leftmost tab(s)
						if (rects[i].x > 0) {
							rects[i].x += i * spacer + indent;
						}
					}
				}
			};
		}

	}

	@Override
	protected Insets getTabInsets(int tabPlacement, int tabIndex) {
		Insets t;
		switch (tabPlacement) {
			case SwingConstants.TOP:
				if (isDockingFrameworkTab()) {
					t = new Insets(6, 5, 6, -10);
				} else {
					t = new Insets(6, 8, 6, 8);
				}
				break;
			case SwingConstants.LEFT:
				t = new Insets(8, 8, 8, 8);
				break;
			case SwingConstants.RIGHT:
				t = new Insets(8, 8, 8, 8);
				break;
			case SwingConstants.BOTTOM:
				t = new Insets(8, 8, 8, 8);
				break;
			default:
				t = new Insets(8, 8, 8, 8);
				break;
		}
		return t;
	}

	@Override
	protected Insets getSelectedTabPadInsets(int tabPlacement) {
		Insets t;
		switch (tabPlacement) {
			case SwingConstants.TOP:
				if (isDockingFrameworkTab()) {
					t = new Insets(0, 5, 0, 0);
				} else {
					t = new Insets(0, 5, 0, 5);
				}
				break;
			case SwingConstants.LEFT:
				t = new Insets(1, 5, 0, 5);
				break;
			case SwingConstants.RIGHT:
				t = new Insets(1, 5, 0, 5);
				break;
			case SwingConstants.BOTTOM:
				t = new Insets(1, 5, 0, 5);
				break;
			default:
				t = new Insets(1, 5, 0, 5);
				break;
		}
		return t;
	}

	@Override
	protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h,
			boolean isSelected) {}

	@Override
	protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect,
			Rectangle textRect, boolean isSelected) {}

	@Override
	protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int xp, int yp, int mw, int mh,
			boolean isSelected) {

		if (isSelected) {
			paintTabBorderSelected(g, tabPlacement, tabIndex, xp, yp, mw, mh);
		} else {
			paintTabBorderFree(g, tabPlacement, tabIndex, xp, yp, mw, mh);
		}
	}

	@Override
	protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
		int width = this.tabPane.getWidth();
		int height = this.tabPane.getHeight();
		Insets insets = this.tabPane.getInsets();

		int x = insets.left;
		int y = insets.top;
		int w = width - insets.right - insets.left;
		int h = height - insets.top - insets.bottom;

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(Colors.TAB_CONTENT_BORDER);
		int r = RapidLookAndFeel.CORNER_TAB_RADIUS;
		Rectangle selTabBounds = new Rectangle();
		switch (tabPlacement) {
			case LEFT:
				x += calculateTabAreaWidth(tabPlacement, this.runCount, this.maxTabWidth);
				w -= x - insets.left;

				// the goal is to draw a round border all around the content except for where the
				// selected tab (if visible) is
				getTabBounds(selectedIndex, selTabBounds);

				// first we draw a round rect around everything
				g2.drawRoundRect(x, y, w - 1, h - 1, r, r);

				// now we remove the left line of the rect depending on whether the tab is in the
				// top
				// left corner, bottom left corner, or in the middle. Reason for that is that the
				// top
				// left or bottom left tab has no upper/lower left corner at all
				g2.setColor(Colors.TAB_BACKGROUND_SELECTED);
				if (selTabBounds.y < r) {
					g2.fillRect(x - 1, y + 1, 5, selTabBounds.height - 1);

					// now we draw the left border line again but not next to the selected tab
					g2.setColor(Colors.TAB_CONTENT_BORDER);
					g2.drawLine(x, y + selTabBounds.y + +selTabBounds.height, x, y + h - r);

					// there are missing border pixels that needs fixing
					g2.drawLine(x, y, x, y);
				} else if (h - (y + selTabBounds.y + selTabBounds.height) < r) {
					g2.fillRect(x, y + selTabBounds.y, 5, selTabBounds.height);

					// now we draw the top border line again but not below the selected tab
					g2.setColor(Colors.TAB_CONTENT_BORDER);
					g2.drawLine(x, y, x, selTabBounds.y - y);

					// there are missing border pixels that needs fixing
					g2.drawLine(x, y + selTabBounds.y + selTabBounds.height, x + 1, y + selTabBounds.y + selTabBounds.height);
				} else {
					g2.fillRect(x, y + selTabBounds.y, 5, selTabBounds.height);

					// now we draw the left border line again but not next to the selected tab
					g2.setColor(Colors.TAB_CONTENT_BORDER);
					g2.drawLine(x + r, y, x + selTabBounds.x, y);
					g2.drawLine(x + selTabBounds.x + selTabBounds.width - 1, y, x + w - r, y);

					// there are missing border pixels that needs fixing
					g2.drawLine(x, y + selTabBounds.y, x, y + selTabBounds.y);
				}

				break;
			case RIGHT:
				w -= calculateTabAreaWidth(tabPlacement, this.runCount, this.maxTabWidth);
				break;
			case BOTTOM:
				h -= calculateTabAreaHeight(tabPlacement, this.runCount, this.maxTabHeight);
				break;
			case TOP:
			default:
				y += calculateTabAreaHeight(tabPlacement, this.runCount, this.maxTabHeight);
				h -= y - insets.top;

				// the goal is to draw a round border all around the content except for where the
				// selected tab (if visible) is
				if (selectedIndex >= 0) {
					getTabBounds(selectedIndex, selTabBounds);
				}

				// first we draw a round rect around everything
				g2.drawRoundRect(x, y, w - 1, h - 1, r, r);

				// now we remove the top line of the rect depending on whether the tab is in the top
				// left corner, top right corner, or in the middle. Reason for that is that the top
				// left or top right tab has no upper left/right corner at all
				g2.setColor(Colors.TAB_BACKGROUND_SELECTED);
				if (selTabBounds.x < r) {
					g2.fillRect(x + 1, y - 1, selTabBounds.width - 1, 5);

					// now we draw the top border line again but not below the selected tab
					g2.setColor(Colors.TAB_CONTENT_BORDER);
					g2.drawLine(x + selTabBounds.x + selTabBounds.width - 1, y, x + w - r, y);

					// there are missing border pixels that needs fixing
					g2.drawLine(x, y, x, y);
					g2.drawLine(x + selTabBounds.x + selTabBounds.width - 1, y - 1, x + selTabBounds.x + selTabBounds.width
							- 1, y - 1);
				} else if (w - (x + selTabBounds.x + selTabBounds.width) < r) {
					g2.fillRect(x + selTabBounds.x + 1, y - 1, selTabBounds.width - 2, 5);

					// now we draw the top border line again but not below the selected tab
					g2.setColor(Colors.TAB_CONTENT_BORDER);
					g2.drawLine(x, y, selTabBounds.x - x, y);

					// there are missing border pixels that needs fixing
					g2.drawLine(x + w, y, x + w, y);
					g2.drawLine(x + w - selTabBounds.width, y - 1, x + w - selTabBounds.width, y - 1);
				} else {
					g2.fillRect(x + selTabBounds.x, y - 1, selTabBounds.width - 1, 5);

					// now we draw the top border line again but not below the selected tab
					g2.setColor(Colors.TAB_CONTENT_BORDER);
					g2.drawLine(x + r, y, x + selTabBounds.x, y);
					g2.drawLine(x + selTabBounds.x + selTabBounds.width - 1, y, x + w - r, y);

					// there are missing border pixels that needs fixing
					g2.drawLine(x + selTabBounds.x, y - 1, x + selTabBounds.x, y - 1);
					g2.drawLine(x + selTabBounds.x + selTabBounds.width - 1, y - 1, x + selTabBounds.x + selTabBounds.width
							- 1, y - 1);
				}
		}

		g2.dispose();
	}

	private void paintTabBorderSelected(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h) {
		if (tabPlacement == SwingConstants.TOP) {
			paintSelectedTop(g, x, y, w, h);
		} else if (tabPlacement == SwingConstants.LEFT) {
			paintSelectedLeft(g, x, y, w, h);
		} else if (tabPlacement == SwingConstants.RIGHT) {
			paintSelectedRight(g, x, y, w, h);
		} else {
			paintSelectedBottom(g, x, y, w, h);
		}
	}

	private static void paintSelectedRight(Graphics g, int x, int y, int w, int h) {
		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[2]);
		g.drawLine(x, y + 1, x + w - 11, y + 1);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[3]);
		g.drawLine(x, y, x + w - 15, y);

		ColorUIResource c1 = RapidLookTools.getColors().getTabbedPaneColors()[4];
		g.setColor(c1);

		g.drawLine(w + x - 10, y + 1, w + x - 10, y + 2);
		g.drawLine(w + x - 9, y + 2, w + x - 9, y + 2);
		g.drawLine(w + x - 8, y + 2, w + x - 8, y + 3);
		g.drawLine(w + x - 7, y + 3, w + x - 7, y + 4);
		g.drawLine(w + x - 6, y + 4, w + x - 6, y + 5);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[5]);

		g.drawLine(x, y + 2, x + w - 11, y + 2);
		g.drawLine(x, y + 3, x + w - 9, y + 3);
		g.drawLine(x, y + 4, x + w - 8, y + 4);
		g.drawLine(x, y + 5, x + w - 7, y + 5);

		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(new GradientPaint(1, y + 6, RapidLookTools.getColors().getTabbedPaneColors()[6], 1, y + h,
				RapidLookTools.getColors().getTabbedPaneColors()[7]));

		int[] xArr = new int[] { x + 4, w + x - 5, w + x - 5, x + 4 };
		int[] yArr = new int[] { y + 6, y + 6, y + h, y + h };
		Polygon p1 = new Polygon(xArr, yArr, 4);

		g2.fillPolygon(p1);

		g.setColor(c1);
		g.drawLine(w + x - 5, y + 6, x + w - 5, y + h - 1);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[1]);
		g.drawLine(x + w - 14, y, x + w - 12, y);
		g.drawLine(w + x - 6, y + 6, x + w - 6, y + 6);
	}

	private static void paintSelectedLeft(Graphics g, int x, int y, int w, int h) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		drawLeftTab(x + 2, y, w - 2, h, g2, Colors.TAB_BACKGROUND_SELECTED);

		g2.dispose();
	}

	private static void paintSelectedTop(Graphics g, int x, int y, int w, int h) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		drawTopTab(x, y, w, h, g2, Colors.TAB_BACKGROUND_SELECTED);

		g2.dispose();
	}

	private static void paintSelectedBottom(Graphics g, int x, int y, int w, int h) {
		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[1]);
		g.drawLine(x + 11, y + h - 1, x + w - 12, y + h - 1);
		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[2]);
		g.drawLine(x + 10, y + h - 2, x + w - 11, y + h - 2);

		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[3]);
		g.drawLine(x + 13, y + h - 1, x + w - 14, y + h - 1);

		ColorUIResource c1 = RapidLookTools.getColors().getTabbedPaneColors()[4];
		g.setColor(c1);

		// left
		g.drawLine(x + 9, y + h - 2, x + 9, y + h - 3);
		g.drawLine(x + 8, y + h - 3, x + 8, y + h - 3);
		g.drawLine(x + 7, y + h - 3, x + 7, y + h - 4);
		g.drawLine(x + 6, y + h - 4, x + 6, y + h - 5);
		g.drawLine(x + 5, y + h - 5, x + 5, y + h - 6);

		// right
		g.drawLine(w + x - 10, y + h - 2, w + x - 10, y + h - 3);
		g.drawLine(w + x - 9, y + h - 3, w + x - 9, y + h - 3);
		g.drawLine(w + x - 8, y + h - 3, w + x - 8, y + h - 4);
		g.drawLine(w + x - 7, y + h - 4, w + x - 7, y + h - 5);
		g.drawLine(w + x - 6, y + h - 5, w + x - 6, y + h - 6);

		// inner section
		g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[5]);

		g.drawLine(x + 10, y + h - 3, x + w - 11, y + h - 3);
		g.drawLine(x + 8, y + h - 4, x + w - 9, y + h - 4);
		g.drawLine(x + 7, y + h - 5, x + w - 8, y + h - 5);
		g.drawLine(x + 6, y + h - 6, x + w - 7, y + h - 6);

		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(new GradientPaint(1, y, RapidLookTools.getColors().getTabbedPaneColors()[7], 1, y + h - 6,
				RapidLookTools.getColors().getTabbedPaneColors()[6]));

		int[] xArr = new int[] { x + 4, w + x - 5, x + w - 1, x };
		int[] yArr = new int[] { y + h - 6, y + h - 6, y, y };
		Polygon p1 = new Polygon(xArr, yArr, 4);
		g2.fillPolygon(p1);

		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setColor(c1);
		g2.drawLine(x, y, x + 4, y + h - 6);
		g2.drawLine(w + x - 1, y, x + w - 5, y + h - 6);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
	}

	/**
	 * Draw a top tab at the given location and size with the given background color.
	 *
	 * @param x
	 *            x location of the tab
	 * @param y
	 *            y location of the tab
	 * @param w
	 *            width of the tab
	 * @param h
	 *            height of the tab
	 * @param g2
	 *            graphics context
	 * @param color
	 *            the background color of the tab
	 */
	private static void drawTopTab(int x, int y, int w, int h, Graphics2D g2, ColorUIResource color) {
		double rTop = RapidLookAndFeel.CORNER_TAB_RADIUS * 0.67;

		g2.setColor(color);
		g2.fill(createTopTabShape(x + 1, y + 1, w - 1, h, rTop, true));

		g2.setColor(Colors.TAB_BORDER);
		g2.draw(createTopTabShape(x, y, w - 1, h, rTop, false));
	}

	/**
	 * Draw a left tab at the given location and size with the given background color.
	 *
	 * @param x
	 *            x location of the tab
	 * @param y
	 *            y location of the tab
	 * @param w
	 *            width of the tab
	 * @param h
	 *            height of the tab
	 * @param g2
	 *            graphics context
	 * @param color
	 *            the background color of the tab
	 */
	private static void drawLeftTab(int x, int y, int w, int h, Graphics2D g2, ColorUIResource color) {
		double rTop = RapidLookAndFeel.CORNER_TAB_RADIUS * 0.67;

		g2.setColor(color);
		g2.fill(createLeftTabShape(x + 1, y + 1, w - 1, h, rTop, true));

		g2.setColor(Colors.TAB_BORDER);
		g2.draw(createLeftTabShape(x, y, w - 1, h, rTop, false));
	}

	/**
	 * Creates the shape for a top tab.
	 *
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @param rTop
	 * @param addBottom
	 *            if {@code false}, the bottom line below the tab will not be added to the shape
	 * @return
	 */
	private static Path2D createTopTabShape(int x, int y, int w, int h, double rTop, boolean addBottom) {
		Path2D path = new Path2D.Double();
		path.append(new Line2D.Double(x, y + h - 1, x, y + rTop), true);

		QuadCurve2D curve = new QuadCurve2D.Double(x, y + rTop, x, y, x + rTop, y);
		path.append(curve, true);

		path.append(new Line2D.Double(x + rTop, y, x + w - rTop, y), true);

		curve = new QuadCurve2D.Double(x + w - rTop, y, x + w, y, x + w, y + rTop);
		path.append(curve, true);

		path.append(new Line2D.Double(x + w, y + rTop, x + w, y + h), true);

		if (addBottom) {
			path.append(new Line2D.Double(x + w, y + h - 1, x, y + h - 1), true);
		}
		return path;
	}

	/**
	 * Creates the shape for a left tab.
	 *
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @param rLeft
	 * @param addSide
	 *            if {@code false}, the closing side line right of the tab will not be added to the
	 *            shape
	 * @return
	 */
	private static Path2D createLeftTabShape(int x, int y, int w, int h, double rLeft, boolean addSide) {
		Path2D path = new Path2D.Double();
		path.append(new Line2D.Double(x + w, y + h, x + rLeft, y + h), true);

		QuadCurve2D curve = new QuadCurve2D.Double(x + rLeft, y + h, x, y + h, x, y + h - rLeft);
		path.append(curve, true);

		path.append(new Line2D.Double(x, y + h - rLeft, x, y + rLeft), true);

		curve = new QuadCurve2D.Double(x, y + rLeft, x, y, x + rLeft, y);
		path.append(curve, true);

		path.append(new Line2D.Double(x + rLeft, y, x + w, y), true);

		if (addSide) {
			path.append(new Line2D.Double(x + w, y, x + w, y + h - 1), true);
		}
		return path;
	}

	private void paintTabBorderFree(Graphics g, int tabPlacement, int tabIndex, int xp, int yp, int mw, int h) {
		int x = xp + 2;
		int y = yp;
		int w = mw - 4;

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (tabPlacement == SwingConstants.BOTTOM) {
			g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[8]);
			g.drawLine(x + 4, y + h - 1, x + w - 5, y + h - 1);
			g.drawLine(x, y, x, y + h - 5);
			g.drawLine(x + w - 1, y, x + w - 1, y + h - 4);

			ColorUIResource c1 = RapidLookTools.getColors().getTabbedPaneColors()[9];
			ColorUIResource c2 = RapidLookTools.getColors().getTabbedPaneColors()[20];
			ColorUIResource c3 = RapidLookTools.getColors().getTabbedPaneColors()[10];

			// left
			g.setColor(c3);
			g.drawLine(x + 2, y + h - 1, x, y + h - 3);
			g.setColor(c1);
			g.drawLine(x, y + h - 4, x + 3, y + h - 1);
			g.drawLine(x + 1, y + h - 2, x + 1, y + h - 2);
			g.setColor(c2);
			g.drawLine(x + 3, y + h - 2, x + 1, y + h - 4);

			// right
			g.setColor(c3);
			g.drawLine(x + w - 1, y + h - 3, x + w - 3, y + h - 1);
			g.setColor(c1);
			g.drawLine(x + w - 4, y + h - 1, x + w - 1, y + h - 4);
			g.drawLine(x + w - 2, y + h - 2, x + w - 2, y + h - 2);
			g.setColor(c2);
			g.drawLine(x + w - 4, y + h - 2, x + w - 2, y + h - 4);

			if (tabIndex != this.rolloveredTabIndex) {
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[11]);
				g.drawLine(x + 1, y, x + w - 2, y);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[12]);
				g.drawLine(x + 1, y + 1, x + w - 2, y + 1);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[13]);
				g.drawLine(x + 1, y + 2, x + w - 2, y + 2);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[14]);
				g.drawLine(x + 1, y + 3, x + w - 2, y + 3);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[15]);
				g.drawLine(x + 1, y + 4, x + w - 2, y + 4);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x + 1, y + 5, x + w - 2, y + 5);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x + 1, y + 6, x + w - 2, y + 6);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.fillRect(x + 1, y + 7, w - 2, h - 11);
				g.drawLine(x + 2, y + h - 4, x + w - 3, y + h - 4);
				g.drawLine(x + 3, y + h - 3, x + w - 4, y + h - 3);
				g.drawLine(x + 4, y + h - 2, x + w - 5, y + h - 2);
			} else {
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.drawLine(x + 1, y, x + w - 2, y);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x + 1, y + 1, x + w - 2, y + 1);
				g.drawLine(x + 1, y + 2, x + w - 2, y + 2);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x + 1, y + 3, x + w - 2, y + 3);
				g.drawLine(x + 1, y + 4, x + w - 2, y + 4);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[15]);
				g.drawLine(x + 1, y + 5, x + w - 2, y + 5);
				g.drawLine(x + 1, y + 6, x + w - 2, y + 6);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[14]);
				g.fillRect(x + 1, y + 7, w - 2, h - 11);
				g.drawLine(x + 2, y + h - 4, x + w - 3, y + h - 4);
				g.drawLine(x + 3, y + h - 3, x + w - 4, y + h - 3);
				g.drawLine(x + 4, y + h - 2, x + w - 5, y + h - 2);
			}
		} else if (tabPlacement == SwingConstants.RIGHT) {
			x -= 2;
			g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[8]);
			g.drawLine(x, y, x + w - 5, y);
			g.drawLine(x + w - 1, y + 4, x + w - 1, y + h - 1);

			ColorUIResource c1 = RapidLookTools.getColors().getTabbedPaneColors()[9];
			ColorUIResource c2 = RapidLookTools.getColors().getTabbedPaneColors()[19];
			ColorUIResource c3 = RapidLookTools.getColors().getTabbedPaneColors()[10];

			// right
			g.setColor(c3);
			g.drawLine(x + w - 1, y + 2, x + w - 3, y);
			g.setColor(c1);
			g.drawLine(x + w - 4, y, x + w - 1, y + 3);
			g.drawLine(x + w - 2, y + 1, x + w - 2, y + 1);
			g.setColor(c2);
			g.drawLine(x + w - 4, y + 1, x + w - 2, y + 3);

			if (tabIndex != this.rolloveredTabIndex) {
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[11]);
				g.drawLine(x, y + 1, x + w - 5, y + 1);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[12]);
				g.drawLine(x, y + 2, x + w - 4, y + 2);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[13]);
				g.drawLine(x, y + 3, x + w - 3, y + 3);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[14]);
				g.drawLine(x, y + 4, x + w - 2, y + 4);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[15]);
				g.drawLine(x, y + 5, x + w - 2, y + 5);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x, y + 6, x + w - 2, y + 6);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x, y + 7, x + w - 2, y + 7);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.fillRect(x, y + 8, w - 1, h - 8);
			} else {
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[14]);
				g.drawLine(x, y + 1, x + w - 5, y + 1);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.drawLine(x, y + 2, x + w - 4, y + 2);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[18]);
				g.drawLine(x, y + 3, x + w - 3, y + 3);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x, y + 4, x + w - 2, y + 4);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[17]);
				g.drawLine(x, y + 5, x + w - 2, y + 5);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x, y + 6, x + w - 2, y + 6);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[16]);
				g.drawLine(x, y + 7, x + w - 2, y + 7);
				g.setColor(RapidLookTools.getColors().getTabbedPaneColors()[15]);
				g.fillRect(x, y + 8, w - 1, h - 8);
			}
		} else if (tabPlacement == SwingConstants.LEFT) {
			w += 4;
			if (tabIndex == rolloveredTabIndex) {
				// highlight on hover
				drawLeftTab(x, y, w, h, g2, Colors.TAB_BACKGROUND_HIGHLIGHT);
			} else {
				drawLeftTab(x, y, w, h, g2, Colors.TAB_BACKGROUND);
			}
		} else { // top
			if (tabIndex == rolloveredTabIndex) {
				// highlight on hover
				drawTopTab(x, y, w, h, g2, Colors.TAB_BACKGROUND_HIGHLIGHT);
			} else {
				drawTopTab(x, y, w, h, g2, Colors.TAB_BACKGROUND);
			}
		}
		g2.dispose();
	}

	@Override
	protected int getTabLabelShiftX(int tabPlacement, int tabIndex, boolean isSelected) {
		if (tabPane.getTabLayoutPolicy() != JTabbedPane.SCROLL_TAB_LAYOUT) {
			if (isSelected) {
				return -5;
			}
		}
		return 0;
	}

	@Override
	protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
		return 0;
	}

	protected void updateMouseOver(Point p) {
		int roi = tabForCoordinate(this.tabPane, (int) p.getX(), (int) p.getY());
		if (this.rolloveredTabIndex != roi) {
			this.rolloveredTabIndex = roi;
			this.tabPane.repaint();
		}
	}

	@Override
	public Insets getContentBorderInsets(int tabPlacement) {
		return new Insets(1, 1, 2, 1);
	}

	@Override
	protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title,
			Rectangle textRect, boolean isSelected) {
		// otherwise the tabs text would not have AA for some reason even though the rest of the
		// components has AA without this
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		if (isSelected) {
			font = font.deriveFont(Font.BOLD);
		}
		super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
	}

	/**
	 * Docking framework tabs use some weird dimensions so they get special treatment here
	 *
	 * @return {@code true} if it is a docking framework tab which should be reduced in size;
	 *         {@code false} otherwise
	 */
	private boolean isDockingFrameworkTab() {
		if (tabPane instanceof DockTabbedPane) {
			return true;
		}
		if (tabPane instanceof TabHeader) {
			TabHeader h = (TabHeader) tabPane;
			if (h.getDockable() != null) {
				return h.getDockable().getDockKey().isCloseEnabled();
			}
		}

		return false;
	}
}
