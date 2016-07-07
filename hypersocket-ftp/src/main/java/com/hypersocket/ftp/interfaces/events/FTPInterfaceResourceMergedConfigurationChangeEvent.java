package com.hypersocket.ftp.interfaces.events;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.certificates.CertificateResource;
import com.hypersocket.certificates.CertificateResourceRepository;
import com.hypersocket.ftp.interfaces.FTPInterfaceResource;
import com.hypersocket.ftp.interfaces.FTPProtocol;
import com.hypersocket.properties.ResourceUtils;
import com.hypersocket.session.Session;
import com.hypersocket.util.SpringApplicationContextProvider;

public class FTPInterfaceResourceMergedConfigurationChangeEvent extends
		FTPInterfaceResourceEvent {

	private static final long serialVersionUID = -2602672997405434974L;
	
	private List<PropertyChangeAndTemplate> changes;
	private ChangeLog changeLog;
	
	public static final String EVENT_RESOURCE_KEY = "ftpInterface.all.changes";

	public FTPInterfaceResourceMergedConfigurationChangeEvent(Object source,
			Session session, FTPInterfaceResource resource, List<PropertyChangeAndTemplate> changes) {
		super(source, EVENT_RESOURCE_KEY, session, resource);
		this.changes = changes;
		this.changeLog = new ChangeLog();
		this.changeLog.compute(changes, resource);
	}

	public FTPInterfaceResourceMergedConfigurationChangeEvent(Object source,
			FTPInterfaceResource resource, Throwable e, Session session, List<PropertyChangeAndTemplate> changes) {
		super(source, EVENT_RESOURCE_KEY, resource, e, session);
		this.changes = changes;
	}

	public List<PropertyChangeAndTemplate> getChanges() {
		return changes;
	}

	public void setChanges(List<PropertyChangeAndTemplate> changes) {
		this.changes = changes;
	}
	
	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
	
	public ChangeLog getChangeLog(){
		return this.changeLog;
	}
	
	/**
	 * This class computes which properties changed and these change will require which listeners to dispose
	 * or create new.
	 * 
	 *  Dispose -> when port, timeout, interface removed, passive external
	 *  Create -> new interface, or due to dispose we need to create a new interface
	 *
	 */
	public static class ChangeLog {
		
		boolean isPortChange;
		boolean isIdelTimeOutChange;
		boolean isPassiveExternalAddressChange;
		boolean isPassivePortChange;
		boolean isInterfaceChange;
		boolean isProtocolChange;
		boolean isCertificateChange;
		
		
		FTPInterfaceResource toCreateFtpInterfaceResource; 
		FTPInterfaceResource toDeleteFtpInterfaceResource; 

		
		public void compute(List<PropertyChangeAndTemplate> changes, FTPInterfaceResource resource){
			
			Set<String> oldInterfaces = null;
			Set<String> newInterfaces = null;
			
			toCreateFtpInterfaceResource = new FTPInterfaceResource();
			cloneToResource(resource,toCreateFtpInterfaceResource);
			
			toDeleteFtpInterfaceResource = new FTPInterfaceResource();
			cloneToResource(resource,toDeleteFtpInterfaceResource);
			
			for (PropertyChangeAndTemplate propertyChangeAndTemplate : changes) {
				
				String resourceKey = propertyChangeAndTemplate.getPropertyTemplate().getResourceKey();
				String[] newValues = ResourceUtils.explodeValues(propertyChangeAndTemplate.getPropertyChange().getNewValue());
				String[] oldValues = ResourceUtils.explodeValues(propertyChangeAndTemplate.getPropertyChange().getOldValue());
				
				if("ftpPort".equals(resourceKey)){
					isPortChange = true;
					toCreateFtpInterfaceResource.setFtpPort(Integer.parseInt(newValues[0]));
					toDeleteFtpInterfaceResource.setFtpPort(Integer.parseInt(oldValues[0]));
				}else if("ftpIdleTimeout".equals(resourceKey)){
					isIdelTimeOutChange = true;
					toCreateFtpInterfaceResource.setFtpIdleTimeout(Integer.parseInt(newValues[0]));
				}else if("ftpPassiveExternalAddress".equals(resourceKey)){
					isPassiveExternalAddressChange = true;
					toCreateFtpInterfaceResource.setFtpPassiveExternalAddress(newValues[0]);
				}else if("ftpPassivePorts".equals(resourceKey)){
					isPassivePortChange = true;
					toCreateFtpInterfaceResource.setFtpPassivePorts(newValues[0]);
				}else if("ftpInterfaces".equals(resourceKey)){
					isInterfaceChange = true;
					oldInterfaces = new HashSet<String>(Arrays.asList(oldValues));
					newInterfaces = new HashSet<String>(Arrays.asList(newValues));
					toCreateFtpInterfaceResource.setFtpInterfaces(propertyChangeAndTemplate.getPropertyChange().getNewValue());
					toDeleteFtpInterfaceResource.setFtpInterfaces(propertyChangeAndTemplate.getPropertyChange().getNewValue());
				}else if("ftpProtocol".equals(resourceKey)){
					isPortChange = true;
					toCreateFtpInterfaceResource.setFtpProtocol(FTPProtocol.valueOf(newValues[0]));
				}else if("ftpCertificate".equals(resourceKey)){
					isProtocolChange = true;
					CertificateResourceRepository repository = SpringApplicationContextProvider.getApplicationContext().getBean(CertificateResourceRepository.class);
					CertificateResource certificateResource = repository.getResourceById(Long.parseLong(newValues[0]));
					toCreateFtpInterfaceResource.setFtpCertificate(certificateResource);
				}
				
			}
			
			
			/**
			 * There is no change other than interfaces, compute added and discarded, others require no change.
			 * In first case if we can limit the damage of creating all new, all other cases old ones have to go.
			 * If there is any change in port, passive or idle timeout, we need to discard all old ones and re create new ones
			 */
			if(isInterfaceChange && !(isPortChange || isIdelTimeOutChange || isPassiveExternalAddressChange || isPassivePortChange || isPortChange || isCertificateChange)){
				Set<String> interfacesToDispose = new HashSet<>();
				for (String oldInterface : oldInterfaces) {
					if(newInterfaces.contains(oldInterface)){
						newInterfaces.remove(oldInterface);
					}else{
						interfacesToDispose.add(oldInterface);
					}
				}
				toDeleteFtpInterfaceResource.setFtpInterfaces(ResourceUtils.implodeValues(interfacesToDispose));
				toCreateFtpInterfaceResource.setFtpInterfaces(ResourceUtils.implodeValues(newInterfaces));
			}else{
				if(oldInterfaces != null){
					toDeleteFtpInterfaceResource.setFtpInterfaces(ResourceUtils.implodeValues(oldInterfaces));
				}
			}
		}

		public FTPInterfaceResource getToCreateFtpInterfaceResource() {
			return toCreateFtpInterfaceResource;
		}
		
		public FTPInterfaceResource getToDeleteFtpInterfaceResource() {
			return toDeleteFtpInterfaceResource;
		}
		
		private void cloneToResource(FTPInterfaceResource resource, FTPInterfaceResource resourceClone) {
			resourceClone.setCreatedDate(resource.getCreateDate());
			resourceClone.setDeleted(resource.isDeleted());
			resourceClone.setFtpCertificate(resource.getFtpCertificate());
			resourceClone.setFtpIdleTimeout(resource.getFtpIdleTimeout());
			resourceClone.setFtpInterfaces(resource.getFtpInterfaces());
			resourceClone.setFtpPassiveExternalAddress(resource.getFtpPassiveExternalAddress());
			resourceClone.setFtpPassivePorts(resource.getFtpPassivePorts());
			resourceClone.setFtpPort(resource.getFtpPort());
			resourceClone.setFtpProtocol(resource.getFtpProtocol());
			resourceClone.setHidden(resource.isHidden());
			resourceClone.setId(resource.getId());
			resourceClone.setName(resource.getName());
		}
	}
}
