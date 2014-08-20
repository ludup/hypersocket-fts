package com.hypersocket.fs;

import com.hypersocket.permissions.PermissionType;
import com.hypersocket.realm.RolePermission;

public enum FileResourcePermission implements PermissionType {

	CREATE("mount.create"),
	READ("mount.read", RolePermission.READ),
	UPDATE("mount.update"),
	DELETE("mount.delete");
	
	private final String val;
	
	private PermissionType[] implies;
	
	private FileResourcePermission(final String val, PermissionType... implies) {
		this.val = val;
		this.implies = implies;
	}

	@Override
	public PermissionType[] impliesPermissions() {
		return implies;
	}
	
	public String toString() {
		return val;
	}

	@Override
	public String getResourceKey() {
		return val;
	}
	
	@Override
	public boolean isSystem() {
		return false;
	}

	@Override
	public boolean isHidden() {
		return false;
	}
}
