package com.hypersocket.vfs.events;

import com.hypersocket.fs.FileResource;
import com.hypersocket.realm.Realm;
import com.hypersocket.reconcile.events.ReconcileCompleteEvent;
import com.hypersocket.reconcile.events.ReconcileStartedEvent;

public class FileResourceReconcileCompletedEvent extends ReconcileCompleteEvent<FileResource> {

	private static final long serialVersionUID = 8298500868354271809L;

	public static final String ATTR_FILES_CREATED = "attr.filesCreated";
	public static final String ATTR_FILES_UPDATED = "attr.filesUpdated";
	public static final String ATTR_FILES_DELETED = "attr.filesDeleted";
	
	public FileResourceReconcileCompletedEvent(Object source, boolean success, Realm realm,
			ReconcileStartedEvent<FileResource> started, int filesCreated, int filesUpdated, int filesDeleted) {
		super(source, success, realm, started);
		addAttribute(ATTR_FILES_CREATED, String.valueOf(filesCreated));
		addAttribute(ATTR_FILES_UPDATED, String.valueOf(filesUpdated));
		addAttribute(ATTR_FILES_DELETED, String.valueOf(filesDeleted));
		
	}

	public FileResourceReconcileCompletedEvent(Object source, Throwable t, Realm realm) {
		super(source, t, realm);
	}

}
