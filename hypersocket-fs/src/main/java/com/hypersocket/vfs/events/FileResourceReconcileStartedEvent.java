package com.hypersocket.vfs.events;

import com.hypersocket.fs.FileResource;
import com.hypersocket.realm.Realm;
import com.hypersocket.reconcile.events.ReconcileStartedEvent;

public class FileResourceReconcileStartedEvent extends ReconcileStartedEvent<FileResource> {

	private static final long serialVersionUID = -5835625271400000833L;

	public FileResourceReconcileStartedEvent(Object source, boolean success, Realm realm, FileResource resource) {
		super(source, success, realm, resource);
	}

	public FileResourceReconcileStartedEvent(Object source, Throwable t, FileResource resource, Realm realm) {
		super(source, t, resource, realm);
	}

}
