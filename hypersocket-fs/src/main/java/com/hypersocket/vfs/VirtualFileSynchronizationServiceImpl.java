package com.hypersocket.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hypersocket.auth.AbstractAuthenticatedServiceImpl;
import com.hypersocket.auth.InvalidAuthenticationContext;
import com.hypersocket.events.EventService;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.fs.FileResourceServiceImpl;
import com.hypersocket.fs.events.FileCreatedEvent;
import com.hypersocket.fs.events.FileDeletedEvent;
import com.hypersocket.fs.events.FileUpdatedEvent;
import com.hypersocket.fs.events.FolderCreatedEvent;
import com.hypersocket.fs.events.FolderDeletedEvent;
import com.hypersocket.fs.events.FolderUpdatedEvent;
import com.hypersocket.properties.ResourceUtils;
import com.hypersocket.realm.Principal;
import com.hypersocket.utils.FileUtils;
import com.hypersocket.vfs.events.FileResourceReconcileCompletedEvent;
import com.hypersocket.vfs.events.FileResourceReconcileStartedEvent;

@Service
public class VirtualFileSynchronizationServiceImpl extends AbstractAuthenticatedServiceImpl implements VirtualFileSynchronizationService {

	static Logger log = LoggerFactory.getLogger(VirtualFileSynchronizationServiceImpl.class);
	
	@Autowired
	VirtualFileRepository repository;
	
	@Autowired
	VirtualFileService resourceService; 
	
	@Autowired
	EventService eventService; 
	
	@Autowired
	FileResourceService fileService; 
	
