package com.hypersocket.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.auth.AuthenticatedService;
import com.hypersocket.fs.FileResource;
import com.hypersocket.realm.Principal;

public interface VirtualFileSynchronizationService extends AuthenticatedService {

	VirtualFile reconcileFile(ReconcileStatistics stats, FileObject fileObject, FileResource resource, VirtualFile virtualFile,
			VirtualFile parent, boolean conflicted,
			Principal principal) throws IOException;

	void reconcileFolder(ReconcileStatistics stats, FileObject fileObject, FileResource resource, VirtualFile folder,
			boolean conflicted, int recurseDepth,
			Principal principal) throws IOException;

	void removeFileResource(FileResource resource);

	void reconcileTopFolder(FileResource resource, int depth, boolean makeDefault, Principal principal) throws IOException;

	boolean canSynchronize(FileResource resource);

	void synchronize(String virtualPath, Principal principal, FileResource... fileResources) throws FileNotFoundException;

	boolean isUserFilesystem(FileResource resource);

}
