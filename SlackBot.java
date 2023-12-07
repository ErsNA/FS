package dmn;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.MethodsClient;
import com.slack.api.model.event.MessageEvent;
import dmn.FS.FS;
import dmn.FS.FreeSpinContract;
import net.sf.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SlackBot {
    public static String Cookie;
    public static String UserAgent;
    public static final String src = "https://portal.dragda.com/";//"https://xhtra.free2durian.com/";
    public static final SimpleDateFormat panelDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static final DecimalFormat decimalFormat = new DecimalFormat("###,###.##");
    private static Set<String> respondedMessages = new HashSet<>();

    public static void main(String[] args) throws Exception {
        FS.createTables();
        Cookie = Files.readString(Paths.get("Cookie.txt"));
        UserAgent = Files.readString(Paths.get("UE.txt"));
        String botToken = "xoxb-4670198736676-6295982161922-mv4ssdEkzLzIN3uUJrtDFDMd"; // Slack App Management'tan alınan bot token'ı
        String appToken = "xapp-1-A068SCG3RM2-6298464644852-3ded854f88dd64385a287b2f97df84ee42e8f16ba3f232ee7c876342f51cbafd"; // Slack App Management'tan alınan bot token'ı
        respondedMessages = readSetFromFile();
        App app = new App(AppConfig.builder().singleTeamBotToken(botToken).build());
        Logger.getLogger(App.class.getName()).setLevel(Level.ALL);
        app.message("", (payload, ctx) -> {
            MessageEvent event = payload.getEvent();
            MethodsClient client = ctx.client();
            String channelId = event.getChannel();
            String userId = event.getUser();
            String messageText = event.getText();
            String userID = messageText.replace(".fs ", "");
            System.out.println("Channel: " + channelId + "\nUser: " + userId + "\nMessage: " + messageText);
            String threadTs = event.getTs();
            if (respondedMessages.contains(threadTs)) {
                return ctx.ack();
            }
            if (isStringInt(userID) && userID.length() == 7) {
                switch (channelId) {
                    case "C0692PFSG1X" -> {
                        client.reactionsAdd(r -> r.channel(channelId).timestamp(threadTs).name("eyes"));
                        FreeSpinContract contract = new FreeSpinContract();
                        contract.setToday(panelDateFormat.format(new Date()));
                        contract.setUserID(userID);
                        new FS().GET(contract);
                        System.out.println(contract.getRank());
                        if (contract.getRank().equals("NoFreespinUser")) {
                            String response = userID + "\n" + contract.getRank() + "\n\n\n" + "Üye FreeSpin alamaz.";
                            client.chatPostMessage(r -> r.channel(channelId).threadTs(threadTs).text(response));
                        } else {
                            JSONObject jsonObject = FS.start(contract);
                            String response = jsonObject.getString("pane") + "\n\n\n" + jsonObject.getString("copy");
                            client.chatPostMessage(r -> r.channel(channelId).threadTs(threadTs).text(response));
                        }

                    }
                }
            }
            respondedMessages.add(threadTs);
            writeSetToFile((HashSet<String>) respondedMessages);
            return ctx.ack();
        });

        SocketModeApp socketModeApp = new SocketModeApp(appToken, app);
        socketModeApp.start();

    }

    public static boolean isStringInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static void CookieRefresher(String SetCookie) throws IOException {
        String yeniauth = SetCookie.replace("path=/; HttpOnly; SameSite=Lax", "").replace(".ASPXAUTH", " .ASPXAUTH").replace("; ", "");
        for (String satir : Cookie.split(";")) {
            if (satir.contains("ASPXAUTH")) {
                if (yeniauth.contains("ASPXAUTH")) {
                    Cookie = Cookie.replace(satir, yeniauth);
                    System.out.println("Cookie Yenilendi");
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("Cookie.txt"));
                    bufferedWriter.write(Cookie);
                    bufferedWriter.close();
                    return;
                }
            }
        }
    }

    private static void writeSetToFile(HashSet<String> set) {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("mySet.json"))) {
            outputStream.writeObject(set);
            System.out.println("HashSet dosyaya yazıldı.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static HashSet<String> readSetFromFile() {
        HashSet<String> set = new HashSet<>();
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("mySet.json"))) {
            set = (HashSet<String>) inputStream.readObject();
            System.out.println("HashSet dosyadan okundu.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return set;
    }

    private static Date date(int Date) {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, Date);
        return cal.getTime();
    }

    public static void refresher() {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String today = simpleDateFormat.format(date(0));
            String yesterday = simpleDateFormat.format(date(-1));

            String POST_PARAMS = "sEcho=4&iColumns=6&sColumns=%2C%2C%2C%2C%2C&iDisplayStart=0&iDisplayLength=1000&mDataProp_0=UserProfileId&bSortable_0=true&mDataProp_1=UserUUID&bSortable_1=true&mDataProp_2=Mobile&bSortable_2=true&mDataProp_3=CreateTime&bSortable_3=true&mDataProp_4=Note&bSortable_4=true&mDataProp_5=Id&bSortable_5=true&iSortCol_0=3&sSortDir_0=desc&iSortingCols=1&startDate=" + yesterday + "&toDate=" + today;

            URL obj = new URL(src + "GamblingFinances/RequestCall1");
            HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Cookie", Cookie);
            httpURLConnection.setRequestProperty("User-Agent", UserAgent);
            // For POST only - START
            httpURLConnection.setDoOutput(true);
            OutputStream os = httpURLConnection.getOutputStream();
            os.write(POST_PARAMS.getBytes(UTF_8));
            os.flush();
            os.close();
            // For POST only - END
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), UTF_8));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                if (httpURLConnection.getHeaderField("Set-Cookie") != null) {
                    CookieRefresher(httpURLConnection.getHeaderField("Set-Cookie"));
                }
            } else {
                System.out.println("refresher çalışırken bir sorun ile karşılaştı");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
