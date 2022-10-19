package ie.tcd.zardaris;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;

import java.nio.file.Paths;
import java.nio.file.Files;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
 
public class CreateCranIndex
{
	// Directory where the search index will be saved
	private static String INDEX_DIRECTORY = "index";
	private static String CRAN_DIRECTORY = "../cran";

	public static void CreateIndex()
	{
		try {
			
			Analyzer analyzer = new StandardAnalyzer();
	
			// Open the directory that contains the search index
			Directory directory;
			directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
	
			// Set up an index writer to add process and save documents to the index
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
			IndexWriter iwriter = new IndexWriter(directory, config);
			
			indexDocument(iwriter);
	
			// Commit everything and close
			iwriter.close();
			directory.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
				   //System.out.println(id);
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
				   
				   if(textual != "" && textual.charAt(0) == ' ')
					   textual = textual.substring(1);
				   
				   if(id.equals("995") || id.equals("996") )
				   {
					   System.out.println("id: " + id);
					   System.out.println("title: " + title);
					   System.out.println("author: " + author);
	     			   System.out.println("bibliography: " + bibliography);
					   System.out.println("textual: " + textual);
				   }
					   
// 				   System.out.println("id: " + id);
//				   System.out.println("title: " + title);
//				   System.out.println("author: " + author);
//     			   System.out.println("bibliography: " + bibliography);
//				   System.out.println("textual: " + textual);
//				   
				   Document doc = new Document();
				   doc.add(new StringField("ID", id, Field.Store.YES));
				   doc.add(new TextField("Title", title, Field.Store.YES));
				   doc.add(new TextField("Author", author, Field.Store.YES));
				   doc.add(new TextField("Bibliography", bibliography, Field.Store.YES));
				   doc.add(new TextField("Textual", textual, Field.Store.YES));
		
				  
				   iwriter.addDocument(doc);
			   }
			} 
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}  
		
	}
	
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

	public static void main(String[] args) throws IOException, ParseException
	{
		if (args[0].equals("create"))
		{
			CreateIndex();
		}
		else if (args[0].equals("query")) 
		{
			QueryCranIndex.Query();
		}
	}
}
