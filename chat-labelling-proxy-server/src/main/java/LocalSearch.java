import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;

public class LocalSearch {

    public String searchStates(String query) {
        BufferedReader reader;
        StringBuilder responseContent = new StringBuilder();
        HttpURLConnection conn = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try{
            URL url = new URL("http://localhost:9200/amazon/_search?&filter_path=hits.hits._source.*&q=" + query);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            String line;
            InputStream stream;

            // Test if the response from the server is successful
            int status = conn.getResponseCode();
            if (status >= 300) {
                stream = conn.getErrorStream();
            }
            else {
                stream = conn.getInputStream();
            }

            reader = new BufferedReader(new InputStreamReader(stream));
            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
            }
            reader.close();

            HashMap results = this.parse(responseContent.toString());
            return objectMapper.writeValueAsString(results);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            assert conn != null;
            conn.disconnect();
        }
        return "";
    }

    public HashMap parse(String responseBody) {
        HashMap<String, ArrayList<HashMap<String, String>>> results = new HashMap<>();
        results.put("Suggest", new ArrayList<>());
        results.put("Answer", new ArrayList<>());

        JSONObject hits = new JSONObject(responseBody);
        JSONArray items = hits.getJSONObject("hits").getJSONArray("hits");

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i).getJSONObject("_source");
            String title = item.getString("title");
            String text = item.getString("text");

            HashMap<String, String> titleMap = new HashMap<>();
            titleMap.put("title", title);
            results.get("Suggest").add(titleMap);

            HashMap<String, String> textMap = new HashMap<>();
            textMap.put("title", text);
            results.get("Answer").add(textMap);
        }

        System.out.println(results);
        return results;
    }
}
