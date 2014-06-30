package com.cekrlic.jlesscss;

import java.io.IOException;

/**
 * Importer with no contact with the outside world.
 */
public class NullImporter implements Importer {

	@Override
	public void setFileImportedCallback(FileImportedCallback callback) {

	}

	@Override
	public Source importFile(String path) throws IOException {
		throw new IOException("No way to import things!");
	}
}
