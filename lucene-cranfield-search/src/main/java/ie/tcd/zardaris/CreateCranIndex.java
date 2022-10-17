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
			
			// ArrayList of documents in the corpus
			ArrayList<Document> documents = new ArrayList<Document>();
	
			// Open the directory that contains the search index
			Directory directory;
			directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
	
			// Set up an index writer to add process and save documents to the index
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
			IndexWriter iwriter = new IndexWriter(directory, config);
			indexDocument(iwriter);
			
	//		for (String arg : args)
	//		{
	//			// Load the contents of the file
	//			System.out.printf("Indexing \"%s\"\n", arg);
	//			String content = new String(Files.readAllBytes(Paths.get(arg)));
	//
	//			// Create a new document and add the file's contents
	//			Document doc = new Document();
	//			doc.add(new StringField("filename", arg, Field.Store.YES));
	//			doc.add(new TextField("content", content, Field.Store.YES));
	//
	//			// Add the file to our linked list
	//			documents.add(doc);
	//		}
	
			// Write all the documents in the linked list to the search index
			//iwriter.addDocuments(documents);
	
			// Commit everything and close
			iwriter.close();
			directory.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void indexDocument(IndexWriter iwriter)
	{
		try {
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
				   System.out.println(id);
				   line = br.readLine();
				   
				   while (!((line).split("\\s+")[0].equals(".I")))
				   {
					   if(line.equals(".T"))
					   {
						   current = "title";
						   line = br.readLine();  
					   }
					   else if(line.equals(".A"))
					   {
						   current = "author";
						   line = br.readLine();
					   }
					   else if(line.equals(".B"))
					   {
						   current = "bibliography";
						   line = br.readLine();
					   }
					   else if(line.equals(".W"))
					   {
						   current = "textual";
						   line = br.readLine();
					   }
					   
					   if(line.charAt(0) != ' ')
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
					   
					   line = br.readLine();
					   
					   if(line == null)
						   break;
				   }
				   System.out.println("id: " + id);
				   System.out.println("title: " + title);
				   System.out.println("author: " + author);
				   System.out.println("bibliography: " + bibliography);
				   System.out.println("textual: " + textual);
				   
				   Document doc = new Document();
				   doc.add(new StringField("ID", id, Field.Store.YES));
				   doc.add(new TextField("Title", title, Field.Store.YES));
				   doc.add(new TextField("Author", author, Field.Store.YES));
				   doc.add(new TextField("Bibliography", bibliography, Field.Store.YES));
				   doc.add(new TextField("Textual", textual, Field.Store.YES));
		
				  
				   iwriter.addDocument(doc);
			   }
			} 
		} catch (IOException e) {
			e.printStackTrace();
		}  
		
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
