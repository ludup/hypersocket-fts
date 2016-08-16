package com.hypersocket.ftp.interfaces;

import java.util.Collection;
import java.util.Map;

import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.realm.Realm;
import com.hypersocket.resource.AbstractResourceService;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceCreationException;

public interface FTPInterfaceResourceService extends
		AbstractResourceService<FTPInterfaceResource> {

	FTPInterfaceResource updateResource(FTPInterfaceResource resourceById, String name, Map<String,String> properties)
			throws ResourceChangeException, AccessDeniedException;

	FTPInterfaceResource createResource(String name, Realm realm, Map<String,String> properties)
			throws ResourceCreationException, AccessDeniedException;

	Collection<PropertyCategory> getPropertyTemplate() throws AccessDeniedException;

	Collection<PropertyCategory> getPropertyTemplate(FTPInterfaceResource resource)
			throws AccessDeniedException;

}
