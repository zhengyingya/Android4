package com.example.earthquake;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.SumPathEffect;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.widget.ArrayAdapter;

public class EarthquakeListFragment extends ListFragment implements LoaderCallbacks<Cursor>{

	ArrayAdapter<Quake> aa;
	SimpleCursorAdapter adapter;
	ArrayList<Quake> earthQuakes = new ArrayList<Quake>();
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
				
		aa = new ArrayAdapter<Quake>(getActivity(), android.R.layout.simple_list_item_1,earthQuakes);

		adapter = new SimpleCursorAdapter(getActivity(),android.R.layout.simple_list_item_1,null,
											new String[] {EarthquakeProvider.KEY_SUMMARY},
											new int[] {android.R.id.text1},0);
		setListAdapter(adapter);
		getLoaderManager().initLoader(0, null, this);
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				refreshEarthquakes();
			}
		});
		t.start();
	}
	
	private static final String TAG = "EARTHQUAKE";
	private Handler handler = new Handler();
	private void refreshEarthquakes() {	
	    handler.post(new Runnable() {
	        public void run() {
	          getLoaderManager().restartLoader(0, null, EarthquakeListFragment.this); 
	        }
	      });
	    
	    URL url;
	    try {
            String quakeFeed = getString(R.string.quake_feed);  
            url = new URL(quakeFeed);  
  
            URLConnection connection;  
            connection = url.openConnection();  
  
            HttpURLConnection httpConnection = (HttpURLConnection)connection;  
            int responseCode = httpConnection.getResponseCode();  
  
            if (responseCode == HttpURLConnection.HTTP_OK) {  
                InputStream in = httpConnection.getInputStream();  
  
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();  
                DocumentBuilder db = dbf.newDocumentBuilder();  
  
                // Parse the earthquake feed.  
                Document dom = db.parse(in);  
                Element docEle = dom.getDocumentElement();  
  
                // Clear the old earthquakes  
                earthQuakes.clear();  
  
                // Get a list of each earthquake entry.  
                NodeList nl = docEle.getElementsByTagName("entry");  
                if (nl != null && nl.getLength() > 0) {  
                    for (int i = 0 ; i < nl.getLength(); i++) {  
  
                        Element entry = (Element)nl.item(i);  
                        Element title = (Element)entry.getElementsByTagName("title").item(0);  
                        Element g = (Element)entry.getElementsByTagName("georss:point").item(0);  
                        Element when = (Element)entry.getElementsByTagName("updated").item(0);  
                        Element link = (Element)entry.getElementsByTagName("link").item(0);  
  
                        String details = title.getFirstChild().getNodeValue();  
                        String hostname = "http://earthquake.usgs.gov";  
                        String linkString = hostname + link.getAttribute("href");  
  
                        String point = g.getFirstChild().getNodeValue();  
                        String dt = when.getFirstChild().getNodeValue();  
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");  
                        Date qdate = new GregorianCalendar(0,0,0).getTime();  
                        try {  
                           qdate = sdf.parse(dt);  
                        } catch (ParseException e) {  
                            Log.d(TAG, "Date parsing exception.", e);  
                        }  
  
                        String[] location = point.split(" ");  
                        Location l = new Location("dummyGPS");  
                        l.setLatitude(Double.parseDouble(location[0]));  
                        l.setLongitude(Double.parseDouble(location[1]));  
  
                        String magnitudeString = details.split(" ")[1];  
                        int end =  magnitudeString.length()-1;  
                        double magnitude = Double.parseDouble(magnitudeString.substring(0, end));  
  
                        details = details.split("-")[1].trim(); //�޸�  
                        //details = details.split(",")[1].trim();  
  
                        final Quake quake = new Quake(qdate, details, l, magnitude, linkString);  
  
                        // Process a newly found earthquake  
                        handler.post(new Runnable() {  
                            @Override  
                            public void run() {  
                                addNewQuake(quake);  
                            }  
                        });  
	                 
	          }
	        }
	      }
	    } catch (MalformedURLException e) {
	      Log.d(TAG, "MalformedURLException", e);
	    } catch (IOException e) {
	      Log.d(TAG, "IOException", e);
	    } catch (ParserConfigurationException e) {
	      Log.d(TAG, "Parser Configuration Exception", e);
	    } catch (SAXException e) {
	      Log.d(TAG, "SAX Exception", e);
	    }
	    finally {
	    }
	  }
	
	private void addNewQuake(Quake _quake) {

		ContentResolver cr = getActivity().getContentResolver();
		String w = EarthquakeProvider.KEY_DATE + " = " + _quake.getDate().getTime();
	    Cursor query = cr.query(EarthquakeProvider.CONTENT_URI, null, w, null, null);
	    if (query.getCount()==0) {
	        ContentValues values = new ContentValues();

	        values.put(EarthquakeProvider.KEY_DATE, _quake.getDate().getTime());
	        values.put(EarthquakeProvider.KEY_DETAILS, _quake.getDetails());   
	        values.put(EarthquakeProvider.KEY_SUMMARY, _quake.toString());

	        double lat = _quake.getLocation().getLatitude();
	        double lng = _quake.getLocation().getLongitude();
	        values.put(EarthquakeProvider.KEY_LOCATION_LAT, lat);
	        values.put(EarthquakeProvider.KEY_LOCATION_LNG, lng);
	        values.put(EarthquakeProvider.KEY_LINK, _quake.getLink());
	        values.put(EarthquakeProvider.KEY_MAGNITUDE, _quake.getMagnitude());

	        cr.insert(EarthquakeProvider.CONTENT_URI, values);
	      }
	      query.close();
    }

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
	    String[] projection = new String[] {
	    	      EarthquakeProvider.KEY_ID,
	    	      EarthquakeProvider.KEY_SUMMARY
	    	    }; 

	    	    MainActivity earthquakeActivity = (MainActivity)getActivity();
	    	    String where = EarthquakeProvider.KEY_MAGNITUDE + " > " + 
	    	                   earthquakeActivity.minimumMagnitude;
	    	   
	    	    CursorLoader loader = new CursorLoader(getActivity(), 
	    	      EarthquakeProvider.CONTENT_URI, projection, where, null, null);
	    	    
	    	    return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		adapter.swapCursor(c);		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub
		
	}
}