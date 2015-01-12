package com.hypersocket.fs.tasks;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hypersocket.events.SystemEvent;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.properties.ResourceTemplateRepository;
import com.hypersocket.scheduler.SchedulerService;
import com.hypersocket.tasks.AbstractTaskProvider;
import com.hypersocket.tasks.Task;
import com.hypersocket.tasks.TaskProviderService;
import com.hypersocket.triggers.TaskResult;
import com.hypersocket.triggers.TriggerResourceService;
import com.hypersocket.triggers.ValidationException;

@Component
public class CreateFileTask extends AbstractTaskProvider {

	static Logger log = LoggerFactory.getLogger(CreateFileTask.class);

	public static final String PROTOCOL = "SFTP";

	public static final String RESOURCE_BUNDLE = "FileTask";

	public static final String ACTION_MKDIR = "mkdir";
	public static final String ACTION_DELETE_FILE = "deleteFile";
	public static final String ACTION_TRUNCATE_FILE = "truncateFile";
	public static final String ACTION_TOUCH_FILE = "touchFile";

	@Autowired
	CreateFileTaskRepository repository;

	@Autowired
	FileResourceService service;

	@Autowired
	TriggerResourceService triggerService;

	@Autowired
	I18NService i18nService;

	@Autowired
	SchedulerService schedulerService;

	@Autowired
	TaskProviderService taskService;

	@PostConstruct
	private void postConstruct() {

		i18nService.registerBundle(RESOURCE_BUNDLE);
		taskService.registerActionProvider(this);
	}

	@Override
	public String getResourceBundle() {
		return RESOURCE_BUNDLE;
	}

	@Override
	public String[] getResourceKeys() {
		return new String[] { ACTION_MKDIR, ACTION_DELETE_FILE,
				ACTION_TRUNCATE_FILE, ACTION_TOUCH_FILE };
	}

	@Override
	public void validate(Task task, Map<String, String> parameters)
			throws ValidationException {
		if (parameters.containsKey("file.path")) {
			throw new ValidationException("Path required");
		}
	}

	@Override
	public TaskResult execute(Task task, SystemEvent event)
			throws ValidationException {

		String path = repository.getValue(task, "file.path");
		// try {

		if (log.isInfoEnabled()) {
			log.info("Path " + path);
		}
		try {
			int index = path.lastIndexOf("/");
			String parentPath = path.substring(0, index);
			String name = path.substring(index, path.length());
			
			if (task.getResourceKey().equals(ACTION_MKDIR)) {
				service.createFolder(parentPath, name, PROTOCOL);

			} else if (task.getResourceKey().equals(ACTION_DELETE_FILE)) {
				 service.deleteFile(path, PROTOCOL);
			} else if (task.getResourceKey().equals(ACTION_TRUNCATE_FILE)) {
				// service.
			} else if (task.getResourceKey().equals(ACTION_TOUCH_FILE)) {
//				 service.createResource(resource, properties);
			} else {
				throw new ValidationException(
						"Invalid resource key for file task");
			}

			return new CreateFileTaskResult(this, event.getCurrentRealm(),
					task, path);
		} catch (IOException | AccessDeniedException e) {
			log.error("Failed to fully process file request for " + path, e);
			return new CreateFileTaskResult(this, e, event.getCurrentRealm(), task,
					 path);
		}
		// } catch (UnknownHostException) {
		// log.error("Failed to fully process file request for " + path, e);
		// return new FileTaskResult(this, e, event.getCurrentRealm(), task,
		// path);
		// }
	}

	@Override
	public ResourceTemplateRepository getRepository() {
		return repository;
	}
}
