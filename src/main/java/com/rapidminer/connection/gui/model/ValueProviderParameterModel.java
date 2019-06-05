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
package com.rapidminer.connection.gui.model;

import com.rapidminer.connection.valueprovider.ValueProviderParameter;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


/**
 * Observable {@link ValueProviderParameter} implementation which is only used for the UI.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class ValueProviderParameterModel implements ValueProviderParameter {

	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty value = new SimpleStringProperty();
	private final BooleanProperty encrypted = new SimpleBooleanProperty(true);
	private final BooleanProperty enabled = new SimpleBooleanProperty(true);

	/**
	 * Copy constructor
	 *
	 * @param parameter
	 * 		the original parameter
	 */
	ValueProviderParameterModel(ValueProviderParameter parameter) {
		this.name.set(parameter.getName());
		this.value.set(parameter.getValue());
		this.encrypted.set(parameter.isEncrypted());
		this.enabled.set(parameter.isEnabled());
	}

	/**
	 * Creates a new {@link ValueProviderParameterModel}
	 *
	 * @param name
	 * 		the name
	 * @param value
	 * 		the value
	 * @param encrypted
	 * 		if it is encrypted
	 * @param enabled
	 * 		if it is enabled
	 */
	ValueProviderParameterModel(String name, String value, boolean encrypted, boolean enabled) {
		this.name.set(name);
		this.value.set(value);
		this.encrypted.set(encrypted);
		this.enabled.set(enabled);
	}


	@Override
	public String getName() {
		return name.get();
	}

	/**
	 * @return the observable name property
	 */
	public StringProperty nameProperty() {
		return name;
	}

	/**
	 * Updates the parameter name
	 *
	 * @param name
	 * 		the new name
	 */
	public void setName(String name) {
		this.name.set(name);
	}

	@Override
	public String getValue() {
		return value.get();
	}

	/**
	 * @return the observable value property
	 */
	public StringProperty valueProperty() {
		return value;
	}

	@Override
	public void setValue(String value) {
		this.value.set(value);
	}

	@Override
	public boolean isEncrypted() {
		return encrypted.get();
	}

	/**
	 * @return the observable encrypted property
	 */
	public BooleanProperty encryptedProperty() {
		return encrypted;
	}

	/**
	 * Updates the encrypted status of this property
	 *
	 * @param encrypted
	 *        {@code true} if the property is encrypted
	 */
	public void setEncrypted(boolean encrypted) {
		this.encrypted.set(encrypted);
	}

	@Override
	public boolean isEnabled() {
		return enabled.get();
	}

	/**
	 * @return the observable enabled property
	 */
	public BooleanProperty enabledProperty() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled.set(enabled);
	}

	/**
	 * Creates a copy of the data <strong>without the listeners</strong>
	 *
	 * @return a copy without the listeners
	 */
	public ValueProviderParameterModel copyDataOnly() {
		return new ValueProviderParameterModel(this);
	}
}
