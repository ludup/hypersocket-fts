package com.hypersocket.fs;

import java.util.Locale;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.auth.AuthenticatedService;
import com.hypersocket.session.Session;

public class SessionAwareUploadEventProcessor implements UploadEventProcessor {

	Session session;
	Locale locale;
	UploadEventProcessor processor;
	AuthenticatedService authenticatedService;

	public SessionAwareUploadEventProcessor(Session session, Locale locale,
			AuthenticatedService authenticatedService,
			UploadEventProcessor processor) {
		this.session = session;
		this.locale = locale;
		this.authenticatedService = authenticatedService;
		this.processor = processor;
	}

	@Override
	public void uploadCannotStart(String mountName, String childPath,
			Throwable t, String protocol) {
		
		boolean setupContext = !authenticatedService.hasAuthenticatedContext();
		if(setupContext) {
			authenticatedService.setCurrentSession(session, locale);
		}
		try {
			processor.uploadCannotStart(mountName, childPath, t, protocol);
		} finally {
			if(setupContext) {
				authenticatedService.clearPrincipalContext();
			}
		}
	}

	@Override
	public long uploadStarted(FileResource resource, String childPath,
			FileObject file, String protocol) {
		
		boolean setupContext = !authenticatedService.hasAuthenticatedContext();
		if(setupContext) {
			authenticatedService.setCurrentSession(session, locale);
		}

		try {
			return processor.uploadStarted(resource, childPath, file, protocol);
		} finally {
			if(setupContext) {
				authenticatedService.clearPrincipalContext();
			}
		}
	}

	@Override
	public void uploadComplete(FileResource resource, String childPath,
			FileObject file, long bytesIn, long timeMillis, String protocol) {

		boolean setupContext = !authenticatedService.hasAuthenticatedContext();
		if(setupContext) {
			authenticatedService.setCurrentSession(session, locale);
		}

		try {
			processor.uploadComplete(resource, childPath, file, bytesIn,
					timeMillis, protocol);
		} finally {
			if(setupContext) {
				authenticatedService.clearPrincipalContext();
			}
		}

	}

	@Override
	public void uploadFailed(FileResource resource, String childPath,
			FileObject file, long bytesIn, Throwable t, String protocol) {

		boolean setupContext = !authenticatedService.hasAuthenticatedContext();
		if(setupContext) {
			authenticatedService.setCurrentSession(session, locale);
		}

		try {
			processor.uploadFailed(resource, childPath, file, bytesIn, t,
					protocol);
		} finally {
			if(setupContext) {
				authenticatedService.clearPrincipalContext();
			}
		}
	}

}
