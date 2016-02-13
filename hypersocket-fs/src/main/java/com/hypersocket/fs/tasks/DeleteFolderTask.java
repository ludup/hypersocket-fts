package com.hypersocket.fs.tasks;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hypersocket.events.SystemEvent;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.properties.ResourceTemplateRepository;
import com.hypersocket.realm.Realm;
import com.hypersocket.tasks.AbstractTaskProvider;
import com.hypersocket.tasks.Task;
import com.hypersocket.tasks.TaskProviderService;
import com.hypersocket.triggers.AbstractTaskResult;
import com.hypersocket.triggers.TriggerResourceService;
import com.hypersocket.triggers.ValidationException;

@Component
public class DeleteFolderTask extends AbstractTaskProvider {

	static Logger log = LoggerFactory.getLogger(DeleteFolderTask.class);

	public static final String PROTOCOL = "TASK";

	public static final String RESOURCE_BUNDLE = "FileTask";

	public static final String RESOURCE_KEY = "deleteFolder";

	@Autowired
	DeleteFolderTaskRepository repository;

	@Autowired
	FileResourceService fileResourceService;

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
		return new String[] { RESOURCE_KEY };
	}

	@Override
	public void validate(Task task, Map<String, String> parameters)
			throws ValidationException {
		if (parameters.containsKey("folder.path")) {
			throw new ValidationException("Path required");
		}
		if (parameters.containsKey("folder.deleteNonEmpty")) {
			throw new ValidationException("Delete non-empty required");
		}
	}

	@Override
	public AbstractTaskResult execute(Task task, Realm currentRealm, SystemEvent event)
			throws ValidationException {

		String path = processTokenReplacements(repository.getValue(task, "folder.path"), event);
		boolean deleteNonEmpty = repository.getBooleanValue(task,
				"folder.deleteNonEmpty");

		if (log.isInfoEnabled()) {
			log.info("Path " + path);
			log.info("Delete non-empty " + deleteNonEmpty);
		}
		try {
			if (!deleteNonEmpty) {
				fileResourceService.deleteFile(path, PROTOCOL);
			} else {
				FileResource resource = fileResourceService.getMountForPath(path);
				deleteFolder(path, resource);
			}

			return new DeleteFolderTaskResult(this, currentRealm,
					task, path, deleteNonEmpty);

		} catch (IOException | AccessDeniedException e) {
			log.error("Failed to fully process folder request for path: "
					+ path + ", delete non-empty: " + deleteNonEmpty, e);
			return new DeleteFolderTaskResult(this, e, currentRealm,
					task, path, deleteNonEmpty);
		}
	}
	
	public String[] getResultResourceKeys() {
		return new String[] { DeleteFolderTaskResult.EVENT_RESOURCE_KEY };
	}

	public void deleteFolder(String path, FileResource resource) throws IOException {
		
		String childPath = fileResourceService.resolveChildPath(resource, path);
		FileObject file = fileResourceService.resolveMountFile(resource);
		file = file.resolveFile(childPath);
		FileObject[] children=file.getChildren();
		
		for(int x=0; x<children.length; x++){
			if(children[x].getType() != FileType.FOLDER){
				children[x].delete();
			}else{
				deleteFolder(path + "/" + children[x].getName().getBaseName(), resource);
			}
		}
		file.delete();
	}

	@Override
	public ResourceTemplateRepository getRepository() {
		return repository;
	}
}
