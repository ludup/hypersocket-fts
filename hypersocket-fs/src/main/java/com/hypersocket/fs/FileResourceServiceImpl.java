package com.hypersocket.fs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.derby.impl.io.vfmem.PathUtil;
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
import com.hypersocket.i18n.I18N;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.menus.MenuRegistration;
import com.hypersocket.menus.MenuService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.PermissionCategory;
import com.hypersocket.permissions.PermissionService;
import com.hypersocket.permissions.PermissionType;
import com.hypersocket.permissions.SystemPermission;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.realm.Realm;
import com.hypersocket.realm.RealmService;
import com.hypersocket.realm.UserVariableReplacement;
import com.hypersocket.resource.AbstractAssignableResourceRepository;
import com.hypersocket.resource.AbstractAssignableResourceServiceImpl;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceCreationException;
import com.hypersocket.resource.TransactionAdapter;
import com.hypersocket.server.HypersocketServer;
import com.hypersocket.session.Session;
import com.hypersocket.ui.UserInterfaceContentHandler;
import com.hypersocket.upload.FileStore;
import com.hypersocket.upload.FileUpload;
import com.hypersocket.upload.FileUploadService;
import com.hypersocket.utils.FileUtils;

@Service
public class FileResourceServiceImpl extends AbstractAssignableResourceServiceImpl<FileResource>
		implements FileResourceService, DownloadEventProcessor, UploadEventProcessor,
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

	private String normalise(String path) {
		path = Paths.get(path).normalize().toString();
		if (File.separatorChar == '\\') {
			path = path.replace('\\', '/');
		}
		return path;
	}

	public FileResource getMountForURIPath(String host, String controllerPath, String path)
			throws AccessDeniedException {

		path = normalise(path);
		if (isURIFilesystemRoot(path)) {
			throw new IllegalArgumentException(
					path + " is the root path! use isWebDAVRoot to check before calling this method");
		} else {
			String rootPath = FileUtils.checkEndsWithSlash(server.resolvePath(controllerPath));
			return getMountForPath(rootPath, path);
		}
	}

	@Override
	public FileResource getMountForPath(String path) throws AccessDeniedException {
		return getMountForPath("/", path);
	}

	private FileResource getMountForPath(String rootPath, String path) throws AccessDeniedException {
		String mountPath = "";
		String mountName = "";

		try {
			mountPath = FileUtils.stripParentPath(rootPath, path);
			mountName = FileUtils.firstPathElement(mountPath);

			assertAnyPermission(SystemPermission.SYSTEM, SystemPermission.SYSTEM_ADMINISTRATION);
			return getResourceByName(mountName);

		} catch (Exception e) {
			for (FileResource r : getPersonalResources()) {
				if (r.getName().equalsIgnoreCase(mountName)) {
					return r;
				}
			}
			log.error("Failed to resolve mount for path " + path);
		}
		return null;
	}

	@Override
	public List<FileResourceScheme> getSchemes() {
		return new ArrayList<FileResourceScheme>(schemes.values());
	}

	public boolean isURIFilesystemRoot(String path) {
		return FileUtils.checkEndsWithSlash(server.resolvePath("fs")).equals(FileUtils.checkEndsWithSlash(path));
	}

	public boolean isURIMountResource(FileResource resource, String controllerPath, String path) {

		path = normalise(path);

		String mountPath = FileUtils.checkEndsWithSlash(server.resolvePath(controllerPath + "/" + resource.getName()));
		path = FileUtils.checkEndsWithSlash(path);
		if (!path.startsWith(mountPath)) {
			throw new IllegalArgumentException(path + " is not a child of mount " + resource.getName());
		}
		return mountPath.equals(path);
	}

	@Override
	public String resolveURIChildPath(FileResource resource, String controllerPath, String path) throws IOException {
		return resolveChildPath(resource, server.resolvePath(controllerPath), path);
	}

	@Override
	public String resolveChildPath(FileResource resource, String path) throws IOException {
		return resolveChildPath(resource, "/", path);
	}

	private String resolveChildPath(FileResource resource, String rootPath, String path) throws IOException {

		rootPath = normalise(rootPath);
		path = normalise(path);

		return FileUtils.checkEndsWithNoSlash(FileUtils.stripParentPath(
				FileUtils.checkEndsWithSlash(rootPath) + FileUtils.checkEndsWithSlash(resource.getName()),
				FileUtils.checkStartsWithSlash(path)));
	}

	public String resolveChildPath(String mountName, String path) throws IOException {

		mountName = normalise(mountName);
		path = normalise(path);

		return FileUtils.checkEndsWithNoSlash(FileUtils.stripParentPath(FileUtils.checkEndsWithSlash("/" + mountName),
				FileUtils.checkEndsWithSlash(path)));
	}

	public String resolveURIChildPath(String mountName, String controllerPath, String path) throws IOException {

		path = normalise(path);

		return FileUtils.checkEndsWithNoSlash(FileUtils.stripParentPath(
				FileUtils.checkEndsWithSlash(server.resolvePath(controllerPath + "/" + mountName)),
				FileUtils.checkEndsWithSlash(path)));
	}

	public String resolveURIMountName(String controllerPath, String path) throws IOException {
		return resolveMountName(server.resolvePath(controllerPath), path);
	}

	private String resolveMountName(String rootPath, String path) throws IOException {

		rootPath = normalise(rootPath);
		path = normalise(path);

		String mountPath = FileUtils.stripParentPath(rootPath, path);
		String mountName = FileUtils.firstPathElement(mountPath);
		return mountName;
	}

	public String resolveMountName(String path) throws IOException {
		return resolveMountName("/", path);
	}

	@Override
	public FileObject resolveMountFile(FileResource resource) throws IOException {

		// TODO verify permissions

		return resolveVFSFile(resource);
	}

	@Override
	public InputStream downloadFile(String path, final long position, final String protocol)
			throws IOException, AccessDeniedException {

		// TODO verify download permission on mount

		path = normalise(path);

		FileResolver<InputStream> resolver = new FileResolver<InputStream>() {
			@Override
			InputStream onFileResolved(FileResource resource, String childPath, FileObject file) throws IOException {

				InputStream in = file.getContent().getInputStream();
				DownloadStartedEvent evt = downloadStarted(resource, childPath, file, in, protocol);

				return new ContentInputStream(resource, childPath, file, evt.getInputStream(), position,
						file.getContent().getSize() - position, FileResourceServiceImpl.this, evt.getTimestamp(), protocol,
						getCurrentSession());
			}

			@Override
			void onFileUnresolved(String mountName, String childPath, IOException t) {
				eventService.publishEvent(
						new DownloadStartedEvent(this, t, getCurrentSession(), mountName, childPath, protocol));
			}
		};
		return resolver.processRequest(path);

	}

	@Override
	public OutputStream uploadFile(String path, final long position, final String protocol)
			throws IOException, AccessDeniedException {

		// TODO verify download permission on mount

		path = normalise(path);

		FileResolver<OutputStream> resolver = new FileResolver<OutputStream>() {
			@Override
			OutputStream onFileResolved(FileResource resource, String childPath, FileObject file) throws IOException {

				UploadStartedEvent event = uploadStarted(resource, childPath, file, protocol);
				
				return new ContentOutputStream(resource, childPath,
						event.getOutputFile(), event.getOutputStream(), position, event.getTimestamp(), new SessionAwareUploadEventProcessor(getCurrentSession(),
								getCurrentLocale(), FileResourceServiceImpl.this, FileResourceServiceImpl.this),
						protocol);
			}

			@Override
			void onFileUnresolved(String mountName, String childPath, IOException t) {
				uploadCannotStart(mountName, childPath, t, protocol);
			}
		};
		return resolver.processRequest(path);

	}

	@Override
	public void uploadCannotStart(String mountName, String childPath, Throwable t, String protocol) {
		eventService
				.publishEvent(new DownloadStartedEvent(this, t, getCurrentSession(), mountName, childPath, protocol));

	}

	@Override
	public UploadStartedEvent uploadStarted(FileResource resource, String childPath, FileObject file, String protocol) {
		
		
		UploadStartedEvent evt = new UploadStartedEvent(this, getCurrentSession(), resource, childPath, file, protocol);
		eventService.publishEvent(evt);
		return evt;
	}

	@Override
	public void uploadComplete(FileResource resource, String childPath, FileObject file, long bytesIn, long timeMillis,
			String protocol) {
		eventService.publishEvent(
				new UploadCompleteEvent(this, getCurrentSession(), resource, childPath, bytesIn, timeMillis, protocol));
	}

	@Override
	public void uploadFailed(FileResource resource, String childPath, FileObject file, long bytesIn, Throwable t,
			String protocol) {
		eventService.publishEvent(
				new UploadCompleteEvent(this, getCurrentSession(), t, resource.getName(), childPath, protocol));
	}

	@Override
	public void downloadURIFile(String host, String controllerPath, String uri, final DownloadProcessor callback,
			final String protocol) throws IOException, AccessDeniedException {

		// TODO verify download permission on mount

		uri = normalise(uri);

		FileResolver<Object> resolver = new FileResolver<Object>() {
			@Override
			FileObject onFileResolved(FileResource resource, String childPath, FileObject file) throws IOException {

				callback.startDownload(resource, childPath, file, FileResourceServiceImpl.this);
				return null;
			}

			@Override
			void onFileUnresolved(String mountName, String childPath, IOException t) {
				eventService.publishEvent(
						new DownloadStartedEvent(this, t, getCurrentSession(), mountName, childPath, protocol));
			}
		};
		resolver.processURIRequest(host, controllerPath, uri);

	}

	@Override
	public FileUpload uploadURIFile(String host, String controllerPath, String uri, final InputStream in,
			final UploadProcessor<?> processor, final String protocol) throws IOException, AccessDeniedException {

		// TODO verify download permission on mount

		uri = normalise(uri);

		FileResolver<FileUpload> resolver = new FileResolver<FileUpload>() {
			@Override
			FileUpload onFileResolved(FileResource resource, String childPath, FileObject file) throws IOException {

				FileUpload upload;
				try {
					upload = uploadService.createFile(in, PathUtil.getBaseName(childPath), getCurrentRealm(), false,
							"upload", new FileObjectUploadStore(file, resource, childPath, protocol));

					if (processor != null) {
						processor.processUpload(resource, resolveMountFile(resource), childPath, file);
					}
					return upload;

				} catch (ResourceCreationException e) {
					throw new IOException(e);
				} catch (AccessDeniedException e) {
					throw new IOException(e);
				}

			}

			@Override
			void onFileUnresolved(String mountName, String childPath, IOException t) {
				eventService.publishEvent(
						new UploadStartedEvent(this, t, getCurrentSession(), mountName, childPath, protocol));
			}
		};

		return resolver.processURIRequest(host, controllerPath, uri);

	}

	@Override
	public boolean deleteURIFile(String host, String controllerPath, String uri, String protocol)
			throws IOException, AccessDeniedException {

		// TODO verify delete permission on mount

		uri = normalise(uri);

		return new DeleteFileResolver(protocol).processURIRequest(host, controllerPath, uri);

	}

	@Override
	public boolean deleteFile(String path, String protocol) throws IOException, AccessDeniedException {

		// TODO verify delete permission on mount

		path = normalise(path);

		return new DeleteFileResolver(protocol).processRequest(path);

	}

	class DeleteFileResolver extends FileResolver<Boolean> {

		String protocol;

		DeleteFileResolver(String protocol) {
			this.protocol = protocol;
		}

		@Override
		Boolean onFileResolved(FileResource resource, String childPath, FileObject file) throws IOException {

			try {
				if (file.exists()) {
					boolean deleted = file.delete();

					if (deleted) {
						eventService.publishEvent(
								new DeleteFileEvent(this, deleted, getCurrentSession(), resource, childPath, protocol));
						return true;
					}
				}

				eventService.publishEvent(
						new DeleteFileEvent(this, false, getCurrentSession(), resource, childPath, protocol));

			} catch (FileSystemException ex) {
				eventService.publishEvent(
						new DeleteFileEvent(this, ex, getCurrentSession(), resource.getName(), childPath, protocol));
			}
			return false;
		}

		@Override
		void onFileUnresolved(String mountName, String childPath, IOException t) {
			eventService
					.publishEvent(new DeleteFileEvent(this, t, getCurrentSession(), mountName, childPath, protocol));
		}
	};

	@Override
	public boolean renameURIFile(String host, String controllerPath, String fromUri, String toUri, String protocol)
			throws IOException, AccessDeniedException {

		// TODO verify rename permission on mount

		return new RenameFileResolver(protocol).processRequest(host, controllerPath, fromUri, toUri);

	}

	@Override
	public boolean renameFile(String fromPath, String toPath, String protocol)
			throws IOException, AccessDeniedException {

		// TODO verify rename permission on mount

		fromPath = normalise(fromPath);
		toPath = normalise(toPath);

		return new RenameFileResolver(protocol).processRequest(fromPath, toPath);
	}

	@Override
	public FileObject getFileObjectForMountFile(String file) throws AccessDeniedException, IOException {
		FileResource mount = getMountForPath(file);
		if (mount != null) {
			String childPath = resolveChildPath(mount, file);
			FileObject MountObj = resolveMountFile(mount);
			return MountObj.resolveFile(childPath);
		} else {
			throw new IOException(
					I18N.getResource(getCurrentLocale(), RESOURCE_BUNDLE, "error.mountDoesNotExist", file));
		}
	}

	class RenameFileResolver extends FilesResolver<Boolean> {

		String protocol;

		RenameFileResolver(String protocol) {
			this.protocol = protocol;
		}

		@Override
		Boolean onFilesResolved(FileResource fromResource, String fromChildPath, FileObject fromFile,
				FileResource toResource, String toChildPath, FileObject toFile) throws IOException {

			try {
				fromFile.moveTo(toFile);

				eventService.publishEvent(new RenameEvent(this, getCurrentSession(), fromResource, fromChildPath,
						toResource, toChildPath, protocol));

				return true;
			} catch (Exception e) {
				if (log.isErrorEnabled()) {
					log.error("Failed to move resource", e);
				}
				eventService.publishEvent(new RenameEvent(this, e, getCurrentSession(), fromResource.getName(),
						fromChildPath, toResource.getName(), toChildPath, protocol));

				return false;
			}
		}

		@Override
		void onFilesUnresolved(String fromMountName, String fromChildPath, String toMountName, String toChildPath,
				IOException t) {
			eventService.publishEvent(new RenameEvent(this, t, getCurrentSession(), fromMountName, fromChildPath,
					toMountName, toChildPath, protocol));
		}
	}

	@Override
	public boolean copyURIFile(String host, String controllerPath, String fromUri, String toUri, String protocol)
			throws IOException, AccessDeniedException {

		// TODO verify rename permission on mount
		fromUri = normalise(fromUri);
		toUri = normalise(toUri);

		return new CopyFileResolver(protocol).processRequest(host, controllerPath, fromUri, toUri);

	}

	@Override
	public boolean copyFile(String fromPath, String toPath, String protocol) throws IOException, AccessDeniedException {

		// TODO verify rename permission on mount
		fromPath = normalise(fromPath);
		toPath = normalise(toPath);

		return new CopyFileResolver(protocol).processRequest(fromPath, toPath);

	}

	class CopyFileResolver extends FilesResolver<Boolean> {

		String protocol;

		CopyFileResolver(String protocol) {
			this.protocol = protocol;
		}

		@Override
		Boolean onFilesResolved(FileResource fromResource, String fromChildPath, FileObject fromFile,
				FileResource toResource, String toChildPath, FileObject toFile) throws IOException {

			try {
				toFile.copyFrom(fromFile, new FileSelector() {

					@Override
					public boolean includeFile(FileSelectInfo fileInfo) throws Exception {
						return true;
					}

					@Override
					public boolean traverseDescendents(FileSelectInfo fileInfo) throws Exception {
						return true;
					}

				});

				eventService.publishEvent(new CopyFileEvent(this, getCurrentSession(), fromResource, fromChildPath,
						toResource, toChildPath, protocol));

				return true;
			} catch (Exception e) {
				if (log.isErrorEnabled()) {
					log.error("Failed to move resource", e);
				}
				eventService.publishEvent(new CopyFileEvent(this, e, getCurrentSession(), fromResource.getName(),
						fromChildPath, toResource.getName(), toChildPath, protocol));

				return false;
			}
		}

		@Override
		void onFilesUnresolved(String fromMountName, String fromChildPath, String toMountName, String toChildPath,
				IOException t) {
			eventService.publishEvent(new CopyFileEvent(this, t, getCurrentSession(), fromMountName, fromChildPath,
					toMountName, toChildPath, protocol));
		}
	};

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
	public FileObject createURIFolder(String host, String controllerPath, String parentUri, String protocol)
			throws IOException, AccessDeniedException {

		return createURIFolder(host, controllerPath, parentUri, null, protocol);
	}

	@Override
	public FileObject createURIFolder(String host, String controllerPath, String parentUri, final String newName,
			String protocol) throws IOException, AccessDeniedException {

		parentUri = normalise(parentUri);

		FileResolver<FileObject> resolver = new CreateFolderFileResolver(newName, protocol);

		return resolver.processURIRequest(host, controllerPath, parentUri);

	}

	@Override
	public FileObject createFolder(String parentPath, final String newName, String protocol)
			throws IOException, AccessDeniedException {

		parentPath = normalise(parentPath);

		FileResolver<FileObject> resolver = new CreateFolderFileResolver(newName, protocol);

		return resolver.processRequest(parentPath);

	}

	class CreateFolderFileResolver extends FileResolver<FileObject> {

		String newName;
		String protocol;

		CreateFolderFileResolver(String newName, String protocol) {
			this.newName = newName;
			this.protocol = protocol;
		}

		@Override
		FileObject onFileResolved(FileResource resource, String childPath, FileObject file) throws IOException {

			FileObject newFile = file.resolveFile(newName != null ? newName : "untitled folder");

			if (newName == null) {
				int i = 2;
				while (newFile.exists()) {
					newFile = file.resolveFile("untitled folder " + i++);
				}
			}

			boolean exists = newFile.exists();

			if (!exists) {
				newFile.createFolder();
			}

			boolean created = newFile.exists();

			eventService.publishEvent(new CreateFolderEvent(this, !exists && created, getCurrentSession(), resource,
					childPath + FileUtils.checkStartsWithSlash(newFile.getName().getBaseName()), protocol));

			return newFile;
		}

		@Override
		void onFileUnresolved(String mountName, String childPath, IOException t) {
			eventService
					.publishEvent(new CreateFolderEvent(this, t, getCurrentSession(), mountName, childPath, protocol));
		}

	}

	protected FileResource assertMountAccess(String name) throws AccessDeniedException {

		if (log.isDebugEnabled()) {
			log.debug("Asserting mount access to " + name);
		}
		try {
			assertAnyPermission(SystemPermission.SYSTEM, SystemPermission.SYSTEM_ADMINISTRATION);
			return getResourceByName(name);
		} catch (Exception e) {
			for (FileResource r : getResources(getCurrentPrincipal())) {
				if (r.getName().equalsIgnoreCase(name)) {
					return r;
				}
			}
			throw new AccessDeniedException(getCurrentPrincipal() + " does not have access to " + name);
		}

	}

	abstract class FileResolver<T> {

		FileResolver() {
		}

		T processRequest(String path) throws IOException, AccessDeniedException {

			String mountName = resolveMountName(path);
			String childPath = resolveChildPath(mountName, path);
			return processRequest(mountName, childPath);
		}

		T processURIRequest(String host, String controllerPath, String path) throws IOException, AccessDeniedException {

			String mountName = resolveURIMountName(controllerPath, path);
			String childPath = resolveURIChildPath(mountName, controllerPath, path);
			return processRequest(mountName, childPath);
		}

		T processRequest(String mountName, String childPath) throws IOException, AccessDeniedException {

			try {

				FileResource mountResource = assertMountAccess(mountName);

				FileObject fromFile = resolveVFSFile(mountResource);

				fromFile = fromFile.resolveFile(childPath);

				return onFileResolved(mountResource, childPath, fromFile);

			} catch (IOException ex) {
				onFileUnresolved(mountName, childPath, ex);
				throw ex;
			}
		}

		abstract T onFileResolved(FileResource resource, String childPath, FileObject file) throws IOException;

		abstract void onFileUnresolved(String mountName, String childPath, IOException t);

	}

	abstract class FilesResolver<T> {

		FilesResolver() {

		}

		T processRequest(String host, String controllerPath, String fromPath, String toPath)
				throws IOException, AccessDeniedException {

			String toMountName = resolveURIMountName(controllerPath, toPath);
			String toChildPath = resolveURIChildPath(toMountName, controllerPath, toPath);
			String fromMountName = resolveURIMountName(controllerPath, fromPath);
			String fromChildPath = resolveURIChildPath(fromMountName, controllerPath, fromPath);

			return process(fromMountName, fromChildPath, toMountName, toChildPath);
		}

		T processRequest(String fromPath, String toPath) throws IOException, AccessDeniedException {

			String toMountName = resolveMountName(toPath);
			String toChildPath = resolveChildPath(toMountName, toPath);
			String fromMountName = resolveMountName(fromPath);
			String fromChildPath = resolveChildPath(fromMountName, fromPath);

			return process(fromMountName, fromChildPath, toMountName, toChildPath);
		}

		T process(String fromMountName, String fromChildPath, String toMountName, String toChildPath)
				throws IOException, AccessDeniedException {
			try {

				FileResource fromResource = assertMountAccess(fromMountName);

				FileObject fromFile = resolveVFSFile(fromResource);

				fromFile = fromFile.resolveFile(fromChildPath);

				FileResource toResource = assertMountAccess(toMountName);

				FileObject toFile = resolveVFSFile(toResource);

				toFile = toFile.resolveFile(toChildPath);

				return onFilesResolved(fromResource, fromChildPath, fromFile, toResource, toChildPath, toFile);

			} catch (IOException ex) {
				onFilesUnresolved(fromMountName, fromChildPath, toMountName, toChildPath, ex);
				throw ex;
			}
		}

		abstract T onFilesResolved(FileResource fromResource, String fromChildPath, FileObject fromFile,
				FileResource toResource, String toChildPath, FileObject toFile) throws IOException;

		abstract void onFilesUnresolved(String fromMountName, String fromChildPath, String toMountName,
				String toChildPath, IOException t);
	}
	
	protected FileObject resolveVFSFile(FileResource resource) throws FileSystemException {
		FileResourceScheme scheme = schemes.get(resource.getScheme());
		if(scheme.getFileService()!=null) {
			FileSystemOptions opts = scheme.getFileService().buildFileSystemOptions(resource);
			return VFS.getManager().resolveFile(
					resource.getPrivateUrl(getCurrentPrincipal(), userVariableReplacement), opts);
		} else {
			return VFS.getManager().resolveFile(
					resource.getPrivateUrl(getCurrentPrincipal(), userVariableReplacement));
		}
	}
	
	protected FileSystemOptions buildFilesystemOptions(FileResource resource) {
		FileResourceScheme scheme = schemes.get(resource.getScheme());
		return scheme.getFileService().buildFileSystemOptions(resource);
		
	}

	@Override
	public void downloadCannotStart(FileResource resource, String childPath, FileObject file, Throwable t,
			String protocol) {
		eventService.publishEvent(
				new DownloadStartedEvent(this, t, getCurrentSession(), resource.getName(), childPath, protocol));
	}

	@Override
	public DownloadStartedEvent downloadStarted(FileResource resource, String childPath, FileObject file, InputStream in, String protocol) {
		DownloadStartedEvent evt = new DownloadStartedEvent(this, getCurrentSession(), resource, childPath, in, protocol);
		eventService.publishEvent(evt);
		return evt;
	}

	@Override
	public void downloadComplete(FileResource resource, String childPath, FileObject file, long bytesOut,
			long timeMillis, String protocol, Session session) {
		eventService.publishEvent(
				new DownloadCompleteEvent(this, session, resource, childPath, bytesOut, timeMillis, protocol));
	}

	@Override
	public void downloadFailed(FileResource resource, String childPath, FileObject file, Throwable t, String protocol,
			Session session) {
		eventService.publishEvent(new DownloadCompleteEvent(this, t, session, resource.getName(), childPath, protocol));
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
	public boolean testVFSUri(String privateUrl) throws FileSystemException {

		FileObject file = VFS.getManager().resolveFile(privateUrl);
		return file.exists();

	}

	@Override
	public UserVariableReplacement getUserVariableReplacement() {
		return userVariableReplacement;
	}

	class FileObjectUploadStore implements FileStore {

		FileObject file;
		FileResource resource;
		String childPath;
		String protocol;

		FileObjectUploadStore(FileObject file, FileResource resource, String childPath, String protocol) {
			this.file = file;
			this.resource = resource;
			this.childPath = childPath;
			this.protocol = protocol;
		}

		@Override
		public long writeFile(Realm realm, String uuid, InputStream in) throws IOException {

			long bytesIn = 0;

			UploadStartedEvent event = uploadStarted(resource, childPath, file, protocol);

			OutputStream out = event.getOutputStream();
			try {

				bytesIn = IOUtils.copyLarge(in, out);
				event.getOutputFile().refresh();

				uploadComplete(resource, event.getTransformationPath(), event.getOutputFile(), bytesIn, System.currentTimeMillis() - event.getTimestamp(),
						protocol);

			} catch (Exception e) {

				eventService.publishEvent(
						new UploadCompleteEvent(this, getCurrentSession(), e, resource.getName(), childPath, protocol));
			} finally {
				FileUtils.closeQuietly(in);
				FileUtils.closeQuietly(out);
			}

			return bytesIn;
		}
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
		
		FileResourceScheme fileScheme = schemes.get(scheme);
		if(fileScheme.getFileService()!=null) {
			return fileScheme.getFileService().getRepository().getPropertyCategories(null);
		}
		return new ArrayList<PropertyCategory>();
	}

	@Override
	public Collection<PropertyCategory> getResourceProperties(FileResource resource) throws AccessDeniedException {
		
		assertAnyPermission(FileResourcePermission.READ);
		
		FileResourceScheme fileScheme = schemes.get(resource.getScheme());
		if(fileScheme.getFileService()!=null) {
			return fileScheme.getFileService().getRepository().getPropertyCategories(resource);
		}
		return new ArrayList<PropertyCategory>();
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
}
