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

import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;


/**
 * The colors used for the RapidLook look and feel.
 *
 * @author Ingo Mierswa, Marco Boeck
 */
public class Colors {

	private static final ColorUIResource INPUT_BACKGROUND = new ColorUIResource(255, 255, 255);
	private static final ColorUIResource INPUT_BACKGROUND_DARK = new ColorUIResource(240, 240, 240);
	private static final ColorUIResource INPUT_BACKGROUND_DISABLED = new ColorUIResource(254, 254, 254);
	private static final ColorUIResource SELECTION_BOX_FOREGROUND = new ColorUIResource(255, 130, 40);
	private static final ColorUIResource SELECTION_BOX_FOREGROUND_DISABLED = new ColorUIResource(155, 155, 155);

	private static final ColorUIResource INPUT_BORDER = new ColorUIResource(187, 187, 187);
	private static final ColorUIResource INPUT_BORDER_DISABLED = new ColorUIResource(227, 227, 227);
	private static final ColorUIResource INPUT_BORDER_FOCUS = new ColorUIResource(145, 145, 145);
	private static final ColorUIResource INPUT_BORDER_DARK = new ColorUIResource(157, 157, 157);
	private static final ColorUIResource INPUT_BORDER_DARK_DISABLED = INPUT_BORDER_DISABLED;
	private static final ColorUIResource INPUT_BORDER_DARK_FOCUS = new ColorUIResource(115, 115, 115);

	public static final ColorUIResource SELECTION_FOREGROUND = new ColorUIResource(155, 155, 155);
	public static final ColorUIResource SELECTION_FOREGROUND_DISABLED = new ColorUIResource(209, 208, 208);

	public static final ColorUIResource SPECIAL_FOREGROUND = SELECTION_BOX_FOREGROUND;

	public static final ColorUIResource WINDOW_BACKGROUND = new ColorUIResource(250, 250, 250);
	public static final ColorUIResource PANEL_BACKGROUND = new ColorUIResource(233, 234, 234);
	public static final ColorUIResource PANEL_BORDER = INPUT_BORDER_FOCUS;
	public static final ColorUIResource POPUP_BORDER = INPUT_BORDER_FOCUS;
	public static final ColorUIResource PANEL_SEPARATOR = new ColorUIResource(216, 216, 216);

	public static final ColorUIResource TEXTFIELD_BACKGROUND = INPUT_BACKGROUND;
	public static final ColorUIResource TEXTFIELD_BACKGROUND_DISABLED = INPUT_BACKGROUND_DISABLED;
	public static final ColorUIResource TEXTFIELD_BORDER = INPUT_BORDER;
	public static final ColorUIResource TEXTFIELD_BORDER_DISABLED = INPUT_BORDER_DISABLED;
	public static final ColorUIResource TEXTFIELD_BORDER_FOCUS = INPUT_BORDER_FOCUS;
	public static final ColorUIResource TEXTFIELD_BORDER_DARK = INPUT_BORDER_DARK;
	public static final ColorUIResource TEXTFIELD_BORDER_DARK_DISABLED = INPUT_BORDER_DARK_DISABLED;
	public static final ColorUIResource TEXTFIELD_BORDER_DARK_FOCUS = INPUT_BORDER_DARK_FOCUS;

	public static final ColorUIResource TEXT_FOREGROUND = new ColorUIResource(0, 0, 0);
	public static final ColorUIResource TEXT_HIGHLIGHT_BACKGROUND = new ColorUIResource(178, 215, 255);
	public static final ColorUIResource TEXT_HIGHLIGHT_FOREGROUND = new ColorUIResource(0, 0, 0);
	public static final ColorUIResource TEXT_LIGHT_FOREGROUND = new ColorUIResource(51, 51, 51);

