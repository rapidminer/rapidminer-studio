/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.look;

import com.rapidminer.gui.tools.SwingTools;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;


/**
 * The colors used for the RapidLook look and feel.
 * 
 * @author Ingo Mierswa
 */
public class Colors {

	public static ColorUIResource white = new ColorUIResource(255, 255, 255);

	public static ColorUIResource black = new ColorUIResource(0, 0, 0);

	// set via UIManager.setColor("VLDocking.highlight", ...)
	public static ColorUIResource vlDockingHighlight = new ColorUIResource(255, 0, 0);

	// set via UIManager.setColor("VLDocking.shadow", ...)
	public static ColorUIResource vlDockingShadow = new ColorUIResource(0, 255, 0);

	private ColorUIResource[] tableHeaderColors = new ColorUIResource[] {
			new ColorUIResource(210, 210, 230),
			new ColorUIResource(220, 220, 240),
			new ColorUIResource(225, 225, 245),
			new ColorUIResource(235, 235, 255),
			new ColorUIResource(245, 145, 0), // highlight 1
			new ColorUIResource(245, 145, 0), // hightlight 2
			new ColorUIResource(250, 250, 255), new ColorUIResource(200, 200, 220), new ColorUIResource(250, 250, 255),
			new ColorUIResource(230, 230, 250), new ColorUIResource(225, 225, 245), new ColorUIResource(215, 215, 235),
			new ColorUIResource(210, 210, 230), new ColorUIResource(190, 190, 210) };

	private ColorUIResource[] buttonSkinColors = new ColorUIResource[] {
			new ColorUIResource(253, 254, 255), // begin pressed // 0
			new ColorUIResource(253, 253, 255), // both
			new ColorUIResource(249, 251, 255), // both
			new ColorUIResource(248, 250, 255), // both
			new ColorUIResource(254, 254, 255), // both
			new ColorUIResource(234, 234, 235), // 5
			new ColorUIResource(230, 232, 235),
			new ColorUIResource(224, 229, 235),
			new ColorUIResource(209, 221, 234), // end pressed
			new ColorUIResource(254, 254, 255),
			new ColorUIResource(253, 254, 255), // 10
			new ColorUIResource(246, 249, 255), new ColorUIResource(243, 247, 255), new ColorUIResource(239, 244, 255),
			new ColorUIResource(233, 240, 255),
			new ColorUIResource(225, 235, 255), // 15
			new ColorUIResource(217, 230, 255), new ColorUIResource(252, 253, 255), new ColorUIResource(247, 251, 255),
			new ColorUIResource(224, 228, 235), // start down
			new ColorUIResource(239, 244, 255),  // 20
			new ColorUIResource(218, 230, 240)  // 21
	};

	/*
	 * private ColorUIResource[] buttonSkinColors = new ColorUIResource[] { new ColorUIResource(253,
	 * 254, 255), // begin pressed new ColorUIResource(253, 253, 255), new ColorUIResource(249, 251,
	 * 255), new ColorUIResource(248, 250, 255), new ColorUIResource(254, 254, 255), new
	 * ColorUIResource(254, 254, 255), new ColorUIResource(250, 252, 255), new ColorUIResource(244,
	 * 249, 255), new ColorUIResource(229, 241, 254), // end pressed new ColorUIResource(254, 254,
	 * 255), new ColorUIResource(253, 254, 255), new ColorUIResource(246, 249, 255), new
	 * ColorUIResource(243, 247, 255), new ColorUIResource(239, 244, 255), new ColorUIResource(233,
	 * 240, 255), new ColorUIResource(225, 235, 255), new ColorUIResource(217, 230, 255), new
	 * ColorUIResource(252, 253, 255), new ColorUIResource(247, 251, 255) };
	 */

	private ColorUIResource[] tabbedPaneColors = new ColorUIResource[] {
			// new ColorUIResource(220, 225, 230), // 0
			new ColorUIResource(200, 205, 210), // 0
			new ColorUIResource(215, 220, 225), new ColorUIResource(170, 170, 190), new ColorUIResource(200, 200, 220),
			new ColorUIResource(190, 200, 220), new ColorUIResource(250, 250, 250),
			new ColorUIResource(255, 255, 255),
			new ColorUIResource(210, 210, 230),
			new ColorUIResource(180, 190, 210),
			new ColorUIResource(200, 200, 220),
			new ColorUIResource(210, 210, 230), // 10
			new ColorUIResource(220, 220, 240), new ColorUIResource(230, 230, 250), new ColorUIResource(235, 235, 255),
			new ColorUIResource(240, 240, 255), new ColorUIResource(245, 245, 255), new ColorUIResource(250, 250, 255),
			new ColorUIResource(255, 255, 255), new ColorUIResource(255, 255, 255), new ColorUIResource(210, 210, 230),
			new ColorUIResource(240, 240, 255), // 20
			new ColorUIResource(245, 145, 0), // highlight
	};

