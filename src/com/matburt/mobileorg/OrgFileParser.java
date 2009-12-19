package com.matburt.mobileorg;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import android.util.Log;

class OrgFileParser {

    String orgPath = "";
    FileInputStream fstream;
    public static final String LT = "MobileOrg";

    OrgFileParser(String orgpath) {
        orgPath = orgpath;
    }

    public void parse() {
        BufferedReader breader = this.getNewHandle();
        Log.d(LT, "Writing out each line from parse.");
        String thisLine;
        try {
            while ((thisLine = breader.readLine()) != null) {
                Log.d(LT, thisLine);
            }
        }
        catch (IOException e) {
            Log.e(LT, "IO Exception on readerline: " + e.getMessage());
        }
    }

    public BufferedReader getNewHandle() {
        BufferedReader breader = null;
        try {
            fstream = new FileInputStream("/data/data/com.matburt.mobileorg/files/" + this.orgPath);
            DataInputStream in = new DataInputStream(fstream);
            breader = new BufferedReader(new InputStreamReader(in));
        }
        catch (Exception e) {
            Log.e(LT, "Error: " + e.getMessage());
        }
        return breader;
    }

}