	public static final ColorUIResource BUTTON_BORDER = new ColorUIResource(177, 177, 177);
	public static final ColorUIResource BUTTON_BORDER_DISABLED = new ColorUIResource(200, 200, 200);
	public static final ColorUIResource BUTTON_BORDER_FOCUS = INPUT_BORDER_FOCUS;
	public static final ColorUIResource BUTTON_BORDER_DARK = INPUT_BORDER_DARK;
	public static final ColorUIResource BUTTON_BORDER_DARK_DISABLED = BUTTON_BORDER_DISABLED;
	public static final ColorUIResource BUTTON_BORDER_DARK_FOCUS = INPUT_BORDER_DARK_FOCUS;
	public static final ColorUIResource BUTTON_BACKGROUND_GRADIENT_START = new ColorUIResource(240, 240, 240);
	public static final ColorUIResource BUTTON_BACKGROUND_GRADIENT_END = new ColorUIResource(218, 218, 218);
	public static final ColorUIResource BUTTON_BACKGROUND_ROLLOVER_GRADIENT_START = new ColorUIResource(225, 225, 225);
	public static final ColorUIResource BUTTON_BACKGROUND_ROLLOVER_GRADIENT_END = new ColorUIResource(213, 213, 213);
	public static final ColorUIResource BUTTON_BACKGROUND_PRESSED_GRADIENT_START = new ColorUIResource(198, 198, 198);
	public static final ColorUIResource BUTTON_BACKGROUND_PRESSED_GRADIENT_END = new ColorUIResource(230, 230, 230);
	public static final ColorUIResource BUTTON_BACKGROUND_DISABLED_GRADIENT_START = new ColorUIResource(210, 210, 210);
	public static final ColorUIResource BUTTON_BACKGROUND_DISABLED_GRADIENT_END = new ColorUIResource(210, 210, 210);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_GRADIENT_START = new ColorUIResource(247, 120, 79);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_GRADIENT_END = new ColorUIResource(232, 86, 39);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_ROLLOVER_GRADIENT_START = new ColorUIResource(232, 105, 64);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_ROLLOVER_GRADIENT_END = new ColorUIResource(217, 90, 49);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_PRESSED_GRADIENT_START = new ColorUIResource(210, 83, 42);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_PRESSED_GRADIENT_END = new ColorUIResource(237, 110, 69);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_DISABLED_GRADIENT_START = BUTTON_BACKGROUND_DISABLED_GRADIENT_START;
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_DISABLED_GRADIENT_END = BUTTON_BACKGROUND_DISABLED_GRADIENT_END;
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_DARK_GRADIENT_START = new ColorUIResource(152, 169, 184);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_DARK_GRADIENT_END = new ColorUIResource(120, 134, 147);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_DARK_ROLLOVER_GRADIENT_START = new ColorUIResource(137, 154, 169);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_DARK_ROLLOVER_GRADIENT_END = new ColorUIResource(122, 139, 154);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_DARK_PRESSED_GRADIENT_START = new ColorUIResource(115, 132, 147);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_DARK_PRESSED_GRADIENT_END = new ColorUIResource(142, 159, 184);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_DARK_DISABLED_GRADIENT_START = BUTTON_BACKGROUND_DISABLED_GRADIENT_START;
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_DARK_DISABLED_GRADIENT_END = BUTTON_BACKGROUND_DISABLED_GRADIENT_END;
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_GRADIENT_START = new ColorUIResource(255, 255, 255);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_GRADIENT_END = new ColorUIResource(240, 240, 240);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_ROLLOVER_GRADIENT_START = new ColorUIResource(248, 248, 248);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_ROLLOVER_GRADIENT_END = new ColorUIResource(233, 233, 233);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_PRESSED_GRADIENT_START = new ColorUIResource(240, 240, 240);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_PRESSED_GRADIENT_END = new ColorUIResource(255, 255, 255);
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_DISABLED_GRADIENT_START = BUTTON_BACKGROUND_DISABLED_GRADIENT_START;
	public static final ColorUIResource BUTTON_BACKGROUND_HIGHLIGHTED_WHITE_DISABLED_GRADIENT_END = BUTTON_BACKGROUND_DISABLED_GRADIENT_END;

	public static final ColorUIResource RAPIDMINER_ORANGE = new ColorUIResource(241, 96, 34);
	public static final ColorUIResource RAPIDMINER_ORANGE_BRIGHT = new ColorUIResource(246, 117, 75);

