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
package com.rapidminer.gui.security;

import java.net.PasswordAuthentication;
import java.util.Arrays;


/**
 * The user credentials stored in a {@link Wallet}. Each username belongs to one URL.
 *
 * @author Miguel Buescher
 *
 */
public class UserCredential {

	private String url;
	private String user;
	private char[] password;

	public UserCredential(String url, String user, char[] password) {
		super();
		this.url = url;
		this.user = user;
		this.password = password;
	}

	public UserCredential(UserCredential entry) {
		this(entry.getURL(), entry.getUsername(), Arrays.copyOf(entry.getPassword(), entry.getPassword().length));
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	public String getURL() {
		return url;
	}

	public String getUsername() {
		return user;
	}

	public char[] getPassword() {
		return password;
	}

	public PasswordAuthentication makePasswordAuthentication() {
		return new PasswordAuthentication(getUsername(), getPassword());
	}
}
