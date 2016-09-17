package com.matburt.mobileorg.synchronizers;

import android.content.Context;

import java.io.BufferedReader;

public class NullSynchronizer extends Synchronizer {

    public NullSynchronizer(Context context) {
        super(context);
    }

    @Override
    public String getRelativeFilesDir() {
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
    public SyncResult synchronize() {
        return new SyncResult();
    }


    @Override
	public void postSynchronize() {
    }

    @Override
    public void addFile(String filename) {

    }

    @Override
    public boolean isConnectable() {
		return true;
	}
}