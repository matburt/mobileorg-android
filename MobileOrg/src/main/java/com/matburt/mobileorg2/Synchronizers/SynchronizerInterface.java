package com.matburt.mobileorg2.Synchronizers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CertificateException;

public interface SynchronizerInterface {

	/**
	 * Called before running the synchronizer to ensure that it's configuration
	 * is in a valid state.
	 */
	boolean isConfigured();

	/**
	 * Called before running the synchronizer to ensure it can connect.
	 */
	boolean isConnectable();
	
	/**
	 * Replaces the file on the remote end with the given content.
	 * 
	 * @param filename Name of the file, without path
	 * @param contents Content of the new file
	 */
	void putRemoteFile(String filename, String contents)
        throws IOException;

	/**
	 * Returns a BufferedReader to the remote file.
	 * 
	 * @param filename
	 *            Name of the file, without path
	 */
	BufferedReader getRemoteFile(String filename)
        throws FileNotFoundException, IOException, CertificateException;

	/**
	 * Use this to disconnect from any services and cleanup.
	 */
	void postSynchronize();
}
