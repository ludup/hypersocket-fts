package com.hypersocket.fs.tasks;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.realm.Realm;
import com.hypersocket.tasks.Task;

public class AbstractCopyFileTaskResult extends FileOperationTaskResult {

	private static final long serialVersionUID = -7915027397936300413L;
	
	public static final String ATTR_DESTINATION = "attr.destination";
	
	public AbstractCopyFileTaskResult(Object source, Realm currentRealm, Task task,
			String resourceKey,
			String originPath, String destinationPath) {
		super(source, resourceKey, true, currentRealm,
				task, originPath);
		addAttribute(ATTR_DESTINATION, destinationPath);
	}

	public AbstractCopyFileTaskResult(Object source, Throwable e, Realm currentRealm,
			Task task, String resourceKey, String originPath, String destinationPath) {
		super(source, resourceKey, e, currentRealm, task, originPath);
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
		return ArrayUtils.add(super.getResourceKeys(), getResourceKey());
	}
}
