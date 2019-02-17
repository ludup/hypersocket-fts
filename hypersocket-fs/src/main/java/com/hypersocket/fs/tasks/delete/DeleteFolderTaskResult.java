package com.hypersocket.fs.tasks.delete;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.fs.tasks.FileOperationTaskResult;
import com.hypersocket.fs.tasks.copy.CopyFileTask;
import com.hypersocket.realm.Realm;
import com.hypersocket.tasks.Task;

public class DeleteFolderTaskResult extends FileOperationTaskResult {

	private static final long serialVersionUID = 3891741262457370571L;

	public static final String EVENT_RESOURCE_KEY = "deleteFolder.result";
	
	public DeleteFolderTaskResult(Object source, Realm currentRealm, Task task,
			String path, boolean deleteNonEmpty) {
		super(source, EVENT_RESOURCE_KEY, true, currentRealm,
				task, path);
	}

	public DeleteFolderTaskResult(Object source, Throwable e,
			Realm currentRealm, Task task, String path, boolean deleteNonEmpty) {
		super(source, EVENT_RESOURCE_KEY, e, currentRealm, task, path);
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
