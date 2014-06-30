package com.cekrlic.jlesscss;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Read data from a on-disk file.
 * @author boky
 * @created 27.6.2014 12:11
 */
public class FileSource implements Source {
	private final Path path;
	private final String content;
	private final String fileName;

	public FileSource(File file) throws IOException {
		this(file.toPath());
	}

	public FileSource(Path path) throws IOException {
		this.path = path;
		fileName = path.toFile().getName();
		content = new String(Files.readAllBytes(path), "UTF-8");
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
		return new FileSystemImporter(path);
	}
}
