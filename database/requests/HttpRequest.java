package com.foltran.mermaid.database.requests;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.foltran.mermaid.MainActivity;
import com.foltran.mermaid.R;
import com.foltran.mermaid.ui.recommendation_feed.RecommendationFeedFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequest extends AsyncTask<String, Void, String> {

    String usersCollection = "users";
    public static final String REQUEST_METHOD = "GET";
    public static final int READ_TIMEOUT = 15000;
    public static final int CONNECTION_TIMEOUT = 15000;

    private MainActivity activity;

    public HttpRequest(MainActivity activity){
        this.activity = activity;
    }

    @Override
    protected String doInBackground(String... params){

        String stringUrl = params[0];
        String result;
        String inputLine;

        try {
            URL myUrl = new URL(stringUrl);

            HttpURLConnection connection =(HttpURLConnection)
                    myUrl.openConnection();

            connection.setRequestMethod(REQUEST_METHOD);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);

            connection.connect();
            InputStreamReader streamReader = new
                    InputStreamReader(connection.getInputStream());

            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();

            while((inputLine = reader.readLine()) != null){
                stringBuilder.append(inputLine);
            }

            reader.close();
            streamReader.close();

            result = stringBuilder.toString();
        }
        catch(IOException e){
            e.printStackTrace();
            result = null;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        Map<String, Object> data = new HashMap<>();
        List<Map<String, Double>> recommendations = new ArrayList<>();

        String[] parsedResults = result.substring(11, result.length() - 2).split("]");

        for (String parsedResult : parsedResults){
            Map<String, Double> curLocation = new HashMap<>();

            curLocation.put(
                    parsedResult.substring(
                            parsedResult.indexOf("\"") + 1,
                            parsedResult.lastIndexOf("\"")),
                    Double.valueOf(parsedResult.substring(
                            parsedResult.lastIndexOf(",") + 1
                    )));

            recommendations.add(curLocation);
        }

        data.put("recommendations", recommendations);
        db.collection(usersCollection).document(user.getEmail())
                .set(data, SetOptions.merge());

        //SystemClock.sleep(3000);


        return result;
    }

    protected void onPostExecute(String result){
        super.onPostExecute(result);

        //{"result":[["Los Angeles, California",0.822103948097509],["Boston, Massachusetts",0.8058750728198143],["San Diego, California",0.7759998755484189],["San Francisco, California",0.7624387032751156],["Seattle, Washington",0.7310038794959425],["Estes Park, Colorado",0.7277608913723421],["Salt Lake City, Utah",0.7198517021871224],["New York City, New York",0.6938410758093317],["Portland, Oregon",0.68888206164789],["Bellingham, Washington",0.6863380453236906],["Chicago, Illinois",0.6559841835659415],["Juneau, Alaska",0.6552396237803081],["New Orleans, Louisiana",0.6543172412706602],["Madison, Wisconsin",0.6350143573658564],["Eugene, Oregon",0.6059811018085518],["Denver, Colorado",0.6002432236418487],["Logan, Utah",0.5917160673855709],["Anchorage, Alaska",0.5881312584718306],["Miami, Florida",0.5754657381083719],["Milwaukee, Wisconsin",0.5698175222068451],["Buffalo, New York",0.4991268184387078]]}

    }
}