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

import java.awt.Color;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.JTextField;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.BorderUIResource.LineBorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.text.DefaultEditorKit;

import com.rapidminer.gui.look.borders.Borders;
import com.rapidminer.gui.look.icons.IconFactory;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;


/**
 * The main look and feel class.
 *
 * @author Ingo Mierswa
 */
public class RapidLookAndFeel extends BasicLookAndFeel {

	private static final long serialVersionUID = 1616331528047010458L;

	/** the control key is not used on Mac for the same things it is used on Windows/Linux */
	private static final String CONTROL_ID = SystemInfoUtilities.getOperatingSystem() != OperatingSystem.OSX ? "control"
			: "meta";

	private final static Colors COLORS = new Colors();

	private Map<String, Object> customUIDefaults = new HashMap<>();

	public static Colors getColors() {
		return COLORS;
	}

	/**
	 * Default empty constructor.
	 */
	public RapidLookAndFeel() {}

	/**
	 * OS X Constructor
	 */
	public RapidLookAndFeel(Map<String, Object> customUIDefaults) {
		this.customUIDefaults = customUIDefaults;
	}

	@Override
	public void initialize() {
		super.initialize();
		RoundedPopupFactory.install();
	}

	@Override
	public void uninitialize() {
		super.uninitialize();
		RoundedPopupFactory.uninstall();
	}

	@Override
	public UIDefaults getDefaults() {
		getColors();

		UIDefaults table = new UIDefaults();
		// copy existing default values over
		// enables AntiAliasing if AntiAliasing is enabled in the OS
		// EXCEPT for key "Menu.opaque" which will glitch out JMenues
		UIDefaults lookAndFeelDefaults = UIManager.getLookAndFeelDefaults();
		Hashtable copy = new Hashtable<>(lookAndFeelDefaults);
		for (Object key : copy.keySet()) {
			if (!String.valueOf(key).equals("Menu.opaque")) {
				table.put(key, lookAndFeelDefaults.get(key));
			}
		}

		initClassDefaults(table);
		initSystemColorDefaults(table);
		initComponentDefaults(table);
		COLORS.addCustomEntriesToTable(table);

		return table;
	}

	@Override
	public boolean isNativeLookAndFeel() {
		return false;
	}

	@Override
	public boolean isSupportedLookAndFeel() {
		return true;
	}

	@Override
	public String getDescription() {
		return "RapidLook Look And Feel";
	}

	@Override
	public String getID() {
		return "RapidLook";
	}

	@Override
	public String getName() {
		return "RapidLook Look And Feel";
	}

	@Override
	protected void initSystemColorDefaults(UIDefaults table) {
		Object[] systemColors = { "desktop", getDesktopColor(), /* Color of the desktop background */
				"activeCaption", getWindowTitleBackground(),
				/* Color for captions (title bars) when they are active. */
				"activeCaptionText", getWindowTitleForeground(),
				/* Text color for text in captions (title bars). */
				"activeCaptionBorder", getPrimaryControlShadow(),
				/* Border color for caption (title bar) window borders. */
				"inactiveCaption", getWindowTitleBackground(),
				/* Color for captions (title bars) when not active. */
				"inactiveCaptionText", getWindowTitleForeground(),
				/* Text color for text in inactive captions (title bars). */
				"inactiveCaptionBorder",
				getControlShadow(),
				/* Border color for inactive caption (title bar) window borders. */
				"window",
				getColors().getCommonBackground(), // getWindowBackground(), /* Default color for
				// the interior of windows */

				"windowBorder", getColors().getCommonBackground(), /* ??? */
				"windowText", getUserTextColor(), /* ??? */
				"menu", getMenuBackground(), /* Background color for menus */
				"menuText", getMenuForeground(), /* Text color for menus */
				"text", getTextBackground(), "textText", getUserTextColor(), "textHighlight", getTextHighlightColor(),
				"textHighlightText", getHighlightedTextColor(), "textInactiveText", getInactiveSystemTextColor(), "control",
				getColors().getCommonBackground(), "controlText", getControlTextColor(), /*
				 * Default
				 * color for
				 * text in
				 * controls
				 */
				"controlHighlight", getControlHighlight(),
				/* Specular highlight (opposite of the shadow) */
				"controlLtHighlight", getControlHighlight(), /* Highlight color for controls */
				"controlShadow", getControlShadow(), /* Shadow color for controls */
				"controlDkShadow", getControlDarkShadow(), /* Dark shadow color for controls */
				"scrollbar", getColors().getCommonBackground(),
				/* Scrollbar background (usually the "track") */
				"info", new ColorUIResource(255, 240, 195), /* ToolTip Background */
				// "infoText", /* ToolTip Text */
		};

		for (int i = 0; i < systemColors.length; i += 2) {
			table.put(systemColors[i], systemColors[i + 1]);
		}
	}

