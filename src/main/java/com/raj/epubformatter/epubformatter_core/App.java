package com.raj.epubformatter.epubformatter_core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.epub.EpubWriter;
import nl.siegmann.epublib.service.MediatypeService;

/**
 * Hello world!
 *
 */
public class App 
{
	
	Set<String> tocSet = new LinkedHashSet<String>();
	List<String> tocList = new ArrayList<String>();
	
	
	public  void JsoupReader(InputStream inputStream, Book newBook, List<String> tocList){
		
		//File input = new File("src/resources/sample_book.htm.html");
    	try {
			Document doc = Jsoup.parse(inputStream, "UTF-8","");
			Element head = doc.select("head").first();
			String firstPubId = (tocList.size()>=1)?tocList.get(0):"NoId";
			Element firstId  = doc.getElementById(firstPubId);
			if(firstId!=null){
			Elements siblings = firstId.siblingElements();
			String h2Text = firstId.html();
			List<Element> elementsBetween = new ArrayList<Element>();
			for(int i=1;i<siblings.size(); i++){ 
				Element sibling = siblings.get(i);
				String siblingId = sibling.id();
				String nextPubId = (tocList.size()>=2)?tocList.get(1):"NoId";
				if(!siblingId.equals(nextPubId)){
					elementsBetween.add(sibling);
				}else{
					
					processElementsBetween(h2Text, head, elementsBetween, newBook);
				      elementsBetween.clear();
				      h2Text = sibling.html();
				}
			}
			
			 if (! elementsBetween.isEmpty()){
				 processElementsBetween(h2Text, head, elementsBetween, newBook);
			 }
				    
			}
			 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
	}
	
	private  void processElementsBetween(String h2Text,Element head,
		    List<Element> elementsBetween, Book newBook) throws IOException {
		
		File newHtmlFile = new File("src/resources/"+h2Text+".html");
		StringBuffer htmlString = new StringBuffer("");
		htmlString.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
		htmlString.append(head);
		htmlString.append("<body>"
				+"<div class=\"c2\">"
				+"</div>");
		htmlString.append("<h2>"
				+h2Text
				+"</h2>");
		  for (Element element : elementsBetween) {
			  htmlString.append(element.toString());
		  }
		  htmlString.append("</body></html>");
		  FileUtils.writeStringToFile(newHtmlFile, htmlString.toString());
		  
		  InputStream fis = new FileInputStream(newHtmlFile);
		  newBook.addSection(h2Text, new Resource(fis,h2Text+".html"));
		  tocList.remove(0);
		}
	
	private  void ePubReaderAndFormatter(){
		EpubReader epubReader = new EpubReader();
		File input = new File("src/resources/pg74-images.epub");
		try {
			InputStream fis = new FileInputStream(input);
			Book book = epubReader.readEpub(fis);
			Book newBook =  new Book();
			Metadata metadata = newBook.getMetadata();
			metadata.addTitle(book.getTitle());
			newBook.setCoverImage(book.getCoverImage());
			
			Resources resources = book.getResources();
			Collection<Resource> resourceCollection =  resources.getAll();
			
			Map<String, Resource> resourceMap = new HashMap<String, Resource>();
			
			for(Resource resource:resourceCollection){
				if(resource.getMediaType() == MediatypeService.XHTML){
					resourceMap.put(resource.getId(), resource);
					//JsoupReader(resource.getInputStream(), newBook);
				}else if(resource.getMediaType()== MediatypeService.CSS){
					newBook.addResource(resource);
				}else if(resource.getMediaType()==MediatypeService.JPG || resource.getMediaType()==MediatypeService.GIF){
					newBook.addResource(resource);
				}else if(resource.getMediaType()==MediatypeService.NCX){
					processNcxDoc(resource.getInputStream());
				}
				
			}
			
			 Map<String, Resource> sortedMap = new TreeMap<String, Resource>();
			 sortedMap.putAll(resourceMap);
			 for(String resourceKey: sortedMap.keySet()){
				 JsoupReader(sortedMap.get(resourceKey).getInputStream(), newBook, tocList);
			 }
			EpubWriter epubWriter = new EpubWriter();
			epubWriter.write(newBook, new FileOutputStream("src/resources/New_Book.epub"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void processNcxDoc(InputStream resource) throws IOException{
		Document doc = Jsoup.parse(resource, "UTF-8","");
		Elements navPoints = doc.select("navPoint");
		for(Element navPoint: navPoints){
			Elements contents = navPoint.getElementsByTag("content");
			for(Element content: contents){
				String src =  content.attr("src");
				String[] splits =  src.split("#");
				tocSet.add(splits[1]);	
			}
		}
		tocList.addAll(tocSet);
	} 
	
    public static void main( String[] args )
    {
    	App app = new App();
    	app.ePubReaderAndFormatter();
    	//JsoupReader();
    }
}
