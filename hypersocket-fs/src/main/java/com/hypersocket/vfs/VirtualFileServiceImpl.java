package com.hypersocket.vfs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.FileProvider;
import org.apache.derby.impl.io.vfmem.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hypersocket.auth.PasswordEnabledAuthenticatedServiceImpl;
import com.hypersocket.cache.CacheService;
import com.hypersocket.config.ConfigurationService;
import com.hypersocket.events.EventService;
import com.hypersocket.fs.ContentInputStream;
import com.hypersocket.fs.ContentOutputStream;
import com.hypersocket.fs.DownloadEventProcessor;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourcePermission;
import com.hypersocket.fs.FileResourceScheme;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.fs.FileResourceServiceImpl;
import com.hypersocket.fs.SessionAwareUploadEventProcessor;
import com.hypersocket.fs.UploadEventProcessor;
import com.hypersocket.fs.UploadProcessor;
import com.hypersocket.fs.events.CopyFileEvent;
import com.hypersocket.fs.events.CreateFileEvent;
import com.hypersocket.fs.events.CreateFolderEvent;
import com.hypersocket.fs.events.DeleteFileEvent;
import com.hypersocket.fs.events.DownloadCompleteEvent;
import com.hypersocket.fs.events.DownloadStartedEvent;
import com.hypersocket.fs.events.RenameEvent;
import com.hypersocket.fs.events.UploadCompleteEvent;
import com.hypersocket.fs.events.UploadStartedEvent;
import com.hypersocket.fs.events.VirtualFolderCreatedEvent;
import com.hypersocket.fs.events.VirtualFolderDeletedEvent;
import com.hypersocket.fs.events.VirtualFolderUpdatedEvent;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.properties.ResourceUtils;
import com.hypersocket.realm.Principal;
import com.hypersocket.realm.Realm;
import com.hypersocket.realm.UserVariableReplacementService;
import com.hypersocket.resource.Resource;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceCreationException;
import com.hypersocket.scheduler.ClusteredSchedulerService;
import com.hypersocket.session.Session;
import com.hypersocket.tables.ColumnSort;
import com.hypersocket.tables.Sort;
import com.hypersocket.upload.FileStore;
import com.hypersocket.upload.FileUpload;
import com.hypersocket.upload.FileUploadService;
import com.hypersocket.utils.FileUtils;
import com.hypersocket.vfs.json.FileSystemColumn;
import com.hypersocket.vfs.json.HttpDownloadProcessor;

