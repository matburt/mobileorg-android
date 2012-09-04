package com.matburt.mobileorg.test.Synchronizers;

import java.io.BufferedReader;
import java.io.IOException;

import android.test.AndroidTestCase;

import com.matburt.mobileorg.Synchronizers.DropboxSynchronizer;

/**
 * This test case uses the actual dropbox synchronizer. This means that the
 * phone's dropbox synchronizer needs to be pointed to a valid org-mobile
 * directory for this to succeed.
 */
public class DropboxSynchronizerTest extends AndroidTestCase {

	private DropboxSynchronizer synchronizer;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.synchronizer = new DropboxSynchronizer(getContext());
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testGetIndex() {
		try {
			BufferedReader remoteFile = synchronizer.getRemoteFile("index.org");
			String firstLine = remoteFile.readLine();
			assertEquals("#+READONLY", firstLine);
		} catch (IOException e) {
			fail("Couldn't get index.org");
		}
	}
	
	public void testGetNonExistingFile() {
		try {
			BufferedReader remoteFile = synchronizer.getRemoteFile("THISFILESHOULDNOTEXIST");
			remoteFile.close();
			fail("Synchronizer should throw exception");
		} catch (IOException e) {}
	}
}
