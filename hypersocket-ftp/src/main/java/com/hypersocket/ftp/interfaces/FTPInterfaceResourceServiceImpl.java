package com.hypersocket.ftp.interfaces;

import java.util.Collection;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hypersocket.events.EventService;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.menus.MenuRegistration;
import com.hypersocket.menus.MenuService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.PermissionCategory;
import com.hypersocket.permissions.PermissionService;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.realm.Realm;
import com.hypersocket.resource.AbstractResourceRepository;
import com.hypersocket.resource.AbstractResourceServiceImpl;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceCreationException;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceCreatedEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceDeletedEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceUpdatedEvent;

@Service
public class FTPInterfaceResourceServiceImpl extends
		AbstractResourceServiceImpl<FTPInterfaceResource> implements
		FTPInterfaceResourceService {

	public static final String RESOURCE_BUNDLE = "FTPInterfaceResourceService";

	@Autowired
	FTPInterfaceResourceRepository repository;

	@Autowired
	I18NService i18nService;

	@Autowired
	PermissionService permissionService;

	@Autowired
	MenuService menuService;

	@Autowired
	EventService eventService;

	public FTPInterfaceResourceServiceImpl() {
		super("FTPInterface");
	}
	
	@PostConstruct
	private void postConstruct() {

		i18nService.registerBundle(RESOURCE_BUNDLE);

		PermissionCategory cat = permissionService.registerPermissionCategory(
				RESOURCE_BUNDLE, "category.fTPInterfaces");

		for (FTPInterfaceResourcePermission p : FTPInterfaceResourcePermission.values()) {
			permissionService.registerPermission(p, cat);
		}

		repository.loadPropertyTemplates("fTPInterfaceResourceTemplate.xml");

//		menuService.registerMenu(new MenuRegistration(RESOURCE_BUNDLE,
//				"fTPInterfaces", "fa-flash", "fTPInterfaces", 100,
//				FTPInterfaceResourcePermission.READ,
//				FTPInterfaceResourcePermission.CREATE,
//				FTPInterfaceResourcePermission.UPDATE,
//				FTPInterfaceResourcePermission.DELETE), MenuService.MENU_RESOURCES);

		/**
		 * Register the events. All events have to be registerd so the system
		 * knows about them.
		 */
		eventService.registerEvent(
				FTPInterfaceResourceEvent.class, RESOURCE_BUNDLE,
				this);
		eventService.registerEvent(
				FTPInterfaceResourceCreatedEvent.class, RESOURCE_BUNDLE,
				this);
		eventService.registerEvent(
				FTPInterfaceResourceUpdatedEvent.class, RESOURCE_BUNDLE,
				this);
		eventService.registerEvent(
				FTPInterfaceResourceDeletedEvent.class, RESOURCE_BUNDLE,
				this);

		repository.getEntityStore().registerResourceService(FTPInterfaceResource.class, repository);
	}
	
	@Override
	protected boolean isSystemResource() {
		return true;
	}

	@Override
	protected AbstractResourceRepository<FTPInterfaceResource> getRepository() {
		return repository;
	}

	@Override
	protected String getResourceBundle() {
		return RESOURCE_BUNDLE;
	}

	@Override
	public Class<FTPInterfaceResourcePermission> getPermissionType() {
		return FTPInterfaceResourcePermission.class;
	}
	
	protected Class<FTPInterfaceResource> getResourceClass() {
		return FTPInterfaceResource.class;
	}
	
	@Override
	protected void fireResourceCreationEvent(FTPInterfaceResource resource) {
		eventService.publishEvent(new FTPInterfaceResourceCreatedEvent(this,
				getCurrentSession(), resource));
	}

	@Override
	protected void fireResourceCreationEvent(FTPInterfaceResource resource,
			Throwable t) {
		eventService.publishEvent(new FTPInterfaceResourceCreatedEvent(this,
				resource, t, getCurrentSession()));
	}

	@Override
	protected void fireResourceUpdateEvent(FTPInterfaceResource resource) {
		eventService.publishEvent(new FTPInterfaceResourceUpdatedEvent(this,
				getCurrentSession(), resource));
	}

	@Override
	protected void fireResourceUpdateEvent(FTPInterfaceResource resource,
			Throwable t) {
		eventService.publishEvent(new FTPInterfaceResourceUpdatedEvent(this,
				resource, t, getCurrentSession()));
	}

	@Override
	protected void fireResourceDeletionEvent(FTPInterfaceResource resource) {
		eventService.publishEvent(new FTPInterfaceResourceDeletedEvent(this,
				getCurrentSession(), resource));
	}

	@Override
	protected void fireResourceDeletionEvent(FTPInterfaceResource resource,
			Throwable t) {
		eventService.publishEvent(new FTPInterfaceResourceDeletedEvent(this,
				resource, t, getCurrentSession()));
	}

	@Override
	public FTPInterfaceResource updateResource(FTPInterfaceResource resource,
			String name, Map<String, String> properties)
			throws ResourceChangeException, AccessDeniedException {

		resource.setName(name);

		/**
		 * Set any additional fields on your resource here before calling
		 * updateResource.
		 * 
		 * Remember to fill in the fire*Event methods to ensure events are fired
		 * for all operations.
		 */
		updateResource(resource, properties);

		return resource;
	}

	@Override
	public FTPInterfaceResource createResource(String name, Realm realm,
			Map<String, String> properties) throws ResourceCreationException,
			AccessDeniedException {

		FTPInterfaceResource resource = new FTPInterfaceResource();
		resource.setName(name);
		resource.setRealm(realm);
		/**
		 * Set any additional fields on your resource here before calling
		 * createResource.
		 * 
		 * Remember to fill in the fire*Event methods to ensure events are fired
		 * for all operations.
		 */
		createResource(resource, properties);

		return resource;
	}

	@Override
	public Collection<PropertyCategory> getPropertyTemplate()
			throws AccessDeniedException {

		assertPermission(FTPInterfaceResourcePermission.READ);

		return repository.getPropertyCategories(null);
	}

	@Override
	public Collection<PropertyCategory> getPropertyTemplate(
			FTPInterfaceResource resource) throws AccessDeniedException {

		assertPermission(FTPInterfaceResourcePermission.READ);

		return repository.getPropertyCategories(resource);
	}

}
