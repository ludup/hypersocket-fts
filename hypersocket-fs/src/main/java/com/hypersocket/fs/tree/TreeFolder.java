package com.hypersocket.fs.tree;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hypersocket.fs.FileResource;
import com.hypersocket.utils.FileUtils;

public class TreeFolder {

	static Logger log = LoggerFactory.getLogger(TreeFolder.class);
	
	private String id;
	private String label;
	private boolean isFolder = true;
	private boolean open = false;
	private String icon = "folder";
	private String lastModified;
	private String type = "Folder";
	private String size = "--";
	private String parent;
	
	private List<TreeFolder> childs = new ArrayList<TreeFolder>();
	
	public TreeFolder(FileObject file, FileObject mount, FileResource resource) throws FileSystemException {
		this(file, mount, resource, false);
	}
	
	public TreeFolder(FileObject file, FileObject mount, FileResource resource, boolean open) throws FileSystemException {	
		this.id = mount.getName().getRelativeName(file.getName());
		this.label = file.getName().getBaseName();
		this.lastModified = FileUtils.formatLastModified(file.getContent().getLastModifiedTime());
		this.open = open;
		this.parent = mount.getName().getRelativeName(file.getParent().getName());
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

	public boolean isOpen() {
		return open;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public List<TreeFolder> getChilds() {
		return childs;
	}

	public void setChilds(List<TreeFolder> childs) {
		this.childs = childs;
	}

	public String getLastModified() {
		return lastModified;
	}

	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}
	
	
}
