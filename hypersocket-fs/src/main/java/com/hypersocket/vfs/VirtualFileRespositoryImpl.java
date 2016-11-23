package com.hypersocket.vfs;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileType;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hypersocket.fs.FileResource;
import com.hypersocket.realm.Principal;
import com.hypersocket.realm.Realm;
import com.hypersocket.realm.RealmRestriction;
import com.hypersocket.repository.AbstractRepositoryImpl;
import com.hypersocket.repository.CriteriaConfiguration;
import com.hypersocket.resource.RealmCriteria;
import com.hypersocket.tables.ColumnSort;
import com.hypersocket.utils.FileUtils;

@Repository
public class VirtualFileRespositoryImpl extends AbstractRepositoryImpl<Long> implements VirtualFileRepository {
	
	static Logger log = LoggerFactory.getLogger(VirtualFileRespositoryImpl.class);
	
	@Override
	@Transactional(readOnly=true)
	public Collection<VirtualFile> getVirtualFilesByResource(VirtualFile parent, Realm realm, Principal principal, FileResource... resources) {
		if(resources.length==0) {
			return Collections.<VirtualFile>emptyList();
		}
		return list("parent", parent, VirtualFile.class, new RealmCriteria(realm), new PrincipalCriteria(principal), new FileResourceCriteria(resources), new ConflictCriteria());
	}
	
	@Override
	@Transactional(readOnly=true)
	public Collection<VirtualFile> getVirtualFiles(VirtualFile parent, Realm realm, Principal principal) {
		return list("parent", parent, VirtualFile.class, new RealmCriteria(realm), new PrincipalCriteria(principal), new ConflictCriteria());
	}

	@Override
	@Transactional(readOnly=true)
	public VirtualFile getVirtualFileByResource(String virtualPath, Realm realm, Principal principal, FileResource... resources) {
		if(resources.length==0) {
			return null;
		}
		return get(VirtualFile.class, new VirtualPathCriteria(virtualPath), new RealmCriteria(realm), new FileResourceCriteria(resources), new PrincipalCriteria(principal), new ConflictCriteria());
	}
	
	@Override
	@Transactional(readOnly=true)
	public VirtualFile getVirtualFile(String virtualPath, Realm realm, Principal principal) {
		return get(VirtualFile.class, new VirtualPathCriteria(virtualPath), new RealmCriteria(realm), new PrincipalCriteria(principal), new ConflictCriteria());
	}
		
	@Override
	@Transactional(readOnly=true)
	public Collection<VirtualFile> getReconciledFiles(VirtualFile parent, Realm realm, Principal principal) {
		return list("parent", parent, VirtualFile.class, new PrincipalCriteria(principal), new ConflictCriteria());
	}
	
	@Override
	@Transactional
	public VirtualFile reconcileNewFolder(String displayName, VirtualFile parent, FileObject fileObject, 
			FileResource resource, boolean conflicted, Principal principal) throws IOException {
		
		String filename = fileObject.getName().getBaseName();
		
		return buildFile(new VirtualFile(),
					displayName,
					FileUtils.checkEndsWithSlash(parent.getVirtualPath() + filename) , 
					VirtualFileType.FOLDER, 
					!resource.isReadOnly(), 
					parent,
					resource,
					conflicted,
					parent.getRealm(),
					fileObject);
	}
	
	@Override
	@Transactional
	public VirtualFile createVirtualFolder(String displayName, VirtualFile parent) throws IOException {
		
		VirtualFile folder = buildFile(new VirtualFile(),
				displayName,
					FileUtils.checkEndsWithSlash(parent.getVirtualPath() + displayName) , 
					VirtualFileType.FOLDER, 
					false, 
					parent,
					null,
					false,
					parent.getRealm(),
					null);
		save(folder);
		return folder;
	}
	
