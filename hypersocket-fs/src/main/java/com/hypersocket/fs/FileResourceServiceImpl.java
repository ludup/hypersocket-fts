package com.hypersocket.fs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hypersocket.browser.BrowserLaunchableService;
import com.hypersocket.config.ConfigurationPermission;
import com.hypersocket.events.EventService;
import com.hypersocket.fs.events.CopyFileEvent;
import com.hypersocket.fs.events.CreateFolderEvent;
import com.hypersocket.fs.events.DeleteFileEvent;
import com.hypersocket.fs.events.DownloadCompleteEvent;
import com.hypersocket.fs.events.DownloadStartedEvent;
import com.hypersocket.fs.events.FileOperationEvent;
import com.hypersocket.fs.events.FileResourceCreatedEvent;
import com.hypersocket.fs.events.FileResourceDeletedEvent;
import com.hypersocket.fs.events.FileResourceEvent;
import com.hypersocket.fs.events.FileResourceUpdatedEvent;
import com.hypersocket.fs.events.RenameEvent;
import com.hypersocket.fs.events.UploadCompleteEvent;
import com.hypersocket.fs.events.UploadStartedEvent;
import com.hypersocket.fs.tasks.CopyFileTaskResult;
import com.hypersocket.fs.tasks.CreateFileTask;
import com.hypersocket.fs.tasks.CreateFileTaskResult;
import com.hypersocket.fs.tasks.DeleteFolderTaskResult;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.menus.MenuRegistration;
import com.hypersocket.menus.MenuService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.PermissionCategory;
import com.hypersocket.permissions.PermissionService;
import com.hypersocket.permissions.PermissionType;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.realm.RealmService;
import com.hypersocket.realm.UserVariableReplacement;
import com.hypersocket.resource.AbstractAssignableResourceRepository;
import com.hypersocket.resource.AbstractAssignableResourceServiceImpl;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceException;
import com.hypersocket.resource.ResourceNotFoundException;
import com.hypersocket.resource.TransactionAdapter;
import com.hypersocket.server.HypersocketServer;
import com.hypersocket.ui.IndexPageFilter;
import com.hypersocket.ui.UserInterfaceContentHandler;
import com.hypersocket.upload.FileUploadService;
import com.hypersocket.vfs.VirtualFileSynchronizationService;

