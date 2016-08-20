package com.matburt.mobileorg.Synchronizers;

import android.content.Context;
import android.util.Log;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.matburt.mobileorg.util.OrgUtils;

import java.io.File;

public class SSHSynchronizer extends Synchronizer {
    private final String LT = "MobileOrg";
    AuthData authData;
    private Session session;

    public SSHSynchronizer(Context context) {
        super(context);
        this.context = context;
        authData = AuthData.getInstance(context);
    }

    @Override
    public String getRelativeFilesDir() {
        return JGitWrapper.GIT_DIR;
    }

    @Override
    public boolean isConfigured() {
        return !(authData.getPath().equals("")
                || authData.getUser().equals("")
                || authData.getHost().equals("")
                || authData.getPassword().equals("")
                && authData.getPubFile().equals(""));
    }

    public void connect() throws JSchException {
        JSch jsch = new JSch();

        session = jsch.getSession(
                authData.getUser(),
                authData.getHost(),
                authData.getPort());

        session = jsch.getSession(
                "bcoste",
                "209.148.83.200",
                authData.getPort());

        JGitWrapper.ConnectionType connection = JGitWrapper.getConnectionType(context);

//        switch (connection){
//            case kHttp:
//                session.setPassword(authData.getPassword());
//                break;
//            case kSshPassword:
//                session.setPassword(authData.getPassword());
//                break;
//            case kSshPubKey:
//                jsch.addIdentity(authData.getPubFile());
//                break;
//        }

        session.setPassword("Itadakimasu2!#");
//                jsch.addIdentity(authData.getPubFile(), authData.getPubFile());

        Log.v("host", authData.getHost() + " " + authData.getUser() + " " + authData.getPort() + " " + authData.getPassword());
        Log.v("host", "connection : " + connection.toString());

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        session.disconnect();
    }

    public SyncResult synchronize(){
        if (isCredentialsRequired()) return new SyncResult();
        SyncResult pullResult = JGitWrapper.pull(context);
        Log.v("git", "changed : "+pullResult.changedFiles.toString());
        Log.v("git", "new : "+pullResult.newFiles.toString());
        Log.v("git", "deleted : "+pullResult.deletedFiles.toString());

        new JGitWrapper.PushTask(context).execute();
        return pullResult;
    }

    /**
     * Except if authentication by Public Key, the user has to enter his password
     *
     * @return
     */
    public boolean isCredentialsRequired() {
        return authData.getPubFile().equals("") && authData.getPassword().equals("");
    }

    @Override
    public void postSynchronize() {
        if (this.session != null)
            this.session.disconnect();
    }

    @Override
    public void addFile(String filename) {
        Log.v("sync", "addding file : "+filename);
        JGitWrapper.add(filename, context);
    }

    @Override
    public boolean isConnectable() throws Exception {
        if (!OrgUtils.isNetworkOnline(context)) return false;

        this.connect();
        return true;
    }

    @Override
    public void clearRepository(Context context) {
        File dir = new File(getAbsoluteFilesDir(context));
        for (File file : dir.listFiles()) {
            if (file.getName().equals(".git")) continue;
            file.delete();
        }
    }
}
