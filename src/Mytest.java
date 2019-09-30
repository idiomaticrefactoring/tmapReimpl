import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mytest {
    String s = "/Users/sally/Downloads/tmap/APISIM/data/index";
    public static Analyzer analyzer = new StandardAnalyzer();
    public static ArrayList<Document> jdocs = new ArrayList<Document>();
    public static ArrayList<Document> sdocs = new ArrayList<Document>();
    public ArrayList<String> j_listFileName = new ArrayList<String>();
    public ArrayList<String> s_listFileName = new ArrayList<String>();
    // key java api_signature,value top-10 swift apis 
    public HashMap<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();

    public String save_path = "/Users/sally/Downloads/tmap/tmap_res/";

    public static void main(String[] args) {
        Mytest mytest = new Mytest();
       
        String filename = "/Users/sally/Downloads/tmap/APISIM/data/swift/String.json";
        try {

            String java_path = "/Users/sally/Downloads/tmap/tmap_data/tmapdata_java/";
            String swift_path = "/Users/sally/Downloads/tmap/tmap_data/tmapdata_swift/";
            //1. get all java classes path
            getAllFileName(java_path, mytest.j_listFileName);
            System.out.println("java docs path size: " + mytest.j_listFileName.size());
            //2. get all swift classes path
            getAllFileName(swift_path, mytest.s_listFileName);
            System.out.println("swift docs path size: " + mytest.s_listFileName.size());
            //3. save every java API into mytest.jdocs
            for (String j_file : mytest.j_listFileName) {
                mytest.readjsonToDoc(j_file, mytest.jdocs);
            }
            //4. save every swift API into mytest.sdocs
            for (String s_file : mytest.s_listFileName) {
                mytest.readjsonToDoc(s_file, mytest.sdocs);
            }

            System.out.println("swift docs: " + mytest.sdocs.size());
            //4.create indexer for all swift APIs
            mytest.Indexer(mytest.sdocs);
            System.out.println("java docs: " + mytest.jdocs.size());

            //5. create a query for a java API, rank all the swift APIs and save the results into mytest.result.
            int i = 0;
            for (Document doc : mytest.jdocs) {
                if (doc.get("mename").contentEquals("init")) {
                    continue;
                }
                ArrayList<String> res= (ArrayList<String>) mytest.customRanking(mytest.createQueryList(doc));
                mytest.result.put(doc.get("apisig"), res);
            }

            String resultFilePath = mytest.save_path + "tmap_res.json";
            mytest.saveFiles(resultFilePath);
            /*BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(resultFilePath));
            JSONObject jsonObject = new JSONObject();
            ;
            for (String key : mytest.result.keySet()) {
                //System.out.println("key: "+key+mytest.result.get(key).size());
                try {
                    String[] strings = new String[mytest.result.get(key).size()];
                    jsonObject.put(key, mytest.result.get(key).toArray(strings));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            String content = jsonObject.toString();
            bufferedWriter.write(content);
            bufferedWriter.flush();
            bufferedWriter.close();*/
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //save all java APIs into given filepath
    public void saveFiles(String filePath)
    {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filePath));
            JSONObject jsonObject = new JSONObject();
            for (String key : result.keySet()) {
                //System.out.println("key: "+key+result.get(key).size());
                try {
                    String[] strings = new String[result.get(key).size()];
                    jsonObject.put(key, result.get(key).toArray(strings));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            String content = jsonObject.toString();
            bufferedWriter.write(content);
            bufferedWriter.flush();
            bufferedWriter.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    public static void getAllFileName(String path, ArrayList<String> listFileName) {
        File file = new File(path);
        File[] files = file.listFiles();

        for (File a : files) {
            // System.out.println("COme here");
            if (a.isDirectory()) {//如果文件夹下有子文件夹，获取子文件夹下的所有文件全路径。
                getAllFileName(a.getAbsolutePath() + "\\", listFileName);
            } else {
                String name = a.getPath();

                //System.out.println(name);
                //String suffix = name.substring(name.lastIndexOf(".") + 1);
                //if (suffix.equals("java" ) || suffix.equals("swift") ){
                listFileName.add(name);
                //System.out.println(name);
                //}
            }
        }
    }

    // create doc and save into @param docs
    public void readjsonToDoc(String filename, ArrayList<Document> docs) throws IOException {
        File f = new File(filename);
        String jsonString = FileUtils.readFileToString(f);
        try {
            JSONObject jobj = new JSONObject(jsonString);


            String classname = jobj.getString("class_name");

            String classdes = jobj.getString("class_description");
            JSONArray mes = jobj.getJSONArray("Methods");
            //JSONObject mes = (JSONObject) jobj.get("Methods");
            for (int i = 0; i < mes.length(); i++) {
                JSONObject me = (JSONObject) mes.get(i);
                String mename = me.getString("method_name");
                String medes = me.getString("method_description");
                String apisig = me.getString("api_sig");
                String mename_old = me.getString("method_name_old");

                System.out.println("classname :" + filename.substring(filename.lastIndexOf("/") + 1));
                //System.out.println("mename :" + mename);
                MyAPIMtd API = new MyAPIMtd(classname, classdes, mename, medes, apisig, mename_old,filename.substring(filename.lastIndexOf("/") + 1));
                Document doc = createDocument(API);
                docs.add(doc);

                //break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    //create document
    public Document createDocument(MyAPIMtd mtd) {

        Document doc = new Document();

        //API NAME
        doc.add(new TextField("classname", mtd.classname, Field.Store.YES));
        doc.add(new TextField("classdes", mtd.classdes, Field.Store.YES));
        //CLASS RELATED
        doc.add(new TextField("mename", mtd.mename, Field.Store.YES));
        doc.add(new TextField("mename_old", mtd.mename, Field.Store.YES));
        doc.add(new TextField("medes", mtd.medes, Field.Store.YES));
        doc.add(new TextField("apisig", mtd.apisig, Field.Store.YES));
        doc.add(new TextField("realClassname", mtd.filename, Field.Store.YES));
        //System.out.println("docID :" + doc.toString());
        return doc;
    }
    // save into Indexer
    public void Indexer(List<Document> docs) throws IOException {

        Directory dir = FSDirectory.open(Paths.get(s));
        //Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        IndexWriter writer = new IndexWriter(dir, iwc);
        writer.deleteAll();
        for (Document doc : docs) {

            writer.addDocument(doc);
        }
        System.out.println("DOC NUMS :" + writer.numRamDocs());
        writer.close();
    }
    // create five quries
    public ArrayList<Query> createQueryList(Document doc) {
        ArrayList<Query> JQueryList = new ArrayList<Query>();
        List<String[]> querylist = new ArrayList<String[]>();
        List<String[]> fieldlist = new ArrayList<String[]>();
        List<BooleanClause.Occur[]> flaglist = new ArrayList<>();
        Pattern p = Pattern.compile("\\s*|\t|\r|\n");
        Matcher m = p.matcher(doc.get("medes"));
        String dest = m.replaceAll("");
        //System.out.println(doc.getField("classname") + "&&&&&&&&\n" + doc.getField("mename") + "&&&&&&&&\n" + doc.getField("classdes") + "&&&&&&&&\n" + doc.getField("medes"));
        System.out.println("*****************************" + doc.get("api_sig"));
        if (dest.contentEquals("")) {
            String[] fields = new String[]{"classname", "mename"};
            String[] fields2 = new String[]{"classname", "mename_old"};

            String[] query1 = {QueryParser.escape(doc.get("classname")), QueryParser.escape(doc.get("mename"))};
            String[] query2 = {QueryParser.escape(doc.get("classname") + "*"), QueryParser.escape(doc.get("mename"))};
            String[] query3 = {QueryParser.escape(doc.get("classname")), QueryParser.escape(doc.get("mename"))};
            String[] query4 = {QueryParser.escape(doc.get("classname") + "*"), QueryParser.escape(doc.get("mename"))};
            String[] query5 = {QueryParser.escape(doc.get("classname") + "*"), QueryParser.escape(doc.get("mename"))};
            BooleanClause.Occur[] flags1 = {BooleanClause.Occur.MUST, BooleanClause.Occur.SHOULD};
            BooleanClause.Occur[] flags2 = {BooleanClause.Occur.MUST, BooleanClause.Occur.SHOULD};
            BooleanClause.Occur[] flags3 = {BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD};
            BooleanClause.Occur[] flags4 = {BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD};
            BooleanClause.Occur[] flags5 = {BooleanClause.Occur.MUST, BooleanClause.Occur.SHOULD};

            flaglist.add(flags1);
            flaglist.add(flags2);
            flaglist.add(flags3);
            flaglist.add(flags4);
            flaglist.add(flags5);


            fieldlist.add(fields);
            fieldlist.add(fields);
            fieldlist.add(fields);
            fieldlist.add(fields);
            fieldlist.add(fields2);

            querylist.add(query1);
            querylist.add(query2);
            querylist.add(query3);
            querylist.add(query4);
            querylist.add(query5);

        } else {
            String[] fields = new String[]{"classname", "mename", "classdes"};
            String[] fields2 = new String[]{"classname", "mename_Old", "classdes"};
            String[] query1 = {QueryParser.escape(doc.get("classname")), QueryParser.escape(doc.get("mename")), QueryParser.escape(doc.get("medes"))};
            String[] query2 = {QueryParser.escape(doc.get("classname") + "*"), QueryParser.escape(doc.get("mename")), QueryParser.escape(doc.get("medes"))};
            String[] query3 = {QueryParser.escape(doc.get("classname")), QueryParser.escape(doc.get("mename")), QueryParser.escape(doc.get("medes"))};
            String[] query4 = {QueryParser.escape(doc.get("classname") + "*"), QueryParser.escape(doc.get("mename")), QueryParser.escape(doc.get("medes"))};
            String[] query5 = {QueryParser.escape(doc.get("classname") + "*"), QueryParser.escape(doc.get("mename")), QueryParser.escape(doc.get("medes"))};

            BooleanClause.Occur[] flags1 = {BooleanClause.Occur.MUST, BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD};
            BooleanClause.Occur[] flags2 = {BooleanClause.Occur.MUST, BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD};
            BooleanClause.Occur[] flags3 = {BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD};
            BooleanClause.Occur[] flags4 = {BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD};
            BooleanClause.Occur[] flags5 = {BooleanClause.Occur.MUST, BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD};
            flaglist.add(flags1);
            flaglist.add(flags2);
            flaglist.add(flags3);
            flaglist.add(flags4);
            flaglist.add(flags5);


            fieldlist.add(fields);
            fieldlist.add(fields);
            fieldlist.add(fields);
            fieldlist.add(fields);
            fieldlist.add(fields2);

            querylist.add(query1);
            querylist.add(query2);
            querylist.add(query3);
            querylist.add(query4);
            querylist.add(query5);

        }
        for (int i = 0; i < querylist.size(); i++) {
            JQueryList.add(createQuery(flaglist.get(i), querylist.get(i), fieldlist.get(i)));
        }
        return JQueryList;
    }
    // rank the all documents for queryList, recompute the similarity and return ranking API list.
    public List customRanking(List<Query> queryList){
        ArrayList<String> res=new ArrayList<String>();
        try {
            HashMap<String,Float> mapDesc = new HashMap<String,Float>();
            Directory directory = FSDirectory.open(Paths.get(s));
            // 索引读取工具
            IndexReader reader = DirectoryReader.open(directory);
            // 索引搜索工具
            Float avg = new Float(queryList.size());
            Float similarity, baseSimilarity, srcScoreRange, targetScoreRange;
            targetScoreRange = new Float(1);
            for (int idx = 0; idx < queryList.size(); idx++) {
                Query query = queryList.get(idx);
                List<ScoreDoc>  scoresList = search(query);
                baseSimilarity = Float.MIN_VALUE;
                srcScoreRange = Float.MIN_VALUE;
                if (result.size() > 0) {
                    baseSimilarity = scoresList.get(scoresList.size() - 1).score;
                    srcScoreRange = scoresList.get(0).score - scoresList.get(scoresList.size() - 1).score;
                }
                for (int i = 0; i < scoresList.size(); i++) {
                    ScoreDoc sd= scoresList.get(i);
                    int docID=sd.doc;
                    // 根据编号去找文档
                    Document doc = reader.document(docID);
                    similarity = sd.score - baseSimilarity;
                    similarity = similarity * (targetScoreRange / srcScoreRange);
                    similarity = similarity / avg;
                    similarity = similarity * (Float.parseFloat("0.01") * (avg - idx));
                    mapDesc.put(doc.get("apisig"),similarity);
                    System.out.println("query: " + query.toString()+"---------\n");
                    System.out.println("id: " + doc.get("classname"));
                    System.out.println("mename: " + doc.get("mename"));
                    res.add(doc.get("apisig")) ;
                    System.out.println("apisig: " + doc.get("apisig"));
                    // 取出文档得分
                    System.out.println("得分： " + sd.score);
                    System.out.println(docID+"------------------------------------------"+reader.numDocs());
                }
            }
            List<Map.Entry<String, Float>> list = new ArrayList<Map.Entry<String, Float>>(mapDesc.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {
                public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2)
                {
                    if ((o2.getValue() - o1.getValue())>0)
                        return 1;
                    else if((o2.getValue() - o1.getValue())==0)
                        return 0;
                    else
                        return -1;
                }
            });
            for(Map.Entry<String, Float> temp:list){

                res.add(temp.getKey());

            }

        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return res;
    }
    // create a query
    public Query createQuery(BooleanClause.Occur[] flags,
                             String[] queries, String[] fields){
        Query query = null;
        try
        {
            query = MultiFieldQueryParser.parse(
                    queries, fields, flags, analyzer);
        }
        catch(Exception e)
        {
            System.out.println(e);
           // System.out.println("here");
            //TODO  Fix for empty search Str
        }
        System.out.println(query==null);
        return query;
    }
    //search  top-10 document for a given query
    public List search(Query query) throws IOException {
        ArrayList<String> res=new ArrayList<String>();
        Directory directory = FSDirectory.open(Paths.get(s));
        // 索引读取工具
        IndexReader reader = DirectoryReader.open(directory);
        // 索引搜索工具
        System.out.println("count: "+reader.numDocs());
        IndexSearcher searcher = new IndexSearcher(reader);
        //System.out.println(reader.getTermVectors().toString());
        TopDocs topDocs = searcher.search(query, 10);
        // 获取总条数
        System.out.println("本次搜索共找到" + topDocs.totalHits + "条数据");
        // 获取得分文档对象（ScoreDoc）数组.SocreDoc中包含：文档的编号、文档的得分
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        List<ScoreDoc> scoreDocs1 = Arrays.asList(scoreDocs);
        return scoreDocs1;
    }


}
