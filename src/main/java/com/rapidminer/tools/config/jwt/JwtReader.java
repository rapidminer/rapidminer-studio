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
package com.rapidminer.tools.config.jwt;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.internal.remote.RemoteRepository;


/**
 * Retrieves a {@link JwtClaim} from a {@link RemoteRepository}, which contains additional information about the user
 *
 * @author Jonas Wilms-Pfau
 * @since 8.1.0
 */
public class JwtReader {

	/**
	 * JWT Wrapper object
	 *
	 * @author Jonas Wilms-Pfau
	 * @since 8.1.0
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class JwtWrapper {

		private String idToken;

		String getIdToken() {
			return idToken;
		}

		public void setIdToken(String idToken) {
			this.idToken = idToken;
		}

	}

	/**
	 * JWT Specification
	 */
	private static final int JWT_HEADER = 0;
	private static final int JWT_PAYLOAD = 1;
	private static final int JWT_SIGNATURE = 2;
	private static final int[] JWT_STRUCTURE = {JWT_HEADER, JWT_PAYLOAD, JWT_SIGNATURE};
	/**
	 * Regex for the JWT Separator
	 */
	private static final String JWT_SEPARATOR_REGEX = "\\.";
	/**
	 * Location of the tokenservice
	 */
	private static final String TOKENSERVICE_RELATIVE_URL = "internal/jaxrest/tokenservice";
	/**
	 * Authorization header key for a connection
	 */
	private static final String AUTH = "Authorization";
	/**
	 * Bearer for the authorization header
	 */
	private static final String BEARER = "Bearer ";

	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * Read the claim from the remote token service without verifying the signature
	 *
	 * <p>
	 * Warning: Don't use the result of this method to give access to sensitive information!
	 * </p>
	 *
	 * @return JwtClaim or null
	 * @throws RepositoryException
	 * @throws IOException
	 */
	public JwtClaim readClaim(RemoteRepository source) throws RepositoryException, IOException {
		try {
			JwtWrapper wrapper = loadJwtWrapper(source);
			if (wrapper == null) {
				return null;
			}
			//Split the token into header, payload and signature
			String[] token = wrapper.getIdToken().split(JWT_SEPARATOR_REGEX);
			//Verify the structure of the Token
			if (token.length == JWT_STRUCTURE.length) {
				//Extract the payload
				String base64Body = token[JWT_PAYLOAD];
				//Base64 Decode
				byte[] jsonBody = Base64.getDecoder().decode(base64Body);
				//Read the decoded JSON
				return mapper.readValue(jsonBody, JwtClaim.class);
			} else {
				throw new RepositoryException("Invalid response from TokenService.");
			}
		} catch (IllegalArgumentException | JsonMappingException e) {
			throw new RepositoryException("Invalid response from TokenService.", e);
		}
	}

	/**
	 * Retrieve the JWT token from remote and set it on the given connection as authorization header.
	 *
	 * @param repository the {@link RemoteRepository} that should be accessed
	 * @param connection the connection to add an authorization header to
	 * @throws IOException
	 * @throws RepositoryException
	 * @since 9.3
	 * @deprecated since 9.5.0, this is a protected method in the ServerClient version 9.3.0 and can be used with a proper client implementation
	 */
	@Deprecated
	public void setJwtAuthorization(RemoteRepository repository, HttpURLConnection connection) throws IOException, RepositoryException {
		final JwtWrapper jwtWrapper = loadJwtWrapper(repository);
		if (jwtWrapper == null) {
			return;
		}
		String token = jwtWrapper.getIdToken();
		connection.setRequestProperty(AUTH, BEARER + token);
	}

	/**
	 * Load the JwtWrapper containing idToken and expiration date from a source repository.
	 */
	private JwtWrapper loadJwtWrapper(RemoteRepository source) throws IOException, RepositoryException {
		if (source == null) {
			return null;
		}
		URLConnection connection = source.getHTTPConnection(TOKENSERVICE_RELATIVE_URL, true);
		if (connection == null) {
			throw new RepositoryException("Could not connect to TokenService.");
		}

		try (InputStream inputStream = connection.getInputStream()) {
			//First extract the outer wrapper
			return mapper.readValue(inputStream, JwtWrapper.class);
		}
	}
}
