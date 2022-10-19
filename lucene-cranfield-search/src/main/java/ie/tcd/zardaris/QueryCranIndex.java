package ie.tcd.zardaris;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Scanner;

import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;

public class QueryCranIndex
{

	// the location of the search index
	private static String INDEX_DIRECTORY = "index";
	private static String CRAN_DIRECTORY = "../cran";
	private static String RESULT_FILE = "results.txt";
	
	// Limit the number of search results we get
	private static int MAX_RESULTS = 10;
	
	public static void Query() throws IOException, ParseException
	{
		// Analyzer used by the query parser.
		// Must be the same as the one used when creating the index
		Analyzer analyzer = new StandardAnalyzer();
		
		// Open the folder that contains our search index
		Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
		
		// create objects to read and search across the index
		DirectoryReader ireader = DirectoryReader.open(directory);
		IndexSearcher isearcher = new IndexSearcher(ireader);
		
		// Create the query parser.
		String[] fields = {"Title", "Author", "Bibliography", "Textual"};
		MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
		
		// query the cran index
		queryIndex(parser, isearcher);
		
		// close everything and quit
		ireader.close();
		directory.close();
	}
	
	public static void queryIndex(MultiFieldQueryParser parser, IndexSearcher isearcher) throws IOException, ParseException
	{
		try {
			Path path = Paths.get(RESULT_FILE);
			Files.deleteIfExists(path);
			BufferedReader br = new BufferedReader(new FileReader(CRAN_DIRECTORY + "/cran.qry"));
			String line = br.readLine(); 
			while (line != null)  
			{  
			   String[] splited = line.split("\\s+");
			   if(splited[0].equals(".I"))
			   {
				   String id = splited[1];
				   String queryString = "";
				   Boolean content = false;
				   System.out.println(id);
				   line = br.readLine();
				   
				   while (!((line).split("\\s+")[0].equals(".I")))
				   {
					   if(line.equals(".W"))
					   {
						   content = true;
						   line = br.readLine();  
					   }
					   
					   line = line.replace("?", "");
					   
					   if(line.charAt(0) != ' ' && !queryString.equals(""))
						   line = " " + line;
					   
					   if(content)
						   queryString+=line;
					   
					   line = br.readLine();
					   
					   if(line == null)
						   break;
				   }
				   
				   Query query = parser.parse(queryString);
				   ScoreDoc[] hits = isearcher.search(query, MAX_RESULTS).scoreDocs;
				   System.out.println("Documents: " + hits.length);
				   for (int i = 0; i < hits.length; i++)
				   {
					   Document hitDoc = isearcher.doc(hits[i].doc);
					   
					   id = id.replaceFirst("^0+(?!$)", "");
					   
					   FileWriter fw = new FileWriter(RESULT_FILE, true); //the true will append the new data
					   fw.write(id + " 0 " + hitDoc.get("ID") + " 0 " + hits[i].score + " STANDARD\n");//appends the string to the file
					   fw.close();
					   
					   System.out.println(id + " " + hitDoc.get("ID") + " " + hits[i].score);
				   }

					System.out.println();	
			   }
			} 
			
			br.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}  
		
	}
	
}
