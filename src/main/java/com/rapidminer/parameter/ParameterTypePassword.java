/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.parameter;

import java.util.logging.Level;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.cipher.CipherException;
import com.rapidminer.tools.cipher.CipherTools;
import com.rapidminer.tools.encryption.EncryptionProviderBuilder;
import com.rapidminer.tools.encryption.EncryptionProviderSymmetric;
import com.rapidminer.tools.encryption.exceptions.EncryptionContextNotFound;
import com.rapidminer.tools.encryption.exceptions.EncryptionNotInitializedException;


/**
 * A parameter for passwords. The parameter is written with asteriks in the GUI but can be read in process configuration
 * file. Please make sure that noone but the user can read the password from such a file.
 *
 * @author Ingo Mierswa, Simon Fischer
 * @deprecated since 9.7, DO NOT USE THIS ANYMORE!!! Always use the new connection framework introduced in 9.3 when
 * secure credential storage is necessary! See {@link com.rapidminer.connection.ConnectionInformation}.
 * ParameterTypePassword is NOT guaranteed to work properly when it comes to repository encryption!!!
 */
@Deprecated
public class ParameterTypePassword extends ParameterTypeString {

	private static final long serialVersionUID = 384977559199162363L;

	public ParameterTypePassword(String key, String description) {
		super(key, description, true);
		setExpert(false);
	}

	@Override
	public String getRange() {
		return "password";
	}

	@Override
	public Object getDefaultValue() {
		return null;
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		// passwords cannot have a default value
	}

	@Override
	public String transformNewValue(String value, String encryptionContext) {
		if (value == null) {
			return null;
		} else {
			return decryptPassword(value, encryptionContext);
		}
	}

	@Override
	public String toXMLString(Object value, String encryptionContext) {
		return encryptPassword(String.valueOf(value), encryptionContext);
	}

	private String encryptPassword(String value, String encryptionContext) {
		try {
			// no encryption at all
			if (encryptionContext == null) {
				return value;
			} else {
				EncryptionProviderSymmetric encryptionProvider = new EncryptionProviderBuilder().withContext(encryptionContext).buildSymmetricProvider();
				return encryptionProvider.encodeToBase64(encryptionProvider.encryptString(value.toCharArray()));
			}
		} catch (EncryptionContextNotFound e) {
			LogService.getRoot().log(Level.SEVERE,
					"com.rapidminer.parameter.ParameterTypePassword.encrypting_password_error_unknown_context", e.getContext());
			return value;
		} catch (EncryptionNotInitializedException e) {
			// something has gone horribly wrong, do not encrypt
			LogService.getRoot().log(Level.SEVERE,
					"com.rapidminer.parameter.ParameterTypePassword.encrypting_password_error_not_initialized");
			return value;
		} catch (Exception e) {
			// something has gone horribly wrong, do not encrypt
			LogService.getRoot().log(Level.SEVERE,
					"com.rapidminer.parameter.ParameterTypePassword.encrypting_password_error_failure", e);
			return value;
		}
	}

	private String decryptPassword(String value, String encryptionContext) {
		// try to decrypt with new encryption framework (introduced in 9.7) first
		try {
			// no decryption at all, so we could return the input directly
			// HOWEVER, legacy XML might very well have an encrypted value here, so we have to try to decrypt silently anyway
			// If it fails, we return the input as intended
			// If we did not do this, existing building blocks and other pieces would fail here
			if (encryptionContext == null) {
				if (CipherTools.isKeyAvailable()) {
					try {
						return CipherTools.decrypt(value);
					} catch (Exception e) {
						// silently ignore
					}
				}

				return value;
			}
			EncryptionProviderSymmetric encryptionProvider = new EncryptionProviderBuilder().withContext(encryptionContext).buildSymmetricProvider();
			return new String(encryptionProvider.decryptString(encryptionProvider.decodeFromBase64(value)));
		} catch (EncryptionContextNotFound e) {
			LogService.getRoot().log(Level.SEVERE,
					"com.rapidminer.parameter.ParameterTypePassword.decrypting_password_error_unknown_context", e.getContext());
			return value;
		} catch (EncryptionNotInitializedException e) {
			// something has gone horribly wrong, do not decrypt
			LogService.getRoot().log(Level.SEVERE,
					"com.rapidminer.parameter.ParameterTypePassword.decrypting_password_error_not_initialized");
			return value;
		} catch (Exception e) {
			// try to decrypt with legacy CipherTools now, might still be old encryption
			LogService.getRoot().log(Level.INFO,
					"com.rapidminer.parameter.ParameterTypePassword.decrypting_password_error_failure");
		}

		// The above failed, fall back to legacy CipherTools
		if (CipherTools.isKeyAvailable()) {
			try {
				return CipherTools.decrypt(value);
			} catch (CipherException e) {
				LogService.getRoot().log(Level.FINE,
						"com.rapidminer.parameter.ParameterTypePassword.password_looks_like_unencrypted_plain_text");
			}
		}

		return value;
	}
}
