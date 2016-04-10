package com.hypersocket.vfs.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hypersocket.vfs.VirtualFileType;

public class TreeNode {

	String id;
	String parent;
	String text;
	String icon;
	String virtualPath;
	TreeState state = new TreeState();
	List<TreeNode> children = new ArrayList<TreeNode>();
	VirtualFileType fileType; 
	Long resourceId;
	String type;
	
	@JsonIgnore
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Long getResourceId() {
		return resourceId;
	}
	
	public void setResourceId(Long resourceId) {
		this.resourceId = resourceId;
	}

	public String getParent() {
		return parent;
	}


	public void setParent(String parent) {
		this.parent = parent;
	}


	public String getText() {
		return text;
	}


	public void setText(String text) {
		this.text = text;
	}


//	public String getIcon() {
//		return icon;
//	}
//
//
//	public void setIcon(String icon) {
//		this.icon = icon;
//	}


	public TreeState getState() {
		return state;
	}


	public void setState(TreeState state) {
		this.state = state;
	}


	public List<TreeNode> getChildren() {
		return children.isEmpty() ? null : children;
	}


	public void setChildren(List<TreeNode> children) {
		this.children = children;
	}

	

	public VirtualFileType getFileType() {
		return fileType;
	}


	public void setFileType(VirtualFileType fileType) {
		this.fileType = fileType;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getVirtualPath() {
		return virtualPath;
	}

	public void setVirtualPath(String virtualPath) {
		this.virtualPath = virtualPath;
	}



	class TreeState {
		boolean opened;
		boolean disabled;
		boolean selected;
		public boolean isOpened() {
			return opened;
		}
		public void setOpened(boolean opened) {
			this.opened = opened;
		}
		public boolean isDisabled() {
			return disabled;
		}
		public void setDisabled(boolean disabled) {
			this.disabled = disabled;
		}
		public boolean isSelected() {
			return selected;
		}
		public void setSelected(boolean selected) {
			this.selected = selected;
		}
		
		
	}
}
