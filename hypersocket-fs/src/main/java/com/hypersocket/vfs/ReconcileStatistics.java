package com.hypersocket.vfs;

import java.util.ArrayList;
import java.util.List;

public class ReconcileStatistics {

	boolean generateChangeEvents = false;
	boolean rebuild = false;
	
	int filesUpdated;
	int filesCreated;
	int filesDeleted;
	int foldersCreated;
	int foldersUpdated;
	int foldersDeleted;

	List<String> conflictedPaths = new ArrayList<String>();

	int numOperations;
	int errors;
}
