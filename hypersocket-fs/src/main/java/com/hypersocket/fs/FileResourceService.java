package com.hypersocket.fs;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.resource.AbstractAssignableResourceService;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceCreationException;

public interface FileResourceService extends
		AbstractAssignableResourceService<FileResource> {

	List<FileResourceScheme> getSchemes();

	void registerScheme(FileResourceScheme scheme);

	Collection<PropertyCategory> getResourceProperties(FileResource resource) throws AccessDeniedException;

	Collection<PropertyCategory> getPropertyTemplates(String scheme) throws AccessDeniedException;

	void createFileResource(FileResource r, Map<String, String> properties) throws ResourceCreationException, AccessDeniedException;

	void updateFileResource(FileResource r, Map<String, String> properties) throws ResourceChangeException, AccessDeniedException;

	FileResourceScheme getScheme(String scheme);

	Collection<FileResource> getResourcesByVirtualPath(String string) throws AccessDeniedException;

	Collection<FileResource> getNonRootResources() throws AccessDeniedException;

	void deleteResource(FileResource resource) throws ResourceChangeException, AccessDeniedException;

	void resetRebuildReconcileStatus(FileResource resource);

}
