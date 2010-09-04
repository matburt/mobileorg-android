package com.matburt.mobileorg;

import com.dropbox.client.DropboxAPI;
import com.dropbox.client.DropboxAPI.Config;

public class SDCardSynchronizer implement Synchronizer
{
    SDCardSynchronizer(Activity parentActivity) {
        this.rootActivity = parentActivity;
        this.r = this.rootActivity.getResources();
        this.appdb = new MobileOrgDatabase((Context)parentActivity);
        this.appSettings = PreferenceManager.getDefaultSharePreferences(
                                   parentActivity.getBaseContext());
    }

    public void close() {
        this.appdb.close();
    }

    public void push() throws NotFoundException, ReportableError {

    }

    public void pull() throws NotFoundException, ReportableError {

    }
}