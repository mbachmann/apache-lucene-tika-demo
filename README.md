# Apache-Lucene-Tika-Demo

https://github.com/mbachmann/apache-lucene-tika-demo

Apache Lucene is a high-performance and full-featured text search engine library written
entirely in Java from the Apache Software Foundation.

It is a technology suitable for nearly any application that requires full-text search,
especially in a cross-platform environment.

## Apache Lucene Features

Lucene offers powerful features like scalable and high-performance indexing of the documents and search capability through a simple API.

- powerful, accurate and efficient search algorithms written in Java
- it is a cross-platform solution
- popular in both academic and commercial settings


The Lucene home page is http://lucene.apache.org.

Lucene provides search over documents; where a document is essentially a collection of fields.

A field consists of:

- a field name that is a string
- and one or more field values.


There are two ways to store text data:

- string fields store the entire item as one string
- text fields store the data as a series of tokens.

## Apache Tika

The Apache Tika™ toolkit detects and extracts metadata and text from over a thousand different file types
(such as PPT, XLS, and PDF).
All of these file types can be parsed through a single interface,
making Tika useful for search engine indexing, content analysis, translation, and much more.

The Apache Tika home page is https://tika.apache.org/

## What is in this demo?

The demo consists of 2 Java Files:

- [SimpleFileIndexer.java](src/main/java/com/example/lucene/SimpleFileIndexer.java)
- [SimpleSearcher.java](src/main/java/com/example/lucene/SimpleSearcher.java)

The pom.xml is expecting a JDK8. The maven plugins can build a fat jar.

```bash
mvn clean package
java -jar target/*s.jar
```

### SimpleFileIndexer

First we want to set the properties of Tika:

```java
 System.setProperty("tika.config", "tika-config.xml");
```

In the resource folder is a file with the name _tika-config.xml_:

```xml
<properties>
   <service-loader initializableProblemHandler="ignore"/>
</properties>

```

This file inhibits warnings about not found handlers:

Then we define where we want to index files and where is the index database folder:

```java
 String homeDir = System.getProperty("user.home");
 String projectDir = System.getProperty("user.dir");

 File indexDir = new File(projectDir + "/Index");
 File dataDir = new File(homeDir + "/Downloads");
```

The data directory it the _Downloads_ folder and the _Index_ folder is created at this project root.

A suffix can be used to filter the indexing:
```java
 String suffix = null; // null "txt"; //"xml";
```

The value _null_ takes all files.

We are using the SimpleAnalyzer for the IndexWriter;

```java
 SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();

 IndexWriterConfig indexWriterConfig = new IndexWriterConfig(simpleAnalyzer);
 indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

 Directory fsDirectory = FSDirectory.open(indexDir.toPath());

 IndexWriter indexWriter = new IndexWriter( fsDirectory, indexWriterConfig);
```

The indexing is recursive through the folders and sub folders.

The index consists of 3 TextFields and 1 StringField.

The TextFields are:

- contentField
- fileNameField
- fileParentField

The StringField is used to store the path to the file:

- filePathField

```java
 Field contentField = new TextField("contents", content, Field.Store.YES);

 // index file name
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
```

The content of the lucene document is produced by Tika

```java
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
```

Depending on the configuration we just add or update the the document:

```java
 Document doc = getDocument(tika, file);

 if (indexWriter.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
   indexWriter.addDocument(doc);
 } else {
   indexWriter.updateDocument(new Term(file.getCanonicalPath()), doc);
 }
```


### SimpleSearcher

We are setting the folders of the index database:
```java
 String projectDir = System.getProperty("user.dir");
 File indexDir = new File(projectDir + "/Index");
```

We define a search string and a max number of hits:

```java
 String query = "java";  // .txt or content of a document
 int hits = 100;    
```

We can use the Simple or StandardAnalyzer:

```java
 Directory directory = FSDirectory.open(indexDir.toPath());
 IndexReader indexReader = DirectoryReader.open(directory);
 IndexSearcher searcher = new IndexSearcher(indexReader);

 Analyzer analyzer = new SimpleAnalyzer();
 // Analyzer analyzer = new StandardAnalyzer();
```

We can create a parser of each type like _filename_, _contents_ and _parent_folder_

```java
 QueryParser filenameParser = new QueryParser("filename", analyzer);
 filenameParser.setAllowLeadingWildcard(true);

 QueryParser contentParser = new QueryParser("contents", analyzer);
 contentParser.setAllowLeadingWildcard(true);

 QueryParser pathParser = new QueryParser("parent_folder", analyzer);
 pathParser.setAllowLeadingWildcard(true);

```

