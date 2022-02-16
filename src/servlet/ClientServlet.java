package servlet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasonerFactory;
import org.apache.jena.vocabulary.ReasonerVocabulary;
import org.json.JSONObject;

import utils.Utils;

/**
 * Servlet implementation class ClientServlet
 */
@WebServlet("/ClientServlet")
public class ClientServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
    public ClientServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    
	    String reqBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
	    
	    JSONObject json = new JSONObject(reqBody);
	    
	    String src = json.get("src").toString();
	    String rules = json.get("rules").toString();
	    String queryStr = json.get("query").toString();
	    
	    PrintWriter out = response.getWriter();
	    
	    final String SUBJECT = "subject";
        final String PREDICATE = "predicate";
        final String OBJECT = "object";
	    
        // Parsing the RDFa content from the given URL of XHTML file
	    Model m = null;
        try {
            Reader in = Utils.getRequest(src);
            m = ModelFactory.createDefaultModel();
            m.read(in, "", "RDF/XML");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            out.write(e.getMessage());
            out.flush();
            out.close();
            return;
        }
        
        
        File file = new File("demo.rules");
        file.createNewFile();
        
        // Writing the given rules to a temporary file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(rules);
            writer.close();
        }catch (IOException e) {
            System.err.println(e.getMessage());
        }

         
        Resource conf = m.createResource();
        conf.addProperty(ReasonerVocabulary.PROPruleMode, "hybrid");
        conf.addProperty(ReasonerVocabulary.PROPruleSet, file.getAbsolutePath());
        
        // Creating a reasoner from the rules file
        Reasoner reasoner = null;
        try {
            reasoner = GenericRuleReasonerFactory.theInstance().create(conf);
        } catch(Exception e) {
            System.err.println(e.getMessage());
            response.setStatus(400);
            out.write(e.getMessage());
            out.flush();
            out.close();
            return;
        }
        
        // Applying the rules to the RDF data possibly inferring new triples
        InfModel infmodel = ModelFactory.createInfModel(reasoner, m);
	    
        // Creating a query from the given query text
        Query q = null;
        try {
            q = QueryFactory.create(queryStr);
        }catch (QueryParseException e) {
            System.err.println(e.getMessage());
            response.setStatus(400);
            out.write(e.getMessage());
            out.flush();
            out.close();
            return;
        }
	    
	    HashMap<String, List<String>> results = new HashMap<>();
        
	    QueryExecution qexec = QueryExecutionFactory.create(q, infmodel);
	    
	    try { //First try SELECT query
	        ResultSet resultSet = qexec.execSelect();
	        List<String> variables = resultSet.getResultVars();
	        variables.forEach(s -> results.put(s, new ArrayList<>()));
	        
	        while (resultSet.hasNext()) {
	            QuerySolution row = resultSet.next();
	            row.varNames().forEachRemaining(s -> {

	                results.get(s).add(row.get(s).toString());
	                
	            });
	        }
	    } catch (QueryExecException | QueryParseException e) {
	        
	        try { //Next try CONSTRUCT query
	            Model resultModel = qexec.execConstruct();
	            StmtIterator stmts = resultModel.listStatements();
                
                results.put(SUBJECT, new ArrayList<>());
                results.put(PREDICATE, new ArrayList<>());
                results.put(OBJECT, new ArrayList<>());
                
                while (stmts.hasNext()) {
                    Statement s = stmts.next();
                    results.get(SUBJECT).add(s.getSubject().toString());
                    results.get(PREDICATE).add(s.getPredicate().toString());
                    results.get(OBJECT).add(s.getObject().toString());
                }
	        } catch (QueryExecException | QueryParseException e2 ) {
	            
	            try { //Next try DESCRIBE query
	                
	                QueryExecution qexec2 = QueryExecutionFactory.create(q, infmodel);
	                Model resultModel = qexec2.execDescribe();
	                StmtIterator stmts = resultModel.listStatements();      
	                
	                results.put(SUBJECT, new ArrayList<>());
	                results.put(PREDICATE, new ArrayList<>());
	                results.put(OBJECT, new ArrayList<>());
	                
	                while (stmts.hasNext()) {
	                    Statement s = stmts.next();
	                    results.get(SUBJECT).add(s.getSubject().toString());
	                    results.get(PREDICATE).add(s.getPredicate().toString());
	                    results.get(OBJECT).add(s.getObject().toString());
	                }
	            } catch (QueryExecException | QueryParseException e3) {
	                
	                try { //Finally try ASK query
	                    QueryExecution qexec2 = QueryExecutionFactory.create(q, infmodel);
	                    boolean result = qexec2.execAsk();
	                    
	                    results.put("result", new ArrayList<String>());
	                    
	                    if (result) {                       
	                        results.get("result").add("yes");
	                    }
	                    else {
	                        results.get("result").add("no");
	                    }
	                } catch (QueryExecException | QueryParseException e4) { //None of the queries succeeded
	                    
	                    System.err.println(e3.getMessage());
	                    response.setStatus(400);
	                    if (e3.getMessage() != null) {
	                        out.write(e3.getMessage());
	                    }
	                    else {
	                        out.write("Malformed query");
	                    }
	                    out.flush();
	                    out.close();
	                    return;
	                    
	                    
	                }                   
	                
	            }
	               
	        }   
	        
	    }

	    
	    JSONObject resultJson = new JSONObject(results);
	    
	    out.write(resultJson.toString());
	    out.flush();
	    out.close();
        
	}

}
