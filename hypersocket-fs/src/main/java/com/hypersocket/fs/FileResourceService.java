package com.hypersocket.fs;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.resource.AbstractAssignableResourceService;
import com.hypersocket.resource.ResourceException;

public interface FileResourceService extends
		AbstractAssignableResourceService<FileResource> {

	List<FileResourceScheme> getSchemes();

	void registerScheme(FileResourceScheme scheme);

	Collection<PropertyCategory> getResourceProperties(FileResource resource) throws AccessDeniedException;

	Collection<PropertyCategory> getPropertyTemplates(String scheme) throws AccessDeniedException;

	void createFileResource(FileResource r, Map<String, String> properties) throws AccessDeniedException, ResourceException;

	void updateFileResource(FileResource r, Map<String, String> properties) throws ResourceException, AccessDeniedException;

	FileResourceScheme getScheme(String scheme);

	Collection<FileResource> getResourcesByVirtualPath(String string) throws AccessDeniedException;

	Collection<FileResource> getNonRootResources() throws AccessDeniedException;

	void deleteResource(FileResource resource) throws ResourceException, AccessDeniedException;

	void resetRebuildReconcileStatus(FileResource resource);

}
