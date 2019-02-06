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
package com.rapidminer.parameter;

/**
 * Connections may require an OAuth. This interface defines the basic authentication steps which are
 * used by the {@link ParameterTypeOAuth} to link RapidMiner with the connection type.
 * 
 * 
 * @author Marcel Michel
 * @since 6.0.003
 * 
 */
public interface OAuthMechanism {

	/**
	 * Starts the OAuth mechanism. Returns the authorization URL as string.
	 * 
	 * @return
	 */
	public abstract String startOAuth();

	/**
	 * Called at the end of the OAuth. Return <code>null</code> if the code is valid otherwise the
	 * error message. If {@link #isOAuth2()} returns false code will be null; otherwise the input
	 * code by the user, which is always != <code>null</code>
	 * 
	 * @param code
	 * 
	 * @return
	 */
	public abstract String endOAuth(String code);

	/**
	 * If the OAuth was successful return the access token, otherwise <code>null</code>
	 * 
	 * @return
	 */
	public abstract String getToken();

	/**
	 * Returns true if auth mechanism is an instance of OAuth 2.0 and therefore requires a
	 * confirmation code.
	 * 
	 */
	public abstract boolean isOAuth2();

}
