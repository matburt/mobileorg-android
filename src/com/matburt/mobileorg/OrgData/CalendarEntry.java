package com.matburt.mobileorg.OrgData;

import android.text.TextUtils;

public class CalendarEntry {
	public String title = "";
	public String description = "";
	public String location = "";
	public long id = -1;
	public long dtStart = 0;
	public long dtEnd = 0;

	@Override
	public boolean equals(Object o) {
		if (o instanceof OrgNodeDate) {
			OrgNodeDate entry = (OrgNodeDate) o;
			return this.dtStart == entry.beginTime
					&& this.dtEnd == entry.endTime
					&& entry.getTitle().startsWith(this.title);
		}

		return super.equals(o);
	}

	public OrgNode getOrgNode() {
		OrgNode node = new OrgNode();
		node.name = this.title;

		String date = OrgNodeDate.getDate(this.dtStart, this.dtEnd);
		String formatedDate = OrgNodeTimeDate.formatDate(
				OrgNodeTimeDate.TYPE.Timestamp, date);

		String payload = formatedDate + "\n" + this.description;

		if (TextUtils.isEmpty(location) == false)
			payload += "\n:LOCATION: " + location;

		node.setPayload(payload);
		return node;
	}
}
