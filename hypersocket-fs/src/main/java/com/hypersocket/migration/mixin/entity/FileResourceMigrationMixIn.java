package com.hypersocket.migration.mixin.entity;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer.None;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hypersocket.fs.FileResource;
import com.hypersocket.migration.helper.MigrationDeserializer;
import com.hypersocket.migration.helper.MigrationSerializerForResource;
import com.hypersocket.migration.mixin.MigrationMixIn;
import com.hypersocket.permissions.Role;
import com.hypersocket.realm.Realm;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = None.class)
public class FileResourceMigrationMixIn extends FileResource implements MigrationMixIn{

	private static final long serialVersionUID = 5611950250698225567L;
	
	private FileResourceMigrationMixIn() {}
	
	@Override
    @JsonIgnore
    public Realm getRealm() { return null;}
	
	@Override
    @JsonSerialize(contentUsing = MigrationSerializerForResource.class)
    @JsonDeserialize(contentUsing = MigrationDeserializer.class)
    public Set<Role> getRoles() {return null;}

}
