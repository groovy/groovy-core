import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version
import org.apache.lucene.index.DirectoryReader

/**
 * Searcher: searches a Lucene index for a query passed as an argument
 *
 * @author Jeremy Rayner <groovy@ross-rayner.com>
 * based on examples in the wonderful 'Lucene in Action' book
 * by Erik Hatcher and Otis Gospodnetic ( http://www.lucenebook.com )
 *
 * June 25th, 2013: Updated for Lucene 4.3.1
 * requires a lucene-4.x.x.jar from http://lucene.apache.org
 */

if (args.size() != 2) {
    throw new Exception("Usage: groovy -cp lucene-4.3.1.jar Searcher <index dir> <query>")
}
def indexDir = new File(args[0]) // Index directory create by Indexer
def q = args[1] // Query string

if (!indexDir.exists() || !indexDir.directory) {
    throw new Exception("$indexDir does not exist or is not a directory")
}

def fsDir = DirectoryReader.open(FSDirectory.open(indexDir))
def is = new IndexSearcher(fsDir) // Open index

def parser = new QueryParser(Version.LUCENE_43, "contents", new StandardAnalyzer(Version.LUCENE_43))
def query = parser.parse(q) // Parse query
def start = new Date().time
def hits = is.search(query, 10) // Search index
def end = new Date().time

println "Found ${hits.totalHits} document(s) (in ${end - start} milliseconds) that matched query '$q':"

hits.scoreDocs.each { scoreDoc ->
    println(is.doc(scoreDoc.doc)["filename"]) // Retrieve matching document and display filename
}
fsDir.close()
