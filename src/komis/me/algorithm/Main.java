package komis.me.algorithm;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.security.auth.Subject;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.SymmetricProperty;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.VCARD;
import com.hp.hpl.jena.vocabulary.XSD;

public class Main {

	/*
	 * This class is for create ontology schema automatically. Is not the main
	 * application for reasoning.
	 */

	private java.sql.Connection connect = null;
	private java.sql.Statement statement = null;
	private java.sql.ResultSet resultSet = null;

	static final String DB_URL = "jdbc:mysql://localhost/*";

	// Database credentials
	static final String USER = "*";
	static final String PASS = "*";

	static long update; // time
	static long current;
	static int days;
	static final double value = 2;// interest
	static final int day = 24 * 60 * 60 * 1000;

	String uid = "*";

	public static void main(String[] args) {

		// initial namespace
		String MLNR = "http://www.example.com/*.owl#";

		// create a Hashmap for storing Ontclasses
		HashMap<String, OntClass> OntClassCache = new HashMap<String, OntClass>();

		Main main = new Main();

		// create owl model
		OntModel model = ModelFactory.createOntologyModel(); // TODO

		/*
		 *  create OntClasses
		 */
		OntClass Food = model.createClass(MLNR + "Food");
		OntClass Restaurant = model.createClass(MLNR + "Restaurant");
		OntClass Dish = model.createClass(MLNR + "Dish");
		OntClass User = model.createClass(MLNR + "User");
		
		/*
		 *  create properties
		 */
		// serve
		ObjectProperty serve = model.createObjectProperty(MLNR + "serve");
		serve.addDomain(Restaurant);
		serve.addRange(Dish);
		
		// is served by
		ObjectProperty  isServedBy = model.createObjectProperty(MLNR + "isServedBy");
		isServedBy.addDomain(Dish);
		isServedBy.addRange(Restaurant);
		
		// tag
		ObjectProperty tag = model.createObjectProperty(MLNR + "tag");
		tag.addDomain(Dish);
		tag.addRange(Food);
		
		// datatypes
		DatatypeProperty name = model.createDatatypeProperty(MLNR + "name");
		name.addDomain(Restaurant);
		name.addRange(XSD.xstring);

		// lat & lon
		DatatypeProperty lat = model.createDatatypeProperty(MLNR + "lat");
		lat.addDomain(Restaurant);
		lat.addRange(XSD.xdouble);
		
		DatatypeProperty lon = model.createDatatypeProperty(MLNR + "lon");
		lon.addDomain(Restaurant);
		lon.addRange(XSD.xdouble);
		
		DatatypeProperty appropriate = model.createDatatypeProperty(MLNR + "appropriate");
		appropriate.addDomain(Restaurant);
		appropriate.addRange(XSD.xstring);

		// open class structure
		InputStream in = FileManager.get().open(
				"C:\\......\\*.owl");
		if (in == null) {
			throw new IllegalArgumentException("File: not found");
		}

		model.read(in, null);

		// initial generic classes
		ArrayList<HashMap<String, String>> foodClassList = new ArrayList<HashMap<String, String>>();
		foodClassList = main.getFoodClassList();

		for (int i = 0; i < foodClassList.size(); i++) {
			HashMap<String, String> classes = foodClassList.get(i);

			OntClass subClass = model.createClass(MLNR + classes.get("")); // 2.
																				// create
																				// subclass

			Food.addSubClass(subClass); // 3. connect superclass and subclass

			OntClassCache.put(classes.get(""), subClass);
		}

		// initial specific INDIVIDUALS
		ArrayList<HashMap<String, String>> specificDataList = new ArrayList<HashMap<String, String>>();
		specificDataList = main.getSpecificDataList();

		for (int i = 0; i < specificDataList.size(); i++) {
			HashMap<String, String> map = specificDataList.get(i);

			OntClass superClass = OntClassCache.get(map.get("")); // 1. get
																		// superclass

			System.out.println(map.get(""));
			
			superClass.createIndividual(MLNR + map.get("")); // 2. create
																	// individuals
			
			System.out.println(map.get(""));

		}

		// create maps to restore
		HashMap<String, Individual> Restaurants = new HashMap<String, Individual>();
		HashMap<String, Individual> Dishes = new HashMap<String, Individual>();

		
		// add restaurant individuals
		ArrayList<HashMap<String, String>> restaurantList = new ArrayList<HashMap<String, String>>();
		restaurantList = main.getRestaurantList();
		for (int i = 0; i < restaurantList.size(); i++) {
			HashMap<String, String> map = restaurantList.get(i);

			Individual individual = Restaurant.createIndividual(MLNR + "R"
					+ map.get(""));
			individual.addProperty(name, "");
			individual.addProperty(lat, map.get(""));
			individual.addProperty(lon, map.get(""));//TODO
			
			Restaurants.put(map.get(""), individual);
		}

		// add dish individuals
		ArrayList<HashMap<String, String>> dishList = new ArrayList<HashMap<String, String>>();
		dishList = main.getDishList();
		for (int i = 0; i < dishList.size(); i++) {
			HashMap<String, String> map = dishList.get(i);

			Individual individual = Dish.createIndividual(MLNR + "D"
					+ map.get(""));
			individual.addProperty(isServedBy, Restaurants.get(map.get("")));
			
			Individual restaurant = model.getIndividual(MLNR + "R" + map.get(""));
			restaurant.addProperty(serve, individual);
			
			Dishes.put(map.get(""), individual);
		}

		// add tags of dishes
		ArrayList<HashMap<String, String>> tagList = new ArrayList<HashMap<String, String>>();
		tagList = main.getTagList();
		for (int i = 0; i < tagList.size(); i++) {
			HashMap<String, String> map = tagList.get(i);

			Individual individual = Dishes.get(map.get("did"));
			
			Individual dish = model.getIndividual(individual.toString());
			
			dish.addProperty(tag, model.getIndividual(MLNR + map.get("tag")));
		}
		
		
		FileOutputStream out = null;
		try {
			// XML format - long and verbose
			// assign a path to store your ontology
			out = new FileOutputStream("C:\\.....\\*.owl");
			// TODO write ontology
			model.write(out, "RDF/XML-ABBREV");

		} catch (IOException ignore) {
			
			ignore.printStackTrace();
		}

	
	}

