package fi.sito.nba.control;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.swing.event.ListSelectionEvent;

import org.deegree.io.DBConnectionPool;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.control.RestActionHandler;
import fi.nls.oskari.db.DatasourceHelper;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.ResponseHelper;
import fi.sito.nba.model.NbaRegistryLayer;
import fi.sito.nba.registry.models.*;
import fi.sito.nba.registry.services.*;
import fi.sito.nba.registry.infrastructure.*;
import fi.sito.nba.service.NbaRegistryLayerService;
import fi.sito.nba.service.NbaRegistryLayerServiceInterface;

import java.sql.*;
import com.microsoft.sqlserver.jdbc.*;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTReader;
//import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

@OskariActionRoute("GetRegistryItems")
public class GetRegistryItemsHandler extends RestActionHandler {
	private static final String PARAM_REGISTER_NAME = "registerName";
	private static final String PARAM_ITEM_ID = "id";
	private static final String PARAM_REGISTRIES = "registries";
	private static final String PARAM_KEYWORD = "keyword";
	private static final String PARAM_GEOMETRY = "geometry";

	private static final Logger LOG = LogFactory
			.getLogger(GetRegistryItemsHandler.class);
	private static NbaRegistryLayerServiceInterface registryLayerService = new NbaRegistryLayerService();

	public void preProcess(ActionParameters params) throws ActionException {
		// common method called for all request methods
		LOG.info(params.getUser(), "accessing route", getName());
	}

