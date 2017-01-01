package com.hypersocket.fs.tasks;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hypersocket.events.SystemEvent;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.properties.ResourceTemplateRepository;
import com.hypersocket.realm.Realm;
import com.hypersocket.tasks.AbstractRetryTaskProvider;
import com.hypersocket.tasks.Task;
import com.hypersocket.tasks.TaskProviderService;
import com.hypersocket.triggers.AbstractTaskResult;
import com.hypersocket.triggers.TriggerResourceService;
import com.hypersocket.triggers.ValidationException;
import com.hypersocket.vfs.VirtualFileService;

@Component
public class CopyFileTask extends AbstractRetryTaskProvider {

	static Logger log = LoggerFactory.getLogger(CopyFileTask.class);

	public static final String PROTOCOL = "TASK";

	public static final String RESOURCE_BUNDLE = "FileTask";

	public static final String ACTION_COPY_FILE = "copyFile";
	public static final String ACTION_COPY_DIR = "copyDir";
	public static final String ACTION_RENAME = "rename";

	@Autowired
	CopyFileTaskRepository repository;

	@Autowired
	VirtualFileService fileResourceService;

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
		return new String[] { ACTION_COPY_FILE, ACTION_COPY_DIR, 
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
	public AbstractTaskResult onExecute(Task task, Realm currentRealm, SystemEvent event)
			throws ValidationException {

		String originPath = processTokenReplacements(repository.getValue(task, "origin.path"), event);
		String destinationPath = processTokenReplacements(repository.getValue(task, "destination.path"), event);

		if (log.isInfoEnabled()) {
			log.info("Origin path " + originPath);
			log.info("Destination path " + destinationPath);
		}
	
		if (task.getResourceKey().equals(ACTION_COPY_FILE)
				|| task.getResourceKey().equals(ACTION_COPY_DIR)) {

			try {
				fileResourceService.copyFile(originPath, destinationPath, PROTOCOL);
				return new CopyFileTaskResult(this, currentRealm, task,
						originPath, destinationPath);
			} catch (Throwable e) {
				return new CopyFileTaskResult(this, e, currentRealm,
						task, originPath, destinationPath);
		
			}
		} else if (task.getResourceKey().equals(ACTION_RENAME)) {
			try {
				fileResourceService.renameFile(originPath, destinationPath, PROTOCOL);
				return new RenameFileTaskResult(this, currentRealm, task,
						originPath, destinationPath);
			} catch (Throwable e) {
				return new RenameFileTaskResult(this, e, currentRealm,
						task, originPath, destinationPath);
		
			}
		} else {
			throw new ValidationException(
					"Invalid resource key for file task");
		}

	}
	
	public String[] getResultResourceKeys() {
		return new String[] { CopyFileTaskResult.EVENT_RESOURCE_KEY };
	}

	@Override
	public ResourceTemplateRepository getRepository() {
		return repository;
	}
}
