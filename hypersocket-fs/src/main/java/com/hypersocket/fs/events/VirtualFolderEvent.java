package com.hypersocket.fs.events;

import com.hypersocket.realm.Realm;
import com.hypersocket.session.Session;
import com.hypersocket.session.events.SessionEvent;

public class VirtualFolderEvent extends SessionEvent {

	private static final long serialVersionUID = -4932246818588667269L;

	public static final String ATTR_VIRTUAL_PATH = "attr.virtualPath";
	
	public VirtualFolderEvent(Object source, String resourceKey, boolean success, Session session, String virtualPath) {
		super(source, resourceKey, success, session);
		addAttribute(ATTR_VIRTUAL_PATH, virtualPath);
	}

	public VirtualFolderEvent(Object source, String resourceKey, boolean success, Session session, Realm realm, String virtualPath) {
		super(source, resourceKey, success, session, realm);
		addAttribute(ATTR_VIRTUAL_PATH, virtualPath);
	}

	public VirtualFolderEvent(Object source, String resourceKey, Throwable e, Session session, String virtualPath) {
		super(source, resourceKey, e, session);
		addAttribute(ATTR_VIRTUAL_PATH, virtualPath);
	}

	public VirtualFolderEvent(Object source, String resourceKey, Throwable e, Session session, Realm currentRealm, String virtualPath) {
		super(source, resourceKey, e, session, currentRealm);
		addAttribute(ATTR_VIRTUAL_PATH, virtualPath);
	}

}
