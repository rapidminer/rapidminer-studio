/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.io.process.rules;

import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.io.IOContainerWriter;
import com.rapidminer.operator.io.IOObjectWriter;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.XMLException;

import java.util.logging.Level;

import org.w3c.dom.Element;


/**
 * 
 * @author Sebastian Land
 */
public class ReplaceIOContainerWriter extends AbstractParseRule {

	public ReplaceIOContainerWriter(Element element) throws XMLException {
		super("iocontainerwriter", element);
	}

	@Override
	protected String apply(final Operator operator, String operatorTypeName, final XMLImporter importer) {
		if (operator instanceof IOContainerWriter) {
			importer.doAfterAutoWire(new Runnable() {

				@Override
				public void run() {
					IOContainerWriter ioContainerWriter = (IOContainerWriter) operator;
					String filenameBase;
					try {
						filenameBase = ioContainerWriter.getParameterAsString(IOContainerWriter.PARAMETER_FILENAME);
					} catch (UndefinedParameterError e1) {
						filenameBase = ioContainerWriter.getName();
					}
					int num = 0;
					for (int i = 0; i < ioContainerWriter.getInputPorts().getNumberOfPorts(); i++) {
						InputPort input = ioContainerWriter.getInputPorts().getPortByIndex(i);
						if (input.isConnected()) {
							OutputPort source = input.getSource();
							input.lock();
							source.lock();
							try {
								source.disconnect();
								IOObjectWriter writer = OperatorService.createOperator(IOObjectWriter.class);
								writer.setParameter(IOObjectWriter.PARAMETER_OBJECT_FILE, filenameBase + "_" + (i + 1));
								source.connectTo(writer.getInputPorts().getPortByIndex(0));

								OutputPort output = ioContainerWriter.getOutputPorts().getPortByIndex(i);
								if (output.isConnected()) {
									InputPort dest = output.getDestination();
									output.lock();
									dest.lock();
									try {
										output.disconnect();
										writer.getOutputPorts().getPortByIndex(0).connectTo(dest);
									} finally {
										output.unlock();
										dest.unlock();
									}
								}
								num++;
							} catch (Exception e) {
								// LogService.getRoot().log(Level.WARNING,
								// "Cannot insert IOObjectWriter: "+e, e);
								LogService
										.getRoot()
										.log(Level.WARNING,
												I18N.getMessage(
														LogService.getRoot().getResourceBundle(),
														"com.rapidminer.io.process.rules.ReplaceIOContainerWriter.inserting_ioobjectwriter_error",
														e), e);
								importer.addMessage("<em class=\"error\">Error while replacing "
										+ ioContainerWriter.getName() + ": " + e + "</em>");
							} finally {
								input.unlock();
								source.unlock();
							}
						}
					}
					ioContainerWriter.remove();
					importer.addMessage("Replaced <var>" + ioContainerWriter.getName() + "</var> (<code>"
							+ ioContainerWriter.getOperatorDescription().getName() + "</code>) by " + num
							+ " <code>IOObjectWriter</code>s</span>");
				}
			});
		}
		return null;
	}

}
