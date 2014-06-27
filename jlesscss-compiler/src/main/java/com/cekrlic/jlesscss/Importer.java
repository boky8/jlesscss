package com.cekrlic.jlesscss;

import java.io.IOException;
import java.util.List;

/**
 * Importer is a utility library that is called when the LESS file does the <tt>@import</tt>. This allows you
 * to implement the import in the context-insensitive way
 */
public interface Importer {

	public static interface FileImportedCallback {
		public void fileImported(String path);
	}

	/**
	 * Populate this list when the file is imported.
	 */
	public static class ListFileImporterCallback implements FileImportedCallback {
		private final List<String> files;

		public ListFileImporterCallback(List<String> files) {
			this.files = files;
		}

		@Override
		public void fileImported(String path) {
			files.add(path);
		}
	}


	/**
	 * Set the callback to be called when the file is imported
	 * @param callback The callback.
	 */
	public void setFileImportedCallback(FileImportedCallback callback);

	public Source importFile(String path) throws IOException;
}