	private ColorUIResource[] spinnerColors = new ColorUIResource[] { new ColorUIResource(230, 230, 250),
			new ColorUIResource(170, 170, 190), new ColorUIResource(240, 240, 255), new ColorUIResource(235, 235, 255),
			new ColorUIResource(220, 220, 240), new ColorUIResource(220, 220, 240), new ColorUIResource(210, 210, 230),
			new ColorUIResource(170, 170, 190), new ColorUIResource(230, 230, 250), new ColorUIResource(110, 110, 110),
			new ColorUIResource(195, 195, 195) };

	private ColorUIResource[][] buttonBorderColors = new ColorUIResource[][] { { // gray
			new ColorUIResource(100, 100, 100), new ColorUIResource(80, 80, 80), new ColorUIResource(150, 150, 150),
					new ColorUIResource(200, 200, 200), new ColorUIResource(150, 150, 150),
					new ColorUIResource(115, 115, 115), new ColorUIResource(125, 125, 125),
					new ColorUIResource(190, 190, 190), new ColorUIResource(100, 100, 100),
					new ColorUIResource(215, 215, 215), new ColorUIResource(235, 235, 235) }, { // orange
																								// hover
			new ColorUIResource(200, 80, 20), new ColorUIResource(180, 60, 0), new ColorUIResource(230, 110, 45),
					new ColorUIResource(255, 205, 175), new ColorUIResource(230, 115, 50),
					new ColorUIResource(220, 105, 40), new ColorUIResource(230, 115, 50), new ColorUIResource(230, 125, 45),
					new ColorUIResource(205, 100, 20), new ColorUIResource(255, 200, 160),
					new ColorUIResource(255, 225, 205) }, { // orange focus
			new ColorUIResource(180, 60, 0), new ColorUIResource(160, 40, 0), new ColorUIResource(210, 90, 25),
					new ColorUIResource(235, 185, 155), new ColorUIResource(210, 95, 30), new ColorUIResource(200, 85, 20),
					new ColorUIResource(210, 95, 30), new ColorUIResource(210, 105, 25), new ColorUIResource(185, 80, 0),
					new ColorUIResource(235, 180, 140), new ColorUIResource(235, 205, 185) }, { // gray
																								// 2
			new ColorUIResource(170, 170, 170), new ColorUIResource(150, 150, 150), new ColorUIResource(200, 200, 200),
					new ColorUIResource(220, 220, 220), new ColorUIResource(200, 200, 200),
					new ColorUIResource(180, 180, 180), new ColorUIResource(185, 185, 185),
					new ColorUIResource(220, 220, 220), new ColorUIResource(150, 150, 150),
					new ColorUIResource(230, 230, 230), new ColorUIResource(240, 240, 240) }, { // default
			new ColorUIResource(100, 100, 100), new ColorUIResource(80, 80, 80), new ColorUIResource(150, 150, 150),
					new ColorUIResource(200, 200, 200), new ColorUIResource(150, 150, 150),
					new ColorUIResource(115, 115, 115), new ColorUIResource(125, 125, 125),
					new ColorUIResource(140, 140, 160), new ColorUIResource(50, 50, 70), new ColorUIResource(165, 165, 185),
					new ColorUIResource(185, 185, 205) } };

	private ColorUIResource[] toolbarButtonColors = new ColorUIResource[] { new ColorUIResource(170, 170, 170),
			new ColorUIResource(250, 250, 250), new ColorUIResource(190, 190, 190), new ColorUIResource(230, 230, 230),
			new ColorUIResource(240, 240, 240), new ColorUIResource(200, 200, 200), new ColorUIResource(215, 215, 215),
			new ColorUIResource(235, 235, 235), new ColorUIResource(220, 220, 220), new ColorUIResource(225, 225, 225) };

