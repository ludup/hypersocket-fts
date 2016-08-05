package com.hypersocket.vfs;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import com.hypersocket.fs.FileResource;
import com.hypersocket.realm.Principal;
import com.hypersocket.realm.Realm;
import com.hypersocket.repository.AbstractRepository;
import com.hypersocket.tables.ColumnSort;

public interface VirtualFileRepository extends AbstractRepository<Long> {

	Map<String, VirtualFile> reconcileParents(FileResource resource, Principal principal);
	
	VirtualFile reconcileParent(FileResource resource, Principal principal);

	void removeReconciledFile(VirtualFile toDelete);

	VirtualFile reconcileFile(String displayName, FileObject obj, FileResource resource, VirtualFile parent, Principal principal) throws FileSystemException;

	VirtualFile reconcileFile(String displayName, FileObject obj, FileResource resource, VirtualFile virtual, VirtualFile parent, Principal principal)
			throws FileSystemException;

	VirtualFile reconcileFolder(String displayName, VirtualFile folder, FileObject fileObject, FileResource resource, boolean conflicted, Principal principal) throws FileSystemException;

	VirtualFile reconcileNewFolder(String displayName, VirtualFile parent, FileObject fileObject, FileResource resource, boolean conflicted, Principal principal) throws FileSystemException;

	VirtualFile reconcileMount(String displayName, FileResource resource, FileObject fileObject, VirtualFile virtualFile, Principal principal) throws FileSystemException;

	Collection<VirtualFile> getVirtualFiles(VirtualFile parent, Realm realm, Principal principal, FileResource... resources);

	VirtualFile getVirtualFileByResource(String virtualPath, Realm realm, Principal principal, FileResource... resources);

	Collection<VirtualFile> getReconciledFiles(VirtualFile parent, Realm realm, Principal principal);

	Collection<VirtualFile> search(String searchColumn, String search, int start, int length, ColumnSort[] sort, 
			VirtualFile parent,
			Realm realm,
			Principal principal,
			FileResource... resources);

	Collection<VirtualFile> getVirtualFolders(Realm realm);

	VirtualFile getRootFolder(Realm realm);

	VirtualFile getVirtualFile(String virtualPath, Realm realm, Principal principal);

	VirtualFile createVirtualFolder(String displayName, VirtualFile parent);

	VirtualFile renameVirtualFolder(VirtualFile fromFolder, String toFolder);

	VirtualFile getVirtualFileById(Long id);

	void saveFile(VirtualFile file);

	void removeFileResource(FileResource resource);

	int removeReconciledFiles(VirtualFile folder);

	int removeReconciledFolder(VirtualFile toDelete);

	void clearFileResource(FileResource resource);
}
