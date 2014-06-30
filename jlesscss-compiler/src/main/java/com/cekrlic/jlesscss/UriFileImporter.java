package com.cekrlic.jlesscss;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Import files from URIs.
 * @author boky
 * @created 30.6.2014 12:46
 */
public class UriFileImporter implements Importer {
	private final URI reference;
	private FileImportedCallback callback;

	public UriFileImporter(URI reference) {
		this.reference = reference;
	}

	public UriFileImporter(URL reference) throws URISyntaxException {
		this(reference.toURI());
	}

	@Override
	public void setFileImportedCallback(FileImportedCallback callback) {
		this.callback = callback;
	}

	@Override
	public Source importFile(String path) throws IOException {
		if(callback!=null) { callback.fileImported(path); }
		return new UriSource(reference.resolve(path));
	}
}
