package com.hypersocket.vfs.json;

import java.io.Serializable;

public class FileView implements Serializable {

	private static final long serialVersionUID = 887348472822993221L;
	
	private String fileName;
	private Long fileSize;
	
	public FileView(String fileName, Long fileSize) {
		super();
		this.fileName = fileName;
		this.fileSize = fileSize;
	}

	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public Long getFileSize() {
		return fileSize;
	}
	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	
}
