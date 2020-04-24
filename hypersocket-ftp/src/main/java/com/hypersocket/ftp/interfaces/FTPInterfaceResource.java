package com.hypersocket.ftp.interfaces;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.hypersocket.certificates.CertificateResource;
import com.hypersocket.realm.Realm;
import com.hypersocket.server.interfaces.InterfaceResource;

@Entity
@Table(name = "ftp_interfaces")
public class FTPInterfaceResource extends InterfaceResource {

	private static final long serialVersionUID = -7196348246139661166L;

	@Column(name = "protocol")
	private FTPProtocol ftpProtocol;

	@Column(name = "passive_ports")
	private String ftpPassivePorts;

	@Column(name = "passive_external_address")
	private String ftpPassiveExternalAddress;

	@Column(name = "idle_timeout")
	private Integer ftpIdleTimeout;

	@OneToOne
	private CertificateResource ftpCertificate;

	@ManyToOne
	@JoinColumn(name = "realm_id", foreignKey = @ForeignKey(name = "ftp_interfaces_cascade_1"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	protected Realm realm;

	@Override
	protected Realm doGetRealm() {
		return realm;
	}

	@Override
	public void setRealm(Realm realm) {
		this.realm = realm;
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

	@Override
	protected Integer getDefaultPort() {
		return 21;
	}
}
