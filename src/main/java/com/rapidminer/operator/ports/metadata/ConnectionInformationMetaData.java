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
package com.rapidminer.operator.ports.metadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationImpl;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.tools.I18N;


/**
 * MetaData object for {@link com.rapidminer.connection.ConnectionInformation} contains the complete configuration.
 *
 * @author Andreas Timm
 * @since 9.2
 */
public class ConnectionInformationMetaData extends MetaData {

	/**
	 * Tag delimiter
	 */
	private static final String TAG_DELIMITER = ", ";
	/**
	 * Maximum description length, about four lines
	 */
	static final int DESCRIPTION_PREVIEW_LENGTH = 300;
	/**
	 * used if no configuration is available
	 */
	static final ConnectionConfiguration UNKNOWN_CONNECTION = new ConnectionConfigurationImpl(I18N.getGUILabel("connection.type.unknown.label"), "unknown");

	// Use this configuration object to show its content as metadata
	private ConnectionConfiguration configuration;

	/**
	 * Constructor required by {@link MetaData#clone()}
	 */
	public ConnectionInformationMetaData() {
		super(ConnectionInformationContainerIOObject.class);
	}

	/**
	 * Create a new {@link ConnectionInformationMetaData} instance with the given {@link ConnectionConfiguration} to
	 * show as its content
	 *
	 * @param connectionConfiguration
	 * 		will be kept as a reference to show its content
	 */
	public ConnectionInformationMetaData(ConnectionConfiguration connectionConfiguration) {
		this();
		configuration = connectionConfiguration;
	}

	/**
	 * Create a new {@link ConnectionInformationMetaData} instance with the given {@link ConnectionConfiguration} to
	 * show as its content
	 *
	 * @param object
	 * 		will be kept as a reference to show its content
	 * @param ignored
	 * 		not used
	 * @see MetaDataFactory#registerIOObjectMetaData(Class, Class)
	 */
	public ConnectionInformationMetaData(ConnectionInformationContainerIOObject object, boolean ignored) {
		this(object.getConnectionInformation().getConfiguration());
	}

	/**
	 * Returns the type of the connection. Might return {@code null}.
	 */
	public String getConnectionType() {
		return configuration == null ? null : configuration.getType();
	}

	@Override
	public String getDescription() {
		final StringBuilder builder = new StringBuilder(super.getDescription());
		ConnectionConfiguration config = Optional.ofNullable(getConfiguration()).orElse(UNKNOWN_CONNECTION);
		String tags = String.join(TAG_DELIMITER, Optional.ofNullable(config.getTags()).orElseGet(Collections::emptyList)).trim();
		String description = StringUtils.abbreviate(Objects.toString(config.getDescription(), "").trim(), DESCRIPTION_PREVIEW_LENGTH);

		if (!description.isEmpty()) {
			builder.append("<p style='margin-top:2px;margin-bottom:3px;'>").append(description).append("</p>");
		}

		if (!tags.isEmpty()) {
			builder.append("<p style='margin-top:2px;'>");
			builder.append(I18N.getGUILabel("connection.metadata.connection_type.tags.label")).append(" ");
			builder.append(tags).append("</p>");
		}

		return builder.toString();
	}

	@Override
	protected String getTitleForDescription() {
		final StringBuilder builder = new StringBuilder();
		ConnectionConfiguration config = Optional.ofNullable(getConfiguration()).orElse(UNKNOWN_CONNECTION);
		// Prevent the enclosing <p> tag in repository view from being empty (= line break)
		builder.append("<style><!-- p {display:block;} --></style>");
		builder.append("<table cellspacing=0 cellpadding=0 style='margin-top:2px;' ><tr>");
		builder.append("<td valign=middle width=20 ><img width=16 height=16 src='icon:///16/").append(ConnectionI18N.getConnectionIconName(config.getType())).append("' />").append("</td>");
		builder.append("<td>");
		builder.append(I18N.getGUIMessage("gui.label.connection.metadata.connection_type.message", "" + config.getName(), "" + ConnectionI18N.getTypeName(config.getType())));
		builder.append("</td>");
		builder.append("</tr></table>");
		return builder.toString();
	}

	/**
	 * Required for Java serialization, this is a special @Override style..
	 *
	 * @param out
	 * 		writing to this stream
	 * @throws IOException
	 * 		could not write
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		ConnectionInformationSerializer.LOCAL.writeJson(out, configuration);
	}

	/**
	 * Required for Java serialization, this is a special @Override style..
	 *
	 * @param in
	 * 		reading from this stream
	 * @throws IOException
	 * 		could not read
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		configuration = ConnectionInformationSerializer.LOCAL.loadConfiguration(in);
	}

	@Override
	public ConnectionInformationMetaData clone() {
		ConnectionInformationMetaData cimdClone = (ConnectionInformationMetaData) super.clone();
		if (configuration == null) {
			return cimdClone;
		}
		try {
			// keep only a copy of the configuration
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ConnectionInformationSerializer.LOCAL.writeJson(baos, configuration);
			cimdClone.configuration = ConnectionInformationSerializer.LOCAL.loadConfiguration(new ByteArrayInputStream(baos.toByteArray()));
		} catch (IOException e) {
			throw new RuntimeException("Cloning the connection configuration failed", e);
		}
		return cimdClone;
	}

	/**
	 * Gets the connection configuration
	 *
	 * @return the configuration object
	 */
	public ConnectionConfiguration getConfiguration() {
		return configuration;
	}
}
