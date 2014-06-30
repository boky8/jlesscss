package com.cekrlic.jlesscss;

/**
 * In-memory LESS source file.
 * @author boky
 * @created 27.6.2014 11:52
 */
public class MemorySource implements Source {


	private final String fileName;
	private final String content;

	public MemorySource(final String fileName, final String content) {
		this.fileName = fileName;
		this.content = content;
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@Override
	public String getContent() {
		return content;
	}

	@Override
	public Importer getImporter() {
		return new NullImporter();
	}
}
