package com.hypersocket.fs.json;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.vfs2.FileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hypersocket.fs.DownloadEventProcessor;
import com.hypersocket.fs.DownloadProcessor;
import com.hypersocket.fs.FileResource;

public class HttpDownloadProcessor implements DownloadProcessor {

	static Logger log = LoggerFactory.getLogger(HttpDownloadProcessor.class);

	public static final String CONTENT_INPUTSTREAM = "ContentInputStream";

	HttpServletRequest request;
	HttpServletResponse response;
	long start;
	long length;

	static MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();

	public HttpDownloadProcessor(HttpServletRequest request,
			HttpServletResponse response, long start, long length) {
		this.request = request;
		this.response = response;
		this.start = start;
		this.length = length;
	}

	@Override
	public void startDownload(FileResource resource, String childPath,
			FileObject file, DownloadEventProcessor downloadEventProcessor) {

		try {
			
			long started = downloadEventProcessor.downloadStarted(resource, childPath, file);
			
			response.setContentType(mimeTypesMap.getContentType(file.getName()
					.getBaseName()));

			if ("true".equals(request.getParameter("forceDownload"))) {
				response.setHeader("Content-disposition",
						"attachment; filename=" + file.getName().getBaseName());
			}

			if (file.getContent().getSize() <= 1024000 && length <= 1024000) {

				InputStream in = new BufferedInputStream(file.getContent().getInputStream());
				if (start > 0) {
					in.skip(start);
				}
				byte[] buf = new byte[65535];
				int r;
				long remaining = length;
				while ((r = in.read(buf, 0,
						(int) Math.min(buf.length, remaining))) > -1
						&& remaining > 0) {
					response.getOutputStream().write(buf, 0, r);
					remaining -= r;
				}

				downloadEventProcessor.downloadComplete(resource, childPath,
						file, length, System.currentTimeMillis() - started);

			} else {
				request.setAttribute(CONTENT_INPUTSTREAM,
						new ContentInputStream(resource, childPath, file,
								start, length, downloadEventProcessor, started));
			}
		} catch (IOException e) {
			downloadEventProcessor.downloadFailed(resource, childPath, file, e);
		}

	}

	class ContentInputStream extends InputStream {

		long remaining;
		long totalBytesOut = 0;
		long timeStarted;
		DownloadEventProcessor eventProcessor;
		FileResource resource;
		String childPath;
		FileObject file;
		InputStream in;

		ContentInputStream(FileResource resource, String childPath,
				FileObject file, long start, long length,
				DownloadEventProcessor eventProcessor, long timeStarted) throws IOException {
			this.eventProcessor = eventProcessor;
			this.resource = resource;
			this.timeStarted = timeStarted;
			this.childPath = childPath;
			this.file = file;
			this.in = new BufferedInputStream(file.getContent().getInputStream());
			this.remaining = in.available();
			if (start > 0) {
				if (log.isDebugEnabled()) {
					log.debug("content range will start at position " + start
							+ " with " + remaining + " available");
				}
				long actual = in.skip(start);
				if (actual != start) {
					throw new IOException(
							"Could not skip the required number of bytes");
				}
			}

			remaining = length;
		}

		@Override
		public int read() throws IOException {
			if (remaining == 0) {
				return -1;
			}
			int b = in.read();
			if (b > -1) {
				remaining--;
				totalBytesOut++;
			}
			if(b == -1) {
				eventProcessor.downloadComplete(resource, childPath, file, totalBytesOut, System.currentTimeMillis() - timeStarted);
				remaining = 0;
			}
			return b;
		}
		
		public int read(byte[] buf, int off, int len) throws IOException {
			if(remaining == 0) {
				return -1;
			}
			int r = in.read(buf, off, len);
			if(r > -1) {
				remaining -= r;
				totalBytesOut+= r;
			}
			if(r == -1) {
				eventProcessor.downloadComplete(resource, childPath, file, totalBytesOut, System.currentTimeMillis() - timeStarted);
				remaining = 0;
			}
			return r;
		}

		public int available() {
			return (int) remaining;
		}
	}

}
