package com.cekrlic.jlesscss;

/**
 * Source for the less file to be compiled
 * @author boky
 * @created 27.6.2014 11:51
 */
public interface Source {

	/**
	 * Get the less file name
	 */
	public String getFileName();

	/**
	 * Get the less content
	 */
	public String getContent();

	/**
	 * Create an imported based on this source as a reference
	 * @return The importer
	 */
	public Importer getImporter();

}
