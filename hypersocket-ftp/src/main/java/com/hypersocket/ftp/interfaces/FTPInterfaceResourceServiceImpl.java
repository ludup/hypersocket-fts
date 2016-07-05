package com.hypersocket.ftp.interfaces;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.impl.DefaultFtpServerContext;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.hypersocket.config.ConfigurationChangedEvent;
import com.hypersocket.events.EventService;
import com.hypersocket.events.SystemEvent;
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
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceCreatedEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceDeletedEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceUpdatedEvent;

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
	FTPSResourceService ftpsResourceService;
	
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
		for (PropertyChange propertyChange : changes) {
			PropertyTemplate template = repository.getPropertyTemplate(resource, propertyChange.getId());
			eventService.publishEvent(new ConfigurationChangedEvent(this, true, getCurrentSession(), template, propertyChange.getOldValue(), propertyChange.getNewValue(), false));
		}
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
		
		FTPInterfaceResource source = repository.getResourceById(resource.getId());
		Set<String> fromSource = null;
		if(source.getFtpInterfaces() != null){
			fromSource = new HashSet<>(Arrays.asList(ResourceUtils.explodeValues(source.ftpInterfaces)));
		}else{
			fromSource = new HashSet<>();
		}
		
		
		if(FTPProtocol.FTP.name().equals(properties.get("ftpProtocol"))){
			DefaultFtpServer ftpServer = (DefaultFtpServer) ftpResourceService.ftpServer;
			String[] interfaces = ResourceUtils.explodeValues(properties.get("ftpInterfaces"));
			if(interfaces != null){
				for (String intface : interfaces) {
					if(fromSource.contains(intface)){
						continue;
					}
					Listener existingListener = ftpServer.getListener(AbstractFTPResourceService.interfaceName(intface, Integer.parseInt(properties.get("ftpPort"))));
					if(existingListener != null){
						throw new IllegalStateException(String.format("Interface with IP %s and Port %s already exists.", intface, properties.get("ftpPort")));
					}
				}
			}
			
		}else if(FTPProtocol.FTPS.name().equals(properties.get("ftpProtocol"))){
			
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
		
		if(FTPProtocol.FTP.name().equals(properties.get("ftpProtocol"))){
			DefaultFtpServer ftpServer = (DefaultFtpServer) ftpResourceService.ftpServer;
			String[] interfaces = ResourceUtils.explodeValues(properties.get("ftpInterfaces"));
			if(interfaces != null){
				for (String intface : interfaces) {
					Listener existingListener = ftpServer.getListener(AbstractFTPResourceService.interfaceName(intface, Integer.parseInt(properties.get("ftpPort"))));
					if(existingListener != null){
						throw new IllegalStateException(String.format("Interface with IP %s and Port %s already exists.", intface, properties.get("ftpPort")));
					}
				}
			}
			
		}else if(FTPProtocol.FTPS.name().equals(properties.get("ftpProtocol"))){
			
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
					ftpsResourceService.start();
				}else if (event instanceof ServerStoppingEvent) {
					ftpResourceService.stop();
					ftpsResourceService.stop();
				}else if(event instanceof ConfigurationChangedEvent){
					System.out.println(((ConfigurationChangedEvent)event).getSource());
					/**
					 * {attr.configOldValue=0:0:0:0:0:0:0:1
192.168.1.101, attr.principalRealm=System, attr.uuid=ef9cdb23-6e61-4046-89bc-190b85177213, attr.principalDesc=, attr.principalName=admin, attr.configNewValue=0:0:0:0:0:0:0:1
192.168.1.101
127.0.0.1, attr.configResourceKey=ftpInterfaces, attr.configItem=i18n/FTPInterfaceResourceService/ftpInterfaces, attr.ipAddress=0:0:0:0:0:0:0:1}




{attr.configOldValue=9031, attr.principalRealm=System, attr.uuid=ef9cdb23-6e61-4046-89bc-190b85177213, attr.principalDesc=, attr.principalName=admin, 
attr.configNewValue=9032, attr.configResourceKey=ftpPort, attr.configItem=i18n/FTPInterfaceResourceService/ftpPort, attr.ipAddress=0:0:0:0:0:0:0:1}
					 */
				}else if(event instanceof FTPInterfaceResourceCreatedEvent){
					//fetch all the listners from the object, add and start them
					FTPInterfaceResource ftpInterfaceResource = (FTPInterfaceResource) ((FTPInterfaceResourceCreatedEvent) event).getResource();
					if(FTPProtocol.FTP.equals(ftpInterfaceResource.ftpProtocol)){
						if(ftpResourceService.running){
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
									ListenerFactory factory = ftpResourceService.createListener(ftpInterfaceResource, intface);
									Listener listener = factory.createListener();
									listener.start(ftpServerContext);
									ftpServerContext.addListener(intface, listener);
								}
							}
						}else{
							//start new
							ftpResourceService.start();
						}
					}
				}else if(event instanceof FTPInterfaceResourceDeletedEvent){
					//fetch all the listners from the server instance and stop them
					FTPInterfaceResource ftpInterfaceResource = (FTPInterfaceResource) ((FTPInterfaceResourceDeletedEvent) event).getResource();
					if(FTPProtocol.FTP.equals(ftpInterfaceResource.ftpProtocol)){
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
						}else{
							//stop running
							ftpResourceService.stop();
						}	
					}
				}else if(event instanceof FTPInterfaceResourceUpdatedEvent){
					//fetch all the listners from the object, add and start them
					FTPInterfaceResource ftpInterfaceResource = (FTPInterfaceResource) ((FTPInterfaceResourceUpdatedEvent) event).getResource();
					if(FTPProtocol.FTP.equals(ftpInterfaceResource.ftpProtocol)){
						if(ftpResourceService.running){
							//add to existing
							DefaultFtpServer ftpServer = (DefaultFtpServer) ftpResourceService.ftpServer;
							DefaultFtpServerContext ftpServerContext = (DefaultFtpServerContext) ftpServer.getServerContext();
							
							Set<String> latestInterfacesSelectedByUser = new HashSet<>();
							String[] interfaces = ResourceUtils.explodeValues(ftpInterfaceResource.ftpInterfaces);
							if (interfaces != null && interfaces.length > 0) {
								for (String intface : interfaces) {
									String interfaceName = AbstractFTPResourceService.interfaceName(intface, ftpInterfaceResource.ftpPort);
									latestInterfacesSelectedByUser.add(interfaceName);
									if(ftpServer.getListeners().containsKey(interfaceName)){
										continue;
									}
									ListenerFactory factory = ftpResourceService.createListener(ftpInterfaceResource, intface);
									Listener listener = factory.createListener();
									listener.start(ftpServerContext);
									ftpServerContext.addListener(interfaceName, listener);
								}
							}
							
							Set<String> existingInterfaces = new HashSet<>(ftpServer.getListeners().keySet());
							for (String intface : latestInterfacesSelectedByUser) {
								if(!existingInterfaces.contains(intface)){
									//removed by user
									System.out.println("Stoping !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + intface);
									ftpServerContext.getListener(intface).stop();
									ftpServerContext.removeListener(intface);
								}
							}
								
						}
					}
				}
			}
		});
	}

}
