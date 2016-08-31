package com.matburt.mobileorg.Synchronizers;

import android.content.Context;
import android.util.Log;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.util.FS;

public class SshSessionFactory extends JschConfigSessionFactory {
    Context context;
    ConnectionType connection;

    SshSessionFactory(Context context) {
        this.context = context;
        this.connection = getConnectionType(context);
    }

    /**
     * Determine if connection if HTTP, SSH+PubKey, SSH+Password
     * based on the provided path and whether or not the user has provided a PubKey
     *
     * @param context
     * @return
     */
    public static ConnectionType getConnectionType(Context context) {
        final AuthData authData = AuthData.getInstance(context);
        if (authData.getHost().startsWith("http")) return ConnectionType.kHttp;
        else {
            if (!authData.getPubFile().equals("")) return ConnectionType.kSshPubKey;
            else return ConnectionType.kSshPassword;
        }
    }

    @Override
    protected void configure(OpenSshConfig.Host host, Session session) {
        if (connection == ConnectionType.kSshPassword) {
            Log.v("host", "password : " + AuthData.getInstance(context).getPassword());
            session.setPassword(AuthData.getInstance(context).getPassword());
        }

    }

    @Override
    protected JSch createDefaultJSch(FS fs) throws JSchException {
        JSch defaultJSch = super.createDefaultJSch(fs);
        Log.v("host", "pub : " + AuthData.getInstance(context).getPubFile());
        Log.v("host", "Connection type : " + connection);
        Log.v("host", "pub file : " + AuthData.getInstance(context).getPubFile());

        if (connection == ConnectionType.kSshPubKey)
            defaultJSch.addIdentity(AuthData.getInstance(context).getPubFile());


        return defaultJSch;
    }

    enum ConnectionType {
        kHttp,
        kSshPubKey,
        kSshPassword
    }
}
