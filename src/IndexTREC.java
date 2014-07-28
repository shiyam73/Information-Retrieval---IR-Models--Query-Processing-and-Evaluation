import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.xml.builders.TermsFilterBuilder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class IndexTREC {

	private static MaxentTagger mtagger = null;

	private IndexTREC() {
	}

	public static void main(String[] args) {
		String usage = "java org.apache.lucene.demo.IndexFiles"
				+ " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
				+ "This indexes the documents in DOCS_PATH, creating a Lucene index"
				+ "in INDEX_PATH that can be searched with SearchFiles";
		String indexPath = "index";
		String docsPath = null;
		boolean create = true;
		for (int i = 0; i < args.length; i++) {
			if ("-index".equals(args[i])) {
				indexPath = args[i + 1];
				i++;
			} else if ("-docs".equals(args[i])) {
				docsPath = args[i + 1];
				i++;
			} else if ("-update".equals(args[i])) {
				create = false;
			}
		}

		if (docsPath == null) {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}

		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()) {
			System.out
					.println("Document directory '"
							+ docDir.getAbsolutePath()
							+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}

		try {
			mtagger = new MaxentTagger("taggers/left3words-wsj-0-18.tagger");
		} catch (Exception e) {
			e.printStackTrace();
		}

		Date start = new Date();
		try {
			System.out.println("Indexing to directory '" + indexPath + "'...");

			Directory dir = FSDirectory.open(new File(indexPath));
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_41);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_41,
					analyzer);

			if (create) {
				// Create a new index in the directory, removing any
				// previously indexed documents:
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				// Add new documents to an existing index:
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}

			// Optional: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer. But if you do this, increase the max heap
			// size to the JVM (eg add -Xmx512m or -Xmx1g):
			//
			iwc.setRAMBufferSizeMB(256.0);

			IndexWriter writer = new IndexWriter(dir, iwc);
			indexDocs(writer, docDir);

			// NOTE: if you want to maximize search performance,
			// you can optionally call forceMerge here. This can be
			// a terribly costly operation, so generally it's only
			// worth it when your index is relatively static (ie
			// you're done adding documents to it):
			//
			// writer.forceMerge(1);

			writer.close();

			System.out.println("Indexing completed .. ");

			IndexReader reader = DirectoryReader.open(FSDirectory
					.open(new File(indexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);

			/*
			 * QueryParser parser = new QueryParser(Version.LUCENE_41,
			 * "contents", analyzer);
			 * 
			 * try { Query q = parser.parse("document");
			 * System.out.println("Freq ::"+reader.docFreq(new Term("contents",
			 * "document"))); TopDocs resTopDocs = searcher.search(q, 10);
			 * System.out.println("Test TopDocs"); } catch (ParseException e) {
			 * // TODO Auto-generated catch block e.printStackTrace(); }
			 */
			Fields fields = MultiFields.getFields(reader);

			File f = new File("Freq.csv");
			FileWriter fis = new FileWriter(f);
			BufferedWriter bis = new BufferedWriter(fis);

			for (String field : fields) {
				Terms terms = fields.terms(field);
				TermsEnum termsEnum = terms.iterator(null);
				BytesRef text;
				while ((text = termsEnum.next()) != null) {
					int r = reader.docFreq(new Term(field, text));
					bis.write(text.utf8ToString() + "," + r + "," + field
							+ "\n");
					// System.out.println("field=" + field + "; text=" +
					// text.utf8ToString() + "; freqCount= " + r);
				}
				bis.flush();
			}
			// reader.getTermVectors(docId);

			ContentIterator it = new ContentIterator(docDir);

			double logN = Math.log(reader.maxDoc());
			for (int i = 0; i < reader.maxDoc(); i++) {

				Document doc = reader.document(i);
				String docId = doc.get("docno");

				StringBuilder sb = new StringBuilder();
				Terms topTerm = reader.getTermVector(
						Integer.parseInt(docId) - 301, "title");
				Terms term = reader.getTermVector(
						Integer.parseInt(docId) - 301, "narr");
				Terms term1 = reader.getTermVector(
						Integer.parseInt(docId) - 301, "desc");

				String content = it.next();
				// System.out.println(content);

				Map<String, Double> queryString = new HashMap<String, Double>();

				if (term != null) {
					// System.out.println("Term is not null");
					// System.out.println("Terms size is "+term.size());

					TermsEnum termEnum = topTerm.iterator(null);
					BytesRef text;
					while ((text = termEnum.next()) != null) {
						// System.out.println("text=" + text.utf8ToString() +
						// "; freqCount= " + r + "; freqCountcontent = " + r1
						// +"; freqCountdesc= " + r2 );
						// sb.append(text.utf8ToString()+ "^5 ");
						String key = text.utf8ToString();
						double freq = (double) termEnum.totalTermFreq();
						// System.out.println(key+"::Val"+termEnum.totalTermFreq());
						Double val = (Double) (5 * (1 + Math.log(freq)));
						queryString.put(key, val);
					}

					termEnum = term.iterator(null);

					while ((text = termEnum.next()) != null) {
						int r = reader.docFreq(new Term("narr", text));
						int r1 = reader.docFreq(new Term("contents", text));
						int r2 = reader.docFreq(new Term("desc", text));
						if (r <= 2) {
							// System.out.println("text=" + text.utf8ToString()
							// + "; freqCount= " + r + "; freqCountcontent = " +
							// r1 +"; freqCountdesc= " + r2 );
							// sb.append(text.utf8ToString()+ "^3 ");
							String key = text.utf8ToString();
							// System.out.println(key+"::Val"+termEnum.totalTermFreq());
							double freq = (double) termEnum.totalTermFreq();
							// System.out.println(key+"::Val"+termEnum.totalTermFreq());
							Double val = (Double) (3 * (1 + Math.log(freq)));
							// queryString.put(key, val);
							if (queryString.containsKey(key)) {
								Double prevVal = queryString.get(key);
								queryString.put(key, prevVal + val);
							} else {
								queryString.put(key, val);
							}
						}
					}
					termEnum = term1.iterator(null);
					while ((text = termEnum.next()) != null) {
						int r = reader.docFreq(new Term("narr", text));
						int r1 = reader.docFreq(new Term("contents", text));
						int r2 = reader.docFreq(new Term("desc", text));
						//System.out.println(r1 + " " + r2);
						if (r <= 2) {
							// System.out.println("text=" + text.utf8ToString()
							// + "; freqCount= " + r + "; freqCountcontent = " +
							// r1 +"; freqCountdesc= " + r2 );
							// sb.append(text.utf8ToString()+ "^2 ");
							String key = text.utf8ToString();
							// System.out.println(key+"::Val"+termEnum.totalTermFreq());
							double freq = (double) termEnum.totalTermFreq();
							// System.out.println(key+"::Val"+termEnum.totalTermFreq());
							Double val = (Double) (2 * (1 + Math.log(freq)));
							// queryString.put(key, val);
							if (queryString.containsKey(key)) {
								Double prevVal = queryString.get(key);
								queryString.put(key, prevVal + val);
							} else {
								queryString.put(key, val);
							}
						}
					}

					String contentParsed = mtagger.tagString(content);
					HashMap<String, Integer> occurence = new HashMap<String, Integer>();
					String[] split = contentParsed.split(" ");
					//StringBuilder contentSb = new StringBuilder();
					for (int j = 0; j < split.length; j++) {
					//	System.out.print(split[j]+" ");
						if (!(split[j].endsWith("NN")
								|| split[j].endsWith("NNS")
								|| split[j].endsWith("NNP")
								|| split[j].endsWith("NNPS")
								|| split[j].endsWith("VB")
								|| split[j].endsWith("VBD")
								|| split[j].endsWith("VBZ")
								|| split[j].endsWith("VBP")
								|| split[j].endsWith("VBG")
								|| split[j].endsWith("VBP")
								|| split[j].endsWith("VBN") || split[j]
									.endsWith("CD"))) {
				//				System.out.print("1 ");
						} else {
				//			System.out.print("2 ");
							String key = split[j].substring(0,
									split[j].indexOf("/")).toLowerCase();
							// System.out.println(key);
							// contentSb.append(split[j]+" ");
							if (!occurence.containsKey(key)) {
								occurence.put(key, 1);
							} else {
								int value = occurence.get(key);
								occurence.put(key, ++value);
							}
							// System.out.println(key+"::Val"+termEnum.totalTermFreq());
						}
					}
			//		System.out.println();
					Iterator<String> nlpIterator = occurence.keySet()
							.iterator();
					while (nlpIterator.hasNext()) {
						String key = nlpIterator.next();
						int r1 = reader.docFreq(new Term("contents", key));
						//System.out.println("r1::"+r1);
						double logr1 = logN;
						if(r1 != 0){
							logr1 = Math.log(r1);
						}
						//if (r1 <= 10) {
						//	System.out.print(key+" ");
							Integer freq = occurence.get(key);
							//System.out.println(freq);	
							//System.out.println(logN);
							//System.out.println(logr1);
							Double val = (Double) ((logN- logr1) * (1 + Math.log(freq)));
							//System.out.println(val);
							// queryString.put(key, val);
							if (queryString.containsKey(key)) {
								Double prevVal = queryString.get(key);
								queryString.put(key, prevVal * val);
							} else {
								queryString.put(key, val);
							}
						//}
					}

					//System.out.println();
					Iterator<String> queryIterator = queryString.keySet()
							.iterator();
					while (queryIterator.hasNext()) {
						String key = queryIterator.next();
						Double weight = queryString.get(key);

						sb.append(key + "^" + weight + " ");
					}

					// System.out.println(docId+" "+contentSb.toString());

					System.out.println(docId + " " + sb.toString().trim());
					// System.out.println("Terms are "+term.toString());
					// term.iterator(new TermsEnum());
				} else {
					System.out.println("Term is null");
				}

				// System.out.println(docId);
				// System.out.print(doc.toString());

				// do something with docId here...
			}

			System.out.println("Reader accessing index path");

			/*
			 * docs = new TrecDocIterator(docDir);
			 * 
			 * while(docs.hasNext()){ //System.out.println("doc. has Next");
			 * Document doc = docs.next();
			 * //System.out.println("Document number:: " +doc.get("docno")); if
			 * (doc != null && doc.get("docno") != null) { Terms tfv =
			 * reader.getTermVector
			 * (Integer.parseInt(doc.get("docno"))-301,"contents"); //Terms tfv
			 * =
			 * reader.getTermFreqVectors(Integer.parseInt(doc.get("docno"))-301
			 * ); System.out.println(tfv.toString()); }
			 * //System.out.println("Contents :: " + doc.get("contents")); }
			 */

			Date end = new Date();
			System.out.println(end.getTime() - start.getTime()
					+ " total milliseconds");

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass()
					+ "\n with message: " + e.getMessage());
		}
	}

	/**
	 * Indexes the given file using the given writer, or if a directory is
	 * given, recurses over files and directories found under the given
	 * directory.
	 * 
	 * NOTE: This method indexes one document per input file. This is slow. For
	 * good throughput, put multiple documents into your input file(s). An
	 * example of this is in the benchmark module, which can create "line doc"
	 * files, one document per line, using the <a href=
	 * "../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
	 * >WriteLineDocTask</a>.
	 * 
	 * @param writer
	 *            Writer to the index where the given file/dir info will be
	 *            stored
	 * @param file
	 *            The file to index, or the directory to recurse into to find
	 *            files to index
	 * @throws IOException
	 *             If there is a low-level I/O error
	 */
	static void indexDocs(IndexWriter writer, File file) throws IOException {
		// do not try to index files that cannot be read
		if (file.canRead()) {
			if (file.isDirectory()) {
				String[] files = file.list();
				// an IO error could occur
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						indexDocs(writer, new File(file, files[i]));
					}
				}
			} else {
				TrecDocIterator docs = new TrecDocIterator(file);
				Document doc;
				while (docs.hasNext()) {
					doc = docs.next();
					if (doc != null && doc.getField("title") != null)
						writer.addDocument(doc);
				}
			}
		}
	}
}
