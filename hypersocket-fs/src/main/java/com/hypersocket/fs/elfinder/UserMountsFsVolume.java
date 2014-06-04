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

public class UserMountsFsVolume implements FsVolume {

	public static final String HTTP_PROTOCOL = "HTTP";

	private static Logger log = LoggerFactory
			.getLogger(UserMountsFsVolume.class);

	FileResourceService fileResourceService;
	FileResource resource;

	MountFsItem rootItem;
	public UserMountsFsVolume(FileResource resource, FileResourceService fileResourceService) throws IOException {
		this.resource = resource;
		this.fileResourceService = fileResourceService;
		
		FileObject file = fileResourceService.resolveMountFile(resource);
		rootItem = new MountFsItem(this, file, resource, file);
	}
	
	private MountFsItem getMountItem(FsItem fsi) {
		return (MountFsItem) fsi;
	}

	@Override
	public void createFile(FsItem fsi) throws IOException {

		MountFsItem item = getMountItem(fsi);

		if (log.isInfoEnabled()) {
			log.info("createFile " + item.getPath());
			;
		}

		item.getFileObject().createFile();
	}

	@Override
	public void createFolder(FsItem fsi) throws IOException {

		try {
			MountFsItem item = getMountItem(fsi);

			if (log.isInfoEnabled()) {
				log.info("createFolder " + item.getPath());
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
			MountFsItem item = getMountItem(fsi);

			if (log.isInfoEnabled()) {
				log.info("deleteFolder " + item.getPath());
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
			MountFsItem mount = getMountItem(newFile);

			if (log.isInfoEnabled()) {
				log.info("exists " + mount.getPath());
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

		if (log.isInfoEnabled()) {
			log.info("fromPath " + relativePath);
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
				return new MountFsItem(this, file, mount, mountFile);
			} catch (IOException e) {
				log.error("Failed to create FsItem for " + relativePath, e);
			}
			return null;
		}
	}

	@Override
	public String getDimensions(FsItem fsi) {

		MountFsItem item = getMountItem(fsi);

		if (log.isInfoEnabled()) {
			log.info("getDimentions " + item.getPath());
		}
		return null;
	}

	@Override
	public long getLastModified(FsItem fsi) {
		if (isRoot(fsi)) {
			return rootItem.getMount().getModifiedDate().getTime();
		} else {
			MountFsItem mount = getMountItem(fsi);

			if (log.isInfoEnabled()) {
				log.info("getLastModified " + mount.getPath());
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
		MountFsItem mount = getMountItem(fsi);

		if (log.isInfoEnabled()) {
			log.info("getMimeType " + mount.getPath());
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
			MountFsItem mount = getMountItem(fsi);

			if (log.isInfoEnabled()) {
				log.info("getName " + mount.getPath());
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
			MountFsItem mount = getMountItem(fsi);

			if (log.isInfoEnabled()) {
				log.info("getParent " + mount.getPath());
			}
			FileObject obj;
			try {
				obj = fileResourceService.resolveMountFile(mount.getMount());
				if (obj.equals(mount.getFileObject())) {
					return rootItem;
				} else {
					return new MountFsItem(this, mount.getFileObject()
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
			MountFsItem item = getMountItem(fsi);

			if (log.isInfoEnabled()) {
				log.info("getPath " + item.getPath());
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
			MountFsItem mount = getMountItem(fsi);

			if (log.isInfoEnabled()) {
				log.info("getSize " + mount.getPath());
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

		MountFsItem mount = getMountItem(fsi);

		if (log.isInfoEnabled()) {
			log.info("getThumbnailFileName " + mount.getPath());
		}

		return null;
	}

	@Override
	public boolean hasChildFolder(FsItem fsi) {

		MountFsItem mount = getMountItem(fsi);

		if (log.isInfoEnabled()) {
			log.info("hasChildFolder " + mount.getPath());
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
			MountFsItem mountItem = getMountItem(fsi);

			if (log.isInfoEnabled()) {
				log.info("isFolder " + mountItem.getPath());
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


		MountFsItem mount = getMountItem(fsi);

		if (log.isInfoEnabled()) {
			log.info("listChildren " + mount.getPath());
		}
		FileObject file = mount.getFileObject();
		try {
			for (FileObject obj : file.getChildren()) {
				if (!obj.getName().getBaseName().equals(".")
						&& !obj.getName().getBaseName().equals("..")) {

					if (log.isInfoEnabled()) {
						log.info("Adding child "
								+ obj.getName().getBaseName());
					}
					items.add(new MountFsItem(this, obj, mount.getMount(),
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
			MountFsItem mount = getMountItem(fsi);

			if (log.isInfoEnabled()) {
				log.info("openInputStream " + mount.getPath());
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
			MountFsItem mount = getMountItem(fsi);

			if (log.isInfoEnabled()) {
				log.info("openOutputStream " + mount.getPath());
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
			MountFsItem mount = getMountItem(src);
			MountFsItem mountDest = getMountItem(dst);
			if (log.isInfoEnabled()) {
				log.info("openOutputStream " + mount.getPath());
			}
			
			fileResourceService.renameFile(mount.getPath(),
					mountDest.getPath(), HTTP_PROTOCOL);
		} catch (AccessDeniedException e) {
			throw new IOException(e);
		}
	}

}