	private ArrayList<HashMap<String, String>> getTagList() {
		ArrayList<HashMap<String, String>> dataList = new ArrayList<HashMap<String, String>>();

		try {

			// each DB has its own driver, so we load it
			Class.forName("com.mysql.jdbc.Driver");

			// setup connection with DB
			connect = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connect.createStatement();

			// your query
			resultSet = statement.executeQuery("");

			// get result
			while (resultSet.next()) {

				HashMap<String, String> map = new HashMap<String, String>();
				String tag = resultSet.getString("");
				String did = resultSet.getString("");
				map.put("", tag);
				map.put("", did);

				dataList.add(map);

			}

			// clean environment
			resultSet.close();
			statement.close();
			connect.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return dataList;
	}

	private static double calculate(double interest, long days) {

		int weight = (int) (30 - days);

		if (weight <= 0) {
			weight = 1;
		}

		double result = interest * weight / 30;
		return result;
	}

	public ArrayList<HashMap<String, String>> getFoodClassList() {

		/*
		 * return class id(class), ontology class name(des)
		 */

		ArrayList<HashMap<String, String>> dataList = new ArrayList<HashMap<String, String>>();

		try {

			// each DB has its own driver, so we load it
			Class.forName("com.mysql.jdbc.Driver");

			// setup connection with DB
			connect = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connect.createStatement();

			// your query
			resultSet = statement.executeQuery("");

			// get result
			while (resultSet.next()) {

				HashMap<String, String> map = new HashMap<String, String>();
				String index = resultSet.getString("");
				String des = resultSet.getString("");
				map.put("", index);
				map.put("", des);

				dataList.add(map);

			}

			// clean environment
			resultSet.close();
			statement.close();
			connect.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return dataList;
	}

	public ArrayList<HashMap<String, String>> getSpecificDataList() {

		ArrayList<HashMap<String, String>> dataList = new ArrayList<HashMap<String, String>>();

		try {

			Class.forName("com.mysql.jdbc.Driver");
			connect = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connect.createStatement();

			// your query
			resultSet = statement.executeQuery("");

			// get result
			while (resultSet.next()) {

				HashMap<String, String> map = new HashMap<String, String>();
				String tid = resultSet.getString("");
				String index = resultSet.getString("");
				String des = resultSet.getString("");
				map.put("", tid);
				map.put("", index);
				map.put("", des);

				dataList.add(map);
			}

			// clean environment
			resultSet.close();
			statement.close();
			connect.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return dataList;
	}

	public ArrayList<HashMap<String, String>> getHistoryDataList() {

		ArrayList<HashMap<String, String>> dataList = new ArrayList<HashMap<String, String>>();

		try {

			// each DB has its own driver, so we load it
			Class.forName("com.mysql.jdbc.Driver");

			// setup connection with DB
			connect = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connect.createStatement();

			// your query
			resultSet = statement
					.executeQuery("");

			// get result
			while (resultSet.next()) {

				HashMap<String, String> map = new HashMap<String, String>();
				String tag = resultSet.getString("");
				String time = resultSet.getString("");
				map.put("", tag);
				map.put("", time);

				dataList.add(map);
			}

			// clean environment
			resultSet.close();
			statement.close();
			connect.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return dataList;
	}

	public HashMap<String, String> getUserProfile() {

		HashMap<String, String> profileMap = new HashMap<String, String>();

		try {

			// each DB has its own driver, so we load it
			Class.forName("com.mysql.jdbc.Driver");

			// setup connection with DB
			connect = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connect.createStatement();

			// your query
			resultSet = statement
					.executeQuery("");
			resultSet.next();

			// pay attention to the numbers
			String uid = resultSet.getString("");
			profileMap.put("", uid);


			resultSet.close();
			statement.close();
			connect.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return profileMap;
	}
	
	private ArrayList<HashMap<String, String>> getDishList() {
		ArrayList<HashMap<String, String>> dataList = new ArrayList<HashMap<String, String>>();

		try {

			// each DB has its own driver, so we load it
			Class.forName("com.mysql.jdbc.Driver");

			// setup connection with DB
			connect = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connect.createStatement();

			// your query
			resultSet = statement.executeQuery("");

			// get result
			while (resultSet.next()) {

				HashMap<String, String> map = new HashMap<String, String>();
				String did = resultSet.getString("");
				String rid = resultSet.getString("");
				map.put("", did);
				map.put("", rid);


				dataList.add(map);

			}

			// clean environment
			resultSet.close();
			statement.close();
			connect.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return dataList;
	}

	private ArrayList<HashMap<String, String>> getRestaurantList() {

		ArrayList<HashMap<String, String>> dataList = new ArrayList<HashMap<String, String>>();

		try {

			// each DB has its own driver, so we load it
			Class.forName("com.mysql.jdbc.Driver");

			// setup connection with DB
			connect = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connect.createStatement();

			// your query
			resultSet = statement.executeQuery("");

			// get result
			while (resultSet.next()) {

				HashMap<String, String> map = new HashMap<String, String>();
				String rid = resultSet.getString("");
				String name = resultSet.getString("");
				String lat = resultSet.getString("");
				String lon = resultSet.getString("");
				map.put("", lat);
				map.put("", lon);
				map.put("", rid);
				map.put("", name);

				dataList.add(map);

			}

			// clean environment
			resultSet.close();
			statement.close();
			connect.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return dataList;
	}

}
