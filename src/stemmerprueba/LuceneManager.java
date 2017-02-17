/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stemmerprueba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.NumericUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author Antonio
 */
public class LuceneManager {

    private final File fichero_header;
    private final Analyzer analizer_header;
    private final Directory directory_header;
    private final IndexWriterConfig config_header;
    private final IndexWriter iwriter_header;

    private final File fichero;
    private final Analyzer analizer;
    private final Directory directory;
    private final IndexWriterConfig config;
    private final IndexWriter iwriter;

    //private final File fichero_facets = new File ("/home/rodrigo/Escritorio/index_facets");
    //private final Directory indexDir = FSDirectory.open(Paths.get(fichero_facets.getCanonicalPath()));
    private final FacetsConfig facetconfig;

    public static String guardado_header;
    public static String guardado_comment;
    //IndexWriter indexWriter = new IndexWriter(indexDir, new IndexWriterConfig(
    //        new WhitespaceAnalyzer()).setOpenMode(OpenMode.CREATE));

    public LuceneManager() throws IOException {
        String cabecera = selectData(1); //selector de fichero de palabras vacías
        String comentario = selectData(2); //selector de directorio donde está la colección de documentos a indexar
        set_guardadoh(cabecera);
        set_guardado(comentario);

        fichero = new File(comentario);
        analizer = new StandardAnalyzer(); //Directory directory = new RAMDirectory();
        directory = FSDirectory.open(Paths.get(fichero.getCanonicalPath()));
        config = new IndexWriterConfig(analizer);
        config.setOpenMode(OpenMode.CREATE); // para no sobrescribir
        iwriter = new IndexWriter(directory, config);

        fichero_header = new File(cabecera);
        analizer_header = new StandardAnalyzer(); //Directory directory = new RAMDirectory();
        directory_header = FSDirectory.open(Paths.get(fichero_header.getCanonicalPath()));
        config_header = new IndexWriterConfig(analizer_header);
        iwriter_header = new IndexWriter(directory_header, config_header);

        facetconfig = new FacetsConfig();
        facetconfig.setIndexFieldName("service_display", "service_facet");
        facetconfig.setIndexFieldName("location_display", "location_facet");
        facetconfig.setIndexFieldName("cleanliness_display", "cleanliness_facet");
    }

