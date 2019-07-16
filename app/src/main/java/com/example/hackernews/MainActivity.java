package com.example.hackernews;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    SQLiteDatabase myData;
    ArrayList<String> urls = new ArrayList<String>();
    ArrayList<String> titles=new ArrayList<String>();
    int noOfArticles=20;
    public class DownloadTask extends AsyncTask<String,Void,String>
    {

        @Override
        protected String doInBackground(String... strings) {
            URL url;
            HttpURLConnection httpURLConnection = null;
            String result = "";
            try{
                String title = "";
                String urlString="";
                int id;
                url = new URL(strings[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream in = httpURLConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while(data!=-1){
                    char temp =(char) data;
                    result = result + temp;
                    data= reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);
                if (jsonArray.length()<noOfArticles){
                    noOfArticles=jsonArray.length();
                }
                myData.execSQL("DELETE FROM news");
                for(int i = 0 ;i<noOfArticles;i++){
                    String articleId=jsonArray.getString(i);
                    try{
                        url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");
                        httpURLConnection = (HttpURLConnection) url.openConnection();
                        in = httpURLConnection.getInputStream();
                        reader = new InputStreamReader(in);
                        data = reader.read();
                        String articleInfo="";
                        while(data!=-1){
                            char temp =(char) data;
                            articleInfo = articleInfo + temp;
                            data= reader.read();
                        }
                        JSONObject jsonObject = new JSONObject(articleInfo);
                        if(!jsonObject.isNull("title")&&!jsonObject.isNull("url")){
                            title=jsonObject.getString("title");
                            urlString=jsonObject.getString("url");
                        }
                        String sql = "INSERT INTO news(id,title,url) VALUES(?,?,?)";
                        SQLiteStatement statement = myData.compileStatement(sql);
                        statement.bindString(1,articleId);
                        statement.bindString(2,title);
                        statement.bindString(3,urlString);
                        statement.execute();

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }
            return result;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myData = this.openOrCreateDatabase("News",MODE_PRIVATE,null);
        myData.execSQL("CREATE TABLE IF NOT EXISTS news(id INTEGER PRIMARY KEY,title VARCHAR,url VARCHAR)");
        DownloadTask task =new DownloadTask();
        try{
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
        }catch (Exception e){
            e.printStackTrace();
        }
        //Adding titles and urls from DATABASE to arrayLists
        try {
            Cursor cursor = myData.rawQuery("SELECT * FROM news",null);
            int titleIndex = cursor.getColumnIndex("title");
            int urlIndex = cursor.getColumnIndex("url");
            cursor.moveToFirst();
            int i=0;
            while (cursor!=null&&i<noOfArticles) {
                titles.add(i,cursor.getString(titleIndex));
                urls.add(i,cursor.getString(urlIndex));
                cursor.moveToNext();
                i++;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        ListView listView = (ListView) findViewById(R.id.listView);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent=new Intent(getApplicationContext(), WebViewActivity.class);
                intent.putExtra("URL",urls.get(i));
                startActivity(intent);
            }
        });



    }
}
