package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;

import android.content.Context;

import com.matburt.mobileorg.Parsing.OrgFileParser;

public class NullSynchronizer extends Synchronizer {

    public NullSynchronizer(Context parentContext) {
        super(parentContext);
    }

    public boolean isConfigured() {
        return true;
    }

    protected void putRemoteFile(String filename, String contents) {
    }

    public BufferedReader getRemoteFile(String filename) {
        return null;
    }

    @Override
    public void sync(OrgFileParser parser) {
        announceSyncDone();
    }

    @Override
    protected void postSynchronize() {
    }
}