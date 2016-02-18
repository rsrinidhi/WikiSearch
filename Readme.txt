****************************************************************************************
									ReadMe.txt
									Project : Building Watson
									Srinidhi Rajanarayanan
****************************************************************************************
				
I. If using IDE, then load pom.xml to load the dependencies. The main class is in 
Lucene.java. Compile and run the program. The arguments to the program are

1. Read the Wikipedia pages and index them in Lucene: "path/to/the/wiki/pages" "config"
   where "config" can be "None", "Stem" or "Lemma".
   
   This will create index folders in the src directory.
   
   e.g: "../Downloads/Wikipages" "Lemma" 
   This will lemmatize the documents.
   
   Lemmatization will take around 25 mins to create indices in Lucene.
   
2. Answering Jeopardy question: The "questions.txt" file can be found under resources.
   To find the answers to the Jeopardy question, the argument is "Lemma" or "Stem" or "None".
   
   e.g: "Lemma"
   
   This will lemmatize the queries and will find the answers in the IndexLemma folder
   created in above step under "src".
   
   Make sure that the index is created first before querying.

II. If running from the terminal, follow the steps below:
1. cd to project folder where pom.xml can be found
2. mvn package
3. java -jar target/Search-1.0-SNAPSHOT-jar-with-dependencies.jar "path/to/the/wiki/pages"
 "Lemma"
 
  This will lemmatize the wiki pages and create an index folder in src.
		
  e.g: java -jar target/Search-1.0-SNAPSHOT-jar-with-dependencies.jar "Lemma"
  
  This will lemmatize the queries and will find the answers in the IndexLemma folder
  created in above step under "src".