	private static ColorUIResource[][] radioButtonColors = new ColorUIResource[][] {
			{ new ColorUIResource(242, 242, 242), new ColorUIResource(197, 197, 197), new ColorUIResource(172, 172, 172),
					new ColorUIResource(153, 153, 153), new ColorUIResource(215, 215, 215) },
			{ // orange border hover
			new ColorUIResource(255, 230, 200), new ColorUIResource(230, 185, 160), new ColorUIResource(220, 155, 160),
					new ColorUIResource(210, 135, 100), new ColorUIResource(240, 205, 190) },
			{ new ColorUIResource(240, 245, 255), new ColorUIResource(255, 255, 255), new ColorUIResource(253, 253, 253),
					new ColorUIResource(251, 251, 251), new ColorUIResource(249, 249, 249),
					new ColorUIResource(247, 247, 247), new ColorUIResource(245, 245, 245),
					new ColorUIResource(243, 243, 243), new ColorUIResource(240, 240, 240),
					new ColorUIResource(238, 238, 238) }, { // gray bullet
			new ColorUIResource(210, 210, 230), new ColorUIResource(200, 200, 220), new ColorUIResource(180, 180, 200),
					new ColorUIResource(170, 170, 190), new ColorUIResource(160, 160, 180),
					new ColorUIResource(150, 150, 170), new ColorUIResource(140, 140, 160),
					new ColorUIResource(120, 120, 140), new ColorUIResource(205, 205, 205) } };

	private static ColorUIResource[][] checkBoxButtonColors = new ColorUIResource[][] {
			{ new ColorUIResource(200, 200, 200), new ColorUIResource(145, 145, 145), new ColorUIResource(105, 105, 105),
					new ColorUIResource(255, 255, 255), new ColorUIResource(252, 252, 252),
					new ColorUIResource(248, 248, 248), new ColorUIResource(245, 245, 245),
					new ColorUIResource(241, 241, 241), new ColorUIResource(238, 238, 238) },
			{ new ColorUIResource(205, 215, 233), new ColorUIResource(170, 185, 215), new ColorUIResource(80, 110, 173),
					new ColorUIResource(238, 245, 255), new ColorUIResource(215, 225, 245) },
			{ new ColorUIResource(235, 235, 235), new ColorUIResource(205, 205, 205), new ColorUIResource(180, 180, 180),
					new ColorUIResource(240, 240, 240), new ColorUIResource(190, 190, 190) },
			{ new ColorUIResource(235, 165, 120), new ColorUIResource(255, 220, 190), new ColorUIResource(230, 180, 130),
					new ColorUIResource(225, 145, 80), new ColorUIResource(200, 130, 70),
					new ColorUIResource(215, 160, 105), new ColorUIResource(240, 190, 155),
					new ColorUIResource(210, 135, 70), new ColorUIResource(245, 170, 100),
					new ColorUIResource(235, 185, 140), new ColorUIResource(210, 135, 70) } };

	private ColorUIResource[][] textFieldBorderColors = new ColorUIResource[][] {
			{ // focus
			new ColorUIResource(200, 125, 50), new ColorUIResource(235, 210, 130), new ColorUIResource(255, 195, 160),
					new ColorUIResource(230, 160, 110) },
			{ new ColorUIResource(110, 110, 110), new ColorUIResource(220, 220, 220), new ColorUIResource(200, 200, 200),
					new ColorUIResource(160, 160, 160) },
			{ new ColorUIResource(160, 160, 160), new ColorUIResource(225, 225, 225), new ColorUIResource(211, 211, 211),
					new ColorUIResource(175, 175, 175) } };

	private ColorUIResource[][] internalFrameTitlePaneColors = new ColorUIResource[][] {
			{ new ColorUIResource(160, 175, 200), new ColorUIResource(125, 150, 190), new ColorUIResource(122, 148, 194),
					new ColorUIResource(85, 123, 187), new ColorUIResource(75, 115, 185), new ColorUIResource(85, 123, 191),
					new ColorUIResource(172, 180, 205), new ColorUIResource(85, 123, 191),
					new ColorUIResource(95, 137, 192), new ColorUIResource(85, 123, 191),
					new ColorUIResource(130, 155, 197), new ColorUIResource(188, 201, 226),
					new ColorUIResource(153, 172, 206), new ColorUIResource(125, 150, 192) },
			{ new ColorUIResource(160, 175, 200), new ColorUIResource(125, 150, 190), new ColorUIResource(122, 148, 194),
					new ColorUIResource(85, 123, 187), new ColorUIResource(75, 115, 185), new ColorUIResource(85, 123, 191),
					new ColorUIResource(85, 125, 193), new ColorUIResource(85, 123, 191),
					new ColorUIResource(172, 180, 205), new ColorUIResource(84, 122, 189),
					new ColorUIResource(95, 137, 192), new ColorUIResource(81, 119, 187), new ColorUIResource(79, 118, 185),
					new ColorUIResource(77, 116, 183), new ColorUIResource(75, 113, 181), new ColorUIResource(73, 112, 179),
					new ColorUIResource(72, 110, 177), new ColorUIResource(70, 108, 176), new ColorUIResource(68, 107, 174),
					new ColorUIResource(67, 105, 172), new ColorUIResource(65, 104, 171), new ColorUIResource(63, 102, 169),
					new ColorUIResource(62, 100, 167), new ColorUIResource(61, 99, 165), new ColorUIResource(59, 96, 162),
					new ColorUIResource(56, 93, 157), new ColorUIResource(49, 85, 148), new ColorUIResource(43, 80, 143),
					new ColorUIResource(117, 120, 130), new ColorUIResource(130, 155, 197),
					new ColorUIResource(188, 201, 226), new ColorUIResource(153, 172, 206),
					new ColorUIResource(125, 150, 192) } };

