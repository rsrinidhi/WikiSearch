import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * Created by srinidhi on 10/31/15.
 */
public class Lucene {

    static CharArraySet stop_word_set;
    static ArrayList<String> Labels = new ArrayList<String>();

    Lucene(){
        stop_word_set = new CharArraySet(Version.LUCENE_40,1000,true);

        try {
            FileReader fr = new FileReader("resources/stop_word_set.txt");
            System.out.println("Reading Stop Words");
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null) {
                stop_word_set.add(line);

            }

            System.out.println("Finished reading Stop Words");
            br.close();
            fr.close();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    public static void labels() throws IOException {
            FileReader fr = new FileReader("resources/Labels.txt");
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null) {

                StringBuilder sb = new StringBuilder();
                TokenStream tokenStream = new StandardTokenizer(Version.LUCENE_40, new StringReader(line));
                tokenStream = new StopFilter(Version.LUCENE_40, tokenStream, stop_word_set);
                CharTermAttribute charTermAttr = tokenStream.getAttribute(CharTermAttribute.class);

                while (tokenStream.incrementToken()) {
                    sb.append(charTermAttr.toString() + " ");
                }

                Labels.add(sb.toString());
            }

        br.close();
        fr.close();
    }

    public static String PorterStemmer(String input){
        System.out.println("Stemming documents");
        StringBuilder sb = new StringBuilder();

        TokenStream tokenStream = new StandardTokenizer(Version.LUCENE_40, new StringReader(input));
        tokenStream = new StopFilter(Version.LUCENE_40, tokenStream, stop_word_set);
        tokenStream = new PorterStemFilter(tokenStream);

        CharTermAttribute charTermAttr = tokenStream.getAttribute(CharTermAttribute.class);

        try {
            while (tokenStream.incrementToken()) {
                sb.append(charTermAttr.toString() + " ");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Finished stemming document");
        return sb.toString();
    }

    public static String Lemma(String input){
        System.out.println("Lemmatizing documents");

        StringBuilder sb = new StringBuilder();

        TokenStream tokenStream = new StandardTokenizer(Version.LUCENE_40, new StringReader(input));
        tokenStream = new StopFilter(Version.LUCENE_40, tokenStream, stop_word_set);

        CharTermAttribute charTermAttr = tokenStream.getAttribute(CharTermAttribute.class);

        try {
            while (tokenStream.incrementToken()) {
                sb.append(charTermAttr.toString() + " ");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        input = sb.toString();

        PrintStream err = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        }));

        Properties props = new Properties();
        props.put("annotators", "tokenize,ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation document = new Annotation(input);
        pipeline.annotate(document);

        System.setErr(err);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        List<String> lemmas = new LinkedList<String>();

        for(CoreMap sentence: sentences) {
            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                lemmas.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }

        for(int i = 0;i < lemmas.size(); i++)
            sb.append(lemmas.get(i) + " ");

        System.out.println("Finished lemmatizing document");
        return sb.toString();
    }

    public static String classify(String content, String document){
        String label = "OTHERS";
        int count  = 0, max = 0;

        for(String string : Labels){
            String[] tokens = string.split(" ");

            for(String token : tokens){
                if(content.contains(token))
                    count++;
            }

            if(count > max) {
                max = count;
                label = string;
            }
        }

        if(count == 0){
            for(String string : Labels){
                String[] tokens = string.split(" ");

                for(String token : tokens){
                    if(document.contains(token))
                        count++;
                }

                if(count > max) {
                    max = count;
                    label = string;
                }
            }
        }

        return label;
    }

    public static void readFile(String filename,Directory index,IndexWriterConfig config, String normalize) throws IOException {

        FileReader fr = new FileReader(filename);
        BufferedReader br = new BufferedReader(fr);
        IndexWriter w = new IndexWriter(index, config);
        String line, content = "", document = "",label = "";
        StringBuilder sb = new StringBuilder();

        while ((line = br.readLine()) != null) {

                if (line.startsWith("[[") && !line.contains("|") && !line.contains("</ref>")) {
                    if (content != "" && document != "") {

                        if (normalize.equalsIgnoreCase("Lemma"))
                            content = Lemma(content);
                        else if (normalize.equalsIgnoreCase("Stem"))
                            content = PorterStemmer(content);

                        label = classify(content, document);
                        addDoc(w, content, document, label);
                    }

                    sb = new StringBuilder();
                    line = line.replace("[","").replace("]","");
                    document = line;
                } else {

                    line = line.replaceAll("(\\[tpl\\](.*?)\\[\\/tpl\\])","");
                    line = line.replaceAll("\\[\\/ref]","");
                    line = line.replaceAll("\\[ref\\]","");
                    line = line.replaceAll("\\<\\/ref\\>","");
                    line = line.replaceAll("\\<ref name=(.*?)\\/\\>","");
                    line = line.replaceAll("\\<ref\\>","");
                    line = line.replaceAll("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "");

                    if (!line.startsWith("==") && !line.startsWith("#")
                        && (line.startsWith("+") == false || line.startsWith("|") == false || line.startsWith("-") == false
                            || line.startsWith("@") == false || line.startsWith("}") == false || line.startsWith("(") == false ))
                        content = sb.append(line).toString();
                }
            }

        w.close();
        br.close();

        System.out.println("Finished Document processing");
    }

    private static void addDoc(IndexWriter w, String text, String title, String label) throws IOException
    {
        Document doc = new Document();

        doc.add(new TextField("text", text, Field.Store.YES));
        doc.add(new StringField("title", title, Field.Store.YES));
        doc.add(new StringField("label", label, Field.Store.YES));
        w.addDocument(doc);
    }

    private static void Index(String FilePath, Directory index,IndexWriterConfig config, String normalize) throws IOException {
        Files.walk(Paths.get(FilePath)).forEach(filePath -> {
            if (Files.isRegularFile(filePath)) {
                System.out.println(filePath.toString());

                try {
                    readFile(filePath.toString(), index, config, normalize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) throws IOException {
        String FilePath,label="", query="", normalize;

        if(args.length == 2) {

            FilePath = args[0];
            normalize = args[1];

            if(normalize.equalsIgnoreCase("Stem")){
                Lucene obj = new Lucene();
                File indexDir = new File("src/IndexStem");
                StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
                Directory index = FSDirectory.open((indexDir));
                IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
                Index(FilePath, index, config,normalize);

            }else if(normalize.equalsIgnoreCase("Lemma")){
                Lucene obj = new Lucene();
                File indexDir = new File("src/IndexLemma");
                StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
                Directory index = FSDirectory.open((indexDir));
                IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
                Index(FilePath, index, config,normalize);

            }else{
                Lucene obj = new Lucene();
                File indexDir = new File("src/Index");
                StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
                Directory index = FSDirectory.open((indexDir));
                IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
                Index(FilePath, index, config,normalize);

            }

            return;

        }else {
            normalize = args[0];

            Lucene obj = new Lucene();
            File indexDir = new File("src/Index");
            StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
            Directory index = FSDirectory.open((indexDir));
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);

            if(normalize.equalsIgnoreCase("Stem")){
                indexDir = new File("src/IndexStem");
                analyzer = new StandardAnalyzer(Version.LUCENE_40);
                index = FSDirectory.open((indexDir));
                config = new IndexWriterConfig(Version.LUCENE_40, analyzer);

            }else if(normalize.equalsIgnoreCase("Lemma")){
                indexDir = new File("src/IndexLemma");
                analyzer = new StandardAnalyzer(Version.LUCENE_40);
                index = FSDirectory.open((indexDir));
                config = new IndexWriterConfig(Version.LUCENE_40, analyzer);

            }

            FileReader fr = new FileReader("resources/questions.txt");
            BufferedReader br = new BufferedReader(fr);
            String line;
            int lineNum = 0;

            while ((line = br.readLine()) != null) {

                if(lineNum % 4 == 0) {
                    label = line;
                    if(normalize.equalsIgnoreCase("Stem")) {
                        label = PorterStemmer(label);

                    }else if(normalize.equalsIgnoreCase("Lemma")){
                        label = Lemma(label);
                    }
                }

                if(lineNum % 4 == 1) {
                    query = line;

                    if (normalize.equalsIgnoreCase("Stem")) {
                        query = PorterStemmer(query);

                    } else if (normalize.equalsIgnoreCase("Lemma")) {
                        query = Lemma(query);
                    }


                    labels();

                    String querystr = "label:" + label + " text:" + query;
                    System.out.println(querystr);

                try {
                    Query q = new QueryParser(Version.LUCENE_40, "text", analyzer).parse(querystr);

                    int hitsPerPage = 20;
                    IndexReader reader = IndexReader.open(index);
                    IndexSearcher searcher = new IndexSearcher(reader);
                    searcher.setSimilarity(new BM25Similarity());
                    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
                    searcher.search(q, collector);
                    ScoreDoc[] hits = collector.topDocs().scoreDocs;

                    System.out.println("Found " + hits.length + " hits.");
                    for (int i = 0; i < hits.length; ++i) {
                        int docId = hits[i].doc;
                        Document d = searcher.doc(docId);
                        System.out.println((i + 1) + ". " + d.get("title"));
                    }
                }catch (Exception e) {
                    System.out.println(e.getMessage());
                }

                    System.out.println();
                }
                lineNum++;
            }

            br.close();
            fr.close();
        }
    }
}