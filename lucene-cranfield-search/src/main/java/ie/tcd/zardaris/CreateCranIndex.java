package ie.tcd.zardaris;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Paths;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
 
public class CreateCranIndex
{
	// Directory where the search index will be saved
	private static String INDEX_DIRECTORY = "index";
	private static String CRAN_DIRECTORY = "../cran";
	
	public static final List<String> ENGLISH_STOP_WORDS = Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by",
																	"for", "if", "in", "into", "is", "it",
																	"no", "not", "of", "on", "or", "such",
																	"that", "the", "their", "then", "there", "these",
																	"they", "this", "to", "was", "will", "with");
	
	public static void CreateIndex(String analyzerType, String similarityType)
	{
		try {
			
			// Set up the analyzer using input
			Analyzer analyzer = null;
			
			Map<String,Analyzer> analyzerMap = new HashMap<>();
			analyzerMap.put("standard", new StandardAnalyzer());
			analyzerMap.put("stop", new StopAnalyzer(new CharArraySet(ENGLISH_STOP_WORDS, false)));
			analyzerMap.put("simple", new SimpleAnalyzer());
			analyzerMap.put("whitespace", new WhitespaceAnalyzer());
			analyzerMap.put("english", new EnglishAnalyzer());
			
			if(analyzerMap.containsKey(analyzerType)) analyzer = analyzerMap.get(analyzerType);
			else 
			{
				analyzer = new StandardAnalyzer();
				analyzerType = "standard";	
			}
	
			// Open the directory that contains the search index
			Directory directory;
			directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
	
			// Set up an index writer to add process and save documents to the index
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
			
			// Set up the similarity using input
			Map<String,Similarity> similarityMap = new HashMap<>();
			similarityMap.put("BM25", new BM25Similarity());
			similarityMap.put("TFIDF", new ClassicSimilarity());
			similarityMap.put("boolean", new BooleanSimilarity());
			similarityMap.put("LMDirichlet", new LMDirichletSimilarity());
			
			if(similarityMap.containsKey(similarityType)) config.setSimilarity(similarityMap.get(similarityType));
			else 
			{
				config.setSimilarity(new BM25Similarity());
				similarityType = "BM25";
			}
			
			System.out.println("Index created using " + analyzerType + " analyzer and " + similarityType + " similarity.");
			
			IndexWriter iwriter = new IndexWriter(directory, config);
			
			indexDocument(iwriter);
	
			// Commit everything and close
			iwriter.close();
			directory.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Run through cran.all.1400 & add documents to index 
	@SuppressWarnings("serial")
	public static void indexDocument(IndexWriter iwriter)
	{
		try {
			ArrayList<String> types = new ArrayList<String>() { 
	            { 
	                add(".I"); 
	                add(".T"); 
	                add(".A"); 
	                add(".B"); 
	                add(".W"); 
	            } 
	        }; 
	        
	        // read cranfield file
			BufferedReader br = new BufferedReader(new FileReader(CRAN_DIRECTORY + "/cran.all.1400"));
			String line = br.readLine(); 
			while (line != null)  
			{  
			   String[] splited = line.split("\\s+");
			   if(splited[0].equals(".I"))
			   {
				   String id = splited[1];
				   String title = "";
				   String author = "";
				   String textual = "";
				   String bibliography = "";
				   String current = "";
				   line = br.readLine();
				   
				   while (!((line).split("\\s+")[0].equals(".I")))
				   {
					   String fieldCheck = fieldType(line);
					   if(!fieldCheck.equals(""))
					   {
						   current = fieldCheck;
						   line = br.readLine(); 
						   if(types.contains(line) || line.split("\\s+")[0].equals(".I") )
							   current = "none";
					   }
					   
					   if(line.charAt(0) != ' ' && current.equals("textual"))
						   line = " " + line;
					   
					   switch(current) 
					   {
					   		case "title":
					   			title+=line;
					   			break;
					   		case "author":
					   			author+=line;
					   			break;
					   		case "bibliography":
					   			bibliography+=line;
					   			break;
					   		case "textual":
					   			textual+=line;
					   			break;	
					   }
					   
					   if (!current.equals("none"))
						   line = br.readLine();
					   
					   if(line == null)
						   break;
				   }
				   
				   // remove extra spaces
				   if(textual != "" && textual.charAt(0) == ' ')
					   textual = textual.substring(1);
				   
				   // Create document with relevant fields
				   Document doc = new Document();
				   doc.add(new StringField("ID", id, Field.Store.YES));
				   doc.add(new TextField("Title", title, Field.Store.YES));
				   doc.add(new TextField("Author", author, Field.Store.YES));
				   doc.add(new TextField("Bibliography", bibliography, Field.Store.YES));
				   doc.add(new TextField("Textual", textual, Field.Store.YES));
				   
				   // add document to index
				   iwriter.addDocument(doc);
			   }
			} 
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}  
		
	}
	
	// Check current section of cran.all.1400
	public static String fieldType(String line)
	{
	   if(line.equals(".T"))
	   {
		   return "title";  
	   }
	   else if(line.equals(".A"))
	   {
		   return "author";
	   }
	   else if(line.equals(".B"))
	   {
		   return "bibliography";
	   }
	   else if(line.equals(".W"))
	   {
		   return "textual";
	   }
	   
	   return "";
	}

	// Parse command line input and run relevant functions
	public static void main(String[] args) throws IOException, ParseException
	{
		if(args.length == 0)
			System.out.println("please insert command");
		
		String command = "";
		String analyzerType = "";
		String similarityType = "";
		
		if(args.length > 0 ) command = args[0];
		if(args.length > 1)	 analyzerType = args[1];
		if(args.length > 2)	 similarityType = args[2];
		
		if (command.equals("create"))
			CreateIndex(analyzerType, similarityType);
		else if (command.equals("query")) 
			QueryCranIndex.Query(analyzerType);
		else if (command.equals("create-query"))
		{
			CreateIndex(analyzerType, similarityType);
			QueryCranIndex.Query(analyzerType);
		}
		else	
			System.out.println("invalid command");
	}
}
