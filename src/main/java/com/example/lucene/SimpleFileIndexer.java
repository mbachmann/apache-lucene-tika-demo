package com.example.lucene;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * This example is indexing the Downloads folder in the user home directory
 * The index database is written to this project folder Index
 * The suffix is a filter about which files to index, null = all extension
 * https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm
 *
 * https://tika.apache.org/
 */
public class SimpleFileIndexer {

    private final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

    public static void main(String[] args) throws Exception {

        System.setProperty("tika.config", "tika-config.xml");

        String projectDir = System.getProperty("user.dir");
        String homeDir = System.getProperty("user.home");

        File indexDir = new File(projectDir + "/Index");
        if (! indexDir.exists()){
            indexDir.mkdir();
        }
        File dataDir = new File(homeDir + "/Downloads");

        // null all documents
        String suffix = null; // null "txt"; //"xml";

        System.out.println("Working Directory = " + projectDir);
        System.out.println("Index Directory   = " + projectDir + "/Index");
        System.out.println("Data Directory    = " + homeDir + "/Downloads with suffix = " + suffix);

        SimpleFileIndexer simpleFileIndexer = new SimpleFileIndexer();

        int numIndex = simpleFileIndexer.createIndex(indexDir, dataDir, suffix);

        System.out.println("Number of total files indexed:  " + numIndex);
    }

    private int createIndex(File indexDir, File dataDir, String suffix) throws Exception {

        SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(simpleAnalyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        Directory fsDirectory = FSDirectory.open(indexDir.toPath());

        IndexWriter indexWriter = new IndexWriter( fsDirectory, indexWriterConfig);

        indexDirectory(indexWriter, dataDir, suffix);
        int numIndexed = indexWriter.maxDoc();
        indexWriter.close();
        return numIndexed;
    }

    private void indexDirectory(IndexWriter indexWriter, File dataDir, String suffix) throws IOException, TikaException {
        Tika tika = new Tika();
        tika.setMaxStringLength(MAX_CONTENT_LENGTH);

        File[] files = dataDir.listFiles();
        for (int i = 0; i < Objects.requireNonNull(files).length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                indexDirectory(indexWriter, f, suffix);
            }
            else {
                indexFileWithIndexWriter(indexWriter, tika, f, suffix);
            }
        }

    }

    private void indexFileWithIndexWriter(IndexWriter indexWriter, Tika tika, File file, String suffix) throws IOException, TikaException {
        if (file.isHidden() || file.isDirectory() || !file.canRead() || !file.exists()) {
            return;
        }

        if (suffix!=null && !file.getName().endsWith(suffix)) {
            return;
        }

        System.out.println("Indexing file:... " + file.getCanonicalPath());

        Document doc = getDocument(tika, file);

        if (indexWriter.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
            indexWriter.addDocument(doc);
        } else {
            indexWriter.updateDocument(new Term(file.getCanonicalPath()), doc);
        }
    }

    private Document getDocument(Tika tika, File file) throws IOException {

        Document document = new Document();

        //index file contents - read the content of the file with Tika
        Metadata metadata = new Metadata();
        Path path = file.toPath();

        String content = "";

        try( TikaInputStream stream = TikaInputStream.get(path, metadata)) {
            content = tika.parseToString(stream, metadata, MAX_CONTENT_LENGTH);

        } catch (TikaException e) {
            System.out.println("File has Tika Error: " + path);
        }
        Field contentField = new TextField("contents", content, Field.Store.YES);

        // index file name
        String fileName = file.getName();
        Field fileNameField = new TextField("filename", file.getName(), Field.Store.YES);

        // index file path
        Field filePathField = new StringField("path", file.getCanonicalPath(), Field.Store.YES);

        // index file parent path
        String parentFolder = file.getPath().replace(file.getName(), "").replaceAll("/", " ").trim();
        Field fileParentField = new TextField("parent_folder", parentFolder, Field.Store.YES);

        document.add(contentField);
        document.add(fileNameField);
        document.add(filePathField);
        document.add(fileParentField);

        return document;
    }
}
