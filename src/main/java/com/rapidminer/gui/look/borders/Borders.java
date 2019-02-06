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
package com.rapidminer.gui.look.borders;

import javax.swing.border.Border;


/**
 * The border creation and maintaining class used for all components. This class creates all borders
 * once and use these singletons for painting. Therefore, this class mainly serves as access point
 * for the border singletons.
 *
 * @author Ingo Mierswa
 */
public class Borders {

	public static Border TOOL_TIP_BORDER = new ToolTipBorder();
	public static Border DUMMY_BORDER = new DummyBorder();
	public static Border POPUP_BORDER = new PopupBorder();
	public static Border SHADOWED_POPUP_MENU_BORDER = new ShadowedPopupMenuBorder();
	public static Border TABLE_HEADER_BORDER = new TableHeaderBorder();
	public static Border SPLIT_PANE_BORDER = new SplitPaneBorder();
	public static Border INTERNAL_FRAME_BORDER = new InternalFrameBorder();
	public static Border COMBO_BOX_BORDER = new ComboBoxBorder();
	public static Border TOOL_BAR_BORDER = new ToolBarBorder();
	public static Border PROGRESS_BAR_BORDER = new ProgressBarBorder();
	public static Border EMPTY_BORDER = new EmptyBorder();
	public static Border EMPTY_BUTTON_BORDER = new EmptyButtonBorder();
	public static Border POPUP_MENU_BORDER = new PopupMenuBorder();
	public static Border TEXT_FIELD_BORDER = new TextFieldBorder();
	public static Border SCROLL_PANE_BORDER = new ScrollPaneBorder();
	public static Border SPINNER_BORDER = new SpinnerBorder();
	public static Border EMPTY_COMBO_BOX_BORDER = new EmptyComboBoxBorder();
	public static Border CHECK_BOX_BORDER = new CheckBoxBorder();
	public static Border COMBO_BOX_LIST_CELL_RENDERER_FOCUS_BORDER = new ComboBoxListCellRendererFocusBorder();
	public static Border MENU_BAR_BORDER = new MenuBarBorder();

	public static Border getCheckBoxBorder() {
		return CHECK_BOX_BORDER;
	}

	public static Border getToolTipBorder() {
		return TOOL_TIP_BORDER;
	}

	public static Border getDummyBorder() {
		return DUMMY_BORDER;
	}

	public static Border getPopupBorder() {
		return POPUP_BORDER;
	}

	public static Border getShadowedPopupMenuBorder() {
		return SHADOWED_POPUP_MENU_BORDER;
	}

	public static Border getTableHeaderBorder() {
		return TABLE_HEADER_BORDER;
	}

	public static Border getSplitPaneBorder() {
		return SPLIT_PANE_BORDER;
	}

	public static Border getInternalFrameBorder() {
		return INTERNAL_FRAME_BORDER;
	}

	public static Border getMenuBarBorder() {
		return MENU_BAR_BORDER;
	}

	public static Border getComboBoxBorder() {
		return COMBO_BOX_BORDER;
	}

	public static Border getToolBarBorder() {
		return TOOL_BAR_BORDER;
	}

	public static Border getProgressBarBorder() {
		return PROGRESS_BAR_BORDER;
	}

	public static Border getEmptyBorder() {
		return EMPTY_BORDER;
	}

	public static Border getEmptyButtonBorder() {
		return EMPTY_BUTTON_BORDER;
	}

	public static Border getPopupMenuBorder() {
		return POPUP_MENU_BORDER;
	}

	public static Border getTextFieldBorder() {
		return TEXT_FIELD_BORDER;
	}

	public static Border getScrollPaneBorder() {
		return SCROLL_PANE_BORDER;
	}

	public static Border getSpinnerBorder() {
		return SPINNER_BORDER;
	}

	public static Border getComboBoxListCellRendererFocusBorder() {
		return COMBO_BOX_LIST_CELL_RENDERER_FOCUS_BORDER;
	}

	public static Border getEmptyComboBoxBorder() {
		return EMPTY_COMBO_BOX_BORDER;
	}
}
