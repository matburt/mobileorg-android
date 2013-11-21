package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.matburt.mobileorg.util.OrgUtils;

public class SSHSynchronizer implements SynchronizerInterface {
	private final String LT = "MobileOrg";

	private String user;
	private String host;
	private String path;
    private String pass;
    private int port;
    private String pubFile;

	private Session session;

	private SharedPreferences appSettings;

	private Context context;

	public SSHSynchronizer(Context context) {
		this.context = context;
		this.appSettings = PreferenceManager
				.getDefaultSharedPreferences(context.getApplicationContext());
		path = appSettings.getString("scpPath", "");
		user = appSettings.getString("scpUser", "");
        host = appSettings.getString("scpHost", "");
        pubFile = appSettings.getString("scpPubFile", "");
        String tmpPort = appSettings.getString("scpPort", "");
        if (tmpPort.equals("")) {
            port = 22;
        }
        else {
            port = Integer.parseInt(tmpPort);
        }
        pass = appSettings.getString("scpPass", "");

        try {
            this.connect();
        } catch (Exception e) {
            Log.e("MobileOrg", "SSH Connection failed");
        }
	}

    public String testConnection(String path, String user, String pass, String host, int port, String pubFile) {
        this.path = path;
        this.user = user;
        this.pass = pass;
        this.host = host;
        this.port = port;
        this.pubFile = pubFile;

        if (this.path.indexOf("index.org") < 0) {
            Log.i("MobileOrg", "Invalid ssh path, must point to index.org");
            return "Invalid ssh path, must point to index.org";
        }

        if (this.path.equals("") ||
            this.user.equals("") ||
            this.host.equals("") ||
            (this.pass.equals("") && this.pubFile.equals(""))) {
            Log.i("MobileOrg", "Test Connection Failed for not being configured");
            return "Missing configuration values";
        }

        try {
            this.connect();
            BufferedReader r = this.getRemoteFile(this.getFileName());
            r.close();
        }
        catch (Exception e) {
            Log.i("MobileOrg", "SSH Get index file failed");
            return "Failed to find index file with error: " + e.toString();
        }
        this.postSynchronize();
        return null;
    }

    private String getFileName() {
        String[] pathElements = this.path.split("/");
        if (pathElements.length > 0) {
            return pathElements[pathElements.length-1];
        }
        return "";
    }

    private String getRootUrl() {
        String[] pathElements = this.path.split("/");
        String directoryActual = "/";
        if (pathElements.length > 1) {
            for (int i = 0; i < pathElements.length - 1; i++) {
                if (pathElements[i].length() > 0) {
                    directoryActual += pathElements[i] + "/";
                }
            }
        }
        return directoryActual;
    }

	@Override
	public boolean isConfigured() {
		if (this.appSettings.getString("scpPath", "").equals("")
				|| this.appSettings.getString("scpUser", "").equals("")
				|| this.appSettings.getString("scpHost", "").equals("")
				|| (this.appSettings.getString("scpPass", "").equals("") && this.appSettings
						.getString("scpPubFile", "").equals("")))
			return false;
		return true;
	}

    public void connect() throws JSchException {
		JSch jsch = new JSch();
		try {
			session = jsch.getSession(user, host, port);
            if (!pubFile.equals("") && !pass.equals("")) {
                jsch.addIdentity(pubFile, pass);
            }
            else if (!pubFile.equals("")) {
                jsch.addIdentity(pubFile);
            }
            else {
                session.setPassword(pass);
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

    public void putRemoteFile(String filename, String contents) throws IOException {
        try {
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            ByteArrayInputStream bas = new ByteArrayInputStream(contents.getBytes());

            sftpChannel.put(bas, this.getRootUrl() + filename);
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
            Log.i("MobileOrg", "SFTP Getting: " + this.getRootUrl() + filename);
            InputStream in = sftpChannel.get(this.getRootUrl() + filename);

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
	public boolean isConnectable() {
		return OrgUtils.isNetworkOnline(context);
	}
}
