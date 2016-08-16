package com.hypersocket.fs.events;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceServiceImpl;
import com.hypersocket.properties.ResourceUtils;
import com.hypersocket.resource.AssignableResourceEvent;
import com.hypersocket.session.Session;

public class FileResourceEvent extends AssignableResourceEvent {

	private static final long serialVersionUID = -8366174502219193895L;
	
	public static final String EVENT_RESOURCE_KEY = "file.event";
	
	public static final String ATTR_FILE_PATH = "attr.filePath";
	public static final String ATTR_PROTOCOL = "attr.protocol";
	public static final String ATTR_USERNAME = "attr.username";
	public static final String ATTR_PASSWORD = "attr.password";
	public static final String ATTR_SERVER = "attr.server";
	public static final String ATTR_PORT = "attr.port";
	public static final String ATTR_READONLY = "attr.readOnly";
	public static final String ATTR_LOGO = "attr.logo";
	public static final String ATTR_SHOW_FOLDERS = "attr.showFolders";
	public static final String ATTR_SHOW_HIDDEN = "attr.showHidden";
	public static final String ATTR_VIRTUAL_PATH = "attr.virtualPath";
	
	public FileResourceEvent(Object source, String resourceKey, boolean success,
			Session session, FileResource resource) {
		super(source, resourceKey, success, session, resource);
		addAttributes(resource);
	}

	public FileResourceEvent(Object source, String resourceKey, Throwable e,
			Session session, FileResource resource) {
		super(source, resourceKey, resource, e, session);
		addAttributes(resource);
	}
	
	private void addAttributes(FileResource resource) {
		addAttribute(ATTR_FILE_PATH, resource.getPath());
		addAttribute(ATTR_PROTOCOL, resource.getScheme());
		addAttribute(ATTR_USERNAME, resource.getUsername());
		if(!StringUtils.isEmpty(resource.getPassword())) {
			if(ResourceUtils.isReplacementVariable(resource.getPassword())) {
				addAttribute(ATTR_PASSWORD, resource.getPassword());
			} else {
				addAttribute(ATTR_PASSWORD, "********" + StringUtils.right(resource.getPassword(), 3));
			}
		} else {
			addAttribute(ATTR_PASSWORD, "");
		}
		addAttribute(ATTR_SERVER , resource.getServer());
		addAttribute(ATTR_PORT, resource.getPort());
		addAttribute(ATTR_READONLY, resource.isReadOnly());
		addAttribute(ATTR_SHOW_FOLDERS, resource.isShowFolders());
		addAttribute(ATTR_SHOW_HIDDEN, resource.isShowHidden());
		addAttribute(ATTR_VIRTUAL_PATH, resource.getVirtualPath());
	}
	
	public String getResourceBundle() {
		return FileResourceServiceImpl.RESOURCE_BUNDLE;
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
}
