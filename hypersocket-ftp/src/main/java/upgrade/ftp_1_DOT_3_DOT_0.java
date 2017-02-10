package upgrade;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hypersocket.certificates.CertificateResource;
import com.hypersocket.certificates.CertificateResourceRepository;
import com.hypersocket.certificates.CertificateResourceService;
import com.hypersocket.certificates.CertificateResourceServiceImpl;
import com.hypersocket.ftp.interfaces.FTPInterfaceResource;
import com.hypersocket.ftp.interfaces.FTPInterfaceResourceRepository;
import com.hypersocket.ftp.interfaces.FTPProtocol;
import com.hypersocket.realm.RealmRepository;
import com.hypersocket.resource.ResourceException;

public class ftp_1_DOT_3_DOT_0 implements Runnable {

	static Logger log = LoggerFactory.getLogger(ftp_1_DOT_3_DOT_0.class);
	
	@Autowired
	FTPInterfaceResourceRepository ftpInterfaceResourceRepository; 
	
	@Autowired
	RealmRepository realmRepository;
	
	@Autowired
	CertificateResourceService certificateResourceService;
	
	@Autowired
	CertificateResourceRepository certificateResourceRepository; 
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
	
		try {
			log.info("Adding default ftp and ftps interfaces");
			
			FTPInterfaceResource ftp = new FTPInterfaceResource();
			ftp.setDeleted(false);
			ftp.setFtpIdleTimeout(500);
			ftp.setAllInterfaces(Boolean.TRUE);
			ftp.setFtpPassivePorts("60000-60999");
			ftp.setPort(9021);
			ftp.setFtpProtocol(FTPProtocol.FTP);
			ftp.setHidden(false);
			ftp.setName("Default FTP");
			ftp.setRealm(realmRepository.getRealmByName("System"));
			
			ftpInterfaceResourceRepository.saveResource(ftp, new HashMap<String, String>());
			
			CertificateResource certificate = certificateResourceRepository.getResourceByName(CertificateResourceServiceImpl.DEFAULT_CERTIFICATE_NAME, realmRepository.getDefaultRealm());
			
			FTPInterfaceResource ftps = new FTPInterfaceResource();
			ftps.setDeleted(false);
			ftps.setFtpIdleTimeout(500);
			ftp.setAllInterfaces(Boolean.TRUE);
			ftps.setFtpPassivePorts("61000-61999");
			ftps.setPort(9022);
			ftps.setFtpProtocol(FTPProtocol.FTPS);
			ftps.setHidden(false);
			ftps.setName("Default FTPS");
			ftps.setRealm(realmRepository.getRealmByName("System"));
			ftps.setFtpCertificate(certificate);
			
			ftpInterfaceResourceRepository.saveResource(ftps, new HashMap<String, String>());
		} catch (ResourceException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

}
