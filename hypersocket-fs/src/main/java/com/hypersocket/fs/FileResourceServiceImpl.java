package com.hypersocket.fs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.apache.commons.vfs2.provider.temp.TemporaryFileProvider;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hypersocket.browser.BrowserLaunchableService;
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
import com.hypersocket.fs.tasks.copy.CopyFileTaskResult;
import com.hypersocket.fs.tasks.create.CreateFileTask;
import com.hypersocket.fs.tasks.create.CreateFileTaskResult;
import com.hypersocket.fs.tasks.delete.DeleteFolderTaskResult;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.menus.MenuService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.PermissionCategory;
import com.hypersocket.permissions.PermissionService;
import com.hypersocket.permissions.PermissionType;
import com.hypersocket.properties.EntityResourcePropertyStore;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.properties.ResourceUtils;
import com.hypersocket.realm.Principal;
import com.hypersocket.realm.RealmService;
import com.hypersocket.realm.UserVariableReplacementService;
import com.hypersocket.repository.CriteriaConfiguration;
import com.hypersocket.resource.AbstractAssignableResourceRepository;
import com.hypersocket.resource.AbstractAssignableResourceServiceImpl;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceCreationException;
import com.hypersocket.resource.ResourceException;
import com.hypersocket.resource.TransactionAdapter;
import com.hypersocket.server.HypersocketServer;
import com.hypersocket.ui.IndexPageFilter;
import com.hypersocket.ui.UserInterfaceContentHandler;
import com.hypersocket.upload.FileUploadService;
import com.hypersocket.vfs.VirtualFile;
import com.hypersocket.vfs.VirtualFileService;

