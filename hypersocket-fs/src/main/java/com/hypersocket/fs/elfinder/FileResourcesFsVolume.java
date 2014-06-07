package com.hypersocket.fs.elfinder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsVolume;
import cn.bluejoe.elfinder.util.MimeTypesUtils;

import com.hypersocket.fs.FileResource;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.permissions.AccessDeniedException;

public class FileResourcesFsVolume implements FsVolume {

	public static final String HTTP_PROTOCOL = "HTTP";

	private static Logger log = LoggerFactory
			.getLogger(FileResourcesFsVolume.class);

	FileResourceService fileResourceService;
	FileResource resource;

	FileResourceFsItem rootItem;
	public FileResourcesFsVolume(FileResource resource, FileResourceService fileResourceService) throws IOException {
		this.resource = resource;
		this.fileResourceService = fileResourceService;
		
		FileObject file = fileResourceService.resolveMountFile(resource);
		rootItem = new FileResourceFsItem(this, file, resource, file);
	}
	
	private FileResourceFsItem getMountItem(FsItem fsi) {
		return (FileResourceFsItem) fsi;
	}

	@Override
	public void createFile(FsItem fsi) throws IOException {

		FileResourceFsItem item = getMountItem(fsi);

		if (log.isDebugEnabled()) {
			log.debug("createFile " + item.getPath());
			;
		}

		item.getFileObject().createFile();
	}

	@Override
	public void createFolder(FsItem fsi) throws IOException {

		try {
			FileResourceFsItem item = getMountItem(fsi);

			if (log.isDebugEnabled()) {
				log.debug("createFolder " + item.getPath());
			}

			fileResourceService.createFolder(item.getParent().getPath(),
					item.getFilename(), HTTP_PROTOCOL);
		} catch (AccessDeniedException e) {
			throw new IOException(e);
		}

	}

	@Override
	public void deleteFile(FsItem fsi) throws IOException {
		deleteFolder(fsi);
	}

	@Override
	public void deleteFolder(FsItem fsi) throws IOException {

		try {
			FileResourceFsItem item = getMountItem(fsi);

			if (log.isDebugEnabled()) {
				log.debug("deleteFolder " + item.getPath());
			}

			fileResourceService.deleteFile(item.getPath(), HTTP_PROTOCOL);
		} catch (AccessDeniedException e) {
			throw new IOException(e);
		}

	}

	@Override
	public boolean exists(FsItem newFile) {
		if (isRoot(newFile)) {
			return true;
		} else {
			FileResourceFsItem mount = getMountItem(newFile);

			if (log.isDebugEnabled()) {
				log.debug("exists " + mount.getPath());
			}
			try {
				return mount.getFileObject().exists();
			} catch (FileSystemException e) {
				log.error("Error in exists " + mount.getPath(), e);
			}
			return false;
		}
	}

	@Override
	public FsItem fromPath(String relativePath) {

		if (log.isDebugEnabled()) {
			log.debug("fromPath " + relativePath);
		}

		if (relativePath.equals(rootItem.getPath())) {
			return rootItem;
		} else {

			try {
				FileResource mount = fileResourceService
						.getMountForPath(relativePath);
				FileObject mountFile = fileResourceService
						.resolveMountFile(mount);
				String childPath = fileResourceService.resolveChildPath(mount,
						relativePath);
				FileObject file = mountFile.resolveFile(childPath);
				return new FileResourceFsItem(this, file, mount, mountFile);
			} catch (IOException e) {
				log.error("Failed to create FsItem for " + relativePath, e);
			}
			return null;
		}
	}

	@Override
	public String getDimensions(FsItem fsi) {

		FileResourceFsItem item = getMountItem(fsi);

		if (log.isDebugEnabled()) {
			log.debug("getDimentions " + item.getPath());
		}
		return null;
	}

	@Override
	public long getLastModified(FsItem fsi) {
		if (isRoot(fsi)) {
			return rootItem.getMount().getModifiedDate().getTime();
		} else {
			FileResourceFsItem mount = getMountItem(fsi);

			if (log.isDebugEnabled()) {
				log.debug("getLastModified " + mount.getPath());
			}
			try {
				return mount.getFileObject().getContent().getLastModifiedTime();
			} catch (FileSystemException e) {
				log.error("Could not getLastModified " + mount.getPath(), e);
			}
			return 0;
		}
	}

	@Override
	public String getMimeType(FsItem fsi) {
		if (isRoot(fsi)) {
			return "directory";
		}
		FileResourceFsItem mount = getMountItem(fsi);

		if (log.isDebugEnabled()) {
			log.debug("getMimeType " + mount.getPath());
		}

		try {
			if (mount.getFileObject().getType() == FileType.FOLDER) {
				return "directory";
			}
		} catch (FileSystemException e) {
		}

		String ext = FilenameUtils.getExtension(mount.getFileObject().getName()
				.getBaseName());
		if (ext != null && !ext.isEmpty()) {
			String mimeType = MimeTypesUtils.getMimeType(ext);
			return mimeType == null ? MimeTypesUtils.UNKNOWN_MIME_TYPE
					: mimeType;
		}

		return MimeTypesUtils.UNKNOWN_MIME_TYPE;
	}

	@Override
	public String getName() {
		return rootItem.getMount().getName();
	}

	@Override
	public String getName(FsItem fsi) {
		if (isRoot(fsi)) {
			return getName();
		} else {
			FileResourceFsItem mount = getMountItem(fsi);

			if (log.isDebugEnabled()) {
				log.debug("getName " + mount.getPath());
			}

			if (mount.isMountRoot()) {
				return mount.getMount().getName();
			} else {
				return mount.getFileObject().getName().getBaseName();
			}
		}
	}

