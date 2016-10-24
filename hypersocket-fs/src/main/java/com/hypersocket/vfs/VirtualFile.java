package com.hypersocket.vfs;


import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hypersocket.fs.FileResource;
import com.hypersocket.realm.Principal;
import com.hypersocket.realm.Realm;
import com.hypersocket.repository.AbstractEntity;

@Entity
@Table(name="virtual_fs")
public class VirtualFile extends AbstractEntity<Long> {

	@Id
	@GeneratedValue(strategy=GenerationType.TABLE)
	@Column(name="id")
	Long id;

	@OneToOne
	Realm realm;
	
	@Column(name="file_type")
	VirtualFileType type;
	
	@OneToOne
	VirtualFile parent;
	
	@Column(name="size")
	Long size;
	
	@Column(name="last_modified")
	Long lastModified;
	
	@Column(name="virtual_path")
	@Lob
	String virtualPath;
	
	@Column(name="filename", length=256)
	String filename;
	
	@Column(name="display_name", length=512)
	String displayName;
	
	@Column(name="writable")
	Boolean writable;
	
	@OneToOne
	FileResource mount;
	
	@ManyToMany(fetch=FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	@JoinTable(name = "virtual_fs_mounts", joinColumns={@JoinColumn(name="resource_id")}, 
			inverseJoinColumns={@JoinColumn(name="mount_id")})
	Set<FileResource> folderMounts;
	
	@Column(name="conflicted")
	Boolean conflicted;
	
	@Column(name="sync")
	Boolean sync;
	
	@OneToOne
	FileResource defaultMount;
	
	@OneToOne
	Principal principal;
	
	@Column(name="hash")
	int hash;
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}

	public Realm getRealm() {
		return realm;
	}

	public void setRealm(Realm realm) {
		this.realm = realm;
	}

	public VirtualFileType getType() {
		return type;
	}

	public void setType(VirtualFileType type) {
		this.type = type;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public Boolean getWritable() {
		return writable == null ? Boolean.FALSE : writable;
	}

	public void setWritable(Boolean writable) {
		this.writable = writable;
	}

	public Long getLastModified() {
		return lastModified;
	}

	public void setLastModified(Long lastModified) {
		this.lastModified = lastModified;
	}

	public String getVirtualPath() {
		return virtualPath;
	}

	public void setVirtualPath(String virtualPath) {
		this.virtualPath = virtualPath;
	}

	public FileResource getMount() {
		return mount == null ? getDefaultMount() : mount;
	}

	public void setMount(FileResource mount) {
		this.mount = mount;
	}

	public VirtualFile getParent() {
		return parent;
	}

	public void setParent(VirtualFile parent) {
		this.parent = parent;
	}

	public int getHash() {
		return hash;
	}

	public void setHash(int hash) {
		this.hash = hash;
	}

	public boolean isMounted() {
		return mount!=null;
	}

	public boolean isHidden() {
		return filename.startsWith(".");
	}
	
	public boolean isFolder() {
		return type==VirtualFileType.FOLDER || type==VirtualFileType.MOUNTED_FOLDER || type==VirtualFileType.ROOT;
	}
	
	public boolean isFile() {
		return type==VirtualFileType.FILE;
	}
	
	public boolean isMount() {
		return type==VirtualFileType.MOUNTED_FOLDER;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Boolean getConflicted() {
		return conflicted!=null && conflicted.booleanValue();
	}

	public void setConflicted(Boolean conflicted) {
		this.conflicted = conflicted;
	}

	public void setDefaultMount(FileResource defaultMount) {
		this.defaultMount = defaultMount;
	}
	
	public FileResource getDefaultMount() {
		return defaultMount;
	}

	public void setPrincipal(Principal principal) {
		this.principal = principal;
	}
	
	@JsonIgnore
	public Principal getPrincipal() {
		return principal;
	}

	public Boolean getSync() {
		return sync==null ? Boolean.FALSE : sync;
	}

	public void setSync(Boolean sync) {
		this.sync = sync;
	}
	
	@JsonIgnore
	public Set<FileResource> getFolderMounts() {
		return folderMounts;
	}
	
	public boolean isVirtualFolder() {
		return mount==null;
	}
	
}
