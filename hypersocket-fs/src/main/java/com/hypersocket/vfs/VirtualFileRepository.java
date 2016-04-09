package com.hypersocket.vfs;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import com.hypersocket.fs.FileResource;
import com.hypersocket.realm.Realm;
import com.hypersocket.repository.AbstractRepository;
import com.hypersocket.tables.ColumnSort;

public interface VirtualFileRepository extends AbstractRepository<Long> {

	Map<String, VirtualFile> reconcileParents(FileResource resource);

	VirtualFile getMountFile(FileResource resource);
	
	VirtualFile reconcileParent(FileResource resource);

	void removeReconciledFile(VirtualFile toDelete);

	VirtualFile reconcileFile(FileObject obj, FileResource resource, VirtualFile parent) throws FileSystemException;

	VirtualFile reconcileFile(FileObject obj, FileResource resource, VirtualFile virtual, VirtualFile parent)
			throws FileSystemException;

	VirtualFile reconcileFolder(VirtualFile folder, FileObject fileObject) throws FileSystemException;

	VirtualFile reconcileNewFolder(VirtualFile parent, FileObject fileObject) throws FileSystemException;

	VirtualFile reconcileMount(FileResource resource, FileObject fileObject) throws FileSystemException;

	Collection<VirtualFile> getVirtualFiles(VirtualFile parent, FileResource... resources);

	VirtualFile getVirtualFile(String virtualPath, FileResource... resources);

	VirtualFile getReconciledFile(String virtualPath);

	Collection<VirtualFile> getReconciledFiles(VirtualFile parent);

	Collection<VirtualFile> search(String searchColumn, String search, int start, int length, ColumnSort[] sort, 
			VirtualFile parent,
			FileResource[] resources);

	int removeReconciledFolder(VirtualFile toDelete);

	Collection<VirtualFile> getMounts();

	VirtualFile getRootFolder(Realm realm);

}