	private ColorUIResource[][] bordersColors = new ColorUIResource[][] { { new ColorUIResource(205, 160, 130),
			new ColorUIResource(175, 110, 70) } };

	private ColorUIResource[][] progressBarColors = new ColorUIResource[][] {
			{ new ColorUIResource(245, 170, 120), new ColorUIResource(255, 215, 150), new ColorUIResource(255, 210, 170),
					new ColorUIResource(250, 180, 140), new ColorUIResource(255, 165, 100),
					new ColorUIResource(250, 155, 100), new ColorUIResource(250, 140, 80),
					new ColorUIResource(250, 170, 130), new ColorUIResource(250, 180, 140),
					new ColorUIResource(255, 160, 105), new ColorUIResource(250, 185, 155),
					new ColorUIResource(255, 160, 95), new ColorUIResource(230, 160, 120) },
			{ new ColorUIResource(255, 180, 130), new ColorUIResource(255, 225, 160), new ColorUIResource(255, 220, 180),
					new ColorUIResource(255, 190, 150), new ColorUIResource(255, 175, 110),
					new ColorUIResource(255, 165, 110), new ColorUIResource(255, 150, 90),
					new ColorUIResource(255, 180, 140), new ColorUIResource(255, 190, 150),
					new ColorUIResource(255, 170, 115), new ColorUIResource(255, 195, 165),
					new ColorUIResource(255, 170, 105), new ColorUIResource(240, 170, 130) }, };

	private ColorUIResource[][] scrollBarColors = new ColorUIResource[][] {
			{ new ColorUIResource(180, 180, 180), new ColorUIResource(240, 240, 240), new ColorUIResource(245, 245, 245),
					new ColorUIResource(248, 248, 248) },
			{ new ColorUIResource(180, 180, 200), new ColorUIResource(200, 200, 220), new ColorUIResource(195, 195, 215),
					new ColorUIResource(190, 190, 210), new ColorUIResource(185, 185, 205),
					new ColorUIResource(180, 180, 200), new ColorUIResource(175, 175, 195),
					new ColorUIResource(170, 170, 190), new ColorUIResource(165, 165, 185),
					new ColorUIResource(160, 160, 180), new ColorUIResource(200, 200, 220),
					new ColorUIResource(180, 180, 200), new ColorUIResource(220, 220, 240),
					new ColorUIResource(200, 200, 220) } };

	private ColorUIResource[][] arrowButtonColors = new ColorUIResource[][] {
			{ new ColorUIResource(220, 220, 240), new ColorUIResource(160, 160, 180), new ColorUIResource(210, 210, 230),
					new ColorUIResource(190, 190, 210), new ColorUIResource(160, 160, 180),
					new ColorUIResource(180, 180, 200), new ColorUIResource(210, 210, 230),
					new ColorUIResource(220, 220, 250) },
			{ new ColorUIResource(230, 230, 250), new ColorUIResource(230, 230, 250), new ColorUIResource(200, 200, 220),
					new ColorUIResource(180, 180, 200), new ColorUIResource(220, 220, 240),
					new ColorUIResource(170, 170, 190), new ColorUIResource(200, 200, 220),
					new ColorUIResource(150, 150, 170), new ColorUIResource(215, 215, 235),
					new ColorUIResource(180, 180, 200), new ColorUIResource(160, 160, 180),
					new ColorUIResource(150, 150, 170), new ColorUIResource(170, 170, 190),
					new ColorUIResource(200, 200, 220), new ColorUIResource(170, 170, 190),
					new ColorUIResource(160, 160, 180), new ColorUIResource(220, 220, 240) } };

