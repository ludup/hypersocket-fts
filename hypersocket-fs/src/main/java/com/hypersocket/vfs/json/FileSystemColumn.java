package com.hypersocket.vfs.json;

import com.hypersocket.tables.Column;

public enum FileSystemColumn implements Column {

	FILENAME, TYPE, LASTMODIFIED, SIZE, NAME;
	
	public String getColumnName() {
		switch(this.ordinal()) {
		case 1:
			return "type";
		case 2: 
			return "lastModified";
		case 3: 
			return "size";
		default:
			return "filename";
		}
	}
}