	@PostConstruct
	private void postConstruct() {
		
		
		eventService.registerEvent(FileCreatedEvent.class, FileResourceServiceImpl.RESOURCE_BUNDLE);
		eventService.registerEvent(FileUpdatedEvent.class, FileResourceServiceImpl.RESOURCE_BUNDLE);
		eventService.registerEvent(FileDeletedEvent.class, FileResourceServiceImpl.RESOURCE_BUNDLE);
		
		eventService.registerEvent(FolderCreatedEvent.class, FileResourceServiceImpl.RESOURCE_BUNDLE);
		eventService.registerEvent(FolderUpdatedEvent.class, FileResourceServiceImpl.RESOURCE_BUNDLE);
		eventService.registerEvent(FolderDeletedEvent.class, FileResourceServiceImpl.RESOURCE_BUNDLE);		
		
	}
	@Override
	public void reconcileFolder(ReconcileStatistics stats,
			FileObject fileObject, 
			FileResource resource, 
			VirtualFile folder,
			boolean conflicted,
			int depth,
			Principal principal) throws IOException {
		
		/**
		 * Return if we have reached the maximum depth (reverse logic 0 is max, 999 is low
		 */
		if(depth==0) {
			return;
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Reconciling folder " + folder.getVirtualPath());
		}
		
		String displayName = conflicted ? 
				String.format("%s (%s)", fileObject.getName().getBaseName(), resource.getName())
				: fileObject.getName().getBaseName();

		if (!FileUtils.checkEndsWithNoSlash(resource.getVirtualPath())
				.equals(FileUtils.checkEndsWithNoSlash(folder.getVirtualPath()))) {
			if (isReconciledFolder(resource, fileObject)) {
				if(hasChanged(displayName, fileObject, resource, folder)) {
					folder = repository.reconcileFolder(displayName, folder, fileObject, resource, conflicted, principal);
					stats.foldersUpdated++;	
					if(stats.generateChangeEvents) {
						fireUpdateEvent(folder);
					}
				}
			}
		}

		Map<String, List<VirtualFile>> reconciledChildren = new HashMap<String, List<VirtualFile>>();
		for (VirtualFile virtual : repository.getReconciledFiles(folder, getCurrentRealm(), principal)) {
			if (!reconciledChildren.containsKey(virtual.getFilename())) {
				reconciledChildren.put(virtual.getFilename(), new ArrayList<VirtualFile>());
			}
			reconciledChildren.get(virtual.getFilename()).add(virtual);
		}

		List<VirtualFile> toDeleteList = new ArrayList<VirtualFile>();

		try {
			for (FileObject obj : fileObject.getChildren()) {

				try {
					String filename = obj.getName().getBaseName();
					
					if(!checkResourceFilter(filename, resource)) {
						continue;
					}
					
					
					String childDisplayName = filename;
					boolean reconciled = false;
					boolean childConflicted = false;
					if (reconciledChildren.containsKey(filename)) {
						List<VirtualFile> virtualFiles = reconciledChildren.remove(filename);
						if (isConflicted(virtualFiles, resource)) {
							childConflicted = true;
							stats.conflictedPaths.add(folder.getVirtualPath() + filename);
						}

						for (VirtualFile virtual : virtualFiles) {
							if (virtual.getMount().equals(resource)) {
								if (obj.getType() == FileType.FOLDER || obj.getType() == FileType.FILE_OR_FOLDER) {
									if (isReconciledFolder(resource, obj)) {
										reconcileFolder(stats, obj, resource, virtual, childConflicted, depth - 1, principal);
									} else {
										toDeleteList.add(virtual);
									}
									reconciled = true;
								} else {
									if (isReconciledFile(resource, obj)) {
										if (hasChanged(childDisplayName, obj, resource, virtual)) {
											reconcileFile(stats, obj, resource, virtual, folder, childConflicted, principal);
										}
									} else {
										toDeleteList.add(virtual);
									}
									reconciled = true;
								}
							}
						}
					}

					if (reconciled) {
						continue;
					}
					
					if (obj.getType() == FileType.FOLDER || obj.getType() == FileType.FILE_OR_FOLDER) {
						if (isReconciledFolder(resource, obj)) {
							VirtualFile childFolder = repository.getVirtualFileByResource(
									FileUtils.checkEndsWithSlash(folder.getVirtualPath()) + obj.getName().getBaseName(),
									getCurrentRealm(), principal, resource);
							if (childFolder == null) {
								childFolder = repository.reconcileNewFolder(childDisplayName, folder, obj, resource,
										childConflicted, principal);
								stats.foldersCreated++;
								if(stats.generateChangeEvents) {
									fireCreatedEvent(childFolder);
								}
							}
							reconcileFolder(stats, obj, resource, childFolder, childConflicted, depth - 1, principal);
						}
					} else if (isReconciledFile(resource, obj)) {
						reconcileFile(stats, obj, resource, null, folder, childConflicted, principal);
					}

				} catch (FileSystemException e) {
					log.error("Failed to reconcile file", e);
					stats.errors++;
				}
			}

			reconciledChildren.put("!!", toDeleteList);

			for (List<VirtualFile> deleteList : reconciledChildren.values()) {
				for (VirtualFile toDelete : deleteList) {
					if (!toDelete.isMounted() || !toDelete.getMount().equals(resource)) {
						continue;
					}
					if (toDelete.isFolder()) {
						stats.filesDeleted += repository.removeReconciledFolder(toDelete);
						stats.foldersDeleted++;
					} else {
						repository.removeReconciledFile(toDelete);
						stats.filesDeleted++;
					}
					
					fireDeletedEvent(toDelete);
				}
			}

		} catch (FileSystemException e) {
			log.error("Failed to reconcile folder", e);
			stats.errors++;
		} 
	}
	