	private ColorUIResource[] fileChooserColors = new ColorUIResource[] { new ColorUIResource(255, 200, 200),
			new ColorUIResource(230, 170, 170) };

	private ColorUIResource[] toolbarColors = new ColorUIResource[] { new ColorUIResource(240, 240, 245) };

	private ImageIconUIResource sliderImage = new ImageIconUIResource(SwingTools.createImage("plaf/slider.png").getImage());

	private ColorUIResource[][] sliderColors = new ColorUIResource[][] { {} };

	private ColorUIResource desktopBackgroundColor = new ColorUIResource(180, 195, 220);

	private static ColorUIResource commonBackground = new ColorUIResource(240, 240, 245);

	private static ColorUIResource commonForeground = new ColorUIResource(0, 0, 0);

	private static ColorUIResource commonFocusColor = new ColorUIResource(205, 85, 0);

	public static ColorUIResource getWhite() {
		return white;
	}

	public static ColorUIResource getBlack() {
		return black;
	}

	public ColorUIResource getMenuItemBackground() {
		return new ColorUIResource(UIManager.getColor("MenuItem.background"));
	}

	public ColorUIResource getMenuItemSelectionBackground() {
		return new ColorUIResource(UIManager.getColor("MenuItem.selectionBackground"));
	}

	public ColorUIResource getMenuItemFadingColor() {
		return new ColorUIResource(UIManager.getColor("MenuItem.fadingColor"));
	}

	public String getName() {
		return "Colors";
	}

	public void addCustomEntriesToTable(UIDefaults table) {
		Object[] values = new Object[] { "MenuItem.background", getWhite(), "MenuItem.selectionBackground",
				new ColorUIResource(150, 150, 170), "MenuItem.fadingColor", new ColorUIResource(235, 235, 255),
				"ToolTip.background", new ColorUIResource(250, 240, 225), "ToolTip.borderColor",
				new ColorUIResource(113, 103, 74) };
		table.putDefaults(values);
	}

	public ColorUIResource getCommonFocusColor() {
		return commonFocusColor;
	}

	public ColorUIResource getTextHighlightBackColor() {
		return new ColorUIResource(150, 150, 170);
	}

	public ColorUIResource[] getButtonSkinColors() {
		return this.buttonSkinColors;
	}

	public ColorUIResource[] getToolbarButtonColors() {
		return this.toolbarButtonColors;
	}

	public ColorUIResource[][] getButtonBorderColors() {
		return this.buttonBorderColors;
	}

	public ColorUIResource getCommonBackground() {
		return commonBackground;
	}

	public ColorUIResource getCommonForeground() {
		return commonForeground;
	}

	public ColorUIResource[][] getRadioButtonColors() {
		return radioButtonColors;
	}

	public ColorUIResource[][] getCheckBoxButtonColors() {
		return checkBoxButtonColors;
	}

	public ColorUIResource[][] getTextFieldBorderColors() {
		return this.textFieldBorderColors;
	}

	public ColorUIResource[][] getInternalFrameTitlePaneColors() {
		return this.internalFrameTitlePaneColors;
	}

	public ColorUIResource[][] getBorderColors() {
		return this.bordersColors;
	}

	public ColorUIResource[][] getProgressBarColors() {
		return this.progressBarColors;
	}

	public ColorUIResource[][] getScrollBarColors() {
		return this.scrollBarColors;
	}

	public ColorUIResource[][] getArrowButtonColors() {
		return this.arrowButtonColors;
	}

	public ColorUIResource[][] getSliderColors() {
		return this.sliderColors;
	}

	public ImageIconUIResource getSliderImage() {
		return this.sliderImage;
	}

	public ColorUIResource[] getSpinnerColors() {
		return this.spinnerColors;
	}

	public ColorUIResource[] getTabbedPaneColors() {
		return this.tabbedPaneColors;
	}

	public ColorUIResource[] getTableHeaderColors() {
		return this.tableHeaderColors;
	}

	public ColorUIResource[] getFileChooserColors() {
		return this.fileChooserColors;
	}

	public ColorUIResource getDesktopBackgroundColor() {
		return this.desktopBackgroundColor;
	}

	public ColorUIResource[] getToolbarColors() {
		return this.toolbarColors;
	}
}
