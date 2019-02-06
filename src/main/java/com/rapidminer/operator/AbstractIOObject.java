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
package com.rapidminer.operator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.ProcessingStep;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.XMLSerialization;


/**
 * This is an abstract superclass for all IOObject. It provides basic implementations for all
 * methods of the IOObject interface. In addition, it also provides static methods which can be used
 * for reading IOObjects from XML strings and input streams / files containing the XML
 * serialization.
 * 
 * @author Ingo Mierswa
 */
public abstract class AbstractIOObject implements IOObject {

	private static final long serialVersionUID = 7131412868947165460L;

	/** The source of this IOObect. Might be null. */
	private String source = null;

	/** The current working operator. */
	private transient LoggingHandler loggingHandler;

	private transient LinkedList<ProcessingStep> processingHistory = new LinkedList<>();

	private transient HashMap<String, Object> userData = new HashMap<>();

	/** Sets the source of this IOObject. */
	@Override
	public void setSource(String sourceName) {
		this.source = sourceName;
	}

	/** Returns the source of this IOObject (might return null if the source is unknown). */
	@Override
	public String getSource() {
		return source;
	}

	@Override
	public void appendOperatorToHistory(Operator operator, OutputPort port) {
		if (operator.getProcess() == null) {
			return;
		}
		if (processingHistory == null) {
			processingHistory = new LinkedList<>();
		}
		ProcessingStep newStep = new ProcessingStep(operator, port);
		if (processingHistory.isEmpty() || !processingHistory.getLast().equals(newStep)) {
			processingHistory.add(newStep);
		}
	}

	@Override
	public List<ProcessingStep> getProcessingHistory() {
		if (processingHistory == null) {
			processingHistory = new LinkedList<>();
		}
		return processingHistory;
	}

	/**
	 * Gets the logging associated with the operator currently working on this IOObject or the
	 * global log service if no operator was set.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public LoggingHandler getLog() {
		if (this.loggingHandler != null) {
			return this.loggingHandler;
		} else {
			return LogService.getGlobal();
		}
	}

	/**
	 * Sets the current working operator, i.e. the operator which is currently working on this
	 * IOObject. This might be used for example for logging.
	 */
	@Override
	public void setLoggingHandler(LoggingHandler loggingHandler) {
		this.loggingHandler = loggingHandler;
	}

	/**
	 * Returns not a copy but the very same object. This is ok for IOObjects which cannot be altered
	 * after creation. However, IOObjects which might be changed (e.g.
	 * {@link com.rapidminer.example.ExampleSet}s) should overwrite this method and return a proper
	 * copy.
	 */
	@Override
	public IOObject copy() {
		return this;
	}

	/**
	 * Initializes the writing of this object. This method is invoked before the actual writing is
	 * performed. The default implementation does nothing.
	 * 
	 * This method should also be used for clean up processes which should be performed before the
	 * actual writing is done. For example, models might decide to keep the example set information
	 * directly after learning (e.g. for visualization reasons) but not to write them down. Please
	 * note that all fields will be written into files unless they are set to null in this method or
	 * they are marked as transient.
	 */
	protected void initWriting() {}

	/**
	 * Just serializes this object with help of a {@link XMLSerialization}. Initializes
	 * {@link #initWriting()} before the actual writing is performed.
	 */
	@Override
	public final void write(OutputStream out) throws IOException {
		initWriting();
		XMLSerialization.getXMLSerialization().writeXML(this, out);
	}

	@Override
	public Object getUserData(String key) {
		if (userData == null) {
			userData = new HashMap<>();
		}
		return userData.get(key);
	}

	@Override
	public Object setUserData(String key, Object value) {
		if (userData == null) {
			userData = new HashMap<>();
		}
		return userData.put(key, value);
	}

	/**
	 * Deserializes an IOObect from the given XML stream. TODO: Make private and remove deprecated
	 * annotation
	 * 
	 * @throws IOException
	 *             if any IO error occurs.
	 * @throws IllegalStateException
	 *             if {@link XMLSerialization#init(ClassLoader)} has never been called.
	 * @deprecated Use {@link #read(InputStreamProvider)} to be able to read all formats
	 *             (xml zipped/not zipped and binary)
	 */
	@Deprecated
	public static IOObject read(InputStream in) throws IOException {
		final XMLSerialization serializer = XMLSerialization.getXMLSerialization();
		if (serializer == null) {
			throw new IllegalStateException(
					"XMLSerialization not initialized, please invoke XMLSerialization.init(ClassLoader) before using this method.");
		}
		return (IOObject) serializer.fromXML(in);
	}

	/** This interface is needed since we must reset the stream in case of an exception. */
	public static interface InputStreamProvider {

		public InputStream getInputStream() throws IOException;
	}

	public static IOObject read(final File file) throws IOException {
		return read(() -> new FileInputStream(file));
	}

	public static IOObject read(final byte[] buf) throws IOException {
		return read(() -> new ByteArrayInputStream(buf));
	}

	public static IOObject read(InputStreamProvider inProvider) throws IOException {
		ObjectInputStream objectIn = null;
		try {
			// try if the object was written as a serializable object
			objectIn = new ObjectInputStream(inProvider.getInputStream());
			IOObject object = (IOObject) objectIn.readObject();
			objectIn.close(); // done in finally
			return object;
		} catch (Exception e) {
			// if not serialized, then try the usual serialization (xml)
			InputStream in = null;
			try {
				in = new GZIPInputStream(inProvider.getInputStream());
			} catch (IOException e1) {
				// do nothing and simply use the given input stream
				in = inProvider.getInputStream();
			}
			try {
				return read(in);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e1) {
					}
				}
			}
		} finally {
			if (objectIn != null) {
				try {
					objectIn.close();
				} catch (IOException e) {
				}
			}
		}
	}

}
