package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLHandshakeException;

public interface SynchronizerInterface {

	/**
	 * Called before running the synchronizer to ensure that it's configuration
	 * is in a valid state.
	 */
	public boolean isConfigured();

	/**
	 * Called before running the synchronizer to ensure it can connect.
	 */
	public boolean isConnectable();
	
	/**
	 * Replaces the file on the remote end with the given content.
	 * 
	 * @param filename Name of the file, without path
	 * @param contents Content of the new file
	 */
	public void putRemoteFile(String filename, String contents)
        throws IOException;

	/**
	 * Returns a BufferedReader to the remote file.
	 * 
	 * @param filename
	 *            Name of the file, without path
	 */
	public BufferedReader getRemoteFile(String filename)
        throws IOException, CertificateException, SSLHandshakeException;

	/**
	 * Use this to disconnect from any services and cleanup.
	 */
	public void postSynchronize();
}
