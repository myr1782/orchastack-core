create database orchadb;


select * from orchadb.CLOUDUSER;    
 
delete from orchadb.CLOUDUSER;

insert into orchadb.CloudUser(entityId,name,nameSpace,`type`,deleted,version) values ('0','admin','admin','Individual',0,1);
insert into orchadb.Individual(entityId,lastName,loginName) values ('0','','admin');

CREATE TABLE orchadb.Users (
    USERNAME VARCHAR(255) NOT NULL, 
    PASSWD VARCHAR(255), 
    PRIMARY KEY (USERNAME)
    );
CREATE TABLE orchadb.Users_Roles (
    USERNAME VARCHAR(255) NOT NULL, 
    ROLE_NAME VARCHAR(255)
    );
        
CREATE TABLE orchadb.Roles (
    NAME VARCHAR(255) NOT NULL, 
    DESCRIPTION VARCHAR(255)
    );    
    
CREATE TABLE orchadb.Permissions (
    NAME VARCHAR(255) NOT NULL, 
    DESCRIPTION VARCHAR(255)
    );    

CREATE TABLE orchadb.Roles_Permissions (
    ROLE_NAME VARCHAR(255) NOT NULL,
    PERMISSION_NAME VARCHAR(255)  
    );
    
insert into orchadb.Users values ('admin','admin');

insert into orchadb.Roles values ('admins','administrators');
insert into orchadb.Roles values ('users','application users');
insert into orchadb.Roles values ('guests','guest users');

insert into orchadb.Users_Roles values ('admin','admins');

insert into orchadb.Permissions values ('users.create','create users');
insert into orchadb.Permissions values ('users.list','list all users');

insert into orchadb.Roles_Permissions values ('admins','users.create');
insert into orchadb.Roles_Permissions values ('admins','users.list');
insert into orchadb.Roles_Permissions values ('users','users.list');

