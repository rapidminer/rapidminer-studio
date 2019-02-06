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
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOMultiplier;
import com.rapidminer.operator.IOMultiplyOperator;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;


/**
 * Replaces the old {@link IOMultiplyOperator} by a number of {@link IOMultiplier}s.
 *
 * @author Simon Fischer
 *
 */
public class ReplaceIOMultiplierRule extends AbstractGenericParseRule {

	private static final VersionNumber APPLIES_UNTIL = new VersionNumber(5, 0, 0, null);

	@Override
	public String apply(final Operator oldMultiplier, VersionNumber processVersion, final XMLImporter importer) {
		if (processVersion == null || processVersion.compareTo(APPLIES_UNTIL) < 0) {
			if (oldMultiplier.getClass().equals(IOMultiplyOperator.class)) {
				importer.doAfterAutoWire(new Runnable() {

					@Override
					public void run() {
						int count = 0;
						ExecutionUnit unit = oldMultiplier.getExecutionUnit();
						int oldIndex = unit.getOperators().indexOf(oldMultiplier);
						int numOutports;
						try {
							numOutports = oldMultiplier.getParameterAsInt(IOMultiplyOperator.PARAMETER_NUMBER_OF_COPIES) + 1;
						} catch (UndefinedParameterError e) {
							importer.addMessage("<div class=\"error\">Cannot replace <code>IOMultiplier</code>. Parameter <var>"
									+ IOMultiplyOperator.PARAMETER_NUMBER_OF_COPIES + "</var> is not set.</div>");
							return;
						}
						OutputPort[] sources = new OutputPort[oldMultiplier.getInputPorts().getNumberOfPorts()];
						InputPort[][] sinks = new InputPort[oldMultiplier.getInputPorts().getNumberOfPorts()][numOutports];
						// copy sources and destinations
						for (int i = 0; i < sources.length; i++) {
							InputPort in = oldMultiplier.getInputPorts().getPortByIndex(i);
							if (in.isConnected()) {
								sources[i] = in.getSource();
								sources[i].lock();
								for (int j = 0; j < numOutports; j++) {
									sinks[i][j] = oldMultiplier.getOutputPorts().getPortByIndex(i * numOutports + j)
											.getDestination();
									if (sinks[i][j] != null) {
										sinks[i][j].lock();
									}
								}
							}
						}
						oldMultiplier.remove();
						for (int i = 0; i < sources.length; i++) {
							try {
								if (sources[i] != null) {
									IOMultiplier newMultiplier = OperatorService.createOperator(IOMultiplier.class);
									unit.addOperator(newMultiplier, oldIndex);
									count++;
									newMultiplier.rename(oldMultiplier.getName() + "_" + count);
									sources[i].connectTo(newMultiplier.getInputPorts().getPortByIndex(0));
									for (int j = 0; j < numOutports; j++) {
										if (sinks[i][j] != null) {
											newMultiplier.getOutputPorts().getPortByIndex(j).connectTo(sinks[i][j]);
											sinks[i][j].unlock();
										}
									}
									sources[i].unlock();
								}
							} catch (OperatorCreationException e) {
								importer.addMessage("<div class=\"error\">Cannot replace <code>IOMultiplier</code>. Cannot create <code>IOMultiplier2</code>.");
								// LogService.getRoot().log(Level.WARNING,
								// "Cannot create IOMultiplier2: " + e, e);
								LogService
										.getRoot()
										.log(Level.WARNING,
												I18N.getMessage(
														LogService.getRoot().getResourceBundle(),
														"com.rapidminer.io.process.rules.ReplaceIOMultiplierRule.creating_iomultiplier2_error",
														e), e);

								return;
							}
						}
						importer.addMessage("Replaced <code>IOMultiplier</code> '<var>" + oldMultiplier.getName()
								+ "</var>' by " + count + " <code>IOMultiplier2</code>.");
					}
				});
			}
		}
		return null;
	}

	@Override
	public List<String> getApplicableOperatorKeys() {
		return Collections.singletonList("iomultiplier");
	}
}