	@Override
	public void handleGet(ActionParameters params) throws ActionException {

		Object response = null;
		Connection connection = null;

		try {
			// set up connection
			SQLServerDataSource ds = new SQLServerDataSource();
			ds.setUser(PropertyUtil.get("nba.db.username"));
			ds.setPassword(PropertyUtil.get("nba.db.password"));
			ds.setURL(PropertyUtil.get("nba.db.url"));
			connection = ds.getConnection();
			
			//get configuration of registry layers from DB
			List<NbaRegistryLayer> registryLayers = registryLayerService.findRegistryLayers();
			
			String registriesParam = "";
			String keywordParam = null;
			Geometry geometryParam = null;
			int itemIdParam = 0;
			String registryNameParam = "";

			if (params.getHttpParam(PARAM_ITEM_ID) != null
					&& !params.getHttpParam(PARAM_ITEM_ID).equals("")
					&& params.getHttpParam(PARAM_REGISTER_NAME) != null
					&& !params.getHttpParam(PARAM_REGISTER_NAME).equals("")) {

				itemIdParam = Integer.parseInt(params.getHttpParam(PARAM_ITEM_ID));
				registryNameParam = params.getHttpParam(PARAM_REGISTER_NAME);
				
				//filter list of layers
				List<NbaRegistryLayer> filteredLayerList = getRegistryLayers(registryNameParam, registryLayers);
				
				JSONObject itemObj = new JSONObject();
				Object service = null;
				IRegistryObject registryItem = null;
				
				switch (registryNameParam) {
				case "ancientMonument":
					service = new AncientMonumentService(connection);
					registryItem = ((AncientMonumentService)service).getAncientMonumentById(itemIdParam);
					itemObj = getItemObject(registryItem, filteredLayerList);
					break;
				case "ancientMaintenance":
					service = new AncientMonumentMaintenanceItemService(
							connection);
					registryItem = ((AncientMonumentMaintenanceItemService)service).getAncientMonumentMaintenanceItemById(itemIdParam);
					itemObj = getItemObject(registryItem, filteredLayerList);
					break;
				case "buildingHeritage":
					service = new BuildingHeritageItemService(
							connection);
					registryItem = ((BuildingHeritageItemService)service).getBuildingHeritageItemById(itemIdParam);
					itemObj = getItemObject(registryItem, filteredLayerList);
					break;
				case "rky1993":
					service = new RKY1993Service(connection);
					registryItem = ((RKY1993Service)service).getRKY1993ById(itemIdParam);
					itemObj = getItemObject(registryItem, filteredLayerList);
					break;
				case "rky2000":
					service = new RKY2000Service(connection);
					registryItem = ((RKY2000Service)service).getRKY2000ById(itemIdParam);
					itemObj = getItemObject(registryItem, filteredLayerList);
					break;
				}

				response = itemObj;
			} else {

				// read parameters
				if (params.getHttpParam(PARAM_REGISTRIES) != null
						&& !params.getHttpParam(PARAM_REGISTRIES).equals("")) {
					registriesParam = params.getHttpParam(PARAM_REGISTRIES);
				}

				if (params.getHttpParam(PARAM_KEYWORD) != null
						&& !params.getHttpParam(PARAM_KEYWORD).equals("")) {
					keywordParam = params.getHttpParam(PARAM_KEYWORD);
				}

				if (params.getHttpParam(PARAM_GEOMETRY) != null
						&& !params.getHttpParam(PARAM_GEOMETRY).equals("")) {
					String geometryStr = params.getHttpParam(PARAM_GEOMETRY);
					geometryParam = (new WKTReader()).read(geometryStr);
				}

				JSONArray generalResultArray = new JSONArray();

				String[] registries = registriesParam.split(",");
				
				
				if (registries[0].length() > 0) {
					
					for (String registry : registries) {

						JSONArray registerResultArray = new JSONArray();

						switch (registry) {
						case "ancientMonument":
							registerResultArray = getAncientMonumentItems(connection,
									keywordParam, geometryParam, registryLayers);
							break;
						case "ancientMaintenance":
							registerResultArray = getAncientMonumentMaintenanceItems(
									connection, keywordParam, geometryParam, registryLayers);
							break;
						case "buildingHeritage":
							registerResultArray = getBuildingHeritageItems(connection,
									keywordParam, geometryParam, registryLayers);
							break;
						case "rky1993":
							registerResultArray = getRKY1993Items(connection, keywordParam,
									geometryParam, registryLayers);
							break;
						case "rky2000":
							registerResultArray = getRKY2000Items(connection, keywordParam,
									geometryParam, registryLayers);
							break;
						}

						for (int i = 0; i < registerResultArray.length(); i++) {
							generalResultArray.put(registerResultArray.get(i));
						}

					}
				} else {
					
					JSONArray registerResultArray1 = getAncientMonumentItems(
							connection, keywordParam, geometryParam, registryLayers);
					JSONArray registerResultArray2 = getAncientMonumentMaintenanceItems(
							connection, keywordParam, geometryParam, registryLayers);
					JSONArray registerResultArray3 = getBuildingHeritageItems(
							connection, keywordParam, geometryParam, registryLayers);
					JSONArray registerResultArray4 = getRKY1993Items(connection,
							keywordParam, geometryParam, registryLayers);
					JSONArray registerResultArray5 = getRKY2000Items(connection,
							keywordParam, geometryParam, registryLayers);

					for (int i = 0; i < registerResultArray1.length(); i++) {
						generalResultArray.put(registerResultArray1.get(i));
					}

					for (int i = 0; i < registerResultArray2.length(); i++) {
						generalResultArray.put(registerResultArray2.get(i));
					}

					for (int i = 0; i < registerResultArray3.length(); i++) {
						generalResultArray.put(registerResultArray3.get(i));
					}

					for (int i = 0; i < registerResultArray4.length(); i++) {
						generalResultArray.put(registerResultArray4.get(i));
					}

					for (int i = 0; i < registerResultArray5.length(); i++) {
						generalResultArray.put(registerResultArray5.get(i));
					}
				}

				response = generalResultArray;
			}
		} catch (Exception e) {
			//throw new ActionException("Error during geting registry item");
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			response = "Message:\n" + e.getMessage() + "\n" + "Cause:\n"
					+ e.getCause() + "\n" + "Stak trace:\n" + sw.toString();
		} finally {
			try {
			connection.close();
			} catch (Exception e) {
			}
		}

		ResponseHelper.writeResponse(params, response);
	}

