package com.hypersocket.vfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hypersocket.events.EventService;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.reconcile.AbstractReconcileJob;
import com.hypersocket.reconcile.AbstractReconcileService;
import com.hypersocket.resource.ResourceNotFoundException;
import com.hypersocket.utils.FileUtils;
import com.hypersocket.vfs.events.FileResourceReconcileCompletedEvent;
import com.hypersocket.vfs.events.FileResourceReconcileStartedEvent;
import com.hypersocket.vfs.reconcile.VirtualFileSystemReconcileService;

public class VirtualFileReconcileJob extends AbstractReconcileJob<FileResource> {

	static Logger log = LoggerFactory.getLogger(VirtualFileReconcileJob.class);
	
	@Autowired
	FileResourceService fileService; 
	
	@Autowired
	VirtualFileService resourceService; 
	
	@Autowired
	VirtualFileRepository repository; 
	
	@Autowired
	VirtualFileSystemReconcileService reconcileService; 
	
	@Autowired
	EventService eventService; 
	
	FileResourceReconcileStartedEvent started = null;
	
	int filesUpdated;
	int filesCreated;
	int filesDeleted;
	int foldersCreated;
	int foldersUpdated;
	int foldersDeleted;
	
	List<String> conflictedPaths = new ArrayList<String>();
	
	int numOperations;
	int errors;

	private void reconcileFolder(String displayName, FileObject fileObject, FileResource resource, VirtualFile folder, boolean conflicted) throws FileSystemException {
		
		if(log.isDebugEnabled()) {
			log.debug("Reconciling folder " + folder.getVirtualPath());
		}
		
		if(folder.getType()==VirtualFileType.FOLDER) {
			repository.reconcileFolder(displayName, folder, fileObject, resource, conflicted);
		}
		
		Map<String,List<VirtualFile>> reconciledChildren = new HashMap<String,List<VirtualFile>>();
		for(VirtualFile virtual : repository.getReconciledFiles(folder)) {
			if(!reconciledChildren.containsKey(virtual.getFilename())) {
				reconciledChildren.put(virtual.getFilename(), new ArrayList<VirtualFile>());
			}
			reconciledChildren.get(virtual.getFilename()).add(virtual);
		}
		
		try {
			for(FileObject obj : fileObject.getChildren()) {
				
				try {
					String filename = obj.getName().getBaseName();
					String childDisplayName = filename;
					boolean reconciled = false;
					boolean childConflicted = false;
					if(reconciledChildren.containsKey(filename)) {
						List<VirtualFile> virtualFiles = reconciledChildren.remove(filename);
						if(isConflicted(virtualFiles, resource)) {
							childDisplayName = String.format("%s (%s)", filename, resource.getName());
							childConflicted = true;
							conflictedPaths.add(folder.getVirtualPath() + filename);
						}
						
						for(VirtualFile virtual : virtualFiles) {
							if(virtual.getMount().equals(resource)) {
								if(obj.getType()==FileType.FOLDER || obj.getType()==FileType.FILE_OR_FOLDER) {
									reconcileFolder(childDisplayName, obj, resource, virtual, childConflicted);
									reconciled = true;
								} else {
									if(hasChanged(childDisplayName, obj, resource, virtual)) {
										reconcileFile(childDisplayName, obj, resource, virtual, folder);
									}
									reconciled = true;
								}
							} 
						} 
					} 
					
					if(reconciled) {
						continue;
					}
					if(obj.getType()==FileType.FOLDER || obj.getType()==FileType.FILE_OR_FOLDER) {
						VirtualFile childFolder = repository.getVirtualFileByResource(
								FileUtils.checkEndsWithSlash(folder.getVirtualPath()) + obj.getName().getBaseName(),
								resource);
						if(childFolder==null) {
							childFolder = repository.reconcileNewFolder(childDisplayName, folder, obj, resource, childConflicted);
							foldersCreated++;
						}
						reconcileFolder(childDisplayName, obj, resource, childFolder, childConflicted);
					} else {
						reconcileFile(childDisplayName, obj, resource, null, folder);
					}
					
					

				} catch(FileSystemException e) {
					log.error("Failed to reconcile file", e);
					errors++;
				}
			}
			
			for(List<VirtualFile> toDeleteList : reconciledChildren.values()) {
				for(VirtualFile toDelete : toDeleteList) {
					if(!toDelete.isMounted() || !toDelete.getMount().equals(resource)) {
						continue;
					}
					if(toDelete.isFolder()) {
						filesDeleted += repository.removeReconciledFolder(toDelete);
						foldersDeleted++;
					} else {
						repository.removeReconciledFile(toDelete);
						filesDeleted++;
					}
					checkFlush();
				}
			}
			
		} catch(FileSystemException e) {
			log.error("Failed to reconcile folder", e);
			errors++;
		} finally {
			checkFlush();
		}
	}
	
