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
package com.rapidminer.tools;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.swing.JComponent;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.RapidMiner;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;


/**
 * @author Simon Fischer, Nils Woehler, Adrian Wilke
 */
public class I18N {

	private static final ExtensibleResourceBundle USER_ERROR_BUNDLE;
	private static final ExtensibleResourceBundle ERROR_BUNDLE;
	private static final ExtensibleResourceBundle GUI_BUNDLE;
	private static final ExtensibleResourceBundle SETTINGS_BUNDLE;

	private static final Locale ORIGINAL_LOCALE = Locale.getDefault();

	public static final String SETTINGS_TYPE_TITLE_SUFFIX = ".title";
	public static final String SETTINGS_TYPE_DESCRIPTION_SUFFIX = ".description";
	public static final String ICON_SUFFIX = ".icon";

	// init I18N
	static {
		ParameterService.init();

		String localeLanguage = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_LOCALE_LANGUAGE);
		Locale locale = Locale.forLanguageTag(Objects.toString(localeLanguage,""));
		if (!locale.getLanguage().isEmpty()) {
			Locale.setDefault(locale);
			LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.I18N.set_locale_to", locale);
		} else {
			locale = Locale.getDefault();
			LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.I18N.using_default_locale", locale);
		}
		JComponent.setDefaultLocale(locale);

		USER_ERROR_BUNDLE = new ExtensibleResourceBundle(ResourceBundle.getBundle(
				"com.rapidminer.resources.i18n.UserErrorMessages", locale, I18N.class.getClassLoader()));
		ERROR_BUNDLE = new ExtensibleResourceBundle(ResourceBundle.getBundle("com.rapidminer.resources.i18n.Errors", locale,
				I18N.class.getClassLoader()));
		GUI_BUNDLE = new ExtensibleResourceBundle(ResourceBundle.getBundle("com.rapidminer.resources.i18n.GUI", locale,
				I18N.class.getClassLoader()));
		SETTINGS_BUNDLE = new ExtensibleResourceBundle(ResourceBundle.getBundle("com.rapidminer.resources.i18n.Settings",
				locale, I18N.class.getClassLoader()));

		ResourceBundle plotterBundle = ResourceBundle.getBundle("com.rapidminer.resources.i18n.PlotterMessages", locale,
				I18N.class.getClassLoader());

