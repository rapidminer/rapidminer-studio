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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.plaf.LayerUI;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ListHoverHelper;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;


/**
 * A cell renderer for lists containing {@link Card}s.
 *
 * @author Nils Woehler
 *
 */
public class CardCellRenderer extends DefaultListCellRenderer {

	private static final long serialVersionUID = 1L;
	private static final String BETA_FLAG = "beta-flag";
	private static final Font BETA_FONT = FontTools.getFont("Open Sans Semibold", Font.BOLD, 12);
	private static final Color BETA_COLOR = Colors.RAPIDMINER_ORANGE_BRIGHT;
	private static final int BETA_Y_OFFSET = 10;
	private static final int BETA_X_OFFSET = 5;

	protected static final int MAX_CAPTION_LENGTH = 13;

	private boolean selected = false;

	private boolean highlighted = false;

	private JLayer<JLabel> layer;

	private class CardLayerUI extends LayerUI<JLabel> {

		private static final long serialVersionUID = 1L;

		@Override
		public void paint(Graphics g, JComponent c) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			Rectangle rec = getBounds();

			g2.setColor(Colors.CARD_PANEL_BACKGROUND);
			g2.fillRect(rec.x, rec.y, rec.width - 1, rec.height);

			int w = (int) rec.getWidth();
			int h = (int) rec.getHeight();
			int x = (int) rec.getX();
			int y = (int) rec.getY();
			if (highlighted) {
				g2.setColor(Colors.CARD_PANEL_BACKGROUND_HIGHLIGHT);
				g2.fillRect(x, y, w, h);
			}
			if (selected) {
				g2.setColor(Colors.CARD_PANEL_BACKGROUND_SELECTED);
				g2.fillRect(x, y, w, h);
			}

			super.paint(g, c);

			if (Boolean.parseBoolean(String.valueOf(c.getClientProperty(BETA_FLAG)))) {
				String betaString = I18N.getGUIMessage("gui.cards.beta_flag.label");
				g2.setFont(BETA_FONT);
				int fontHeight = g2.getFontMetrics().getHeight();
				int fontWidth = g2.getFontMetrics().stringWidth(betaString);
				g2.setColor(BETA_COLOR);
				g2.fillRect(0, BETA_Y_OFFSET, fontWidth + BETA_X_OFFSET * 2, fontHeight);
				g2.setColor(Color.WHITE);
				g2.drawString(betaString, BETA_X_OFFSET, BETA_Y_OFFSET + fontHeight - 4);
			}

			g2.dispose();
		}
	}

	/**
	 * Creates a {@link CardCellRenderer} that is used by lists containing {@link Card}s.
	 */
	public CardCellRenderer() {
		setVerticalTextPosition(SwingConstants.BOTTOM);
		setHorizontalTextPosition(SwingConstants.CENTER);
		setHorizontalAlignment(CENTER);
		setOpaque(false);
		layer = new JLayer<>(this, new CardLayerUI());
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		if (isSelected) {
			selected = true;
			label.setBorder(null);
		} else {
			selected = false;
			label.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 2));
		}
		if (ListHoverHelper.index(list) == index) {
			highlighted = true;
		} else {
			highlighted = false;
		}

		Card card = (Card) value;
		String title = card.getTitle();
		String caption = card.getFooter();

		if (caption != null) {
			StringBuilder builder = new StringBuilder();
			builder.append("<html>");
			builder.append("<div style='text-align: center;width: 40px;'>");
			builder.append("<div>");
			builder.append(title);
			builder.append("</div>");
			builder.append("<div style='font-size:x-small; font-style:italic;margin-top:3px;'>");
			builder.append("(");
			if (caption.length() > MAX_CAPTION_LENGTH) {
				builder.append(getShortenedCaption(caption));
			} else {
				builder.append(caption);
			}
			builder.append(")");
			builder.append("</div>");
			builder.append("</div>");
			builder.append("</html>");
			title = builder.toString();
		}

		label.setText(title);
		label.setIcon(card.getIcon());

		layer.setToolTipText(card.getTip());
		layer.putClientProperty(BETA_FLAG, card.isBeta());

		return layer;
	}

	/**
	 * @param caption
	 *            the full length caption
	 *
	 * @return a shortened caption that has at most {@link #MAX_CAPTION_LENGTH} characters.
	 */
	private Object getShortenedCaption(String caption) {

		// split caption by spaces
		String[] captionParts = caption.split(" ");
		int captionLength = 0;

		// start with the last part of the caption
		int index = captionParts.length - 1;

		// create shortened version of caption only if caption has been splitted into multiple
		// parts..
		if (index != -1) {

			// add more splitted caption parts as long as string length is less than
			// MAX_CAPTION_LENGTH
			while (index >= 0 && captionLength < MAX_CAPTION_LENGTH) {
				captionLength += captionParts[index].length();

				// increase length by one for all missing spaces except the last part
				if (index < captionParts.length - 1) {
					++captionLength;
				}

				// only decrease index if caption is still short enough
				if (captionLength < MAX_CAPTION_LENGTH) {
					--index;
				} else {
					// else remove last added part
					++index;
				}
			}

			// sanity checks
			if (index < 0) {
				++index;
			}
			if (index == captionParts.length) {
				--index;
			}

			StringBuilder captionBuilder = new StringBuilder();

			// build shortened caption string, starting from the calculated index
			for (int i = index; i < captionParts.length; ++i) {
				captionBuilder.append(captionParts[i]);

				// add spaces for all parts except the last one
				if (i < captionParts.length - 1) {
					captionBuilder.append(" ");
				}
			}

			// generate shortened caption from caption builder
			caption = captionBuilder.toString();
		}

		// check once more if max caption length has not been violated
		if (caption.length() > MAX_CAPTION_LENGTH) {
			caption = caption.substring(caption.length() - MAX_CAPTION_LENGTH);
		}

		caption = "..." + caption;

		return caption;
	}
}