	@Override
	protected void initComponentDefaults(UIDefaults table) {
		super.initComponentDefaults(table);

		Object fieldInputMap = new UIDefaults.LazyInputMap(new Object[] { CONTROL_ID + " C", DefaultEditorKit.copyAction,
				CONTROL_ID + " V", DefaultEditorKit.pasteAction, CONTROL_ID + " X", DefaultEditorKit.cutAction, "COPY",
				DefaultEditorKit.copyAction, "PASTE", DefaultEditorKit.pasteAction, "CUT", DefaultEditorKit.cutAction,
				"shift LEFT", DefaultEditorKit.selectionBackwardAction, "shift KP_LEFT",
				DefaultEditorKit.selectionBackwardAction, "shift RIGHT", DefaultEditorKit.selectionForwardAction,
				"shift KP_RIGHT", DefaultEditorKit.selectionForwardAction, CONTROL_ID + " LEFT",
				DefaultEditorKit.previousWordAction, CONTROL_ID + " KP_LEFT", DefaultEditorKit.previousWordAction,
				CONTROL_ID + " RIGHT", DefaultEditorKit.nextWordAction, CONTROL_ID + " KP_RIGHT",
				DefaultEditorKit.nextWordAction, CONTROL_ID + " shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
				CONTROL_ID + " shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction, CONTROL_ID + " shift RIGHT",
				DefaultEditorKit.selectionNextWordAction, CONTROL_ID + " shift KP_RIGHT",
				DefaultEditorKit.selectionNextWordAction, CONTROL_ID + " A", DefaultEditorKit.selectAllAction, "HOME",
				DefaultEditorKit.beginLineAction, "END", DefaultEditorKit.endLineAction, "shift HOME",
				DefaultEditorKit.selectionBeginLineAction, "shift END", DefaultEditorKit.selectionEndLineAction,
				"BACK_SPACE", DefaultEditorKit.deletePrevCharAction, CONTROL_ID + " BACK_SPACE",
				DefaultEditorKit.deletePrevWordAction, "DELETE", DefaultEditorKit.deleteNextCharAction,
				CONTROL_ID + " DELETE", DefaultEditorKit.deleteNextWordAction, "RIGHT", DefaultEditorKit.forwardAction,
				"LEFT", DefaultEditorKit.backwardAction, "KP_RIGHT", DefaultEditorKit.forwardAction, "KP_LEFT",
				DefaultEditorKit.backwardAction, "ENTER", JTextField.notifyAction, CONTROL_ID + " BACK_SLASH", "unselect",
				CONTROL_ID + " shift O", "toggle-componentOrientation" });

		Object multilineInputMap = new UIDefaults.LazyInputMap(new Object[] { CONTROL_ID + " C",
				DefaultEditorKit.copyAction, CONTROL_ID + " V", DefaultEditorKit.pasteAction, CONTROL_ID + " X",
				DefaultEditorKit.cutAction, "COPY", DefaultEditorKit.copyAction, "PASTE", DefaultEditorKit.pasteAction,
				"CUT", DefaultEditorKit.cutAction, "shift LEFT", DefaultEditorKit.selectionBackwardAction, "shift KP_LEFT",
				DefaultEditorKit.selectionBackwardAction, "shift RIGHT", DefaultEditorKit.selectionForwardAction,
				"shift KP_RIGHT", DefaultEditorKit.selectionForwardAction, CONTROL_ID + " LEFT",
				DefaultEditorKit.previousWordAction, CONTROL_ID + " KP_LEFT", DefaultEditorKit.previousWordAction,
				CONTROL_ID + " RIGHT", DefaultEditorKit.nextWordAction, CONTROL_ID + " KP_RIGHT",
				DefaultEditorKit.nextWordAction, CONTROL_ID + " shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
				CONTROL_ID + " shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction, CONTROL_ID + " shift RIGHT",
				DefaultEditorKit.selectionNextWordAction, CONTROL_ID + " shift KP_RIGHT",
				DefaultEditorKit.selectionNextWordAction, CONTROL_ID + " A", DefaultEditorKit.selectAllAction, "HOME",
				DefaultEditorKit.beginLineAction, "END", DefaultEditorKit.endLineAction, "shift HOME",
				DefaultEditorKit.selectionBeginLineAction, "shift END", DefaultEditorKit.selectionEndLineAction, "UP",
				DefaultEditorKit.upAction, "KP_UP", DefaultEditorKit.upAction, "DOWN", DefaultEditorKit.downAction,
				"KP_DOWN", DefaultEditorKit.downAction, "PAGE_UP", DefaultEditorKit.pageUpAction, "PAGE_DOWN",
				DefaultEditorKit.pageDownAction, "shift PAGE_UP", "selection-page-up", "shift PAGE_DOWN",
				"selection-page-down", CONTROL_ID + " shift PAGE_UP", "selection-page-left",
				CONTROL_ID + " shift PAGE_DOWN", "selection-page-right", "shift UP", DefaultEditorKit.selectionUpAction,
				"shift KP_UP", DefaultEditorKit.selectionUpAction, "shift DOWN", DefaultEditorKit.selectionDownAction,
				"shift KP_DOWN", DefaultEditorKit.selectionDownAction, "ENTER", DefaultEditorKit.insertBreakAction,
				"BACK_SPACE", DefaultEditorKit.deletePrevCharAction, CONTROL_ID + " BACK_SPACE",
				DefaultEditorKit.deletePrevWordAction, "DELETE", DefaultEditorKit.deleteNextCharAction,
				CONTROL_ID + " DELETE", DefaultEditorKit.deleteNextWordAction, "RIGHT", DefaultEditorKit.forwardAction,
				"LEFT", DefaultEditorKit.backwardAction, "KP_RIGHT", DefaultEditorKit.forwardAction, "KP_LEFT",
				DefaultEditorKit.backwardAction, "TAB", DefaultEditorKit.insertTabAction, CONTROL_ID + " BACK_SLASH",
				"unselect", CONTROL_ID + " HOME", DefaultEditorKit.beginAction, CONTROL_ID + " END",
				DefaultEditorKit.endAction, CONTROL_ID + " shift HOME", DefaultEditorKit.selectionBeginAction,
				CONTROL_ID + " shift END", DefaultEditorKit.selectionEndAction, CONTROL_ID + " T", "next-link-action",
				CONTROL_ID + " shift T", "previous-link-action", CONTROL_ID + " SPACE", "activate-link-action",
				CONTROL_ID + " shift O", "toggle-componentOrientation" });

		Object toolTipBorder = new UIDefaults.ProxyLazyValue("javax.swing.plaf.BorderUIResource$LineBorderUIResource",
				new Object[] { new ColorUIResource(100, 100, 100) });

		Object focusCellHighlightBorder = new UIDefaults.ProxyLazyValue(
				"javax.swing.plaf.BorderUIResource$LineBorderUIResource", new Object[] { COLORS.getTextHighlightBackColor()
						.darker() });

		Object fontDialog12 = new UIDefaults.ProxyLazyValue("javax.swing.plaf.FontUIResource", null, new Object[] {
				"Dialog", Integer.valueOf(0), Integer.valueOf(12) });

		ColorUIResource caretColor = new ColorUIResource(0, 25, 100);

		Object textFieldMargin = new InsetsUIResource(4, 6, 4, 6);
		Object tabbedpaneTabInsets = new InsetsUIResource(3, 10, 0, 10);

		Object defaultDirectoryIcon = SwingTools.createImage("plaf/folder.png");

		Integer zero = Integer.valueOf(0);
		Integer one = Integer.valueOf(1);

		Object sliderFocusInsets = new InsetsUIResource(2, 2, 2, 2);

		Object[] defaults = {
				// Button
				"Button.font",
				fontDialog12,
				"Button.background",
				COLORS.getCommonBackground(),
				"Button.foreground",
				COLORS.getCommonForeground(),
				"Button.margin",
				new InsetsUIResource(10, 20, 10, 20),
				"Button.textShiftOffset",
				one,
				"Button.focusInputMap",
				new UIDefaults.LazyInputMap(new Object[] { "SPACE", "pressed", "released SPACE", "released", "ENTER",
						"pressed", "released ENTER", "released" }),
						// ToggleButton
						"ToggleButton.font",
						fontDialog12,
						"ToggleButton.background",
						COLORS.getCommonBackground(),
						"ToggleButton.foreground",
						COLORS.getCommonForeground(),
						"ToggleButton.textShiftOffset",
						one,
						"ToggleButton.margin",
						textFieldMargin,
						"Button.focusInputMap",
						new UIDefaults.LazyInputMap(new Object[] { "SPACE", "pressed", "released SPACE", "released" }),
						// RadioButton
						"RadioButton.font",
						fontDialog12,
						"RadioButton.background",
						COLORS.getCommonBackground(),
						"RadioButton.foreground",
						COLORS.getCommonForeground(),
						"RadioButton.textShiftOffset",
						zero,
						"RadioButton.focusInputMap",
						new UIDefaults.LazyInputMap(new Object[] { "SPACE", "pressed", "released SPACE", "released", "RETURN",
						"pressed" }),
						// CheckBox
						"CheckBox.font",
						fontDialog12,
						"CheckBox.background",
						COLORS.getCommonBackground(),
						"CheckBox.foreground",
						COLORS.getCommonForeground(),
						"CheckBox.textShiftOffset",
						zero,
						"CheckBox.focusInputMap",
						new UIDefaults.LazyInputMap(new Object[] { "SPACE", "pressed", "released SPACE", "released", "RETURN",
						"pressed" }),
						// TextField
						"TextField.selectionBackground",
						COLORS.getTextHighlightBackColor(),
						// ComboBox
						"ComboBox.font",
						fontDialog12,
						"ComboBox.selectionBackground",
						COLORS.getTextHighlightBackColor(),
						"ComboBox.selectionForeground",
						getHighlightedTextColor(),
						"ComboBox.background",
						COLORS.getCommonBackground(),
						"ComboBox.foreground",
						COLORS.getCommonForeground(),
						"ComboBox.textShiftOffset",
						one,
						"ComboBox.disabledForeground",
						getInactiveSystemTextColor(),
						"ComboBox.ancestorInputMap",
						new UIDefaults.LazyInputMap(new Object[] { "ESCAPE", "hidePopup", "PAGE_UP", "pageUpPassThrough",
								"PAGE_DOWN", "pageDownPassThrough", "HOME", "homePassThrough", "END", "endPassThrough", "DOWN",
								"selectNext", "KP_DOWN", "selectNext", "alt DOWN", "togglePopup", "alt KP_DOWN", "togglePopup",
								"alt UP", "togglePopup", "alt KP_UP", "togglePopup", "SPACE", "spacePopup", "ENTER", "enterPressed",
								"UP", "selectPrevious", "KP_UP", "selectPrevious" }),
								// Filechooser
								"FileChooser.defaultDirectoryIcon",
								defaultDirectoryIcon,
								"FileChooser.newFolderIcon",
								null,
								"FileChooser.upFolderIcon",
								null,
								"FileChooser.homeFolderIcon",
								null,
								"FileChooser.detailsViewIcon",
								null,
								"FileChooser.listViewIcon",
								null,
								"FileView.directoryIcon",
								null,
								"FileView.fileIcon",
								null,
								"FileView.computerIcon",
								null,
								"FileView.hardDriveIcon",
								null,
								"FileView.floppyDriveIcon",
								null,
								"FileChooser.ChangeViewNormalIcon",
								SwingTools.createImage("plaf/fc/fc_change_1.png"),
								"FileChooser.ChangeViewHighlightedIcon",
								SwingTools.createImage("plaf/fc/fc_change_2.png"),
								"FileChooser.BookmarksAddNormalIcon",
								SwingTools.createImage("plaf/fc/fc_bookmark_1.png"),
								"FileChooser.BookmarksAddHighlightedIcon",
								SwingTools.createImage("plaf/fc/fc_bookmark_2.png"),
								"FileChooser.HomeNormalIcon",
								SwingTools.createImage("plaf/fc/fc_home_1.png"),
								"FileChooser.HomeHighlightedIcon",
								SwingTools.createImage("plaf/fc/fc_home_2.png"),
								"FileChooser.HomeDisabledIcon",
								SwingTools.createImage("plaf/fc/fc_home_3.png"),
								"FileChooser.NewFolderNormalIcon",
								SwingTools.createImage("plaf/fc/fc_new_1.png"),
								"FileChooser.NewFolderHighlightedIcon",
								SwingTools.createImage("plaf/fc/fc_new_2.png"),
								"FileChooser.NewFolderDisabledIcon",
								SwingTools.createImage("plaf/fc/fc_new_3.png"),
								"FileChooser.UpFolderNormalIcon",
								SwingTools.createImage("plaf/fc/fc_up_1.png"),
								"FileChooser.UpFolderHighlightedIcon",
								SwingTools.createImage("plaf/fc/fc_up_2.png"),
								"FileChooser.UpFolderDisabledIcon",
								SwingTools.createImage("plaf/fc/fc_up_3.png"),
								"FileChooser.BackFolderNormalIcon",
								SwingTools.createImage("plaf/fc/fc_back1.png"),
								"FileChooser.BackFolderHighlightedIcon",
								SwingTools.createImage("plaf/fc/fc_back2.png"),
								"FileChooser.BackFolderDisabledIcon",
								SwingTools.createImage("plaf/fc/fc_back3.png"),
								"FileChooser.ancestorInputMap",
								new UIDefaults.LazyInputMap(new Object[] { "ESCAPE", "cancelSelection", "BACK_SPACE", "Go Up", "ENTER",
								"approveSelection" }),
								// InternalFrame
								"InternalFrame.titleFont",
								new FontUIResource("Dialog", 1, 12),
								"InternalFrame.activeTitleForeground",
								Colors.getBlack(),
								"InternalFrame.inactiveTitleForeground",
								Colors.getBlack(),
								"InternalFrame.closeIcon",
								SwingTools.createImage("plaf/close_icon.png"),
								"InternalFrame.rolloverCloseIcon",
								SwingTools.createImage("plaf/armed_close_icon.png"),
								"InternalFrame.maximizeIcon",
								SwingTools.createImage("plaf/maximize_icon.png"),
								"InternalFrame.rolloverMaximizeIcon",
								SwingTools.createImage("plaf/armed_maximize_icon.png"),
								"InternalFrame.minimizeIcon",
								SwingTools.createImage("plaf/minimize_icon.png"),
								"InternalFrame.rolloverMinimizeIcon",
								SwingTools.createImage("plaf/armed_minimize_icon.png"),
								"InternalFrame.iconifyIcon",
								SwingTools.createImage("plaf/iconify_icon.png"),
								"InternalFrame.rolloverIconifyIcon",
								SwingTools.createImage("plaf/armed_iconify_icon.png"),
								"InternalFrame.icon",
								SwingTools.createImage("plaf/internal_icon.png"),
								// DesktopIcon
								"DesktopIcon.foreground",
								getControlTextColor(),
								"DesktopIcon.width",
								new Integer(140),
								"DesktopIcon.font",
								getMainFont(),
								// Desktop
								"Desktop.background",
								COLORS.getDesktopBackgroundColor(),
								"Desktop.ancestorInputMap",
								new UIDefaults.LazyInputMap(new Object[] { CONTROL_ID + " F5", "restore", CONTROL_ID + " F4", "close",
										CONTROL_ID + " F7", "move", CONTROL_ID + " F8", "resize", "RIGHT", "right", "KP_RIGHT", "right",
										"shift RIGHT", "shrinkRight", "shift KP_RIGHT", "shrinkRight", "LEFT", "left", "KP_LEFT", "left",
										"shift LEFT", "shrinkLeft", "shift KP_LEFT", "shrinkLeft", "UP", "up", "KP_UP", "up", "shift UP",
										"shrinkUp", "shift KP_UP", "shrinkUp", "DOWN", "down", "KP_DOWN", "down", "shift DOWN",
										"shrinkDown", "shift KP_DOWN", "shrinkDown", "ESCAPE", "escape", CONTROL_ID + " F9", "minimize",
										CONTROL_ID + " F10", "maximize", CONTROL_ID + " F6", "selectNextFrame", CONTROL_ID + " TAB",
										"selectNextFrame", CONTROL_ID + " alt F6", "selectNextFrame", "shift ctrl alt F6",
										"selectPreviousFrame", CONTROL_ID + " F12", "navigateNext", "shift ctrl F12", "navigatePrevious" }),
										// Label
										"Label.font",
										fontDialog12,
										"Label.foreground",
										getSystemTextColor(),
										"Label.disabledForeground",
										getInactiveSystemTextColor(),
										"Label.background",
										COLORS.getCommonBackground(),
										// List
										"List.focusCellHighlightBorder",
										focusCellHighlightBorder,
										"List.font",
										fontDialog12,
										"List.background",
										Colors.getWhite(),
										"List.selectionBackground",
										COLORS.getTextHighlightBackColor(),
										"List.focusInputMap",
										new UIDefaults.LazyInputMap(new Object[] { CONTROL_ID + " C", "copy", CONTROL_ID + " V", "paste",
												CONTROL_ID + " X", "cut", "COPY", "copy", "PASTE", "paste", "CUT", "cut", "UP", "selectPreviousRow",
												"KP_UP", "selectPreviousRow", "shift UP", "selectPreviousRowExtendSelection", "shift KP_UP",
												"selectPreviousRowExtendSelection", "DOWN", "selectNextRow", "KP_DOWN", "selectNextRow",
												"shift DOWN", "selectNextRowExtendSelection", "shift KP_DOWN", "selectNextRowExtendSelection",
												"LEFT", "selectPreviousColumn", "KP_LEFT", "selectPreviousColumn", "shift LEFT",
												"selectPreviousColumnExtendSelection", "shift KP_LEFT", "selectPreviousColumnExtendSelection",
												"RIGHT", "selectNextColumn", "KP_RIGHT", "selectNextColumn", "shift RIGHT",
												"selectNextColumnExtendSelection", "shift KP_RIGHT", "selectNextColumnExtendSelection", "HOME",
												"selectFirstRow", "shift HOME", "selectFirstRowExtendSelection", "END", "selectLastRow",
												"shift END", "selectLastRowExtendSelection", "PAGE_UP", "scrollUp", "shift PAGE_UP",
												"scrollUpExtendSelection", "PAGE_DOWN", "scrollDown", "shift PAGE_DOWN",
												"scrollDownExtendSelection", CONTROL_ID + " A", "selectAll", CONTROL_ID + " SLASH", "selectAll",
												CONTROL_ID + " BACK_SLASH", "clearSelection" }),
												// MenuBar
												"MenuBar.font",
												fontDialog12,
												"MenuBar.windowBindings",
												new Object[] { "F10", "takeFocus" }

				,
				// Menu Item
				"MenuItem.font",
				fontDialog12,
				"MenuItem.acceleratorFont",
				fontDialog12,
				"MenuItem.selectionBackground",
				getMenuSelectedBack(),
				// RadioButtonMenuItem
				"RadioButtonMenuItem.font",
				fontDialog12,
				"RadioButtonMenuItem.selectionBackground",
				getMenuSelectedBack(),
				"RadioButtonMenuItem.checkIcon",
				new UIDefaults.ProxyLazyValue("com.rapidminer.gui.look.icons.IconFactory", "getRadioButtonMenuItemIcon"),
				// CheckBoxMenuItem
				"CheckBoxMenuItem.font",
				fontDialog12,
				"CheckBoxMenuItem.selectionBackground",
				getMenuSelectedBack(),
				"CheckBoxMenuItem.checkIcon",
				new UIDefaults.ProxyLazyValue("com.rapidminer.gui.look.icons.IconFactory", "getCheckBoxMenuItemIcon"),
				// Menus
				"Menu.font",
				fontDialog12,
				"Menu.selectionBackground",
				getMenuSelectedBack(),
				// PopupMenu
				"PopupMenu.font",
				fontDialog12,
				// OptionPane
				"OptionPane.font",
				fontDialog12,
				"OptionPane.informationIcon",
				SwingTools.createImage("plaf/information.png"),
				"OptionPane.errorIcon",
				SwingTools.createImage("plaf/error.png"),
				"OptionPane.questionIcon",
				SwingTools.createImage("plaf/question.png"),
				"OptionPane.warningIcon",
				SwingTools.createImage("plaf/warning.png"),
				// Panel
				"Panel.font",
				fontDialog12,
				// ProggressBar
				"ProgressBar.foreground",
				new ColorUIResource(40, 40, 40),
				"ProgressBar.background",
				new ColorUIResource(240, 240, 240),
				"ProgressBar.font",
				new java.awt.Font("Dialog", 0, 11),
				"ProgressBar.cycleTime",
				new Integer(12000),
				// ScrollBar
				"ScrollBar.border",
				null,
				"ScrollBar.background",
				getColors().getCommonBackground(),
				"ScrollBar.focusInputMap",
				new UIDefaults.LazyInputMap(new Object[] { "RIGHT", "positiveUnitIncrement", "KP_RIGHT",
						"positiveUnitIncrement", "DOWN", "positiveUnitIncrement", "KP_DOWN", "positiveUnitIncrement",
						"PAGE_DOWN", "positiveBlockIncrement", "LEFT", "negativeUnitIncrement", "KP_LEFT",
						"negativeUnitIncrement", "UP", "negativeUnitIncrement", "KP_UP", "negativeUnitIncrement", "PAGE_UP",
						"negativeBlockIncrement", "HOME", "minScroll", "END", "maxScroll" }),
						// ScrollPane
						"ScrollPane.font",
						fontDialog12,
						"ScrollPane.ancestorInputMap",
						new UIDefaults.LazyInputMap(new Object[] { "RIGHT", "unitScrollRight", "KP_RIGHT", "unitScrollRight",
								"DOWN", "unitScrollDown", "KP_DOWN", "unitScrollDown", "LEFT", "unitScrollLeft", "KP_LEFT",
								"unitScrollLeft", "UP", "unitScrollUp", "KP_UP", "unitScrollUp", "PAGE_UP", "scrollUp", "PAGE_DOWN",
								"scrollDown", CONTROL_ID + " PAGE_UP", "scrollLeft", CONTROL_ID + " PAGE_DOWN", "scrollRight",
								CONTROL_ID + " HOME", "scrollHome", CONTROL_ID + " END", "scrollEnd" }),
								// ViewPort
								"Viewport.font",
								fontDialog12,
								// Slider
								"Slider.focusInsets",
								sliderFocusInsets,
								"Slider.disabledForeground",
								getInactiveSystemTextColor(),
								"Slider.majorColor",
								new ColorUIResource(100, 100, 100),
								"Slider.minorColor",
								new ColorUIResource(180, 180, 180),
								"Slider.majorDisabledColor",
								new ColorUIResource(180, 180, 180),
								"Slider.minorDisabledColor",
								new ColorUIResource(220, 220, 220),
								"Slider.trackWidth",
								new Integer(7),
								"Slider.majorTickLength",
								new Integer(6),
								"Slider.focusInputMap",
								new UIDefaults.LazyInputMap(new Object[] { "RIGHT", "positiveUnitIncrement", "KP_RIGHT",
										"positiveUnitIncrement", "DOWN", "negativeUnitIncrement", "KP_DOWN", "negativeUnitIncrement",
										"PAGE_DOWN", "negativeBlockIncrement", CONTROL_ID + " PAGE_DOWN", "negativeBlockIncrement", "LEFT",
										"negativeUnitIncrement", "KP_LEFT", "negativeUnitIncrement", "UP", "positiveUnitIncrement", "KP_UP",
										"positiveUnitIncrement", "PAGE_UP", "positiveBlockIncrement", CONTROL_ID + " PAGE_UP",
										"positiveBlockIncrement", "HOME", "minScroll", "END", "maxScroll" }),
										// Spinner
										"Spinner.font",
										fontDialog12,
										"Spinner.background",
										COLORS.getCommonBackground(),
										"Spinner.margin",
										new InsetsUIResource(10, 10, 10, 10),
										"Spinner.ancestorInputMap",
										new UIDefaults.LazyInputMap(new Object[] { "UP", "increment", "KP_UP", "increment", "DOWN", "decrement",
												"KP_DOWN", "decrement", }),
												// SplitPane
												"SplitPane.dividerSize",
												Integer.valueOf(9),
												"SplitPane.highlight",
												new ColorUIResource(250, 250, 250),
												"SplitPane.shadow",
												new ColorUIResource(200, 200, 200),
												"SplitPane.ancestorInputMap",
												new UIDefaults.LazyInputMap(new Object[] { "UP", "negativeIncrement", "DOWN", "positiveIncrement", "LEFT",
														"negativeIncrement", "RIGHT", "positiveIncrement", "KP_UP", "negativeIncrement", "KP_DOWN",
														"positiveIncrement", "KP_LEFT", "negativeIncrement", "KP_RIGHT", "positiveIncrement", "HOME",
														"selectMin", "END", "selectMax", "F8", "startResize", "F6", "toggleFocus", CONTROL_ID + " TAB",
														"focusOutForward", CONTROL_ID + " shift TAB", "focusOutBackward" }),

														"SplitPaneDivider.border",
														null,
														// TabbedPane
														"TabbedPane.tabAreaInsets",
														tabbedpaneTabInsets,
														"TabbedPane.font",
														fontDialog12,
														"TabbedPane.focusInputMap",
														new UIDefaults.LazyInputMap(new Object[] { "RIGHT", "navigateRight", "KP_RIGHT", "navigateRight", "LEFT",
																"navigateLeft", "KP_LEFT", "navigateLeft", "UP", "navigateUp", "KP_UP", "navigateUp", "DOWN",
																"navigateDown", "KP_DOWN", "navigateDown", CONTROL_ID + " DOWN", "requestFocusForVisibleComponent",
																CONTROL_ID + " KP_DOWN", "requestFocusForVisibleComponent", }),
																"TabbedPane.ancestorInputMap",
																new UIDefaults.LazyInputMap(new Object[] { CONTROL_ID + " PAGE_DOWN", "navigatePageDown",
																		CONTROL_ID + " PAGE_UP", "navigatePageUp", CONTROL_ID + " UP", "requestFocus",
																		CONTROL_ID + " KP_UP", "requestFocus", }),
																		// Table
																		"Table.font",
																		fontDialog12,
																		"Table.background",
																		Colors.getWhite(),
																		"Table.selectionForeground",
																		getUserTextColor(),
																		"Table.gridColor",
																		new ColorUIResource(200, 200, 200),
																		"Table.focusCellForeground",
																		getHighlightedTextColor(),
																		"Table.focusCellHighlightBorder",
																		new LineBorderUIResource(COLORS.getTextHighlightBackColor().brighter()),
																		"Table.ancestorInputMap",
																		new UIDefaults.LazyInputMap(new Object[] { CONTROL_ID + " C", "copy", CONTROL_ID + " V", "paste",
																				CONTROL_ID + " X", "cut", "COPY", "copy", "PASTE", "paste", "CUT", "cut", "RIGHT",
																				"selectNextColumn", "KP_RIGHT", "selectNextColumn", "LEFT", "selectPreviousColumn", "KP_LEFT",
																				"selectPreviousColumn", "DOWN", "selectNextRow", "KP_DOWN", "selectNextRow", "UP",
																				"selectPreviousRow", "KP_UP", "selectPreviousRow", "shift RIGHT", "selectNextColumnExtendSelection",
																				"shift KP_RIGHT", "selectNextColumnExtendSelection", "shift LEFT",
																				"selectPreviousColumnExtendSelection", "shift KP_LEFT", "selectPreviousColumnExtendSelection",
																				"shift DOWN", "selectNextRowExtendSelection", "shift KP_DOWN", "selectNextRowExtendSelection",
																				"shift UP", "selectPreviousRowExtendSelection", "shift KP_UP", "selectPreviousRowExtendSelection",
																				"PAGE_UP", "scrollUpChangeSelection", "PAGE_DOWN", "scrollDownChangeSelection", "HOME",
																				"selectFirstColumn", "END", "selectLastColumn", "shift PAGE_UP", "scrollUpExtendSelection",
																				"shift PAGE_DOWN", "scrollDownExtendSelection", "shift HOME", "selectFirstColumnExtendSelection",
																				"shift END", "selectLastColumnExtendSelection", CONTROL_ID + " PAGE_UP",
																				"scrollLeftChangeSelection", CONTROL_ID + " PAGE_DOWN", "scrollRightChangeSelection",
																				CONTROL_ID + " HOME", "selectFirstRow", CONTROL_ID + " END", "selectLastRow",
																				CONTROL_ID + " shift PAGE_UP", "scrollRightExtendSelection", CONTROL_ID + " shift PAGE_DOWN",
																				"scrollLeftExtendSelection", CONTROL_ID + " shift HOME", "selectFirstRowExtendSelection",
																				CONTROL_ID + " shift END", "selectLastRowExtendSelection", "TAB", "selectNextColumnCell",
																				"shift TAB", "selectPreviousColumnCell", "ENTER", "selectNextRowCell", "shift ENTER",
																				"selectPreviousRowCell", CONTROL_ID + " A", "selectAll", "ESCAPE", "cancel", "F2", "startEditing" }),
																				// TableHeader
																				"TableHeader.font",
																				fontDialog12,
																				"TableHeader.background",
																				getColors().getCommonBackground(),
																				// TextField
																				"TextField.margin",
																				textFieldMargin,
																				"TextField.font",
																				fontDialog12,
																				"TextField.caretForeground",
																				caretColor,
																				"TextField.focusInputMap",
																				fieldInputMap,
																				"TextField.background",
																				Colors.getWhite(),
																				"FormattedTextField.margin",
																				textFieldMargin,
																				"FormattedTextField.font",
																				fontDialog12,
																				"FormattedTextField.caretForeground",
																				caretColor,
																				"FormattedTextField.background",
																				getWhiteBackground(),
																				"FormattedTextField.focusInputMap",
																				new UIDefaults.LazyInputMap(new Object[] { CONTROL_ID + " C", DefaultEditorKit.copyAction,
																						CONTROL_ID + " V", DefaultEditorKit.pasteAction, CONTROL_ID + " X", DefaultEditorKit.cutAction,
																						"COPY", DefaultEditorKit.copyAction, "PASTE", DefaultEditorKit.pasteAction, "CUT",
																						DefaultEditorKit.cutAction, "shift LEFT", DefaultEditorKit.selectionBackwardAction, "shift KP_LEFT",
																						DefaultEditorKit.selectionBackwardAction, "shift RIGHT", DefaultEditorKit.selectionForwardAction,
																						"shift KP_RIGHT", DefaultEditorKit.selectionForwardAction, CONTROL_ID + " LEFT",
																						DefaultEditorKit.previousWordAction, CONTROL_ID + " KP_LEFT", DefaultEditorKit.previousWordAction,
																						CONTROL_ID + " RIGHT", DefaultEditorKit.nextWordAction, CONTROL_ID + " KP_RIGHT",
																						DefaultEditorKit.nextWordAction, CONTROL_ID + " shift LEFT",
																						DefaultEditorKit.selectionPreviousWordAction, CONTROL_ID + " shift KP_LEFT",
																						DefaultEditorKit.selectionPreviousWordAction, CONTROL_ID + " shift RIGHT",
																						DefaultEditorKit.selectionNextWordAction, CONTROL_ID + " shift KP_RIGHT",
																						DefaultEditorKit.selectionNextWordAction, CONTROL_ID + " A", DefaultEditorKit.selectAllAction,
																						"HOME", DefaultEditorKit.beginLineAction, "END", DefaultEditorKit.endLineAction, "shift HOME",
																						DefaultEditorKit.selectionBeginLineAction, "shift END", DefaultEditorKit.selectionEndLineAction,
																						"BACK_SPACE", DefaultEditorKit.deletePrevCharAction, "DELETE", CONTROL_ID + " BACK_SPACE",
						DefaultEditorKit.deletePrevWordAction, DefaultEditorKit.deleteNextCharAction,
						CONTROL_ID + " DELETE", DefaultEditorKit.deleteNextWordAction, "RIGHT",
						DefaultEditorKit.forwardAction, "LEFT", DefaultEditorKit.backwardAction, "KP_RIGHT",
						DefaultEditorKit.forwardAction, "KP_LEFT", DefaultEditorKit.backwardAction, "ENTER",
						JTextField.notifyAction, CONTROL_ID + " BACK_SLASH", "unselect", CONTROL_ID + " shift O",
						"toggle-componentOrientation", "ESCAPE", "reset-field-edit", "UP", "increment", "KP_UP",
						"increment", "DOWN", "decrement", "KP_DOWN", "decrement", }),
																						"PasswordField.margin",
																						textFieldMargin,
																						"PasswordField.font",
																						fontDialog12,
																						"PasswordField.caretForeground",
																						caretColor,
																						"PasswordField.focusInputMap",
																						fieldInputMap,
																						"PasswordField.background",
																						Colors.getWhite(),
																						"EditorPane.margin",
																						textFieldMargin,
																						"EditorPane.font",
																						fontDialog12,
																						"EditorPane.caretForeground",
																						caretColor,
																						"EditorPane.focusInputMap",
																						multilineInputMap,
																						"EditorPane.background",
																						Colors.getWhite(),
																						"TextPane.margin",
																						textFieldMargin,
																						"TextPane.font",
																						fontDialog12,
																						"TextPane.caretForeground",
																						caretColor,
																						"TextPane.focusInputMap",
																						multilineInputMap,
																						"TextPane.background",
																						Colors.getWhite(),
																						"TextArea.margin",
																						textFieldMargin,
																						"TextArea.font",
																						fontDialog12,
																						"TextArea.caretForeground",
																						caretColor,
																						"TextArea.focusInputMap",
																						multilineInputMap,
																						"TextArea.background",
																						Colors.getWhite(),
																						// TitledBorder
																						"TitledBorder.font",
																						Colors.getWhite(),
																						// ToolBar
																						"ToolBar.background",
																						COLORS.getToolbarColors()[0],
																						"ToolBar.margin",
																						new InsetsUIResource(0, 0, 0, 0),
																						"ToolBar.dockingForeground",
																						Color.ORANGE,
																						"ToolBar.ancestorInputMap",
																						new UIDefaults.LazyInputMap(new Object[] { "UP", "navigateUp", "KP_UP", "navigateUp", "DOWN",
																								"navigateDown", "KP_DOWN", "navigateDown", "LEFT", "navigateLeft", "KP_LEFT", "navigateLeft",
																								"RIGHT", "navigateRight", "KP_RIGHT", "navigateRight" }),
																								// ToolTip
																								"ToolTip.font",
																								fontDialog12,
																								"tooltip.border",
																								toolTipBorder,
																								// Tree
																								"Tree.background",
																								getWhiteBackground(),
																								"Tree.font",
																								fontDialog12,
																								"Tree.selectionBorder",
																								focusCellHighlightBorder,
																								"Tree.selectionBackground", // HERE
																								COLORS.getTextHighlightBackColor(),
																								"Tree.editorBorder",
																								focusCellHighlightBorder,
																								"Tree.line",
																								new ColorUIResource(220, 220, 220),
																								"Tree.selectionBorderColor",
																								COLORS.getTextHighlightBackColor().brighter(),
																								"Tree.openIcon",
																								SwingTools.createImage("plaf/tree_open.png"),
																								"Tree.closedIcon",
																								SwingTools.createImage("plaf/tree_closed.png"),
																								"Tree.leafIcon",
																								SwingTools.createImage("plaf/tree_leaf.png"),
																								"Tree.expandedIcon",
																								SwingTools.createImage("plaf/tree_expanded.png"),
																								"Tree.collapsedIcon",
																								SwingTools.createImage("plaf/tree_collapsed.png"),
																								"Tree.focusInputMap",
																								new UIDefaults.LazyInputMap(new Object[] { CONTROL_ID + " C", "copy", CONTROL_ID + " V", "paste",
																										CONTROL_ID + " X", "cut", "COPY", "copy", "PASTE", "paste", "CUT", "cut", "UP", "selectPrevious",
																										"KP_UP", "selectPrevious", "shift UP", "selectPreviousExtendSelection", "shift KP_UP",
																										"selectPreviousExtendSelection", "DOWN", "selectNext", "KP_DOWN", "selectNext", "shift DOWN",
																										"selectNextExtendSelection", "shift KP_DOWN", "selectNextExtendSelection", "RIGHT", "selectChild",
																										"KP_RIGHT", "selectChild", "LEFT", "selectParent", "KP_LEFT", "selectParent", "PAGE_UP",
																										"scrollUpChangeSelection", "shift PAGE_UP", "scrollUpExtendSelection", "PAGE_DOWN",
																										"scrollDownChangeSelection", "shift PAGE_DOWN", "scrollDownExtendSelection", "HOME", "selectFirst",
																										"shift HOME", "selectFirstExtendSelection", "END", "selectLast", "shift END",
																										"selectLastExtendSelection", "F2", "startEditing", CONTROL_ID + " A", "selectAll",
																										CONTROL_ID + " SLASH", "selectAll", CONTROL_ID + " BACK_SLASH", "clearSelection",
																										CONTROL_ID + " SPACE", "toggleSelectionPreserveAnchor", "shift SPACE", "extendSelection",
																										CONTROL_ID + " HOME", "selectFirstChangeLead", CONTROL_ID + " END", "selectLastChangeLead",
																										CONTROL_ID + " UP", "selectPreviousChangeLead", CONTROL_ID + " KP_UP", "selectPreviousChangeLead",
																										CONTROL_ID + " DOWN", "selectNextChangeLead", CONTROL_ID + " KP_DOWN", "selectNextChangeLead",
																										CONTROL_ID + " PAGE_DOWN", "scrollDownChangeLead", CONTROL_ID + " shift PAGE_DOWN",
																										"scrollDownExtendSelection", CONTROL_ID + " PAGE_UP", "scrollUpChangeLead",
																										CONTROL_ID + " shift PAGE_UP", "scrollUpExtendSelection", CONTROL_ID + " LEFT", "scrollLeft",
																										CONTROL_ID + " KP_LEFT", "scrollLeft", CONTROL_ID + " RIGHT", "scrollRight",
																										CONTROL_ID + " KP_RIGHT", "scrollRight", "SPACE", "toggleSelectionPreserveAnchor", }),
																										"Tree.ancestorInputMap", new UIDefaults.LazyInputMap(new Object[] { "ESCAPE", "cancel" }) };

		table.putDefaults(defaults);

		getColors().addCustomEntriesToTable(table);
		initBorderDefaults(table);
	}

