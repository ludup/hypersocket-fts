package com.hypersocket.fs;

import org.apache.commons.vfs2.FileObject;

public interface DownloadProcessor {

	void startDownload(FileResource resource, String childPath, FileObject file,
			DownloadEventProcessor downloadEventProcessor);

}
