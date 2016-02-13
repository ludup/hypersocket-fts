package com.hypersocket.fs.json;

import com.hypersocket.tables.Column;

public enum FileResourceColumn implements Column {

	NAME, SCHEME;
	
	public String getColumnName() {
		switch(this.ordinal()) {
		case 1:
			return "scheme";
		default:
			return "name";
		}
	}
}