	private static Color getWhiteBackground() {
		return Color.WHITE;
	}

	private Object[] getUIDefaults() {
		Object[] uiDefaults = { "SpinnerUI", "com.rapidminer.gui.look.ui.SpinnerUI", "FileChooserUI",
				"com.rapidminer.gui.look.fc.FileChooserUI", "ToolBarUI", "com.rapidminer.gui.look.ui.ToolBarUI",
				"DesktopIconUI", "com.rapidminer.gui.look.ui.DesktopIconUI", "SliderUI",
				"com.rapidminer.gui.look.ui.SliderUI", "CheckBoxUI", "com.rapidminer.gui.look.ui.CheckBoxUI", "ComboBoxUI",
				"com.rapidminer.gui.look.ui.ComboBoxUI", "RadioButtonUI", "com.rapidminer.gui.look.ui.RadioButtonUI",
				"TextFieldUI", "com.rapidminer.gui.look.ui.TextFieldUI", "FormattedTextFieldUI",
				"com.rapidminer.gui.look.ui.FormattedTextFieldUI", "PasswordFieldUI",
				"com.rapidminer.gui.look.ui.PasswordFieldUI", "EditorPaneUI", "com.rapidminer.gui.look.ui.EditorPaneUI",
				"TextPaneUI", "com.rapidminer.gui.look.ui.TextPaneUI", "TextAreaUI",
				"com.rapidminer.gui.look.ui.TextAreaUI", "ProgressBarUI", "com.rapidminer.gui.look.ui.ProgressBarUI",
				"TreeUI", "com.rapidminer.gui.look.ui.TreeUI", "SplitPaneUI", "com.rapidminer.gui.look.ui.SplitPaneUI",
				"ScrollBarUI", "com.rapidminer.gui.look.ui.ScrollBarUI", "ButtonUI", "com.rapidminer.gui.look.ui.ButtonUI",
				"ToggleButtonUI", "com.rapidminer.gui.look.ui.ToggleButtonUI", "TabbedPaneUI",
				"com.rapidminer.gui.look.ui.TabbedPaneUI", "TableUI", "com.rapidminer.gui.look.ui.TableUI", "TableHeaderUI",
				"com.rapidminer.gui.look.ui.TableHeaderUI", "MenuUI", "com.rapidminer.gui.look.ui.MenuUI", "MenuBarUI",
				"com.rapidminer.gui.look.ui.MenuBarUI", "MenuItemUI", "com.rapidminer.gui.look.ui.MenuItemUI",
				"RadioButtonMenuItemUI", "com.rapidminer.gui.look.ui.RadioButtonMenuItemUI", "CheckBoxMenuItemUI",
				"com.rapidminer.gui.look.ui.CheckBoxMenuItemUI", "PopupMenuSeparatorUI",
				"com.rapidminer.gui.look.ui.PopupMenuSeparatorUI", "InternalFrameUI",
				"com.rapidminer.gui.look.ui.InternalFrameUI", "LabelUI", "com.rapidminer.gui.look.ui.LabelUI", "ListUI",
		"com.rapidminer.gui.look.ui.ListUI" };

		if (!customUIDefaults.isEmpty()) {
			for (String key : customUIDefaults.keySet()) {
				for (int i = 0; i < uiDefaults.length; i += 2) {
					if (key.equals(uiDefaults[i])) {
						uiDefaults[i + 1] = customUIDefaults.get(key);
					}
				}
			}
		}

		return uiDefaults;
	}

