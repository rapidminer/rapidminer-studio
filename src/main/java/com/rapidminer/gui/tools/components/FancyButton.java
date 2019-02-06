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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.border.DropShadowBorder;


/**
 * This is a {@link JButton} which looks fancier. The background is transparent and only when
 * hovering over the button it is highlighted (floating above the background).
 *
 * @author Marco Boeck
 *
 */
public class FancyButton extends JButton implements FancyConstants {

	private static final int HEIGHT_EXTENSION = 20;

	private static final long serialVersionUID = 6492754313852421831L;

	private int preferredHeightExtension = HEIGHT_EXTENSION;

	/** flag indicating if the button is hovered */
	private boolean hovered;

	/** flag indicating whether to draw trailing arrow */
	private boolean drawArrow;

	/** the color used to display the text when not hovered */
	private Color normalTextColor = NORMAL_TEXTCOLOR;

	/** the color used to display the text when hovered */
	private Color hoveredTextColor = HOVERED_TEXTCOLOR;

	private Color customBackgroundColor;

	private Color customBorderColor;

	private MouseListener hoverListener = new MouseAdapter() {

		@Override
		public void mouseExited(MouseEvent e) {
			hovered = false;
			setBorderPainted(false);
			setForeground(normalTextColor);
			repaint();
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			hovered = true;
			setBorderPainted(true);
			setForeground(hoveredTextColor);
			repaint();
		}
	};

	private FocusListener focusListener = new FocusAdapter() {

		@Override
		public void focusLost(FocusEvent e) {
			hovered = false;
			setBorderPainted(false);
			setForeground(normalTextColor);
			repaint();
		}
	};

	/**
	 * Creates a button with no set text or icon.
	 */
	public FancyButton() {
		this(null, null);
	}

	/**
	 * Creates a button with an icon.
	 *
	 * @param icon
	 *            the Icon image to display on the button
	 */
	public FancyButton(Icon icon) {
		this(null, icon);
	}

	/**
	 * Creates a button with text.
	 *
	 * @param text
	 *            the text of the button
	 */
	public FancyButton(String text) {
		this(text, null);
	}

	/**
	 * Creates a button with initial text and an icon.
	 *
	 * @param text
	 *            the text of the button
	 * @param icon
	 *            the Icon image to display on the button
	 */
	public FancyButton(String text, Icon icon) {
		super(text, icon);

		// setup fancy button things
		addMouseListener(hoverListener);
		addFocusListener(focusListener);

		setBorderPainted(false);
		setBorder(new DropShadowBorder(SHADOW_COLOR, 5, 0.5f, 12, false, false, true, true));
		setFocusPainted(false);
		setContentAreaFilled(false);
		setHorizontalAlignment(SwingConstants.LEFT);
		setHorizontalTextPosition(SwingConstants.RIGHT);

		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		setForeground(normalTextColor);
		drawArrow = true;
	}

	/**
	 * Creates a button where properties are taken from the <code>Action</code> supplied.
	 *
	 * @param a
	 *            the <code>Action</code> used to specify the new button
	 *
	 */
	public FancyButton(Action a) {
		this();
		super.setAction(a);
	}

	/**
	 * If set to <code>true</code>, the trailing arrow will not be drawn.
	 *
	 * @param flag
	 */
	public void setDrawArrow(boolean flag) {
		drawArrow = flag;
	}

	/**
	 * Sets the text color when the {@link FancyButton} is not hovered over.
	 *
	 * @param textColor
	 */
	public void setTextColor(Color textColor) {
		this.normalTextColor = textColor;
		setForeground(textColor);
	}

	/**
	 * Sets the text color when the {@link FancyButton} is hovered over.
	 *
	 * @param textColor
	 */
	public void setHoveredTextColor(Color textColor) {
		this.hoveredTextColor = textColor;
	}

	/**
	 * Sets how much the height of the button should be enhanced. Default is
	 * {@link FancyButton#HEIGHT_EXTENSION}
	 *
	 * @param heightExtension
	 */
	public void setButtonHeightEnhancement(int heightExtension) {
		this.preferredHeightExtension = heightExtension;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension prefSize = super.getPreferredSize();
		prefSize.setSize(prefSize.getWidth() + (drawArrow ? 75 : -5), prefSize.getHeight() + preferredHeightExtension);
		return prefSize;
	}

	@Override
	public boolean isRolloverEnabled() {
		// force it to disable rollover because we have custom hover highlighting
		return false;
	}

	public void setBackgroundColor(Color backgroundColor) {
		customBackgroundColor = backgroundColor;
	};

	public void setBorderColor(Color borderColor) {
		customBorderColor = borderColor;
	}

	@Override
	public void paintComponent(Graphics g) {
		if (getModel().isArmed()) {
			((Graphics2D) g).translate(1.1, 1.1);
		}

		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Graphics2D g2 = (Graphics2D) g.create();

		// the default background is transparent
		if (hovered || customBackgroundColor != null) {
			// fill background
			g2.setColor(customBackgroundColor != null ? customBackgroundColor : BACKGROUND_COLOR);
			g2.fillRect(0, 0, getWidth() - 6, getHeight() - 6);
		}

		if (hovered) {
			// draw border which complements the DropShadow
			g2.setColor(customBorderColor != null ? customBorderColor : BORDER_LIGHTEST_GRAY);
			g2.drawRect(0, 0, getWidth() - 6, getHeight() - 6);
		} else if (customBorderColor != null) {
			g2.setColor(customBorderColor);
			g2.drawRect(0, 0, getWidth() - 6, getHeight() - 6);
		}

		if (hovered) {
			if (drawArrow) {
				// draw arrow
				GeneralPath path = new GeneralPath();
				path.moveTo(getWidth() * 0.82, getHeight() * 0.3);
				path.lineTo(getWidth() * 0.92, getHeight() * 0.5);
				path.lineTo(getWidth() * 0.82, getHeight() * 0.7);
				Stroke arrowStroke = new BasicStroke(5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
				g2.setStroke(arrowStroke);
				g2.setColor(HOVERED_TEXTCOLOR);
				g2.draw(path);
			}

			// cut off arrow ends so the cut looks like an imaginary vertical line
			g2.setColor(customBackgroundColor != null ? customBackgroundColor : BACKGROUND_COLOR);
			g2.fillRect((int) (getWidth() * 0.81), (int) (getHeight() * 0.29), 7, (int) (getHeight() * 0.5));
		}
		g2.dispose();

		super.paintComponent(g);
	}

}
