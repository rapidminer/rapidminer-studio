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

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.JTableHeader;

import com.rapidminer.gui.look.painters.CachedPainter;
import com.vlsolutions.swing.toolbars.VLToolBar;


/**
 * Some tool methods in regards to the Look and Feel of the UI.
 *
 * @author Ingo Mierswa, Marco Boeck
 */
public final class RapidLookTools {

	private static boolean vlDockingAvailable = true;

	/**
	 * The client property of a {@link JProgressBar} if the text should not appear below it but
	 * instead the progress bar should have full height and the text should appear in front of it.
	 * Expected type: {@link Boolean}
	 *
	 * @since 7.0.0
	 */
	public static final String PROPERTY_PROGRESSBAR_COMPRESSED = "progressbar_compressed";

	/**
	 * The client property of a {@link JTableHeader} if the background should not be the default
	 * background color. Expected type: {@link ColorUIResource}
	 *
	 * @since 7.0.0
	 */
	public static final String PROPERTY_TABLE_HEADER_BACKGROUND = "table_header_bg";

	/**
	 * The client property of a few {@link JComponent}s (currently supported by: {@link JComboBox})
	 * which take input and should not be displayed in a bright theme but rather in a dark theme.
	 * Expected type: {@link Boolean}
	 *
	 * @since 7.0.0
	 */
	public static final String PROPERTY_INPUT_BACKGROUND_DARK = "input_dark_bg";

	/**
	 * If buttons should be highlighted in orange or not. Set to {@code true} or {@code false}. Default is {@code false}.
	 *
	 * @since 8.1
	 */
	public static final String PROPERTY_BUTTON_HIGHLIGHT = "button_highlight";

	/**
	 * If buttons should be highlighted in dark gray or not. Set to {@code true} or {@code false}. Default is {@code false}.
	 *
	 * @since 8.1.2
	 */
	public static final String PROPERTY_BUTTON_HIGHLIGHT_DARK = "button_highlight_dark";

	/**
	 * If buttons should be highlighted in white or not. Set to {@code true} or {@code false}. Default is {@code false}.
	 *
	 * @since 9.2.0
	 */
	public static final String PROPERTY_BUTTON_HIGHLIGHT_WHITE = "button_highlight_white";

	/**
	 * If buttons should have a darker border. Set to {@code true} or {@code false}. Default is {@code false}.
	 *
	 * @since 8.1
	 */
	public static final String PROPERTY_BUTTON_DARK_BORDER = "button_dark_border";

	/**
	 * If buttons should be circular. Set to {@code true} or {@code false}. Default is {@code false}.
	 *
	 * @since 8.1.2
	 */
	public static final String PROPERTY_BUTTON_CIRCLE = "button_circular";

	/**
	 * If input fields should have a darker border. Set to {@code true} or {@code false}. Default is {@code false}.
	 *
	 * @since 8.1
	 */
	public static final String PROPERTY_INPUT_DARK_BORDER = "input_dark_border";

	/**
	 * If input fields should be part of a composite input/button. Normally, each component has rounded borders all around so they look good as a standalone field.
	 * If you want to have inputs and buttons right next to each other with no padding, this would look ugly.
	 * Setting this to either {@link javax.swing.SwingConstants#LEFT}, {@link javax.swing.SwingConstants#CENTER} or {@link javax.swing.SwingConstants#RIGHT}
	 * will produce a composite input border which is not rounded on the right, both sides, or the left respectively. Default is {@code -1}, aka standalone with round borders all around.
	 *
	 * @since 8.1
	 */
	public static final String PROPERTY_INPUT_TYPE_COMPOSITE = "input_type_composite";

	/**
	 * If a tabbed pane should lay out its tabs at the top (horizontally) using the full available width - and not more.
	 * So 2 tabs would take ~50% each, 3 tabs ~33% each, etc.
	 *
	 * @since 9.4.0
	 */
	public static final String PROPERTY_TABBED_PANE_FULL_WIDTH = "tabbed_pane_full_width";

	static {
		try {
			VLToolBar.class.getName();
		} catch (NoClassDefFoundError e) {
			vlDockingAvailable = false;
		}
	}

	public static void clearMenuCache() {
		CachedPainter.clearMenuCache();
	}

	public static void drawMenuItemBackground(Graphics g, JMenuItem menuItem) {
		Color oldColor = g.getColor();
		ButtonModel model = menuItem.getModel();
		int w = menuItem.getWidth();
		int h = menuItem.getHeight();

		if (model.isArmed() || model.isSelected() && menuItem instanceof JMenu) {
			g.setColor(Colors.MENU_ITEM_BACKGROUND_SELECTED);
			g.fillRect(0, 0, w, h);
		} else if (!(menuItem instanceof JMenu && ((JMenu) menuItem).isTopLevelMenu())) {
			drawMenuItemFading(menuItem, g);
		}
		g.setColor(oldColor);
	}

