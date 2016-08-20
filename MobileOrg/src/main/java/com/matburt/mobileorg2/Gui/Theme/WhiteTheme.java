package com.matburt.mobileorg.Gui.Theme;

import android.graphics.Color;

public class WhiteTheme extends DefaultTheme {

	public WhiteTheme() {
		super();
		c0Black = Color.rgb(0xff, 0xff, 0xff);
		c1Red = Color.rgb(0x20, 0x00, 0x00);
		c2Green = Color.rgb(0x00, 0x20, 0x00);
		c3Yellow = Color.rgb(0x20, 0x20, 0x00);
		c4Blue = Color.rgb(0x0, 0x0, 0x20);
		c5Purple = Color.rgb(0x20, 0x00, 0x20);
		c6Cyan = Color.rgb(0x00, 0x20, 0x20);
		c7White = Color.rgb(0x10, 0x10, 0x10);

		c9LRed = Color.rgb(0xa0, 0x0, 0x0);
		caLGreen = Color.rgb(0x20, 0xa0, 0x20);
		cbLYellow = Color.rgb(0x80, 0x80, 0x00);
		ccLBlue = Color.rgb(0x00, 0x00, 0x80);
		cdLPurple = Color.rgb(0x80, 0x00, 0x80);
		ceLCyan = Color.rgb(0x00, 0x80, 0x80);
		cfLWhite = Color.rgb(0x00, 0x00, 0x00);

		levelColors = new int[] { ccLBlue, c3Yellow, ceLCyan, c2Green,
				c5Purple, ccLBlue, c2Green, ccLBlue, c3Yellow, ceLCyan };
		
		defaultFontColor = "black";
		defaultBackground = Color.rgb(0xff, 0xff, 0xff);
		defaultForeground = Color.rgb(0x10, 0x10, 0x10);
	}
}
