/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tools.color;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.tools.I18N;


/**
 * Utility class for choosing color GUIs.
 *
 * @author Marco Boeck
 * @since 9.2.0
 */
public enum ColorChooserUtilities {

	INSTANCE;

	/**
	 * Opens a color chooser dialog with Hue Saturation Lightness selection and returns the selected color.
	 *
	 * @param startColor
	 * 		the initially selected color
	 * @return the selected color or {@code null} if the user cancelled the dialog
	 * @since 9.2.0
	 */
	public Color chooseColor(Color startColor) {
		JColorChooser colorChooser = new JColorChooser(startColor != null ? startColor : Color.BLACK);
		JLabel previewLabel = new JLabel() {

			@Override
			public void paint(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setColor(colorChooser.getColor());
				g2.fillRect(0, 0, getWidth(), getHeight() - 7);
				g2.dispose();
			}
		};
		previewLabel.setPreferredSize(new Dimension(150, 50));
		colorChooser.setPreviewPanel(previewLabel);
		// only keep HSL panel
		colorChooser.removeChooserPanel(colorChooser.getChooserPanels()[4]);
		colorChooser.removeChooserPanel(colorChooser.getChooserPanels()[3]);
		colorChooser.removeChooserPanel(colorChooser.getChooserPanels()[1]);
		colorChooser.removeChooserPanel(colorChooser.getChooserPanels()[0]);

		AtomicReference<Color> colorReference = new AtomicReference<>();
		JDialog dialog = JColorChooser.createDialog(ApplicationFrame.getApplicationFrame(),
				I18N.getGUILabel("persistent_charts.configuration.generic.color.dialog.title"),
				true, colorChooser, event -> colorReference.set(colorChooser.getColor()), null);
		dialog.setVisible(true);
		// control flow only continues once user closed dialog
		return colorReference.get();
	}
}
