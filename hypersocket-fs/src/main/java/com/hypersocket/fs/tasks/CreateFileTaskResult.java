package com.hypersocket.fs.tasks;

import com.hypersocket.events.SystemEventStatus;
import com.hypersocket.realm.Realm;
import com.hypersocket.tasks.Task;
import com.hypersocket.triggers.TaskResult;

public class CreateFileTaskResult extends TaskResult {

	private static final long serialVersionUID = -5091699080613107838L;

	public CreateFileTaskResult(Object source, Realm currentRealm, Task task,
			String path) {
		super(source, "file.path", SystemEventStatus.SUCCESS, currentRealm,
				task);
		addAttribute("file.path", path);
	}

	public CreateFileTaskResult(Object source, Throwable e, Realm currentRealm,
			Task task, String path) {
		super(source, "file.path", e, currentRealm, task);
		addAttribute("file.path", path);
	}

	@Override
	public boolean isPublishable() {
		return true;
	}

	@Override
	public String getResourceBundle() {
		return CreateFileTask.RESOURCE_BUNDLE;
	}

}
