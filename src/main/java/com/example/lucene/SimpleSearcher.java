package com.example.lucene;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;




public class SimpleSearcher {
    public static void main(String[] args) throws Exception {
        String projectDir = System.getProperty("user.dir");
        File indexDir = new File(projectDir + "/Index");
        if (! indexDir.exists()){
            indexDir.mkdir();
        }

        // we are trying to find the word This
        String query = ".xml";  // .txt or content of a document
        int hits = 100;

        System.out.println("Index Directory   = " + projectDir + "/Index");
        System.out.println("Search for [" + query + "] in content, filename and path. \n");

        SimpleSearcher searcher = new SimpleSearcher();
        searcher.searchIndex(indexDir, query, hits);
    }

    private void searchIndex(File indexDir, String queryStr, int maxHits) throws Exception {

        Directory directory = FSDirectory.open(indexDir.toPath());
        IndexReader indexReader = DirectoryReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(indexReader);

        SimpleAnalyzer analyzer = new SimpleAnalyzer();
        //Analyzer analyzer = new StandardAnalyzer();

        QueryParser filenameParser = new QueryParser("filename", analyzer);
        filenameParser.setAllowLeadingWildcard(true);

        QueryParser contentParser = new QueryParser("contents", analyzer);
        contentParser.setAllowLeadingWildcard(true);

        QueryParser pathParser = new QueryParser("parent_folder", analyzer);
        pathParser.setAllowLeadingWildcard(true);


        printHits(searcher, filenameParser.parse(queryStr), maxHits,"filenameParser");
        printHits(searcher, contentParser.parse(queryStr), maxHits,"contentParser");
        printHits(searcher, pathParser.parse(queryStr), maxHits, "parent_folder");

    }

    private void printHits(IndexSearcher searcher,  Query query, int maxHits, String parserName) throws IOException {
        TopDocs topDocs = searcher.search(query, maxHits);
        ScoreDoc[] hits = topDocs.scoreDocs;
        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document doc = searcher.doc(docId);
            System.out.println(doc.get("path"));
        }

        System.out.println(parserName + ": Found " + hits.length);
    }

}
