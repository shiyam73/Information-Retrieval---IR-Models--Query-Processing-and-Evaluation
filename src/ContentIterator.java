import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;


public class ContentIterator implements Iterator<String> {

	protected BufferedReader rdr;
	protected boolean at_eof = false;
	
	//private static MaxentTagger mtagger = null; 
	// The tagged string

	
	public ContentIterator(File file) throws FileNotFoundException {
		try{
			//mtagger = new MaxentTagger("taggers/left3words-wsj-0-18.tagger");
		}catch(Exception e){
			e.printStackTrace();
		}

		rdr = new BufferedReader(new FileReader(file));
		//System.out.println("Reading " + file.toString());
	}
	
	@Override
	public boolean hasNext() {
		return !at_eof;
	}

	@Override
	public String next() {
		Document doc = new Document();
		StringBuffer sb = new StringBuffer();
		StringBuffer contentsb = new StringBuffer();
		boolean tagmatches = false;
		try {
			String line;
			Pattern docno_tag = Pattern.compile("<num> Number: ([\\d]*)");
			Pattern titleTag = Pattern.compile("<title> (.*)");
			Pattern narrativeTag = Pattern.compile("<narr> Narrative:");
			Pattern descTag = Pattern.compile("<desc> Description:");

			boolean in_doc = false;
			while (true) {
				line = rdr.readLine();
				if (line == null) {
					at_eof = true;
					break;
				}
				if (!in_doc) {
					if (line.startsWith("<top>"))
						in_doc = true;
					else
						continue;
				}
				if (line.startsWith("</top>")) {
					in_doc = false;
					//sb.append(line);
					break;
				}
				
				Matcher m = docno_tag.matcher(line);
				if (m.find()) {
					tagmatches = true;
					String docno = m.group(1);
					//System.out.println(docno);

					FieldType type = new FieldType();
					type.setIndexed(true);
					type.setStored(true);
					type.setStoreTermVectors(true);

					doc.add(new Field("docno", docno, type));
					//System.out.println("Doc::"+docno);
					sb = new StringBuffer();
				}

				m = titleTag.matcher(line);
				if (m.find()) {
				//	//System.out.println("Title tag matches");
					tagmatches = true;
					String title = m.group(1);
				//	//System.out.println(title);
					sb.append(title);
				}

				
				m = descTag.matcher(line);
				if(m.find()){
					tagmatches = true;
					FieldType type = new FieldType();
					type.setIndexed(true);
					type.setStored(true);
					type.setStoreTermVectors(true);
					
					//String titleParsed = mtagger.tagString(sb.toString().trim());
				//	contentsb.append(titleParsed);
					doc.add(new Field("title", sb.toString(), type));
					
				//	//System.out.println("Title::"+sb.toString());
					//sb = new StringBuffer();
				}
				
				m = narrativeTag.matcher(line);
				if(m.find()){
					tagmatches = true;
					FieldType type = new FieldType();
					type.setIndexed(true);
					type.setStored(true);
					type.setStoreTermVectors(true);

					//String descParsed = mtagger.tagString(sb.toString().trim());
				//	contentsb.append(descParsed);
					doc.add(new Field("desc", sb.toString().trim(), type));
				//	//System.out.println("Desc::" +sb.toString());
					//sb = new StringBuffer();
					
				}
				
				if(!tagmatches){
					sb.append(line.trim()+" ");
				}else{
					////System.out.println("tagmatches"+line.trim());
					tagmatches = false;
				}
			}
			if (sb.length() > 0){
				////System.out.println("Contents :: "+sb.toString());
				FieldType type = new FieldType();
				type.setIndexed(true);
				type.setStored(true);
				type.setStoreTermVectors(true);

			//	String narrParsed = mtagger.tagString(sb.toString().trim());
			//	contentsb.append(narrParsed);
				
				//doc.add(new Field("narr", sb.toString().trim(),type));
				
				return sb.toString();
			//	String text = doc.get("desc");
			//	sb.append(text);
			//	doc.add(new Field("contents", sb.toString().trim(),type));
				
			//	//System.out.println("Narrative::"+sb.toString());
			}
			
		} catch (IOException e) {
			doc = null;
		}
		return sb.toString();
		//return doc;
	}

	@Override
	public void remove() {
		// Do nothing, but don't complain
	}


}
