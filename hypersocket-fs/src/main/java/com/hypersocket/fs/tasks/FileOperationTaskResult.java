package com.hypersocket.fs.tasks;

import com.hypersocket.realm.Realm;
import com.hypersocket.tasks.Task;
import com.hypersocket.triggers.TaskResult;

public class FileOperationTaskResult extends TaskResult {

	private static final long serialVersionUID = -5267005980338783100L;

	public static String ATTR_PATH = "attr.path";
	
	public FileOperationTaskResult(Object source, String resourceKey,
			boolean success, Realm currentRealm, Task task, String path) {
		super(source, resourceKey, success, currentRealm, task);
		addAttribute(ATTR_PATH, path);
	}

	public FileOperationTaskResult(Object source, String eventResourceKey,
			Throwable e, Realm currentRealm, Task task, String path) {
		super(source, eventResourceKey, e, currentRealm, task);
		addAttribute(ATTR_PATH, path);
	}

	@Override
	public boolean isPublishable() {
		return true;
	}

	@Override
	public String getResourceBundle() {
		return CopyFileTask.RESOURCE_BUNDLE;
	}

}
