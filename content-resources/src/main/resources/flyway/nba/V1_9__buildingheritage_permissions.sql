insert into oskari_resource (resource_type,resource_mapping) values ('attribute', 'BuildingHeritageItem_areas+conservationId'); 
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values ((select id from oskari_resource where resource_mapping = 'BuildingHeritageItem_areas+conservationId'), 'ROLE', 'VIEW_ATTRIBUTE', 1),((select id from oskari_resource where resource_mapping = 'BuildingHeritageItem_areas+conservationId'), 'ROLE', 'VIEW_ATTRIBUTE', 2),((select id from oskari_resource where resource_mapping = 'BuildingHeritageItem_areas+conservationId'), 'ROLE', 'VIEW_ATTRIBUTE', 3),((select id from oskari_resource where resource_mapping = 'BuildingHeritageItem_areas+conservationId'), 'ROLE', 'VIEW_ATTRIBUTE', 4);