package com.matburt.mobileorg.test.Synchronizers;

import java.io.BufferedReader;
import java.util.ArrayList;

import android.content.ContentResolver;

import com.matburt.mobileorg.Parsing.OrgFileParser;
import com.matburt.mobileorg.provider.OrgDatabase;
import com.matburt.mobileorg.provider.OrgFile;

public class OrgFileParserStub extends OrgFileParser {
	ArrayList<String> filesParsed = new ArrayList<String>();
	
	public OrgFileParserStub(OrgDatabase db, ContentResolver resolver) {
		super(db, resolver);
	}

	@Override
	public void parse(OrgFile orgFile, BufferedReader breader) {
		filesParsed.add(orgFile.filename);
	}

	
	public void reset() {
		filesParsed = new ArrayList<String>();
	}
}