@Service
public class FileResourceServiceImpl extends AbstractAssignableResourceServiceImpl<FileResource>
		implements FileResourceService {

	static Logger log = LoggerFactory.getLogger(FileResourceServiceImpl.class);

	public static final String RESOURCE_BUNDLE = "FileResourceService";

	public static final String MENU_FILE_SYSTEMS = "fileSystems";

	public static final String ACTIONS_FILE = "fileActions";
	public static final String ACTIONS_BULK = "bulkActions";

	@Autowired
	HypersocketServer server;

	@Autowired
	I18NService i18nService;

	@Autowired
	PermissionService permissionService;

	@Autowired
	MenuService menuService;

	@Autowired
	UserInterfaceContentHandler jQueryUIContentHandler;

	@Autowired
	IndexPageFilter indexPage;
	
	@Autowired
	FileResourceRepository resourceRepository;

	@Autowired
	EventService eventService;

	@Autowired
	RealmService realmService;

	@Autowired
	FileUploadService uploadService;

	@Autowired
	UserVariableReplacement userVariableReplacement;
	
	@Autowired
	BrowserLaunchableService browserLaunchableService;
	
	@Autowired
	VirtualFileSynchronizationService syncService; 
	

	Map<String, FileResourceScheme> schemes = new HashMap<String, FileResourceScheme>();

	public FileResourceServiceImpl() {
		super("fileResource");
	}

	@PostConstruct
	public void postConstruct() {

		if (log.isDebugEnabled()) {
			log.debug("Constructing FileResourceService");
		}

		resourceRepository.loadPropertyTemplates("fileResourceTemplate.xml");

		i18nService.registerBundle(FileResourceServiceImpl.RESOURCE_BUNDLE);

		PermissionCategory cat = permissionService.registerPermissionCategory(FileResourceServiceImpl.RESOURCE_BUNDLE,
				"category.fileResources");

		for (FileResourcePermission p : FileResourcePermission.values()) {
			permissionService.registerPermission(p, cat);
		}

		menuService.registerMenu(new MenuRegistration(RESOURCE_BUNDLE, "fileSystems", "fa-folder-open", null, 200,
				FileResourcePermission.READ, FileResourcePermission.CREATE, FileResourcePermission.UPDATE,
				FileResourcePermission.DELETE), MenuService.MENU_RESOURCES);

		menuService.registerMenu(new MenuRegistration(RESOURCE_BUNDLE, "fileResources", "fa-folder-open", "filesystems",
				200, FileResourcePermission.READ, FileResourcePermission.CREATE, FileResourcePermission.UPDATE,
				FileResourcePermission.DELETE), MENU_FILE_SYSTEMS);

		menuService.registerMenu(
				new MenuRegistration(RESOURCE_BUNDLE, "myFilesystems", "fa-folder-open", "myFilesystems", 200) {
					public boolean canRead() {
						return resourceRepository.getAssignableResourceCount(
								realmService.getAssociatedPrincipals(getCurrentPrincipal())) > 0;
					}
				}, MenuService.MENU_MY_RESOURCES);

		menuService.registerMenu(new MenuRegistration(RESOURCE_BUNDLE,
				"fileSettings", "fa-cog", "fileSettings", 9999, 
				ConfigurationPermission.READ,
				null, 
				ConfigurationPermission.UPDATE,
				null), FileResourceServiceImpl.MENU_FILE_SYSTEMS);
		
		if (log.isInfoEnabled()) {
			log.info("VFS reports the following schemes available");

			try {
				for (String s : VFS.getManager().getSchemes()) {
					if (log.isInfoEnabled()) {
						log.info(s);
					}
				}
			} catch (FileSystemException e) {
				log.error("Could not load file schemes", e);
			}
		}

		eventService.registerEvent(FileResourceEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(FileResourceCreatedEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(FileResourceUpdatedEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(FileResourceDeletedEvent.class, RESOURCE_BUNDLE);

		eventService.registerEvent(FileOperationEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(DownloadStartedEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(DownloadCompleteEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(UploadStartedEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(UploadCompleteEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(CopyFileEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(CreateFolderEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(DeleteFileEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(RenameEvent.class, RESOURCE_BUNDLE);

		eventService.registerEvent(CopyFileTaskResult.class, CreateFileTask.RESOURCE_BUNDLE);
		eventService.registerEvent(CreateFileTaskResult.class, CreateFileTask.RESOURCE_BUNDLE);
		eventService.registerEvent(DeleteFolderTaskResult.class, CreateFileTask.RESOURCE_BUNDLE);

		registerScheme(new FileResourceScheme("file", false, false, false, false, -1, true, false, true, true ));
		registerScheme(new FileResourceScheme("tmp", true, false, false, false, -1, false, false, true, false));

		indexPage.addScript("${uiPath}/js/jstree.js");
		indexPage.addStyleSheet("${uiPath}/css/themes/default/style.min.css");
		indexPage.addStyleSheet("${uiPath}/css/fs.css");
		
		if (log.isDebugEnabled()) {
			log.debug("FileResourceService constructed");
		}
	}

	private boolean isVFSScheme(String scheme) {
		try {
			for (String s : VFS.getManager().getSchemes()) {
				if (s.equals(scheme)) {
					return true;
				}
			}
		} catch (FileSystemException e) {
		}
		return false;
	}

	@Override
	public void registerScheme(FileResourceScheme scheme) {
		if (schemes.containsKey(scheme.getScheme())) {
			throw new IllegalArgumentException(scheme.getScheme() + " is already a registerd scheme");
		} else if (scheme.getProvider() == null && !isVFSScheme(scheme.getScheme())) {
			throw new IllegalArgumentException(scheme.getScheme() + " is not a valid VFS scheme");
		}

		if (log.isInfoEnabled()) {
			log.info("Registering file resource scheme " + scheme.getScheme() + " isRemote=" + scheme.isRemote()
					+ " supportsCredentials=" + scheme.isSupportsCredentials());
		}

		try {

			if (scheme.getProvider() != null && !VFS.getManager().hasProvider(scheme.getScheme())) {
				((DefaultFileSystemManager) VFS.getManager()).addProvider(scheme.getScheme(),
						scheme.getProvider().newInstance());
				if (!isVFSScheme(scheme.getScheme())) {
					log.error("Scheme is still not reported as registred!");
					return;
				}
			}

			schemes.put(scheme.getScheme(), scheme);
		} catch (Throwable e) {
			log.error("Failed to add scheme " + scheme.getScheme(), e);
		}

	}

	@Override
	public FileResourceScheme getScheme(String name) {
		return schemes.get(name);
	}
	
	@Override
	public List<FileResourceScheme> getSchemes() {
		return new ArrayList<FileResourceScheme>(schemes.values());
	}

	
	@Override
	protected AbstractAssignableResourceRepository<FileResource> getRepository() {
		return resourceRepository;
	}

	@Override
	protected PermissionType getDeletePermission() {
		return FileResourcePermission.DELETE;
	}

	@Override
	protected PermissionType getReadPermission() {
		return FileResourcePermission.READ;
	}

	@Override
	protected String getResourceBundle() {
		return RESOURCE_BUNDLE;
	}

	@Override
	public Class<?> getPermissionType() {
		return FileResourcePermission.class;
	}

	@Override
	protected void fireResourceCreationEvent(FileResource resource) {
		eventService.publishEvent(new FileResourceCreatedEvent(this, getCurrentSession(), resource));
	}

	@Override
	protected void fireResourceCreationEvent(FileResource resource, Throwable t) {
		eventService.publishEvent(new FileResourceCreatedEvent(this, t, getCurrentSession(), resource));
	}

	@Override
	protected void fireResourceUpdateEvent(FileResource resource) {
		eventService.publishEvent(new FileResourceUpdatedEvent(this, getCurrentSession(), resource));
	}

	@Override
	protected void fireResourceUpdateEvent(FileResource resource, Throwable t) {
		eventService.publishEvent(new FileResourceUpdatedEvent(this, t, getCurrentSession(), resource));
	}

	@Override
	protected void fireResourceDeletionEvent(FileResource resource) {
		eventService.publishEvent(new FileResourceDeletedEvent(this, getCurrentSession(), resource));
	}

	@Override
	protected void fireResourceDeletionEvent(FileResource resource, Throwable t) {
		eventService.publishEvent(new FileResourceDeletedEvent(this, t, getCurrentSession(), resource));
	}

	@Override
	protected Class<FileResource> getResourceClass() {
		return FileResource.class;
	}

	@Override
	protected void updateFingerprint() {
		super.updateFingerprint();
		browserLaunchableService.updateFingerprint();
	}

	@Override
	public Collection<PropertyCategory> getPropertyTemplates(String scheme) throws AccessDeniedException {
		
		assertAnyPermission(FileResourcePermission.READ);
		
		List<PropertyCategory> results = new ArrayList<PropertyCategory>();
		
		FileResourceScheme fileScheme = schemes.get(scheme);
		if(fileScheme.getFileService()!=null) {
			if(fileScheme.getFileService().getRepository()!=null) {
				results.addAll(fileScheme.getFileService().getRepository().getPropertyCategories(null));
			}
		}
		results.addAll(getRepository().getPropertyCategories(null));
		return results;
	}

	@Override
	public Collection<PropertyCategory> getResourceProperties(FileResource resource) throws AccessDeniedException {
		
		assertAnyPermission(FileResourcePermission.READ);
		
		List<PropertyCategory> results = new ArrayList<PropertyCategory>();
		FileResourceScheme fileScheme = schemes.get(resource.getScheme());
		if(fileScheme.getFileService()!=null && fileScheme.getFileService().getRepository()!=null) {
			results.addAll(fileScheme.getFileService().getRepository().getPropertyCategories(resource));
		}
		
		results.addAll(getRepository().getPropertyCategories(resource));
		return results;
	}
	
	@Override
	public void createFileResource(FileResource resource, Map<String, String> properties) throws AccessDeniedException, ResourceException {
		createResource(resource, properties, new TransactionAdapter<FileResource>() {

			@Override
			public void afterOperation(FileResource resource, Map<String, String> properties) {
				
				FileResourceScheme scheme = schemes.get(resource.getScheme());
				if(scheme.getFileService()!=null) {
					if(scheme.getFileService().getRepository()!=null) {
						scheme.getFileService().getRepository().setValues(resource, properties);
					}
				}
				
				doInitialReconcile(resource, scheme);
			}
		});
	}

	private void doInitialReconcile(FileResource resource, FileResourceScheme scheme) {
		
		try {
			boolean makeDefault = !resource.isReadOnly();
			if(makeDefault) {
				Collection<FileResource> resources = getResourcesByVirtualPath(resource.getVirtualPath());
				for(FileResource r : resources) {
					if(!r.equals(resource)) {
						if(!r.isReadOnly()) {
							makeDefault = false;
							break;
						}
					}
				}
			}
			
			syncService.reconcileTopFolder(resource, resourceRepository.getIntValue(resource, "fs.initialReconcileDepth"), makeDefault, null);
				
		} catch (AccessDeniedException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		
	}
	
	@Override
	public void updateFileResource(FileResource resource, Map<String, String> properties) throws AccessDeniedException, ResourceException {
		
		try {
			FileResource original = getResourceById(resource.getId());
			if(!original.getPath().equals(resource.getPath())) {
				properties.put("fs.rebuildOnNextReconcile", "true");
			}
		} catch (ResourceNotFoundException e) {
			throw new ResourceChangeException(e);
		}
		
		updateResource(resource, null, properties, new TransactionAdapter<FileResource>() {

			@Override
			public void afterOperation(FileResource resource, Map<String, String> properties) {
				
				FileResourceScheme scheme = schemes.get(resource.getScheme());
				if(scheme.getFileService()!=null && scheme.getFileService().getRepository()!=null) {
					scheme.getFileService().getRepository().setValues(resource, properties);
				}
				
				doInitialReconcile(resource, scheme);
			}
		});
	}
	
	@Override
	public void resetRebuildReconcileStatus(FileResource resource) {
		getRepository().setValue(resource, "fs.rebuildOnNextReconcile", "false");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void deleteResource(FileResource resource) throws AccessDeniedException, ResourceException {
		
		super.deleteResource(resource, new TransactionAdapter<FileResource>() {

			@Override
			public void beforeOperation(FileResource resource, Map<String, String> properties) {
				syncService.removeFileResource(resource);
			}
			
		});
	}

	@Override
	public Collection<FileResource> getResourcesByVirtualPath(String virtualPath) throws AccessDeniedException {
		
		assertPermission(FileResourcePermission.READ);
		
		return resourceRepository.getResourcesByVirtualPath(virtualPath, getCurrentRealm());
	}
	
	@Override
	public Collection<FileResource> getNonRootResources() throws AccessDeniedException {
		
		assertPermission(FileResourcePermission.READ);
		
		return resourceRepository.getNonRootResources(getCurrentRealm());
	}

}
