package com.hypersocket.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.resource.AbstractAssignableResourceService;

public interface FileResourceService extends AbstractAssignableResourceService<FileResource> {

	static final String RESOURCE_BUNDLE = "FileResourceService";
	
	FileResource getMountForPath(String host, String controllerPath, String path) throws FileNotFoundException;

	boolean isWebDAVRoot(String path);

	boolean isMountResource(FileResource resource, String controllerPath, String path);

	String resolveChildPath(FileResource resource, String controllerPath, String path) throws IOException;

	List<FileResourceScheme> getSchemes();

	void registerScheme(FileResourceScheme scheme);

	boolean deleteFile(String host, String controllerPath, String uri)
			throws IOException, AccessDeniedException;
	
	boolean renameFile(String host, String controllerPath, String fromUri, String toUri)
			throws IOException, AccessDeniedException;

	FileObject createFolder(String host, String controllerPath, String parentUri) throws IOException, AccessDeniedException;

	FileObject createFolder(String host, String controllerPath, String parentUri, String newName) throws IOException, AccessDeniedException;
	
	FileObject resolveMountFile(FileResource resource) throws IOException;

	void downloadFile(String host, String controllerPath, String uri,
			DownloadProcessor callback) throws IOException, AccessDeniedException;

	boolean copyFile(String host, String controllerPath, String fromUri,
			String toUri) throws IOException, AccessDeniedException;

	void uploadFile(String host, String controllerPath, String uri,
			InputStream in, UploadProcessor<?> processor) throws IOException, AccessDeniedException;

}