@Service
public class VirtualFileServiceImpl extends PasswordEnabledAuthenticatedServiceImpl
		implements VirtualFileService, DownloadEventProcessor, UploadEventProcessor {

	static Logger log = LoggerFactory.getLogger(VirtualFileServiceImpl.class);

	@Autowired
	ClusteredSchedulerService schedulerService;

	@Autowired
	FileResourceService fileService;

	@Autowired
	VirtualFileRepository virtualRepository;

	@Autowired
	UserVariableReplacementService userVariableReplacement;
	
	@Autowired
	EventService eventService;

	@Autowired
	FileUploadService uploadService;

	@Autowired
	ConfigurationService configurationService;

	@Autowired
	CacheService cacheService;

	Map<String, FileSystemManager> managers = new HashMap<>();
	Map<String, FileProvider> providers = new HashMap<>();

	ThreadLocal<Principal> overridePrincipal = new ThreadLocal<Principal>();
	ThreadLocal<String> overrideUsername = new ThreadLocal<String>();
	ThreadLocal<String> overridePassword = new ThreadLocal<String>();

	enum CACHE {
		CHILDREN
	}

	@PostConstruct
	private void postConstruct() {
		
		eventService.registerEvent(VirtualFolderCreatedEvent.class, FileResourceServiceImpl.RESOURCE_BUNDLE);
		eventService.registerEvent(VirtualFolderDeletedEvent.class, FileResourceServiceImpl.RESOURCE_BUNDLE);
	}

	@Override
	public void setupCredentials(Principal principal, String username, String password) {
		overridePrincipal.set(principal);
		overrideUsername.set(username);
		overridePassword.set(password);
	}

	@Override
	public void clearCredentials() {
		overridePrincipal.remove();
		overrideUsername.remove();
		overridePassword.remove();
	}

	@Override
	public Collection<VirtualFile> getVirtualFolders() throws AccessDeniedException {

		assertPermission(FileResourcePermission.READ);

		return virtualRepository.getVirtualFolders(getCurrentRealm());
	}

	@Override
	public VirtualFile getRootFolder() throws IOException, AccessDeniedException {
		return virtualRepository.getRootFolder(getCurrentRealm());
	}

	@Override
	public Collection<FileResource> getNonRootMounts() throws AccessDeniedException {

		assertPermission(FileResourcePermission.READ);

		return fileService.getNonRootResources();
	}

	@Override
	public Collection<FileResource> getRootMounts() throws AccessDeniedException {

		assertPermission(FileResourcePermission.READ);

		return fileService.getResourcesByVirtualPath("/");
	}

	@Override
	public VirtualFile getFileById(Long id) throws AccessDeniedException {

		assertPermission(FileResourcePermission.READ);

		return virtualRepository.getVirtualFileById(id);
	}

	@Override
	public VirtualFile getFile(String virtualPath) throws IOException, AccessDeniedException {

		virtualPath = normalise(virtualPath);

		VirtualFile file = null;
		Set<FileResource> resources = new HashSet<FileResource>();
		
		if (virtualPath.equals("/")) {
			file = getRootFolder();
		} else {
			file = virtualRepository.getVirtualFile(virtualPath, getCurrentRealm(), getCurrentPrincipal());
			if(file!=null) {
				if(permissionService.hasAdministrativePermission(getCurrentPrincipal())) {
					resources.addAll(file.getFolderMounts());
				} else {
					resources.addAll(fileService.getPersonalResources(
							getCurrentPrincipal(), 
							file.getParent().getFolderMounts()));
				}
			}
		}

		if (file == null) {

			VirtualFile parentFile = getFile(FileUtils.stripLastPathElement(virtualPath));
			if (parentFile == null) {
				throw new FileNotFoundException(virtualPath);
			}
			
			VirtualFile containingVirtualFolder = parentFile;
			while(!containingVirtualFolder.isVirtualFolder()) {
				containingVirtualFolder = containingVirtualFolder.getParent();
			}
			
			if(permissionService.hasAdministrativePermission(getCurrentPrincipal())) {
				resources.addAll(containingVirtualFolder.getFolderMounts());
			} else {
				resources.addAll(fileService.getPersonalResources(
						getCurrentPrincipal(), 
						containingVirtualFolder.getFolderMounts()));
			}

			// Lookup file from resources
			for (FileResource resource : resources) {

				if (FileUtils.checkEndsWithSlash(virtualPath)
						.startsWith(FileUtils.checkEndsWithSlash(resource.getVirtualPath()))) {

					String childPath = FileUtils.stripParentPath(resource.getVirtualPath(), virtualPath);

					FileObject resourceFile = getFileObject(resource);
					FileObject fileObject = resourceFile;
					if (StringUtils.isNotBlank(childPath) && fileObject.getType()==FileType.FOLDER) {
						fileObject = resourceFile.resolveFile(childPath);
						if (!fileObject.exists()) {
							continue;
						}
					}

					switch (fileObject.getType()) {
					case FOLDER:
						return checkWriteState(virtualRepository.reconcileNewFolder(
								fileObject.getName().getBaseName(), parentFile,
								fileObject, resource, false, null), resources);
					case FILE:
						return checkWriteState(virtualRepository.reconcileFile(
								fileObject.getName().getBaseName(), fileObject, resource,
								parentFile, null), resources);
					default:
						// Not supported
					}
				}
			}
		}

		if (file == null) {
			throw new FileNotFoundException(virtualPath);
		}

		return checkWriteState(file, resources);
	}

	@Override
	public Collection<FileResource> getMountsForPath(String virtualPath) throws IOException, AccessDeniedException {
		VirtualFile file = getFile(virtualPath);
		return fileService.getResourcesByVirtualPath(file.getVirtualPath());
	}

	@Override
	public Collection<VirtualFile> getChildren(String virtualPath) throws IOException, AccessDeniedException {
		VirtualFile file = getFile(virtualPath);
		return getChildren(file);
	}

	@Override
	public Collection<VirtualFile> getChildren(VirtualFile parentFile) throws AccessDeniedException, IOException {

		Map<String,VirtualFile> results = new HashMap<String,VirtualFile>();
		
		Set<FileResource> resources = new HashSet<FileResource>();

		VirtualFile containingVirtualFolder = parentFile;
		while(!containingVirtualFolder.isVirtualFolder()) {
			containingVirtualFolder = containingVirtualFolder.getParent();
		}
		
		if(permissionService.hasAdministrativePermission(getCurrentPrincipal())) {
			resources.addAll(containingVirtualFolder.getFolderMounts());
		} else {
			resources.addAll(fileService.getPersonalResources(
					getCurrentPrincipal(), 
					containingVirtualFolder.getFolderMounts()));
		}
		
		if (parentFile.isVirtualFolder()) {
			buildFileMap(results, virtualRepository.getVirtualFilesByResource(parentFile, 
					getCurrentRealm(), null,
					getPrincipalResources()), resources);
		}

		if(!parentFile.isMounted() && parentFile.getFolderMounts().isEmpty()) {
			return results.values();
		}
		
		for (FileResource resource : resources) {

			if (!parentFile.getVirtualPath().startsWith(resource.getVirtualPath())) {
				continue;
			}

			if(log.isDebugEnabled()) {
				log.debug(String.format("%s %s", resource.getName(), resource.getPath()));
			}
			String childPath = FileUtils.stripParentPath(resource.getVirtualPath(), parentFile.getVirtualPath());

			FileObject resourceFile = getFileObject(resource);
			FileObject fileObject = resourceFile;
			if (StringUtils.isNotBlank(childPath)) {
				fileObject = resourceFile.resolveFile(childPath);
				if (!fileObject.exists()) {
					throw new FileNotFoundException(parentFile.getVirtualPath());
				}
			}

			if (!fileObject.exists()) {
				throw new FileNotFoundException(parentFile.getVirtualPath());
			}

			switch (fileObject.getType()) {
			case FOLDER:
				for (FileObject child : fileObject.getChildren()) {
					switch (child.getType()) {
					case FOLDER:
						if (isReconciledFolder(resource, child)) {
							buildFileMap(results, virtualRepository.reconcileNewFolder(child.getName().getBaseName(), parentFile,
									child, resource, false, null), resources);
						}
						break;
					case FILE:
						if (isReconciledFile(resource, child)) {
							buildFileMap(results, virtualRepository.reconcileFile(child.getName().getBaseName(), child, resource,
									parentFile, null), resources);
						}
						break;
					default:
						// Unsupported file type
					}
				}
				break;
			case FILE:
				buildFileMap(results, virtualRepository.reconcileFile(
						fileObject.getName().getBaseName(), 
						fileObject, 
						resource, 
						parentFile, 
						null), resources);
				break;
			default:
				// What do we do here?
			}
		}

		return results.values();
	}

	private void buildFileMap(Map<String,VirtualFile> map, Collection<VirtualFile> results, Set<FileResource> accessible) throws FileSystemException {
		for(VirtualFile file : results) {
			buildFileMap(map, file, accessible);
		}
	}
	
	private void buildFileMap(Map<String,VirtualFile> map, VirtualFile file, Set<FileResource> accessible) throws FileSystemException {
		file = checkWriteState(file, accessible);
		VirtualFile existing = map.get(file.getFilename());
		if(existing==null) {
			map.put(file.getFilename(), file);
		} else {
			if(!existing.isVirtualFolder()) {
				if(file.isVirtualFolder()) {
					map.put(file.getFilename(), file);
				}
			}
		}
	}
	
	private VirtualFile checkWriteState(VirtualFile file, Set<FileResource> accessible) throws FileSystemException {
		if(file.isVirtualFolder()) {
			
			if(file.getDefaultMount()!=null) {
				if(!accessible.contains(file.getDefaultMount())) {
					file.setWritable(false);
				} else {
					file.setWritable(!file.getDefaultMount().isReadOnly());
				}
			}
		} else {
			if(accessible.contains(file.getMount())) {
				file.setWritable(!file.getMount().isReadOnly() && file.getFileObject().isWriteable());
			} else {
				file.setWritable(false);
			}
		}
		return file;
	}

	@Override
	public Boolean deleteFile(String virtualPath, String proto) throws IOException, AccessDeniedException {

		virtualPath = normalise(virtualPath);
		VirtualFile file = getFile(virtualPath);

		return deleteFile(file, proto);
	}

	@Override
	public Boolean deleteFile(VirtualFile file, String proto) throws IOException, AccessDeniedException {

		if (!file.isMounted()) {

			Collection<FileResource> mounts = fileService.getResourcesByVirtualPath(file.getVirtualPath());
			if (!mounts.isEmpty()) {
				
				eventService.publishEvent(new VirtualFolderDeletedEvent(this, new AccessDeniedException(),
						getCurrentSession(), file.getVirtualPath()));
				throw new AccessDeniedException();
			}

			try {
				
				virtualRepository.deleteVirtualFolder(file);
				
				eventService.publishEvent(new VirtualFolderDeletedEvent(this, 
						getCurrentSession(), 
						file.getVirtualPath()));

				return true;

			} catch (Throwable t) {
				eventService.publishEvent(
						new VirtualFolderDeletedEvent(this, t, getCurrentSession(), file.getVirtualPath()));
				throw t;
			}
		} else {

			DeleteFileResolver resolver = new DeleteFileResolver(proto);

			boolean success = resolver.processRequest(file);
			return success;
		}
	}

	@Override
	public VirtualFile createFile(String virtualPath, String proto) throws IOException, AccessDeniedException {

		virtualPath = normalise(virtualPath);

		try {
			getFile(virtualPath);
			throw new FileExistsException(virtualPath);
		} catch (FileNotFoundException ex) {

			String parentPath = FileUtils.stripLastPathElement(virtualPath);
			String newName = FileUtils.stripParentPath(parentPath, virtualPath);

			CreateFileFileResolver resolver = new CreateFileFileResolver(newName, proto);

			VirtualFile parent = getFile(parentPath);

			FileObject newFolder = resolver.processRequest(virtualPath);
			return virtualRepository.reconcileFile(newFolder.getName().getBaseName(), newFolder,
					resolver.getParentResource(), parent, getOwnerPrincipal(resolver.getParentResource()));
		}
	}

	@Override
	public VirtualFile createFolder(String virtualPath, String proto) throws IOException, AccessDeniedException {
		return createFolder(virtualPath, proto, false);
	}

	@Override
	public VirtualFile createFolder(String virtualPath, String proto, boolean disableEvent)
			throws IOException, AccessDeniedException {

		virtualPath = FileUtils.checkEndsWithSlash(normalise(virtualPath));

		try {
			getFile(virtualPath);
			throw new FileExistsException(virtualPath);
		} catch (FileNotFoundException ex) {
			String parentPath = FileUtils.stripLastPathElement(virtualPath);
			String newName = FileUtils.stripParentPath(parentPath, virtualPath);

			CreateFolderFileResolver resolver = new CreateFolderFileResolver(newName, proto, !disableEvent);

			VirtualFile parent = getFile(parentPath);

			FileObject newFolder = resolver.processRequest(virtualPath);
			return virtualRepository.reconcileNewFolder(newFolder.getName().getBaseName(), parent, newFolder,
					resolver.getParentResource(), false, getOwnerPrincipal(resolver.getParentResource()));
		}
	}

	@Override
	public VirtualFile createVirtualFolder(String virtualPath) throws IOException, AccessDeniedException {

		virtualPath = FileUtils.checkEndsWithSlash(normalise(virtualPath));

		String parentPath = virtualPath;
		String newName = "untitled folder";

		VirtualFile parent = getFile(parentPath);
		int i = 0;
		while (true) {
			try {
				getFile(FileUtils.checkEndsWithSlash(virtualPath) + newName);
				newName = "untitled folder " + ++i;
			} catch (FileNotFoundException ex) {
				break;
			}
		}

		try {
			VirtualFile file = virtualRepository.createVirtualFolder(newName, parent);
			eventService.publishEvent(new VirtualFolderCreatedEvent(this, getCurrentSession(),
					FileUtils.checkEndsWithSlash(virtualPath) + newName));
			return file;
		} catch (Throwable e) {
			eventService.publishEvent(new VirtualFolderCreatedEvent(this, e, getCurrentSession(), virtualPath));
			throw e;
		}
	}

	@Override
	public VirtualFile createUntitledFolder(String virtualPath, String proto)
			throws IOException, AccessDeniedException {

		virtualPath = FileUtils.checkEndsWithSlash(normalise(virtualPath));

		String newName = "untitled folder";
		int i = 1;
		while (true) {
			try {
				getFile(FileUtils.checkEndsWithSlash(virtualPath) + newName);
				newName = "untitled folder " + i++;
			} catch (FileNotFoundException e) {
				break;
			}
		}

		virtualPath = FileUtils.checkEndsWithSlash(virtualPath) + newName;

		VirtualFile parentFile = getFile(FileUtils.stripLastPathElement(virtualPath));

		CreateFolderFileResolver resolver = new CreateFolderFileResolver(newName, proto, false);

		FileObject newFolder = resolver.processRequest(virtualPath);
		return virtualRepository.reconcileNewFolder(newFolder.getName().getBaseName(), parentFile, newFolder,
				parentFile.getMount(), false, getOwnerPrincipal(resolver.getParentResource()));
	}

	@Override
	public VirtualFile renameFile(String fromVirtualPath, String toVirtualPath, String proto)
			throws IOException, AccessDeniedException {

		fromVirtualPath = normalise(fromVirtualPath);
		toVirtualPath = normalise(toVirtualPath);
		VirtualFile fromFile = getFile(fromVirtualPath);

		if (fromVirtualPath.equals(toVirtualPath)) {
			return fromFile;
		}

		return renameFile(fromFile, toVirtualPath, proto);
	}

	@Override
	public VirtualFile renameFile(VirtualFile fromFile, String toVirtualPath, String proto)
			throws IOException, AccessDeniedException {
		try {
			getFile(toVirtualPath);
			if (!fromFile.isMounted()) {
				eventService.publishEvent(new VirtualFolderUpdatedEvent(this, new FileExistsException(toVirtualPath),
						getCurrentSession(), fromFile.getVirtualPath(), toVirtualPath));
			}
			throw new FileExistsException(toVirtualPath);
		} catch (FileNotFoundException ex) {

			if (!fromFile.isMounted()) {
				if (!getChildren(fromFile).isEmpty()) {
					eventService.publishEvent(new VirtualFolderUpdatedEvent(this, new AccessDeniedException(),
							getCurrentSession(), fromFile.getVirtualPath(), toVirtualPath));
					throw new AccessDeniedException();
				}
				/**
				 * Rename a virtual folder
				 */
				try {
					String originalPath = fromFile.getVirtualPath();
					VirtualFile file = virtualRepository.renameVirtualFolder(fromFile, toVirtualPath);
					eventService.publishEvent(
							new VirtualFolderUpdatedEvent(this, getCurrentSession(), originalPath, toVirtualPath));
					return file;
				} catch (Throwable t) {
					eventService.publishEvent(new VirtualFolderUpdatedEvent(this, t, getCurrentSession(),
							fromFile.getVirtualPath(), toVirtualPath));
					throw t;
				}
			}
			RenameFileResolver resolver = new RenameFileResolver(proto);
			if (!resolver.processRequest(fromFile, toVirtualPath)) {
				throw new IOException(
						String.format("Failed to rename file %s to %s", fromFile.getVirtualPath(), toVirtualPath));
			}

			VirtualFile parent = getFile(FileUtils.stripLastPathElement(toVirtualPath));
			VirtualFile existingFile = virtualRepository.getVirtualFile(toVirtualPath, getCurrentRealm(),
					getCurrentPrincipal());
			String displayName = FileUtils.lastPathElement(toVirtualPath);
			if (existingFile != null) {
				displayName = String.format("%s (%s)", displayName, resolver.getToMount().getName());
			}
			return virtualRepository.reconcileFile(displayName, resolver.getToFile(), resolver.getToMount(), parent,
					getOwnerPrincipal(resolver.getToMount()));
		}
	}

	@Override
	public boolean copyFile(String fromPath, String toPath, String proto) throws IOException, AccessDeniedException {

		fromPath = normalise(fromPath);
		toPath = normalise(toPath);

		getFile(FileUtils.stripLastPathElement(toPath));
		CopyFileResolver resolver = new CopyFileResolver(proto);
		boolean success = resolver.processRequest(fromPath, toPath);
		return success;

	}

	protected FileResource[] getPrincipalResources() throws AccessDeniedException {
		if (permissionService.hasSystemPermission(getCurrentPrincipal())) {
			return fileService.getResources(getCurrentRealm()).toArray(new FileResource[0]);
		} else {
			return fileService.getPersonalResources().toArray(new FileResource[0]);
		}
	}

	@Override
	public void downloadFile(String virtualPath, final HttpDownloadProcessor processor, final String proto)
			throws AccessDeniedException, IOException {

		virtualPath = normalise(virtualPath);
		VirtualFile file = getFile(virtualPath);
		downloadFile(file, processor, proto);

	}

	@Override
	public void downloadFile(VirtualFile file, final HttpDownloadProcessor processor, final String proto)
			throws AccessDeniedException, IOException {

		if(!file.isMounted()) {
			throw new IOException("Download must be a file and have a valid mount point!");
		}
		FileResolver<Object> resolver = new FileResolver<Object>(true, false) {

			void checkFile(FileObject file) throws IOException {
				if (file.getType() != FileType.FILE) {
					throw new FileNotFoundException("Download must be a file!");
				}
			}

			@Override
			Object onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath)
					throws IOException {

				processor.startDownload(resource, childPath, file, VirtualFileServiceImpl.this);
				return null;
			}

			@Override
			void onFileUnresolved(String path, Exception t) {
				eventService.publishEvent(new DownloadStartedEvent(this, t, getCurrentSession(), path, proto));
			}
		};

		resolver.processRequest(file);
	}

	@Override
	public FileUpload uploadFile(String virtualPath, final InputStream in, final UploadProcessor<?> processor,
			final String proto) throws AccessDeniedException, IOException {

		virtualPath = normalise(virtualPath);
		String parentPath = FileUtils.checkEndsWithSlash(FileUtils.stripLastPathElement(virtualPath));
		final VirtualFile parentFile = getFile(parentPath);

		FileResolver<FileUpload> resolver = new FileResolver<FileUpload>(false, true) {

			@Override
			FileUpload onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath)
					throws IOException {

				FileUpload upload;
				try {

					FileObjectUploadStore store = new FileObjectUploadStore(file, resource, childPath, proto);
					upload = uploadService.createFile(in, PathUtil.getBaseName(childPath), getCurrentRealm(), false,
							"upload", store);

					if (processor != null) {
						processor.processUpload(resource, resolveVFSFile(parentFile.getMount()), childPath, file);
					}
					return upload;

				} catch (ResourceCreationException e) {
					throw new IOException(e.getMessage(), e);
				} catch (AccessDeniedException e) {
					throw new IOException(e.getMessage(), e);
				}
			}

			@Override
			void onFileUnresolved(String path, Exception t) {
				eventService.publishEvent(new UploadStartedEvent(this, t, getCurrentSession(), path, proto));
			}
		};

		return resolver.processRequest(virtualPath);

	}

	@Override
	public Collection<VirtualFile> listChildren(String virtualPath, String proto)
			throws IOException, AccessDeniedException {
		return getChildren(virtualPath);
	}

	@Override
	public long getSearchCount(String virtualPath, String searchColumn, String search, String flags) throws AccessDeniedException {
		VirtualFile file = virtualRepository.getVirtualFileByResource(virtualPath, getCurrentRealm(),
				getCurrentPrincipal(), getPrincipalResources());
		return virtualRepository.getCount(VirtualFile.class, searchColumn, search, new ParentCriteria(file),
				new ConflictCriteria());
	}

	@Override
	public Collection<VirtualFile> searchFiles(String virtualPath, String searchColumn, String search, int offset,
			int limit, ColumnSort[] sort, String proto, String flags) throws AccessDeniedException, IOException {

		VirtualFile parentFile = virtualRepository.getVirtualFile(virtualPath, getCurrentRealm(), null);
		if (parentFile == null) {
			parentFile = getFile(virtualPath);
		}

		List<VirtualFile> results = new ArrayList<VirtualFile>(getChildren(parentFile));
		search = search.toLowerCase();
		for (Iterator<VirtualFile> it = results.iterator(); it.hasNext();) {
			VirtualFile file = it.next();
			if (StringUtils.isNotBlank(search)) {
				if (!file.getFilename().toLowerCase().startsWith(search) 
						&& !file.getFilename().toLowerCase().endsWith(search)) {
					it.remove();
					continue;
				}
			}
			if(StringUtils.isNotBlank(flags)) {
				if(file.getMount()==null || !file.getMount().getFlags().contains(flags)) {
					it.remove();
					continue;
				}
			}
		}
		

		if (sort.length > 0) {
			final FileSystemColumn column = (FileSystemColumn) sort[0].getColumn();
			final Sort sortDirection = sort[0].getSort();
			Collections.sort(results, new Comparator<VirtualFile>() {

				@Override
				public int compare(VirtualFile o1, VirtualFile o2) {

					switch (column) {
					case LASTMODIFIED:
						if (sortDirection == Sort.ASC) {
							return o1.getLastModified().compareTo(o2.getLastModified());
						} else {
							return o2.getLastModified().compareTo(o1.getLastModified());
						}
					case SIZE:
						if (sortDirection == Sort.ASC) {
							return o1.getSize().compareTo(o2.getSize());
						} else {
							return o2.getSize().compareTo(o1.getSize());
						}
					case TYPE:
						if (sortDirection == Sort.ASC) {
							return o1.getType().compareTo(o2.getType());
						} else {
							return o2.getType().compareTo(o1.getType());
						}
					default:
						if (sortDirection == Sort.ASC) {
							return o1.getFilename().compareTo(o2.getFilename());
						} else {
							return o2.getFilename().compareTo(o1.getFilename());
						}
					}
				}
			});
		}
		return results;
	}

	private boolean checkResourceFilter(String filename, FileResource resource) {

		String[] includeFilters = ResourceUtils
				.explodeValues(fileService.getResourceProperty(resource, "fs.includeFilter"));
		String[] excludeFilters = ResourceUtils
				.explodeValues(fileService.getResourceProperty(resource, "fs.excludeFilter"));

		boolean included = includeFilters.length == 0;

		if (!included) {
			for (String include : includeFilters) {
				if (filename.matches(include)) {
					included = true;
					break;
				}
				if (filename.endsWith(include)) {
					included = true;
					break;
				}
			}
		}

		if (included && excludeFilters.length > 0) {
			for (String exclude : excludeFilters) {
				if (filename.matches(exclude)) {
					included = false;
					break;
				}
				if (filename.endsWith(exclude)) {
					included = false;
					break;
				}
			}
		}
		return included;
	}

	private boolean isReconciledFile(FileResource resource, FileObject obj) {
		try {
			if (obj.isHidden() || obj.getName().getBaseName().startsWith(".")) {
				return resource.isShowHidden();
			}
			return checkResourceFilter(obj.getName().getBaseName(), resource);
		} catch (FileSystemException e) {
			return true;
		}
	}

	private boolean isReconciledFolder(FileResource resource, FileObject obj) {
		if (!resource.isShowFolders()) {
			return false;
		}
		// Check for hidden folder or filter
		return isReconciledFile(resource, obj);
	}

	abstract class FileResolver<T> {

		FileObject file;
		boolean fileExits;
		boolean isUpdate;
		FileResource parentResource;
		VirtualFile virtualFile;
		String overrideUsername;
		String overridePassword;
		Principal overridePrincipal;

		FileResolver(boolean fileExists, boolean isUpdate) {
			this.fileExits = fileExists;
			this.isUpdate = isUpdate;
		}

		FileResolver(boolean fileExists, boolean isUpdate, Principal overridePrincipal, String overrideUsername,
				String overridePassword) {
			this.fileExits = fileExists;
			this.isUpdate = isUpdate;
			this.overridePrincipal = overridePrincipal;
			this.overrideUsername = overrideUsername;
			this.overridePassword = overridePassword;
		}

		T processRequest(String path) throws IOException, AccessDeniedException {

			path = normalise(path);

			if (fileExits) {
				virtualFile = VirtualFileServiceImpl.this.getFile(path);
				String childPath = FileUtils.stripParentPath(virtualFile.getMount().getVirtualPath(), path);
				return processRequest(virtualFile.getMount(), childPath, path);
			} else {
				String parentPath = FileUtils.stripLastPathElement(path);
				VirtualFile parentFile = VirtualFileServiceImpl.this.getFile(parentPath);
				if (parentFile.isMounted()) {
					String childPath = FileUtils.stripParentPath(parentFile.getMount().getVirtualPath(), path);
					parentResource = parentFile.getMount();
					if (isUpdate && parentResource.isReadOnly()) {
						throw new AccessDeniedException();
					}
					return processRequest(parentResource, childPath, path);
				} else {
					parentResource = parentFile.getDefaultMount();

					if (parentResource == null) {
						throw new AccessDeniedException();
					}
					if (isUpdate && parentResource.isReadOnly()) {
						throw new AccessDeniedException();
					}
					String childPath = FileUtils.stripParentPath(parentResource.getVirtualPath(), path);
					return processRequest(parentResource, childPath, path);
				}
			}
		}

		T processRequest(VirtualFile file) throws IOException, AccessDeniedException {

			String childPath = FileUtils.stripParentPath(file.getMount().getVirtualPath(), file.getVirtualPath());
			return processRequest(file.getMount(), childPath, file.getVirtualPath());

		}

		T processRequest(FileResource resource, String childPath, String path)
				throws IOException, AccessDeniedException {

			try {
				if (overridePrincipal == null) {
					overridePrincipal = getCurrentPrincipal();
				}
				if (overrideUsername == null) {
					overrideUsername = resource.getUsername();
				}
				if (overridePassword == null) {
					overridePassword = resource.getPassword();
				}
				file = resolveVFSFile(resource);
				if(StringUtils.isNotBlank(childPath) && file.getType()==FileType.FOLDER) {
					file = file.resolveFile(childPath);
				}

				checkFile(file);

				return onFileResolved(resource, childPath, file, path);

			} catch (IOException ex) {
				onFileUnresolved(path, ex);
				throw ex;
			}
		}

		void checkFile(FileObject file) throws IOException {

		}

		FileObject getFile() {
			return file;
		}

		FileResource getParentResource() {
			return parentResource;
		}

		abstract T onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath)
				throws IOException;

		abstract void onFileUnresolved(String path, Exception t);

	}

	abstract class ParentResolver<T> {

		FileObject file;
		FileResource parentResource = null;

		ParentResolver() {
		}

		T processRequest(String path) throws IOException, AccessDeniedException {

			String parentPath = FileUtils.stripLastPathElement(path);
			VirtualFile parentFile = VirtualFileServiceImpl.this.getFile(parentPath);
			if (parentFile.isMounted()) {
				parentResource = parentFile.getMount();
				if (parentResource.isReadOnly()) {
					throw new AccessDeniedException();
				}
				if (!checkParent(parentResource)) {
					throw new AccessDeniedException();
				}
				String childPath = FileUtils.stripParentPath(parentFile.getMount().getVirtualPath(), path);
				return processRequest(parentResource, childPath, path);
			} else {
				parentResource = parentFile.getDefaultMount();
				if (parentResource == null) {
					throw new AccessDeniedException();
				}
				if (parentResource.isReadOnly()) {
					throw new AccessDeniedException();
				}
				if (!checkParent(parentResource)) {
					throw new AccessDeniedException();
				}
				String childPath = FileUtils.stripParentPath(parentResource.getVirtualPath(), path);
				return processRequest(parentResource, childPath, path);
			}
		}

		boolean checkParent(FileResource parentResource) {
			return true;
		}

		T processRequest(FileResource resource, String childPath, String path)
				throws IOException, AccessDeniedException {

			try {
				file = resolveVFSFile(resource);
				if (StringUtils.isNotBlank(childPath)) {
					file = file.resolveFile(childPath);
				}

				return onFileResolved(resource, childPath, file, path);

			} catch (IOException ex) {
				onFileUnresolved(path, ex);
				throw ex;
			}
		}

		FileObject getFile() {
			return file;
		}

		FileResource getParentResource() {
			return parentResource;
		}

		abstract T onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath)
				throws IOException;

		abstract void onFileUnresolved(String path, Exception t);

	}

	class CreateFolderFileResolver extends ParentResolver<FileObject> {

		String newName;
		String protocol;
		boolean wantsEvent = true;

		CreateFolderFileResolver(String newName, String protocol, boolean wantsEvent) {
			this.newName = newName;
			this.protocol = protocol;
		}

		@Override
		boolean checkParent(FileResource parentResource) {
			return parentResource.isShowFolders();
		}

		@Override
		FileObject onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath)
				throws IOException {

			FileObject newFile = null;
			if (newName == null) {
				newFile = file.resolveFile(newName != null ? newName : "untitled file");

				if (newName == null) {
					int i = 2;
					while (newFile.exists()) {
						newFile = file.resolveFile("untitled file " + i++);
					}
				}

				childPath = FileUtils.checkEndsWithSlash(childPath) + newName;
			} else {
				newFile = file;
			}

			boolean exists = newFile.exists();

			if (!exists) {
				newFile.createFolder();
			}

			boolean created = newFile.exists();

			if (wantsEvent) {
				eventService.publishEvent(new CreateFolderEvent(this, !exists && created, getCurrentSession(), resource,
						childPath, protocol));
			}

			return newFile;
		}

		@Override
		void onFileUnresolved(String path, Exception t) {
			if (wantsEvent) {
				eventService.publishEvent(new CreateFolderEvent(this, t, getCurrentSession(), path, protocol));
			}
		}

	}

	class CreateFileFileResolver extends FileResolver<FileObject> {

		String newName;
		String protocol;

		CreateFileFileResolver(String newName, String protocol) {
			super(false, true);
			this.newName = newName;
			this.protocol = protocol;
		}

		@Override
		FileObject onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath)
				throws IOException {

			FileObject newFile = null;
			if (newName == null) {
				newFile = file.resolveFile(newName != null ? newName : "untitled file");

				if (newName == null) {
					int i = 2;
					while (newFile.exists()) {
						newFile = file.resolveFile("untitled file " + i++);
					}
				}
			} else {
				newFile = file;
			}

			boolean exists = newFile.exists();

			if (!exists) {
				newFile.createFile();
			}

			boolean created = newFile.exists();

			eventService.publishEvent(new CreateFileEvent(this, !exists && created, getCurrentSession(), resource,
					childPath + FileUtils.checkStartsWithSlash(newFile.getName().getBaseName()), protocol));

			return newFile;
		}

		@Override
		void onFileUnresolved(String path, Exception t) {
			eventService.publishEvent(new CreateFileEvent(this, t, getCurrentSession(), path, protocol));
		}

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

				if (!toFile.getParent().exists()) {
					toFile.getParent().createFolder();
				}

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
				eventService.publishEvent(
						new CopyFileEvent(this, e, getCurrentSession(), fromResource.getVirtualPath() + fromChildPath,
								toResource.getVirtualPath() + toChildPath, protocol));

				return false;
			}
		}

		@Override
		void onFilesUnresolved(String fromPath, String toPath, IOException t) {
			eventService.publishEvent(new CopyFileEvent(this, t, getCurrentSession(), fromPath, toPath, protocol));
		}
	};

	class RenameFileResolver extends FilesResolver<Boolean> {

		String protocol;

		RenameFileResolver(String protocol) {
			this.protocol = protocol;
		}

		@Override
		Boolean onFilesResolved(FileResource fromResource, String fromChildPath, FileObject fromFile,
				FileResource toResource, String toChildPath, FileObject toFile) throws IOException {

			try {

				if (!toFile.getParent().exists()) {
					toFile.getParent().createFolder();
				}

				fromFile.moveTo(toFile);

				eventService.publishEvent(new RenameEvent(this, getCurrentSession(), fromResource, fromChildPath,
						toResource, toChildPath, protocol));

				return true;
			} catch (Exception e) {
				if (log.isErrorEnabled()) {
					log.error("Failed to move resource", e);
				}
				eventService.publishEvent(
						new RenameEvent(this, e, getCurrentSession(), fromChildPath, toChildPath, protocol));

				return false;
			}
		}

		@Override
		void onFilesUnresolved(String fromPath, String toPath, IOException t) {
			eventService.publishEvent(new RenameEvent(this, t, getCurrentSession(), fromPath, toPath, protocol));

		}
	}

	class DeleteFileResolver extends FileResolver<Boolean> {

		String protocol;
		boolean existed;

		DeleteFileResolver(String protocol) {
			super(true, true);
			this.protocol = protocol;
		}

		@Override
		Boolean onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath)
				throws IOException {
			try {

				if (existed = file.exists()) {
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
				eventService.publishEvent(new DeleteFileEvent(this, ex, getCurrentSession(), childPath, protocol));
			}
			return false;
		}

		@Override
		void onFileUnresolved(String path, Exception t) {
			eventService.publishEvent(new DeleteFileEvent(this, t, getCurrentSession(), path, protocol));
		}

		boolean isExisted() {
			return existed;
		}
	};

	abstract class FilesResolver<T> {

		FileObject fromFile;
		FileObject toFile;
		FileResource fromMount;
		FileResource toMount;

		FilesResolver() {
		}

		T processRequest(String fromPath, String toPath) throws IOException, AccessDeniedException {

			VirtualFile fromFile = getFile(fromPath);
			return processRequest(fromFile, toPath);
		}

		T processRequest(VirtualFile fromFile, String toPath) throws IOException, AccessDeniedException {

			fromMount = fromFile.getMount();

			try {
				VirtualFile toFile = getFile(toPath);
				toMount = toFile.getMount() != null ? toFile.getMount() : toFile.getDefaultMount();
			} catch (FileNotFoundException ex) {
				String toParent = FileUtils.stripLastPathElement(toPath);
				VirtualFile toParentFile = getFile(toParent);
				toMount = toParentFile.getMount() != null ? toParentFile.getMount() : toParentFile.getDefaultMount();
			}

			if (toMount == null) {
				throw new AccessDeniedException("The target destination is not mounted.");
			}
			if (toMount.isReadOnly()) {
				throw new AccessDeniedException("The target destination is read only.");
			}
			String toChildPath = FileUtils.stripParentPath(toMount.getVirtualPath(), toPath);
			String fromChildPath = FileUtils.stripParentPath(fromMount.getVirtualPath(), fromFile.getVirtualPath());

			return process(fromMount, fromChildPath, fromFile.getVirtualPath(), toMount, toChildPath, toPath);
		}

		T process(FileResource fromResource, String fromChildPath, String fromPath, FileResource toResource,
				String toChildPath, String toPath) throws IOException, AccessDeniedException {
			try {

				fromFile = resolveVFSFile(fromResource);
				fromFile = fromFile.resolveFile(fromChildPath);

				toFile = resolveVFSFile(toResource);
				toFile = toFile.resolveFile(toChildPath);

				switch (fromFile.getType()) {
				case FILE:
					if (toFile.exists() && toFile.getType() == FileType.FOLDER) {
						String filename = FileUtils.lastPathElement(fromPath);
						toFile = toFile.resolveFile(filename);
						if (StringUtils.isBlank(toChildPath)) {
							toChildPath = filename;
						} else {
							toChildPath = FileUtils.checkEndsWithSlash(toChildPath) + filename;
						}
					}
					break;
				case FOLDER:
					switch (toFile.getType()) {
					case FILE:
					case FILE_OR_FOLDER:
						throw new IOException(String.format(
								"Source path %s and destination path %s types are incompatible", fromPath, toPath));
					default:
					}

					break;
				default:
				}
				return onFilesResolved(fromResource, fromChildPath, fromFile, toResource, toChildPath, toFile);

			} catch (IOException ex) {
				onFilesUnresolved(fromPath, toPath, ex);
				throw ex;
			}
		}

		FileResource getFromMount() {
			return fromMount;
		}

		FileResource getToMount() {
			return toMount;
		}

		FileObject getFromFile() {
			return fromFile;
		}

		FileObject getToFile() {
			return toFile;
		}

		abstract T onFilesResolved(FileResource fromResource, String fromChildPath, FileObject fromFile,
				FileResource toResource, String toChildPath, FileObject toFile) throws IOException;

		abstract void onFilesUnresolved(String fromPath, String toPath, IOException t);
	}

	public FileObject getFileObject(String virtualPath) throws IOException, AccessDeniedException {

		virtualPath = normalise(virtualPath);

		VirtualFile file;
		try {
			file = getFile(virtualPath);
			if (file.getFileObject() != null) {
				return file.getFileObject();
			}
			String childPath = FileUtils.stripParentPath(file.getMount().getVirtualPath(), virtualPath);
			return resolveVFSFile(file.getMount()).resolveFile(childPath);
		} catch (FileNotFoundException e) {
			String parent = FileUtils.stripLastPathElement(virtualPath);
			VirtualFile parentFile = getFile(parent);

			if (parentFile.getMount() == null && parentFile.getDefaultMount() == null) {
				throw new AccessDeniedException();
			}
			FileResource resource = parentFile.getMount();
			FileObject parentObject = resolveVFSFile(resource);
			String childPath = FileUtils.stripParentPath(resource.getVirtualPath(), virtualPath);
			return parentObject.resolveFile(childPath);
		}
	}

	private String getUsername(FileResource resource) {
		String username = overrideUsername.get();
		if (StringUtils.isBlank(username)) {
			username = resource.getUsername();
		}
		return username;
	}

	private String getPassword(FileResource resource) {
		String password = overridePassword.get();
		if (StringUtils.isBlank(password)) {
			password = resource.getPassword();
		}
		return password;
	}

	public <T extends Resource> FileSystemManager getManager(T id, CacheStrategy cacheStrategy)
			throws FileSystemException {
		synchronized (managers) {
			String key = String.valueOf(id == null ? "__DEFAULT__" : id.getId());
			FileSystemManager mgr = managers.get(key);
			if (mgr == null) {
				// TODO remove from cache when resource is deleted
				DefaultFileSystemManager vfsMgr = new DefaultFileSystemManager();
				vfsMgr.setLogger(LogFactory.getLog(key));
				vfsMgr.setCacheStrategy(cacheStrategy);
				for (Map.Entry<String, FileProvider> en : providers.entrySet()) {
					if(!vfsMgr.hasProvider(en.getKey()))
						vfsMgr.addProvider(en.getKey(), en.getValue());
				}
				vfsMgr.init();
				managers.put(key, vfsMgr);
				mgr = vfsMgr;
			}
			return mgr;
		}
	}

	protected FileObject resolveVFSFile(FileResource resource) throws IOException {
		FileResourceScheme scheme = fileService.getScheme(resource.getScheme());

		FileObject obj;
		if (scheme.getFileService() != null) {
			String url = resource.getPrivateUrl(getCurrentPrincipal(), userVariableReplacement, getUsername(resource),
					getPassword(resource));
			FileSystemOptions opts = scheme.getFileService().buildFileSystemOptions(resource);
			obj = getManager(resource, resource.getCacheStrategy()).resolveFile(url, opts);
		} else {
			String url = resource.getPrivateUrl(getCurrentPrincipal(), userVariableReplacement, getUsername(resource),
					getPassword(resource));
			obj = getManager(resource, resource.getCacheStrategy()).resolveFile(url);
		}

		if (scheme.isCreateRoot()) {
			if (!obj.exists()) {
				obj.createFolder();
			}
		}

		return obj;
	}

	protected FileSystemOptions buildFilesystemOptions(FileResource resource) throws IOException {
		FileResourceScheme scheme = fileService.getScheme(resource.getScheme());
		return scheme.getFileService().buildFileSystemOptions(resource);
	}

	private String normalise(String path) {
		path = Paths.get(path).normalize().toString();
		if (File.separatorChar == '\\') {
			path = path.replace('\\', '/');
		}
		return path;
	}

	@Override
	public void downloadCannotStart(FileResource resource, String childPath, FileObject file, Throwable t,
			String protocol) {
		if (checkEventFilter(file.getName().getBaseName())) {
			eventService.publishEvent(new DownloadStartedEvent(this, t, getCurrentSession(), childPath, protocol));
		}
	}

	@Override
	public DownloadStartedEvent downloadStarted(FileResource resource, String childPath, FileObject file,
			InputStream in, String protocol) {
		DownloadStartedEvent evt = new DownloadStartedEvent(this, getCurrentSession(), resource, childPath, in,
				protocol);

		if (checkEventFilter(file.getName().getBaseName())) {
			eventService.publishEvent(evt);
		}
		return evt;
	}

	@Override
	public void downloadComplete(FileResource resource, String childPath, FileObject file, long bytesOut,
			long timeMillis, String protocol, Session session) {

		setCurrentSession(session, Locale.getDefault());
		try {
			if (checkEventFilter(file.getName().getBaseName())) {
				eventService.publishEvent(
						new DownloadCompleteEvent(this, session, resource, childPath, bytesOut, timeMillis, protocol));
			}
		} finally {
			clearPrincipalContext();
		}
	}

	@Override
	public void downloadFailed(FileResource resource, String childPath, FileObject file, Throwable t, String protocol,
			Session session) {

		setCurrentSession(session, Locale.getDefault());
		try {
			if (checkEventFilter(file.getName().getBaseName())) {
				eventService.publishEvent(new DownloadCompleteEvent(this, t, session, resource, childPath, protocol));
			}
		} finally {
			clearPrincipalContext();
		}
	}

	@Override
	public void uploadCannotStart(String virtualPath, Throwable t, String protocol) {

		if (checkEventFilter(FileUtils.lastPathElement(virtualPath))) {
			eventService.publishEvent(new UploadStartedEvent(this, t, getCurrentSession(), virtualPath, protocol));
		}
	}

	@Override
	public UploadStartedEvent uploadStarted(FileResource resource, String childPath, FileObject file, String protocol) {

		UploadStartedEvent evt = new UploadStartedEvent(this, getCurrentSession(), resource, childPath, file, protocol);

		if (checkEventFilter(file.getName().getBaseName())) {
			eventService.publishEvent(evt);
		}

		return evt;
	}

	@Override
	public void uploadComplete(FileResource resource, String childPath, FileObject file, long bytesIn, long timeMillis,
			String protocol) {

		if (checkEventFilter(file.getName().getBaseName())) {
			eventService.publishEvent(new UploadCompleteEvent(this, getCurrentSession(), resource, childPath, bytesIn,
					timeMillis, protocol));
		}
	}

	private boolean checkEventFilter(String filename) {

		String[] excludeFilters = configurationService.getValues("fsEvents.excludeFilter");

		boolean exclude = false;
		for (String filter : excludeFilters) {
			log.info("Checking filter " + filter + " for " + filename);
			if (filename.endsWith(filter)) {
				exclude = true;
				log.info("Simple matched filter " + filter + " for " + filename);
				break;
			} else if (filename.matches(filter)) {
				exclude = true;
				log.info("Regex matched filter " + filter + " for " + filename);
				break;
			} else if (filter.indexOf("*") > 0) {
				String starts = filter.substring(0, filter.indexOf("*"));
				if (filename.startsWith(starts)) {
					log.info("StartsWith matched filter " + filter + " for " + filename);
					exclude = true;
					break;
				}
			}
		}

		return !exclude;
	}

	@Override
	public void uploadFailed(FileResource resource, String childPath, FileObject file, long bytesIn, Throwable t,
			String protocol) {
		if (checkEventFilter(file.getName().getBaseName())) {
			eventService
					.publishEvent(new UploadCompleteEvent(this, getCurrentSession(), t, resource, childPath, protocol));
		}
	}

	class FileObjectUploadStore implements FileStore {

		FileObject file;
		FileResource resource;
		String childPath;
		String protocol;
		String virtualPath;

		FileObjectUploadStore(FileObject file, FileResource resource, String childPath, String protocol) {
			this.file = file;
			this.resource = resource;
			this.childPath = childPath;
			this.protocol = protocol;
			this.virtualPath = FileUtils.checkEndsWithSlash(resource.getVirtualPath()) + childPath;
		}

		public FileObject getFileObject() {
			return file;
		}

		public String getVirtualPath() {
			return virtualPath;
		}

		@Override
		public long writeFile(Realm realm, String filename, String uuid, InputStream in) throws IOException {

			long bytesIn = 0;

			UploadStartedEvent event = uploadStarted(resource, childPath, file, protocol);
			file = event.getOutputFile();
			virtualPath = event.getTransformationPath();

			OutputStream out = event.getOutputStream();
			try {

				bytesIn = IOUtils.copyLarge(in, out);
				event.getOutputFile().refresh();

				uploadComplete(resource,
						FileUtils.stripParentPath(resource.getVirtualPath(), event.getTransformationPath()),
						event.getOutputFile(), bytesIn, System.currentTimeMillis() - event.getTimestamp(), protocol);

			} catch (Exception e) {

				eventService.publishEvent(new UploadCompleteEvent(this, getCurrentSession(), e, resource,
						FileUtils.stripParentPath(resource.getVirtualPath(), event.getTransformationPath()), protocol));
			} finally {
				FileUtils.closeQuietly(in);
				FileUtils.closeQuietly(out);
			}

			return bytesIn;
		}

		@Override
		public InputStream getInputStream(FileUpload fileByUuid) throws IOException {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public InputStream downloadFile(String virtualPath, final long position, final String proto)
			throws IOException, AccessDeniedException {

		virtualPath = normalise(virtualPath);

		getFile(virtualPath);

		FileResolver<InputStream> resolver = new FileResolver<InputStream>(true, false) {

			@Override
			InputStream onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath)
					throws IOException {
				InputStream in = file.getContent().getInputStream();
				DownloadStartedEvent evt = downloadStarted(resource, childPath, file, in, proto);

				return new ContentInputStream(resource, childPath, file, evt.getInputStream(), position,
						file.getContent().getSize() - position, VirtualFileServiceImpl.this, evt.getTimestamp(), proto,
						getCurrentSession());
			}

			@Override
			void onFileUnresolved(String path, Exception t) {
				eventService.publishEvent(new DownloadStartedEvent(this, t, getCurrentSession(), path, proto));
			}
		};

		return resolver.processRequest(virtualPath);
	}

	@Override
	public InputStream downloadFile(String virtualPath, final long position, final String proto,
			Principal overridePrincipal, String overrideUsername, String overridePassword)
			throws IOException, AccessDeniedException {

		virtualPath = normalise(virtualPath);

		getFile(virtualPath);

		FileResolver<InputStream> resolver = new FileResolver<InputStream>(true, false, overridePrincipal,
				overrideUsername, overridePassword) {

			@Override
			InputStream onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath)
					throws IOException {
				InputStream in = file.getContent().getInputStream();
				DownloadStartedEvent evt = downloadStarted(resource, childPath, file, in, proto);

				return new ContentInputStream(resource, childPath, file, evt.getInputStream(), position,
						file.getContent().getSize() - position, VirtualFileServiceImpl.this, evt.getTimestamp(), proto,
						getCurrentSession());
			}

			@Override
			void onFileUnresolved(String path, Exception t) {
				eventService.publishEvent(new DownloadStartedEvent(this, t, getCurrentSession(), path, proto));
			}
		};

		return resolver.processRequest(virtualPath);
	}

	@Override
	public OutputStream uploadFile(String virtualPath, final long position, final String proto)
			throws IOException, AccessDeniedException {
		return uploadFile(virtualPath, position, proto, this);
	}

	@Override
	public OutputStream uploadFile(String virtualPath, final long position, final String proto,
			final UploadEventProcessor uploadProcessor) throws IOException, AccessDeniedException {

		virtualPath = normalise(virtualPath);

		String parentPath = FileUtils.checkEndsWithSlash(FileUtils.stripLastPathElement(virtualPath));
		final VirtualFile parentFile = getFile(parentPath);

		VirtualFile file = null;

		try {
			file = getFile(virtualPath);
		} catch (FileNotFoundException e) {
		}

		FileResolver<OutputStream> resolver = new FileResolver<OutputStream>(file != null, true) {

			@Override
			OutputStream onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath)
					throws IOException {
				UploadStartedEvent event = uploadProcessor.uploadStarted(resource, childPath, file, proto);

				return new ContentOutputStream(resource, childPath, virtualPath, virtualRepository, parentFile,
						event.getOutputFile(), event.getOutputStream(), position,
						event.getTimestamp(), new SessionAwareUploadEventProcessor(getCurrentSession(),
								getCurrentLocale(), VirtualFileServiceImpl.this, uploadProcessor),
						proto, getOwnerPrincipal(resource));
			}

			@Override
			void onFileUnresolved(String virtualPath, Exception t) {
				uploadProcessor.uploadCannotStart(virtualPath, t, proto);
			}
		};
		return resolver.processRequest(virtualPath);

	}

	public Principal getOwnerPrincipal(FileResource resource) {
		return null;
	}

	@Override
	public FileObject getFileObject(FileResource resource) throws IOException {
		return resolveVFSFile(resource);
	}

	@Override
	public void setLastModified(String virtualPath, final long lastModified, String protocol)
			throws IOException, AccessDeniedException {

		virtualPath = normalise(virtualPath);

		getFile(virtualPath);

		FileResolver<Object> resolver = new FileResolver<Object>(true, true) {

			@Override
			Object onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath)
					throws IOException {
				file.getContent().setLastModifiedTime(lastModified);
				return null;
			}

			@Override
			void onFileUnresolved(String path, Exception t) {
			}

		};

		resolver.processRequest(virtualPath);
	}

	@Override
	public UserVariableReplacementService getUserVariableReplacement() {
		return userVariableReplacement;
	}

	@Override
	public void setDefaultMount(VirtualFile file, FileResource mount)
			throws AccessDeniedException, ResourceChangeException {

		assertPermission(FileResourcePermission.UPDATE);

		if (mount != null && mount.isReadOnly()) {
			throw new ResourceChangeException(FileResourceServiceImpl.RESOURCE_BUNDLE, "error.readOnly",
					mount.getName());
		}
		file.setDefaultMount(mount);
		file.setWritable(mount != null);
		virtualRepository.saveFile(file);
	}

	@Override
	public boolean isRootWritable(Principal currentPrincipal) throws IOException, AccessDeniedException {

		VirtualFile root = getRootFolder();
		if (root.getDefaultMount() == null) {
			return false;
		}
		Collection<FileResource> resources = fileService.getPersonalResources();
		return resources.contains(root.getDefaultMount());
	}

	@Override
	public void detachMount(FileResource resource) {
		virtualRepository.removeFileResource(resource);
	}

	@Override
	public void attachMount(VirtualFile mountedFile, FileResource resource) {
		virtualRepository.addFileResource(mountedFile, resource);
	}

	@Override
	public void addProvider(String scheme, FileProvider provider) throws FileSystemException {
		synchronized (managers) {
			if (providers.containsKey(scheme))
				throw new IllegalArgumentException(String.format("Provider already registered for %s.", scheme));
			for (Map.Entry<String, FileSystemManager> en : managers.entrySet()) {
				if (en.getValue() instanceof DefaultFileSystemManager) {
					((DefaultFileSystemManager) en.getValue()).addProvider(scheme, provider);
				}
			}
			providers.put(scheme, provider);
		}

	}
}