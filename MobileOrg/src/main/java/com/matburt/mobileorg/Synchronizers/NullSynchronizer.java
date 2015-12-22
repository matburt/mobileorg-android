package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.InputStream;

public class NullSynchronizer implements SynchronizerInterface {

    public NullSynchronizer() {
    }

    public boolean isConfigured() {
        return true;
    }

    @Override
    public void putRemoteFile(String filename, String contents) {
    }

    @Override
    public void putRemoteFile(String filename, InputStream contents) {
    }

    @Override
    public BufferedReader getRemoteFile(String filename) {
        return null;
    }

	@Override
	public InputStream getRemoteFileStream(String filename) {
		return null;
	}

	@Override
	public void postSynchronize() {
    }

	@Override
	public boolean isConnectable() {
		return true;
	}

}