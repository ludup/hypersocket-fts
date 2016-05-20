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

	VirtualFile reconcileFile(String displayName, FileObject obj, FileResource resource, VirtualFile parent) throws FileSystemException;

	VirtualFile reconcileFile(String displayName, FileObject obj, FileResource resource, VirtualFile virtual, VirtualFile parent)
			throws FileSystemException;

	VirtualFile reconcileFolder(String displayName, VirtualFile folder, FileObject fileObject, FileResource resource, boolean conflicted) throws FileSystemException;

	VirtualFile reconcileNewFolder(String displayName, VirtualFile parent, FileObject fileObject, FileResource resource, boolean conflicted) throws FileSystemException;

	VirtualFile reconcileMount(String displayName, FileResource resource, FileObject fileObject, VirtualFile virtualFile) throws FileSystemException;

	Collection<VirtualFile> getVirtualFiles(VirtualFile parent, FileResource... resources);

	VirtualFile getVirtualFileByResource(String virtualPath, FileResource... resources);

	Collection<VirtualFile> getReconciledFiles(VirtualFile parent);

	Collection<VirtualFile> search(String searchColumn, String search, int start, int length, ColumnSort[] sort, 
			VirtualFile parent,
			FileResource... resources);

	int removeReconciledFolder(VirtualFile toDelete);

	Collection<VirtualFile> getVirtualFolders();

	VirtualFile getRootFolder(Realm realm);

	VirtualFile getVirtualFile(String virtualPath);

	VirtualFile createVirtualFolder(String displayName, VirtualFile parent);

	VirtualFile renameVirtualFolder(VirtualFile fromFolder, String toFolder);

	VirtualFile getVirtualFileById(Long id);

	void saveFile(VirtualFile file);

}
