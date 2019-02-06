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
package com.rapidminer.gui.actions.export;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.print.PageFormat;
import java.beans.Transient;

import javax.swing.JPanel;

import org.jdesktop.swingx.border.DropShadowBorder;


/**
 * The preview panel used by the {@link PrintAndExportDialog} to show a print preview.
 *
 * @author Nils Woehler
 *
 */
public class PrintPreviewPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int MAX_DIM = 430;

	enum Orientation {
		LANDSCAPE, PORTRAIT;
	}

	private class ComponentPreviewPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private final Orientation orientation;

		private ComponentPreviewPanel(Orientation orientation) {
			this.orientation = orientation;
			setBorder(new DropShadowBorder(Color.GRAY, 2, 0.7f, 2, false, false, true, true));
		}

		@Override
		@Transient
		public Dimension getMinimumSize() {
			return getDimension();
		}

		@Override
		@Transient
		public Dimension getMaximumSize() {
			return getDimension();
		}

		@Override
		@Transient
		public Dimension getPreferredSize() {
			return getDimension();
		}

		private Dimension getDimension() {
			double pWidth = pageFormat.getWidth();
			double pHeight = pageFormat.getHeight();

			double width = MAX_DIM;
			double height = MAX_DIM;

			if (orientation == Orientation.LANDSCAPE) {
				double scaleFactor = pHeight / pWidth;
				height = width * scaleFactor;
			} else {
				double scaleFactor = pWidth / pHeight;
				width = height * scaleFactor;
			}
			return new Dimension((int) Math.rint(width), (int) Math.rint(height));
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			double width = getDimension().getWidth() - 4;
			double height = getDimension().getHeight() - 4;

			g.setColor(Color.white);
			g.fillRect(2, 2, (int) width, (int) height);

			double widthFactor = width / pageFormat.getWidth();
			double x = pageFormat.getImageableX() * widthFactor + 2;
			double scaledWidth = pageFormat.getImageableWidth() * widthFactor;

			double heightFactor = height / pageFormat.getHeight();
			double y = pageFormat.getImageableY() * heightFactor + 2;
			double scaledHeight = pageFormat.getImageableHeight() * heightFactor;

			printer.print(g, x, y, scaledWidth, scaledHeight, 0);
		}
	}

	private final ComponentPrinter printer;
	private PageFormat pageFormat;

	private CardLayout cardLayout;

	private ComponentPreviewPanel landscapePreview;
	private ComponentPreviewPanel portraitPreview;

	private JPanel landscapePanel;

	private JPanel portraitPanel;

	/**
	 * Creates a preview panel for the specified {@link PrintableComponent}.
	 *
	 * @param comp
	 *            the {@link PrintableComponent} the preview panel should be created for.
	 */
	public PrintPreviewPanel(PrintableComponent comp, PageFormat pageFormat) {
		this.printer = new ComponentPrinter(comp);
		this.cardLayout = new CardLayout();
		this.pageFormat = pageFormat;
		setLayout(cardLayout);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.CENTER;

		landscapePanel = new JPanel(new GridBagLayout());
		landscapePreview = new ComponentPreviewPanel(Orientation.LANDSCAPE);
		landscapePanel.add(landscapePreview, gbc);
		add(landscapePanel, Orientation.LANDSCAPE.toString());

		portraitPanel = new JPanel(new GridBagLayout());
		portraitPreview = new ComponentPreviewPanel(Orientation.PORTRAIT);
		portraitPanel.add(portraitPreview, gbc);
		add(portraitPanel, Orientation.PORTRAIT.toString());

		// set page format
		setPageFormat(pageFormat);
	}

	public void setPageFormat(PageFormat pageFormat) {
		this.pageFormat = pageFormat;
		remove(landscapePanel);
		remove(portraitPanel);
		add(landscapePanel, Orientation.LANDSCAPE.toString());
		add(portraitPanel, Orientation.PORTRAIT.toString());
		if (pageFormat.getOrientation() == PageFormat.LANDSCAPE) {
			cardLayout.show(this, Orientation.LANDSCAPE.toString());
		} else {
			cardLayout.show(this, Orientation.PORTRAIT.toString());
		}
		repaint();
	}

}
