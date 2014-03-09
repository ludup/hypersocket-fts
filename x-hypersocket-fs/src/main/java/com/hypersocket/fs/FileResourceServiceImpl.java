package com.hypersocket.fs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.apache.log4j.lf5.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hypersocket.events.EventService;
import com.hypersocket.fs.events.CopyFileEvent;
import com.hypersocket.fs.events.CreateFolderEvent;
import com.hypersocket.fs.events.DeleteFileEvent;
import com.hypersocket.fs.events.DownloadCompleteEvent;
import com.hypersocket.fs.events.DownloadStartedEvent;
import com.hypersocket.fs.events.FileResourceEvent;
import com.hypersocket.fs.events.RenameEvent;
import com.hypersocket.fs.events.UploadCompleteEvent;
import com.hypersocket.fs.events.UploadStartedEvent;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.menus.MenuRegistration;
import com.hypersocket.menus.MenuService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.PermissionCategory;
import com.hypersocket.permissions.PermissionService;
import com.hypersocket.permissions.PermissionType;
import com.hypersocket.resource.AbstractAssignableResourceRepository;
import com.hypersocket.resource.AbstractAssignableResourceServiceImpl;
import com.hypersocket.server.HypersocketServer;
import com.hypersocket.session.Session;
import com.hypersocket.ui.jquery.JQueryUIContentHandler;
import com.hypersocket.util.FileUtils;

