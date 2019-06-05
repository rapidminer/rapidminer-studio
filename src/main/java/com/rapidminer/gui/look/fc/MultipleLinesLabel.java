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
package com.rapidminer.gui.look.fc;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.UIManager;


/**
 * A new swing component similar to a label but containing multiple lines.
 *
 * @author Ingo Mierswa
 */
public class MultipleLinesLabel extends JComponent implements SwingConstants {

	private static final long serialVersionUID = -4783596538296904867L;

	private int lineHeight, lineAscent;

	private int maxWidth = -1;

	private int textHeight = -1;

	private int[] lineWidths;

	private int btnMarginWidth = 1;

	private String text = "";

	private boolean multiLine = true;

	private static Font labelFont = new java.awt.Font("SansSerif", 0, 11);

	private int horizontalAlignment = LEADING;

	private int textAlignment = LEADING;

	private int verticalAlignment = CENTER;

	private FontMetrics fontMetrics;

	private Vector<String> vector = new Vector<>();

	private boolean needUpdate = true;

	public MultipleLinesLabel() {
		this("", CENTER, CENTER, TOP);
	}

	public MultipleLinesLabel(String text) {
		this(text, LEADING, LEADING, TOP);
	}

	public MultipleLinesLabel(String text, int horizontalAlignment) {
		this(text, horizontalAlignment, LEADING, CENTER);
	}

	public MultipleLinesLabel(String text, int horizontalAlignment, int textAlignment) {
		this(text, horizontalAlignment, textAlignment, CENTER);
	}

	public MultipleLinesLabel(String str, int horizontalAlignment, int textAlignment, int verticalAlignment) {
		this.setForeground(UIManager.getColor("Label.foreground"));
		this.setBackground(UIManager.getColor("textHighlight"));
		this.setFont(labelFont);

		setText(str);
		this.horizontalAlignment = horizontalAlignment;
		this.textAlignment = textAlignment;
		this.verticalAlignment = verticalAlignment;

		this.needUpdate = true;
	}

	private void updateTextVector() {
		this.vector = new Vector<String>();

		int w = this.getWidth();
		if (w > 6) {
			w -= 4;
		}

		char[] ca = this.text.toCharArray();
		String tempStr = "";
		Vector<String> sentVec = new Vector<String>();
		char lastChar;

		lastChar = ca[0];
		tempStr += lastChar;

		for (int i = 1; i < ca.length; i++) {
			if (lastChar == ' ') {
				if (ca[i] == ' ') {
					tempStr += ca[i];
				} else {
					sentVec.add(tempStr);
					tempStr = String.valueOf(ca[i]);
				}
			} else {
				if (ca[i] == ' ') {
					sentVec.add(tempStr);
					tempStr = String.valueOf(ca[i]);
				} else {
					tempStr += ca[i];
				}
			}
			lastChar = ca[i];
		}
		if (!tempStr.equals("")) {
			sentVec.add(tempStr);
		}

		StringBuffer lineStr = new StringBuffer();
		for (String str : sentVec) {
			if (this.fontMetrics.stringWidth(str) <= w) {
				if (this.fontMetrics.stringWidth(lineStr + str) <= w) {
					lineStr.append(str);
				} else {
					if (lineStr.length() > 0) {
						this.vector.add(lineStr.toString());
						lineStr = new StringBuffer();
					}
					lineStr = new StringBuffer(str);
				}
			} else {
				if (lineStr.length() > 0) {
					this.vector.add(lineStr.toString());
					lineStr = new StringBuffer();
				}

				ca = str.toCharArray();
				int first = 0;
				for (int i = 0; i < ca.length; i++) {
					if (this.fontMetrics.stringWidth(str.substring(first, i)) > w) {
						this.vector.add(str.substring(first, i - 1));
						first = i - 1;
					}
				}
				lineStr = new StringBuffer(str.substring(first, str.length()));
			}
		}

		if (lineStr.length() > 0) {
			this.vector.add(lineStr.toString());
		}
	}

	private void recalculateDimension() {
		this.fontMetrics = getFontMetrics(getFont());

		this.lineHeight = this.fontMetrics.getHeight();
		this.lineAscent = this.fontMetrics.getAscent();

		this.lineWidths = new int[this.vector.size()];

		this.maxWidth = 0;
		for (int i = 0; i < this.vector.size(); i++) {
			this.lineWidths[i] = this.fontMetrics.stringWidth(this.vector.elementAt(i));
			this.maxWidth = Math.max(this.maxWidth, this.lineWidths[i]);
		}

		this.maxWidth += 2 * this.btnMarginWidth;
		this.textHeight = this.vector.size() * this.lineHeight;

		revalidate();
	}

	@Override
	public Dimension getMaximumSize() {
		return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(10, 20);
	}

	public Dimension getMinimumSize1() {
		if (this.maxWidth == -1 || this.textHeight == -1) {
			recalculateDimension();
		}
		Insets insets = getInsets();

		return new Dimension(this.maxWidth + insets.left + insets.right, this.textHeight + insets.top + insets.bottom);
	}

