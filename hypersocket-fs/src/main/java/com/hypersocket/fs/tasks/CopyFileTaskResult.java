package com.hypersocket.fs.tasks;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.realm.Realm;
import com.hypersocket.tasks.Task;

public class CopyFileTaskResult extends FileOperationTaskResult {

	private static final long serialVersionUID = -7915027397936300413L;

	public static final String EVENT_RESOURCE_KEY = "";
	
	public static final String ATTR_DESTINATION = "attr.destination";
	
	public CopyFileTaskResult(Object source, Realm currentRealm, Task task,
			String originPath, String destinationPath) {
		super(source, EVENT_RESOURCE_KEY, true, currentRealm,
				task, originPath);
		addAttribute(ATTR_DESTINATION, destinationPath);
	}

	public CopyFileTaskResult(Object source, Throwable e, Realm currentRealm,
			Task task, String originPath, String destinationPath) {
		super(source, EVENT_RESOURCE_KEY, e, currentRealm, task, originPath);
		addAttribute(ATTR_DESTINATION, destinationPath);
	}

	@Override
	public boolean isPublishable() {
		return true;
	}

	@Override
	public String getResourceBundle() {
		return CopyFileTask.RESOURCE_BUNDLE;
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
}
