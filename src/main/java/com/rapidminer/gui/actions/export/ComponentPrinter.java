/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.license.LicenseTools;
import com.rapidminer.gui.tools.PrintingTools;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;


/**
 * A Printable and Pageable that can print an arbitrary number of components. It scales and
 * translates each page such that one component is visible per page.
 * 
 * @author Simon Fischer, Ingo Mierswa, Nils Woehler
 */
public class ComponentPrinter implements Printable, Pageable {

	private PrintableComponent[] components = null;

	private PageFormat pageFormat = PrintingTools.getPrinterJob().defaultPage();

	public static final Font TITLE_FONT = new Font("LucidaSans", Font.PLAIN, 9);

	/** The given components that should be printed. */
	public ComponentPrinter(PrintableComponent... components) {
		this.components = components;
	}

	@Override
	public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
		return print(g, pageFormat.getImageableX(), pageFormat.getImageableY(), pageFormat.getImageableWidth(),
				pageFormat.getImageableHeight(), pageIndex);
	}

	/**
	 * 
	 * @param g
	 *            the graphics object
	 * @param pageFormat
	 *            the page format
	 * @param width
	 *            the downscaled width
	 * @param height
	 *            the downscaled height
	 * @param pageIndex
	 *            the page index that should be printed
	 */
	public int print(Graphics g, double x, double y, double width, double height, int pageIndex) {
		if (pageIndex >= components.length) {
			return NO_SUCH_PAGE;
		}

		String title = components[pageIndex].getExportName();
		if (title == null) {
			title = LicenseTools.translateProductName(ProductConstraintManager.INSTANCE.getActiveLicense());
		}
		Rectangle2D rect = TITLE_FONT.getStringBounds(title, ((Graphics2D) g).getFontRenderContext());
		g.setFont(TITLE_FONT);
		int stringX = (int) (x + width / 2 - rect.getWidth() / 2);
		int stringY = (int) (y - rect.getY());
		g.drawString(title, stringX, stringY);

		// remove string rect height from graphic pane
		height = height - rect.getHeight() * 2;

		Graphics2D translated = (Graphics2D) g.create((int) x, (int) (y + rect.getHeight() * 2), (int) width, (int) height);

		double widthFactor = width / components[pageIndex].getExportComponent().getWidth();
		double heightFactor = height / components[pageIndex].getExportComponent().getHeight();
		double scaleFactor = Math.min(widthFactor, heightFactor);
		translated.scale(scaleFactor, scaleFactor);
		components[pageIndex].getExportComponent().print(translated);
		translated.dispose();
		return PAGE_EXISTS;
	}

	@Override
	public int getNumberOfPages() {
		return components.length;
	}

	@Override
	public PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException {
		return pageFormat;
	}

	@Override
	public Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException {
		return this;
	}

	/**
	 * @return the {@link PrintableComponent} for the specified index
	 */
	public PrintableComponent getPrintableComponent(int index) {
		return components[index];
	}

}
