package com.hypersocket.fs.tasks.create;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.fs.tasks.FileOperationTaskResult;
import com.hypersocket.realm.Realm;
import com.hypersocket.tasks.Task;

public class CreateFileTaskResult extends FileOperationTaskResult {

	private static final long serialVersionUID = -5091699080613107838L;

	public static final String EVENT_RESOURCE_KEY = "createFile.result";
	
	public CreateFileTaskResult(Object source, Realm currentRealm, Task task,
			String path) {
		super(source, EVENT_RESOURCE_KEY, true, currentRealm,
				task, path);
	}

	public CreateFileTaskResult(Object source, Throwable e, Realm currentRealm,
			Task task, String path) {
		super(source, EVENT_RESOURCE_KEY, e, currentRealm, task, path);
	}

	@Override
	public boolean isPublishable() {
		return true;
	}

	@Override
	public String getResourceBundle() {
		return CreateFileTask.RESOURCE_BUNDLE;
	}
	
	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
}
