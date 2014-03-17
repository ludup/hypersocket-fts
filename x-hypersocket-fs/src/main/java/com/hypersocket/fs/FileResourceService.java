package com.hypersocket.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.vfs2.FileObject;

import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.resource.AbstractAssignableResourceService;

public interface FileResourceService extends AbstractAssignableResourceService<FileResource> {

	static final String RESOURCE_BUNDLE = "FileResourceService";
	
	FileResource getMountForURIPath(String host, String controllerPath, String path) throws FileNotFoundException;

	boolean isURIFilesystemRoot(String path);

	boolean isURIMountResource(FileResource resource, String controllerPath, String path);

	String resolveURIChildPath(FileResource resource, String controllerPath, String path) throws IOException;

	List<FileResourceScheme> getSchemes();

	void registerScheme(FileResourceScheme scheme);

	boolean deleteURIFile(String host, String controllerPath, String uri)
			throws IOException, AccessDeniedException;
	
	boolean renameURIFile(String host, String controllerPath, String fromUri, String toUri)
			throws IOException, AccessDeniedException;

	FileObject createURIFolder(String host, String controllerPath, String parentUri) throws IOException, AccessDeniedException;

	FileObject createURIFolder(String host, String controllerPath, String parentUri, String newName) throws IOException, AccessDeniedException;
	
	FileObject resolveMountFile(FileResource resource) throws IOException;

	void downloadURIFile(String host, String controllerPath, String uri,
			DownloadProcessor callback) throws IOException, AccessDeniedException;

	boolean copyURIFile(String host, String controllerPath, String fromUri,
			String toUri) throws IOException, AccessDeniedException;

	void uploadURIFile(String host, String controllerPath, String uri,
			InputStream in, UploadProcessor<?> processor) throws IOException, AccessDeniedException;

	FileResource getMountForPath(String path);

	String resolveChildPath(FileResource resource, String path)
			throws IOException;

	InputStream downloadFile(String path, long position) throws IOException,
			AccessDeniedException;

	OutputStream uploadFile(String path, long position) throws IOException,
			AccessDeniedException;

	boolean deleteFile(String path) throws IOException, AccessDeniedException;

	boolean renameFile(String fromPath, String toPath) throws IOException,
			AccessDeniedException;

	boolean copyFile(String fromPath, String toPath) throws IOException,
			AccessDeniedException;

}