	public static final ColorUIResource COMBOBOX_BACKGROUND = INPUT_BACKGROUND;
	public static final ColorUIResource COMBOBOX_BACKGROUND_DARK = INPUT_BACKGROUND_DARK;
	public static final ColorUIResource COMBOBOX_BACKGROUND_DISABLED = INPUT_BACKGROUND_DISABLED;
	public static final ColorUIResource COMBOBOX_ARROW = new ColorUIResource(75, 75, 75);
	public static final ColorUIResource COMBOBOX_ARROW_DISABLED = new ColorUIResource(175, 175, 175);
	public static final ColorUIResource COMBOBOX_BORDER = INPUT_BORDER;
	public static final ColorUIResource COMBOBOX_BORDER_DISABLED = INPUT_BORDER_DISABLED;
	public static final ColorUIResource COMBOBOX_BORDER_FOCUS = INPUT_BORDER_FOCUS;

	public static final ColorUIResource SPINNER_BACKGROUND = INPUT_BACKGROUND;
	public static final ColorUIResource SPINNER_ARROW = new ColorUIResource(150, 150, 150);
	public static final ColorUIResource SPINNER_BUTTON_ROLLOVER = new ColorUIResource(225, 225, 225);
	public static final ColorUIResource SPINNER_BUTTON_PRESSED = new ColorUIResource(205, 205, 205);
	public static final ColorUIResource SPINNER_BUTTON_BACKGROUND = new ColorUIResource(245, 245, 245);
	public static final ColorUIResource SPINNER_BUTTON_DISABLED = new ColorUIResource(240, 240, 240);
	public static final ColorUIResource SPINNER_BORDER = TEXTFIELD_BORDER;

	public static final ColorUIResource SLIDER_TRACK_BACKGROUND = INPUT_BACKGROUND;
	public static final ColorUIResource SLIDER_TRACK_BACKGROUND_DISABLED = INPUT_BACKGROUND_DISABLED;
	public static final ColorUIResource SLIDER_TRACK_FOREGROUND = SELECTION_BOX_FOREGROUND;
	public static final ColorUIResource SLIDER_TRACK_BORDER = TEXTFIELD_BORDER;
	public static final ColorUIResource SLIDER_THUMB_BACKGROUND = INPUT_BACKGROUND;
	public static final ColorUIResource SLIDER_THUMB_BORDER = TEXTFIELD_BORDER;
	public static final ColorUIResource SLIDER_THUMB_BORDER_FOCUS = TEXTFIELD_BORDER_FOCUS;

	public static final ColorUIResource CHECKBOX_BACKGROUND = INPUT_BACKGROUND;
	public static final ColorUIResource CHECKBOX_BACKGROUND_DISABLED = INPUT_BACKGROUND_DISABLED;
	public static final ColorUIResource CHECKBOX_BORDER = INPUT_BORDER;
	public static final ColorUIResource CHECKBOX_BORDER_DISABLED = INPUT_BORDER_DISABLED;
	public static final ColorUIResource CHECKBOX_BORDER_FOCUS = INPUT_BORDER_FOCUS;
	public static final ColorUIResource CHECKBOX_CHECKED = SELECTION_BOX_FOREGROUND;
	public static final ColorUIResource CHECKBOX_CHECKED_DISABLED = SELECTION_BOX_FOREGROUND_DISABLED;

	public static final ColorUIResource RADIOBUTTON_BACKGROUND = CHECKBOX_BACKGROUND;
	public static final ColorUIResource RADIOBUTTON_BACKGROUND_DISABLED = CHECKBOX_BACKGROUND_DISABLED;
	public static final ColorUIResource RADIOBUTTON_BORDER = CHECKBOX_BORDER;
	public static final ColorUIResource RADIOBUTTON_BORDER_DISABLED = CHECKBOX_BORDER_DISABLED;
	public static final ColorUIResource RADIOBUTTON_BORDER_FOCUS = CHECKBOX_BORDER_FOCUS;
	public static final ColorUIResource RADIOBUTTON_CHECKED = CHECKBOX_CHECKED;
	public static final ColorUIResource RADIOBUTTON_CHECKED_DISABLED = CHECKBOX_CHECKED_DISABLED;

	public static final ColorUIResource LINKBUTTON_LOCAL = new ColorUIResource(0, 0, 238);
	public static final ColorUIResource LINKBUTTON_REMOTE = LINKBUTTON_LOCAL;

	public static final ColorUIResource TOOLTIP_BACKGROUND = new ColorUIResource(252, 252, 252);
	public static final ColorUIResource TOOLTIP_FOREGROUND = new ColorUIResource(0, 0, 0);
	public static final ColorUIResource TOOLTIP_BORDER = new ColorUIResource(0, 0, 0);

