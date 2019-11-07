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
package com.rapidminer.repository.internal.remote;

import com.rapidminer.tools.FunctionWithThrowable;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.function.Supplier;


/**
 * General container for the response of a request to RM Server. Can only be used for responses <= 2 Gibibyte.
 *
 * <strong>
 * Avoid using this if possible. Consume responses in the {@link BaseServerClient} implementation and return proper Objects instead.
 * </strong>
 *
 * @author Andreas Timm
 * @since 9.5.0
 */
public class ResponseContainer {
    private FunctionWithThrowable<Void, InputStream, IOException> inputStream;
    private FunctionWithThrowable<Void, Integer, IOException> responseCode;
    private FunctionWithThrowable<Void, String, IOException> responseMessage;
    private Supplier<String> contentType;
    private FunctionWithThrowable<Void, OutputStream, IOException> outputStream;

    /**
     * The ResponseContainer reads a {@link HttpURLConnection} to keep the data even if the connection does not exist
     * any longer. Can copy the {@link InputStream} unless keepOriginalStream is set to true.
     *
     * @param connection         to read and copy from
     * @param keepOriginalStream will forward access to the original URLConnection {@link InputStream}, reading this may
     *                           fail if the connection was closed in between.
     * @throws IOException         in case accessing the server failed technically
     */
    public ResponseContainer(HttpURLConnection connection, boolean keepOriginalStream) throws IOException {
        // we need to keep a copy here to hold the data even if the connection was closed
        if (connection.getDoOutput()) {
            outputStream = nil -> connection.getOutputStream();
            responseCode = nil -> connection.getResponseCode();
            responseMessage = nil -> connection.getResponseMessage();
            contentType = connection::getContentType;
        } else {
            int responseCd = connection.getResponseCode();
            responseCode = nil -> responseCd;
            String responseMsg = connection.getResponseMessage();
            responseMessage = nil -> responseMsg;
            String contentTyp = connection.getContentType();
            contentType = () -> contentTyp;
        }

        if (connection.getDoInput()) {
            // cannot write output after reading input, so this needs to keep the original
            if (connection.getDoOutput() || keepOriginalStream) {
                inputStream = nil -> connection.getInputStream();
            } else {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(IOUtils.toByteArray(connection.getInputStream()));
                inputStream = nil -> byteArrayInputStream;
                connection.disconnect();
            }
        }
    }

    /**
     * The original responseCode of the connection. See {@link HttpURLConnection#getResponseCode()}
     *
     * @return a HTTP status response code like 200
     */
    public int getResponseCode() {
        return responseCode.apply(null);
    }

    /**
     * Retrieve the original or cached content of the original InputStream. Will throw a caught Exception when copying the InputStream already led to an IOException.
     *
     * @return the {@link InputStream} to be consumed by the caller
     * @throws IOException in case accessing the {@link InputStream} fails
     */
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            throw new IOException("No inputstream available");
        }
        return inputStream.applyWithException(null);
    }

    /**
     * The original connection's responseMessage, see {@link HttpURLConnection#getResponseMessage()}
     *
     * @return the response of the connection
     */
    public String getResponseMessage() {
        return responseMessage.apply(null);
    }

    /**
     * Returns the value of the {@code content-type} header field.
     *
     * @return content type of the response, like "text/json"
     */
    public String getContentType() {
        return contentType.get();
    }

    /**
     * For writing purposes the {@link OutputStream} is referenced here directly, may be null in case the output was not requested from
     *
     * @return an outputStream to write to
     * @throws IOException if accessing the {@link OutputStream} fails
     */
    public OutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            throw new IOException("No outputstream available");
        }
        return outputStream.applyWithException(null);
    }
}