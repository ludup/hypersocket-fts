package com.hypersocket.ftp.interfaces;

import com.hypersocket.tables.Column;

public enum FTPInterfaceResourceColumns implements Column {

	NAME;
	
	/**
	 * TODO rename this class and add any additional columns you 
	 * need to display in the resource table.
	 */
	public String getColumnName() {
		switch(this.ordinal()) {
		default:
			return "name";
		}
	}
}