	@Override
	protected void initClassDefaults(UIDefaults table) {
		super.initClassDefaults(table);
		table.putDefaults(getUIDefaults());
	}

	private void initBorderDefaults(UIDefaults table) {
		Object[] borderDefaults = {
				"a",
				IconFactory.getSliderThumb(),
				"TextField.border",
				Borders.getTextFieldBorder(),
				"PasswordField.border",
				Borders.getTextFieldBorder(),
				"FormattedTextField.border",
				Borders.getTextFieldBorder(),
				"SplitPane.border",
				Borders.getSplitPaneBorder(),
				"ScrollPane.border",
				Borders.getScrollPaneBorder(),
				"InternalFrame.border",
				Borders.getInternalFrameBorder(),
				"Table.scrollPaneBorder",
				null,  // removed table border, original: Borders.getSplitPaneBorder()
				"Table.tabbedPaneBorder",
				null,  // remove double borders
				"ToolBar.border", Borders.getToolBarBorder(), "Spinner.border", Borders.getSpinnerBorder(),
				"ComboBox.border", Borders.getComboBoxBorder(), "Button.border", Borders.getEmptyButtonBorder(),
				"ToggleButton.border", Borders.getEmptyButtonBorder(), "ProgressBar.border", Borders.getProgressBarBorder(),
				"PopupMenu.border", Borders.getPopupMenuBorder(), "MenuBar.border", Borders.getMenuBarBorder(),
				"CheckBox.border", Borders.getCheckBoxBorder(), "RadioButton.border", Borders.getCheckBoxBorder(),
				"ToolTip.border", Borders.getToolTipBorder(), "CheckBox.icon", IconFactory.getCheckBoxIcon(),
				"RadioButton.icon", IconFactory.getRadioButtonIcon(), "ComboBox.focusCellHighlightBorder",
				Borders.getComboBoxListCellRendererFocusBorder(), };
		table.putDefaults(borderDefaults);
	}

