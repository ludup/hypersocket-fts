package com.hypersocket.migration.mixin.entity;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hypersocket.fs.FileResource;
import com.hypersocket.migration.helper.MigrationDeserializer;
import com.hypersocket.migration.helper.MigrationSerializerForResource;
import com.hypersocket.migration.mixin.MigrationMixIn;
import com.hypersocket.realm.Principal;
import com.hypersocket.realm.Realm;
import com.hypersocket.vfs.VirtualFile;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualFileMigrationMixIn extends VirtualFile implements MigrationMixIn{

	private static final long serialVersionUID = 963464527903740748L;
	
	private VirtualFileMigrationMixIn() {}
	
	@Override
    @JsonIgnore
    public Realm getRealm() { return null;}
	
	@Override
	@JsonIgnore
	public VirtualFile getParent() {return null;}
	
	@Override
	@JsonSerialize(using = MigrationSerializerForResource.class)
    @JsonDeserialize(using = MigrationDeserializer.class)
	public FileResource getMount() {return null;}
	
	@Override
	@JsonSerialize(contentUsing = MigrationSerializerForResource.class)
    @JsonDeserialize(contentUsing = MigrationDeserializer.class)
	public Set<FileResource> getFolderMounts() {return null;}
	
	@Override
	@JsonSerialize(using = MigrationSerializerForResource.class)
    @JsonDeserialize(using = MigrationDeserializer.class)
	public FileResource getDefaultMount() {return null;}
	
	@Override
	@JsonSerialize(using = MigrationSerializerForResource.class)
    @JsonDeserialize(using = MigrationDeserializer.class)
	public Principal getPrincipal() {return null;}

}
