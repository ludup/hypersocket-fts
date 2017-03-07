package com.hypersocket.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.provider.FileProvider;

import com.hypersocket.auth.AuthenticatedService;
import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.UploadEventProcessor;
import com.hypersocket.fs.UploadProcessor;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.realm.Principal;
import com.hypersocket.realm.UserVariableReplacementService;
import com.hypersocket.resource.Resource;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.tables.ColumnSort;
import com.hypersocket.upload.FileUpload;
import com.hypersocket.vfs.json.HttpDownloadProcessor;

public interface VirtualFileService extends AuthenticatedService {

	VirtualFile getFile(String virtualPath) throws AccessDeniedException, IOException;

	Collection<VirtualFile> getChildren(String virtualPath) throws AccessDeniedException, IOException;

	Collection<VirtualFile> getChildren(VirtualFile folder) throws AccessDeniedException, IOException;

	Boolean deleteFile(String virtualPath, String proto) throws IOException, AccessDeniedException;

	VirtualFile createFolder(String virtualPath, String proto) throws IOException, AccessDeniedException;

	VirtualFile renameFile(String virtualPath, String toUri, String proto) throws IOException, AccessDeniedException;

	FileUpload uploadFile(String virtualPath, InputStream in, UploadProcessor<?> processor, String proto) throws AccessDeniedException, IOException;

	Collection<VirtualFile> listChildren(String virtualPath, String proto) throws IOException, AccessDeniedException;

	long getSearchCount(String virtualPath, String searchColumn, String search, String flags) throws AccessDeniedException;

	Collection<VirtualFile> searchFiles(String virtualPath, 
			String searchColumn, String search, int offset, int limit,
			ColumnSort[] sorting, String proto, String flags) throws AccessDeniedException, IOException;

	VirtualFile getRootFolder() throws AccessDeniedException, IOException;

	boolean copyFile(String fromPath, String toPath, String proto) throws IOException, AccessDeniedException;

	void downloadFile(String virtualPath, HttpDownloadProcessor processor, String proto)
			throws FileNotFoundException, AccessDeniedException, IOException;

	InputStream downloadFile(String realPath, long position, String proto) throws IOException, AccessDeniedException;

	InputStream downloadFile(String realPath, long position, String proto, Principal overridePrincipal, String overrideUsername, String overridePassword) throws IOException, AccessDeniedException;
	
	FileObject getFileObject(String path) throws IOException, AccessDeniedException;
	
	OutputStream uploadFile(String realPath, long position, String proto) throws IOException, AccessDeniedException;

	FileObject getFileObject(FileResource resource) throws IOException;

	VirtualFile createFile(String path, String protocol) throws IOException, AccessDeniedException;

	void setLastModified(String path, long lastModified, String protocol) throws IOException, AccessDeniedException;

	UserVariableReplacementService getUserVariableReplacement();

	VirtualFile createUntitledFolder(String virtualPath, String proto) throws IOException, AccessDeniedException;

	Collection<VirtualFile> getVirtualFolders() throws AccessDeniedException;

	VirtualFile createVirtualFolder(String virtualPath) throws IOException, AccessDeniedException;

	Collection<FileResource> getRootMounts() throws AccessDeniedException;

	Boolean deleteFile(VirtualFile file, String proto) throws IOException, AccessDeniedException;

	VirtualFile renameFile(VirtualFile fromFile, String toVirtualPath, String proto)
			throws IOException, AccessDeniedException;

	void downloadFile(VirtualFile file, HttpDownloadProcessor processor, String proto)
			throws AccessDeniedException, IOException;

	Collection<FileResource> getNonRootMounts() throws AccessDeniedException;

	Collection<FileResource> getMountsForPath(String virtualPath) throws AccessDeniedException, IOException;

	VirtualFile getFileById(Long id) throws AccessDeniedException;

	void setDefaultMount(VirtualFile file, FileResource mount) throws AccessDeniedException, ResourceChangeException;

	OutputStream uploadFile(String virtualPath, long position, String proto, UploadEventProcessor uploadProcessor)
			throws IOException, AccessDeniedException;

	VirtualFile createFolder(String virtualPath, String proto, boolean disableEvent)
			throws IOException, AccessDeniedException;

	boolean isRootWritable(Principal currentPrincipal) throws AccessDeniedException, IOException;

	void detachMount(FileResource resource);

	void attachMount(VirtualFile mountedFile, FileResource resource);

	void clearCredentials();

	void setupCredentials(Principal principal, String username, String password);

	<T extends Resource> FileSystemManager getManager(T id, CacheStrategy cacheStrategy) throws FileSystemException;

	void addProvider(String scheme, FileProvider newInstance) throws FileSystemException;
}
