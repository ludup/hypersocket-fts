package com.hypersocket.fs.tree;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hypersocket.fs.FileResource;
import com.hypersocket.util.FileUtils;

public class TreeFolderParent {

	static Logger log = LoggerFactory.getLogger(TreeFolderParent.class);
	
	String id;
	String label;
	boolean isFolder = true;
	boolean open = false;
	String icon = "folder";
	String lastModified;
	String type = "Folder";
	String size = "--";
	List<TreeFolderParent> childs = new ArrayList<TreeFolderParent>();
	
	public TreeFolderParent(FileObject file, FileObject mount, FileResource resource) throws FileSystemException {
		this.id = mount.getName().getRelativeName(file.getParent().getName());
		this.label = "\u2191";
		this.lastModified = FileUtils.formatLastModified(file.getParent().getContent().getLastModifiedTime());
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

	public List<TreeFolderParent> getChilds() {
		return childs;
	}

	public void setChilds(List<TreeFolderParent> childs) {
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
	
	
}
