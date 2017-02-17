/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stemmerprueba;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;
import java.util.Calendar;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import com.healthmarketscience.jackcess.Index;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.DrillSideways.DrillSidewaysResult;
import org.apache.lucene.facet.DrillSideways;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/**
 *
 * @author rodrigo
 */
public class LuceneSearcher {

    private File fichero_header;
    private Directory directory_header;

    private File fichero;
    private Directory directory;

    private final FacetsConfig facetconfig;
    private Query text_query;

    private static String guardado_header;
    private static String guardado_comment;

    public LuceneSearcher() throws IOException {
               
        fichero = new File("unknown");
        //directory = FSDirectory.open(Paths.get(fichero.getCanonicalPath()));

        fichero_header = new File("unknown");
        //directory_header = FSDirectory.open(Paths.get(fichero_header.getCanonicalPath()));

        facetconfig = new FacetsConfig();
        facetconfig.setIndexFieldName("service_display", "service_facet");
        facetconfig.setIndexFieldName("location_display", "location_facet");
        facetconfig.setIndexFieldName("cleanliness_display", "cleanliness_facet");

    }
    
    public void setPathComment(String path) throws IOException{
        fichero = new File(path);
        directory = FSDirectory.open(Paths.get(fichero.getCanonicalPath()));
        System.out.println("Set comment: " + fichero.getCanonicalPath());
    }
    
    public void setPathHeader(String path) throws IOException{
        fichero_header = new File(path);
        directory_header = FSDirectory.open(Paths.get(fichero_header.getCanonicalPath()));
        System.out.println("Set header: " + fichero_header.getCanonicalPath());
    }
    
    public String getIndexPath() throws IOException{
        //System.out.println("Solicitado path de indice: " + fichero.getCanonicalPath());
        return fichero.getCanonicalPath();
    }
    public String getIndexHeaderPath() throws IOException{
        //System.out.println("Solicitado path de indice: " + fichero_header.getCanonicalPath());
        return fichero_header.getCanonicalPath();
    }

    //BÚSQUEDA POR TEXTO:
    public String buscarDocumentosTerminos(String texto) throws IOException, SAXException, TikaException, ParseException, org.apache.lucene.queryparser.classic.ParseException {

        String queries = null;
        IndexReader ireader = DirectoryReader.open(directory);
        IndexSearcher isearcher = new IndexSearcher(ireader);

        IndexReader ireader2 = DirectoryReader.open(directory_header);
        IndexSearcher isearcher2 = new IndexSearcher(ireader2);

        DirectoryReader indexReader = DirectoryReader.open(directory);
        DirectoryReader headerReader = DirectoryReader.open(directory_header);
        IndexSearcher header_searcher = new IndexSearcher(headerReader);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        // Parse a simple query that searches for "text":
        Analyzer analyzer = new StandardAnalyzer();
        BufferedReader in = null;
        if (queries != null) {
            in = Files.newBufferedReader(Paths.get(queries), StandardCharsets.UTF_8);
        } else {
            in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        }

        QueryParser parser = new QueryParser("comment", analyzer);
        TermQuery query = new TermQuery(new Term("comment", texto)); //TEXTO A BUSCAR
        text_query = query; // guardo consulta para filtrados posteriores
        ScoreDoc[] hits = isearcher.search(query, 20).scoreDocs;

        String string_resultado = "";

        // Iterate through the results:
        ArrayList<Document> res_doc = new ArrayList<>();
        System.out.println("Antes de hitDoc comment path:" + getIndexPath() + "\nheader path" + getIndexHeaderPath());
        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = isearcher.doc(hits[i].doc);

            //Analyzer analyzer = new StandardAnalyzer();
            QueryParser parser2 = new QueryParser("hotel", analyzer);
            // System.out.println("id display: " + hitDoc.get("id_display"));
            query = new TermQuery(new Term("id_display", hitDoc.get("id_display"))); //TEXTO A BUSCAE
            ScoreDoc[] hotel_hits = header_searcher.search(query, 1).scoreDocs;
            //  System.out.println("relustados en index header: " + hotel_hits.length);
            if (hotel_hits.length > 0) {
                Document hitDoc_Hotel = header_searcher.doc(hotel_hits[0].doc);
                //System.out.println("hotel: " + hitDoc_Hotel.get("hotel") + " (id:" + hitDoc_Hotel.get("id_display") + ")");

                string_resultado += (i+1) + ".- hotel: " + hitDoc_Hotel.get("hotel") +" (Score: "+hits[i].score+")"+ "\n" + hitDoc.get("comment") + "\n" + "Location Rating: " + hitDoc.get("location_display")
                        + "\nRating: " + hitDoc.get("rating_display") + "\n" + "Fecha: " + hitDoc.get("date_display") + "\n\n";
            }

            int j = 0;

            res_doc.add(hitDoc);

            String id = hitDoc.get("id_display");
            // System.out.println(id);

            query = new TermQuery(new Term("id_display", id));
            ScoreDoc[] hit = isearcher2.search(query, 20).scoreDocs;
            Document hitDoc2 = isearcher2.doc(hit[j].doc);
        }
        //obtención de facetas para esta consulta

