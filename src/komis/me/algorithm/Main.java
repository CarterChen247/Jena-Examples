package komis.me.algorithm;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.sql.DriverManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.Subject;

import org.apache.jena.atlas.json.JSON;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasonerFactory;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.sparql.function.library.print;
import com.hp.hpl.jena.sparql.resultset.JSONOutput;
import com.hp.hpl.jena.sparql.resultset.JSONOutputResultSet;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;
import com.hp.hpl.jena.sparql.resultset.SPARQLResult;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.VCARD;
import com.hp.hpl.jena.vocabulary.XSD;

public class Main {

	private java.sql.Connection connect = null;
	private java.sql.Statement statement = null;
	private java.sql.ResultSet resultSet = null;

	static final String DB_URL = "jdbc:mysql://localhost/*";

	// Database credentials
	static final String USER = "*";
	static final String PASS = "*";

	// configure your namespace for ontology
	static final String MLNR = "http://www.example.com/*.owl#";
	
	// the path of your ontology
	static final String ontologyAddress = "C:\\.....*.owl";

	private static String uid;
	private static int category;
	private static boolean urgent;
	private static String context;
	private static String similarUsers;

	private ArrayList<String> loveDishesList;
	private List<String> similarUserList;
	private UserContext userContext;
	private String feeling;
	private String foodClasses[];

	public static void main(String[] args) {

		// get arguments
		uid = args[0];
		category = Integer.parseInt(args[1]);
		
		// TODO optimize it
		if (category == 0){
			category = -1;
		}
		
		urgent = Boolean.parseBoolean(args[2]);
		context = args[3];
		similarUsers = args[4]; // shows "empty" if user didn't select
								// personalization

		Main main = new Main();
		main.infer();

	}

	private void infer() {
		
		retrieveJSON();
		getLovedDishes();
		startInference();

	}

	private void startInference() {

		
		// insert ontology model
		OntModel model = ModelFactory.createOntologyModel();
		InputStream in = FileManager.get().open(ontologyAddress);
		if (in == null) {
			throw new IllegalArgumentException("File: not found");
		}
		model.read(in, null);
		model.setNsPrefix("MLNR", MLNR);
		
		OntClass ontClassUser = model.getOntClass(MLNR + "User");
		DatatypeProperty feel = model.createDatatypeProperty(MLNR + "feel");
		feel.addDomain(ontClassUser);
		feel.addRange(XSD.xstring);
		Individual currentUser = ontClassUser.createIndividual(MLNR + "currentUser");
		currentUser.addProperty(feel, feeling);

		// insert your rule
		List<Rule> rules = Rule
				.rulesFromURL("C:\\......*.rules");
		Reasoner reasoner = new GenericRuleReasoner(rules);
		reasoner.setDerivationLogging(true);
		InfModel inf = ModelFactory.createInfModel(reasoner, model);
		

		// Situation I. non-personalization
		if (similarUsers.equals("empty")) {
			


			// Situation II. personalize
		} else if (loveDishesList.size() >= 1) {

			OntClass ontClassCurrentUser = model.getOntClass(MLNR + "User");
			OntClass ontClassDish = model.createClass(MLNR + "Dish");
			
			ObjectProperty like = model.createObjectProperty(MLNR + "like");
			like.addDomain(ontClassCurrentUser);
			like.addRange(ontClassDish);
			
			Individual individualUSER = ontClassCurrentUser.createIndividual(MLNR + "currentUser");//TODO
			for (int index = 0; index < loveDishesList.size(); index++) {
				Individual dish = ontClassDish.createIndividual(MLNR + "D"
						+ loveDishesList.get(index));
				individualUSER.addProperty(like, dish);
			} 
			
			InfModel newInf = ModelFactory.createInfModel(reasoner, model);
			
			// if user has selected a category
			if (category != -1) { 

				String foodClass = null;
				for (int index = 1; index <= 13; index++) {
					
					if (index == category){
						foodClass = foodClasses[index];
					}
					
				}
				
				// TODO p-1
				String queryString = "PREFIX MLNR:  <http://www.example.com/*.owl#>\n"
						+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "SELECT ?a, ?b \n" 
						+ "WHERE{ ?a MLNR:* ?b . ?b MLNR:* ?c .  ?c rdf:type MLNR:*  }";
				Query query = QueryFactory.create(queryString);
				QueryExecution qexec = QueryExecutionFactory.create(query,	newInf);
				ResultSet results = qexec.execSelect();
				ResultSetFormatter.outputAsJSON(System.out, results);
			
			// if user did not select a category
			}else{
				
				// TODO p-2
				String queryString = "PREFIX MLNR:  <http://www.example.com/*.owl#>\n"
						+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "SELECT ?a, ?b \n" 
						+ "WHERE{ ?a MLNR:* ?b . ?b MLNR:* ?c .  ?c rdf:type MLNR:*  }";
				Query query = QueryFactory.create(queryString);
				QueryExecution qexec = QueryExecutionFactory.create(query,	newInf);
				ResultSet results = qexec.execSelect();
				ResultSetFormatter.outputAsJSON(System.out, results);
				
			}
			
			
		} else {

			System.out.println("inference engine: can not determine similar users");

		}

	}

	private void retrieveJSON() {

		// decode JSON
		Gson gson = new Gson();

		// 1. get similar users
		Type listType = new TypeToken<ArrayList<String>>() {
		}.getType();
		similarUserList = gson.fromJson(similarUsers, listType);

		// 2. get context information
		userContext = gson.fromJson(context, UserContext.class);
		
		double temperature = Double.parseDouble(userContext.getTemperature());
		
		// TODO temperature. If the weather is too hot
		if (temperature > 78.8){
			feeling = "hot";
			
		// if the weather is too cold
		}else if (temperature < 68){
			feeling = "cold";
			
		}
		
		
	}

	public void getLovedDishes() {

		loveDishesList = new ArrayList<String>();

		try {

			Class.forName("com.mysql.jdbc.Driver");
			connect = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connect.createStatement();

			// your query
			resultSet = statement
					.executeQuery("");

			// get result
			while (resultSet.next()) {

				String did = resultSet.getString("");

				loveDishesList.add(did);
			}

			resultSet.close();
			statement.close();
			connect.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private String getSimilarUserQueryString() {

		String queryString = "";

		if (similarUserList.size() > 1) {

			for (int index = 0; index < similarUserList.size(); index++) {

				// the first data, doesn't need a common
				if (index == 0) {
					queryString = queryString + similarUserList.get(index);
				} else {
					queryString = queryString + ",";
					queryString = queryString + similarUserList.get(index);
				}
			}

		} else {

			// get first data
			queryString = similarUserList.get(0);

		}

		return queryString;
	}
}
