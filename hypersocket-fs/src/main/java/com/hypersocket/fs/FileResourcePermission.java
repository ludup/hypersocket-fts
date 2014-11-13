package com.hypersocket.fs;

import com.hypersocket.permissions.PermissionType;
import com.hypersocket.realm.RealmPermission;
import com.hypersocket.realm.RolePermission;

public enum FileResourcePermission implements PermissionType {

	READ("mount.read", RolePermission.READ, RealmPermission.READ),
	CREATE("mount.create", READ),
	UPDATE("mount.update", READ),
	DELETE("mount.delete", READ);
	
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
