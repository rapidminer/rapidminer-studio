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
package com.rapidminer.gui.tools;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.MessageFormat;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.ConditionalAction;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;


/**
 * This will create an action, whose settings are take from a .properties file being part of the GUI
 * Resource bundles of RapidMiner. These might be accessed using the I18N class.
 *
 * A resource action needs a key specifier, which will be used to build the complete keys of the
 * form: gui.action.<specifier>.label = Which will be the caption gui.action.<specifier>.icon = The
 * icon of this action. For examples used in menus or buttons gui.action.<specifier>.acc = The
 * accelerator key used for menu entries gui.action.<specifier>.tip = Which will be the tool tip
 * gui.action.<specifier>.mne = Which will give you access to the mnemonics key. Please make it the
 * same case as in the label
 *
 * @author Simon Fischer, Sebastian Land
 */
public abstract class ResourceAction extends ConditionalAction {

	private static final long serialVersionUID = -3699425760142415331L;

	private final String key;

	private final String iconName;

	private final IconType iconType;

	/**
	 * Specifies the style of the icon.
	 */
	public enum IconType {
		/** standard, multi-colored icons for 24x24 and higher, grey for 16x16 */
		NORMAL,

		/** flat, single-colored icons */
		FLAT,

		/** monochrome icons for 16x16 */
		MONO
	};

