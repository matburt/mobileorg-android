package com.matburt.mobileorg2.Synchronizers;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.HashSet;

public abstract class Synchronizer {

	abstract public String getRelativeFilesDir();

	public String getAbsoluteFilesDir(Context context) {
		return context.getFilesDir() + "/" + getRelativeFilesDir();
	}

	/**
	 * Delete all files from the synchronized repository
	 * except repository configuration files
	 * @param context
	 */
	public void clearRepository(Context context) {
		File dir = new File(getAbsoluteFilesDir(context));
		for (File file : dir.listFiles()) {
			file.delete();
		}
	}

	/**
	 * Called before running the synchronizer to ensure that it's configuration
	 * is in a valid state.
	 */
	abstract boolean isConfigured();

	/**
	 * Called before running the synchronizer to ensure it can connect.
	 */
	abstract boolean isConnectable();
	
	/**
	 * Replaces the file on the remote end with the given content.
	 * 
	 * @param filename Name of the file, without path
	 * @param contents Content of the new file
	 */
	abstract void putRemoteFile(String filename, String contents)
			throws IOException;

	/**
	 * Returns a BufferedReader to the remote file.
	 * 
	 * @param filename
	 *            Name of the file, without path
	 */
	abstract BufferedReader getRemoteFile(String filename)
			throws IOException, CertificateException;

	abstract HashSet<String> synchronize();


	/**
	 * Use this to disconnect from any services and cleanup.
	 */
	abstract void postSynchronize();

	/**
	 * Synchronize a new file
	 *
	 * @param filename
	 */
	abstract public void addFile(String filename);

}
