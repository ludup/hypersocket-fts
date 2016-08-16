package com.hypersocket.ftp.interfaces.events;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.ftp.interfaces.FTPInterfaceResource;
import com.hypersocket.session.Session;

public class FTPInterfaceResourceUpdatedEvent extends
		FTPInterfaceResourceEvent {

	private static final long serialVersionUID = -7346854772572811691L;
	
	/**
	 * TODO rename to suit your resource and replace ftpInterface with lower case
	 * name of your resource.
	 * 
	 * You typically add attributes to the base FTPInterfaceResourceEvent
	 * class so these can be reused across all resource events.
	 */
	public static final String EVENT_RESOURCE_KEY = "ftpInterface.updated";

	public FTPInterfaceResourceUpdatedEvent(Object source,
			Session session, FTPInterfaceResource resource) {
		super(source, EVENT_RESOURCE_KEY, session, resource);
	}

	public FTPInterfaceResourceUpdatedEvent(Object source,
			FTPInterfaceResource resource, Throwable e, Session session) {
		super(source, EVENT_RESOURCE_KEY, resource, e, session);
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
}