@Service
@Transactional
public class FileResourceServiceImpl extends
		AbstractAssignableResourceServiceImpl<FileResource> implements
		FileResourceService, DownloadEventProcessor {

	static Logger log = LoggerFactory.getLogger(FileResourceServiceImpl.class);

	@Autowired
	HypersocketServer server;

	@Autowired
	I18NService i18nService;

	@Autowired
	PermissionService permissionService;

	@Autowired
	MenuService menuService;

	@Autowired
	JQueryUIContentHandler jQueryUIContentHandler;

	@Autowired
	FileResourceRepository resourceRepository;

	@Autowired
	EventService eventService;

	Map<String, FileResourceScheme> schemes = new HashMap<String, FileResourceScheme>();

	public FileResourceServiceImpl() {

	}

	@PostConstruct
	public void postConstruct() {

		if (log.isDebugEnabled()) {
			log.debug("Constructing FileResourceService");
		}

		i18nService.registerBundle(RESOURCE_BUNDLE);

		PermissionCategory cat = permissionService.registerPermissionCategory(
				RESOURCE_BUNDLE, "category.fileResources");

		for (FileResourcePermission p : FileResourcePermission.values()) {
			permissionService.registerPermission(p.getResourceKey(), cat);
		}

		menuService.registerMenu(new MenuRegistration(RESOURCE_BUNDLE,
				"filesystems", "filesystems", 200, FileResourcePermission.READ,
				FileResourcePermission.CREATE, FileResourcePermission.UPDATE,
				FileResourcePermission.DELETE), MenuService.MENU_RESOURCES);

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

		eventService.registerEvent(DownloadStartedEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(DownloadCompleteEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(UploadStartedEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(UploadCompleteEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(CopyFileEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(CreateFolderEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(DeleteFileEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(RenameEvent.class, RESOURCE_BUNDLE);

		registerScheme(new FileResourceScheme("file", false, false));
		registerScheme(new FileResourceScheme("ftp", true, true));
		registerScheme(new FileResourceScheme("ftps", true, true));
		registerScheme(new FileResourceScheme("http", true, true));
		registerScheme(new FileResourceScheme("https", true, true));
		registerScheme(new FileResourceScheme("webdav", true, true));
		registerScheme(new FileResourceScheme("tmp", false, false));
		registerScheme(new FileResourceScheme("smb", true, true));

		jQueryUIContentHandler
				.addAlias("^/viewfs/.*$", "content/fileview.html");

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
			throw new IllegalArgumentException(scheme.getScheme()
					+ " is already a registerd scheme");
		} else if (!isVFSScheme(scheme.getScheme())) {
			throw new IllegalArgumentException(scheme.getScheme()
					+ " is not a valid VFS scheme");
		}

		if (log.isInfoEnabled()) {
			log.info("Registering file resource scheme " + scheme.getScheme()
					+ " isRemote=" + scheme.isRemote()
					+ " supportsCredentials=" + scheme.isSupportsCredentials());
		}
		schemes.put(scheme.getScheme(), scheme);
	}

	public FileResource getMountForPath(String host, String controllerPath,
			String path) {

		if (isWebDAVRoot(path)) {
			throw new IllegalArgumentException(
					path
							+ " is the root path! use isWebDAVRoot to check before calling this method");
		} else {
			try {
				String rootPath = FileUtils.checkEndsWithSlash(server
						.resolvePath(controllerPath));
				String mountPath = FileUtils.stripParentPath(rootPath, path);
				String mountName = FileUtils.firstPathElement(mountPath);

				for (FileResource r : getResources()) {
					if (r.getName().equals(mountName)) {
						return r;
					}
				}
			} catch (IOException e) {
				log.error("Failed to resolve mount for path " + path);
			}
			return null;
		}
	}

	@Override
	public List<FileResourceScheme> getSchemes() {
		return new ArrayList<FileResourceScheme>(schemes.values());
	}

	public boolean isWebDAVRoot(String path) {
		return FileUtils.checkEndsWithSlash(server.resolvePath("fs")).equals(
				FileUtils.checkEndsWithSlash(path));
	}

	public boolean isMountResource(FileResource resource,
			String controllerPath, String path) {
		String mountPath = FileUtils.checkEndsWithSlash(server
				.resolvePath(controllerPath + "/" + resource.getName()));
		path = FileUtils.checkEndsWithSlash(path);
		if (!path.startsWith(mountPath)) {
			throw new IllegalArgumentException(path
					+ " is not a child of mount " + resource.getName());
		}
		return mountPath.equals(path);
	}

	public String resolveChildPath(FileResource resource,
			String controllerPath, String path) throws IOException {
		return FileUtils.stripParentPath(
				FileUtils.checkEndsWithSlash(server.resolvePath(controllerPath
						+ "/" + resource.getName())),
				FileUtils.checkEndsWithSlash(path));
	}

	public String resolveChildPath(String mountName, String controllerPath,
			String path) throws IOException {
		return FileUtils
				.stripParentPath(FileUtils.checkEndsWithSlash(server
						.resolvePath(controllerPath + "/" + mountName)),
						FileUtils.checkEndsWithSlash(path));
	}

	public String resolveMountName(String controllerPath, String path)
			throws IOException {
		String rootPath = FileUtils.checkEndsWithSlash(server
				.resolvePath(controllerPath));
		String mountPath = FileUtils.stripParentPath(rootPath, path);
		String mountName = FileUtils.firstPathElement(mountPath);
		return mountName;
	}

	@Override
	public FileObject resolveMountFile(FileResource resource)
			throws IOException {

		// TODO verify permissions

		return VFS.getManager().resolveFile(resource.getPrivateUrl());
	}

	@Override
	public void downloadFile(String host, String controllerPath, String uri,
			final DownloadProcessor callback) throws IOException,
			AccessDeniedException {

		// TODO verify download permission on mount

		FileResolver<Object> resolver = new FileResolver<Object>() {
			@Override
			FileObject onFileResolved(FileResource resource, String childPath,
					FileObject file) throws IOException {

				callback.startDownload(resource, childPath, file,
						FileResourceServiceImpl.this);
				return null;
			}

			@Override
			void onFileUnresolved(String mountName, String childPath,
					IOException t) {
				eventService.publishEvent(new DownloadStartedEvent(this, t,
						getCurrentSession(), mountName, childPath));
			}
		};
		resolver.processRequest(host, controllerPath, uri);

	}

	@Override
	public void uploadFile(String host, String controllerPath, String uri,
			final InputStream in, final UploadProcessor<?> processor)
			throws IOException, AccessDeniedException {

		// TODO verify download permission on mount

		FileResolver<Object> resolver = new FileResolver<Object>() {
			@Override
			FileObject onFileResolved(FileResource resource, String childPath,
					FileObject file) throws IOException {

				file.createFile();

				UploadStartedEvent evt;
				long bytesIn = 0;

				eventService.publishEvent(evt = new UploadStartedEvent(this,
						getCurrentSession(), resource, childPath));

				try {
					
					StreamUtils.copy(in, file.getContent().getOutputStream());
					eventService.publishEvent(new UploadCompleteEvent(this,
							getCurrentSession(), resource, childPath, bytesIn,
							System.currentTimeMillis() - evt.getTimestamp()));
					
				} catch (Exception e) {
					
					eventService.publishEvent(new UploadCompleteEvent(this,
							getCurrentSession(), e, resource.getName(), childPath));
				}
				
				processor.processUpload(resource, resolveMountFile(resource),
						childPath, file);
				return null;
			}

			@Override
			void onFileUnresolved(String mountName, String childPath,
					IOException t) {
				eventService.publishEvent(new UploadStartedEvent(this, t,
						getCurrentSession(), mountName, childPath));
			}
		};
		resolver.processRequest(host, controllerPath, uri);

	}

	@Override
	public boolean deleteFile(String host, String controllerPath, String uri)
			throws IOException, AccessDeniedException {

		// TODO verify delete permission on mount

		FileResolver<Boolean> resolver = new FileResolver<Boolean>() {
			@Override
			Boolean onFileResolved(FileResource resource, String childPath,
					FileObject file) throws IOException {

				try {
					if (file.exists()) {
						boolean deleted = file.delete();

						eventService.publishEvent(new DeleteFileEvent(this,
								deleted, getCurrentSession(), resource,
								childPath));

						return deleted;
					} else {
						eventService
								.publishEvent(new DeleteFileEvent(this, false,
										getCurrentSession(), resource,
										childPath));
					}
				} catch (FileSystemException ex) {
					eventService
							.publishEvent(new DeleteFileEvent(this, ex,
									getCurrentSession(), resource.getName(),
									childPath));
				}
				return false;
			}

			@Override
			void onFileUnresolved(String mountName, String childPath,
					IOException t) {
				eventService.publishEvent(new DeleteFileEvent(this, t,
						getCurrentSession(), mountName, childPath));
			}
		};
		return resolver.processRequest(host, controllerPath, uri);

	}

	@Override
	public boolean renameFile(String host, String controllerPath,
			String fromUri, String toUri) throws IOException,
			AccessDeniedException {

		// TODO verify rename permission on mount

		FilesResolver<Boolean> resolver = new FilesResolver<Boolean>() {
			@Override
			Boolean onFilesResolved(FileResource fromResource,
					String fromChildPath, FileObject fromFile,
					FileResource toResource, String toChildPath,
					FileObject toFile) throws IOException {

				try {
					fromFile.moveTo(toFile);

					eventService.publishEvent(new RenameEvent(this,
							getCurrentSession(), fromResource, fromChildPath,
							toResource, toChildPath));

					return true;
				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error("Failed to move resource", e);
					}
					eventService.publishEvent(new RenameEvent(this, e,
							getCurrentSession(), fromResource.getName(),
							fromChildPath, toResource.getName(), toChildPath));

					return false;
				}
			}

			@Override
			void onFilesUnresolved(String fromMountName, String fromChildPath,
					String toMountName, String toChildPath, IOException t) {
				eventService.publishEvent(new RenameEvent(this, t,
						getCurrentSession(), fromMountName, fromChildPath,
						toMountName, toChildPath));
			}
		};
		return resolver.processRequest(host, controllerPath, fromUri, toUri);

	}

	@Override
	public boolean copyFile(String host, String controllerPath, String fromUri,
			String toUri) throws IOException, AccessDeniedException {

		// TODO verify rename permission on mount

		FilesResolver<Boolean> resolver = new FilesResolver<Boolean>() {
			@Override
			Boolean onFilesResolved(FileResource fromResource,
					String fromChildPath, FileObject fromFile,
					FileResource toResource, String toChildPath,
					FileObject toFile) throws IOException {

				try {
					toFile.copyFrom(fromFile, new FileSelector() {

						@Override
						public boolean includeFile(FileSelectInfo fileInfo)
								throws Exception {
							return true;
						}

						@Override
						public boolean traverseDescendents(
								FileSelectInfo fileInfo) throws Exception {
							return true;
						}

					});

					eventService.publishEvent(new CopyFileEvent(this,
							getCurrentSession(), fromResource, fromChildPath,
							toResource, toChildPath));

					return true;
				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error("Failed to move resource", e);
					}
					eventService.publishEvent(new CopyFileEvent(this, e,
							getCurrentSession(), fromResource.getName(),
							fromChildPath, toResource.getName(), toChildPath));

					return false;
				}
			}

			@Override
			void onFilesUnresolved(String fromMountName, String fromChildPath,
					String toMountName, String toChildPath, IOException t) {
				eventService.publishEvent(new CopyFileEvent(this, t,
						getCurrentSession(), fromMountName, fromChildPath,
						toMountName, toChildPath));
			}
		};
		return resolver.processRequest(host, controllerPath, fromUri, toUri);

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
	public FileObject createFolder(String host, String controllerPath,
			String parentUri) throws IOException, AccessDeniedException {
		return createFolder(host, controllerPath, parentUri, null);
	}

	@Override
	public FileObject createFolder(String host, String controllerPath,
			String parentUri, final String newName) throws IOException,
			AccessDeniedException {

		assertAnyPermission(FileResourcePermission.CREATE,
				FileResourcePermission.CONTENT_READ_WRITE);

		FileResolver<FileObject> resolver = new FileResolver<FileObject>() {

			@Override
			FileObject onFileResolved(FileResource resource, String childPath,
					FileObject file) throws IOException {

				FileObject newFile = file.resolveFile(newName != null ? newName
						: "untitled folder");

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

				eventService.publishEvent(new CreateFolderEvent(this, !exists
						&& created, getCurrentSession(), resource, childPath));

				return newFile;
			}

			@Override
			void onFileUnresolved(String mountName, String childPath,
					IOException t) {
				eventService.publishEvent(new CreateFolderEvent(this, t,
						getCurrentSession(), mountName, childPath));
			}

		};

		return resolver.processRequest(host, controllerPath, parentUri);

	}

	protected FileResource assertMountAccess(String name)
			throws AccessDeniedException {
		for (FileResource r : getResources(getCurrentPrincipal())) {
			if (r.getName().equalsIgnoreCase(name)) {
				return r;
			}
		}
		throw new AccessDeniedException(getCurrentPrincipal()
				+ " does not have access to " + name);
	}

	protected String processTokens(String url) {
		Session session = getCurrentSession();
		url = url.replace("${username}", session.getPrincipal()
				.getPrincipalName());
		url = url.replace("${password}", "");
		return url;
	}

	abstract class FileResolver<T> {

		FileResolver() {
		}

		T processRequest(String host, String controllerPath, String path)
				throws IOException, AccessDeniedException {

			String mountName = resolveMountName(controllerPath, path);
			String childPath = resolveChildPath(mountName, controllerPath, path);
			try {

				FileResource mountResource = assertMountAccess(mountName);

				FileObject fromFile = VFS.getManager().resolveFile(
						processTokens(mountResource.getPrivateUrl()));

				fromFile = fromFile.resolveFile(childPath);

				return onFileResolved(mountResource, childPath, fromFile);

			} catch (IOException ex) {
				onFileUnresolved(mountName, childPath, ex);
				throw ex;
			}
		}

		abstract T onFileResolved(FileResource resource, String childPath,
				FileObject file) throws IOException;

		abstract void onFileUnresolved(String mountName, String childPath,
				IOException t);

	}

	abstract class FilesResolver<T> {

		FilesResolver() {

		}

		T processRequest(String host, String controllerPath, String fromPath,
				String toPath) throws IOException, AccessDeniedException {

			String toMountName = resolveMountName(controllerPath, toPath);
			String toChildPath = resolveChildPath(toMountName, controllerPath,
					toPath);
			String fromMountName = resolveMountName(controllerPath, fromPath);
			String fromChildPath = resolveChildPath(fromMountName,
					controllerPath, fromPath);

			try {

				FileResource fromResource = assertMountAccess(fromMountName);

				FileObject fromFile = VFS.getManager().resolveFile(
						fromResource.getPrivateUrl());

				fromFile = fromFile.resolveFile(fromChildPath);

				FileResource toResource = assertMountAccess(toMountName);

				FileObject toFile = VFS.getManager().resolveFile(
						toResource.getPrivateUrl());

				toFile = toFile.resolveFile(toChildPath);

				return onFilesResolved(fromResource, fromChildPath, fromFile,
						toResource, toChildPath, toFile);

			} catch (IOException ex) {
				onFilesUnresolved(fromMountName, fromChildPath, toMountName,
						toChildPath, ex);
				throw ex;
			}
		}

		abstract T onFilesResolved(FileResource fromResource,
				String fromChildPath, FileObject fromFile,
				FileResource toResource, String toChildPath, FileObject toFile)
				throws IOException;

		abstract void onFilesUnresolved(String fromMountName,
				String fromChildPath, String toMountName, String toChildPath,
				IOException t);
	}

	@Override
	public void downloadCannotStart(FileResource resource, String childPath,
			FileObject file, Throwable t) {
		eventService.publishEvent(new DownloadStartedEvent(this, t,
				getCurrentSession(), resource.getName(), childPath));
	}

	@Override
	public long downloadStarted(FileResource resource, String childPath,
			FileObject file) {
		FileResourceEvent evt = new DownloadStartedEvent(this,
				getCurrentSession(), resource, childPath);
		eventService.publishEvent(evt);
		return evt.getTimestamp();
	}

	@Override
	public void downloadComplete(FileResource resource, String childPath,
			FileObject file, long bytesOut, long timeMillis) {
		eventService
				.publishEvent(new DownloadCompleteEvent(this,
						getCurrentSession(), resource, childPath, bytesOut,
						timeMillis));
	}

	@Override
	public void downloadFailed(FileResource resource, String childPath,
			FileObject file, Throwable t) {
		eventService.publishEvent(new DownloadCompleteEvent(this, t,
				getCurrentSession(), resource.getName(), childPath));
	}
}