	private JSONObject getItemObject(IRegistryObject registryObject, List<NbaRegistryLayer> registryLayers) {
		JSONObject item = new JSONObject();
		try {

			item.put("id", registryObject.getObjectId());
			item.put("coordinateX", registryObject.calculateCentroid().getX());
			item.put("coordinateY", registryObject.calculateCentroid().getY());
			item.put("nbaUrl", registryObject.generateNbaUrl());
			
			JSONArray mapLayersArray = new JSONArray(); 
			for (NbaRegistryLayer registryLayer : registryLayers) {
				JSONObject mapLayerObject = new JSONObject();
				mapLayerObject.put("mapLayerID", registryLayer.getLayerId());
				mapLayerObject.put("toHighlight", registryLayer.getToHighlight());
				mapLayerObject.put("attribute", registryLayer.getItemIdAttribute());
				
				mapLayersArray.put(mapLayerObject);
			}
			item.put("mapLayers", mapLayersArray);
			
			Envelope envelope = registryObject.calculateEnvelope().getEnvelopeInternal();
			JSONArray bounds = new JSONArray();
			bounds.put(envelope.getMinX());
			bounds.put(envelope.getMinY());
			bounds.put(envelope.getMaxX());
			bounds.put(envelope.getMaxY());
			item.put("bounds", bounds);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return item;
	}
	
	private JSONArray getAncientMonumentMaintenanceItems(Connection con,
			String keyword, Geometry geometry, List<NbaRegistryLayer> registryLayers) {

		JSONArray resultArray = new JSONArray();

		String results = "";

		AncientMonumentMaintenanceItemService svc = new AncientMonumentMaintenanceItemService(
				con);

		RegistryObjectCollection<AncientMonumentMaintenanceItem> monuments = (RegistryObjectCollection<AncientMonumentMaintenanceItem>) svc
				.findAncientMonumentMaintenanceItems(keyword, geometry);

		if (monuments != null) {
			
			List<NbaRegistryLayer> filteredLayers = getRegistryLayers("ancientMaintenance", registryLayers);
			
			RegistryObjectIterator iterator = (RegistryObjectIterator) monuments
					.iterator();
			while (iterator.hasNext()) {
				try {
					AncientMonumentMaintenanceItem monument = (AncientMonumentMaintenanceItem) iterator
							.next();
					JSONObject item = new JSONObject();
					item.put("id", monument.getObjectId());
					item.put("desc", monument.getObjectName());
					item.put("coordinateX", monument.calculateCentroid().getX());
					item.put("coordinateY", monument.calculateCentroid().getY());
					item.put("nbaUrl", monument.generateNbaUrl());
					JSONArray mapLayersArray = new JSONArray(); 
					for (NbaRegistryLayer registryLayer : filteredLayers) {
						JSONObject mapLayerObject = new JSONObject();
						mapLayerObject.put("mapLayerID", registryLayer.getLayerId());
						mapLayerObject.put("toHighlight", registryLayer.getToHighlight());
						mapLayerObject.put("attribute", registryLayer.getItemIdAttribute());
						
						mapLayersArray.put(mapLayerObject);
					}
					item.put("mapLayers", mapLayersArray);
					
					Envelope envelope = monument.calculateEnvelope().getEnvelopeInternal();
					JSONArray bounds = new JSONArray();
					bounds.put(envelope.getMinX());
					bounds.put(envelope.getMinY());
					bounds.put(envelope.getMaxX());
					bounds.put(envelope.getMaxY());
					item.put("bounds", bounds);
					
					resultArray.put(item);
				} catch (JSONException e) {
					return null;
				}
			}
		}
		return resultArray;
	}

	private JSONArray getAncientMonumentItems(Connection con, String keyword,
			Geometry geometry, List<NbaRegistryLayer> registryLayers) {

		JSONArray resultArray = new JSONArray();

		String results = "";

		AncientMonumentService svc = new AncientMonumentService(con);

		RegistryObjectCollection<AncientMonument> monuments = (RegistryObjectCollection<AncientMonument>) svc
				.findAncientMonuments(keyword, geometry);
		if (monuments != null) {
			
			List<NbaRegistryLayer> filteredLayers = getRegistryLayers("ancientMonument", registryLayers);
			
			RegistryObjectIterator iterator = (RegistryObjectIterator) monuments
					.iterator();
			while (iterator.hasNext()) {
				try {
					AncientMonument monument = (AncientMonument) iterator.next();
					
					JSONObject item = new JSONObject();
					item.put("id", monument.getObjectId());
					item.put("desc", monument.getObjectName());
					
					Point centroid = monument.calculateCentroid(); 
					
					if (centroid != null) {
						item.put("coordinateX", centroid.getX());
						item.put("coordinateY", centroid.getY());
					}
					
					item.put("nbaUrl", monument.generateNbaUrl());
					JSONArray mapLayersArray = new JSONArray(); 
					for (NbaRegistryLayer registryLayer : filteredLayers) {
						JSONObject mapLayerObject = new JSONObject();
						mapLayerObject.put("mapLayerID", registryLayer.getLayerId());
						mapLayerObject.put("toHighlight", registryLayer.getToHighlight());
						mapLayerObject.put("attribute", registryLayer.getItemIdAttribute());
						
						mapLayersArray.put(mapLayerObject);
					}
					item.put("mapLayers", mapLayersArray);
					
					Envelope envelope = monument.calculateEnvelope().getEnvelopeInternal();
					JSONArray bounds = new JSONArray();
					bounds.put(envelope.getMinX());
					bounds.put(envelope.getMinY());
					bounds.put(envelope.getMaxX());
					bounds.put(envelope.getMaxY());
					item.put("bounds", bounds);
					
					resultArray.put(item);
				} catch (JSONException e) {
					return null;
				}
			}
		}
		return resultArray;
	}

	private JSONArray getBuildingHeritageItems(Connection con, String keyword,
			Geometry geometry, List<NbaRegistryLayer> registryLayers) {

		JSONArray resultArray = new JSONArray();

		String results = "";

		BuildingHeritageItemService svc = new BuildingHeritageItemService(con);

		RegistryObjectCollection<BuildingHeritageItem> monuments = (RegistryObjectCollection<BuildingHeritageItem>) svc
				.findBuildingHeritageItems(keyword, geometry);
		if (monuments != null) {
			
			List<NbaRegistryLayer> filteredLayers = getRegistryLayers("buildingHeritage", registryLayers);
			
			RegistryObjectIterator iterator = (RegistryObjectIterator) monuments
					.iterator();
			while (iterator.hasNext()) {
				try {
					BuildingHeritageItem monument = (BuildingHeritageItem) iterator.next();
					
					JSONObject item = new JSONObject();
					item.put("id", monument.getObjectId());
					item.put("desc", monument.getObjectName());

					Point centroid = monument.calculateCentroid(); 
					
					if (centroid != null) {
						item.put("coordinateX", centroid.getX());
						item.put("coordinateY", centroid.getY());
					}
					
					item.put("nbaUrl", monument.generateNbaUrl());
					JSONArray mapLayersArray = new JSONArray(); 
					for (NbaRegistryLayer registryLayer : filteredLayers) {
						JSONObject mapLayerObject = new JSONObject();
						mapLayerObject.put("mapLayerID", registryLayer.getLayerId());
						mapLayerObject.put("toHighlight", registryLayer.getToHighlight());
						mapLayerObject.put("attribute", registryLayer.getItemIdAttribute());
						
						mapLayersArray.put(mapLayerObject);
					}
					item.put("mapLayers", mapLayersArray);
					
					Envelope envelope = monument.calculateEnvelope().getEnvelopeInternal();
					JSONArray bounds = new JSONArray();
					bounds.put(envelope.getMinX());
					bounds.put(envelope.getMinY());
					bounds.put(envelope.getMaxX());
					bounds.put(envelope.getMaxY());
					item.put("bounds", bounds);
					
					resultArray.put(item);
				} catch (JSONException e) {
					return null;
				}
			}
		}
		return resultArray;
	}

	private JSONArray getRKY1993Items(Connection con, String keyword,
			Geometry geometry, List<NbaRegistryLayer> registryLayers) {

		JSONArray resultArray = new JSONArray();

		String results = "";

		RKY1993Service svc = new RKY1993Service(con);

		RegistryObjectCollection<RKY1993> monuments = (RegistryObjectCollection<RKY1993>) svc.findRKY1993(keyword,
				geometry);
		if (monuments != null) {
			
			List<NbaRegistryLayer> filteredLayers = getRegistryLayers("rky1993", registryLayers);
			
			RegistryObjectIterator iterator = (RegistryObjectIterator) monuments
					.iterator();
			while (iterator.hasNext()) {
				try {
					RKY1993 monument = (RKY1993) iterator.next();
					
					JSONObject item = new JSONObject();
					item.put("id", monument.getObjectId());
					item.put("desc", monument.getObjectName());

					Point centroid = monument.calculateCentroid(); 
					
					if (centroid != null) {
						item.put("coordinateX", centroid.getX());
						item.put("coordinateY", centroid.getY());
					}
					
					item.put("nbaUrl", monument.generateNbaUrl());
					JSONArray mapLayersArray = new JSONArray(); 
					for (NbaRegistryLayer registryLayer : filteredLayers) {
						JSONObject mapLayerObject = new JSONObject();
						mapLayerObject.put("mapLayerID", registryLayer.getLayerId());
						mapLayerObject.put("toHighlight", registryLayer.getToHighlight());
						mapLayerObject.put("attribute", registryLayer.getItemIdAttribute());
						
						mapLayersArray.put(mapLayerObject);
					}
					item.put("mapLayers", mapLayersArray);
					
					Envelope envelope = monument.calculateEnvelope().getEnvelopeInternal();
					JSONArray bounds = new JSONArray();
					bounds.put(envelope.getMinX());
					bounds.put(envelope.getMinY());
					bounds.put(envelope.getMaxX());
					bounds.put(envelope.getMaxY());
					item.put("bounds", bounds);
					
					resultArray.put(item);
				} catch (JSONException e) {
					return null;
				}
			}
		}
		return resultArray;
	}

	private JSONArray getRKY2000Items(Connection con, String keyword,
			Geometry geometry, List<NbaRegistryLayer> registryLayers) {

		JSONArray resultArray = new JSONArray();

		String results = "";

		RKY2000Service svc = new RKY2000Service(con);

		RegistryObjectCollection<RKY2000> monuments = (RegistryObjectCollection<RKY2000>) svc
				.findRKY2000(keyword, geometry);

		if (monuments != null) {
			
			List<NbaRegistryLayer> filteredLayers = getRegistryLayers("rky2000", registryLayers);
			
			RegistryObjectIterator iterator = (RegistryObjectIterator) monuments
					.iterator();
			
			while (iterator.hasNext()) {
				try {
					RKY2000 monument = (RKY2000) iterator.next();

					JSONObject item = new JSONObject();
					item.put("id", monument.getObjectId());
					item.put("desc", monument.getObjectName());

					Point centroid = monument.calculateCentroid();
					if (centroid != null) {
						item.put("coordinateX", centroid.getX());
						item.put("coordinateY", centroid.getY());
					}
					
					item.put("nbaUrl", monument.generateNbaUrl());
					JSONArray mapLayersArray = new JSONArray(); 
					for (NbaRegistryLayer registryLayer : filteredLayers) {
						JSONObject mapLayerObject = new JSONObject();
						mapLayerObject.put("mapLayerID", registryLayer.getLayerId());
						mapLayerObject.put("toHighlight", registryLayer.getToHighlight());
						mapLayerObject.put("attribute", registryLayer.getItemIdAttribute());
						
						mapLayersArray.put(mapLayerObject);
					}
					item.put("mapLayers", mapLayersArray);
					
					Envelope envelope = monument.calculateEnvelope().getEnvelopeInternal();
					JSONArray bounds = new JSONArray();
					bounds.put(envelope.getMinX());
					bounds.put(envelope.getMinY());
					bounds.put(envelope.getMaxX());
					bounds.put(envelope.getMaxY());
					item.put("bounds", bounds);
					
					resultArray.put(item);
				} catch (JSONException e) {
					return null;
				}
			}
		}
		return resultArray;
	}
	
	private List<NbaRegistryLayer> getRegistryLayers(String registryName, List<NbaRegistryLayer> registryLayers) {
		//filter list of layers
		List<NbaRegistryLayer> filteredLayerList = new ArrayList<>();
		for (NbaRegistryLayer nbaRegistryLayer : registryLayers) {
			if (nbaRegistryLayer.getRegistryName().equals(registryName)) {
				filteredLayerList.add(nbaRegistryLayer);
			}
		}
		
		return filteredLayerList;
	}

}