package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;

public class NullSynchronizer implements SynchronizerInterface {

    public NullSynchronizer() {
    }

    public boolean isConfigured() {
        return true;
    }

    public void putRemoteFile(String filename, String contents) {
    }

    public BufferedReader getRemoteFile(String filename) {
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