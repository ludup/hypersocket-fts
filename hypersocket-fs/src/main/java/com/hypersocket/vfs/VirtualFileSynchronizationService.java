package com.hypersocket.vfs;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import com.hypersocket.fs.FileResource;

public interface VirtualFileSynchronizationService {

	void reconcileFile(ReconcileStatistics stats, FileObject fileObject, FileResource resource, VirtualFile virtualFile,
			VirtualFile parent, boolean b) throws FileSystemException;

	void reconcileFolder(ReconcileStatistics stats, FileObject fileObject, FileResource resource, VirtualFile folder,
			boolean conflicted) throws FileSystemException;

	void removeFileResource(FileResource resource);

}