	/**
	 * Determine if file or folder conflicts with others. The original file that was not conflicted
	 * wins and will be treated as not being in conflict. Only subsequent files/folders.
	 * @param files
	 * @param resource
	 * @return
	 */
	private boolean isConflicted(List<VirtualFile> files, FileResource resource) {
		boolean conflicted = files.size() > 0;
		for(VirtualFile file : files) {
			if(file.getMount().equals(resource)) {
				conflicted = files.size() > 1 && file.getConflicted();
			}
		}
		return conflicted;
	}
	
	private void checkFlush() {
		numOperations++;
		if(numOperations % 25 == 0) {
			repository.flush();
		}
	}
	
	private void reconcileFile(String displayName, FileObject obj, FileResource resource, VirtualFile virtual, VirtualFile parent) throws FileSystemException {
		if(virtual==null) {
			if(log.isDebugEnabled()) {
				log.debug("Creating file " + parent.getVirtualPath() + obj.getName().getBaseName());
			}
			repository.reconcileFile(displayName, obj, resource, parent);
			filesCreated++;
		} else {
			if(log.isDebugEnabled()) {
				log.debug("Updating file " + parent.getVirtualPath() + obj.getName().getBaseName());
			}
			repository.reconcileFile(displayName, obj, resource, virtual, parent);
			filesUpdated++;
		}
		checkFlush();
	}
	
	private boolean hasChanged(String displayName, FileObject obj, FileResource resource, VirtualFile virtual) throws FileSystemException {
		return virtual.getHash()!=VirtualFileUtils.generateHash(obj.getName().getBaseName(),
				virtual.getVirtualPath(),
				virtual.getType().ordinal(), 
				obj.getContent().getLastModifiedTime(),
				virtual.getType()==VirtualFileType.FILE ? obj.getContent().getSize() : 0L,
				!resource.isReadOnly(),
				!displayName.equals(obj.getName().getBaseName()));
	}

	@Override
	protected AbstractReconcileService<FileResource> getReconcileService() {
		return reconcileService;
	}

	@Override
	protected FileResource getResource(Long id) throws ResourceNotFoundException, AccessDeniedException {
		return fileService.getResourceById(id);
	}

	@Override
	protected void doReconcile(FileResource resource) throws Exception {

		if(StringUtils.isBlank(resource.getVirtualPath())) {
			throw new IOException("Reconcile cannot be performed without a virtual path value.");
		}
		
		FileObject fileObject = resourceService.getFileObject(resource);
		VirtualFile existingPath = repository.getVirtualFile(resource.getVirtualPath());
		VirtualFile virtualFile = repository.getMountFile(resource);
		virtualFile = repository.reconcileMount(
				FileUtils.lastPathElement(resource.getVirtualPath()), 
					resource, fileObject, virtualFile);
		
		String displayName = fileObject.getName().getBaseName();
		if(existingPath!=null) {
			displayName = String.format("%s (%s)", displayName, resource.getName());
		}
		switch(virtualFile.getType()) {
		case MOUNTED_FOLDER:
		case ROOT:
			reconcileFolder(displayName, fileObject, resource, virtualFile, existingPath!=null);
			break;
		default:
			reconcileFile(displayName, fileObject, resource, virtualFile, virtualFile.getParent());
			break;
		}

		repository.flush();

	}

	@Override
	protected void fireReconcileStartedEvent(FileResource resource) {
		
		eventService.publishEvent(started = new FileResourceReconcileStartedEvent(this, true, resource.getRealm(), resource));
		
	}

	@Override
	protected void fireReconcileCompletedEvent(FileResource resource) {
		
		eventService.publishEvent(new FileResourceReconcileCompletedEvent(
				this, 
				true, 
				resource.getRealm(), 
				started,
				filesCreated, 
				filesUpdated, 
				filesDeleted));
		
	}

	@Override
	protected void fireReconcileFailedEvent(FileResource resource, Throwable t) {
		
		if(started==null) {
			eventService.publishEvent(new FileResourceReconcileStartedEvent(this, t, resource.getRealm()));
		} else {
			eventService.publishEvent(new FileResourceReconcileCompletedEvent(this, t, resource.getRealm()));
			
		}
	}
}
