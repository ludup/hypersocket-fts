package upgrade;

import java.util.Date;
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

public class ftp_0_DOT_0_DOT_1 implements Runnable {

	static Logger log = LoggerFactory.getLogger(ftp_0_DOT_0_DOT_1.class);
	
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
	
		Date now = new Date();
		log.info("Adding default ftp and ftps interfaces");
		
		FTPInterfaceResource ftp = new FTPInterfaceResource();
		ftp.setCreatedDate(now);
		ftp.setDeleted(false);
		ftp.setFtpIdleTimeout(500);
		ftp.setFtpInterfaces("127.0.0.1");
		ftp.setFtpPassivePorts("60000-61000");
		ftp.setFtpPort(9021);
		ftp.setFtpProtocol(FTPProtocol.FTP);
		ftp.setHidden(false);
		ftp.setName("DefaultFTP");
		ftp.setRealm(realmRepository.getDefaultRealm());
		
		ftpInterfaceResourceRepository.saveResource(ftp, new HashMap<String, String>());
		
		CertificateResource certificate = certificateResourceRepository.getResourceByName(CertificateResourceServiceImpl.DEFAULT_CERTIFICATE_NAME, realmRepository.getDefaultRealm());
		
		FTPInterfaceResource ftps = new FTPInterfaceResource();
		ftps.setCreatedDate(now);
		ftps.setDeleted(false);
		ftps.setFtpIdleTimeout(500);
		ftps.setFtpInterfaces("127.0.0.1");
		ftps.setFtpPassivePorts("60000-61000");
		ftps.setFtpPort(9022);
		ftps.setFtpProtocol(FTPProtocol.FTPS);
		ftps.setHidden(false);
		ftps.setName("DefaultFTPS");
		ftps.setRealm(realmRepository.getDefaultRealm());
		ftps.setFtpCertificate(certificate);
		
		ftpInterfaceResourceRepository.saveResource(ftps, new HashMap<String, String>());
	}

}
