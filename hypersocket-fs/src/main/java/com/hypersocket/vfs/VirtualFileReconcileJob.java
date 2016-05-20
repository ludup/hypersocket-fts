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
	
	@Autowired
	VirtualFileSynchronizationService syncService;
	
	FileResourceReconcileStartedEvent started = null;
	ReconcileStatistics stats = null;
	
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

		if (StringUtils.isBlank(resource.getVirtualPath())) {
			throw new IOException("Reconcile cannot be performed without a virtual path value.");
		}

		stats = new ReconcileStatistics();
		
		FileObject fileObject = resourceService.getFileObject(resource);
		VirtualFile parentFile = repository.getVirtualFile(resource.getVirtualPath());

		if (fileObject.exists() && fileObject.getType() == FileType.FILE) {
			
			VirtualFile virtualFile = repository.getVirtualFile(
					FileUtils.checkEndsWithSlash((resource.getVirtualPath())
					+ FileUtils.lastPathElement(resource.getPath())));
			syncService.reconcileFile(stats, fileObject, resource, virtualFile, parentFile, 
					virtualFile!=null && !virtualFile.getMount().equals(resource));
		} else {
			if (!fileObject.exists()) {
				fileObject.createFolder();
			}
			syncService.reconcileFolder(stats, fileObject, resource, parentFile, false);
		}

		repository.flush();

	}

	@Override
	protected void fireReconcileStartedEvent(FileResource resource) {
		eventService.publishEvent(
				started = new FileResourceReconcileStartedEvent(this, true, resource.getRealm(), resource));
	}

	@Override
	protected void fireReconcileCompletedEvent(FileResource resource) {

		eventService.publishEvent(new FileResourceReconcileCompletedEvent(this, true, resource.getRealm(), started,
				stats.filesCreated, 
				stats.filesUpdated, 
				stats.filesDeleted, 
				stats.foldersCreated, 
				stats.foldersUpdated, 
				stats.foldersDeleted));

	}

	@Override
	protected void fireReconcileFailedEvent(FileResource resource, Throwable t) {

		if (started == null) {
			eventService.publishEvent(new FileResourceReconcileStartedEvent(this, t, resource.getRealm()));
		} else {
			eventService.publishEvent(new FileResourceReconcileCompletedEvent(this, t, resource.getRealm()));

		}
	}
}
