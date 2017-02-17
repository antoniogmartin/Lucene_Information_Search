package stemmerprueba;

import GUI.TripadvisorView;
import stemmerprueba.LuceneManager;
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
import javax.swing.JFileChooser;

import org.apache.commons.lang.StringUtils;

import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;

public class Lanzador {

    private static final Lanzador instance = new Lanzador();
    
    private Lanzador() {
    }
     public static Lanzador getInstance() {
        return instance;
    }
     
    
    public static void main(final String[] args) throws Exception {
       
        Lanzador tikaObjectModel=Lanzador.getInstance();
        TripadvisorView tripAdvisorView = new TripadvisorView();
        tripAdvisorView.setTikaObject(tikaObjectModel);
        tripAdvisorView.showView();  
            
    }
}


