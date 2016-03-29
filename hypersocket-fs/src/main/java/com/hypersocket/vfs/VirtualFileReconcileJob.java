package com.hypersocket.vfs;

import java.io.IOException;
import java.util.HashMap;
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
	
	int numOperations;
	int errors;

	private void reconcileFolder(FileObject fileObject, FileResource resource, VirtualFile folder) throws FileSystemException {
		
		if(log.isDebugEnabled()) {
			log.debug("Reconciling folder " + folder.getVirtualPath());
		}
		
		if(folder.getType()==VirtualFileType.FOLDER) {
			repository.reconcileFolder(folder, fileObject);
		}
		
		Map<String,VirtualFile> reconciledChildren = new HashMap<String,VirtualFile>();
		for(VirtualFile virtual : repository.getReconciledFiles(folder)) {
			reconciledChildren.put(virtual.getFilename(), virtual);
		}
		
		try {
			for(FileObject obj : fileObject.getChildren()) {
				
				try {
					
					String filename = obj.getName().getBaseName();
					if(reconciledChildren.containsKey(filename)) {
						VirtualFile virtual = reconciledChildren.remove(filename);
						
						if(obj.getType()==FileType.FOLDER || obj.getType()==FileType.FILE_OR_FOLDER) {
							reconcileFolder(obj, resource, virtual);
						} else {
							if(hasChanged(obj, resource, virtual)) {
								reconcileFile(obj, resource, virtual, folder);
							}
						}
						
					} else {
						if(obj.getType()==FileType.FOLDER || obj.getType()==FileType.FILE_OR_FOLDER) {
							VirtualFile childFolder = repository.getReconciledFile(
									FileUtils.checkEndsWithSlash(folder.getVirtualPath()) + obj.getName().getBaseName());
							if(childFolder==null) {
								childFolder = repository.reconcileNewFolder(folder, obj);
							}
							reconcileFolder(obj, resource, childFolder);
						} else {
							reconcileFile(obj, resource, null, folder);
						}
					}

				} catch(FileSystemException e) {
					log.error("Failed to reconcile file", e);
					errors++;
				}
			}
			
			for(VirtualFile toDelete : reconciledChildren.values()) {
				if(toDelete.isFolder()) {
					filesDeleted += repository.removeReconciledFolder(toDelete);
				} else {
					repository.removeReconciledFile(toDelete);
					filesDeleted++;
				}
				checkFlush();
			}
			
		} catch(FileSystemException e) {
			log.error("Failed to reconcile folder", e);
			errors++;
		} finally {
			checkFlush();
		}
		
	}
	
	private void checkFlush() {
		numOperations++;
		if(numOperations % 25 == 0) {
			repository.flush();
		}
	}
	
	private void reconcileFile(FileObject obj, FileResource resource, VirtualFile virtual, VirtualFile parent) throws FileSystemException {
		if(virtual==null) {
			if(log.isDebugEnabled()) {
				log.debug("Creating file " + parent.getVirtualPath() + obj.getName().getBaseName());
			}
			repository.reconcileFile(obj, resource, parent);
			filesCreated++;
		} else {
			if(log.isDebugEnabled()) {
				log.debug("Updating file " + parent.getVirtualPath() + obj.getName().getBaseName());
			}
			repository.reconcileFile(obj, resource, virtual, parent);
			filesUpdated++;
		}
		checkFlush();
	}
	
	private boolean hasChanged(FileObject obj, FileResource resource, VirtualFile virtual) throws FileSystemException {
		return virtual.getHash()!=VirtualFileUtils.generateHash(obj.getName().getBaseName(),
				virtual.getVirtualPath(),
				virtual.getType().ordinal(), 
				obj.getContent().getLastModifiedTime(),
				virtual.getType()==VirtualFileType.FILE ? obj.getContent().getSize() : 0L,
				!resource.isReadOnly());
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
			throw new IOException("Reconcile cannot be performed without a virtual path value");
		}
		
		FileObject fileObject = resourceService.getFileObject(resource);
		VirtualFile folder = repository.getMountFile(resource);
		if(folder==null) {
			folder = repository.reconcileMount(resource, fileObject);
		}
		
		reconcileFolder(fileObject, resource, folder);
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
