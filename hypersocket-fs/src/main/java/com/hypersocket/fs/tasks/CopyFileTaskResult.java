package com.hypersocket.fs.tasks;

import com.hypersocket.events.SystemEventStatus;
import com.hypersocket.realm.Realm;
import com.hypersocket.tasks.Task;
import com.hypersocket.triggers.TaskResult;

public class CopyFileTaskResult extends TaskResult {

	private static final long serialVersionUID = -7915027397936300413L;

	public CopyFileTaskResult(Object source, Realm currentRealm, Task task,
			String originPath, String destinationPath) {
		super(source, "file.path", SystemEventStatus.SUCCESS, currentRealm,
				task);
		addAttribute("origin.path", originPath);
		addAttribute("destination.path", destinationPath);
	}

	public CopyFileTaskResult(Object source, Throwable e, Realm currentRealm,
			Task task, String originPath, String destinationPath) {
		super(source, "file.path", e, currentRealm, task);
		addAttribute("origin.path", originPath);
		addAttribute("destination.path", destinationPath);
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
