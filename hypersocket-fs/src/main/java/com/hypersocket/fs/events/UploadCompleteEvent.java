package com.hypersocket.fs.events;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.vfs2.FileObject;

import com.hypersocket.fs.FileResource;
import com.hypersocket.session.Session;

public class UploadCompleteEvent extends FileOperationEvent {

	private static final long serialVersionUID = 2948287474957129496L;

	public static final String EVENT_RESOURCE_KEY = "fs.uploadComplete";

	public static final String ATTR_BYTES_IN = "attr.bytesIn";
	public static final String ATTR_TRANSFER_TIME_MILLIS = "attr.transferTimeMillis";

	public UploadCompleteEvent(Object source, Session currentSession,
			FileResource resource, FileObject file, String childPath, long bytesIn,
			long timeMillis, String protocol) {
		super(source, "fs.uploadComplete", true, currentSession, resource,
				file, childPath, protocol);
		addAttribute(ATTR_BYTES_IN, String.valueOf(bytesIn));
		addAttribute(ATTR_TRANSFER_TIME_MILLIS, String.valueOf(timeMillis));
	}

	public UploadCompleteEvent(Object source,
			Session currentSession, Throwable e, FileResource resource, String childPath, String protocol) {
		super(source, "fs.uploadComplete", e, currentSession, resource, childPath, protocol);
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
	
	public boolean isUsage() {
		return true;
	}
}
