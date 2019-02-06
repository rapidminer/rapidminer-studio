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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Base64;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.internal.remote.RemoteRepository;


/**
 * Tests for correct behavior of the JwtReader.
 *
 * @author Andreas Timm
 * @since 8.2
 */
public class JwtReaderTest {

	private static final String INTERNAL_JAXREST_TOKENSERVICE = "internal/jaxrest/tokenservice";
	private JwtReader jwtReader = new JwtReader();

	@Test
	public void readRepoNull() throws IOException, RepositoryException {
		JwtClaim jwtClaim = jwtReader.readClaim(null);
		Assert.assertNull(jwtClaim);
	}

	@Test(expected = RepositoryException.class)
	public void readRepoConnectionNull() throws IOException, RepositoryException {
		RemoteRepository repository = Mockito.mock(RemoteRepository.class);
		jwtReader.readClaim(repository);
	}

	@Test(expected = RepositoryException.class)
	public void readEmptyInput() throws IOException, RepositoryException {
		RemoteRepository repository = Mockito.mock(RemoteRepository.class);
		HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
		Mockito.when(repository.getHTTPConnection(INTERNAL_JAXREST_TOKENSERVICE, true)).thenReturn(connection);
		InputStream inputStream = new ByteArrayInputStream("".getBytes());
		Mockito.when(connection.getInputStream()).thenReturn(inputStream);
		JwtClaim jwtClaim = jwtReader.readClaim(repository);
		Assert.assertNull(jwtClaim);
	}

	@Test(expected = RepositoryException.class)
	public void readWrongInput() throws IOException, RepositoryException {
		RemoteRepository repository = Mockito.mock(RemoteRepository.class);
		HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
		Mockito.when(repository.getHTTPConnection(INTERNAL_JAXREST_TOKENSERVICE, true)).thenReturn(connection);
		InputStream inputStream = new ByteArrayInputStream("{\"idToken\":\"i d token\"}".getBytes());
		Mockito.when(connection.getInputStream()).thenReturn(inputStream);
		JwtClaim jwtClaim = jwtReader.readClaim(repository);
	}

	@Test(expected = RepositoryException.class)
	public void readMoreWrongInputPartOne() throws IOException, RepositoryException {
		RemoteRepository repository = Mockito.mock(RemoteRepository.class);
		HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
		Mockito.when(repository.getHTTPConnection(INTERNAL_JAXREST_TOKENSERVICE, true)).thenReturn(connection);
		InputStream inputStream = new ByteArrayInputStream("{\"idToken\":\"a.b\"}".getBytes());
		Mockito.when(connection.getInputStream()).thenReturn(inputStream);
		JwtClaim jwtClaim = jwtReader.readClaim(repository);
	}

	@Test(expected = RepositoryException.class)
	public void readMoreWrongInputPartTwo() throws IOException, RepositoryException {
		RemoteRepository repository = Mockito.mock(RemoteRepository.class);
		HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
		Mockito.when(repository.getHTTPConnection(INTERNAL_JAXREST_TOKENSERVICE, true)).thenReturn(connection);
		InputStream inputStream = new ByteArrayInputStream("{\"idToken\":\"a.b.c.d\"}".getBytes());
		Mockito.when(connection.getInputStream()).thenReturn(inputStream);
		JwtClaim jwtClaim = jwtReader.readClaim(repository);
	}

	@Test(expected = RepositoryException.class)
	public void readMoreWrongInputPartThree() throws IOException, RepositoryException {
		RemoteRepository repository = Mockito.mock(RemoteRepository.class);
		HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
		Mockito.when(repository.getHTTPConnection(INTERNAL_JAXREST_TOKENSERVICE, true)).thenReturn(connection);
		InputStream inputStream = new ByteArrayInputStream("{\"idToken\":\"a.b.c\"}".getBytes());
		Mockito.when(connection.getInputStream()).thenReturn(inputStream);
		JwtClaim jwtClaim = jwtReader.readClaim(repository);
	}

	@Test
	public void readInput() throws IOException, RepositoryException {
		RemoteRepository repository = Mockito.mock(RemoteRepository.class);
		HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
		Mockito.when(repository.getHTTPConnection(INTERNAL_JAXREST_TOKENSERVICE, true)).thenReturn(connection);
		String payload = Base64.getEncoder().encodeToString("{\"admin\":false,\"sub\":\"way\"}".getBytes());
		String jwtWrap = "{\"idToken\":\"header." + payload + ".signature\"}";
		InputStream inputStream = new ByteArrayInputStream(jwtWrap.getBytes());
		Mockito.when(connection.getInputStream()).thenReturn(inputStream);
		JwtClaim jwtClaim = jwtReader.readClaim(repository);
		Assert.assertFalse("Sent admin as false and expecting it to be set correctly", jwtClaim.isAdmin());
		Assert.assertEquals("added 'way' as 'sub' entry of the payload but it is gone..", "way", jwtClaim.getSub());

		payload = Base64.getEncoder().encodeToString("{\"admin\":true,\"sub\":\"aru\"}".getBytes());
		jwtWrap = "{\"idToken\":\"header." + payload + ".signature\"}";
		inputStream = new ByteArrayInputStream(jwtWrap.getBytes());
		Mockito.when(connection.getInputStream()).thenReturn(inputStream);
		jwtClaim = jwtReader.readClaim(repository);
		Assert.assertTrue("Sent admin as true and expecting it to be set correctly", jwtClaim.isAdmin());
		Assert.assertEquals("added 'aru' as 'sub' entry of the payload but it is gone..", "aru", jwtClaim.getSub());
	}
}
