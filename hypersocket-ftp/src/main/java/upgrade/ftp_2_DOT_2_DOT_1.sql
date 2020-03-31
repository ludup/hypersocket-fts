EXIT IF FRESH;
ALTER TABLE ftp_interfaces ADD CONSTRAINT ftp_interfaces_cascade_1 FOREIGN KEY (realm_id) REFERENCES realms (resource_id) ON DELETE CASCADE;
