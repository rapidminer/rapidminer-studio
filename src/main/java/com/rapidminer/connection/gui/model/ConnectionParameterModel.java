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

import java.util.Objects;

import org.apache.commons.lang.StringUtils;

import com.rapidminer.connection.gui.components.InjectedParameterPlaceholderLabel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


/**
 * ConnectionParameterModel, maps to {@link com.rapidminer.connection.configuration.ConfigurationParameter}.
 * <p>
 *     Note that you can access the property objects, allowing you to register listeners for changes on each property where needed.
 * </p>
 *
 * <p>For components rendering this model, it's important to respect the encrypted, injected, and potentially (if needed) the enabled state
 * <ul>
 * <li>It is required to not display the value of an encrypted parameter in plain text!</li>
 * <li>Injected Parameters should be identifiable, see {@link InjectedParameterPlaceholderLabel
 * InjectedParameterPlaceholderLabel}</li>
 * </ul>
 * </p>
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class ConnectionParameterModel {

	protected final StringProperty groupName = new SimpleStringProperty();
	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty value = new SimpleStringProperty();
	private final BooleanProperty encrypted = new SimpleBooleanProperty();
	private final StringProperty injectorName = new SimpleStringProperty();
	private final BooleanProperty enabled = new SimpleBooleanProperty();
	private final StringProperty validationError = new SimpleStringProperty();
	private final ConnectionParameterGroupModel parent;


	ConnectionParameterModel(ConnectionParameterGroupModel parent, String name, String value, boolean isEncrypted, String injectorName, boolean isEnabled) {
		this.parent = parent;
		this.groupName.set(parent.getName());
		this.name.set(name);
		this.value.set(value);
		this.encrypted.set(isEncrypted);
		this.injectorName.set(injectorName);
		this.enabled.set(isEnabled);
	}

	/**
	 * Copy constructor.
	 *
	 * @param parameter
	 * 		the original which values will be copied
	 */
	ConnectionParameterModel(ConnectionParameterGroupModel parent, ConnectionParameterModel parameter) {
		this.parent = parent;
		this.groupName.set(parameter.groupName.get());
		this.name.set(parameter.name.get());
		this.value.set(parameter.value.get());
		this.encrypted.set(parameter.encrypted.get());
		this.injectorName.set(parameter.injectorName.get());
		this.enabled.set(parameter.isEnabled());
	}

	/**
	 * @return {@code true} if the parameter is editable; {@code false} otherwise.
	 */
	public boolean isEditable() {
		return parent.getParent().isEditable();
	}

	/**
	 * @return the {@link ConnectionModel#getType()}
	 */
	public String getType() {
		return parent.getParent().getType();
	}

	public String getGroupName() {
		return groupName.get();
	}

	public String getName() {
		return name.get();
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public StringProperty nameProperty() {
		return name;
	}

	public String getValue() {
		return value.get();
	}

	public void setValue(String value) {
		this.value.set(value);
	}

	public StringProperty valueProperty() {
		return value;
	}

	public boolean isEncrypted() {
		return encrypted.get();
	}

	public void setEncrypted(boolean isEncrypted) {
		this.encrypted.set(isEncrypted);
	}

	public BooleanProperty encryptedProperty() {
		return encrypted;
	}

	/**
	 * Tests whether the injector name is not null and not empty.
	 *
	 * @return {@code true} if an inject is set; {@code false} otherwise
	 */
	public boolean isInjected() {
		return StringUtils.trimToNull(injectorNameProperty().get()) != null;
	}

	public String getInjectorName() {
		return injectorName.get();
	}


	/**
	 * @return the assigned value provider or {@code null}
	 */
	public ValueProviderModel getValueProvider() {
		for (ValueProviderModel vp : getParent().getParent().valueProvidersProperty()) {
			if (Objects.equals(getInjectorName(), vp.getName())) {
				return vp;
			}
		}
		return null;
	}

	public void setInjectorName(String injectorName) {
		this.injectorName.set(injectorName);
	}

	public StringProperty injectorNameProperty() {
		return injectorName;
	}

	public String getValidationError() {
		return validationError.get();
	}

	public void setValidationError(String validationError) {
		this.validationError.set(validationError);
	}

	public StringProperty validationErrorProperty() {
		return validationError;
	}

	/**
	 * See {@link com.rapidminer.connection.configuration.ConfigurationParameter#isEnabled()}.
	 *
	 * @return {@code true} if the parameter is enabled; {@code false} otherwise
	 */
	public boolean isEnabled() {
		return enabled.get();
	}

	public void setEnabled(boolean enabled) {
		this.enabled.set(enabled);
	}

	public BooleanProperty enabledProperty() {
		return enabled;
	}

	/**
	 * Creates a copy of the data <strong>without the listeners</strong>
	 *
	 * @return a copy without the listeners
	 */
	public ConnectionParameterModel copyDataOnly() {
		return new ConnectionParameterModel(getParent(), this);
	}

	/**
	 * @return the group this parameter belongs to
	 */
	ConnectionParameterGroupModel getParent() {
		return parent;
	}

	/**
	 * @return the connection this parameter is part of
	 */
	ConnectionModel getConnection() {
		return getParent().getParent();
	}

}
