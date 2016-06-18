package com.matburt.mobileorg2.Synchronizers;

import android.content.Context;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.matburt.mobileorg2.util.OrgUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashSet;

public class SSHSynchronizer extends Synchronizer {
    private final String LT = "MobileOrg";
    AuthData authData;
    private Session session;
	private Context context;

	public SSHSynchronizer(Context context) {
		this.context = context;
        authData = AuthData.getInstance(context);
    }

    public String testConnection(String path, String user, String pass, String host, int port, String pubFile) {
        if (!authData.isValid()) return "Missing configuration values";

        try {
            this.connect();
            BufferedReader r = this.getRemoteFile(authData.getFileName());
            r.close();
        }
        catch (Exception e) {
            Log.i("MobileOrg", "SSH Get index file failed");
            return "Failed to find index file with error: " + e.toString();
        }
        this.postSynchronize();
        return null;
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
		try {
            session = jsch.getSession(
                    authData.getUser(),
                    authData.getHost(),
                    authData.getPort());

            if (!authData.getPubFile().equals("") && !authData.getPassword().equals("")) {
                jsch.addIdentity(authData.getPubFile(), authData.getPubFile());
            } else if (!authData.getPubFile().equals("")) {
                jsch.addIdentity(authData.getPubFile());
            }
            else {
                session.setPassword(authData.getPassword());
            }

			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);

			session.connect();
			Log.d(LT, "SSH Connected");
		} catch (JSchException e) {
			Log.d(LT, e.getLocalizedMessage());
            throw e;
		}
    }

    public HashSet<String> synchronize(){
        HashSet<String> changedFiles = JGitWrapper.pull(context);
        new JGitWrapper.PushGitRepoTask(context).execute();
        return changedFiles;
    }

    public void putRemoteFile(String filename, String contents) throws IOException {
        try {
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            ByteArrayInputStream bas = new ByteArrayInputStream(contents.getBytes());

            sftpChannel.put(bas, authData.getRootUrl() + filename);
            sftpChannel.exit();
        } catch (Exception e) {
            Log.e("MobileOrg", "Exception in putRemoteFile: " + e.toString());
            throw new IOException(e);
        }
	}

	public BufferedReader getRemoteFile(String filename) throws IOException {
        StringBuilder contents = null;
        try {
            Channel channel = session.openChannel( "sftp" );
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            Log.i("MobileOrg", "SFTP Getting: " + authData.getRootUrl() + filename);
            InputStream in = sftpChannel.get(authData.getRootUrl() + filename);

            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            contents = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                contents.append(line + "\n");
            }
            sftpChannel.exit();
        } catch (Exception e) {
            Log.e("MobileOrg", "Exception in getRemoteFile: " + e.toString());
            throw new IOException(e);
        }
        return new BufferedReader(new StringReader(contents.toString()));
    }

	@Override
	public void postSynchronize() {
		if(this.session != null)
			this.session.disconnect();
	}

    @Override
    public void addFile(String filename) {
        Log.v("sync", "addding file");
        JGitWrapper.add(filename, context);
    }

    @Override
    public boolean isConnectable() {
        if( ! OrgUtils.isNetworkOnline(context) ) return false;

        try {
            Log.v("sync","trying connect");
//            this.connect();
            Log.v("sync","session success");
        } catch (Exception e) {
            Log.e("MobileOrg", "SSH Connection failed");
            return false;
        }

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
