package com.cekrlic.jlesscss;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Import files from disk
 * @author boky
 * @created 27.6.2014 12:22
 */
public class FileSystemImporter implements Importer {
	private final Path reference;
	private FileImportedCallback callback;

	public FileSystemImporter(Path reference) {
		this.reference = reference;
	}


	@Override
	public void setFileImportedCallback(FileImportedCallback callback) {
		this.callback = callback;
	}

	@Override
	public Source importFile(String path) throws IOException {

		if(callback!=null) { callback.fileImported(path); }
		return new FileSource(reference.resolve(path));
	}
}
