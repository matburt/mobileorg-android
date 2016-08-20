package com.matburt.mobileorg;

import android.content.Context;
import android.support.v4.content.ContextCompat;

public class Style {
    public static int gray, red, green, yellow, blue, foreground, foregroundDark, black, background, highlightedBackground;
    public static int nTitleColors = 3;
    public static int[] titleColor;
    public static int[] titleFontSize;
    
    Style(Context context){
        gray = ContextCompat.getColor(context, R.color.colorGray);
        red = ContextCompat.getColor(context, R.color.colorRed);
        green = ContextCompat.getColor(context, R.color.colorGreen);
        yellow = ContextCompat.getColor(context, R.color.colorYellow);
        blue = ContextCompat.getColor(context, R.color.colorBlue);
        foreground = ContextCompat.getColor(context, R.color.colorPrimary);
        foregroundDark = ContextCompat.getColor(context, R.color.colorPrimaryDark);
        black = ContextCompat.getColor(context, R.color.colorBlack);

        titleColor = new int[nTitleColors];
        titleColor[0] = foregroundDark;
        titleColor[1] = foreground;
        titleColor[2] = black;

        titleFontSize = new int[nTitleColors];
        titleFontSize[0] = 25;
        titleFontSize[1] = 20;
        titleFontSize[2] = 16;
    }

}
