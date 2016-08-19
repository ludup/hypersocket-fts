package com.hypersocket.ftp.interfaces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.impl.DefaultFtpServerContext;
import org.apache.ftpserver.impl.PassivePorts;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.hypersocket.auth.AuthenticationModuleType;
import com.hypersocket.auth.AuthenticationSchemeRepository;
import com.hypersocket.auth.UsernameAndPasswordAuthenticator;
import com.hypersocket.events.EventService;
import com.hypersocket.events.SystemEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceCreatedEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceDeletedEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceEvent;
import com.hypersocket.ftp.interfaces.events.FTPInterfaceResourceUpdatedEvent;
import com.hypersocket.i18n.I18N;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.menus.MenuService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.PermissionCategory;
import com.hypersocket.permissions.PermissionService;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.properties.ResourceUtils;
import com.hypersocket.realm.Realm;
import com.hypersocket.realm.RealmAdapter;
import com.hypersocket.resource.AbstractResourceRepository;
import com.hypersocket.resource.AbstractResourceServiceImpl;
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
	
	static Logger log = LoggerFactory.getLogger(FTPInterfaceResourceServiceImpl.class);

	public static final String RESOURCE_BUNDLE = "FTPInterfaceResourceService";
	
	public static final String AUTHENTICATION_SCHEME_NAME = "FTP";

	public static final String AUTHENTICATION_SCHEME_RESOURCE_KEY = "ftp";

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
	
	@Autowired
	AuthenticationSchemeRepository schemeRepository;

	public FTPInterfaceResourceServiceImpl() {
		super("FTPInterface");
	}
	
	@PostConstruct
	private void postConstruct() {

		i18nService.registerBundle(RESOURCE_BUNDLE);
		
		setupRealms();
		
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
	
	private void setupRealms() {

		realmService.registerRealmListener(new RealmAdapter() {
			
			public boolean hasCreatedDefaultResources(Realm realm) {
				return schemeRepository.getSchemeByResourceKeyCount(realm,
						AUTHENTICATION_SCHEME_RESOURCE_KEY) > 0;
			}
			public void onCreateRealm(Realm realm) {
				
				if (log.isInfoEnabled()) {
					log.info("Creating " + AUTHENTICATION_SCHEME_NAME
							+ " authentication scheme for realm "
							+ realm.getName());
				}
				
				List<String> modules = new ArrayList<String>();
				modules.add(UsernameAndPasswordAuthenticator.RESOURCE_KEY);
				schemeRepository.createScheme(realm,
						AUTHENTICATION_SCHEME_NAME, modules,
						AUTHENTICATION_SCHEME_RESOURCE_KEY, 
						false, 
						1, 
						AuthenticationModuleType.BASIC);
			}
		});

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
		
		checkFTPSHasCertificate(properties);
		
		checkPassivePortRange(properties.get("ftpPassivePorts"));
		
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

		checkFTPSHasCertificate(properties);
		
		checkPassivePortRange(properties.get("ftpPassivePorts"));
		
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
				}else if(event instanceof FTPInterfaceResourceCreatedEvent){
					FTPInterfaceResource ftpInterfaceResource = (FTPInterfaceResource) ((FTPInterfaceResourceCreatedEvent) event).getResource();
					createInterfaces(ftpInterfaceResource);
				}else if(event instanceof FTPInterfaceResourceUpdatedEvent){
					FTPInterfaceResource ftpInterfaceResource = (FTPInterfaceResource) ((FTPInterfaceResourceUpdatedEvent) event).getResource();
					updateInterfaces(ftpInterfaceResource);
				}else if(event instanceof FTPInterfaceResourceDeletedEvent){
					FTPInterfaceResource ftpInterfaceResource = (FTPInterfaceResource) ((FTPInterfaceResourceDeletedEvent) event).getResource();
					deleteInterfaces(ftpInterfaceResource);
				}
			}
		});
	}
	
	private void updateInterfaces(FTPInterfaceResource ftpInterfaceResource){
		
		DefaultFtpServer ftpServer = (DefaultFtpServer) ftpResourceService.ftpServer;
		DefaultFtpServerContext ftpServerContext = (DefaultFtpServerContext) ftpServer.getServerContext();
		
		Map<String, Listener> currentListeners = ftpServerContext.getListeners();
		
		if(currentListeners.isEmpty()){
			//nothing to compute create all new
			createInterfaces(ftpInterfaceResource);
			return;
		}else{
			//stop existing and create all new
			for (Entry<String, Listener> listenerEntry : currentListeners.entrySet()) {
				listenerEntry.getValue().stop();
				log.info(String.format("Stopping ftp instance %s", listenerEntry.getKey()));
				ftpServerContext.removeListener(listenerEntry.getKey());
			}
			createInterfaces(ftpInterfaceResource);
		}
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
					log.info(String.format("Stopping ftp instance %s:%s", interfaceName, ftpInterfaceResource.ftpProtocol.name()));
					if(ftpServerContext.getListener(interfaceName) != null){
						ftpServerContext.getListener(interfaceName).stop();
					}
					ftpServerContext.removeListener(interfaceName);
				}
			}
		}	
	}
	
	private void checkFTPSHasCertificate(Map<String, String> properties) {
		if(FTPProtocol.FTPS.name().equals(properties.get("ftpProtocol")) && StringUtils.isEmpty(properties.get("ftpCertificate"))){
			throw new IllegalStateException(I18N.getResource(getCurrentLocale(), FTPInterfaceResourceServiceImpl.RESOURCE_BUNDLE, "ftps.certificate.missing", new Object[0]));
		}
	}
	
	private void checkPassivePortRange(String passivePorts){
		try{
			new PassivePorts(passivePorts, true);
		}catch(IllegalArgumentException e){
			throw new IllegalArgumentException(I18N.getResource(getCurrentLocale(), FTPInterfaceResourceServiceImpl.RESOURCE_BUNDLE, "ftp.port.range.large", passivePorts));
		}
	}

}
