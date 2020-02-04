package com.hypersocket.fs;

import java.util.Locale;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.auth.AuthenticatedService;
import com.hypersocket.fs.events.UploadStartedEvent;
import com.hypersocket.session.Session;

public class SessionAwareUploadEventProcessor implements UploadEventProcessor {

	private Session session;
	private Locale locale;
	private UploadEventProcessor processor;
	private AuthenticatedService authenticatedService;

	public SessionAwareUploadEventProcessor(Session session, Locale locale,
			AuthenticatedService authenticatedService,
			UploadEventProcessor processor) {
		this.session = session;
		this.locale = locale;
		this.authenticatedService = authenticatedService;
		this.processor = processor;
	}

	@Override
	public void uploadCannotStart(String virtualPath,
			Throwable t, String protocol) {
		
		boolean setupContext = !authenticatedService.hasAuthenticatedContext();
		if(setupContext) {
			authenticatedService.setCurrentSession(session, locale);
		}
		try {
			processor.uploadCannotStart(virtualPath, t, protocol);
		} finally {
			if(setupContext) {
				authenticatedService.clearPrincipalContext();
			}
		}
	}

	@Override
	public UploadStartedEvent uploadStarted(FileResource resource, String virtualPath,
			FileObject file, String protocol) {
		
		boolean setupContext = !authenticatedService.hasAuthenticatedContext();
		if(setupContext) {
			authenticatedService.setCurrentSession(session, locale);
		}

		try {
			return processor.uploadStarted(resource, virtualPath, file, protocol);
		} finally {
			if(setupContext) {
				authenticatedService.clearPrincipalContext();
			}
		}
	}

	@Override
	public void uploadComplete(FileResource resource, String virtualPath,
			FileObject file, long bytesIn, long timeMillis, String protocol) {

		boolean setupContext = !authenticatedService.hasAuthenticatedContext();
		if(setupContext) {
			authenticatedService.setCurrentSession(session, locale);
		}

		try {
			processor.uploadComplete(resource, virtualPath, file, bytesIn,
					timeMillis, protocol);
		} finally {
			if(setupContext) {
				authenticatedService.clearPrincipalContext();
			}
		}

	}

	@Override
	public void uploadFailed(FileResource resource, String virtualPath,
			FileObject file, long bytesIn, Throwable t, String protocol) {

		boolean setupContext = !authenticatedService.hasAuthenticatedContext();
		if(setupContext) {
			authenticatedService.setCurrentSession(session, locale);
		}

		try {
			processor.uploadFailed(resource, virtualPath, file, bytesIn, t,
					protocol);
		} finally {
			if(setupContext) {
				authenticatedService.clearPrincipalContext();
			}
		}
	}

}
