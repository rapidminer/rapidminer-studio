/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.versioned.datasummary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.operator.ports.metadata.ConnectionInformationMetaData;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.tools.encryption.EncryptionProvider;
import com.rapidminer.versioning.repository.DataSummary;

/**
 * {@link DataSummarySerializer} for {@link com.rapidminer.connection.ConnectionInformation ConnectionInformation},
 * i.e. for (de)serializing {@link com.rapidminer.connection.configuration.ConnectionConfiguration ConnectionConfiguration}.
 * Utilizes {@link ConnectionInformationSerializer} and uses the suffix {@link #CON_INFO_SUFFIX}.
 * <p>
 * Like previous meta data handling for connections, this is done asymmetrically, i.e. connection meta data is written
 * to disk encrypted with the {@link EncryptionProvider#DEFAULT_CONTEXT local encryption context}, but read with
 * a {@code null} encryption context to prevent leaking encrypted values through the meta data.
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public enum ConnectionDataSummarySerializer implements DataSummarySerializer {

	INSTANCE;

	/** Same as {@value ConnectionEntry#CON_SUFFIX} minus the leading dot */
	public static final String CON_INFO_SUFFIX = ConnectionEntry.CON_SUFFIX.replaceFirst("^\\.", "");

	@Override
	public String getSuffix() {
		return CON_INFO_SUFFIX;
	}

	@Override
	public Class<? extends DataSummary> getSummaryClass() {
		return ConnectionInformationMetaData.class;
	}

	@Override
	public void serialize(Path path, DataSummary dataSummary) throws IOException {
		if (!(dataSummary instanceof ConnectionInformationMetaData)) {
			// noop
			return;
		}
		try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			ConnectionInformationSerializer.INSTANCE
					.writeJson(out, ((ConnectionInformationMetaData) dataSummary).getConfiguration(), EncryptionProvider.DEFAULT_CONTEXT);
		}
	}

	@Override
	public DataSummary deserialize(Path path) throws IOException {
		try (InputStream inputStream = Files.newInputStream(path)) {
			return new ConnectionInformationMetaData(ConnectionInformationSerializer.INSTANCE
					.loadConfiguration(inputStream, null));
		}
	}
}