	public static final ColorUIResource SPLITPANE_BORDER = INPUT_BORDER;
	public static final ColorUIResource SPLITPANE_BORDER_FOCUS = INPUT_BORDER_FOCUS;
	public static final ColorUIResource SPLITPANE_DOTS = new ColorUIResource(255, 255, 255);

	public static final ColorUIResource TAB_BORDER = new ColorUIResource(187, 187, 187);
	public static final ColorUIResource TAB_BACKGROUND = new ColorUIResource(213, 213, 213);
	public static final ColorUIResource TAB_BACKGROUND_HIGHLIGHT = new ColorUIResource(233, 233, 234);
	public static final ColorUIResource TAB_BACKGROUND_SELECTED = TAB_BACKGROUND_HIGHLIGHT;
	public static final ColorUIResource TAB_BACKGROUND_START = new ColorUIResource(243, 243, 243);
	public static final ColorUIResource TAB_BACKGROUND_START_SELECTED = new ColorUIResource(255, 255, 255);
	public static final ColorUIResource TAB_CONTENT_BORDER = TAB_BORDER;

	public static final ColorUIResource START_DIALOG_BACKGROUND = BUTTON_BACKGROUND_GRADIENT_END;
	public static final ColorUIResource START_DIALOG_ORANGE_FONT = new ColorUIResource(231, 84, 36);


	public static final ColorUIResource CARD_PANEL_BACKGROUND = PANEL_BACKGROUND;
	public static final ColorUIResource CARD_PANEL_BACKGROUND_HIGHLIGHT = new ColorUIResource(220, 220, 220);
	public static final ColorUIResource CARD_PANEL_BACKGROUND_SELECTED = new ColorUIResource(208, 209, 209);

	public static final ColorUIResource SCROLLBAR_TRACK_BACKGROUND = PANEL_BACKGROUND;
	public static final ColorUIResource SCROLLBAR_TRACK_BORDER = TAB_BORDER;
	public static final ColorUIResource SCROLLBAR_THUMB_BACKGROUND = new ColorUIResource(198, 199, 199);
	public static final ColorUIResource SCROLLBAR_THUMB_BACKGROUND_ROLLOVER = new ColorUIResource(188, 188, 186);
	public static final ColorUIResource SCROLLBAR_THUMB_BACKGROUND_PRESSED = new ColorUIResource(175, 175, 175);
	public static final ColorUIResource SCROLLBAR_THUMB_FOREGROUND = new ColorUIResource(151, 152, 152);
	public static final ColorUIResource SCROLLBAR_THUMB_BORDER = SCROLLBAR_TRACK_BORDER;
	public static final ColorUIResource SCROLLBAR_ARROW = new ColorUIResource(98, 98, 98);
	public static final ColorUIResource SCROLLBAR_ARROW_PRESSED = SCROLLBAR_THUMB_BACKGROUND_PRESSED;
	public static final ColorUIResource SCROLLBAR_ARROW_ROLLOVER = new ColorUIResource(120, 120, 120);
	public static final ColorUIResource SCROLLBAR_ARROW_BORDER = SCROLLBAR_TRACK_BORDER;
	public static final ColorUIResource SCROLLBAR_ARROW_BACKGROUND = SCROLLBAR_TRACK_BACKGROUND;

	public static final ColorUIResource TABLE_HEADER_BACKGROUND_GRADIENT_START = new ColorUIResource(231, 232, 232);
	public static final ColorUIResource TABLE_HEADER_BACKGROUND_GRADIENT_END = new ColorUIResource(225, 225, 225);
	public static final ColorUIResource TABLE_HEADER_BACKGROUND_FOCUS = new ColorUIResource(208, 208, 208);
	public static final ColorUIResource TABLE_HEADER_BACKGROUND_PRESSED = new ColorUIResource(193, 193, 193);
	public static final ColorUIResource TABLE_HEADER_BORDER = INPUT_BORDER;
	public static final ColorUIResource TABLE_CELL_BORDER = new ColorUIResource(228, 228, 228);
	public static final ColorUIResource TABLE_CELL_BORDER_HIGHLIGHT = new ColorUIResource(212, 212, 212);

