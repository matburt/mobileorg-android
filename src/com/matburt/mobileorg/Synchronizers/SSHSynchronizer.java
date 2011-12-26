package com.matburt.mobileorg.Synchronizers;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import android.content.Context;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;

public class SSHSynchronizer extends Synchronizer {
	private final String LT = "MobileOrg";

	private String user;
	private String host;
	private String remotePath;

	private Session session;

	public SSHSynchronizer(Context context, MobileOrgApplication appInst) {
		super(context, appInst);

		String orgPath = appSettings.getString("scpUrl", "");
		user = appSettings.getString("scpUser", "");
		host = orgPath.substring(0, orgPath.indexOf(':'));
		remotePath = orgPath.substring(orgPath.indexOf(':') + 1);

		JSch jsch = new JSch();
		try {
			session = jsch.getSession(user, host, 22);
			session.setPassword(appSettings.getString("scpPass", ""));

			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);

			session.connect();
			Log.d(LT, "Connected");
		} catch (JSchException e) {
			Log.d(LT, e.getLocalizedMessage());
		}
	}

	@Override
	public boolean isConfigured() {
		if (this.appSettings.getString("scpUrl", "").equals(""))
			return false;
		return true;
	}

	@Override
	protected void putRemoteFile(String filename, String contents)
			throws IOException {
		try {
			Log.d(LT, "Uploading: " + filename);

			// exec 'scp -t rfile' remotely
			String command = "scp -p -t " + remotePath + filename;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			if (checkAck(in) != 0)
				return;

			// send "C0644 filesize filename", where filename should not include
			// '/'
			long filesize = contents.getBytes().length;
			command = "C0644 " + filesize + " ";
			command += filename;
			command += "\n";

			out.write(command.getBytes());
			out.flush();

			if (checkAck(in) != 0)
				return;

			// send content
			out.write(contents.getBytes());

			byte[] buf = new byte[1024];

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0)
				return;

			out.close();
			channel.disconnect();
		} catch (Exception e) {
			Log.d(LT, e.getLocalizedMessage());
		}
	}

	@Override
	protected BufferedReader getRemoteFile(String filename) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		try {

			final String command = "scp -f " + remotePath + filename;
			Log.d(LT, "Running: " + command);
			ChannelExec channel = (ChannelExec) session.openChannel("exec");
			channel.setCommand(command);

			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			byte[] buf = new byte[1024];

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			while (true) {
				int c = checkAck(in);
				if (c != 'C') {
					break;
				}

				// read '0644 '
				in.read(buf, 0, 5);

				// Read file size.
				long filesize = 0L;
				while (true) {
					if (in.read(buf, 0, 1) < 0) {
						// error
						Log.e(LT, "Error reading file size.");
						break;
					}
					if (buf[0] == ' ')
						break;
					filesize = filesize * 10L + (long) (buf[0] - '0');
				}

				// Read (and ignore) filename.
				// TODO: to reduced to while not 0x0a.
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a)
						break;
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();

				// Fetch the file content.
				int sizeRead;
				while (true) {
					if (buf.length < filesize)
						sizeRead = buf.length;
					else
						sizeRead = (int) filesize;
					sizeRead = in.read(buf, 0, sizeRead);
					if (sizeRead < 0) {
						// error
						Log.e(LT, "Content fetching interrupted. Remaining "
								+ filesize);
						break;
					}
					buffer.write(buf, 0, sizeRead);
					filesize -= sizeRead;
					if (filesize == 0L)
						break;
				}

				if (checkAck(in) != 0) {
					// Error ?
					Log.e(LT, "Failed to conclude copy");
					break;
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
			}

			Log.d(LT, "disconnecting...");
			channel.disconnect();
		} catch (Exception e) {
			Log.d(LT, e.getMessage() + e.getLocalizedMessage());
		}

		return new BufferedReader(new StringReader(buffer.toString()));
	}

	private static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				// System.out.print(sb.toString());
			}
			if (b == 2) { // fatal error
				// System.out.print(sb.toString());
			}
		}
		return b;
	}

	@Override
	protected void postSynchronize() {
		if(this.session != null)
			this.session.disconnect();
	}
}
