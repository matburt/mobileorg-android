package com.matburt.mobileorg.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class OrgUtils {
	
	public static String getTimestamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd EEE HH:mm]");		
		return sdf.format(new Date());
	}
}