	public static void drawMenuItemFading(Component c, Graphics g) {
		int w = c.getWidth();
		int h = c.getHeight();
		if (h < 0 || w < 0) {
			return;
		}

		g.setColor(Colors.MENU_ITEM_BACKGROUND);
		g.fillRect(0, 0, w, h);
	}

	public static boolean isToolbarButton(JComponent b) {
		return b.getParent() instanceof JToolBar || vlDockingAvailable && isVLToolbarButton(b);
	}

	public static boolean isVLToolbarButton(JComponent b) {
		return b.getParent() instanceof VLToolBar;
	}

	/**
	 * Drasw a button in a toolbar.
	 *
	 * @param b
	 *            the button
	 * @param g
	 *            the graphics instance
	 */
	public static void drawToolbarButton(Graphics g, AbstractButton b) {
		if (!b.isEnabled()) {
			return;
		}

		if (b.getModel().isSelected() && b.isRolloverEnabled() || b.getModel().isPressed() && b.getModel().isArmed()
				&& b.isRolloverEnabled()) {
			if (b.isContentAreaFilled()) {
				drawButton(b, g, createShapeForButton(b));
			}
			if (b.isBorderPainted()) {
				drawButtonBorder(b, g, createBorderShapeForButton(b));
			}
		} else if (b.getModel().isRollover() && b.isRolloverEnabled()) {
			if (b.isBorderPainted()) {
				drawButtonBorder(b, g, createBorderShapeForButton(b));
			}
		}
	}

	public static boolean isLeftToRight(Component c) {
		return c.getComponentOrientation().isLeftToRight();
	}

	public static Colors getColors() {
		return RapidLookAndFeel.getColors();
	}

	/**
	 * Creates the default {@link Shape} for the given button.
	 *
	 * @param b
	 *            the button to create the shape for
	 * @return the shape instance
	 */
	public static Shape createShapeForButton(AbstractButton b) {
		int w = b.getWidth();
		int h = b.getHeight();
		boolean circular = Boolean.parseBoolean(String.valueOf(b.getClientProperty(RapidLookTools.PROPERTY_BUTTON_CIRCLE)));

		if (circular) {
			return createCircle(w, h);
		} else {
			return new RoundRectangle2D.Double(1, 1, w - 2, h - 2, RapidLookAndFeel.CORNER_DEFAULT_RADIUS,
					RapidLookAndFeel.CORNER_DEFAULT_RADIUS);
		}
	}

	/**
	 * Creates the border {@link Shape} for the given button.
	 *
	 * @param b
	 *            the button to create the border shape for
	 * @return the border shape instance
	 */
	public static Shape createBorderShapeForButton(AbstractButton b) {
		int w = b.getWidth();
		int h = b.getHeight();
		boolean circular = Boolean.parseBoolean(String.valueOf(b.getClientProperty(RapidLookTools.PROPERTY_BUTTON_CIRCLE)));

		if (circular) {
			return createCircle(w, h);
		} else {
			return new RoundRectangle2D.Double(0, 0, w - 1, h - 1, RapidLookAndFeel.CORNER_DEFAULT_RADIUS,
					RapidLookAndFeel.CORNER_DEFAULT_RADIUS);
		}
	}

	/**
	 * Draws the given button border with the specified shape.
	 *
	 * @param b
	 * @param g
	 * @param shape
	 */
	public static void drawButtonBorder(AbstractButton b, Graphics g, Shape shape) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		boolean darkBorder = Boolean.parseBoolean(String.valueOf(b.getClientProperty(RapidLookTools.PROPERTY_BUTTON_DARK_BORDER)));
		if (darkBorder) {
			if (b.isEnabled()) {
				if (b.hasFocus()) {
					g2.setColor(Colors.BUTTON_BORDER_DARK_FOCUS);
				} else {
					g2.setColor(Colors.BUTTON_BORDER_DARK);
				}
			} else {
				g2.setColor(Colors.BUTTON_BORDER_DARK_DISABLED);
			}
		} else {
			if (b.isEnabled()) {
				if (b.hasFocus()) {
					g2.setColor(Colors.BUTTON_BORDER_FOCUS);
				} else {
					g2.setColor(Colors.BUTTON_BORDER);
				}
			} else {
				g2.setColor(Colors.BUTTON_BORDER_DISABLED);
			}
		}


