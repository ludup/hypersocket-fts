package com.hypersocket.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.realm.UserVariableReplacement;
import com.hypersocket.resource.AbstractAssignableResourceService;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceCreationException;
import com.hypersocket.upload.FileUpload;

public interface FileResourceService extends
		AbstractAssignableResourceService<FileResource> {

	FileResource getMountForURIPath(String host, String controllerPath,
			String path) throws FileNotFoundException, AccessDeniedException;

	boolean isURIFilesystemRoot(String path);

	boolean isURIMountResource(FileResource resource, String controllerPath,
			String path);

	String resolveURIChildPath(FileResource resource, String controllerPath,
			String path) throws IOException;

	List<FileResourceScheme> getSchemes();

	void registerScheme(FileResourceScheme scheme);

	boolean deleteURIFile(String host, String controllerPath, String uri,
			String protocol) throws IOException, AccessDeniedException;

	boolean renameURIFile(String host, String controllerPath, String fromUri,
			String toUri, String protocol) throws IOException,
			AccessDeniedException;

	FileObject createURIFolder(String host, String controllerPath,
			String parentUri, String protocol) throws IOException,
			AccessDeniedException;

	FileObject createURIFolder(String host, String controllerPath,
			String parentUri, String newName, String protocol)
			throws IOException, AccessDeniedException;

	FileObject resolveMountFile(FileResource resource) throws IOException;

	void downloadURIFile(String host, String controllerPath, String uri,
			DownloadProcessor callback, String protocol) throws IOException,
			AccessDeniedException;

	boolean copyURIFile(String host, String controllerPath, String fromUri,
			String toUri, String protocol) throws IOException,
			AccessDeniedException;

	FileUpload uploadURIFile(String host, String controllerPath, String uri,
			InputStream in, UploadProcessor<?> processor, String protocol)
			throws IOException, AccessDeniedException;

	FileResource getMountForPath(String path) throws AccessDeniedException;

	String resolveChildPath(FileResource resource, String path)
			throws IOException;

	InputStream downloadFile(String path, long position, String protocol)
			throws IOException, AccessDeniedException;

	OutputStream uploadFile(String path, long position, String protocol)
			throws IOException, AccessDeniedException;

	boolean deleteFile(String path, String protocol) throws IOException,
			AccessDeniedException;

	boolean renameFile(String fromPath, String toPath, String protocol)
			throws IOException, AccessDeniedException;

	boolean copyFile(String fromPath, String toPath, String protocol)
			throws IOException, AccessDeniedException;

	FileObject createFolder(String parentPath, String newName, String protocol)
			throws IOException, AccessDeniedException;

	boolean testVFSUri(String privateUrl) throws FileSystemException;

	UserVariableReplacement getUserVariableReplacement();

	FileObject getFileObjectForMountFile(String file)
			throws AccessDeniedException, IOException;

	Collection<PropertyCategory> getResourceProperties(FileResource resource) throws AccessDeniedException;

	Collection<PropertyCategory> getPropertyTemplates(String scheme) throws AccessDeniedException;

	void createFileResource(FileResource r, Map<String, String> properties) throws ResourceCreationException, AccessDeniedException;

	void updateFileResource(FileResource r, Map<String, String> properties) throws ResourceChangeException, AccessDeniedException;

}
