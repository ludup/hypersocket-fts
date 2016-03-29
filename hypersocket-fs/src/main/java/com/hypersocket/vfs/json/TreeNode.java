package com.hypersocket.vfs.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TreeNode {

	String id;
	String parent;
	String text;
	String icon;
	TreeState state = new TreeState();
	List<TreeNode> children = new ArrayList<TreeNode>();
	
	@JsonIgnore
	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
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
