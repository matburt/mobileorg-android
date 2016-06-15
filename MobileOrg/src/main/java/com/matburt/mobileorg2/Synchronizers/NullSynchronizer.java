package com.matburt.mobileorg2.Synchronizers;

import android.content.Context;

import java.io.BufferedReader;
import java.util.HashSet;

public class NullSynchronizer implements SynchronizerInterface {

    public NullSynchronizer() {
    }

    @Override
    public String getFilesDir() {
        return null;
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
    public HashSet<String> synchronize() {

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