	private void paintOneLine(Graphics g, Dimension d, Insets insets, int y) {
		int ha = getBidiHorizontalAlignment(this.horizontalAlignment);
		int ww = this.fontMetrics.stringWidth(this.text);

		int x = 0;

		if (ha == LEFT) {
			ha = getBidiHorizontalAlignment(this.textAlignment);
			if (ha == LEFT) {
				x = insets.left;
			} else if (ha == RIGHT) {
				x = this.maxWidth - ww + insets.left;
			} else if (ha == CENTER) {
				x = insets.left + (this.maxWidth - ww) / 2;
			}
		} else if (ha == RIGHT) {
			ha = getBidiHorizontalAlignment(this.textAlignment);
			if (ha == LEFT) {
				x = d.width - this.maxWidth - insets.right;
			} else if (ha == RIGHT) {
				x = d.width - ww - insets.right;
			} else if (ha == CENTER) {
				x = d.width - this.maxWidth - insets.right + (this.maxWidth - ww) / 2;
			}
		} else if (ha == CENTER) {
			ha = getBidiHorizontalAlignment(this.textAlignment);

			// Just imagine that ha=LEFT (much easier), and follow code
			int clientAreaWidth = d.width - insets.left - insets.right;
			if (ha == LEFT) {
				x = insets.left + (clientAreaWidth - this.maxWidth) / 2;
			} else if (ha == RIGHT) {
				x = insets.left + (clientAreaWidth - this.maxWidth) / 2 + this.maxWidth - ww;
			} else if (ha == CENTER) {
				x = insets.left + (clientAreaWidth - ww) / 2;
			}
		}

		int re = (this.getHeight() + this.fontMetrics.getHeight()) / 2 - 2;
		int w = this.fontMetrics.stringWidth(this.text);
		int last = 0;
		if (w > this.getWidth()) {
			char[] ca = this.text.toCharArray();
			last = ca.length;
			for (int i = 0; i < ca.length; i++) {
				if (this.fontMetrics.stringWidth(this.text.substring(0, i) + "..." + 2) > getWidth()) {
					last = i - 1;
					g.drawString(this.text.substring(0, last) + "...", 2, re);
					return;
				}
			}
		}
		g.drawString(this.text, x, re);
	}

	@Override
	protected void paintComponent(Graphics g) {

		if (this.getWidth() <= 0 || this.getHeight() <= 0) {
			return;
		}

		super.paintComponent(g);
		updateInfo();

		// background
		if (isOpaque()) {
			g.setColor(this.getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}

		g.setColor(this.getForeground());

		Dimension d = getSize();

		if (d.width != this.maxWidth || d.height != this.textHeight) {
			recalculateDimension();
		}

		Insets insets = this.getInsets();

		int y = 0;

		y = insets.top + this.lineAscent;

		if (this.multiLine) {
			paintMultiLine(g, d, insets, y);
		} else {
			paintOneLine(g, d, insets, y);
		}
	}

	private void paintMultiLine(Graphics g, Dimension d, Insets insets, int y) {
		for (int i = 0; i < this.vector.size(); i++) {
			int x = 0;
			int clientAreaWidth = d.width - insets.left - insets.right;
			x = insets.left + (clientAreaWidth - this.lineWidths[i]) / 2;
			x += this.btnMarginWidth;
			g.drawString(this.vector.elementAt(i), x, y);
			y += this.lineHeight;
		}
	}

	private void updateInfo() {
		if (this.text == null || this.text.isEmpty()) {
			return;
		}
		if (!this.needUpdate) {
			return;
		}
		updateTextVector();
		recalculateDimension();

		this.needUpdate = false;
	}

	public void setText(String str) {
		this.text = str;

		this.fontMetrics = getFontMetrics(getFont());
		this.needUpdate = true;
	}

	private int getBidiHorizontalAlignment(int ha) {
		if (ha == LEADING) {
			if (getComponentOrientation().isLeftToRight()) {
				ha = LEFT;
			} else {
				ha = RIGHT;
			}
		} else if (ha == TRAILING) {
			if (getComponentOrientation().isLeftToRight()) {
				ha = RIGHT;
			} else {
				ha = LEFT;
			}
		}
		return ha;
	}

	public int getVerticalAlignment() {
		return this.verticalAlignment;
	}

	public void setVerticalAlignment(int verticalAlignment) {
		this.verticalAlignment = verticalAlignment;
		this.needUpdate = true;
		repaint();
	}

	public int getHorizontalAlignment() {
		return this.horizontalAlignment;
	}

	public void setHorizontalAlignment(int horizontalAlignment) {
		this.horizontalAlignment = horizontalAlignment;
		this.needUpdate = true;
		repaint();
	}

	public int getTextAlignment() {
		return this.textAlignment;
	}

	public void setTextAlignment(int textAlignment) {
		this.textAlignment = textAlignment;
		this.needUpdate = true;
		repaint();
	}

	public void setMultiLine(boolean b) {
		this.multiLine = b;
		this.needUpdate = true;
		updateInfo();
	}

	public void setNeed_update(boolean need_update) {
		this.needUpdate = need_update;
	}

	public int getPreferredLineWidth() {
		return this.fontMetrics.stringWidth(this.text);
	}

	public int getPreferredLineHeight() {
		return this.fontMetrics.getHeight();
	}

	public int getLineDiff() {
		if (this.vector.size() > 1) {
			return this.fontMetrics.getHeight() * (this.vector.size() - 1);
		} else {
			return 0;
		}
	}

	@Override
	public Dimension getPreferredSize() {
		if (this.multiLine) {
			return new Dimension(getPreferredLineWidth(), this.fontMetrics.getHeight() * this.vector.size());
		} else {
			return new Dimension(getPreferredLineWidth(), this.fontMetrics.getHeight());
		}
	}
}
