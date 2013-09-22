package com.matburt.mobileorg.test.Synchronizers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.util.HashMap;

import javax.net.ssl.SSLHandshakeException;

import com.matburt.mobileorg.Synchronizers.SynchronizerInterface;

public class SynchronizerStub implements SynchronizerInterface {

	int putRemoteFileCount = 0;
	int getRemoteFileCount = 0;
	HashMap<String, String> files = new HashMap<String, String>();

	SynchronizerStub() {
	}

	@Override
	public boolean isConfigured() {
		return true;
	}

	@Override
	public void putRemoteFile(String filename, String contents)
			throws IOException {
		putRemoteFileCount++;
		addFile(filename, contents);
	}

	@Override
	public BufferedReader getRemoteFile(String filename) throws IOException,
			CertificateException, SSLHandshakeException {
		getRemoteFileCount++;
		String contents = files.get(filename);
		if(contents == null)
			throw new IOException("File \"" + filename + "\" not found");
		
		InputStream is = new ByteArrayInputStream(contents.getBytes());
		return new BufferedReader(new InputStreamReader(is));
	}

	@Override
	public void postSynchronize() {		
	}

	public void addFile(String filename, String contents) {
		files.remove(filename);
		files.put(filename, contents);
	}

	public void reset() {
		files = new HashMap<String, String>();
		putRemoteFileCount = 0;
		getRemoteFileCount = 0;
	}

	@Override
	public boolean isConnectable() {
		return true;
	}
}
