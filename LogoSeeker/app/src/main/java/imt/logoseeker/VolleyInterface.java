package imt.logoseeker;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

class VolleyInterface
{
    private Activity mainActivity;
    private Context context;
    private String url;
    private RequestQueue requestQueue;
    private int countRequest;
    private ArrayList<Brand> brandsFromJson;
    private int nbBrands;

    VolleyInterface(Activity act, Context context, String url){
        this.mainActivity = act;
        this.context = context;
        this.url = url;

        ProgressBar progressBar2 = (ProgressBar) mainActivity.findViewById(R.id.progressBar2);
        ImageView logoStart = (ImageView) mainActivity.findViewById(R.id.logoStart);
        countRequest = 0;
        requestQueue = Volley.newRequestQueue(this.context);
        requestQueue.addRequestFinishedListener(request -> {
            --countRequest;
            if(countRequest <= 0)
            {
                boolean loadingFinished = false;
                if (brandsFromJson != null) {
                    if (brandsFromJson.size() == nbBrands) {
                        boolean missingClassifier = false;
                        for (Brand br : brandsFromJson) {
                            if (mainActivity.getFileStreamPath(br.getClassifierName()) == null) {
                                missingClassifier = true;
                                break;
                            }
                        }
                        if (!missingClassifier) {
                            if (mainActivity.getFileStreamPath("vocabulary.yml") != null) {
                                loadingFinished = true;
                            }
                        }
                    }
                }

                if(loadingFinished) {
                    progressBar2.setVisibility(View.GONE);
                    progressBar2.postInvalidate();
                    logoStart.setVisibility(View.GONE);

                    Button buttonCap = (Button) mainActivity.findViewById(R.id.b_capture);
                    buttonCap.setVisibility(View.VISIBLE);
                    Button buttonLib = (Button) mainActivity.findViewById(R.id.b_Library);
                    buttonLib.setVisibility(View.VISIBLE);
                    Button buttonAnalisys = (Button) mainActivity.findViewById(R.id.b_analysis);
                    buttonAnalisys.setVisibility(View.VISIBLE);

                    ImageView iv = (ImageView) mainActivity.findViewById(R.id.v_picture);
                    iv.setVisibility(View.VISIBLE);
                }
                else
                {
                    // Retrying
                    getJSONIndex();
                    getVocabulary();
                }
            }
            else
            {
                // PROGRESS BAR
                progressBar2.setProgress(nbBrands + 2 - countRequest);
            }
        });

        getJSONIndex();
        getVocabulary();
    }

    private void getJSONIndex()
    {
        // ON RESPONSE : we create the Brand objects stoking JSON info
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, this.url + "index.json", null, response -> {
            if (response != null) {
                brandsFromJson = new ArrayList<>();
                try {
                    JSONArray jsonArray = response.getJSONArray("brands");
                    ProgressBar progressBar2 = (ProgressBar) mainActivity.findViewById(R.id.progressBar2);
                    progressBar2.setMax(jsonArray.length() + 2);
                    nbBrands = jsonArray.length();
                    for (int i = 0; i < nbBrands; i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);

                        JSONArray images = obj.getJSONArray("images");
                        String[] imgNames = new String[images.length()];
                        for (int j = 0; j < images.length();j++) {
                            imgNames[j] = images.get(j).toString();
                        }

                        Brand br = new Brand(obj.getString("brandname"), obj.getString("url"), obj.getString("classifier"),imgNames);
                        brandsFromJson.add(br);
                    }
                    // getting classifiers
                    for(Brand br: brandsFromJson)
                    {
                        getClassifier(br);
                    }
                }
                catch(Exception ex)
                {
                    Log.e("CATCH JSON", ex.toString());
                }
            }
        }, error -> Log.e("LOG", error.toString()));
        requestQueue.add(jsonObjectRequest);
        countRequest++;
    }

    private void getVocabulary()
    {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url + "vocabulary.yml",
                response -> {
                    if (response != null)
                    {
                        try {
                            writeToFile(response,"vocabulary.yml",context);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> Log.e("LOG", error.toString()));
        requestQueue.add(stringRequest);
        countRequest++;
    }

    private void getClassifier(Brand br)
    {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url + "classifiers/" + br.getClassifierName(),
                response -> {
                    if (response != null)
                    {
                        try {
                            writeToFile(response,br.getClassifierName(),context);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> Log.e("LOG", error.toString()));
        requestQueue.add(stringRequest);
        countRequest++;
    }

    private void writeToFile(String data,String name,Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(name, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    ArrayList<Brand> getBrandsFromJson() {
        return brandsFromJson;
    }

    int getNbBrands() {
        return nbBrands;
    }

}