	private boolean checkResourceFilter(String filename, FileResource resource) {
		
		String[] includeFilters = ResourceUtils.explodeValues(fileService.getResourceProperty(resource,"fs.includeFilter"));
		String[] excludeFilters = ResourceUtils.explodeValues(fileService.getResourceProperty(resource,"fs.excludeFilter"));
		
		boolean included = includeFilters.length==0;
		
		if(!included) {
			for(String include : includeFilters) {
				if(filename.matches(include)) {
					included = true;
					break;
				}
				if(filename.endsWith(include)) {
					included = true;
					break;
				}
			}
		}
		
		if(included && excludeFilters.length > 0) {
			for(String exclude : excludeFilters) {
				if(filename.matches(exclude)) {
					included = false;
					break;
				}
				if(filename.endsWith(exclude)) {
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
			return true;
		} catch (FileSystemException e) {
			return true;
		}
	}

	private boolean isReconciledFolder(FileResource resource, FileObject obj) {
		if (!resource.isShowFolders()) {
			return false;
		}
		// Check for hidden folder
		return isReconciledFile(resource, obj);
	}

	/**
	 * Determine if file or folder conflicts with others. The original file that
	 * was not conflicted wins and will be treated as not being in conflict.
	 * Only subsequent files/folders.
	 * 
	 * @param files
	 * @param resource
	 * @return
	 */
	private boolean isConflicted(List<VirtualFile> files, FileResource resource) {
		boolean conflicted = files.size() > 0;
		for (VirtualFile file : files) {
			if (file.getMount().equals(resource)) {
				conflicted = files.size() > 1 && file.getConflicted();
			}
		}
		return conflicted;
	}
	
	@Override
	public VirtualFile reconcileFile(ReconcileStatistics stats, FileObject obj, FileResource resource, VirtualFile virtual,
			VirtualFile parent, 
			boolean conflicted,
			Principal principal) throws IOException {
		
		String displayName = conflicted ? 
				String.format("%s (%s)", obj.getName().getBaseName(), resource.getName())
				: obj.getName().getBaseName();
				
		if (virtual == null) {
			if (log.isDebugEnabled()) {
				log.debug("Creating file " + parent.getVirtualPath() + obj.getName().getBaseName());
			}
			if(obj.getType()==FileType.FILE) {
				virtual = repository.reconcileFile(displayName, obj, resource, parent, principal);
				stats.filesCreated++;
			} else if(obj.getType()==FileType.FOLDER) {
				virtual = repository.reconcileNewFolder(displayName, parent, obj, resource, conflicted, principal);
				stats.foldersCreated++;
			}
			
			if(stats.generateChangeEvents) {
				fireCreatedEvent(virtual);
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Updating file " + parent.getVirtualPath() + obj.getName().getBaseName());
			}
			
			if(obj.getType()==FileType.FILE) {
				virtual = repository.reconcileFile(displayName, obj, resource, virtual, parent, principal);
				stats.filesUpdated++;
			} else if(obj.getType()==FileType.FOLDER) {
				virtual = repository.reconcileFolder(displayName, virtual, obj, resource, conflicted, principal);
				stats.foldersUpdated++;
			}
			
			if(stats.generateChangeEvents) {
				fireUpdateEvent(virtual);
			}
		}
		
		return virtual;
	}
	
	private void fireUpdateEvent(VirtualFile virtual) throws InvalidAuthenticationContext, IOException {
		
		switch(virtual.getType()) {
		case FOLDER:
			eventService.publishEvent(new FolderUpdatedEvent(this, getCurrentSession(), 
					virtual.getMount(), 
					FileUtils.stripParentPath(virtual.getMount().getVirtualPath(), virtual.getVirtualPath()), 
					"SYSTEM"));
			break;
		case FILE:
			eventService.publishEvent(new FileUpdatedEvent(this, getCurrentSession(), 
					virtual.getMount(), 
					FileUtils.stripParentPath(virtual.getMount().getVirtualPath(), virtual.getVirtualPath()), 
					"SYSTEM"));
			break;
		default:
			// Ignore?
			break;
		}
		
	}

	private void fireCreatedEvent(VirtualFile virtual) throws InvalidAuthenticationContext, IOException {
		switch(virtual.getType()) {
		case FOLDER:
			eventService.publishEvent(new FolderCreatedEvent(this, getCurrentSession(), 
					virtual.getMount(), 
					FileUtils.stripParentPath(virtual.getMount().getVirtualPath(), virtual.getVirtualPath()), 
					"SYSTEM"));
			break;
		case FILE:
			eventService.publishEvent(new FileCreatedEvent(this, getCurrentSession(), 
					virtual.getMount(), 
					FileUtils.stripParentPath(virtual.getMount().getVirtualPath(), virtual.getVirtualPath()), 
					"SYSTEM"));
			break;
		default:
			// Ignore?
			break;
		}
	}
	
	private void fireDeletedEvent(VirtualFile virtual) throws InvalidAuthenticationContext, IOException {
		switch(virtual.getType()) {
		case FOLDER:
			eventService.publishEvent(new FolderDeletedEvent(this, getCurrentSession(), 
					virtual.getMount(), 
					FileUtils.stripParentPath(virtual.getMount().getVirtualPath(), virtual.getVirtualPath()), 
					"SYSTEM"));
			break;
		case FILE:
			eventService.publishEvent(new FileDeletedEvent(this, getCurrentSession(), 
					virtual.getMount(), 
					FileUtils.stripParentPath(virtual.getMount().getVirtualPath(), virtual.getVirtualPath()), 
					"SYSTEM"));
			break;
		default:
			// Ignore?
			break;
		}
	}

	public void removeFileResource(FileResource resource) {
		repository.removeFileResource(resource);
	}

	private boolean hasChanged(String displayName, FileObject obj, FileResource resource, VirtualFile virtual)
			throws FileSystemException {
		return virtual.getHash() != VirtualFileUtils.generateHash(obj.getName().getBaseName(), virtual.getVirtualPath(),
				virtual.getType().ordinal(), obj.getType().hasAttributes() ? obj.getContent().getLastModifiedTime() : 0L,
				virtual.getType() == VirtualFileType.FILE ? obj.getContent().getSize() : 0L, !resource.isReadOnly(),
				!displayName.equals(obj.getName().getBaseName()));
	}

	@Override
	public void reconcileTopFolder(FileResource resource, int depth, boolean makeDefault, Principal principal) throws IOException {
		
		ReconcileStatistics stats = new ReconcileStatistics();
		stats.generateChangeEvents = fileService.getResourceBooleanProperty(resource, "fs.generateChangeEventsOnRebuild");
		
		VirtualFile parentFile = repository.getVirtualFile(resource.getVirtualPath(), getCurrentRealm(), principal);

		Hibernate.initialize(parentFile);
		
		if(!parentFile.getFolderMounts().contains(resource)) {
			parentFile.getFolderMounts().add(resource);
			repository.saveFile(parentFile);
		}
		
		if(makeDefault) {
			parentFile.setDefaultMount(resource);
			parentFile.setWritable(true);
			repository.saveFile(parentFile);
		}
		
		if(isUserFilesystem(resource)) {
			return;
		}
		
		FileObject fileObject = resourceService.getFileObject(resource);
		
		if(!fileObject.exists()) {
			throw new FileNotFoundException(String.format("%s does not exist", resource.getUrl()));
		}
		
		repository.clearFileResource(resource);
		
		try {
		
		switch(fileObject.getType()) {
		case FOLDER:
			reconcileFolder(stats, fileObject, resource, parentFile, false, depth, null);
			break;
		default:
			// We don't handle others
		}
		
		} catch(IOException ex) {
			log.error(String.format("Failed to reconcile %s", resource.getName()), ex);
			throw ex;
		}
	}
	
	@Override
	public boolean canSynchronize(FileResource resource) {
		
		if(!fileService.getResourceBooleanProperty(resource, "fs.reconcileEnabled")) {
			return false;
		}
		
		if(!fileService.getScheme(resource.getScheme()).isSupportsCredentials()) {
			return true;
		}
		
		return !isUserFilesystem(resource);
	}
	
	@Override
	public boolean isUserFilesystem(FileResource resource) {
		
		if(StringUtils.isNotBlank(resource.getUsername()) && StringUtils.isNotBlank(resource.getPassword())) {
			/**
			 * We have filled in credentials, but are they variables?
			 */
			return !(!ResourceUtils.containsReplacementVariable(resource.getUsername()) 
					&& !ResourceUtils.containsReplacementVariable(resource.getPassword())
					&& !ResourceUtils.containsReplacementVariable(resource.getPath()));
		}
		
		/**
		 * We consider no credentials also valid
		 */
		return !(StringUtils.isBlank(resource.getUsername()) 
				&& StringUtils.isBlank(resource.getPassword()) 
				&& !ResourceUtils.containsReplacementVariable(resource.getPath()));
	}

	@Override
	public void synchronize(String virtualPath, Principal principal, boolean throwError, FileResource... resources) throws IOException {
		
		ReconcileStatistics stats = new ReconcileStatistics();
		for(FileResource resource : resources) {
			
				if(FileUtils.checkEndsWithSlash(virtualPath).startsWith(FileUtils.checkEndsWithSlash(resource.getVirtualPath()))) {
					String childPath = FileUtils.stripParentPath(resource.getVirtualPath(), virtualPath);
					
					try {
						FileObject resourceFile = resourceService.getFileObject(resource);
						FileObject fileObject = resourceFile;
						if(StringUtils.isNotBlank(childPath)) {
							fileObject = resourceFile.resolveFile(childPath);
							if(!fileObject.exists()) {
								log.warn(String.format("Cannot synchronize %s/%s because it does not exist", resource.getName(), childPath));
								continue;
							}
						}
	
						if(!fileObject.exists() && throwError) {
							throw new FileNotFoundException(String.format("%s is not a valid path [%s]", virtualPath, fileObject.getPublicURIString()));
						} else if(!fileObject.exists()) {
							continue;
						}
						
						String parentPath = virtualPath; 
						VirtualFile parentFile;
						
						List<String> parentPaths = new ArrayList<String>();
						do {
							parentPath = FileUtils.stripLastPathElement(parentPath);
							parentFile = repository.getVirtualFile(parentPath, getCurrentRealm(), principal);
							if(parentFile==null) {
								parentPaths.add(0, parentPath);
							}
						} while(parentFile==null);
						
						for(String parent : parentPaths) {
							String path = FileUtils.stripParentPath(resource.getVirtualPath(), parent);
							parentFile = reconcileFile(stats, resourceFile.resolveFile(path), resource, null, parentFile, false, principal);
						}
	
						VirtualFile file = repository.getVirtualFile(virtualPath, getCurrentRealm(), principal);

						switch(fileObject.getType()) {
						case FOLDER:
							if(file==null) {
								file = repository.reconcileNewFolder(fileObject.getName().getBaseName(), parentFile, fileObject, resource, false, principal);
							}
							reconcileFolder(stats, fileObject, resource, file, false, 1, resourceService.getOwnerPrincipal(resource));
							file.setSync(true);
							repository.saveFile(file); // Ensure modified date gets updated.
							break;
						case FILE:
							if(file==null) {
								file = repository.reconcileFile(fileObject.getName().getBaseName(), fileObject, resource, parentFile, principal);
							}
							if(file.isFolder()) {
								parentFile = file;
								file = repository.getVirtualFile(FileUtils.checkEndsWithSlash(virtualPath) + fileObject.getName().getBaseName(), getCurrentRealm(), principal);
							}
							file = reconcileFile(stats, fileObject, resource, file, parentFile, false, resourceService.getOwnerPrincipal(resource));
							file.setSync(true);
							repository.saveFile(file); // Ensure modified date gets updated.
							break;
						default:
							if(log.isDebugEnabled()) {
								log.debug(String.format("Unhandled file type %s for path %s/%s",fileObject.getType().getName(), resource.getUrl(), childPath));
							}
							break;
						}
					} catch(IOException e) {
						if(throwError) {
							throw e;
						}
					}
				}
		}
		repository.flush();
	}
}
