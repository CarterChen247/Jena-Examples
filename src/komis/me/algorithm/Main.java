package komis.me.algorithm;

import java.io.InputStream;
import java.sql.DriverManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.TimeZone;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.function.library.date;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.XSD;

public class Main {

	/*
	 * This class is for inference user preferences automatically. Is not the
	 * main application for reasoning.
	 */

	// JDBC , will change to jena one day
	private java.sql.Connection connect = null;
	private java.sql.Statement statement = null;
	private java.sql.ResultSet resultSet = null;

	static final String DB_URL = "jdbc:mysql://localhost/*";

	// Database credentials
	static final String USER = "*";
	static final String PASS = "*";
	static String[] foodClasses;
	static double[] interests;

	static long update; // time
	static String lastUpdateTime; 
	static long current = System.currentTimeMillis();
	static int days;
	static final double gain = 2;// interest
//	static final int day = 24 * 60 * 60 * 1000;
	static final int STARTFROM = 3;

	static String uid;

	public static void main(String[] args) {

		uid = args[0];

		// initial namespace
		String MLNR = "http://www.example.com/*.owl#";

		// create a Hashmap for storing Ontclasses
		HashMap<String, OntClass> OntClassCache = new HashMap<String, OntClass>();

		Main main = new Main();

		// create owl model
		OntModel schema = ModelFactory.createOntologyModel(); // TODO

		/*
		 * create OntClasses
		 */
		OntClass Food = schema.createClass(MLNR + "Food");
		OntClass Restaurant = schema.createClass(MLNR + "Restaurant");
		OntClass Dish = schema.createClass(MLNR + "Dish");
		OntClass User = schema.createClass(MLNR + "User");

		/*
		 * create properties
		 */

		// tag
		ObjectProperty tag = schema.createObjectProperty(MLNR + "");
		tag.addDomain(Dish);
		tag.addRange(Food);

		// datatypes
		DatatypeProperty name = schema.createDatatypeProperty(MLNR + "");
		name.addDomain(Restaurant);
		name.addRange(XSD.xstring);

		// datatypes
		DatatypeProperty value = schema.createDatatypeProperty(MLNR + "");
		name.addDomain(Food);
		name.addRange(XSD.xdouble);

		// open class structure
		InputStream in = FileManager.get().open(
				"C:\\.......\\*.owl");
		if (in == null) {
			throw new IllegalArgumentException("File: not found");
		}

		schema.read(in, null);

		// initial generic classes
		ArrayList<HashMap<String, String>> foodClassList = new ArrayList<HashMap<String, String>>(); // TODO
		foodClassList = main.getFoodClassList();
		foodClasses = new String[foodClassList.size() + 1];

		for (int i = 0; i < foodClassList.size(); i++) {
			HashMap<String, String> classes = foodClassList.get(i);

			OntClass subClass = schema.createClass(MLNR + classes.get("")); // 2.
																				// create
																				// subclass
			Food.addSubClass(subClass); // 3. connect superclass and subclass

			OntClassCache.put(classes.get(""), subClass); // start from 1

			int j = i; // save the name in foodClasses
			foodClasses[++j] = classes.get("");
//			System.out.println(j + "" + classes.get("name"));
		}

		// initial specific classes
		ArrayList<HashMap<String, String>> specificDataList = new ArrayList<HashMap<String, String>>();
		specificDataList = main.getSpecificDataList();

		for (int i = 0; i < specificDataList.size(); i++) {
			HashMap<String, String> map = specificDataList.get(i);

			OntClass superClass = OntClassCache.get(map.get("")); // 1. get
																		// superclass
//			System.out.println(map.get("index"));

			superClass.createIndividual(MLNR + map.get(""));
//			System.out.println(map.get("name"));

			OntClass subClass = schema.createClass(MLNR + map.get(""));
			superClass.addSubClass(subClass);// 2. create subclasses

		}
		// check user profile
		main.checkUserProfile();

		// fetch user history
		ArrayList<HashMap<String, String>> historyDataList = new ArrayList<HashMap<String, String>>();
		historyDataList = main.getHistoryDataList();

		// fetch user profile (dimensions)
		HashMap<String, String> profile = new HashMap<String, String>();
		profile = main.getUserProfile(); // TODO get user profile

		for (int i = STARTFROM; i <= profile.size(); i++) {

			OntClass foodClass = schema.getOntClass(MLNR + foodClasses[i]);
			foodClass.addProperty(value, profile.get(foodClasses[i]));

		}

		for (int i = 0; i < historyDataList.size(); i++) {
			HashMap<String, String> map = historyDataList.get(i);
			String tagName = map.get("");
			if (i == 0) {
				String time = map.get("");
				profile.put("", time);

				// convert datetime to long
				SimpleDateFormat format = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss");

				try {
					Date date = format.parse(time);
					update = date.getTime();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}

			// get superclass of tags
			OntClass tagClass = schema.getOntClass(MLNR + tagName);

			String localName = tagClass.getSuperClass().getLocalName();

			try {
				// get superClass
				OntClass foodClass = schema.getOntClass(MLNR + localName);

				// calculate
				Double interestValue = Double.parseDouble(foodClass
						.getPropertyValue(value).toString());
				interestValue = interestValue + gain;
				foodClass.addProperty(value, interestValue.toString());

			} catch (Exception e) {

				e.printStackTrace();

			}

		}



		interests = new double[foodClasses.length];

		for (int i = STARTFROM; i <= profile.size(); i++) {

			OntClass foodClass = schema.getOntClass(MLNR + foodClasses[i]);
			Double finalInterestValue = Double.parseDouble(foodClass
					.getPropertyValue(value).toString());
			
			double random = new Random().nextInt(10);
			finalInterestValue = finalInterestValue + (random/10000);
			
			interests[i] = finalInterestValue;

		}

		main.updateUserProfile(); // TODO update user profile

		
		System.out.println("[MODULE] update user profile complete");
	}

	private void updateUserProfile() {

		// your query
		String sql = "";

		try {

			// each DB has its own driver, so we load it
			Class.forName("com.mysql.jdbc.Driver");

			// setup connection with DB
			connect = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connect.createStatement();
			statement.executeUpdate(sql);

			statement.close();
			connect.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private String getCurrentTime() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));

		// get current time
		String currentTime = dateFormat.format(current);
		return currentTime;

	}


	private ArrayList<HashMap<String, String>> getFoodClassList() {

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

	public void checkUserProfile() {

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

			String userID = resultSet.getString("");

		} catch (Exception e) {
			System.out.println("");
			
			// if user does not exist

//			String sql = "INSERT INTO profile (uid) VALUES (" + uid + ")";
//
//			try {
//
//				// each DB has its own driver, so we load it
//				Class.forName("com.mysql.jdbc.Driver");
//
//				// setup connection with DB
//				connect = DriverManager.getConnection(DB_URL, USER, PASS);
//				statement = connect.createStatement();
//				statement.executeUpdate(sql);
//
//				statement.close();
//				connect.close();
//
//			} catch (Exception ex) {
//
//				ex.printStackTrace();
//
//			}
		}
		

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

			lastUpdateTime = resultSet.getString("");  // TODO time
			

		} catch (Exception ex) {
			
			ex.printStackTrace();
			
		}

	}
}