package com.hypersocket.vfs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.apache.derby.impl.io.vfmem.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hypersocket.auth.PasswordEnabledAuthenticatedServiceImpl;
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
import com.hypersocket.realm.Principal;
import com.hypersocket.realm.Realm;
import com.hypersocket.realm.UserVariableReplacement;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceCreationException;
import com.hypersocket.scheduler.SchedulerService;
import com.hypersocket.session.Session;
import com.hypersocket.tables.ColumnSort;
import com.hypersocket.upload.FileStore;
import com.hypersocket.upload.FileUpload;
import com.hypersocket.upload.FileUploadService;
import com.hypersocket.utils.FileUtils;
import com.hypersocket.vfs.json.HttpDownloadProcessor;

@Service
public class VirtualFileServiceImpl extends PasswordEnabledAuthenticatedServiceImpl 
	implements VirtualFileService, DownloadEventProcessor, UploadEventProcessor {

	static Logger log = LoggerFactory.getLogger(VirtualFileServiceImpl.class);
	
	@Autowired
	SchedulerService schedulerService; 
	
	@Autowired
	FileResourceService fileService; 
	
	@Autowired
	VirtualFileRepository virtualRepository; 
	
	@Autowired
	UserVariableReplacement userVariableReplacement;
	
	@Autowired
	EventService eventService; 
	
	@Autowired
	FileUploadService uploadService;

	@Autowired
	VirtualFileSynchronizationService syncService;

	@Override
	public Collection<VirtualFile> getVirtualFolders() throws AccessDeniedException {
		
		assertPermission(FileResourcePermission.READ);
		
		return virtualRepository.getVirtualFolders(getCurrentRealm());
	}
	
	@Override
	public VirtualFile getRootFolder() throws FileNotFoundException, AccessDeniedException {
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
	public VirtualFile getFile(String virtualPath) throws FileNotFoundException, AccessDeniedException {
		
		virtualPath = normalise(virtualPath);
		
		if(virtualPath.equals("/")) {
			return getRootFolder();
		}
		
		FileResource[] resources = getPrincipalResources();
		VirtualFile file = virtualRepository.getVirtualFileByResource(virtualPath, getCurrentRealm(), getCurrentPrincipal(), resources);

		if(file!=null) {
			int invalidateCacheMs = fileService.getResourceIntProperty(file.getMount(), "fs.invalidateCacheSeconds") * 1000;
			if(file.isFolder()) {
				if(!file.getSync() || (System.currentTimeMillis() - file.getModifiedDate().getTime()) > invalidateCacheMs) {
					file = null;
				}
			}
		}
		
		if(file==null) {
			syncService.synchronize(virtualPath, getCurrentPrincipal(), resources);
			file = virtualRepository.getVirtualFile(virtualPath, getCurrentRealm(), null);
			if(file==null) {
				throw new FileNotFoundException(virtualPath);
			}
		}
		return file;
	}
	
	@Override
	public Principal getOwnerPrincipal(FileResource resource) {
		return syncService.canSynchronize(resource) ? null : syncService.isUserFilesystem(resource) ? getCurrentPrincipal() : null;
	}
	
	@Override
	public Collection<FileResource> getMountsForPath(String virtualPath) throws FileNotFoundException, AccessDeniedException {
		VirtualFile file = getFile(virtualPath);
		return fileService.getResourcesByVirtualPath(file.getVirtualPath());
	}
	
	@Override
	public Collection<VirtualFile> getChildren(String virtualPath) throws FileNotFoundException, AccessDeniedException {
		VirtualFile file = getFile(virtualPath);
		return getChildren(file);
	}
	
	@Override
	public Collection<VirtualFile> getChildren(VirtualFile folder) throws AccessDeniedException {
		return virtualRepository.getVirtualFiles(folder, getCurrentRealm(), getCurrentPrincipal(), getPrincipalResources());
	}

	@Override
	public Boolean deleteFile(String virtualPath, String proto) throws IOException, AccessDeniedException {
		
		virtualPath = normalise(virtualPath);
		VirtualFile file = getFile(virtualPath);
		
		return deleteFile(file, proto);
	}
	
	@Override
	public Boolean deleteFile(VirtualFile file, String proto) throws IOException, AccessDeniedException {
		
		if(!file.isMounted()) {
			
			Collection<FileResource> mounts = fileService.getResourcesByVirtualPath(file.getVirtualPath());
			if(!mounts.isEmpty()) {			
				eventService.publishEvent(new VirtualFolderDeletedEvent(this, 
						new AccessDeniedException(),
						getCurrentSession(), 
						file.getVirtualPath()));
				throw new AccessDeniedException();
			}
			
			try {
				virtualRepository.removeReconciledFolder(file, true);
				eventService.publishEvent(new VirtualFolderDeletedEvent(this, 
						getCurrentSession(), 
						file.getVirtualPath()));
				return true;
			
			} catch(Throwable t) {
				eventService.publishEvent(new VirtualFolderDeletedEvent(this, 
						t,
						getCurrentSession(), 
						file.getVirtualPath()));
				throw t;
			}
		} else {
		
			DeleteFileResolver resolver = new DeleteFileResolver(proto);
			if(resolver.getFile()==null || !resolver.getFile().exists()) {
				virtualRepository.removeReconciledFile(file);
				return true;
			}
			boolean success = resolver.processRequest(file);
			if(success) {
				virtualRepository.removeReconciledFile(file);
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public VirtualFile createFile(String virtualPath, String proto) throws IOException, AccessDeniedException {
		
		virtualPath = normalise(virtualPath);
		
		try {
			getFile(virtualPath);
			throw new FileExistsException(virtualPath);
		} catch(FileNotFoundException ex) {
			
			String parentPath = FileUtils.stripLastPathElement(virtualPath);
			String newName = FileUtils.stripParentPath(parentPath, virtualPath);
			
			CreateFileFileResolver resolver = new CreateFileFileResolver(newName, proto);
			
			VirtualFile parent = getFile(parentPath);
			
			FileObject newFolder = resolver.processRequest(virtualPath);
			return virtualRepository.reconcileFile(newFolder.getName().getBaseName(), newFolder, resolver.getParentResource(), parent, getOwnerPrincipal(resolver.getParentResource()));
		}
	}
	
	
	@Override
	public VirtualFile createFolder(String virtualPath, String proto) throws IOException, AccessDeniedException {
		return createFolder(virtualPath, proto, false);
	}
	
	@Override
	public VirtualFile createFolder(String virtualPath, String proto, boolean disableEvent) throws IOException, AccessDeniedException {
		
		virtualPath = FileUtils.checkEndsWithSlash(normalise(virtualPath));
		
		try {
			getFile(virtualPath);
			throw new FileExistsException(virtualPath);
		} catch(FileNotFoundException ex) {
			String parentPath = FileUtils.stripLastPathElement(virtualPath);
			String newName = FileUtils.stripParentPath(parentPath, virtualPath);
			
			CreateFolderFileResolver resolver = new CreateFolderFileResolver(newName, proto, !disableEvent);
			
			VirtualFile parent = getFile(parentPath);
			
			FileObject newFolder = resolver.processRequest(virtualPath);
			return virtualRepository.reconcileNewFolder(newFolder.getName().getBaseName(), parent, newFolder, resolver.getParentResource(), false, getOwnerPrincipal(resolver.getParentResource()));
			
		}
	}
	
	@Override
	public VirtualFile createVirtualFolder(String virtualPath) throws IOException, AccessDeniedException {
		
		virtualPath = FileUtils.checkEndsWithSlash(normalise(virtualPath));
		
		String parentPath = virtualPath;
		String newName = "untitled folder";
		
		VirtualFile parent = getFile(parentPath);
		int i = 0;
		while(true) {
			try {
				getFile(FileUtils.checkEndsWithSlash(virtualPath) + newName);
				newName = "untitled folder " + ++i;
			} catch(FileNotFoundException ex) {
				break;
			}
		}

		try {
			VirtualFile file = virtualRepository.createVirtualFolder(newName, parent);
			
			eventService.publishEvent(new VirtualFolderCreatedEvent(this, 
					getCurrentSession(), 
					 FileUtils.checkEndsWithSlash(virtualPath) + newName));
			return file;
		} catch(Throwable e) {
			eventService.publishEvent(new VirtualFolderCreatedEvent(this, 
					e,
					getCurrentSession(), 
					 virtualPath));
			throw e;
		}
	}
	
	@Override
	public VirtualFile createUntitledFolder(String virtualPath, String proto) throws IOException, AccessDeniedException {
		
		virtualPath = FileUtils.checkEndsWithSlash(normalise(virtualPath));
		
		String newName = "untitled folder";
		int i = 1;
		while(true) {
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
		return virtualRepository.reconcileNewFolder(newFolder.getName().getBaseName(), parentFile, newFolder, parentFile.getMount(), false, getOwnerPrincipal(resolver.getParentResource()));
	}

	@Override
	public VirtualFile renameFile(String fromVirtualPath, String toVirtualPath, String proto) throws IOException, AccessDeniedException {
		
		fromVirtualPath = normalise(fromVirtualPath);
		toVirtualPath = normalise(toVirtualPath);
		VirtualFile fromFile = getFile(fromVirtualPath);

		if(fromVirtualPath.equals(toVirtualPath)) {
			return fromFile;
		}
		
		return renameFile(fromFile, toVirtualPath, proto);
	}
	
	@Override
	public VirtualFile renameFile(VirtualFile fromFile, String toVirtualPath, String proto) throws IOException, AccessDeniedException {
		try {
			getFile(toVirtualPath);
			if(!fromFile.isMounted()) {
				eventService.publishEvent(new VirtualFolderUpdatedEvent(this, 
						new FileExistsException(toVirtualPath),
						getCurrentSession(), 
						fromFile.getVirtualPath(),
						toVirtualPath));
			}
			throw new FileExistsException(toVirtualPath);
		} catch(FileNotFoundException ex) {
			
			if(!fromFile.isMounted()) {
				if(!getChildren(fromFile).isEmpty()) {
					eventService.publishEvent(new VirtualFolderUpdatedEvent(this, 
							new AccessDeniedException(),
							getCurrentSession(), 
							fromFile.getVirtualPath(),
							toVirtualPath));
					throw new AccessDeniedException();
				}
				/**
				 * Rename a virtual folder
				 */
				try {
					String originalPath = fromFile.getVirtualPath();
					VirtualFile file =  virtualRepository.renameVirtualFolder(fromFile, toVirtualPath);
					eventService.publishEvent(new VirtualFolderUpdatedEvent(this, 
							getCurrentSession(), 
							originalPath,
							toVirtualPath));
					return file;
				} catch(Throwable t) {
					eventService.publishEvent(new VirtualFolderUpdatedEvent(this, 
							t,
							getCurrentSession(), 
							fromFile.getVirtualPath(),
							toVirtualPath));
					throw t;
				}
			}
			RenameFileResolver resolver = new RenameFileResolver(proto);
			if(!resolver.processRequest(fromFile, toVirtualPath)) {
				throw new IOException(String.format("Failed to rename file %s to %s", fromFile.getVirtualPath(), toVirtualPath));
			}
			
			VirtualFile parent = getFile(FileUtils.stripLastPathElement(toVirtualPath));
			VirtualFile existingFile = virtualRepository.getVirtualFile(toVirtualPath, getCurrentRealm(), getCurrentPrincipal());
			String displayName = FileUtils.lastPathElement(toVirtualPath);
			if(existingFile!=null) {
				displayName = String.format("%s (%s)", displayName, resolver.getToMount().getName());
			}
			if(fromFile.isFolder()) {
				return virtualRepository.reconcileFolder(displayName, fromFile, resolver.getToFile(), resolver.getToMount(), existingFile!=null, getOwnerPrincipal(resolver.getToMount()));
			} else {
				
				virtualRepository.removeReconciledFile(fromFile);	
				return virtualRepository.reconcileFile(displayName, resolver.getToFile(), resolver.getToMount(), parent, getOwnerPrincipal(resolver.getToMount()));
			}
		}
	}
	
	@Override
	public boolean copyFile(String fromPath, String toPath, String proto) throws IOException, AccessDeniedException {

		fromPath = normalise(fromPath);
		toPath = normalise(toPath);

		VirtualFile toParent = getFile(FileUtils.stripLastPathElement(toPath));
		
		CopyFileResolver resolver = new CopyFileResolver(proto);
		boolean success = resolver.processRequest(fromPath, toPath);

		boolean isTargetRootOfMount = FileUtils.checkEndsWithNoSlash(resolver.getToMount().getVirtualPath()).equals(FileUtils.checkEndsWithNoSlash(toPath));
		VirtualFile existingFile = virtualRepository.getVirtualFile(toPath, getCurrentRealm(), getCurrentPrincipal());
		
		String displayName = FileUtils.lastPathElement(toPath);
		boolean conflicted = false;
		if(existingFile!=null && !existingFile.getMount().equals(resolver.getToMount())) {
			displayName = String.format("%s (%s)", displayName, resolver.getToMount().getName());
			conflicted = true;
		}
		if(success) {
			switch(resolver.getToFile().getType()) {
			case FILE:
				virtualRepository.reconcileFile(displayName, resolver.getToFile(), resolver.getToMount(), toParent, getOwnerPrincipal(resolver.getToMount()));
				break;
			case FOLDER:
				if(!isTargetRootOfMount) {
					if(existingFile==null) {
						virtualRepository.reconcileNewFolder(displayName, toParent, resolver.getToFile(), resolver.getToMount(), conflicted, null);
					} else {
						
						virtualRepository.reconcileFolder(displayName, existingFile, resolver.getToFile(), resolver.getToMount(), conflicted, null);
					}
				}
				syncService.synchronize(toPath, getOwnerPrincipal(resolver.getToMount()), resolver.getToMount());
				break;
			default:
				log.error(String.format("File %s is not a file or folder", toPath));
				return false;
			}
		
			return true;
		}
		return false;
	}
	
	protected FileResource[] getPrincipalResources() throws AccessDeniedException {
		if(permissionService.hasSystemPermission(getCurrentPrincipal())) {
			return fileService.getResources(getCurrentRealm()).toArray(new FileResource[0]);
		} else {
			return fileService.getPersonalResources().toArray(new FileResource[0]);
		}
	}

	@Override
	public void downloadFile(String virtualPath, final HttpDownloadProcessor processor, final String proto) throws AccessDeniedException, IOException {
		
		virtualPath = normalise(virtualPath);
		VirtualFile file = getFile(virtualPath);
		downloadFile(file, processor, proto);
		
	}
	
	@Override
	public void downloadFile(VirtualFile file, final HttpDownloadProcessor processor, final String proto) throws AccessDeniedException, IOException {
		
		if(file.getMount()==null) {
			throw new IOException("Download must be a file and have a valid mount point!");
		}
		FileResolver<Object> resolver = new FileResolver<Object>(true, false) {

			void checkFile(FileObject file) throws IOException {
				if(file.getType()!=FileType.FILE) {
					throw new IOException("Download must be a file!");
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
				eventService.publishEvent(
						new DownloadStartedEvent(this, t, getCurrentSession(), path, proto));
			}
		};
		
		resolver.processRequest(file);
	}

	@Override
	public FileUpload uploadFile(String virtualPath, final InputStream in, final UploadProcessor<?> processor, final String proto) throws AccessDeniedException, IOException {
		
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
					upload = uploadService.createFile(in, PathUtil.getBaseName(childPath), getCurrentRealm(), false, "upload", store);

					if (processor != null) {
						processor.processUpload(resource, resolveVFSFile(parentFile.getMount()), childPath, file);
					}
					
					VirtualFile existingFile = virtualRepository.getVirtualFile(store.getVirtualPath(), getCurrentRealm(), getOwnerPrincipal(resource));
					
					String displayName = FileUtils.lastPathElement(store.getVirtualPath());
					if(existingFile!=null && !existingFile.getMount().equals(resource)) {
						displayName = String.format("%s (%s)", displayName, parentFile.getMount().getName());
					} 
					
					if(existingFile!=null) {
						virtualRepository.reconcileFile(displayName, store.getFileObject(), resource, 
								existingFile, existingFile.getParent(), getOwnerPrincipal(resource));
					} else {
						virtualRepository.reconcileFile(displayName, store.getFileObject(), resource, 
								VirtualFileServiceImpl.this.getFile(FileUtils.stripLastPathElement(virtualPath)), getOwnerPrincipal(resource));
					}

					return upload;

				} catch (ResourceCreationException e) {
					throw new IOException(e);
				} catch (AccessDeniedException e) {
					throw new IOException(e);
				}
			}

			@Override
			void onFileUnresolved(String path, Exception t) {
				eventService.publishEvent(
						new UploadStartedEvent(this, t, getCurrentSession(), path, proto));
			}
		};
		
		return resolver.processRequest(virtualPath);

	}

	@Override
	public Collection<VirtualFile> listChildren(String virtualPath, String proto) throws FileNotFoundException, AccessDeniedException {
		return getChildren(virtualPath);
	}

	@Override
	public long getSearchCount(String virtualPath, String searchColumn, String search) throws AccessDeniedException {
		VirtualFile file = virtualRepository.getVirtualFileByResource(virtualPath, getCurrentRealm(), getCurrentPrincipal(), getPrincipalResources());
		return virtualRepository.getCount(VirtualFile.class, searchColumn, search, new ParentCriteria(file));
	}

	@Override
	public Collection<VirtualFile> searchFiles(String virtualPath, 
			String searchColumn,
			String search, 
			int offset, 
			int limit,
			ColumnSort[] sort,
			String proto) throws AccessDeniedException, IOException {
		
		FileResource[] resources = getPrincipalResources();
		VirtualFile searchPath = getFile(virtualPath);
		return virtualRepository.search(searchColumn, search, offset, limit, sort, searchPath, getCurrentRealm(), getCurrentPrincipal(), resources);

	}

	
	abstract class FileResolver<T> {

		FileObject file;
		boolean fileExits;
		boolean isUpdate;
		FileResource parentResource;
		VirtualFile virtualFile;
		
		FileResolver(boolean fileExists, boolean isUpdate) {
			this.fileExits = fileExists;
			this.isUpdate = isUpdate;
		}

		T processRequest(String path) throws IOException, AccessDeniedException {

			path = normalise(path);
			
			if(fileExits) {
				virtualFile = VirtualFileServiceImpl.this.getFile(path);
				String childPath = FileUtils.stripParentPath(virtualFile.getMount().getVirtualPath(), path);
				return processRequest(virtualFile.getMount(), childPath, path);
			} else {
				String parentPath = FileUtils.stripLastPathElement(path);
				VirtualFile parentFile = VirtualFileServiceImpl.this.getFile(parentPath);
				if(parentFile.isMounted()) {
					String childPath = FileUtils.stripParentPath(parentFile.getMount().getVirtualPath(), path);
					parentResource = parentFile.getMount();
					if(isUpdate && parentResource.isReadOnly()) {
						throw new AccessDeniedException();
					}
					return processRequest(parentResource, childPath, path);
				} else {
					parentResource = parentFile.getDefaultMount();
					
					if(parentResource==null) {
						throw new AccessDeniedException();
					}
					if(isUpdate && parentResource.isReadOnly()) {
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
		
		T processRequest(FileResource resource, String childPath, String path) throws IOException, AccessDeniedException {

			try {
				file = resolveVFSFile(resource);
				file = file.resolveFile(childPath);

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
		
		abstract T onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath) throws IOException;

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
			if(parentFile.isMounted()) {
				parentResource = parentFile.getMount();
				if(parentResource.isReadOnly()) {
					throw new AccessDeniedException();
				}
				if(!checkParent(parentResource)) {
					throw new AccessDeniedException();
				}
				String childPath = FileUtils.stripParentPath(parentFile.getMount().getVirtualPath(), path);
				return processRequest(parentResource, childPath, path);
			} else {
				parentResource = parentFile.getDefaultMount();
				if(parentResource==null) {
					throw new AccessDeniedException();
				}
				if(parentResource.isReadOnly()) {
					throw new AccessDeniedException();
				}
				if(!checkParent(parentResource)) {
					throw new AccessDeniedException();
				}
				String childPath = FileUtils.stripParentPath(parentResource.getVirtualPath(), path);
				return processRequest(parentResource, childPath, path);
			}
		}

		boolean checkParent(FileResource parentResource) {
			return true;
		}
		
		T processRequest(FileResource resource, String childPath, String path) throws IOException, AccessDeniedException {

			try {
				file = resolveVFSFile(resource);
				if(StringUtils.isNotBlank(childPath)) {
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
		
		abstract T onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath) throws IOException;

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
			if(newName==null) {
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

			if(wantsEvent) {
				eventService.publishEvent(new CreateFolderEvent(this, !exists && created, getCurrentSession(), resource,
					childPath, protocol));
			}

			return newFile;
		}

		@Override
		void onFileUnresolved(String path, Exception t) {
			if(wantsEvent) {
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
			if(newName==null) {
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
				eventService.publishEvent(new CopyFileEvent(this, e, getCurrentSession(),
						fromResource.getVirtualPath() + fromChildPath, 
						toResource.getVirtualPath() + toChildPath, protocol));

				return false;
			}
		}

		@Override
		void onFilesUnresolved(String fromPath, String toPath, IOException t) {
			eventService.publishEvent(new CopyFileEvent(this, t, getCurrentSession(), fromPath,
					 toPath, protocol));
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
				fromFile.moveTo(toFile);

				eventService.publishEvent(new RenameEvent(this, getCurrentSession(), 
						fromResource, fromChildPath,
						toResource, toChildPath, protocol));

				return true;
			} catch (Exception e) {
				if (log.isErrorEnabled()) {
					log.error("Failed to move resource", e);
				}
				eventService.publishEvent(new RenameEvent(this, e, getCurrentSession(),
						fromChildPath, toChildPath, protocol));

				return false;
			}
		}

		@Override
		void onFilesUnresolved(String fromPath, String toPath, IOException t) {
			eventService.publishEvent(new RenameEvent(this, t, getCurrentSession(), fromPath,
					toPath, protocol));
			
		}
	}
	
	class DeleteFileResolver extends FileResolver<Boolean> {

		String protocol;

		DeleteFileResolver(String protocol) {
			super(true, true);
			this.protocol = protocol;
		}

		@Override
		Boolean onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath)
				throws IOException {
			try {
				
				if (file.exists()) {
					boolean deleted = file.delete();

					if (deleted) {
						eventService.publishEvent(new DeleteFileEvent(
								this, deleted, getCurrentSession(), resource, childPath, protocol));
						return true;
					}
				}

				eventService.publishEvent(new DeleteFileEvent(
						this, false, getCurrentSession(), resource, childPath, protocol));

			} catch (FileSystemException ex) {
				eventService.publishEvent(new DeleteFileEvent(
						this, ex, getCurrentSession(), childPath, protocol));
			}
			return false;
		}

		@Override
		void onFileUnresolved(String path, Exception t) {
			eventService.publishEvent(new DeleteFileEvent(this, t, getCurrentSession(), path, protocol));
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
				toMount = toFile.getMount()!=null ? toFile.getMount() : toFile.getDefaultMount();
			} catch(FileNotFoundException ex) {
				String toParent = FileUtils.stripLastPathElement(toPath);
				VirtualFile toParentFile = getFile(toParent);
				toMount = toParentFile.getMount()!=null ? toParentFile.getMount() : toParentFile.getDefaultMount();
			}

			if(toMount==null) {
				throw new AccessDeniedException();
			}
			if(toMount.isReadOnly()) {
				throw new AccessDeniedException();
			}
			String toChildPath = FileUtils.stripParentPath(toMount.getVirtualPath(), toPath);
			String fromChildPath = FileUtils.stripParentPath(fromMount.getVirtualPath(), fromFile.getVirtualPath());

			return process(fromMount, fromChildPath, fromFile.getVirtualPath(), 
					toMount, toChildPath, toPath);
		}

		T process(FileResource fromResource, String fromChildPath, String fromPath,
				FileResource toResource, String toChildPath, String toPath)
				throws IOException, AccessDeniedException {
			try {

				fromFile = resolveVFSFile(fromResource);
				fromFile = fromFile.resolveFile(fromChildPath);

				toFile = resolveVFSFile(toResource);
				toFile = toFile.resolveFile(toChildPath);

				switch(fromFile.getType()) {
				case FILE: 
					if(toFile.getType()!=FileType.FILE) {
						String filename = FileUtils.lastPathElement(fromPath);
						toFile = toFile.resolveFile(filename);
						if(StringUtils.isBlank(toChildPath)) {
							toChildPath = filename;
						} else {
							toChildPath = FileUtils.checkEndsWithSlash(toChildPath) + filename;
						}
					}
					break;
				case FOLDER:
					switch(toFile.getType()) {
					case FILE:
					case FILE_OR_FOLDER:
						throw new IOException(String.format("Source path %s and destination path %s types are incompatible", fromPath, toPath));
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
			String childPath = FileUtils.stripParentPath(file.getMount().getVirtualPath(), virtualPath);
			return resolveVFSFile(file.getMount()).resolveFile(childPath);
		} catch (FileNotFoundException e) {
			String parent = FileUtils.stripLastPathElement(virtualPath);
			VirtualFile parentFile = getFile(parent);
			if(parentFile.getMount()==null && parentFile.getDefaultMount()==null) {
				throw new AccessDeniedException();
			}
			FileResource resource = parentFile.getMount()!=null ? parentFile.getMount() : parentFile.getDefaultMount();
			FileObject parentObject = resolveVFSFile(resource);
			String childPath =  FileUtils.stripParentPath(resource.getVirtualPath(), virtualPath);
			return parentObject.resolveFile(childPath);
		}
		
	}
	
	protected FileObject resolveVFSFile(FileResource resource) throws FileSystemException {
		FileResourceScheme scheme = fileService.getScheme(resource.getScheme());
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
		eventService.publishEvent(
				new DownloadStartedEvent(this, t, getCurrentSession(), childPath, protocol));
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
		eventService.publishEvent(new DownloadCompleteEvent(this, t, session, resource, childPath, protocol));
	}
	

	@Override
	public void uploadCannotStart(String virtualPath, Throwable t, String protocol) {
		eventService
				.publishEvent(new UploadStartedEvent(this, t, getCurrentSession(), virtualPath, protocol));
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
				new UploadCompleteEvent(this, getCurrentSession(), t, resource, childPath, protocol));
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
		public long writeFile(Realm realm, String uuid, InputStream in) throws IOException {

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
						event.getOutputFile(), 
						bytesIn, 
						System.currentTimeMillis() - event.getTimestamp(),
						protocol);

			} catch (Exception e) {

				eventService.publishEvent(
						new UploadCompleteEvent(this, getCurrentSession(), e, resource, 
								FileUtils.stripParentPath(resource.getVirtualPath(), event.getTransformationPath()), protocol));
			} finally {
				FileUtils.closeQuietly(in);
				FileUtils.closeQuietly(out);
			}

			return bytesIn;
		}
	}

	@Override
	public InputStream downloadFile(String virtualPath, final long position, final String proto)
			throws IOException, AccessDeniedException {
		
		virtualPath = normalise(virtualPath);
		
		getFile(virtualPath);

		FileResolver<InputStream> resolver = new FileResolver<InputStream>(true,false) {

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
				eventService.publishEvent(
						new DownloadStartedEvent(this, t, getCurrentSession(), path, proto));
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
	public OutputStream uploadFile(String virtualPath, final long position, final String proto, final UploadEventProcessor uploadProcessor)
			throws IOException, AccessDeniedException {
		
		virtualPath = normalise(virtualPath);
		
		String parentPath = FileUtils.checkEndsWithSlash(FileUtils.stripLastPathElement(virtualPath));
		final VirtualFile parentFile = getFile(parentPath);	
		
		VirtualFile file = null;

		try {
			file = getFile(virtualPath);
		} catch (FileNotFoundException e) {
		}
		
		FileResolver<OutputStream> resolver = new FileResolver<OutputStream>(file!=null, true) {

			@Override
			OutputStream onFileResolved(FileResource resource, String childPath, FileObject file, String virtualPath)
					throws IOException {
				UploadStartedEvent event = uploadProcessor.uploadStarted(resource, childPath, file, proto);
				
				return new ContentOutputStream(resource, childPath, virtualPath, virtualRepository, parentFile,
						event.getOutputFile(), event.getOutputStream(), position, event.getTimestamp(), 
						new SessionAwareUploadEventProcessor(getCurrentSession(),
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
	public UserVariableReplacement getUserVariableReplacement() {
		return userVariableReplacement;
	}

	@Override
	public void setDefaultMount(VirtualFile file, FileResource mount) throws AccessDeniedException, ResourceChangeException {
		
		assertPermission(FileResourcePermission.UPDATE);
		
		if(mount!=null && mount.isReadOnly()) {
			throw new ResourceChangeException(FileResourceServiceImpl.RESOURCE_BUNDLE, "error.readOnly", mount.getName());
		}
		file.setDefaultMount(mount);
		file.setWritable(mount!=null);
		virtualRepository.saveFile(file);
	}
	
}