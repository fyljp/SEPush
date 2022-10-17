import java.io.Console;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputFilter.Config;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main{
    public static void main(String[] args){
        App(args.toString());
    }

    public static void App(String args) {
        while(true){
            args = args.toLowerCase();
            System.out.println("Log::SEPush Loaded!");
            LoadConfig();
            if(args.equals("run")){
                Run();
            }else{
                Console con = System.console();
                String Action = con.readLine("Action:");
                if(Action.equals("run")){
                    Run();
                }else if(Action.equals("config")){
                    ConfigSetup();
                }else if(Action.equals("add")){
                    AddSitemap();
                }
            }
        }
        

    }
    //Load Sitemap List
    static ArrayList<String> SitemapList = new ArrayList<>();
    //Load Url List
    static ArrayList<String> UrlList = new ArrayList<>();
    //load submitted list
    static ArrayList<String> UnSubmittedList = new ArrayList<>();
    //Others Config
    static HashMap<String,String> ConfigMap = new HashMap<String,String>();

    private static void Run(){
        
        
        Discover();
        SubmitBaidu();

    }

    private static void AddSitemap(){
        Console con = System.console();
        String SiteMap = con.readLine("=>Please input sitemap xml file:");
        if(!SiteMap.isBlank()){
            if(SitemapList.contains(SiteMap)){
                System.out.print("Log::Sitemap is already exist.");
            }else{
                SitemapList.add(SiteMap);
                SaveConfig();
                System.out.println("Log::Sitemap add success.");
            }
        }

    }
    private static void ConfigSetup(){
        Console con = System.console();
        String BaiduSite = con.readLine("=>Please Input Baidu Site: Example:https://ov0vo.cn Now Value:" + ConfigMap.getOrDefault("BaiduSite", "") + "\r\n");
        String BaiduToken = con.readLine("=>Please Input Baidu Token: Now Value:" + ConfigMap.getOrDefault("BaiduToken", "") + "\r\n");
        if(!BaiduSite.equals("")){
            if(ConfigMap.containsKey("BaiduSite")){
                ConfigMap.remove("BaiduSite");
            }
            ConfigMap.put("BaiduSite", BaiduSite);
        }
        if(!BaiduToken.equals("")){
            if(ConfigMap.containsKey("BaiduToken")){
                ConfigMap.remove("BaiduToken");
            }
            ConfigMap.put("BaiduToken", BaiduToken);
        }
        while(true){
            String Save = con.readLine("BaiduSite = " + ConfigMap.get("BaiduSite") + "\r\nBaiduToken = " + ConfigMap.get("BaiduToken") + "\r\nIs is Correct? y/n");
            if(Save.toLowerCase().equals("y")){
                SaveConfig();
                break;
            }else if(Save.toLowerCase().equals("n")){
                LoadConfig();
                break;
            }
        }
        
    }
    private static void SubmitBaidu(){
        while(true){
            String CachdUrl = "";
            int count = 0;
            ArrayList<String> UnSubmittedListCloned = (ArrayList<String>)UnSubmittedList.clone();
            for(String UrlItem : UnSubmittedList){
                CachdUrl += UrlItem + "\r\n";
                UnSubmittedListCloned.remove(UrlItem);
                count++;
                if(count > 100){
                    break;
                }
            }
            String Res = httpPost("http://data.zz.baidu.com/urls?site=" + ConfigMap.get("BaiduSite") + "&token=" + ConfigMap.get("BaiduToken") , CachdUrl);
            if(Res.equals("")){
                System.out.println("Log::Request Failed!");
                return;
            }
            Pattern p = Pattern.compile("\"error\":401");
            Boolean isFailed = Pattern.matches("\"error\":401", Res);
            System.out.println(Res);
            if(isFailed){
                Console con = System.console();
                String Continue = con.readLine("=>Contunue?");
                if(Continue.toLowerCase().equals("n")){
                    break;
                }
                
            }
            UnSubmittedList = (ArrayList<String>)UnSubmittedListCloned.clone();
            if(UnSubmittedList.isEmpty()){
                break;
            }
        }
        SaveConfig();
        System.out.println("Log::Submit Finished");

    }


    private static void Discover(){
        for(String SitemapItem : SitemapList){
            try{
                String Siterep = HttpGet(SitemapItem);
                Pattern p = Pattern.compile("<loc>(.*?)</loc>");
                Matcher m = p.matcher(Siterep);
                while(m.find()){
                    if(!UrlList.contains(m.group(1))){
                        UrlList.add(m.group(1));
                        UnSubmittedList.add(m.group(1));
                        System.out.println("Log::discover new url:" + m.group(1));
                    }
                }
            }catch(Exception err){
                System.out.println(err.getMessage());
            }
        }
        SaveConfig();
    }

    private static void LoadConfig(){
        System.out.println("Log::Config loading");
        try{
            ObjectInputStream SitemapListOis = new ObjectInputStream(
                new FileInputStream("SitemapList"));
            SitemapList = (ArrayList) SitemapListOis.readObject();
            SitemapListOis.close();

        }catch(Exception err){
            System.out.println(err.getMessage());
        }

        try{
            ObjectInputStream UrlListOis = new ObjectInputStream(
                new FileInputStream("UrlList"));
            UrlList = (ArrayList<String>) UrlListOis.readObject();
            UrlListOis.close();

        }catch(Exception err){
            System.out.println(err.getMessage());
        }

        try{
            ObjectInputStream UnSubmittedListOis = new ObjectInputStream(
                new FileInputStream("UnSubmittedList"));
            UnSubmittedList = (ArrayList<String>) UnSubmittedListOis.readObject();
            UnSubmittedListOis.close();

        }catch(Exception err){
            System.out.println(err.getMessage());
        }

        try{
            ObjectInputStream ConfigMapOis = new ObjectInputStream(
                new FileInputStream("ConfigMap"));
            ConfigMap = (HashMap<String,String>) ConfigMapOis.readObject();
            ConfigMapOis.close();

        }catch(Exception err){
            System.out.println(err.getMessage());
        }
        
    }

    private static void SaveConfig(){
        try{
            ObjectOutputStream SitemapListOos = new ObjectOutputStream(new FileOutputStream("SitemapList"));
            SitemapListOos.writeObject(SitemapList);
            System.out.println("Log::SitemapList saved.");
            SitemapListOos.close();
        }catch(Exception err){
            System.out.println(err.getMessage());
        }

        try{
            ObjectOutputStream UrlListOos = new ObjectOutputStream(new FileOutputStream("UrlList"));
            UrlListOos.writeObject(UrlList);
            System.out.println("Log::UrlList saved.");
            UrlListOos.close();
        }catch(Exception err){
            System.out.println(err.getMessage());
        }

        try{
            ObjectOutputStream UnSubmittedListOos = new ObjectOutputStream(new FileOutputStream("UnSubmittedList"));
            UnSubmittedListOos.writeObject(UnSubmittedList);
            System.out.println("Log::UnSubmittedList saved.");
            UnSubmittedListOos.close();
        }catch(Exception err){
            System.out.println(err.getMessage());
        }

        try{
            ObjectOutputStream ConfigMapOos = new ObjectOutputStream(new FileOutputStream("ConfigMap"));
            ConfigMapOos.writeObject(ConfigMap);
            System.out.println("Log::ConfigMap saved.");
            ConfigMapOos.close();

        }catch(Exception err){
            System.out.println(err.getMessage());
        }
        

    }

    static HttpClient httpClient = HttpClient.newBuilder().build();

    private static String HttpGet(String Url){

        try{
            HttpRequest request = HttpRequest.newBuilder(new URI(Url))
            .header("Accept", "*/*")
            .timeout(Duration.ofSeconds(5))
            .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
    
        }catch(Exception err){
            System.out.println(err.getMessage());
        }
        return "";
    }

    private static String  httpPost(String Url, String Data){
        try{
            HttpRequest request = HttpRequest.newBuilder(new URI(Url))
            .header("Accept", "*/*")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(Duration.ofSeconds(5))
            .POST(BodyPublishers.ofString(Data,StandardCharsets.UTF_8))
            .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
    
        }catch(Exception err){
            System.out.println(err.getMessage());
        }
        return "";
    }
}