	@Override
	@Transactional
	public VirtualFile reconcileFolder(String displayName, VirtualFile folder, FileObject fileObject, 
			FileResource resource, boolean conflicted, Principal principal) throws IOException {
		
		String filename = fileObject.getName().getBaseName();
		
		return buildFile(folder,
					displayName,
					FileUtils.checkEndsWithSlash(folder.getParent().getVirtualPath() + filename) , 
					VirtualFileType.FOLDER, 
					!resource.isReadOnly(), 
					folder.getParent(),
					resource,
					conflicted,
					folder.getRealm(),
					fileObject);
	}
	
	@Override
	@Transactional
	public VirtualFile getRootFolder(Realm realm) throws IOException {
		VirtualFile rootFile = get("filename", "__ROOT__", VirtualFile.class, new RealmCriteria(realm));
		if(rootFile==null) {
			rootFile = buildFile(new VirtualFile(),
					"__ROOT__",
					"/",
					VirtualFileType.ROOT,
					false,
					null,
					null,
					false,
					realm,
					null);
			saveFile(rootFile);
		}
		return rootFile;
	}
	
	VirtualFile buildFile(VirtualFile file, String displayName, String virtualPath, 
			VirtualFileType type, boolean writable,
			VirtualFile parent, 
			FileResource resource, boolean conflicted,
			Realm realm, FileObject fileObject) throws IOException {
		
		FileContent content = fileObject==null ? null : fileObject.getContent();
		try {
			file.setRealm(realm);
			file.setFilename(fileObject==null ? displayName :fileObject.getName().getBaseName());
			file.setDisplayName(displayName);
			file.setVirtualPath(virtualPath);
			file.setType(type);
			file.setWritable(writable);
			file.setSize(fileObject!=null && fileObject.getType() == FileType.FILE ? content.getSize() : 0L);
			file.setLastModified(content==null ? 
					  file.getLastModified() == null ? System.currentTimeMillis() : file.getLastModified() 
					: content.getLastModifiedTime());
			file.setParent(parent);
			file.setMount(resource);
			file.setConflicted(conflicted);	
			file.setFileObject(fileObject);
		} finally {
			if(content!=null) {
				content.close();
			}	
		}
		return file;
	}

	@Override
	@Transactional
	public void removeReconciledFile(VirtualFile toDelete) {
		delete(toDelete);
	}

	@Override
	@Transactional
	public VirtualFile reconcileFile(String displayName, FileObject obj, FileResource resource, VirtualFile parent, Principal principal) throws IOException {
		return buildFile(new VirtualFile(), 
				displayName,
				FileUtils.checkEndsWithSlash(parent.getVirtualPath()) + obj.getName().getBaseName(), 
				VirtualFileType.FILE, 
				!resource.isReadOnly() && obj.isWriteable(), 
				parent, 
				resource,
				!displayName.equals(obj.getName().getBaseName()),
				resource.getRealm(),
				obj);
	}
	
	@Override
	@Transactional
	public VirtualFile reconcileFile(String displayName, FileObject obj, FileResource resource, VirtualFile virtual, VirtualFile parent, Principal principal) throws IOException {
		return buildFile(virtual, 
				displayName,
				FileUtils.checkEndsWithSlash(parent.getVirtualPath()) + obj.getName().getBaseName(), 
				VirtualFileType.FILE, 
				!resource.isReadOnly() && obj.isWriteable(), 
				parent, 
				resource,
				!displayName.equals(obj.getName().getBaseName()),
				resource.getRealm(),
				obj);
	}
	
	

//	@Override
//	@Transactional
//	public int removeReconciledFolder(VirtualFile toDelete) {
//		
//		int filesToDelete = 0;
//		boolean hasFiles =  false;
//		for(VirtualFile child :  list("parent", toDelete, VirtualFile.class, new ConflictCriteria())) {
//			if(child.isFolder()) {
//				filesToDelete += removeReconciledFolder(child);
//				continue;
//			}
//			hasFiles = true;
//		}
//		
//		if(hasFiles) {
//			filesToDelete += removeReconciledFiles(toDelete);
//		}
//		
//		
//		Query update = createQuery("delete from VirtualFile where id = :id", true);
//		update.setParameter("id", toDelete.getId());
//		filesToDelete += update.executeUpdate();
//		return filesToDelete;
//	}
	
//	@Override
//	@Transactional
//	public int removeReconciledFiles(VirtualFile folder) {
//		
//		Query update = createQuery("delete from VirtualFile where parent = :parent", true);
//		update.setEntity("parent", folder);
//		int updates = update.executeUpdate();
//		return updates;
//	}
	