	/**
	 * Creates a new {@link ResourceAction} with the standard {@link IconType}.
	 *
	 * @param i18nKey
	 * @param i18nArgs
	 */
	public ResourceAction(String i18nKey, Object... i18nArgs) {
		this(false, i18nKey, i18nArgs);

		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	/**
	 * Creates a new {@link ResourceAction} with the specified {@link IconType}.
	 *
	 * @param i18nKey
	 * @param iconType
	 * @param i18nArgs
	 */
	public ResourceAction(String i18nKey, IconType iconType, Object... i18nArgs) {
		this(false, i18nKey, iconType, i18nArgs);

		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	/**
	 * Creates a new {@link ResourceAction} with the standard {@link IconType}.
	 *
	 * @param smallIcon
	 * @param i18nKey
	 * @param i18nArgs
	 */
	public ResourceAction(boolean smallIcon, String i18nKey, Object... i18nArgs) {
		this(smallIcon ? 16 : 24, i18nKey, i18nArgs);
	}

	/**
	 * Creates a new {@link ResourceAction} with the standard {@link IconType}.
	 *
	 * @param iconSize
	 * @param i18nKey
	 * @param i18nArgs
	 */
	public ResourceAction(int iconSize, String i18nKey, Object... i18nArgs) {
		this(iconSize, i18nKey, IconType.NORMAL, i18nArgs);
	}

	/**
	 * Creates a new {@link ResourceAction} with the specified {@link IconType}.
	 *
	 * @param smallIcon
	 * @param i18nKey
	 * @param iconType
	 * @param i18nArgs
	 */
	public ResourceAction(boolean smallIcon, String i18nKey, IconType iconType, Object... i18nArgs) {
		this(smallIcon ? 16 : 24, i18nKey, iconType, i18nArgs);
	}

	/**
	 * Creates a new {@link ResourceAction} with the specified {@link IconType}.
	 *
	 * @param iconSize
	 * @param i18nKey
	 * @param iconType
	 * @param i18nArgs
	 */
	public ResourceAction(int iconSize, String i18nKey, IconType iconType, Object... i18nArgs) {
		super(i18nArgs == null || i18nArgs.length == 0 ? getMessage(i18nKey + ".label")
				: MessageFormat.format(getMessage(i18nKey + ".label"), i18nArgs));
		putValue(ACTION_COMMAND_KEY, i18nKey);
		this.key = i18nKey;
		this.iconType = iconType;
		String mne = getMessageOrNull(i18nKey + ".mne");
		if (mne != null && mne.length() > 0) {
			String name = (String) getValue(NAME);
			if (name != null && name.length() > 0 && name.indexOf(mne.charAt(0)) == -1
					&& name.indexOf(Character.toLowerCase(mne.charAt(0))) == -1) {
				LogService.getRoot().log(Level.FINE, "com.rapidminer.gui.tools.ResourceAction.key_not_found",
						new Object[] { mne, i18nKey, name });
			}
			mne = mne.toUpperCase();
			putValue(MNEMONIC_KEY, (int) mne.charAt(0));
		}
		String acc = getMessageOrNull(i18nKey + ".acc");
		KeyStroke accStroke = null;
		if (acc != null) {
			if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX) {
				acc = acc.replace("ctrl", "meta");
				acc = acc.replace("control", "meta");
			}
			accStroke = KeyStroke.getKeyStroke(acc);
			putValue(ACCELERATOR_KEY, accStroke);
		}
		String tip = getMessageOrNull(i18nKey + ".tip");
		if (tip != null) {
			if (accStroke != null) {
				tip += " (" + SwingTools.formatKeyStroke(accStroke) + ")";
			}
			putValue(SHORT_DESCRIPTION,
					i18nArgs == null || i18nArgs.length == 0 ? tip : MessageFormat.format(tip, i18nArgs));
		}
		this.iconName = getMessageOrNull(i18nKey + ".icon");
		if (getIconName() != null && !getIconName().trim().isEmpty()) {
			ImageIcon small = null;
			ImageIcon large = null;
			if (iconType == IconType.FLAT) {
				small = SwingTools.createIcon("flat_icons/16/" + getIconName());
				large = SwingTools.createIcon("flat_icons/" + iconSize + "/" + getIconName());
			}
			if (small == null) {
				small = SwingTools.createIcon("16/" + getIconName(), iconType == IconType.MONO);
			}
			if (large == null) {
				large = SwingTools.createIcon(iconSize + "/" + getIconName(), iconType == IconType.MONO);
			}
			putValue(LARGE_ICON_KEY, iconSize == 16 ? small != null ? small : large : large);
			putValue(SMALL_ICON, small != null ? small : large);
		}
		putValue("rm_id", i18nKey);
	}

	/**
	 * Adds the action to the input and action map of the components.
	 *
	 * @param condition
	 *            one out of {@link JComponent#WHEN_FOCUSED}, ...
	 * @param disableOnFocusLost
	 *            if <code>true</code>, will disable the action on FocusLost event and enable it
	 *            again on FocusGained (if conditions of superclass are met).
	 * @param components
	 *            the {@link JComponent}s to register this action to
	 */
	public void addToActionMap(int condition, boolean disableOnFocusLost, boolean initiallyDisabled, String actionKey,
			JComponent... components) {
		for (JComponent comp : components) {
			if (comp == null) {
				throw new IllegalArgumentException("components must not be null!");
			}

			KeyStroke keyStroke = (KeyStroke) getValue(ACCELERATOR_KEY);
			if (keyStroke != null) {
				actionKey = actionKey == null ? key : actionKey;
				comp.getInputMap(condition).put(keyStroke, actionKey);
				comp.getActionMap().put(actionKey, this);
			} else {
				LogService.getRoot().log(Level.FINE, "com.rapidminer.gui.tools.ResourceAction.add_action_key_error", key);
			}
			if (disableOnFocusLost) {
				comp.addFocusListener(new FocusListener() {

					@Override
					public void focusLost(FocusEvent e) {
						if (!e.isTemporary()) {
							// focus lost here means disable it no matter the conditions
							ResourceAction.this.setEnabled(false);
							ResourceAction.super.setDisabledDueToFocusLost(true);
						}
					}

					@Override
					public void focusGained(FocusEvent e) {
						if (!e.isTemporary()) {
							// focus gained here means enable it if conditions are fulfilled
							ResourceAction.super.setDisabledDueToFocusLost(false);
							RapidMinerGUI.getMainFrame().getActions().enableActions();
						}
					}
				});
				if (initiallyDisabled) {
					if (SwingUtilities.isEventDispatchThread()) {
						super.setDisabledDueToFocusLost(true);
						setEnabled(false);
					} else {
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								ResourceAction.super.setDisabledDueToFocusLost(true);
								ResourceAction.this.setEnabled(false);
							}

						});
					}
				}
			}
		}
	}

	@Override
	public void setEnabled(boolean newValue) {
		// overwritten because we need to update it's state if it is registered in the global search

		boolean changed = isEnabled() != newValue;
		super.setEnabled(newValue);

		if (!changed || !isGlobalSearchReady()) {
			return;
		}

		// this must not crash the EDT, so it's wrapped in try/catch
		try {
			if (RapidMinerGUI.getMainFrame().getActionsGlobalSearchManager().isActionRegistered(this)) {
				RapidMinerGUI.getMainFrame().getActionsGlobalSearchManager().addAction(this);
			}
		} catch (Throwable e) {
			// We cannot risk blowing up the EDT, so catch absolutely everything here
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.ResourceAction.error.update_global_search", e);
		}
	}

	/**
	 * Adds the action to the input and action map of the component.
	 *
	 * @param condition
	 *            one out of WHEN_IN_FOCUES, ...
	 */
	public void addToActionMap(JComponent component, int condition) {
		addToActionMap(component, null, condition);
	}

	/**
	 * Adds the action to the input and action map of the component.
	 *
	 * @param condition
	 *            one out of WHEN_IN_FOCUES, ...
	 */
	public void addToActionMap(JComponent component, String actionKey, int condition) {
		addToActionMap(condition, false, false, actionKey, component);
	}

	/**
	 * Adds this action to the Global Search for actions (see {@link com.rapidminer.search.GlobalSearchManager}.
	 * Only call after {@link com.rapidminer.gui.MainFrame} has been initialized!
	 * <p>
	 *     See {@link #removeFromGlobalSearch()} to remove it again.
	 * </p>
	 * @since 8.1
	 */
	public void addToGlobalSearch() {
		if (!isGlobalSearchReady()) {
			return;
		}

		RapidMinerGUI.getMainFrame().getActionsGlobalSearchManager().addAction(this);
	}

	/**
	 * Removes this action from the Global Search (see {@link com.rapidminer.search.GlobalSearchManager}.
	 * @since 8.1
	 */
	public void removeFromGlobalSearch() {
		if (!isGlobalSearchReady()) {
			return;
		}

		RapidMinerGUI.getMainFrame().getActionsGlobalSearchManager().removeAction(this);
	}

	/**
	 * This returns the i18n key of this action.
	 */
	public String getKey() {
		return key;
	}

	public String getIconName() {
		return iconName;
	}

	public IconType getIconType() {
		return iconType;
	}

	/**
	 * Whether the Global Search is ready or not.
	 *
	 * @return {@code true} if it is ready; {@code false} otherwise
	 */
	private boolean isGlobalSearchReady() {
		return RapidMinerGUI.getMainFrame() != null &&
				RapidMinerGUI.getMainFrame().getActionsGlobalSearchManager() != null &&
				RapidMinerGUI.getMainFrame().getActionsGlobalSearchManager().isInitialized();
	}

	private static String getMessage(String key) {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.action." + key);
	}

	private static String getMessageOrNull(String key) {
		return I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.action." + key);
	}

}