		GUI_BUNDLE.addResourceBundle(plotterBundle);
	}

	/** Returns the resource bundle for error messages and quick fixes. */
	public static ResourceBundle getErrorBundle() {
		return ERROR_BUNDLE;
	}

	/**
	 * Type of I18N message for the settings bundle. Provided toString() methods include a point
	 * ('.') as prefix, followed by the type in lower case. E.g. '.description'.
	 */
	public enum SettingsType {
		TITLE {

			@Override
			public String toString() {
				return SETTINGS_TYPE_TITLE_SUFFIX;
			}
		},
		DESCRIPTION {

			@Override
			public String toString() {
				return SETTINGS_TYPE_DESCRIPTION_SUFFIX;
			}
		}
	}

	public static ResourceBundle getGUIBundle() {
		return GUI_BUNDLE;
	}

	public static ResourceBundle getUserErrorMessagesBundle() {
		return USER_ERROR_BUNDLE;
	}

	public static ResourceBundle getSettingsBundle() {
		return SETTINGS_BUNDLE;
	}

	/** registers the properties of the given bundle on the global error bundle */
	public static void registerErrorBundle(ResourceBundle bundle) {
		registerErrorBundle(bundle, false);
	}

	/** registers the properties of the given bundle on the global gui bundle */
	public static void registerGUIBundle(ResourceBundle bundle) {
		registerGUIBundle(bundle, false);
	}

	/** registers the properties of the given bundle on the global userError bundle */
	public static void registerUserErrorMessagesBundle(ResourceBundle bundle) {
		registerUserErrorMessagesBundle(bundle, false);
	}

	/** registers the properties of the given bundle on the global settings bundle */
	public static void registerSettingsBundle(ResourceBundle bundle) {
		registerSettingsBundle(bundle, false);
	}

	/**
	 * Registers the properties of the given bundle on the global error bundle
	 * @param bundle bundle to register
	 * @param overwrite always false, internal api
	 */
	public static void registerErrorBundle(ResourceBundle bundle, boolean overwrite) {
		registerBundle(ERROR_BUNDLE, bundle, overwrite);
	}

	/**
	 * Registers the properties of the given bundle on the global gui bundle
	 * @param bundle bundle to register
	 * @param overwrite always false, internal api
	 */
	public static void registerGUIBundle(ResourceBundle bundle, boolean overwrite) {
		registerBundle(GUI_BUNDLE, bundle, overwrite);
	}

	/**
	 * Registers the properties of the given bundle on the global userError bundle
	 * @param bundle bundle to register
	 * @param overwrite always false, internal api
	 */
	public static void registerUserErrorMessagesBundle(ResourceBundle bundle, boolean overwrite) {
		registerBundle(USER_ERROR_BUNDLE, bundle, overwrite);
	}

	/**
	 * Registers the properties of the given bundle on the global settings bundle
	 * @param bundle bundle to register
	 * @param overwrite always false, internal api
	 */
	public static void registerSettingsBundle(ResourceBundle bundle, boolean overwrite) {
		registerBundle(SETTINGS_BUNDLE, bundle, overwrite);
	}

	/**
	 * Registers the given bundle in the targetBundle
	 *
	 * @param targetBundle the target bundle
	 * @param bundle bundle to register
	 * @param overwrite always false, internal api
	 */
	private static void registerBundle(ExtensibleResourceBundle targetBundle, ResourceBundle bundle, boolean overwrite) {
		if (overwrite) {
			targetBundle.addResourceBundleAndOverwrite(bundle);
		} else {
			targetBundle.addResourceBundle(bundle);
		}
	}

	/**
	 * Returns a message if found or the key if not found. Arguments <b>can</b> be specified which
	 * will be used to format the String. In the {@link ResourceBundle} the String '{0}' (without ')
	 * will be replaced by the first argument, '{1}' with the second and so on.
	 *
	 * Catches the exception thrown by ResourceBundle in the latter case.
	 * @param bundle the bundle that contains the key
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or the key if it does not exists in the bundle
	 **/
	public static String getMessage(ResourceBundle bundle, String key, Object... arguments) {
		try {
			return getMessageOptimistic(bundle, key, arguments);
		} catch (MissingResourceException e) {
			LogService.getRoot().log(Level.FINEST, "com.rapidminer.tools.I18N.missing_key", key);
			return key;
		}
	}

	/**
	 * Convenience method to call {@link #getMessage(ResourceBundle, String, Object...)} with return
	 * value from {@link #getGUIBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or the key if it does not exists in the bundle
	 */
	public static String getGUIMessage(String key, Object... arguments) {
		return getMessage(getGUIBundle(), key, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessageOrNull(ResourceBundle, String, Object...)} with
	 * return value from {@link #getGUIBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or null if the key does not exists in the bundle
	 */
	public static String getGUIMessageOrNull(String key, Object... arguments) {
		return getMessageOrNull(getGUIBundle(), key, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessage(ResourceBundle, String, Object...)} with return
	 * value from {@link #getErrorBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or the key if it does not exists in the bundle
	 */
	public static String getErrorMessage(String key, Object... arguments) {
		return getMessage(getErrorBundle(), key, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessageOrNull(ResourceBundle, String, Object...)} with
	 * return value from {@link #getErrorBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or null if the key does not exists in the bundle
	 */
	public static String getErrorMessageOrNull(String key, Object... arguments) {
		return getMessageOrNull(getErrorBundle(), key, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessage(ResourceBundle, String, Object...)} with return
	 * value from {@link #getUserErrorMessagesBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or the key if it does not exists in the bundle
	 */
	public static String getUserErrorMessage(String key, Object... arguments) {
		return getMessage(getUserErrorMessagesBundle(), key, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessageOrNull(ResourceBundle, String, Object...)} with
	 * return value from {@link #getUserErrorMessagesBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or null if the key does not exists in the bundle
	 */
	public static String getUserErrorMessageOrNull(String key, Object... arguments) {
		return getMessageOrNull(getUserErrorMessagesBundle(), key, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessage(ResourceBundle, String, Object...)} with return
	 * value from {@link #getSettingsBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or the key if it does not exists in the bundle
	 */
	public static String getSettingsMessage(String key, SettingsType type, Object... arguments) {
		return getMessage(getSettingsBundle(), key + type, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessageOrNull(ResourceBundle, String, Object...)} with
	 * return value from {@link #getSettingsBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or null if the key does not exists in the bundle
	 */
	public static String getSettingsMessageOrNull(String key, SettingsType type, Object... arguments) {
		return getMessageOrNull(getSettingsBundle(), key + type, arguments);
	}

	/**
	 * Returns a message if found or <code>null</code> if not.
	 *
	 * Arguments <b>can</b> be specified which will be used to format the String. In the
	 * {@link ResourceBundle} the String '{0}' (without ') will be replaced by the first argument,
	 * '{1}' with the second and so on.
	 * @param bundle the bundle that contains the key
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or null if key not exists in the bundle
	 */
	public static String getMessageOrNull(ResourceBundle bundle, String key, Object... arguments) {
		try {
			return getMessageOptimistic(bundle, key, arguments);
		} catch (MissingResourceException e) {
			return null;
		}
	}

	/**
	 * This will return the value of the property gui.label.-key- of the GUI bundle or the key
	 * itself if unknown.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or the key if it does not exists in the bundle
	 */
	public static String getGUILabel(String key, Object... arguments) {
		return getMessage(GUI_BUNDLE, "gui.label." + key, arguments);
	}

	/**
	 * Gets the original result of {@link Locale#getDefault()} (before it is exchanged with a custom locale due to our
	 * language chooser).
	 *
	 * @return the original default locale object before it is changed due to our I18N combobox
	 * @since 8.2.1
	 */
	public static Locale getOriginalLocale() {
		return ORIGINAL_LOCALE;
	}

	/**
	 * Returns a message if found or the key if not found. Arguments <b>can</b> be specified which
	 * will be used to format the String. In the {@link ResourceBundle} the String '{0}' (without ')
	 * will be replaced by the first argument, '{1}' with the second and so on.
	 *
	 * @param bundle the bundle that contains the key
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key
	 * @throws java.util.MissingResourceException – if no object for the given key can be found
	 */
	private static String getMessageOptimistic(ResourceBundle bundle, String key, Object... arguments) throws MissingResourceException {
		String message = bundle.getString(key);
		if (arguments == null || arguments.length == 0) {
			return message;
		}
		return MessageFormat.format(message, arguments);
	}

	/**
	 * <p>
	 * Registers a new language tag to be shown in the RapidMiner Settings
	 * </p>
	 *
	 * <p>
	 * Important: Use underscore for the .properties files, but hyphen for the language tag!<br/>
	 * Examples:
	 * <table border="1">
	 * <tr>
	 * <th>Language Tag</th><th>Filename</th>
	 * </tr>
	 * <tr>
	 * <td>English Fallback</td> <td>MyExtGUI.properties</td>
	 * </tr>
	 * <tr>
	 * <td>"de"</td> <td>MyExtGUI_de.properties</td>
	 * </tr>
	 * <tr>
	 * <td>"de-AT"</td> <td>MyExtGUI_de_AT.properties</td>
	 * </tr>
	 * </table>
	 * If de-AT is the selected language, first de-AT files are checked, second de, finally {@link Locale#ROOT}
	 *
	 * @param languageTag
	 * 		An IETF BCP 47 language tag, i.e. "fr", "zh", "de" or "en-GB"
	 * @throws IllegalArgumentException
	 * 		if the given {@code languageTag} is not valid
	 * @throws NullPointerException
	 * 		if {@code languageTag} is {@code null}
	 * @since 9.1.0
	 */
	public static void registerLanguage(String languageTag) {
		// Check if language key is valid
		if (Locale.forLanguageTag(languageTag).getLanguage().isEmpty()) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.I18N.add_language_wrong_format", String.valueOf(languageTag));
			throw new IllegalArgumentException(languageTag + " is not a valid IETF BCP 47 language tag.");
		}

		ParameterType type = ParameterService.getParameterType(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_LOCALE_LANGUAGE);
		if (!(type instanceof ParameterTypeCategory)) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.tools.I18N.add_language_failed", languageTag);
			return;
		}
		ParameterTypeCategory lng = ((ParameterTypeCategory) type);
		if (ArrayUtils.contains(lng.getValues(), languageTag)) {
			//already registered
			return;
		}
		// Add language key to the end, not sorted
		String[] languages = (String[]) ArrayUtils.add(lng.getValues(), languageTag);
		ParameterService.registerParameter(new ParameterTypeCategory(lng.getKey(), lng.getDescription(), languages, lng.getDefault(), lng.isExpert()));
	}
}
