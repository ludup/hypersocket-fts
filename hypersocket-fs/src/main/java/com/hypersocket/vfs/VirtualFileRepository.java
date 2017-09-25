package com.hypersocket.vfs;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import com.hypersocket.fs.FileResource;
import com.hypersocket.realm.Principal;
import com.hypersocket.realm.Realm;
import com.hypersocket.repository.AbstractRepository;
import com.hypersocket.tables.ColumnSort;

public interface VirtualFileRepository extends AbstractRepository<Long> {

	void removeReconciledFile(VirtualFile toDelete);

	VirtualFile reconcileFile(String displayName, FileObject obj, FileResource resource, VirtualFile parent, Principal principal) throws FileSystemException, IOException;

	VirtualFile reconcileFile(String displayName, FileObject obj, FileResource resource, VirtualFile virtual, VirtualFile parent, Principal principal)
			throws FileSystemException, IOException;

	VirtualFile reconcileFolder(String displayName, VirtualFile folder, FileObject fileObject, FileResource resource, boolean conflicted, Principal principal) throws FileSystemException, IOException;

	VirtualFile reconcileNewFolder(String displayName, VirtualFile parent, FileObject fileObject, FileResource resource, boolean conflicted, Principal principal) throws FileSystemException, IOException;

	VirtualFile getVirtualFileByResource(String virtualPath, Realm realm, Principal principal, FileResource... resources);

	Collection<VirtualFile> getReconciledFiles(VirtualFile parent, Realm realm, Principal principal);

	Collection<VirtualFile> search(String searchColumn, String search, int start, int length, ColumnSort[] sort, 
			VirtualFile parent,
			Realm realm,
			Principal principal,
			FileResource... resources);

	Collection<VirtualFile> getVirtualFolders(Realm realm);

	VirtualFile getRootFolder(Realm realm) throws IOException;

	VirtualFile getVirtualFile(String virtualPath, Realm realm, Principal principal);

	VirtualFile createVirtualFolder(String displayName, VirtualFile parent) throws IOException;

	VirtualFile renameVirtualFolder(VirtualFile fromFolder, String toFolder) throws IOException;

	VirtualFile getVirtualFileById(Long id);

	void saveFile(VirtualFile file);

	void removeFileResource(FileResource resource);

	void clearFileResource(FileResource resource);
	
	Collection<VirtualFile> getVirtualFiles(VirtualFile parent, Realm realm, Principal principal);

	Collection<VirtualFile> getVirtualFilesByResource(VirtualFile parent, Realm realm, Principal principal,
			FileResource... resources);

	void addFileResource(VirtualFile mountedFile, FileResource resource);

	void deleteVirtualFolder(VirtualFile file);

	void deleteRealm(Realm realm);
}
