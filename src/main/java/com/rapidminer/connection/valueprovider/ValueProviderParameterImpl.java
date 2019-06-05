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
package com.rapidminer.connection.valueprovider;

import java.io.IOException;
import java.security.Key;
import java.util.Objects;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.cipher.CipherException;
import com.rapidminer.tools.cipher.CipherTools;
import com.rapidminer.tools.cipher.KeyGeneratorTool;

/**
 * The implementation of {@link ValueProviderParameter}. This is a (enabled or disabled and possibly encrypted)
 * key/value pair, where only the {@code value} is mutable and the {@code name} is mandatory.
 * <p>
 * When written to Json using an {@link com.fasterxml.jackson.databind.ObjectMapper ObjectMapper}
 * and this parameter is flagged as encrypted, the {@code value} will be encrypted
 * with the normal RapidMiner encryption method.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ValueProviderParameterImpl implements ValueProviderParameter {

	/**
	 * Mix-In that allows to serialize "value" unencrypted
	 */
	public abstract class UnencryptedValueMixIn {
		@JsonIgnore abstract String getJsonValue();
		@JsonIgnore abstract void setJsonValue(String value);
		@JsonGetter("value") abstract String getValue();
		@JsonSetter("value") abstract String setValue();
	}

	private String name;
	private boolean encrypted;
	private boolean enabled = true;
	private String value;

	/** Minimal constructor */
	@JsonCreator
	public ValueProviderParameterImpl(@JsonProperty(value = "name", required = true) String name) {
		this(name, null, false);
	}

	/** Key/value constructor. {@code value} cannot be {@code null} or empty here. */
	public ValueProviderParameterImpl(String name, String value) {
		this(name, ValidationUtil.requireNonEmptyString(value, "value"), false);
	}

	/** Constructor for enabled parameters. Only {@code name} is mandatory here. */
	public ValueProviderParameterImpl(String name, String value, boolean encrypted) {
		this(name, value, encrypted, true);
	}

	/** Full constructor. Only {@code name} is mandatory here. */
	public ValueProviderParameterImpl(String name, String value, boolean encrypted, boolean enabled) {
		setName(name);
		setEncrypted(encrypted);
		setEnabled(enabled);
		setValue(value);
	}

	@Override
	public String getName() {
		return name;
	}

	/** Sets the name of this parameter. Must be neither {@code null} nor empty. */
	private void setName(String name) {
		this.name = ValidationUtil.requireNonEmptyString(name, "name");
	}

	@Override
	public String getValue() {
		return value;
	}

	/**
	 * Json specific getter for the value. Will simply return the value if this parameter is not encrypted.
	 * If this parameter is encrypted, will return the encrypted value using the {@link Key} defined per Studio user.
	 * If a key cannot be found, or encrypting does not work, will return {@code null} to prevent leaking the value.
	 */
	@JsonGetter(value = "value")
	private String getJsonValue() {
		if (!encrypted || getValue() == null) {
			return getValue();
		}
		Key key;
		try {
			key = KeyGeneratorTool.getUserKey();
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.encryption.could_not_retrieve_key", e);
			return null;
		}
		try {
			return CipherTools.encrypt(getValue(), key);
		} catch (CipherException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.encryption.could_not_encrypt", e);
			return null;
		}
	}

	/** Sets the value of this parameter. Can be either {@code null} or empty. */
	@Override
	public void setValue(String value) {
		this.value = StringUtils.stripToNull(value);
	}

	/**
	 * Json specific setter for the value. Will simply set the value if this parameter is not encrypted.
	 * If this parameter is encrypted, will try to decrypt the value using the {@link Key} defined per Studio user.
	 * If a key cannot be found, or decrypting does not work, will set the value to {@code null}.
	 */
	@JsonSetter(value = "value")
	private void setJsonValue(String value) {
		value = StringUtils.stripToNull(value);
		if (!encrypted || value == null) {
			setValue(value);
			return;
		}
		Key key;
		try {
			key = KeyGeneratorTool.getUserKey();
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.encryption.could_not_retrieve_key", e);
			setValue(null);
			return;
		}
		try {
			setValue(CipherTools.decrypt(value, key));
		} catch (CipherException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.encryption.could_not_decrypt", e);
			setValue(null);
		}
	}

	@Override
	public boolean isEncrypted() {
		return encrypted;
	}

	/**
	 * Sets this parameter to be encrypted. This is only used during creation, either programmatically or when parsing Json.
	 * To ensure that encrypted values are decrypted, and since the order inside a Json string cannot be guaranteed,
	 * this will try to decrypt the value if it was already set before.
	 */
	private void setEncrypted(boolean encrypted) {
		if (this.encrypted == encrypted) {
			return;
		}
		this.encrypted = encrypted;
		if (encrypted && value != null) {
			// decrypt value if the Json order was different than expected
			setJsonValue(value);
		}
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ValueProviderParameterImpl that = (ValueProviderParameterImpl) o;
		return encrypted == that.encrypted &&
				enabled == that.enabled &&
				Objects.equals(name, that.name) &&
				Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, encrypted, enabled, value);
	}

	@Override
	public String toString() {
		return name + ": " + (encrypted ? "(encrypted)" : value) + (enabled ? "" : " - disabled");
	}
}
