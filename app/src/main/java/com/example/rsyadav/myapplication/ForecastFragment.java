package com.example.rsyadav.myapplication;

/**
 * Created by Vikrant Yadav on 06-12-2014.
 */

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public  class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;
    private String[] forecastArray ={"Unable to connect to Server","","","","","",""};
    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //Line added for fragment to be ale to handle menu events.
        setHasOptionsMenu(true);
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        weatherTask.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        List<String> weekForecast = new ArrayList<String>(
                Arrays.asList(forecastArray));

        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                weekForecast);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ListView listview = (ListView) rootView.findViewById(R.id.listview_forecast);
        listview.setAdapter(mForecastAdapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String weatherString = mForecastAdapter.getItem(position);
                Intent intent = new Intent(getActivity(),DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT,weatherString);
                startActivity(intent);
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater){
        inflater.inflate(R.menu.forecast_fragment,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id= item.getItemId();

        if( id == R.id.action_refresh){
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class FetchWeatherTask extends AsyncTask<Void, Void, String[]>{

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(Void... params){
            /**
             * Now the networking snippet is entered here
             */
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;

            try{
                /**
                 * Construct the URL for the OpenWeatherMap query
                 * for query to New Delhi, IN to provide weather information for entire week in JSON mode and metric unit
                 * http://api.openweathermap.org/data/2.5/forecast/daily?q=Delhi,IN&mode=json&units=metric&cnt=7
                 */
                URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=Delhi,IN&mode=json&units=metric&cnt=7");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                //Read the input stream into string
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if(inputStream == null){
                    forecastJsonStr = null;
                    return null;
                }
                /**
                 * Since the inputStream is OK, we get input it into BufferedReader (reader), which
                 * later will be stored into StringBuffer (buffer).
                 */
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while((line = reader.readLine()) != null){
                    //Adding a new line makes debugging a lot easier
                    buffer.append(line+'\n');
                }

                if(buffer.length() == 0){
                    //The stream is empty, no point in parsing
                    forecastJsonStr = null;
                    return null;
                }
                forecastJsonStr = buffer.toString();
                /**
                 * Segment for parsing the openWeatherMap API JSON Objects
                 * The values are now stored in the weather[] array of the type Weather,
                 * defined in the Weather.java file.
                 */
                JSONObject read = new JSONObject(forecastJsonStr);
                JSONArray list = read.getJSONArray("list");
                for(int i=0;i<list.length();i++){
                    forecastArray[i]= String.valueOf(list.getJSONObject(i).getJSONArray("weather").getJSONObject(0).getString("main")+"\t");
                    forecastArray[i]+=list.getJSONObject(i).getJSONObject("temp").getInt("max")+"/";
                    forecastArray[i]+=list.getJSONObject(i).getJSONObject("temp").getInt("min")+"-";
                    forecastArray[i]+=list.getJSONObject(i).getJSONArray("weather").getJSONObject(0).getString("description");
                }
            }catch(IOException e){
                Log.e(LOG_TAG, "Error", e);
                forecastArray = null;
                return null;
            } catch (JSONException e) {
                forecastArray = null;
            Log.e(LOG_TAG,"Error",e);
            }finally{
                if( urlConnection != null){
                    urlConnection.disconnect();
                }
                if( reader != null){
                    try{
                        reader.close();
                    }catch(final IOException e){
                        Log.e(LOG_TAG,"Error closing stream",e);
                    }
                }
            }
            return forecastArray;
        }

        @Override
        protected void onPostExecute(String[] result){
            if(result != null){
                mForecastAdapter.clear();
                for(String dayForecastStr : result){
                    mForecastAdapter.add(dayForecastStr);
                }
            }
        }
    }
}
