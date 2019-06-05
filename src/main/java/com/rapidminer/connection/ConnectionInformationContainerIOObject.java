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
package com.rapidminer.connection;

import java.io.IOException;

import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Tools;


/**
 * Container to pass the {@link ConnectionInformation} around as an {@link com.rapidminer.operator.IOObject IOObject}.
 *
 * @author Andreas Timm
 * @since 9.3
 */
public class ConnectionInformationContainerIOObject extends ResultObjectAdapter {

	// the contained connection information
	private transient ConnectionInformation connectionInformation;

	/**
	 * Set this container up with the given {@link ConnectionInformation}.
	 */
	public ConnectionInformationContainerIOObject(ConnectionInformation connectionInformation) {
		this.connectionInformation = connectionInformation;
	}

	/**
	 * Access the {@link ConnectionInformation}
	 *
	 * @return the connection information
	 */
	public ConnectionInformation getConnectionInformation() {
		return connectionInformation;
	}

	@Override
	public Annotations getAnnotations() {
		return connectionInformation.getAnnotations();
	}

	@Override
	public ConnectionInformationContainerIOObject copy() {
		ConnectionInformationBuilder copyBuilder;
		try {
			copyBuilder = new ConnectionInformationBuilder(connectionInformation);
		} catch (IOException e) {
			return null;
		}
		return new ConnectionInformationContainerIOObject(copyBuilder.build());
	}

	@Override
	public String toString() {
		ConnectionConfiguration configuration = connectionInformation.getConfiguration();
		if (configuration == null) {
			return "Empty connection";
		}
		return "Connection: " + configuration.getName() + " of type " + configuration.getType();
	}

	@Override
	public String toResultString() {
		ConnectionConfiguration configuration = connectionInformation.getConfiguration();
		if (configuration == null) {
			return "Empty connection";
		}
		String result = "<b>Name:</b> " + configuration.getName() + "<br/><br/><b>Type:</b> ";
		String connectionType = configuration.getType();
		boolean typeKnown = ConnectionHandlerRegistry.getInstance().isTypeKnown(connectionType);
		if (typeKnown) {
			String icon = ConnectionI18N.getConnectionIconName(connectionType);
			java.net.URL url = Tools.getResource("icons/16/" + icon);
			if (url != null) {
				result += "<img src=\"" + url + "\"/> ";
			}
			result += ConnectionI18N.getTypeName(configuration.getType());
		} else {
			result += I18N.getGUILabel("connection.unknown_type.label");
		}
		result += "<br/><br/><b>Location:</b> " + RepositoryLocation.REPOSITORY_PREFIX;
		Repository repository = connectionInformation.getRepository();
		if (repository == null) {
			return result;
		}
		return result + repository.getName();
	}

	@Override
	public String getName() {
		return "Connection";
	}

}
