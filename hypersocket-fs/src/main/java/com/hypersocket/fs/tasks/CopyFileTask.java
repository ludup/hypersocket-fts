package com.hypersocket.fs.tasks;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.vfs2.FileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hypersocket.events.SystemEvent;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.properties.ResourceTemplateRepository;
import com.hypersocket.tasks.AbstractTaskProvider;
import com.hypersocket.tasks.Task;
import com.hypersocket.tasks.TaskProviderService;
import com.hypersocket.triggers.TaskResult;
import com.hypersocket.triggers.TriggerResourceService;
import com.hypersocket.triggers.ValidationException;

@Component
public class CopyFileTask extends AbstractTaskProvider {

	static Logger log = LoggerFactory.getLogger(CopyFileTask.class);

	public static final String PROTOCOL = "TASK";

	public static final String RESOURCE_BUNDLE = "FileTask";

	public static final String ACTION_COPY_FILE = "copyFile";
	public static final String ACTION_COPY_DIR = "copyDir";
	public static final String ACTION_MOVE = "move";
	public static final String ACTION_RENAME = "rename";

	@Autowired
	CopyFileTaskRepository repository;

	@Autowired
	FileResourceService service;

	@Autowired
	TriggerResourceService triggerService;

	@Autowired
	I18NService i18nService;

	@Autowired
	TaskProviderService taskService;

	@PostConstruct
	private void postConstruct() {

		i18nService.registerBundle(RESOURCE_BUNDLE);
		taskService.registerTaskProvider(this);
	}

	@Override
	public String getResourceBundle() {
		return RESOURCE_BUNDLE;
	}

	@Override
	public String[] getResourceKeys() {
		return new String[] { ACTION_COPY_FILE, ACTION_COPY_DIR, ACTION_MOVE,
				ACTION_RENAME };
	}

	@Override
	public void validate(Task task, Map<String, String> parameters)
			throws ValidationException {
		if (parameters.containsKey("origin.path")) {
			throw new ValidationException("Origin path required");
		}
		if (parameters.containsKey("destination.path")) {
			throw new ValidationException("Destination path required");
		}
	}

	@Override
	public TaskResult execute(Task task, SystemEvent event)
			throws ValidationException {

		String originPath = repository.getValue(task, "origin.path");
		String destinationPath = repository.getValue(task, "destination.path");

		if (log.isInfoEnabled()) {
			log.info("Origin path " + originPath);
			log.info("Destination path " + destinationPath);
		}
		try {

			if (task.getResourceKey().equals(ACTION_COPY_FILE)
					|| task.getResourceKey().equals(ACTION_COPY_DIR)) {

				service.copyFile(originPath, destinationPath, PROTOCOL);
			} else if (task.getResourceKey().equals(ACTION_MOVE)) {
				
				FileResource resource = service.getMountForPath(originPath);
				String originChildPath = service.resolveChildPath(resource,
						originPath);
				FileObject originFile = service.resolveMountFile(resource);
				originFile = originFile.resolveFile(originChildPath);
				if (originFile.exists()) {
					String destinationChildPath = service.resolveChildPath(
							resource, destinationPath);
					FileObject destinationFile = service
							.resolveMountFile(resource);
					destinationFile = destinationFile
							.resolveFile(destinationChildPath);
					originFile.moveTo(destinationFile);
				} else {
					throw new Exception("File " + originPath
							+ " does not exist");
				}
			} else if (task.getResourceKey().equals(ACTION_RENAME)) {
				service.renameFile(originPath, destinationPath, PROTOCOL);
			} else {
				throw new ValidationException(
						"Invalid resource key for file task");
			}

			return new CopyFileTaskResult(this, event.getCurrentRealm(), task,
					originPath, destinationPath);
		} catch (Exception e) {
			log.error("Failed to fully process file request for origin: "
					+ originPath + ", destination: " + destinationPath, e);
			return new CopyFileTaskResult(this, e, event.getCurrentRealm(),
					task, originPath, destinationPath);
		}
	}

	@Override
	public ResourceTemplateRepository getRepository() {
		return repository;
	}
}