	@Override
	@Transactional
	public void removeFileResource(FileResource resource) {

		
		VirtualFile mountPoint = getVirtualFile(resource.getVirtualPath(), resource.getRealm(), null);
		
		do {
			mountPoint.getFolderMounts().remove(resource);
			save(mountPoint);
			mountPoint = mountPoint.getParent();
		} while(mountPoint!=null);
		
		Query update = createQuery("update VirtualFile set defaultMount = null where defaultMount = :mount", true);
		update.setEntity("mount", resource);
		update.executeUpdate();
		
		update = createQuery("update VirtualFile set parent = :parent where mount = :mount", true);
		update.setParameter("parent", null);
		update.setEntity("mount", resource);
		update.executeUpdate();
		
		update = createQuery("delete from VirtualFile where mount = :mount", true);
		update.setEntity("mount", resource);
		update.executeUpdate();
		
	}
	
	@Override
	@Transactional
	public void clearFileResource(FileResource resource) {
		
		
		Query update = createQuery("update VirtualFile set defaultMount = null where defaultMount = :mount", true);
		update.setEntity("mount", resource);
		update.executeUpdate();
		
		update = createQuery("update VirtualFile set parent = :parent where mount = :mount", true);
		update.setParameter("parent", null);
		update.setEntity("mount", resource);
		update.executeUpdate();
		
		update = createQuery("delete from VirtualFile where mount = :mount", true);
		update.setEntity("mount", resource);
		update.executeUpdate();
		
	}

	@Override
	@Transactional(readOnly=true)
	public Collection<VirtualFile> getVirtualFolders(Realm realm) {
		
		return list(VirtualFile.class, new RealmRestriction(realm), new CriteriaConfiguration() {
			
			@Override
			public void configure(Criteria criteria) {
				criteria.add(
						Restrictions.and(
						Restrictions.in("type", new VirtualFileType[] { VirtualFileType.FOLDER, VirtualFileType.ROOT }),
						Restrictions.isNull("mount")));
				criteria.addOrder(Order.asc("id"));
			}
		});
	}

	@Override
	@Transactional
	public VirtualFile renameVirtualFolder(VirtualFile fromFolder, String toFolder) throws IOException {

		String newName = FileUtils.lastPathElement(toFolder);
		VirtualFile folder = buildFile(fromFolder,
					newName,
					FileUtils.checkEndsWithSlash(toFolder) , 
					VirtualFileType.FOLDER, 
					true, 
					fromFolder.getParent(),
					null,
					false,
					fromFolder.getRealm(),
					null);
		save(folder);
		return folder;
	}

	@Override
	@Transactional(readOnly=true)
	public VirtualFile getVirtualFileById(Long id) {
		return get("id", id, VirtualFile.class);
	}

	@Override
	@Transactional
	public void saveFile(VirtualFile file) {
		save(file);
	}

	@Override
	public Collection<VirtualFile> search(String searchColumn, String search, int start, int length, ColumnSort[] sort,
			VirtualFile parent, Realm realm, Principal principal, FileResource... resources) {
		return super.search(VirtualFile.class, searchColumn, search, start, length, sort);
	}

	@Override
	public void addFileResource(VirtualFile mountedFile, FileResource resource) {

		do {
			mountedFile.getFolderMounts().add(resource);
			save(mountedFile);
			mountedFile = mountedFile.getParent();
		} while(mountedFile!=null);
	}
}
