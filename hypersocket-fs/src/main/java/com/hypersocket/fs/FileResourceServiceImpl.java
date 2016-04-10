package com.hypersocket.fs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.hypersocket.browser.BrowserLaunchableService;
import com.hypersocket.config.ConfigurationChangedEvent;
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
import com.hypersocket.resource.ResourceCreationException;
import com.hypersocket.resource.TransactionAdapter;
import com.hypersocket.server.HypersocketServer;
import com.hypersocket.ui.IndexPageFilter;
import com.hypersocket.ui.UserInterfaceContentHandler;
import com.hypersocket.upload.FileUploadService;

@Service
public class FileResourceServiceImpl extends AbstractAssignableResourceServiceImpl<FileResource>
		implements FileResourceService,
		ApplicationListener<ConfigurationChangedEvent> {

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

		menuService.registerExtendableTable(ACTIONS_FILE);
		menuService.registerExtendableTable(ACTIONS_BULK);

		if (log.isInfoEnabled()) {
			log.info("VFS reports the following schemes available");

			try {
				for (String s : VFS.getManager().getSchemes()) {
					if (log.isInfoEnabled())
						log.info(s);
				}
			} catch (FileSystemException e) {
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

		registerScheme(new FileResourceScheme("file", false, false, false));
		registerScheme(new FileResourceScheme("ftp", true, true, true));
		registerScheme(new FileResourceScheme("ftps", true, true, true));
		registerScheme(new FileResourceScheme("http", true, true, true));
		registerScheme(new FileResourceScheme("https", true, true, true));
		registerScheme(new FileResourceScheme("tmp", false, false, false));
		registerScheme(new FileResourceScheme("smb", true, true, false));

		jQueryUIContentHandler.addAlias("^/viewfs/.*$", "content/fileview.html");
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
	
	protected FileSystemOptions buildFilesystemOptions(FileResource resource) {
		FileResourceScheme scheme = schemes.get(resource.getScheme());
		return scheme.getFileService().buildFileSystemOptions(resource);
		
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
	public void onApplicationEvent(ConfigurationChangedEvent event) {

		if (event.getAttribute(ConfigurationChangedEvent.ATTR_CONFIG_RESOURCE_KEY).startsWith("jcifs.")) {
			jcifs.Config.setProperty(event.getAttribute(ConfigurationChangedEvent.ATTR_CONFIG_RESOURCE_KEY),
					event.getAttribute(ConfigurationChangedEvent.ATTR_NEW_VALUE));
		}
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
			results.addAll(fileScheme.getFileService().getRepository().getPropertyCategories(null));
		}
		results.addAll(getRepository().getPropertyCategories(null));
		return results;
	}

	@Override
	public Collection<PropertyCategory> getResourceProperties(FileResource resource) throws AccessDeniedException {
		
		assertAnyPermission(FileResourcePermission.READ);
		
		List<PropertyCategory> results = new ArrayList<PropertyCategory>();
		FileResourceScheme fileScheme = schemes.get(resource.getScheme());
		if(fileScheme.getFileService()!=null) {
			results.addAll(fileScheme.getFileService().getRepository().getPropertyCategories(resource));
		}
		
		results.addAll(getRepository().getPropertyCategories(resource));
		return results;
	}

	@Override
	public void createFileResource(FileResource resource, Map<String, String> properties) throws ResourceCreationException, AccessDeniedException {
		createResource(resource, properties, new TransactionAdapter<FileResource>() {

			@Override
			public void afterOperation(FileResource resource, Map<String, String> properties) {
				
				FileResourceScheme scheme = schemes.get(resource.getScheme());
				if(scheme.getFileService()!=null) {
					scheme.getFileService().getRepository().setValues(resource, properties);
				}
				
			}
		});
	}

	@Override
	public void updateFileResource(FileResource resource, Map<String, String> properties) throws ResourceChangeException, AccessDeniedException {
		updateResource(resource, properties, new TransactionAdapter<FileResource>() {

			@Override
			public void afterOperation(FileResource resource, Map<String, String> properties) {
				
				FileResourceScheme scheme = schemes.get(resource.getScheme());
				if(scheme.getFileService()!=null) {
					scheme.getFileService().getRepository().setValues(resource, properties);
				}
				
			}
		});
	}

	@Override
	public Collection<FileResource> getResourcesByVirtualPath(String virtualPath) throws AccessDeniedException {
		
		assertPermission(FileResourcePermission.READ);
		
		return resourceRepository.getResourcesByVirtualPath(virtualPath);
	}

}
