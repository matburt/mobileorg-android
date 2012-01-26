package com.matburt.mobileorg.Synchronizers;

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