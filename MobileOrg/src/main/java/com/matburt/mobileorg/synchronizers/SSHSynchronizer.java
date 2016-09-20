package com.matburt.mobileorg.synchronizers;

import android.content.Context;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.matburt.mobileorg.util.OrgUtils;

import org.eclipse.jgit.util.FS;

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
                && AuthData.getPublicKey(context).equals(""));
    }

    public void connect() {
        try {
            SshSessionFactory sshSessionFactory = new SshSessionFactory(context);
            JSch jSch = sshSessionFactory.createDefaultJSch(FS.detect());


            session = jSch.getSession(
                    authData.getUser(),
                    authData.getHost(),
                    authData.getPort());

            session.setPassword(AuthData.getInstance(context).getPassword());

            // TODO: find a way to check for host key
//            jSch.setKnownHosts("/storage/sdcard0/Download/known_hosts");
            session.setConfig("StrictHostKeyChecking", "no");

            session.connect();
            session.disconnect();
        } catch (JSchException e) {
            e.printStackTrace();
        }

    }

    public SyncResult synchronize(){
        if (isCredentialsRequired()) return new SyncResult();
        SyncResult pullResult = JGitWrapper.pull(context);

        new JGitWrapper.PushTask(context).execute();
        return pullResult;
    }

    /**
     * Except if authentication by Public Key, the user has to enter his password
     *
     * @return
     */
    public boolean isCredentialsRequired() {
        return false;
    }

    @Override
    public void postSynchronize() {
        if (this.session != null)
            this.session.disconnect();
    }

    @Override
    public void addFile(String filename) {
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