	public static ColorUIResource getDesktopColor() {
		return new ColorUIResource(0, 0, 0);
	}

	public static ColorUIResource getControlShadow() {
		return new ColorUIResource(50, 50, 50);
	}

	public static ColorUIResource getControlDarkShadow() {
		return new ColorUIResource(0, 0, 0);
	}

	public static ColorUIResource getControlHighlight() {
		return new ColorUIResource(255, 255, 255);
	}

	public static ColorUIResource getPrimaryControl() {
		return new ColorUIResource(255, 255, 255);
	}

	public static ColorUIResource getPrimaryControlShadow() {
		return new ColorUIResource(180, 180, 180);
	}

	public static ColorUIResource getPrimaryControlDarkShadow() {
		return new ColorUIResource(70, 70, 70);
	}

	public static ColorUIResource getPrimaryControlHighlight() {
		return new ColorUIResource(255, 255, 255);
	}

	public static ColorUIResource getSystemTextColor() {
		return new ColorUIResource(0, 0, 0);
	}

	public static ColorUIResource getControlTextColor() {
		return new ColorUIResource(0, 0, 0);
	}

	public static ColorUIResource getInactiveControlTextColor() {
		return new ColorUIResource(150, 150, 150);
	}

	public static ColorUIResource getTextBackground() {
		return new ColorUIResource(255, 255, 255);
	}