		g2.draw(shape);
	}

	/**
	 * Draws the given button with the specified shape.
	 *
	 * @param b
	 * @param g
	 * @param shape
	 */
	public static void drawButton(AbstractButton b, Graphics g, Shape shape) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int h = (int) shape.getBounds().getHeight();
		ColorUIResource colorGradientStart;
		ColorUIResource colorGradientEnd;
		boolean highlighted = Boolean.parseBoolean(String.valueOf(b.getClientProperty(PROPERTY_BUTTON_HIGHLIGHT)));
		boolean highlightedDark = Boolean.parseBoolean(String.valueOf(b.getClientProperty(PROPERTY_BUTTON_HIGHLIGHT_DARK)));
		boolean highlightedWhite = Boolean.parseBoolean(String.valueOf(b.getClientProperty(PROPERTY_BUTTON_HIGHLIGHT_WHITE)));
		if (highlighted) {
			if (b.isEnabled()) {
				if (b.getModel().isPressed() || b.getModel().isSelected()) {
					colorGradientStart = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_PRESSED_GRADIENT_START;
					colorGradientEnd = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_PRESSED_GRADIENT_END;
				} else if (b.getModel().isRollover()) {
					colorGradientStart = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_ROLLOVER_GRADIENT_START;
					colorGradientEnd = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_ROLLOVER_GRADIENT_END;
				} else {
					colorGradientStart = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_GRADIENT_START;
					colorGradientEnd = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_GRADIENT_END;
				}
			} else {
				colorGradientStart = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_DISABLED_GRADIENT_START;
				colorGradientEnd = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_DISABLED_GRADIENT_END;
			}
		} else if (highlightedDark) {
			if (b.isEnabled()) {
				if (b.getModel().isPressed() || b.getModel().isSelected()) {
					colorGradientStart = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_DARK_PRESSED_GRADIENT_START;
					colorGradientEnd = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_DARK_PRESSED_GRADIENT_END;
				} else if (b.getModel().isRollover()) {
					colorGradientStart = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_DARK_ROLLOVER_GRADIENT_START;
					colorGradientEnd = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_DARK_ROLLOVER_GRADIENT_END;
				} else {
					colorGradientStart = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_DARK_GRADIENT_START;
					colorGradientEnd = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_DARK_GRADIENT_END;
				}
			} else {
				colorGradientStart = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_DARK_DISABLED_GRADIENT_START;
				colorGradientEnd = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_DARK_DISABLED_GRADIENT_END;
			}
		} else if (highlightedWhite) {
			if (b.isEnabled()) {
				if (b.getModel().isPressed() || b.getModel().isSelected()) {
					colorGradientStart = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_PRESSED_GRADIENT_START;
					colorGradientEnd = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_PRESSED_GRADIENT_END;
				} else if (b.getModel().isRollover()) {
					colorGradientStart = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_ROLLOVER_GRADIENT_START;
					colorGradientEnd = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_ROLLOVER_GRADIENT_END;
				} else {
					colorGradientStart = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_GRADIENT_START;
					colorGradientEnd = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_GRADIENT_END;
				}
			} else {
				colorGradientStart = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_DISABLED_GRADIENT_START;
				colorGradientEnd = Colors.BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_DISABLED_GRADIENT_END;
			}
		} else {
			if (b.isEnabled()) {
				if (b.getModel().isPressed() || b.getModel().isSelected()) {
					colorGradientStart = Colors.BUTTON_BACKGROUND_PRESSED_GRADIENT_START;
					colorGradientEnd = Colors.BUTTON_BACKGROUND_PRESSED_GRADIENT_END;
				} else if (b.getModel().isRollover()) {
					colorGradientStart = Colors.BUTTON_BACKGROUND_ROLLOVER_GRADIENT_START;
					colorGradientEnd = Colors.BUTTON_BACKGROUND_ROLLOVER_GRADIENT_END;
				} else {
					colorGradientStart = Colors.BUTTON_BACKGROUND_GRADIENT_START;
					colorGradientEnd = Colors.BUTTON_BACKGROUND_GRADIENT_END;
				}
			} else {
				colorGradientStart = Colors.BUTTON_BACKGROUND_DISABLED_GRADIENT_START;
				colorGradientEnd = Colors.BUTTON_BACKGROUND_DISABLED_GRADIENT_END;
			}
		}

		Paint gp = new GradientPaint(0, 0, colorGradientStart, 0, h, colorGradientEnd);
		g2.setPaint(gp);

		g2.fill(shape);
	}

	/**
	 * Creates a circle of a rectangle with the given width and height. Must have at least 16px of width and height, otherwise results will be broken.
	 *
	 * @param width
	 * 		the width
	 * @param height
	 * 		the height
	 * @return the ellipsis, never {@code null}
	 */
	private static Ellipse2D.Double createCircle(int width, int height) {
		int centerX = (1 + width) / 2 + 1;
		int centerY = (1 + height - 1) / 2;
		int cornerX = centerX + width / 2 - 6;
		int cornerY = centerY + width / 2 - 6;
		Ellipse2D.Double circle = new Ellipse2D.Double();
		circle.setFrameFromCenter(centerX, centerY, cornerX, cornerY);
		return circle;
	}
}
