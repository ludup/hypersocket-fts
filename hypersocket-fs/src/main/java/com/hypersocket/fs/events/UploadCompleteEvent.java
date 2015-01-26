package com.hypersocket.fs.events;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class UploadCompleteEvent extends FileOperationEvent {

	private static final long serialVersionUID = 2948287474957129496L;

	public static final String EVENT_RESOURCE_KEY = "fs.uploadComplete";

	public static final String ATTR_BYTES_IN = "attr.bytesIn";
	public static final String ATTR_TRANSFER_TIME_MILLIS = "attr.transferTimeMillis";

	public UploadCompleteEvent(Object source, Session currentSession,
			FileResource resource, String childPath, long bytesIn,
			long timeMillis, String protocol) {
		super(source, "fs.uploadComplete", true, currentSession, resource,
				childPath, protocol);
		addAttribute(ATTR_BYTES_IN, String.valueOf(bytesIn));
		addAttribute(ATTR_TRANSFER_TIME_MILLIS, String.valueOf(timeMillis));
	}

	public UploadCompleteEvent(Object source,
			Session currentSession, Throwable e, String name, String childPath, String protocol) {
		super(source, "fs.uploadComplete", e, currentSession, name, childPath, protocol);
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
}
