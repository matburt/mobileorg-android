package com.matburt.mobileorg.util;

import android.app.Activity;
import android.content.ContentResolver;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class OrgNode2HtmlTest {
    private Activity activity;
    private ContentResolver contentResolver;

    @Before
    public void setUp() throws Exception {
        activity = new Activity();
        contentResolver = activity.getContentResolver();
    }

    @org.junit.Test
    public void testUrlToHTML() throws Exception {
        OrgNode2Html o = new OrgNode2Html(contentResolver, activity);
        assertEquals("<html><body><font color='white'><pre>" +
                     "<a href=\"http://mobileorg.ncogni.to/\">http://mobileorg.ncogni.to/</a>" +
                     "</pre></font></body></html>",
                o.toHTML("http://mobileorg.ncogni.to/"));
    }
}