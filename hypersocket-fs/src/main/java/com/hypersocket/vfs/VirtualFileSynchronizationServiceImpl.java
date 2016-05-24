package com.hypersocket.vfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hypersocket.fs.FileResource;
import com.hypersocket.utils.FileUtils;

@Service
public class VirtualFileSynchronizationServiceImpl implements VirtualFileSynchronizationService {

	static Logger log = LoggerFactory.getLogger(VirtualFileSynchronizationServiceImpl.class);
	
	@Autowired
	VirtualFileRepository repository;
	
	@Override
	public void reconcileFolder(ReconcileStatistics stats,
			FileObject fileObject, 
			FileResource resource, 
			VirtualFile folder,
			boolean conflicted) throws FileSystemException {

		if (log.isDebugEnabled()) {
			log.debug("Reconciling folder " + folder.getVirtualPath());
		}
		
		String displayName = conflicted ? 
				String.format("%s (%s)", fileObject.getName().getBaseName(), resource.getName())
				: fileObject.getName().getBaseName();

		if (!FileUtils.checkStartsWithNoSlash(resource.getVirtualPath())
				.equals(FileUtils.checkStartsWithNoSlash(folder.getVirtualPath()))) {
			if (isReconciledFolder(resource, fileObject)) {
				repository.reconcileFolder(displayName, folder, fileObject, resource, conflicted);
			}
		}

		Map<String, List<VirtualFile>> reconciledChildren = new HashMap<String, List<VirtualFile>>();
		for (VirtualFile virtual : repository.getReconciledFiles(folder)) {
			if (!reconciledChildren.containsKey(virtual.getFilename())) {
				reconciledChildren.put(virtual.getFilename(), new ArrayList<VirtualFile>());
			}
			reconciledChildren.get(virtual.getFilename()).add(virtual);
		}

		List<VirtualFile> toDeleteList = new ArrayList<VirtualFile>();

		try {
			for (FileObject obj : fileObject.getChildren()) {

				try {
					String filename = obj.getName().getBaseName();
					String childDisplayName = filename;
					boolean reconciled = false;
					boolean childConflicted = false;
					if (reconciledChildren.containsKey(filename)) {
						List<VirtualFile> virtualFiles = reconciledChildren.remove(filename);
						if (isConflicted(virtualFiles, resource)) {
							childConflicted = true;
							stats.conflictedPaths.add(folder.getVirtualPath() + filename);
						}

						for (VirtualFile virtual : virtualFiles) {
							if (virtual.getMount().equals(resource)) {
								if (obj.getType() == FileType.FOLDER || obj.getType() == FileType.FILE_OR_FOLDER) {
									if (isReconciledFolder(resource, obj)) {
										reconcileFolder(stats, obj, resource, virtual, childConflicted);
									} else {
										toDeleteList.add(virtual);
									}
									reconciled = true;
								} else {
									if (isReconciledFile(resource, obj)) {
										if (hasChanged(childDisplayName, obj, resource, virtual)) {
											reconcileFile(stats, obj, resource, virtual, folder, childConflicted);
										}
									} else {
										toDeleteList.add(virtual);
									}
									reconciled = true;
								}
							}
						}
					}

					if (reconciled) {
						continue;
					}
					
					if (obj.getType() == FileType.FOLDER || obj.getType() == FileType.FILE_OR_FOLDER) {
						if (isReconciledFolder(resource, obj)) {
							VirtualFile childFolder = repository.getVirtualFileByResource(
									FileUtils.checkEndsWithSlash(folder.getVirtualPath()) + obj.getName().getBaseName(),
									resource);
							if (childFolder == null) {
								childFolder = repository.reconcileNewFolder(childDisplayName, folder, obj, resource,
										childConflicted);
								stats.foldersCreated++;
							}
							reconcileFolder(stats, obj, resource, childFolder, childConflicted);
						}
					} else if (isReconciledFile(resource, obj)) {
						reconcileFile(stats, obj, resource, null, folder, childConflicted);
					}

				} catch (FileSystemException e) {
					log.error("Failed to reconcile file", e);
					stats.errors++;
				}
			}

			reconciledChildren.put("!!", toDeleteList);

			for (List<VirtualFile> deleteList : reconciledChildren.values()) {
				for (VirtualFile toDelete : deleteList) {
					if (!toDelete.isMounted() || !toDelete.getMount().equals(resource)) {
						continue;
					}
					if (toDelete.isFolder()) {
						stats.filesDeleted += repository.removeReconciledFolder(toDelete);
						stats.foldersDeleted++;
					} else {
						repository.removeReconciledFile(toDelete);
						stats.filesDeleted++;
					}
					checkFlush(stats);
				}
			}

		} catch (FileSystemException e) {
			log.error("Failed to reconcile folder", e);
			stats.errors++;
		} finally {
			checkFlush(stats);
		}
	}

	private boolean isReconciledFile(FileResource resource, FileObject obj) {
		try {
			if (obj.isHidden() || obj.getName().getBaseName().startsWith(".")) {
				return resource.isShowHidden();
			}
			return true;
		} catch (FileSystemException e) {
			return true;
		}
	}

	private boolean isReconciledFolder(FileResource resource, FileObject obj) {
		if (!resource.isShowFolders()) {
			return false;
		}
		// Check for hidden folder
		return isReconciledFile(resource, obj);
	}

	/**
	 * Determine if file or folder conflicts with others. The original file that
	 * was not conflicted wins and will be treated as not being in conflict.
	 * Only subsequent files/folders.
	 * 
	 * @param files
	 * @param resource
	 * @return
	 */
	private boolean isConflicted(List<VirtualFile> files, FileResource resource) {
		boolean conflicted = files.size() > 0;
		for (VirtualFile file : files) {
			if (file.getMount().equals(resource)) {
				conflicted = files.size() > 1 && file.getConflicted();
			}
		}
		return conflicted;
	}
	
	@Override
	public void reconcileFile(ReconcileStatistics stats, FileObject obj, FileResource resource, VirtualFile virtual,
			VirtualFile parent, boolean conflicted) throws FileSystemException {
		
		String displayName = conflicted ? 
				String.format("%s (%s)", obj.getName().getBaseName(), resource.getName())
				: obj.getName().getBaseName();
				
		if (virtual == null) {
			if (log.isDebugEnabled()) {
				log.debug("Creating file " + parent.getVirtualPath() + obj.getName().getBaseName());
			}
			repository.reconcileFile(displayName, obj, resource, parent);
			stats.filesCreated++;
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Updating file " + parent.getVirtualPath() + obj.getName().getBaseName());
			}
			repository.reconcileFile(displayName, obj, resource, virtual, parent);
			stats.filesUpdated++;
		}
		checkFlush(stats);
	}

	private boolean hasChanged(String displayName, FileObject obj, FileResource resource, VirtualFile virtual)
			throws FileSystemException {
		return virtual.getHash() != VirtualFileUtils.generateHash(obj.getName().getBaseName(), virtual.getVirtualPath(),
				virtual.getType().ordinal(), obj.getContent().getLastModifiedTime(),
				virtual.getType() == VirtualFileType.FILE ? obj.getContent().getSize() : 0L, !resource.isReadOnly(),
				!displayName.equals(obj.getName().getBaseName()));
	}
	
	private void checkFlush(ReconcileStatistics stats) {
		stats.numOperations++;
		if (stats.numOperations % 25 == 0) {
			repository.flush();
		}
	}
}
