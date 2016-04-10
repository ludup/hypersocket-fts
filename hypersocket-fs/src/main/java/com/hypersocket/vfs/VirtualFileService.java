package com.hypersocket.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.auth.AuthenticatedService;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.UploadProcessor;
import com.hypersocket.json.ResourceList;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.realm.UserVariableReplacement;
import com.hypersocket.tables.ColumnSort;
import com.hypersocket.upload.FileUpload;
import com.hypersocket.vfs.json.HttpDownloadProcessor;

public interface VirtualFileService extends AuthenticatedService {

	VirtualFile getFile(String virtualPath) throws FileNotFoundException, AccessDeniedException;

	Collection<VirtualFile> getChildren(String virtualPath) throws FileNotFoundException, AccessDeniedException;

	Collection<VirtualFile> getChildren(VirtualFile folder) throws AccessDeniedException;

	Boolean deleteFile(String virtualPath, String proto) throws IOException, AccessDeniedException;

	VirtualFile createFolder(String virtualPath, String proto) throws IOException, AccessDeniedException;

	VirtualFile renameFile(String virtualPath, String toUri, String proto) throws IOException, AccessDeniedException;

	FileUpload uploadFile(String virtualPath, InputStream in, UploadProcessor<?> processor, String proto) throws AccessDeniedException, IOException;

	Collection<VirtualFile> listChildren(String virtualPath, String proto) throws FileNotFoundException, AccessDeniedException;

	long getSearchCount(String virtualPath, String searchColumn, String search) throws AccessDeniedException;

	Collection<VirtualFile> searchFiles(String virtualPath, String searchColumn, String search, int offset, int limit,
			ColumnSort[] sorting, String proto) throws AccessDeniedException;

	VirtualFile getRootFolder() throws FileNotFoundException, AccessDeniedException;

	boolean copyFile(String fromPath, String toPath, String proto) throws IOException, AccessDeniedException;

	void downloadFile(String virtualPath, HttpDownloadProcessor processor, String proto)
			throws FileNotFoundException, AccessDeniedException, IOException;

	InputStream downloadFile(String realPath, long position, String proto) throws IOException, AccessDeniedException;

	FileObject getFileObject(String path) throws IOException, AccessDeniedException;
	
	OutputStream uploadFile(String realPath, long position, String proto) throws IOException, AccessDeniedException;

	FileObject getFileObject(FileResource resource) throws IOException;

	VirtualFile createFile(String path, String protocol) throws IOException, AccessDeniedException;

	void setLastModified(String path, long lastModified, String protocol) throws IOException, AccessDeniedException;

	UserVariableReplacement getUserVariableReplacement();

	VirtualFile createUntitledFolder(String virtualPath, String proto) throws IOException, AccessDeniedException;

	Collection<VirtualFile> getMountedFolders() throws AccessDeniedException;

	VirtualFile createVirtualFolder(String virtualPath) throws IOException, AccessDeniedException;

	Collection<FileResource> getRootMounts() throws AccessDeniedException;

	Boolean deleteFile(VirtualFile file, String proto) throws IOException, AccessDeniedException;

	VirtualFile renameFile(VirtualFile fromFile, String toVirtualPath, String proto)
			throws IOException, AccessDeniedException;

	void downloadFile(VirtualFile file, HttpDownloadProcessor processor, String proto)
			throws AccessDeniedException, IOException;

}
