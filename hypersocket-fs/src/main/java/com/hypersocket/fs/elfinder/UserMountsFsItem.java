package com.hypersocket.fs.elfinder;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsVolume;

public class UserMountsFsItem implements FsItem {

	UserMountsFsVolume volume;
	
	UserMountsFsItem(UserMountsFsVolume volume) {
		this.volume = volume;
	}
	
	@Override
	public FsVolume getVolume() {
		return volume;
	}

}
