package com.rapidminer.repository.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;

import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.LogService;


/**
 * A read-only resource based {@link BlobEntry}.
 *
 * @author Jan Czogalla
 * @since 9.0
 */
public class ResourceBlobEntry extends ResourceDataEntry implements BlobEntry {

	private String mimeType;

	protected ResourceBlobEntry(ResourceFolder parent, String name, String resource, ResourceRepository repository) {
		super(parent, name, resource, repository);
	}

	@Override
	public InputStream openInputStream() throws RepositoryException {
		return getResourceStream(".blob");
	}

	@Override
	public OutputStream openOutputStream(String mimeType) throws RepositoryException {
		throw new RepositoryException("Repository is read only.");
	}

	@Override
	public String getMimeType() {
		if (mimeType == null) {
			try (InputStream in = getResourceStream(".properties")) {
				Properties props = new Properties();
				props.loadFromXML(in);
				mimeType = props.getProperty("mimetype");
			} catch (IOException | RepositoryException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.repository.resource.ZipResourceBlob.failed_to_load_mimetype", getName());
			}
		}
		return mimeType;
	}

	@Override
	public String getDescription() {
		return getName();
	}
}
