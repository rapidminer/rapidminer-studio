/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
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
package com.rapidminer.connection.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JPanel;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.gui.components.ConnectionInfoPanel;
import com.rapidminer.connection.gui.model.ConnectionModelConverter;
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.report.Reportable;
import com.rapidminer.repository.Repository;


/**
 * Renderer for {@link ConnectionInformationContainerIOObject}s that reuses the {@link ConnectionInfoPanel}.
 *
 * @author Gisa Meier
 * @since 9.3.0
 */
public class ConnectionInformationRenderer extends AbstractRenderer {

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		return null;
	}

	@Override
	public String getName() {
		return "Connection";
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		ConnectionInformationContainerIOObject connectionObject = (ConnectionInformationContainerIOObject) renderable;
		ConnectionInformation connectionInformation = connectionObject.getConnectionInformation();
		Repository repository = connectionInformation.getRepository();

		ConnectionInfoPanel connectionInfoPanel =
				new ConnectionInfoPanel(ConnectionModelConverter.fromConnection(connectionInformation,
						repository != null ? repository.getLocation() : null,false), false);
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(connectionInfoPanel, BorderLayout.NORTH);
		return panel;
	}


}
