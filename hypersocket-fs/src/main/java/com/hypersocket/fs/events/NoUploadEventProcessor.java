package com.hypersocket.fs.events;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.UploadEventProcessor;
import com.hypersocket.session.Session;

public class NoUploadEventProcessor implements UploadEventProcessor {

		Session session;
		public NoUploadEventProcessor(Session session) {
			this.session = session;
		}
		
		@Override
		public void uploadCannotStart(String virtualPath, Throwable t, String protocol) {
		}

		@Override
		public UploadStartedEvent uploadStarted(FileResource resource, String virtualPath, FileObject file,
				String protocol) {
			return new UploadStartedEvent(this, session, resource, virtualPath, file, protocol);
		}

		@Override
		public void uploadComplete(FileResource resource, String virtualPath, FileObject file, long bytesIn,
				long timeMillis, String protocol) {
		}

		@Override
		public void uploadFailed(FileResource resource, String virtualPath, FileObject file, long bytesIn, Throwable t,
				String protocol) {
		}
		
	}