	@Override
	public FsItem getParent(FsItem fsi) {
		if (isRoot(fsi)) {
			return null;
		} else {
			FileResourceFsItem mount = getMountItem(fsi);

			if (log.isDebugEnabled()) {
				log.debug("getParent " + mount.getPath());
			}
			FileObject obj;
			try {
				obj = fileResourceService.resolveMountFile(mount.getMount());
				if (obj.equals(mount.getFileObject())) {
					return rootItem;
				} else {
					return new FileResourceFsItem(this, mount.getFileObject()
							.getParent(), mount.getMount(), obj);
				}
			} catch (IOException e) {
				log.error("Failed to getParent for " + mount.getPath());
			}
			return null;
		}
	}

	@Override
	public String getPath(FsItem fsi) throws IOException {
		if (isRoot(fsi)) {
			return rootItem.getPath();
		} else {
			FileResourceFsItem item = getMountItem(fsi);

			if (log.isDebugEnabled()) {
				log.debug("getPath " + item.getPath());
			}

			return item.getPath();
		}
	}

	@Override
	public FsItem getRoot() {
		return rootItem;
	}

	@Override
	public long getSize(FsItem fsi) {
		if (isRoot(fsi)) {
			return 0;
		} else {
			FileResourceFsItem mount = getMountItem(fsi);

			if (log.isDebugEnabled()) {
				log.debug("getSize " + mount.getPath());
			}
			try {
				if (mount.getFileObject().getType() == FileType.FOLDER) {
					return 0;
				} else {
					return mount.getFileObject().getContent().getSize();
				}
			} catch (FileSystemException e) {
				log.error("Failed to getSize for " + mount.getPath(), e);
			}
			return 0;
		}
	}

	@Override
	public String getThumbnailFileName(FsItem fsi) {

		FileResourceFsItem mount = getMountItem(fsi);

		if (log.isDebugEnabled()) {
			log.debug("getThumbnailFileName " + mount.getPath());
		}

		return null;
	}

	@Override
	public boolean hasChildFolder(FsItem fsi) {

		FileResourceFsItem mount = getMountItem(fsi);

		if (log.isDebugEnabled()) {
			log.debug("hasChildFolder " + mount.getPath());
		}

		try {
			for (FileObject obj : mount.getFileObject().getChildren()) {
				if (obj.getType() == FileType.FOLDER) {
					return true;
				}
			}
		} catch (FileSystemException e) {
			log.error(
					"Failed to determine children for " + mount.getPath(),
					e);
		}
		return false;
		
	}

	@Override
	public boolean isFolder(FsItem fsi) {
		if (isRoot(fsi)) {
			return true;
		} else {
			FileResourceFsItem mountItem = getMountItem(fsi);

			if (log.isDebugEnabled()) {
				log.debug("isFolder " + mountItem.getPath());
			}
			try {
				return mountItem.getFileObject().getType() == FileType.FOLDER;
			} catch (FileSystemException e) {
				return false;
			}
		}
	}

	@Override
	public boolean isRoot(FsItem fsi) {
		return fsi.equals(rootItem);
	}

	@Override
	public FsItem[] listChildren(FsItem fsi) {

		ArrayList<FsItem> items = new ArrayList<FsItem>();


		FileResourceFsItem mount = getMountItem(fsi);

		if (log.isDebugEnabled()) {
			log.debug("listChildren " + mount.getPath());
		}
		FileObject file = mount.getFileObject();
		try {
			for (FileObject obj : file.getChildren()) {
				if (!obj.getName().getBaseName().equals(".")
						&& !obj.getName().getBaseName().equals("..")) {

					if (log.isDebugEnabled()) {
						log.debug("Adding child "
								+ obj.getName().getBaseName());
					}
					items.add(new FileResourceFsItem(this, obj, mount.getMount(),
							mount.getMountFile()));
				}
			}
		} catch (Exception e) {
			log.error("Failed to listChildren for " + mount.getPath(), e);
		}
		

		return items.toArray(new FsItem[0]);
	}

	@Override
	public InputStream openInputStream(FsItem fsi) throws IOException {
		try {
			FileResourceFsItem mount = getMountItem(fsi);

			if (log.isDebugEnabled()) {
				log.debug("openInputStream " + mount.getPath());
			}
			return fileResourceService.downloadFile(
					mount.getPath(), 0, HTTP_PROTOCOL);
		} catch (AccessDeniedException e) {
			throw new IOException(e);
		}
	}

	@Override
	public OutputStream openOutputStream(FsItem fsi) throws IOException {
		try {
			FileResourceFsItem mount = getMountItem(fsi);

			if (log.isDebugEnabled()) {
				log.debug("openOutputStream " + mount.getPath());
			}
			
			return fileResourceService.uploadFile(mount.getPath(),
					0, HTTP_PROTOCOL);
		} catch (AccessDeniedException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void rename(FsItem src, FsItem dst) throws IOException {

		try {
			FileResourceFsItem mount = getMountItem(src);
			FileResourceFsItem mountDest = getMountItem(dst);
			if (log.isDebugEnabled()) {
				log.debug("openOutputStream " + mount.getPath());
			}
			
			fileResourceService.renameFile(mount.getPath(),
					mountDest.getPath(), HTTP_PROTOCOL);
		} catch (AccessDeniedException e) {
			throw new IOException(e);
		}
	}

}