@Service
public class FileResourceServiceImpl extends AbstractAssignableResourceServiceImpl<FileResource>
		implements FileResourceService {

	static Logger log = LoggerFactory.getLogger(FileResourceServiceImpl.class);

	public static final String RESOURCE_BUNDLE = "FileResourceService";

	public static final String MENU_FILE_SYSTEMS = "fileSystems";

	public static final String ACTIONS_FILE = "fileActions";
	public static final String ACTIONS_BULK = "fileToolbarActions";

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
	UserVariableReplacementService userVariableReplacement;
	
	@Autowired
	BrowserLaunchableService browserLaunchableService;

	@Autowired
	VirtualFileService virtualFileService; 
	
	List<FileResourceProcessor> processors = new ArrayList<FileResourceProcessor>();
	Map<String, FileResourceScheme> schemes = new HashMap<String, FileResourceScheme>();
	List<FileResourceScheme> userSchemes = new ArrayList<FileResourceScheme>();
	
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
		
		if (log.isInfoEnabled()) {
			log.info("VFS reports the following schemes available");

			try {
				for (String s : virtualFileService.getManager(null, CacheStrategy.ON_CALL).getSchemes()) {
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

		if(!Boolean.getBoolean("fs.disableLocalFiles")) {
			registerScheme(new FileResourceScheme("file", false, false, false, false, -1, true, false, true, true, DefaultLocalFileProvider.class));
			registerScheme(new FileResourceScheme("tmp", true, false, false, false, -1, false, false, true, false, TemporaryFileProvider.class));
		}
		
		indexPage.addScript("${uiPath}/js/jstree.js");
		indexPage.addScript("${uiPath}/js/fileTree.js");
		indexPage.addStyleSheet("${uiPath}/css/themes/default/style.min.css");
		indexPage.addStyleSheet("${uiPath}/css/fs.css");
		
		if (log.isDebugEnabled()) {
			log.debug("FileResourceService constructed");
		}

		EntityResourcePropertyStore.registerResourceService(FileResource.class, getRepository());
	}

	@Override
	public void registerProcessor(FileResourceProcessor processor) {
		processors.add(processor);
	}
	
	@Override
	public boolean hasScheme(String scheme) {
		return schemes.containsKey(scheme);
	}
	
	@Override
	public void registerScheme(FileResourceScheme scheme) {
		if (schemes.containsKey(scheme.getScheme())) {
			throw new IllegalArgumentException(scheme.getScheme() + " is already a registerd scheme");
		}

		if (log.isInfoEnabled()) {
			log.info("Registering file resource scheme " + scheme.getScheme() + " isRemote=" + scheme.isRemote()
					+ " supportsCredentials=" + scheme.isSupportsCredentials());
		}

		try {
			virtualFileService.addProvider(scheme.getScheme(), scheme.getProvider()==null ? null : scheme.getProvider().newInstance());
			schemes.put(scheme.getScheme(), scheme);
			if(scheme.isUserFilesystem()) {
				userSchemes.add(scheme);
			}
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
		return userSchemes;
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
			public void beforeOperation(FileResource resource, Map<String,String> properties) {
				processFile(resource, properties);
			}
			
			@Override
			public void afterOperation(FileResource resource, Map<String, String> properties) {
				setSchemeProperties(resource, properties);
				configureDefaults(resource, true);
			}
		});
	}
	
	protected void processFile(FileResource resource, Map<String, String> properties) {
		resource.setFlags("r" + (resource.isReadOnly() ? "" : "w"));
		for(FileResourceProcessor processor : processors) {
			processor.processFileResource(resource, properties);
		}
	}

	protected void configureDefaults(FileResource resource, boolean isNew) {
		try {
			VirtualFile mountedFile = virtualFileService.getFile(resource.getVirtualPath(), resource.getRealm());
			if(isNew) {
				virtualFileService.attachMount(mountedFile, resource);
			}
			FileResourceScheme scheme = getScheme(resource.getScheme());
			if(mountedFile.getDefaultMount()==null && !resource.isReadOnly() && scheme.isUserFilesystem()) {
				virtualFileService.setDefaultMount(mountedFile, resource);
			}
		} catch (Throwable e) {
			ResourceException re = new ResourceCreationException(RESOURCE_BUNDLE, 
					"error.virtualFileError", e.getMessage());
			re.initCause(e);
			throw new IllegalStateException(re.getMessage(), re);
		}
	}

	protected void setSchemeProperties(FileResource resource, Map<String, String> properties) {
		FileResourceScheme scheme = schemes.get(resource.getScheme());
		if(scheme.getFileService()!=null) {
			if(scheme.getFileService().getRepository()!=null) {
				scheme.getFileService().getRepository().setValues(resource, properties);
			}
		}
	}

	@Override
	public void updateFileResource(FileResource resource, Map<String, String> properties) throws AccessDeniedException, ResourceException {
		
		updateResource(resource, null, properties, new TransactionAdapter<FileResource>() {

			@Override
			public void beforeOperation(FileResource resource, Map<String,String> properties) {
				processFile(resource, properties);
			}
			
			@Override
			public void afterOperation(FileResource resource, Map<String, String> properties) {
				setSchemeProperties(resource, properties);
				configureDefaults(resource, false);
			}
		});
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public void deleteResource(FileResource resource) throws AccessDeniedException, ResourceException {
		
		try {
			virtualFileService.detachMount(resource);
		} catch (Throwable e) {
			ResourceChangeException re = new ResourceChangeException(RESOURCE_BUNDLE, "error.virtualFileError", e.getMessage());
			re.initCause(re);
			throw re;
		}
		
		super.deleteResource(resource);
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

	@Override
	public Collection<FileResource> getPersonalResources(Principal principal, final Set<FileResource> folderMounts) {
		
		return resourceRepository.getAssignedResources(realmService.getAssociatedPrincipals(principal), new CriteriaConfiguration() {
			@Override
			public void configure(Criteria criteria) {
				criteria.add(Restrictions.in("id", ResourceUtils.createResourceIdArray(folderMounts)));
			}
		});
	}

}
