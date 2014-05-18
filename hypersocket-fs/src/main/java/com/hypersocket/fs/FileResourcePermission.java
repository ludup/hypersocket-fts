package com.hypersocket.fs;

import com.hypersocket.permissions.PermissionType;

public enum FileResourcePermission implements PermissionType {

	CREATE("mount.create"),
	READ("mount.read"),
	UPDATE("mount.update"),
	DELETE("mount.delete");
	
	private final String val;
	
	private FileResourcePermission(final String val) {
		this.val = val;
	}
	
	public String toString() {
		return val;
	}

	@Override
	public String getResourceKey() {
		return val;
	}

}
