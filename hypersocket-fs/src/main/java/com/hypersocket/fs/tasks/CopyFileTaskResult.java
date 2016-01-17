package com.hypersocket.fs.tasks;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.realm.Realm;
import com.hypersocket.tasks.Task;

public class CopyFileTaskResult extends AbstractCopyFileTaskResult {

	private static final long serialVersionUID = -7915027397936300413L;

	public static final String EVENT_RESOURCE_KEY = "fs.copyFile";
	
	public static final String ATTR_DESTINATION = "attr.destination";
	
	public CopyFileTaskResult(Object source, Realm currentRealm, Task task,
			String originPath, String destinationPath) {
		super(source, currentRealm, task, EVENT_RESOURCE_KEY, originPath, destinationPath);
	}

	public CopyFileTaskResult(Object source, Throwable e, Realm currentRealm,
			Task task, String originPath, String destinationPath) {
		super(source, e, currentRealm, task, EVENT_RESOURCE_KEY, originPath, destinationPath);
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
