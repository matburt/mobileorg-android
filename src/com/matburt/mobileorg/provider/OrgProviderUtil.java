package com.matburt.mobileorg.provider;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.matburt.mobileorg.provider.OrgContract.Files;
import com.matburt.mobileorg.provider.OrgContract.OrgData;
import com.matburt.mobileorg.provider.OrgContract.Todos;

public class OrgProviderUtil {
	
//	public static ArrayList<HashMap<String, Integer>> getGroupedTodos(ContentResolver resolver) {
//		ArrayList<HashMap<String, Integer>> todos = new ArrayList<HashMap<String, Integer>>();
//		Cursor cursor = resolver.query(Todos.CONTENT_URI, Todos.DEFAULT_COLUMNS, null, null, Todos.GROUP);
//
//		if (cursor.getCount() > 0) {
//			HashMap<String, Integer> grouping = new HashMap<String, Integer>();
//			int resultgroup = 0;
//
//			for (cursor.moveToFirst(); cursor.isAfterLast() == false; cursor
//					.moveToNext()) {
//				// If new result group, create new grouping
//				if (resultgroup != cursor.getInt(0)) {
//					resultgroup = cursor.getInt(0);
//					todos.add(grouping);
//					grouping = new HashMap<String, Integer>();
//				}
//				// Add item to grouping
//				grouping.put(cursor.getString(1), cursor.getInt(2));
//			}
//
//			todos.add(grouping);
//		}
//
//		cursor.close();
//		return todos;
//	}
}
