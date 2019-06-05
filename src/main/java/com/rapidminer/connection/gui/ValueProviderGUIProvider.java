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
package com.rapidminer.connection.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JDialog;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandler;
import com.rapidminer.tools.I18N;


/**
 * GUI Provider to configure complicated setups for {@link com.rapidminer.connection.valueprovider.ValueProvider ValueProviders}. These need to take care of setting the values immediately themselves.
 *
 * @author Andreas Timm
 * @since 9.3
 */
public interface ValueProviderGUIProvider {

	/**
	 * Used in the {@link #getCustomLabel} method to add more information to the
	 * injection label and the injector selection.
	 */
	enum CustomLabel {
		/**
		 * Used as a indicator for injected parameter, default "injected by Injector Name"
		 * I18N key: "gui.label.connection.valueprovider.type.{valueProviderType}.injected_parameter.label"
		 */
		INJECTED_PARAMETER,
		/**
		 * Used for the injector selection dropdown, default "Injector Name"
		 * I18N key: "gui.label.connection.valueprovider.type.{valueProviderType}.injector_selection.label"
		 */
		INJECTOR_SELECTION
	}

	/**
	 * If this provider was registered it has to handle the complete configuration of this {@link ValueProviderHandler}
	 *
	 * @param parent
	 * 		the parent dialog
	 * @param handler
	 * 		the handler that should be configured
	 * @param connection
	 * 		the connection which contains the handler
	 * @param editmode
	 * 		if the editmode should be active, so instead of showing the values the user should be able to edit them
	 * @return a UI component to edit the given handler
	 */
	JComponent createConfigurationComponent(JDialog parent, ValueProvider handler, ConnectionInformation connection, boolean editmode);

	/**
	 * Returns a custom label for either the value provider selection, or the placeholder text
	 * <p>The default implementation uses the keys <pre>"gui.label.connection.valueprovider.type.{valueProviderType}.injected_parameter.label"</pre>
	 * and
	 * <pre>"gui.label.connection.valueprovider.type.{valueProviderType}.injector_selection.label"</pre> </p>
	 * </p>
	 *
	 * Implementations should just call {@code super.getCustomLabel} with additional i18n arguments. The value provider
	 * name is always passed as the first (0) parameter, parameter 1 till n are implementation specific.
	 *
	 * @param key
	 * 		the label type
	 * @param provider
	 * 		the value provider
	 * @param connection
	 * 		the connection which contains the value provider
	 * @param group
	 * 		the group of the parameter which is injected
	 * @param parameterKey
	 * 		the parameter key which is injected
	 * @param args
	 * 		can be used by implementations to pass additional i18n arguments
	 * @return the custom label, or {@code null} if no custom label exists
	 */
	default String getCustomLabel(CustomLabel key, ValueProvider provider, ConnectionInformation connection, String group, String parameterKey, Object... args) {
		List<Object> params = new ArrayList<>();
		params.add(provider.getName());
		if (args != null) {
			params.addAll(Arrays.asList(args));
		}
		String type = provider.getType().replace(':', '.');
		String keyString = key.toString().toLowerCase(Locale.ENGLISH);
		String fullKey = String.join(ConnectionI18N.KEY_DELIMITER, ConnectionI18N.VALUE_PROVIDER_TYPE_PREFIX, type, keyString, ConnectionI18N.LABEL_SUFFIX);
		return I18N.getGUIMessageOrNull(fullKey, params.toArray());
	}
}
