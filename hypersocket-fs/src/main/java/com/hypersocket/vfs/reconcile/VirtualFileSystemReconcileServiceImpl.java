package com.hypersocket.vfs.reconcile;

import java.util.Collection;

import org.quartz.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hypersocket.events.SystemEvent;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceRepository;
import com.hypersocket.fs.events.FileResourceCreatedEvent;
import com.hypersocket.fs.events.FileResourceDeletedEvent;
import com.hypersocket.fs.events.FileResourceEvent;
import com.hypersocket.fs.events.FileResourceUpdatedEvent;
import com.hypersocket.properties.ResourceTemplateRepository;
import com.hypersocket.realm.events.ResourceEvent;
import com.hypersocket.reconcile.AbstractReconcileServiceImpl;
import com.hypersocket.server.events.ServerStartedEvent;
import com.hypersocket.vfs.VirtualFileReconcileJob;
import com.hypersocket.vfs.events.FileResourceReconcileCompletedEvent;
import com.hypersocket.vfs.events.FileResourceReconcileStartedEvent;

@Service
public class VirtualFileSystemReconcileServiceImpl extends AbstractReconcileServiceImpl<FileResource>
      implements VirtualFileSystemReconcileService {

	@Autowired
	FileResourceRepository repository;
	
	@Override
	protected boolean isTriggerEvent(SystemEvent event) {
		return event instanceof ServerStartedEvent
				|| event instanceof FileResourceEvent;
	}

	@Override
	protected Class<? extends Job> getReconcileJobClass() {
		return VirtualFileReconcileJob.class;
	}

	@Override
	protected int getReconcileSuccessInterval(FileResource resource) {
		return repository.getIntValue(resource, "fs.reconcileSuccessInterval")* 60000;
	}

	@Override
	protected int getReconcileFailureInterval(FileResource resource) {
		return repository.getIntValue(resource, "fs.reconcileFailedInterval") * 60000;
	}

	@Override
	protected Collection<FileResource> getReconcilingResources() {
		return repository.allResources();
	}

	@Override
	protected ResourceTemplateRepository getRepository() {
		return repository;
	}

	@Override
	protected Class<? extends ResourceEvent> getResourceCreatedEventClass() {
		return FileResourceCreatedEvent.class;
	}

	@Override
	protected Class<? extends ResourceEvent> getResourceUpdatedEventClass() {
		return FileResourceUpdatedEvent.class;
	}

	@Override
	protected Class<? extends ResourceEvent> getResourceDeletedEventClass() {
		return FileResourceDeletedEvent.class;
	}

	@Override
	protected FileResource getResourceFromEvent(SystemEvent event) {
		if(event instanceof FileResourceEvent) {
			return (FileResource) ((FileResourceEvent)event).getResource();
		} else if(event instanceof FileResourceReconcileStartedEvent) {
			return ((FileResourceReconcileStartedEvent)event).getResource();
		} else if(event instanceof FileResourceReconcileCompletedEvent) {
			return ((FileResourceReconcileCompletedEvent)event).getResource();
		}
		return null;
	}

	@Override
	protected boolean isReconciledResource(FileResource resource) {
		return true;
	}

}
