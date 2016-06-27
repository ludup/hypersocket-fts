package com.hypersocket.ftp.interfaces;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.hypersocket.certificates.CertificateResource;
import com.hypersocket.resource.RealmResource;

@Entity
@Table(name="fTPInterface_resource")
public class FTPInterfaceResource extends RealmResource {

	private static final long serialVersionUID = -7196348246139661166L;

	@Column(name="interfaces", length=1024)
	String ftpInterfaces;
	
	@Column(name="port")
	Integer ftpPort;
	
	@Column(name="protocol")
	FTPProtocol ftpProtocol;
	
	@Column(name="passive_ports")
	String ftpPassivePorts;
	
	@Column(name="passive_external_address")
	String ftpPassiveExternalAddress;
	
	@Column(name="idle_timeout")
	Integer ftpIdleTimeout;
	
	@OneToOne
	CertificateResource ftpCertificate;

	public String getFtpInterfaces() {
		return ftpInterfaces;
	}

	public void setFtpInterfaces(String ftpInterfaces) {
		this.ftpInterfaces = ftpInterfaces;
	}

	public Integer getFtpPort() {
		return ftpPort;
	}

	public void setFtpPort(Integer ftpPort) {
		this.ftpPort = ftpPort;
	}

	public FTPProtocol getFtpProtocol() {
		return ftpProtocol;
	}

	public void setFtpProtocol(FTPProtocol ftpProtocol) {
		this.ftpProtocol = ftpProtocol;
	}

	public CertificateResource getFtpCertificate() {
		return ftpCertificate;
	}

	public void setFtpCertificate(CertificateResource ftpCertificate) {
		this.ftpCertificate = ftpCertificate;
	}

	public String getFtpPassivePorts() {
		return ftpPassivePorts;
	}

	public void setFtpPassivePorts(String ftpPassivePorts) {
		this.ftpPassivePorts = ftpPassivePorts;
	}

	public String getFtpPassiveExternalAddress() {
		return ftpPassiveExternalAddress;
	}

	public void setFtpPassiveExternalAddress(String ftpPassiveExternalAddress) {
		this.ftpPassiveExternalAddress = ftpPassiveExternalAddress;
	}

	public Integer getFtpIdleTimeout() {
		return ftpIdleTimeout;
	}

	public void setFtpIdleTimeout(Integer ftpIdleTimeout) {
		this.ftpIdleTimeout = ftpIdleTimeout;
	}

}
