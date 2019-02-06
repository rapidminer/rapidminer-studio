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
package com.rapidminer.operator.tools;

import com.rapidminer.tools.XMLSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * 
 * @author Simon Fischer
 * 
 */
class JavaBinaryBodySerializer implements BodySerializer {

	@SuppressWarnings("resource")
	@Override
	public Object deserialize(InputStream in) throws IOException {
		ObjectInputStream oin = new RMObjectInputStream(in);
		try {
			return oin.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException("Deserialized unknown class: " + e, e);
		}
	}

	@Override
	public void serialize(Object object, OutputStream out) throws IOException {
		ObjectOutputStream oout = new ObjectOutputStream(out);
		oout.writeObject(object);
	}
}


class XMLBodySerializer implements BodySerializer {

	@Override
	public Object deserialize(InputStream in) throws IOException {
		XMLSerialization xmlSerialization = XMLSerialization.getXMLSerialization();
		if (xmlSerialization == null) {
			throw new IOException("XML Serialization not initialized.");
		}
		return xmlSerialization.fromXML(in);
	}

	@Override
	public void serialize(Object object, OutputStream out) throws IOException {
		XMLSerialization xmlSerialization = XMLSerialization.getXMLSerialization();
		if (xmlSerialization == null) {
			throw new IOException("XML Serialization not initialized.");
		}
		xmlSerialization.writeXML(object, out);
	}
}


class GZippedXMLBodySerializer extends XMLBodySerializer {

	@Override
	public Object deserialize(InputStream in) throws IOException {
		return super.deserialize(new GZIPInputStream(in));
	}

	@Override
	public void serialize(Object object, OutputStream out) throws IOException {
		super.serialize(new GZIPOutputStream(out), out);
	}
}


/**
 * Encapsulates some standard ways to serialize and deserialize objects from streams.
 * 
 * NOTE: When adding new enum constants, don't change the ordering of the old, since the ordinal
 * value will be used to identify the serialization type.
 * 
 * @author Simon Fischer
 * 
 */
public enum SerializationType {

	/** Plain binary java serialization using Object{In/Out}putStream. */
	JAVA_BINARY(new JavaBinaryBodySerializer()),

	/** Uses xstream XML serialization. */
	XML(new XMLBodySerializer()),

	/** Uses xstream XML serialization wrapped in a GZip stream. */
	XML_ZIPPED(new GZippedXMLBodySerializer()),

	STREAMED_EXAMPLE_SET_DENSE(new StreamedExampleSetBodySerializer(ExampleSetToStream.VERSION_1)),

	STREAMED_EXAMPLE_SET_DENSE_2(new StreamedExampleSetBodySerializer(ExampleSetToStream.VERSION_2)),

	STREAMED_EXAMPLE_SET_DENSE_3(new StreamedExampleSetBodySerializer(ExampleSetToStream.VERSION_3));

	public static SerializationType STREAMED_EXAMPLE_SET_DENSE_CURRENT_VERSION = STREAMED_EXAMPLE_SET_DENSE_3;

	private BodySerializer bodySerializer;

	private SerializationType(BodySerializer bodySerializer) {
		this.bodySerializer = bodySerializer;
	}

	public BodySerializer getBodySerializer() {
		return bodySerializer;
	}
}
