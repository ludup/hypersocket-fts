package com.hypersocket.fs.tasks;

import com.hypersocket.events.SystemEventStatus;
import com.hypersocket.realm.Realm;
import com.hypersocket.tasks.Task;
import com.hypersocket.triggers.TaskResult;

public class DeleteFolderTaskResult extends TaskResult {

	private static final long serialVersionUID = 3891741262457370571L;

	public DeleteFolderTaskResult(Object source, Realm currentRealm, Task task,
			String path, boolean deleteNonEmpty) {
		super(source, "folder.path", SystemEventStatus.SUCCESS, currentRealm,
				task);
		addAttribute("folder.path", path);
		addAttribute("folder.deleteNonEmpty", deleteNonEmpty);
	}

	public DeleteFolderTaskResult(Object source, Throwable e,
			Realm currentRealm, Task task, String path, boolean deleteNonEmpty) {
		super(source, "folder.path", e, currentRealm, task);
		addAttribute("folder.path", path);
		addAttribute("folder.deleteNonEmpty", deleteNonEmpty);
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
