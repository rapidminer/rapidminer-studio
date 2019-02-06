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
package com.rapidminer.io.process.rules;

import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.SimpleOperatorChain;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPorts;

import java.util.Collections;
import java.util.List;


/**
 *
 * @author Sebastian Land
 */
public class PassthroughShortcutRule extends AbstractGenericParseRule {

	private static final VersionNumber APPLIES_UNTIL = new VersionNumber(5, 0, 0, null);

	@Override
	public String apply(final Operator operator, VersionNumber processVersion, final XMLImporter importer) {
		if (operator.getClass().equals(SimpleOperatorChain.class)
				&& (processVersion == null || processVersion.compareTo(APPLIES_UNTIL) < 0)) {
			importer.doAfterAutoWire(new Runnable() {

				@Override
				public void run() {
					OperatorChain chain = (OperatorChain) operator;
					InputPorts inputs = chain.getInputPorts();
					OutputPorts sources = chain.getSubprocess(0).getInnerSources();
					InputPorts sinks = chain.getSubprocess(0).getInnerSinks();
					OutputPorts outputs = chain.getOutputPorts();
					boolean found;
					do {
						found = false;
						for (int leftIndex = 0; leftIndex < sources.getNumberOfPorts(); leftIndex++) {
							OutputPort source = sources.getPortByIndex(leftIndex);
							if (!source.isConnected()) {
								continue;
							}
							InputPort sink = source.getDestination();
							if (sinks.getAllPorts().contains(sink)) {
								int rightIndex = sinks.getAllPorts().indexOf(sink);
								InputPort correspondingInput = inputs.getPortByIndex(leftIndex);
								OutputPort correspondingOutput = outputs.getPortByIndex(rightIndex);
								if (correspondingInput.isConnected() && correspondingOutput.isConnected()) {
									OutputPort originalSource = correspondingInput.getSource();
									InputPort finalDestination = correspondingOutput.getDestination();
									originalSource.lock();
									finalDestination.lock();

									originalSource.disconnect();
									source.disconnect();
									correspondingOutput.disconnect();

									originalSource.connectTo(finalDestination);

									originalSource.unlock();
									finalDestination.unlock();
									found = true;

									importer.addMessage("The connection from <code>" + source.getSpec()
											+ "</code> to <code>" + sink
											+ "</code> was replaced by the direct connection from <code>"
											+ originalSource.getSpec() + "</code> to <code>" + finalDestination.getSpec()
											+ "</code>.");
								}
							}
						}
					} while (found);
				}
			});
		}
		return null;
	}

	@Override
	public List<String> getApplicableOperatorKeys() {
		return Collections.singletonList("subprocess");
	}

}
