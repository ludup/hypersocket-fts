package com.hypersocket.vfs;

import com.hypersocket.utils.FastHash;

public class VirtualFileUtils {

	public static int generateHash(String filename, String virtualPath, int type, long lastModified, long size, boolean writable) {

		FastHash hash = new FastHash();
		hash.putString(filename);
		hash.putString(virtualPath);
		hash.putInt(type);
		hash.putLong(lastModified);
		hash.putLong(size);
		hash.putBoolean(writable);
		
		return hash.doFinal();
	}
}
