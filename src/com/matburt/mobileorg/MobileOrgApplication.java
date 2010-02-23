package com.matburt.mobileorg;

import android.app.Application;
import java.util.ArrayList;

public class MobileOrgApplication extends Application {
    public Node rootNode = null;
    public ArrayList<Integer> nodeSelection;
}