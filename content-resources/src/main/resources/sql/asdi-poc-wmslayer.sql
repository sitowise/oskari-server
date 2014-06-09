
-- Layer Group;
INSERT INTO oskari_layergroup (id,locale) values (893,'{ fi:{name:"ASDI"},sv:{name:"ASDI"},en:{name:"ASDI"}}');

-- Map Layers;
INSERT INTO oskari_maplayer(id, type, name, groupId,
                            url,
                            options,
                            locale)
  VALUES(893,'wmslayer', 'asdi:asdi', 893,
         'http://karttatiili.fi/dataset/asdi/service/wms',
         '{"singleTile":true}',
         '{ fi:{name:"Arctic SDI Finland",subtitle:""},sv:{name:"Arctic SDI Finland",subtitle:""},en:{name:"Arctic SDI Finland",subtitle:""}}');

-- link to inspire theme;
INSERT INTO oskari_maplayer_themes(maplayerid,
                                   themeid)
  VALUES(893,
         (SELECT id FROM portti_inspiretheme WHERE locale LIKE '%Others%'));

-- add layer as resource for mapping permissions;
INSERT INTO oskari_resource(resource_type, resource_mapping) values ('maplayer', 'http://karttatiili.fi/dataset/asdi/service/wms+asdi:asdi');

-- permissions;
-- adding permissions to roles with id 10110, 2, and 3;

-- give view_layer permission for the resource to ROLE 10110 (guest);
INSERT INTO oskari_permission(oskari_resource_id, external_type, permission, external_id) values
((SELECT MAX(id) FROM oskari_resource), 'ROLE', 'VIEW_LAYER', '10110');

INSERT INTO oskari_permission(oskari_resource_id, external_type, permission, external_id) values
((SELECT MAX(id) FROM oskari_resource), 'ROLE', 'VIEW_LAYER', '1');

-- give view_layer permission for the resource to ROLE 2 (user);
INSERT INTO oskari_permission(oskari_resource_id, external_type, permission, external_id) values
((SELECT MAX(id) FROM oskari_resource), 'ROLE', 'VIEW_LAYER', '2');

-- give publish permission for the resource to ROLE 3 (admin);
INSERT INTO oskari_permission(oskari_resource_id, external_type, permission, external_id) values
((SELECT MAX(id) FROM oskari_resource), 'ROLE', 'PUBLISH', '3');

-- give view_published_layer permission for the resource to ROLE 10110 (guest);
INSERT INTO oskari_permission(oskari_resource_id, external_type, permission, external_id) values
((SELECT MAX(id) FROM oskari_resource), 'ROLE', 'VIEW_PUBLISHED', '10110');

-- give view_published_layer permission for the resource to ROLE 2 (user);
INSERT INTO oskari_permission(oskari_resource_id, external_type, permission, external_id) values
((SELECT MAX(id) FROM oskari_resource), 'ROLE', 'VIEW_PUBLISHED', '2');



-- Add tutorial layers here;