        indexReader.close();
        headerReader.close();

        ireader.close();
        ireader2.close();
        return string_resultado;
        //return res_doc;
    }

    public String buscarDocumentosProximidad(String texto) throws IOException, SAXException, TikaException, ParseException, org.apache.lucene.queryparser.classic.ParseException {

        String queries = null;
        IndexReader ireader = DirectoryReader.open(directory);
        IndexSearcher isearcher = new IndexSearcher(ireader);

        IndexReader ireader2 = DirectoryReader.open(directory_header);
        IndexSearcher isearcher2 = new IndexSearcher(ireader2);

        DirectoryReader indexReader = DirectoryReader.open(directory);
        DirectoryReader headerReader = DirectoryReader.open(directory_header);
        IndexSearcher header_searcher = new IndexSearcher(headerReader);
        IndexSearcher searcher = new IndexSearcher(indexReader);

// Parse a simple query that searches for "text":
        Analyzer analyzer = new StandardAnalyzer();
        BufferedReader in = null;
        if (queries != null) {
            in = Files.newBufferedReader(Paths.get(queries), StandardCharsets.UTF_8);
        } else {
            in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        }

        QueryParser parser = new QueryParser("comment", analyzer);
        Query query = parser.parse(texto); //TEXTO A BUSCAE
        text_query = query; // guardo consulta para filtrados posteriores

        ScoreDoc[] hits = isearcher.search(query, 20).scoreDocs;
        String string_resultado = "";
        ArrayList<Document> res_doc = new ArrayList<>();
// Iterate through the results:
        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = isearcher.doc(hits[i].doc);
            QueryParser parser2 = new QueryParser("hotel", analyzer);
            //System.out.println("id display: " + hitDoc.get("id_display"));
            query = new TermQuery(new Term("id_display", hitDoc.get("id_display"))); //TEXTO A BUSCAE
            ScoreDoc[] hotel_hits = header_searcher.search(query, 1).scoreDocs;
            //System.out.println("relustados en index header: " + hotel_hits.length);
            if (hotel_hits.length > 0) {
                Document hitDoc_Hotel = header_searcher.doc(hotel_hits[0].doc);
                //System.out.println("hotel: " + hitDoc_Hotel.get("hotel") + " (id:" + hitDoc_Hotel.get("id_display") + ")");
                string_resultado += (i+1) + ".- hotel: " + hitDoc_Hotel.get("hotel") + "\n" + hitDoc.get("comment") + "\n" + "Location Rating: " + hitDoc.get("location_display")
                        + "\nRating: " + hitDoc.get("rating_display") + "\n" + "Fecha: " + hitDoc.get("date_display") + "\n\n";
            }

        }

        ireader.close();
        ireader2.close();

        return string_resultado;
    }

    //FIN BUSQUEDA POR TEXTO.
    
    //facets searh an drill Down 
    public List<FacetResult> search_facets() throws IOException, org.apache.lucene.queryparser.classic.ParseException {

        DirectoryReader indexReader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(indexReader, "location_facet");

        // Aggregates the facet counts
        FacetsCollector fc = new FacetsCollector();

        // new MatchAllDocsQuery() is for "browsing" (counts facets for all non-deleted docs in the index); normally
        //Query q = new TermQuery(new Term("comment", "excellent"));
        FacetsCollector.search(searcher, GetTextQuery(), 20, fc); //GetTextQuery toma la consulta hecha con Buscar

        // Retrieve results
        Facets facets = new SortedSetDocValuesFacetCounts(state, fc);

        List<FacetResult> results = new ArrayList<>();
        results.add(facets.getTopChildren(20, "location_display"));

        indexReader.close();

        return results;
    }

    public String drillDown(String rate) throws IOException {
        DirectoryReader indexReader = DirectoryReader.open(directory);
        DirectoryReader headerReader = DirectoryReader.open(directory_header);
        IndexSearcher header_searcher = new IndexSearcher(headerReader);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        ArrayList<Document> DocumentosFiltrados = new ArrayList<>();
        String string_resultado = "";
        SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(indexReader, "location_facet");
        FacetsCollector fc = new FacetsCollector();

        // Retrieve results
        Facets facets = new SortedSetDocValuesFacetCounts(state, fc);
        FacetResult result = facets.getTopChildren(20, "location_display");

        //Query sq = new TermQuery(new Term("comment", "excellent"));
        BooleanQuery.Builder finalQueryBuilder = new BooleanQuery.Builder();
        finalQueryBuilder.add(GetTextQuery(), Occur.MUST);

        DrillDownQuery dq = new DrillDownQuery(facetconfig);
        dq.add("location_display", rate);
        finalQueryBuilder.add(dq, Occur.FILTER);

        TopDocs resultDocs = FacetsCollector.search(searcher, finalQueryBuilder.build(), 20, fc);
        ScoreDoc[] hits = resultDocs.scoreDocs;

        int cont = 1;
        for (ScoreDoc sd : hits) {
            Document hitDoc = searcher.doc(sd.doc);
            DocumentosFiltrados.add(hitDoc);

            // Se lanza consulta a index header para obtener el nombre del hotel dado el id del documento de comentario
            Analyzer analyzer = new StandardAnalyzer();
            QueryParser parser = new QueryParser("hotel", analyzer);
            //System.out.println("id display: " + hitDoc.get("id_display"));
            TermQuery query = new TermQuery(new Term("id_display", hitDoc.get("id_display"))); //TEXTO A BUSCAE
            ScoreDoc[] hotel_hits = header_searcher.search(query, 1).scoreDocs;
            //System.out.println("relustados en index header: " + hotel_hits.length);
            if (hotel_hits.length > 0) {
                Document hitDoc_Hotel = header_searcher.doc(hotel_hits[0].doc);
                //System.out.println("hotel: " + hitDoc_Hotel.get("hotel") + " (id:" + hitDoc_Hotel.get("id_display") + ")");
                string_resultado += cont + ".- hotel: " + hitDoc_Hotel.get("hotel") + "\n" + hitDoc.get("comment") + "\n" + "Location Rating: " + hitDoc.get("location_display")
                        + "\nRating: " + hitDoc.get("rating_display") + "\n" + "Fecha: " + hitDoc.get("date_display") + "\n\n";
            }

            cont++;
        }

        indexReader.close();
        headerReader.close();

        return string_resultado;
    }

    public String filterByRank(float inicio, float fin) throws IOException {
        String resultado = "";
        if (inicio <= fin) {
            IndexReader ireader = DirectoryReader.open(directory);
            IndexSearcher isearcher = new IndexSearcher(ireader);
            IndexReader ireader_header = DirectoryReader.open(directory_header);
            IndexSearcher isearcher_header = new IndexSearcher(ireader_header);
            System.out.println("Rates insertados: "+inicio+" --- "+fin);
            Query query_rating = FloatPoint.newRangeQuery("rating", inicio, fin);

            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            booleanQuery.add(GetTextQuery(), BooleanClause.Occur.MUST);//fallo
            booleanQuery.add(query_rating, BooleanClause.Occur.FILTER);
            int hitsPerPage = 20;
            ArrayList<Document> DocumentosFiltrados = new ArrayList<>();
            TopDocsCollector collector = TopScoreDocCollector.create(hitsPerPage);//tambien se le puede pasar priority queue
            isearcher.search(booleanQuery.build(), collector);

            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            int cont = 0;
            for (ScoreDoc sd : hits) {
                Document hitDoc = isearcher.doc(sd.doc);
                DocumentosFiltrados.add(hitDoc);

                // Se lanza consulta a index header para obtener el nombre del hotel dado el id del documento de comentario
                Analyzer analyzer = new StandardAnalyzer();
                QueryParser parser = new QueryParser("hotel", analyzer);
                TermQuery queryTerm = new TermQuery(new Term("id_display", hitDoc.get("id_display")));

                //busqueda sobre documento
                ScoreDoc[] hotel_hits = isearcher_header.search(queryTerm, 1).scoreDocs;

                if (hotel_hits.length > 0) {
                    Document hitDoc_Hotel = isearcher_header.doc(hotel_hits[0].doc);
                    resultado += cont + ".- hotel: " + hitDoc_Hotel.get("hotel") + "\n" + hitDoc.get("comment") + "\n" + "Location Rating: " + hitDoc.get("location_display")
                        + "\nRating: " + hitDoc.get("rating_display") + "\n" + "Fecha: " + hitDoc.get("date_display") + "\n\n";

                }

                cont++;
            }

            ireader_header.close();
            ireader.close();
        } else {
        }
        return resultado;
    }

    public String dateQuery(String inicio_date, String fin_date) throws IOException, ParseException {

        String resultado = "";
        long ini = DateTools.stringToTime(inicio_date);
        long fin = DateTools.stringToTime(fin_date);

        if (ini <= fin) {
            IndexReader ireader = DirectoryReader.open(directory);
            IndexSearcher isearcher = new IndexSearcher(ireader);
            IndexReader ireader_header = DirectoryReader.open(directory_header);
            IndexSearcher isearcher_header = new IndexSearcher(ireader_header);

            Query query_date = LongPoint.newRangeQuery("date", ini, fin);

            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            booleanQuery.add(GetTextQuery(), BooleanClause.Occur.MUST);//fallo
            booleanQuery.add(query_date, BooleanClause.Occur.FILTER);
            int hitsPerPage = 20;
            ArrayList<Document> DocumentosFiltrados = new ArrayList<>();
            TopDocsCollector collector = TopScoreDocCollector.create(hitsPerPage);//tambien se le puede pasar priority queue
            isearcher.search(booleanQuery.build(), collector);

            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            //System.out.println("HIITS"+hits.length);

            int cont = 0;
            for (ScoreDoc sd : hits) {
                Document hitDoc = isearcher.doc(sd.doc);
                DocumentosFiltrados.add(hitDoc);

                // Se lanza consulta a index header para obtener el nombre del hotel dado el id del documento de comentario
                Analyzer analyzer = new StandardAnalyzer();
                QueryParser parser = new QueryParser("hotel", analyzer);
                TermQuery queryTerm = new TermQuery(new Term("id_display", hitDoc.get("id_display")));

                //busqueda sobre documento
                ScoreDoc[] hotel_hits = isearcher_header.search(queryTerm, 1).scoreDocs;
                //System.out.println("resultados en index header: " + hotel_hits.length);

                if (hotel_hits.length > 0) {
                    Document hitDoc_Hotel = isearcher_header.doc(hotel_hits[0].doc);
//                    System.out.println(cont + ".- Hotel: " +  hitDoc_Hotel.get("hotel") + "\n" + hitDoc.get("comment") +"\n" + "Rating_Location: "+hitDoc.get("location_display") 
//                          +  "\nRating::"+hitDoc_Hotel.get("rating_display")+ "\n\n");
                    resultado += cont + ".- hotel: " + hitDoc_Hotel.get("hotel") + "\n" + hitDoc.get("comment") + "\n" + "Location Rating: " + hitDoc.get("location_display")
                        + "\nRating: " + hitDoc.get("rating_display") + "\n" + "Fecha: " + hitDoc.get("date_display") + "\n\n";

                }

                cont++;
            }

            ireader_header.close();
            ireader.close();
        } else {
        }
        return resultado;
    }

    public String PriceOrder() throws IOException {
        DirectoryReader indexReader = DirectoryReader.open(directory);
        DirectoryReader headerReader = DirectoryReader.open(directory_header);
        IndexSearcher header_searcher = new IndexSearcher(headerReader);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        String string_resultado = "";
        ScoreDoc[] hits;
        Sort sort;

        sort = new Sort(new SortedNumericSortField("price_sorted", SortField.Type.FLOAT));
        TopDocs td = searcher.search(new MatchAllDocsQuery(), 20, sort);
        hits = searcher.search(GetTextQuery(), 20, sort).scoreDocs;

        int cont = 1;
        for (ScoreDoc sd : hits) {
            Document hitDoc = searcher.doc(sd.doc);

            // Se lanza consulta a index header para obtener el nombre del hotel dado el id del documento de comentario
            Analyzer analyzer = new StandardAnalyzer();
            QueryParser parser = new QueryParser("hotel", analyzer);
            //System.out.println("id display: " + hitDoc.get("id_display"));
            TermQuery query = new TermQuery(new Term("id_display", hitDoc.get("id_display"))); //TEXTO A BUSCAE
            ScoreDoc[] hotel_hits = header_searcher.search(query, 1).scoreDocs;
            //System.out.println("relustados en index header: " + hotel_hits.length);

            if (hotel_hits.length > 0) {
                Document hitDoc_Hotel = header_searcher.doc(hotel_hits[0].doc);
                string_resultado += cont + ".- hotel: " + hitDoc_Hotel.get("hotel") + "\n" + hitDoc.get("comment") + "\n" + "Location Rating: " + hitDoc.get("location_display")
                        + "\nRating: " + hitDoc.get("rating_display") + "\n" + "Fecha: " + hitDoc.get("date_display")+ "\nPrecio: " + hitDoc.get("price_display") + "\n\n";
            }

            cont++;
        }

        indexReader.close();
        headerReader.close();

        return string_resultado;
    }

    void rankQuery(String campo, float inicio, float fin) throws IOException {

        IndexReader ireader = DirectoryReader.open(directory_header);
        IndexSearcher isearcher = new IndexSearcher(ireader);
        Query query = FloatPoint.newRangeQuery(campo, inicio, fin); //TEXTO A BUSCAE 
        int hitsPerPage = 100;
        TopDocsCollector collector = TopScoreDocCollector.create(hitsPerPage);//tambien se le puede pasar priority queue
        isearcher.search(query, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;
        //System.out.println("length:: "+hits.length);

        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = isearcher.doc(hits[i].doc);
            //System.out.println("salida "+hitDoc.get(campo+"_display"));
            //System.out.println("salida "+hitDoc.toString());
        }
        ireader.close();
        directory.close();
    }

    Query GetTextQuery() {
        return text_query;
    }

    public void set_guardado(String guardado) {
        this.guardado_comment = guardado;
    }

    public String get_guardado() {
        return guardado_comment;
    }

    public void set_guardadoh(String guardado) {
        this.guardado_header = guardado;
    }

    public String get_guardadoh() {
        return guardado_header;
    }
}