	public static ColorUIResource getUserTextColor() {
		return new ColorUIResource(0, 0, 0);
	}

	public static ColorUIResource getTextHighlightBackColor() {
		return new ColorUIResource(90, 110, 170);
	}

	public static ColorUIResource getHighlightedTextColor() {
		return new ColorUIResource(Color.white);
	}

	public static ColorUIResource getInactiveSystemTextColor() {
		return new ColorUIResource(180, 180, 180);
	}

	// TODO: This is for the internal docking frame titles
	public static ColorUIResource getWindowTitleBackground() {
		return new ColorUIResource(220, 225, 230);
	}

	public static ColorUIResource getWindowTitleForeground() {
		return new ColorUIResource(255, 255, 255);
	}

	public static ColorUIResource getMenuBackground() {
		return new ColorUIResource(250, 250, 250);
	}

	public static ColorUIResource getMenuForeground() {
		return new ColorUIResource(0, 0, 0);
	}

	public static ColorUIResource getMenuSelectedBackground() {
		return new ColorUIResource(40, 115, 217);
	}

	public static ColorUIResource getMenuSelectedForeground() {
		return new ColorUIResource(255, 255, 255);
	}

	public static ColorUIResource getMenuDisabledForeground() {
		return new ColorUIResource(180, 180, 180);
	}

	public static ColorUIResource getMenuSelectedBack() {
		return new ColorUIResource(90, 110, 170);
	}

	public static ColorUIResource getWindowBackground() {
		return new ColorUIResource(90, 110, 170);

	}

	public static FontUIResource getMainFont() {
		return new FontUIResource("Dialog", 0, 12);
	}

	public static FontUIResource getTextfieldFont() {
		return new FontUIResource("Dialog", 0, 12);
	}

	public ColorUIResource getTextHighlightColor() {
		return getColors().getTextHighlightBackColor();
	}
}
