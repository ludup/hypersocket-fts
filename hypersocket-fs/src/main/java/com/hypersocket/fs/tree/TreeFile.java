package com.hypersocket.fs.tree;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;

import com.hypersocket.util.FileUtils;

public class TreeFile {

	String id;
	String label;
	boolean isFolder = false;
	String icon = "file";
	String size;
	String type;
	String lastModified;
	String parent;
	
	static ConfigurableMimeFileTypeMap mimeTypesMap = new ConfigurableMimeFileTypeMap();
	
	public TreeFile(FileObject file, FileObject mount) throws FileSystemException {
		this(file, mount, false);
	}
	
	public TreeFile(FileObject file, FileObject mount, boolean open) throws FileSystemException {	
		this.id = mount.getName().getRelativeName(file.getName());
		this.label = file.getName().getBaseName();
		this.type = mimeTypesMap.getContentType(file.getName().getBaseName()); //TODO format
		this.size = FileUtils.formatSize(file.getContent().getSize());
		this.lastModified = FileUtils.formatLastModified(file.getContent().getLastModifiedTime());
		this.parent =  mount.getName().getRelativeName(file.getParent().getName());
	}
	
	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@JsonProperty("isFolder")
	public boolean isFolder() {
		return isFolder;
	}

	public void setFolder(boolean isFolder) {
		this.isFolder = isFolder;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getLastModified() {
		return lastModified;
	}

	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

}
