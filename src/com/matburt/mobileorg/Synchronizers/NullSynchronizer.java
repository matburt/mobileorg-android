package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import android.content.Context;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;

public class NullSynchronizer extends Synchronizer {

    public NullSynchronizer(Context parentContext, MobileOrgApplication appInst) {
        super(parentContext, appInst);
    }

    public boolean isConfigured() {
        return true;
    }

    protected void putRemoteFile(String filename, String contents) {
    }

    protected BufferedReader getRemoteFile(String filename) {
        return null;
    }

    @Override
    public void sync() {
        announceSyncDone();
    }

    @Override
    protected void postSynchronize() {
    }
}