    /**
     * Método selector de directorios/ficheros con datos de entrada
     *
     * @param origen=>entero es el tipo de ruta solicitada 0 (palabras vacías) o
     * 1 (directorio de colecciones)
     *
     * @return String con la ruta seleccionada.
     */
    public static String selectData(int origen) throws IOException {
        String nombre = "";
        JFileChooser MI_ARCHIVO = new JFileChooser(System.getProperty("user.dir"));

        if (origen == 0) {
            MI_ARCHIVO.setDialogTitle("Seleccione directorio de donde tomar el DATASET");
            MI_ARCHIVO.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else if (origen == 1) {
            MI_ARCHIVO.setDialogTitle("Seleccione directorio de salida donde desea Guardar el indice de cabecera");
            MI_ARCHIVO.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else if (origen == 2) {
            MI_ARCHIVO.setDialogTitle("Seleccione directorio de salida donde desea Guardar el indice de comentario");
            MI_ARCHIVO.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }

        MI_ARCHIVO.showOpenDialog(MI_ARCHIVO);
        File SELECTOR_FICHEROS = MI_ARCHIVO.getSelectedFile();

        if (SELECTOR_FICHEROS != null) {
            nombre = MI_ARCHIVO.getSelectedFile().getPath();
        } else {
            System.out.println("HASTA PRONTO:");
            System.out.println("No se ha seleccionado la ubicación requerida");
            System.exit(1);
        }

        return nombre;
    }

    public void closeWriter() throws IOException {
        iwriter.close();
        iwriter_header.close();
    }

    void indexar(ArrayList<Document> documentos) throws Exception {
        for (Document doc : documentos) {
            iwriter.addDocument(doc);
        }
    }

    public void createIndex(File file, int id) throws IOException, SAXException, TikaException, ParseException {

        //parse document
        Parser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        FileInputStream inputstream = new FileInputStream(file);
        ParseContext context = new ParseContext();
        parser.parse(inputstream, handler, metadata, context);

        //System.out.println(file.getCanonicalPath());
        String[] list_Comment = StringUtils.substringsBetween(handler.toString(), "<Content>", "\r");
        String[] list_Date = StringUtils.substringsBetween(handler.toString(), "<Date>", "\r");
        String[] list_Location = StringUtils.substringsBetween(handler.toString(), "<Location>", "\r");
        String[] list_Cleanliness = StringUtils.substringsBetween(handler.toString(), "<Cleanliness>", "\r");
        String[] list_Service = StringUtils.substringsBetween(handler.toString(), "<Service>", "\r");

        /*
            System.out.println("comentarios: " + list_Comment.length);
            System.out.println("fechas: " + list_Date.length);
            System.out.println("locations: " + list_Location.length);
            System.out.println("limpieza: " + list_Cleanliness.length);
            System.out.println("servicio: " + list_Service.length);
         */
        String url = StringUtils.substringBetween(handler.toString(), "<URL>", "\n");
        String hotelName = "Desconocido";
        String hotelLocation = "Desconocido";

        if (url != null) {
            Pattern photel = Pattern.compile(".*-(.*?)-(.*?).html$");
            Matcher mhotel = photel.matcher(url);
            while (mhotel.find()) {
                hotelName = mhotel.group(1);
                hotelLocation = mhotel.group(2);
            }
        }

        String rating = StringUtils.substringBetween(handler.toString(), "<Overall Rating>", "\n");
        String price = StringUtils.substringBetween(handler.toString(), "<Avg. Price>", "\n");

        float value = repareStringToFloat(price);
        float rate = repareStringToFloat(rating);//resolver el tema de rating o price UNKNOWN
        
        if(value < 0){
            value = (float) 99999.00; // hacemos que si precio desconocido(-1) se muestren los últimos
        }
        
        //header fields     
        Document doc_cabecera = new Document();

        doc_cabecera.add(new StringField("hotel", hotelName, Field.Store.YES));//se indexa y no tokeniza

        doc_cabecera.add(new StringField("location", hotelLocation, Field.Store.YES));

        doc_cabecera.add(new SortedNumericDocValuesField("id", id));
        doc_cabecera.add(new StringField("id_display", Integer.toString(id), Field.Store.YES));

        iwriter_header.addDocument(doc_cabecera);

        //campos de comentatirios
        int contador;
        for (contador = 0; contador < list_Comment.length; contador++) {
            Document doc_comment = new Document();

            doc_comment.add(new StringField("id_display", Integer.toString(id), Field.Store.YES));
            doc_comment.add(new TextField("comment", list_Comment[contador], Field.Store.YES));
            doc_comment.add(new LongPoint("date", indexableDate(list_Date[contador])));
            doc_comment.add(new StringField("date_display", DateTools.timeToString(indexableDate(list_Date[contador]), DateTools.Resolution.DAY), Field.Store.YES));

            doc_comment.add(new IntPoint("location", Integer.parseInt(list_Location[contador])));
            doc_comment.add(new StringField("location_display", list_Location[contador], Field.Store.YES));
            doc_comment.add(new SortedSetDocValuesFacetField("location_display", list_Location[contador]));

            FloatPoint fieldFloat = new FloatPoint("rating", rate);
            doc_comment.add(fieldFloat);

            String rating_string;
            if(rate < 0){
              rating_string = "Desconocido"  ;
            }
            else{
                rating_string = Float.toString(rate);
            }
            doc_comment.add(new StringField("rating_display", rating_string, Field.Store.YES));

            doc_comment.add(new IntPoint("cleanliness", Integer.parseInt(list_Cleanliness[contador])));
            doc_comment.add(new StringField("cleanliness_display", list_Cleanliness[contador], Field.Store.YES));
            doc_comment.add(new SortedSetDocValuesFacetField("cleanliness_display", list_Cleanliness[contador]));

            doc_comment.add(new IntPoint("service", Integer.parseInt(list_Service[contador])));
            doc_comment.add(new StringField("service_display", list_Service[contador], Field.Store.YES));
            doc_comment.add(new SortedSetDocValuesFacetField("service_display", list_Service[contador]));

            FloatPoint fieldFloat2 = new FloatPoint("price", value);
            doc_comment.add(fieldFloat2);
            
            String price_string;
            if(value == 99999.00){
              price_string = "Desconocido"  ;
            }
            else{
                price_string = Float.toString(value);
            }
            doc_comment.add(new StringField("price_display", price_string, Field.Store.YES));
            doc_comment.add(new SortedNumericDocValuesField("price_sorted", NumericUtils.floatToSortableInt(value)));
            
            
            iwriter.addDocument(facetconfig.build(doc_comment));
        }

        System.out.println("Docs generados: " + contador);

    }

    long indexableDate(String fecha) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        Date date = format.parse(fecha);
        Calendar idate = Calendar.getInstance();
        idate.setTime(date);
        idate.set(Calendar.HOUR, 12);

        String indexableDate = DateTools.dateToString(idate.getTime(), DateTools.Resolution.DAY);
        //System.out.println(indexableDate);
        //System.out.println("--------->"+DateTools.stringToTime(indexableDate));
        return DateTools.stringToTime(indexableDate);
    }

    public static float repareStringToFloat(String s) {
        float res = -1;
        if (!s.contains("Unkonwn")) {//EN la coleccion está escrito erroneamente Unkonwn
            if (s.startsWith("$")) {
                s = s.replaceAll(",", "");//hay numeros con comas
                s = s.substring(1, s.length() - 1);
                res = Float.parseFloat(s);

            } else if (Character.isDigit(s.charAt(0))) {
                s = s.substring(0, s.length() - 1);
                res = Float.parseFloat(s);
            }
        }
        return res;
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