	public static final ColorUIResource MENU_ITEM_SEPARATOR = new ColorUIResource(223, 224, 224);
	public static final ColorUIResource MENU_ITEM_BACKGROUND = new ColorUIResource(254, 254, 254);
	public static final ColorUIResource MENU_ITEM_BACKGROUND_SELECTED = TEXT_HIGHLIGHT_BACKGROUND;
	public static final ColorUIResource MENUBAR_BORDER = INPUT_BORDER;
	public static final ColorUIResource MENUBAR_BORDER_HIGHLIGHT = INPUT_BORDER_FOCUS;
	public static final ColorUIResource MENUBAR_BACKGROUND_HIGHLIGHT = PANEL_BACKGROUND;

	public static final ColorUIResource PROGRESSBAR_BACKGROUND = new ColorUIResource(247, 247, 247);
	public static final ColorUIResource PROGRESSBAR_DETERMINATE_FOREGROUND_GRADIENT_START = new ColorUIResource(255, 153, 2);
	public static final ColorUIResource PROGRESSBAR_DETERMINATE_FOREGROUND_GRADIENT_END = new ColorUIResource(245, 131, 23);
	public static final ColorUIResource PROGRESSBAR_INDETERMINATE_FOREGROUND_1 = new ColorUIResource(255, 153, 2);
	public static final ColorUIResource PROGRESSBAR_INDETERMINATE_FOREGROUND_2 = new ColorUIResource(255, 181, 58);
	public static final ColorUIResource PROGRESSBAR_BORDER = new ColorUIResource(221, 221, 221);

	public static final ColorUIResource MULTI_STEP_PROGRESSBAR_NEUTRAL = new ColorUIResource(60, 60, 60);
	public static final ColorUIResource MULTI_STEP_PROGRESSBAR_NEUTRAL_LIGHT = new ColorUIResource(200, 200, 200);

	public static final ColorUIResource WARNING_COLOR = new ColorUIResource(255, 230, 152);

	public static final ColorUIResource WHITE = new ColorUIResource(255, 255, 255);
	public static final ColorUIResource BLACK = new ColorUIResource(0, 0, 0);

	private ColorUIResource[] tabbedPaneColors = new ColorUIResource[]{new ColorUIResource(200, 205, 210),
			new ColorUIResource(215, 220, 225), new ColorUIResource(170, 170, 190), new ColorUIResource(200, 200, 220),
			new ColorUIResource(190, 200, 220), new ColorUIResource(250, 250, 250), new ColorUIResource(255, 255, 255),
			new ColorUIResource(210, 210, 230), new ColorUIResource(180, 190, 210), new ColorUIResource(200, 200, 220),
			new ColorUIResource(210, 210, 230), new ColorUIResource(220, 220, 240), new ColorUIResource(230, 230, 250),
			new ColorUIResource(235, 235, 255), new ColorUIResource(240, 240, 255), new ColorUIResource(245, 245, 255),
			new ColorUIResource(250, 250, 255), new ColorUIResource(255, 255, 255), new ColorUIResource(255, 255, 255),
			new ColorUIResource(210, 210, 230), new ColorUIResource(240, 240, 255), new ColorUIResource(245, 145, 0),};

	private ColorUIResource[] fileChooserColors = new ColorUIResource[]{new ColorUIResource(255, 200, 200),
			new ColorUIResource(230, 170, 170)};

	private ColorUIResource desktopBackgroundColor = new ColorUIResource(180, 195, 220);

	public void addCustomEntriesToTable(UIDefaults table) {
		Object[] values = new Object[]{"ToolTip.background", TOOLTIP_BACKGROUND, "ToolTip.foreground", TOOLTIP_FOREGROUND,
				"ToolTip.borderColor", TOOLTIP_BORDER};
		table.putDefaults(values);
	}

	public ColorUIResource getCommonBackground() {
		return PANEL_BACKGROUND;
	}

	public ColorUIResource[] getTabbedPaneColors() {
		return this.tabbedPaneColors;
	}

	public ColorUIResource[] getFileChooserColors() {
		return this.fileChooserColors;
	}

	public ColorUIResource getDesktopBackgroundColor() {
		return this.desktopBackgroundColor;
	}
}
