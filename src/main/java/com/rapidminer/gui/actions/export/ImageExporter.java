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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.ps.PSGraphics2D;
import org.freehep.graphicsio.svg.SVGGraphics2D;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.rapidminer.gui.tools.PrintingTools;
import com.rapidminer.tools.I18N;


/**
 * A class for exporting {@link PrintableComponent}s as image. The currently supported formats are
 * PNG, JPG, PDF, SVG, and EPS.
 *
 * @author Nils Woehler
 *
 */
public class ImageExporter {

	private static final String EPS = "eps";
	private static final String SVG = "svg";
	private static final String JPG = "jpg";
	private static final String PNG = "png";
	private static final String PDF = "pdf";

	public enum ExportStatus {
		EXPORTED, ABORTED
	}

	private static final String[] FILE_EXTENSIONS = new String[] { PNG, JPG, SVG, EPS, PDF };

	private static final String[] EXTENSION_DESCRIPTIONS = new String[] { "Portable Network Graphics", "JPEG",
			"Scalable Vector Graphics", "Encapsulated PostScript", "Portable Document Format" };

	private PrintableComponent printableComponent;

	public ImageExporter(PrintableComponent printableComponent) {
		this.printableComponent = printableComponent;
	}

	/**
	 * Opens a dialog that prompts for a file location. Depending on the file extensions the
	 * provided component is exported as a bitmap or vector graphics image.
	 *
	 * @return the export status, which is EXPORTED or ABORTED.
	 *
	 * @throws IOException
	 *             in case something goes wrong
	 */
	public ExportStatus exportImage() throws ImageExportException {
		File chosenFile;
		try {
			chosenFile = PrintingTools.promptForFileLocation("export_image", FILE_EXTENSIONS, EXTENSION_DESCRIPTIONS);
		} catch (IOException e) {
			throw new ImageExportException("", e);
		}

		// no file chosen, do nothing
		if (chosenFile == null) {
			return ExportStatus.ABORTED;
		}

		String extension = chosenFile.getName().substring(chosenFile.getName().lastIndexOf('.') + 1);
		switch (extension) {
			case JPG:
				exportBitmap(JPG, chosenFile);
				break;
			case PNG:
				exportBitmap(PNG, chosenFile);
				break;
			case SVG:
				exportVectorGraphics(SVG, chosenFile);
				break;
			case EPS:
				exportVectorGraphics(EPS, chosenFile);
				break;
			case PDF:
				exportVectorGraphics(PDF, chosenFile);
				break;
			default:
				throw new ImageExportException(I18N.getMessage(I18N.getUserErrorMessagesBundle(),
						"error.image_export.unknown_format", extension));
		}

		return ExportStatus.EXPORTED;
	}

	private void exportBitmap(String formatName, File outputFile) throws ImageExportException {
		int width = printableComponent.getExportComponent().getWidth();
		int height = printableComponent.getExportComponent().getHeight();
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics graphics = image.getGraphics();
		graphics.setColor(Color.white);
		graphics.fillRect(0, 0, width, height);
		printableComponent.getExportComponent().print(graphics);
		try {
			ImageIO.write(image, formatName, outputFile);
		} catch (IOException e) {
			throw new ImageExportException(I18N.getMessage(I18N.getUserErrorMessagesBundle(),
					"error.image_export.export_failed"), e);
		}
	}

	private void exportVectorGraphics(String formatName, File outputFile) throws ImageExportException {
		Component component = printableComponent.getExportComponent();
		int width = component.getWidth();
		int height = component.getHeight();
		try (FileOutputStream fs = new FileOutputStream(outputFile)) {
			switch (formatName) {
				case PDF:
					// create pdf document with slightly increased width and height
					// (otherwise the image gets cut off)
					Document document = new Document(new Rectangle(width + 5, height + 5));
					PdfWriter writer = PdfWriter.getInstance(document, fs);
					document.open();
					PdfContentByte cb = writer.getDirectContent();
					PdfTemplate tp = cb.createTemplate(width, height);
					Graphics2D g2 = tp.createGraphics(width, height, new DefaultFontMapper());
					component.print(g2);
					g2.dispose();
					cb.addTemplate(tp, 0, 0);
					document.close();
					break;
				case SVG:
					exportFreeHep(component, fs, new SVGGraphics2D(fs, new Dimension(width, height)));
					break;
				case EPS:
					exportFreeHep(component, fs, new PSGraphics2D(fs, new Dimension(width, height)));
					break;
				default:
					// cannot happen
					break;
			}
		} catch (Exception e) {
			throw new ImageExportException(I18N.getMessage(I18N.getUserErrorMessagesBundle(),
					"error.image_export.export_failed"), e);
		}
	}

	private void exportFreeHep(Component component, FileOutputStream fs, VectorGraphics vg) {
		vg.startExport();
		component.print(vg);
		vg.endExport();
		vg.dispose();
	}

}
