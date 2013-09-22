package com.matburt.mobileorg.Gui.Theme;

import android.content.Context;
import android.graphics.Color;

import com.matburt.mobileorg.util.PreferenceUtils;

public class DefaultTheme {
	
	public int gray = Color.GRAY;
	
	public int c0Black = Color.rgb(0x00, 0x00, 0x00);
	public int c1Red = Color.rgb(0xd0, 0x00, 0x00);
	public int c2Green = Color.rgb(0x00, 0xa0, 0x00);
	public int c3Yellow = Color.rgb(0xc0, 0x80, 0x00);
	public int c4Blue = Color.rgb(0x22, 0x22, 0xf0);
	public int c5Purple = Color.rgb(0xa0, 0x00, 0xa0);
	public int c6Cyan = Color.rgb(0x00, 0x80, 0x80);
	public int c7White = Color.rgb(0xc0, 0xc0, 0xc0);

	public int c9LRed = Color.rgb(0xff, 0x77, 0x77);
	public int caLGreen = Color.rgb(0x77, 0xff, 0x77);
	public int cbLYellow = Color.rgb(0xff, 0xff, 0x00);
	public int ccLBlue = Color.rgb(0x88, 0x88, 0xff);
	public int cdLPurple = Color.rgb(0xff, 0x00, 0xff);
	public int ceLCyan = Color.rgb(0x00, 0xff, 0xff);
	public int cfLWhite = Color.rgb(0xff, 0xff, 0xff);

	public int defaultForeground = Color.rgb(0xc0, 0xc0, 0xc0);
	public int defaultBackground = Color.rgb(0x00, 0x00, 0x00);
	
	public int[] levelColors;
	
	public String defaultFontColor = "white";
	
	public DefaultTheme() {
		levelColors = new int[] { ccLBlue, c3Yellow, ceLCyan, c2Green,
				c5Purple, ccLBlue, c2Green, ccLBlue, c3Yellow, ceLCyan };
	}
	
	
	public static DefaultTheme getTheme(Context context) {
		final String themeName = PreferenceUtils.getThemeName();
		if(themeName.equals("Light"))
				return new WhiteTheme();
		else if(themeName.equals("Monochrome"))
			return new MonoTheme();
		else
			return new DefaultTheme();
	}
}
