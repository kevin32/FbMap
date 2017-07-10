package com.example.kevin.fbmap.mapRoute;

import android.os.AsyncTask;

import java.io.IOException;

/**
 * This class is used to download JSON info.
 * We can use it later to draw path on the map.
 */

public class DownloadPathJSON extends AsyncTask<String, Void, String> {
    @Override
    protected String doInBackground(String... url) {
        String data = "";
        MapHttpConnection http = new MapHttpConnection();
        try {
            data = http.readUrl(url[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }


        return data;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        new DrawPathToMap().execute(result);
    }

}