package com.hypersocket.ftp.interfaces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.impl.DefaultFtpServerContext;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.hypersocket.config.ConfigurationChangedEvent;
import com.hypersocket.events.EventService;
import com.hypersocket.events.SystemEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceCreatedEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceDeletedEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceMergedConfigurationChangeEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceUpdatedEvent;
import com.hypersocket.ftp.interfaces.events.PropertyChangeAndTemplate;
import com.hypersocket.i18n.I18N;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.menus.MenuService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.PermissionCategory;
import com.hypersocket.permissions.PermissionService;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.properties.PropertyTemplate;
import com.hypersocket.properties.ResourceUtils;
import com.hypersocket.realm.Realm;
import com.hypersocket.resource.AbstractResourceRepository;
import com.hypersocket.resource.AbstractResourceServiceImpl;
import com.hypersocket.resource.PropertyChange;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceCreationException;
import com.hypersocket.server.events.ServerStartedEvent;
import com.hypersocket.server.events.ServerStoppingEvent;
import com.hypersocket.server.interfaces.InterfaceRegistrationService;
import com.hypersocket.session.SessionService;

@Service
public class FTPInterfaceResourceServiceImpl extends
		AbstractResourceServiceImpl<FTPInterfaceResource> implements
		FTPInterfaceResourceService, ApplicationListener<SystemEvent> {

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
	
	@Autowired
	InterfaceRegistrationService interfaceService;
	
	@Autowired
	FTPResourceService ftpResourceService;
	
	@Autowired
	SessionService sessionService;

	public FTPInterfaceResourceServiceImpl() {
		super("FTPInterface");
	}
	
	@PostConstruct
	private void postConstruct() {

		i18nService.registerBundle(RESOURCE_BUNDLE);
		
		interfaceService.registerAdditionalInterface("ftpInterfaces");

		PermissionCategory cat = permissionService.registerPermissionCategory(
				RESOURCE_BUNDLE, "category.ftpInterfaces");

		for (FTPInterfaceResourcePermission p : FTPInterfaceResourcePermission.values()) {
			permissionService.registerPermission(p, cat);
		}

		repository.loadPropertyTemplates("ftpInterfaceResourceTemplate.xml");


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
	protected boolean fireNonStandardEvents(FTPInterfaceResource resource, List<PropertyChange> changes) {
		List<PropertyChangeAndTemplate> propertyChangeAndTemplates = new ArrayList<>();
		
		for (PropertyChange propertyChange : changes) {
			PropertyTemplate template = repository.getPropertyTemplate(resource, propertyChange.getId());
			PropertyChangeAndTemplate propertyChangeAndTemplate = new PropertyChangeAndTemplate(template, propertyChange);
			propertyChangeAndTemplates.add(propertyChangeAndTemplate);
			eventService.publishEvent(new ConfigurationChangedEvent(this, true, getCurrentSession(), template, propertyChange.getOldValue(), propertyChange.getNewValue(), false));
		}
		eventService.publishEvent(new FTPInterfaceResourceMergedConfigurationChangeEvent(this, getCurrentSession(), resource, propertyChangeAndTemplates));
		return super.fireNonStandardEvents(resource, changes);
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
		
		if(FTPProtocol.FTPS.name().equals(properties.get("ftpProtocol")) && StringUtils.isEmpty(properties.get("ftpCertificate"))){
			throw new IllegalStateException(I18N.getResource(getCurrentLocale(), FTPInterfaceResourceServiceImpl.RESOURCE_BUNDLE, "ftps.certificate.missing", new Object[0]));
		}
		
		Set<String> fromSource = null;
		if(resource.getFtpInterfaces() != null){
			fromSource = new HashSet<>(Arrays.asList(ResourceUtils.explodeValues(resource.ftpInterfaces)));
		}else{
			fromSource = new HashSet<>();
		}
		
		
		DefaultFtpServer ftpServer = (DefaultFtpServer) ftpResourceService.ftpServer;
		String[] interfaces = ResourceUtils.explodeValues(properties.get("ftpInterfaces"));
		if(interfaces != null && ftpServer != null){
			for (String intface : interfaces) {
				if(fromSource.contains(intface)){
					continue;
				}
				Listener existingListener = ftpServer.getListener(AbstractFTPResourceService.interfaceName(intface, Integer.parseInt(properties.get("ftpPort"))));
				if(existingListener != null){
					throw new IllegalStateException(I18N.getResource(getCurrentLocale(), FTPInterfaceResourceServiceImpl.RESOURCE_BUNDLE, "ftp.interface.already.registered", intface, properties.get("ftpPort")));
				}
			}
		}
			

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
		sessionService.getCurrentLocale();
		
		DefaultFtpServer ftpServer = (DefaultFtpServer) ftpResourceService.ftpServer;
		String[] interfaces = ResourceUtils.explodeValues(properties.get("ftpInterfaces"));
		if(interfaces != null && ftpServer != null){
			for (String intface : interfaces) {
				Listener existingListener = ftpServer.getListener(AbstractFTPResourceService.interfaceName(intface, Integer.parseInt(properties.get("ftpPort"))));
				if(existingListener != null){
					throw new IllegalStateException(I18N.getResource(getCurrentLocale(), FTPInterfaceResourceServiceImpl.RESOURCE_BUNDLE, "ftp.interface.already.registered", intface, properties.get("ftpPort")));
				}
			}
		}
			
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

	@Override
	public void onApplicationEvent(final SystemEvent event) {
		sessionService.executeInSystemContext(new Runnable() {
			
			@Override
			public void run() {
				if (event instanceof ServerStartedEvent) {
					ftpResourceService.start();
				}else if (event instanceof ServerStoppingEvent) {
					ftpResourceService.stop();
				}else if(event instanceof FTPInterfaceResourceMergedConfigurationChangeEvent){
					FTPInterfaceResourceMergedConfigurationChangeEvent mergedChangeEvent = (FTPInterfaceResourceMergedConfigurationChangeEvent) event;
					deleteInterfaces(mergedChangeEvent.getChangeLog().getToDeleteFtpInterfaceResource());
					createInterfaces(mergedChangeEvent.getChangeLog().getToCreateFtpInterfaceResource());
				}else if(event instanceof FTPInterfaceResourceCreatedEvent){
					//fetch all the listners from the object, add and start them
					FTPInterfaceResource ftpInterfaceResource = (FTPInterfaceResource) ((FTPInterfaceResourceCreatedEvent) event).getResource();
					createInterfaces(ftpInterfaceResource);
				}else if(event instanceof FTPInterfaceResourceDeletedEvent){
					//fetch all the listners from the server instance and stop them
					FTPInterfaceResource ftpInterfaceResource = (FTPInterfaceResource) ((FTPInterfaceResourceDeletedEvent) event).getResource();
					deleteInterfaces(ftpInterfaceResource);
				}
			}
		});
	}
	
	private void createInterfaces(FTPInterfaceResource ftpInterfaceResource){
		if(!ftpResourceService.running){
			ftpResourceService.start();
		}
		
		//add to existing
		DefaultFtpServer ftpServer = (DefaultFtpServer) ftpResourceService.ftpServer;
		DefaultFtpServerContext ftpServerContext = (DefaultFtpServerContext) ftpServer.getServerContext();
		
		String[] interfaces = ResourceUtils.explodeValues(ftpInterfaceResource.ftpInterfaces);
		if (interfaces != null && interfaces.length > 0) {
			for (String intface : interfaces) {
				Listener existingListener = ftpServer.getListener(AbstractFTPResourceService.interfaceName(intface, ftpInterfaceResource.ftpPort));
				if(existingListener != null){
					continue;
				}
				ListenerFactory factory = ftpResourceService.createListener(ftpInterfaceResource, intface, ftpResourceService.getSslConfiguration(ftpInterfaceResource));
				Listener listener = factory.createListener();
				listener.start(ftpServerContext);
				ftpServerContext.addListener(AbstractFTPResourceService.interfaceName(intface, ftpInterfaceResource.ftpPort), listener);
			}
		}
	}
	
	private void deleteInterfaces(FTPInterfaceResource ftpInterfaceResource){
		if(ftpResourceService.running){
			//remove from existing
			DefaultFtpServer ftpServer = (DefaultFtpServer) ftpResourceService.ftpServer;
			DefaultFtpServerContext ftpServerContext = (DefaultFtpServerContext) ftpServer.getServerContext();
			
			String[] interfaces = ResourceUtils.explodeValues(ftpInterfaceResource.ftpInterfaces);
			if (interfaces != null && interfaces.length > 0) {
				for (String intface : interfaces) {
					String interfaceName = AbstractFTPResourceService.interfaceName(intface, ftpInterfaceResource.ftpPort);
					if(ftpServerContext.getListener(interfaceName) != null){
						ftpServerContext.getListener(interfaceName).stop();
					}
					ftpServerContext.removeListener(interfaceName);
				}
			}
		}	
	}

}
