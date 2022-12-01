import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;

public class RainforestAPISearch {

    public String searchStates(String query, String refinements) {
        BufferedReader reader;
        StringBuilder responseContent = new StringBuilder();
        HttpURLConnection conn = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try{
            System.out.println("Querying for: " + query);
            System.out.println("with refinements: " + refinements);
            URL url = new URL("https://api.rainforestapi.com/request" +
                    "?api_key=DB04626EED0E436C80223796F57DE85F&type=search&amazon_domain=amazon.com" +
                    "&search_term=" + query +
                    "&refinements=" + refinements);
            System.out.println(url);

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
        results.put("Filters", new ArrayList<>());

        // ANSWERS AND SUGGESTIONS
        JSONObject hits = new JSONObject(responseBody);
        JSONArray items = hits.getJSONArray("search_results");

        System.out.println("Found " + items.length() + " items.");

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String link = item.getString("link");
            String title = item.getString("title");
//            System.out.println(title);

            HashMap<String, String> titleMap = new HashMap<>();
            titleMap.put("title", title);
            titleMap.put("id", "suggest-" + i);
            titleMap.put("from", "rainforest search");
            results.get("Suggest").add(titleMap);

            HashMap<String, String> linkMap = new HashMap<>();
            linkMap.put("link", link);
            linkMap.put("content", "empty");
            linkMap.put("title", title);
            linkMap.put("id", "suggest-" + i);
            linkMap.put("from", "rainforest search");
            results.get("Answer").add(linkMap);
        }
        // FILTERS AND REFINEMENTS
        JSONObject filters = hits.getJSONObject("refinements");
        for (Iterator<String> it = filters.keys(); it.hasNext(); ) {
            String key = it.next();
            JSONArray filterCategories = filters.getJSONArray(key);

            for (int i = 0; i < filterCategories.length(); i++) {
                HashMap<String, String> filterMap = new HashMap<>();

                JSONObject item = filterCategories.getJSONObject(i);
                String filterName = item.getString("refinement_display_name") +
                        ": " + item.getString("name");
                String filterValue = item.getString("value");

                if (!filterValue.contains("undefined")) {
                    filterMap.put("name", filterName);
                    filterMap.put("value", filterValue);
                    results.get("Filters").add(filterMap);
                }
            }
        }
        System.out.println(results);
        return results;
    }
}
