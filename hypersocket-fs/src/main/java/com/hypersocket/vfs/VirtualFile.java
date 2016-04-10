package com.hypersocket.vfs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.hypersocket.fs.FileResource;
import com.hypersocket.realm.Realm;
import com.hypersocket.repository.AbstractEntity;

@Entity
@Table(name="virtual_fs")
public class VirtualFile extends AbstractEntity<Long> {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
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
	String virtualPath;
	
	@Column(name="filename")
	String filename;
	
	@Column(name="display_name")
	String displayName;
	
	@Column(name="writable")
	Boolean writable;
	
	@OneToOne
	FileResource mount;
	
	@Column(name="conflicted")
	Boolean conflicted;
	
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
		return mount;
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
		return type==VirtualFileType.FOLDER || type==VirtualFileType.MOUNTED_FOLDER;
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
	
	
}
