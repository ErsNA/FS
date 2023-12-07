package dmn.FS;

import dmn.Core.ObjectHelper;
import dmn.SlackBot;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;

public class FS {

    public static Connection conn = null;
    public static DecimalFormat decimalFormat = new DecimalFormat("###,###.##");
    public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    public static String NoFs = "'Kredi kartı ile ile Yatırma','SimpleKK Yatırma','Kredi Karti ile Yatırma','SimpleKK ile Yatırma','Aninda Kredi Karti ile Yatırma','FlexCreditCard ile Yatırma'";

    public static void testDelete() {
        try {
            conn = new ObjectHelper().getConnection();
            Statement stmt = conn.createStatement();
            long time = simpleDateFormat.parse("12/6/2023 19:27:31").getTime();
            stmt.executeUpdate("DELETE FROM Yatırım where dateOf>" + time);
        } catch (SQLException | ParseException e) {
            throw new RuntimeException(e);
        }
    }
    public static JSONObject start(FreeSpinContract contract) throws IOException {
        JSONObject jsonObject = new JSONObject();
        StringBuilder builder = new StringBuilder();
        builder.append("Üye No:   ").append(contract.getUserID());
        Delete();
        double totalrecord = Deposit(contract, 0);
        int sayi = (int) Math.ceil((totalrecord / 8000));
        for (int i = 0; i < sayi; i++) {
            int iDisplayStart = 8000 * (i + 1);
            System.out.println("Kalan işlem sayısı: " + (sayi - i));
            Deposit(contract, iDisplayStart);
        }
        DepositHesapla(contract);
        if (contract.isKrediKarti()) {
            builder.append("\nİki yatırım birleştirilirse hakkı var arada oyun oynamamış.");
            jsonObject.put("copy", "Kredi kartı yatırımlarına FreeSpin bonusu tanımlanmamaktadır. Farklı bir yöntem ile minimum 250 TL yatırım yaptıktan sonra canlı destek hattına bağlanarak Freespin talebinde bulunabilirsiniz.");
        } else if (contract.isPesPeseYatirim()) {
            builder.append("\nBugüne ait yatırımı yok");
            jsonObject.put("copy", "Son yatırım: " + decimalFormat.format(contract.getLastDeposit()) + "\n Bir önceki yatırım: " + decimalFormat.format(contract.getSecondDeposit()));
        } else if (contract.getYat() > 0) {
            if (contract.isLast()) {
                builder.append("\nYatırımını kullanmış.");
                jsonObject.put("copy", "Freespin haklarınızı bakiyenizi kullanmadan önce talep etmeniz gerekmektedir.");
            } else {
                if (contract.getYat() < 250) {
                    builder.append("\nYatırımı 250 TL den az\nYatırım miktarı: ").append(contract.getLastDeposit()).append(" TL");
                    jsonObject.put("copy", decimalFormat.format(contract.getYat()) + " TL yatırımınız mevcut. FreeSpin talep edebilmeniz için minimum 250 TL yatırım yapmış olmanız gerekmektedir. Yatırımınızı yaptıktan sonra bakiyenizi kullanmadan canlı destek hattına bağlanarak FreeSpin haklarınızı talep edebilirsiniz.");
                } else {
                    FsSorgulaYeni(contract);
                    FsHesapla(contract);

                    if (contract.getAlinanFs() <= 5) {
                        builder.append("\nBugün eklenen toplam:\t").append(contract.getAlinanFs());
                        builder.append("\nSon Hakkı:\t").append(contract.getFsTutar()).append(" TRY ").append(contract.getFsHakki()).append(" FS");
                        if (contract.getFsHakki() > 0) {
                            if (FsEkleYeni(contract)) {
                                jsonObject.put("copy", contract.getFsTutar() + " TRY " + contract.getFsHakki() + " FS eklendi" );
                            } else {
                                jsonObject.put("copy", contract.getFsTutar() + " TRY " + contract.getFsHakki() + " FS hakkı var.\nEKLEME YAPILAMADI!!!");
                            }
//                            jsonObject.put("copy", "Üyeye cevap:\n\nSon Hakkı:\t" + contract.getFsHakki() + "\nSpin Değeri:\t" + contract.getFsTutar());
                        } else {
                            builder.append("\nBugünkü yatırımları için tüm hakları eklenmiş.");
                            jsonObject.put("copy", "Bugüne ait yatırımlarınız karşılığında bütün freespin haklarınızı almışsınız. Yeni yatırım yaptıktan sonra tekrar talepte bulunabilirsiniz.");
                        }
                    } else {
                        builder.append("\nBugün içerisinde 5 kez fs almışsınız.");
                        jsonObject.put("copy", "Gün içerisinde maksimum alınabilecek freespin hakkınızı doldurmuşsunuz. Şuan için hakkınız bulunmamaktadır.");
                    }
                }
            }
        } else if (contract.getLastDeposit() == -1) {
            builder.append("\nYatırım yok.");
            jsonObject.put("copy", "FreeSpin bonusundan faydalanabilmek için 250 TL ve üzeri yatırımınız olması gerekmektedir. Yatırım yaptıktan sonra canlı destek hattına bağlanarak talep edebilirsiniz.");
        } else if (contract.isLast()) {
            builder.append("\nYatırımını kullanmış.");
            jsonObject.put("copy", "Freespin haklarınızı bakiyenizi kullanmadan önce talep etmeniz gerekmektedir.");
        }
        jsonObject.put("pane", builder.toString());
        return jsonObject;
    }

    public void GET(FreeSpinContract contract) throws IOException {
        String GET_URL = SlackBot.src + "Core/PlayerProfile?UserProfileID=" + contract.getUserID();
        URL obj = new URL(GET_URL);
        HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setRequestProperty("User-Agent", SlackBot.UserAgent);
        httpURLConnection.setRequestProperty("Cookie", SlackBot.Cookie);
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), UTF_8));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            Document doc = Jsoup.parse(response.toString());
            Elements profile = doc.getElementsByClass("form-group");
            for (Element element : profile) {
                String key = element.text();
                String value = element.getElementsByClass("player-rank-input").attr("value");
                if (key.equals("Statü")) {
                    contract.setRank(value);
                }
            }
        } else {
            out.println("GET request not worked");
        }
    }

    public static void Delete() {
        try {
            conn = new ObjectHelper().getConnection();
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DELETE FROM FreeSpin");
            stmt.executeUpdate("DELETE FROM Yatırım");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static double Deposit(FreeSpinContract contract, int iDisplayStart) throws IOException {
        String POST_PARAMS = "sEcho=3&iColumns=8&sColumns=%2C%2C%2C%2C%2C%2C%2C&iDisplayStart=" + iDisplayStart + "&iDisplayLength=8000&mDataProp_0=TransactionStartDate&mDataProp_1=type&mDataProp_2=amount&mDataProp_3=transCurrency&mDataProp_4=currentbalance&mDataProp_5=userCurrency&mDataProp_6=CustomerNote&mDataProp_7=Status&UserProfileID=" + contract.getUserID() + "&startDate=" + date(0) + "&toDate=" + date(0) + "&type=&status=1&amountFrom=&amountTo=&games=on&product=0";
        URL obj = new URL(SlackBot.src + "Core/PlayerTransactions1R");
        HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Cookie", SlackBot.Cookie);
        httpURLConnection.setRequestProperty("User-Agent", SlackBot.UserAgent);
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
                SlackBot.CookieRefresher(httpURLConnection.getHeaderField("Set-Cookie"));
            }
            try {
                JSONObject jsonObject = JSONObject.fromObject(response.toString());
                JSONArray json = jsonObject.getJSONArray("aaData");//////////////////////
                StringBuilder degerler = new StringBuilder();
                for (Object obje : json) {
                    JSONObject item = JSONObject.fromObject(obje);
                    String type = item.getString("type").replace("'", "''");
                    double amount = item.getDouble("amount");
                    String TransactionStartDate = item.getString("TransactionStartDate");
                    long tarih = Long.parseLong(TransactionStartDate.replace("/Date(", "").replace(")/", ""));
                    degerler.append("('").append(type).append("',").append(amount).append(",'").append(tarih).append("'),\n");
                }
                if (!degerler.toString().isBlank() || !degerler.toString().isEmpty()) {
                    degerler.append("bitisnoktasi");
                    String deger = degerler.toString().replace(",\nbitisnoktasi", "");
                    AddDeposit(deger);
                }
                return jsonObject.getInt("iTotalDisplayRecords") - 8000;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            out.println("POST request not worked");
        }
        return 0;
    }

    public static void AddDeposit(String degerler) {
        try {
            conn = new ObjectHelper().getConnection();
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("INSERT INTO Yatırım (types, amount, dateOf) VALUES" + degerler);
            stmt.executeUpdate("DELETE FROM `Yatırım` where types like '%Bonus ile Yatırma From Admin%' or types like '%Jeton%' or types like '%P2P%'");

//            stmt.executeUpdate("DELETE FROM `Yatırım` where dateOf>"+new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse("12/5/2023 00:38:55").getTime());
//            stmt.executeUpdate("DELETE FROM `Yatırım` where dateOf>" + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse("12/05/2023 01:44:07").getTime());
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void DepositHesaplat(FreeSpinContract contract) {
        try {

            conn = new ObjectHelper().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet resultSet;
            contract.setFirstDepositLong(stmt.executeQuery("SELECT min(dateOf) FROM Yatırım where types like '% Yatırma%' and types not in (" + NoFs + ")").getLong(1));

            resultSet = stmt.executeQuery("select * from Yatırım order by dateOf desc");
            if (resultSet.next()) {
                String types = resultSet.getString("types");
                double amount = resultSet.getDouble("amount");
                long dateOf = resultSet.getLong("dateOf");
                contract.setLastTransactions(types);
                if (types.contains(" Yatırma") && !NoFs.contains(types)) {
                    contract.setLastDeposit(amount);
                    contract.setYat(amount);
                    resultSet = stmt.executeQuery("select * from Yatırım where dateOf<" + dateOf + " order by dateOf desc");
                    if (resultSet.next()) {
                        String types2 = resultSet.getString("types");
                        double amount2 = resultSet.getDouble("amount");
                        long dateOf2 = resultSet.getLong("dateOf");
                        if (types2.contains(" Yatırma") && !NoFs.contains(types2) && amount < 250 && (amount2 + amount) >= 250) {
                            if (!resultSet.next()) {
                                contract.setPesPeseYatirim(true);
                                contract.setSecondDeposit(amount2);
                            }
                        }
                    }
                }
                if (NoFs.contains(types)) {
                    contract.setKrediKarti(true);
                }
            } else {
                contract.setYat(0);
                contract.setLastDeposit(-1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void DepositHesapla(FreeSpinContract contract) {
        try {

            conn = new ObjectHelper().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet resultSet;
            contract.setFirstDepositLong(stmt.executeQuery("SELECT min(dateOf) FROM Yatırım where types like '% Yatırma%' and types not in (" + NoFs + ")").getLong(1));

            resultSet = stmt.executeQuery("select * from Yatırım where types like '% Yatırma%' order by dateOf desc");
            if (resultSet.next()) {
                String types = resultSet.getString("types");
                double amount = resultSet.getDouble("amount");
                long dateOf = resultSet.getLong("dateOf");
                if (NoFs.contains(types)) {
                    contract.setKrediKarti(true);
                }
                if (!NoFs.contains(types)) {
                    contract.setLastDeposit(amount);
                    contract.setYat(amount);
                    resultSet = stmt.executeQuery("select * from Yatırım where dateOf<" + dateOf + " order by dateOf desc");
                    if (resultSet.next()) {
                        String types2 = resultSet.getString("types");
                        double amount2 = resultSet.getDouble("amount");
                        long dateOf2 = resultSet.getLong("dateOf");
                        if (types2.contains(" Yatırma") && !NoFs.contains(types2) && amount < 250 && (amount2 + amount) >= 250) {
                            if (!resultSet.next()) {
                                contract.setPesPeseYatirim(true);
                                contract.setSecondDeposit(amount2);
                            }
                        }
                    }
                }
                resultSet = stmt.executeQuery("select * from Yatırım where amount>0 and dateOf>" + dateOf);
                if (resultSet.next()) {
                    String tip = resultSet.getString("types");
                    if (!tip.contains("Free Bonus") && !tip.contains("Çekme")) {
                        contract.setLast(true);
                        contract.setLastTransactions(resultSet.getString("types"));
                    }
                }
            } else {
                contract.setYat(0);
                contract.setLastDeposit(-1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void AddFreeSpin(String degerler) {
        try {
            conn = new ObjectHelper().getConnection();
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("INSERT INTO FreeSpin (SpinCount, CreateDate, Category, Game) VALUES" + degerler);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void FsHesapla(FreeSpinContract contract) {
        String NoFs = "'Kredi kartı ile ile Yatırma','SimpleKK Yatırma','Kredi Karti ile Yatırma','SimpleKK ile Yatırma','Aninda Kredi Karti ile Yatırma'";
        try {
            conn = new ObjectHelper().getConnection();
            Statement stmt = conn.createStatement();
            contract.setAlinanFs(stmt.executeQuery("select count(SpinCount) from FreeSpin where CreateDate > " + contract.getFirstDepositLong()).getInt(1));
            ResultSet lastFS = stmt.executeQuery("select max(CreateDate) from FreeSpin");
            long fsLong;
            if (lastFS.next()) {
                fsLong = lastFS.getLong(1);
            } else {
                fsLong = 0;
            }
            long deposit = stmt.executeQuery("select max(dateOf) from Yatırım where types like '% Yatırma%' and types not in (" + NoFs + ") and dateOf > " + fsLong).getLong(1);
            ResultSet rs = stmt.executeQuery("select * from Yatırım where dateOf =" + deposit);
            double totalyat = 0;
            if (rs.next()) {
                totalyat = rs.getDouble("amount");
            }
            if (totalyat >= 250 && totalyat < 500) {
                contract.setFsHakki(10);
                contract.setFsTutar(2);
            } else if (totalyat >= 500 && totalyat < 1000) {
                contract.setFsHakki(10);
                contract.setFsTutar(3);
            } else if (totalyat >= 1000 && totalyat < 5000) {
                contract.setFsHakki(10);
                contract.setFsTutar(4);
            } else if (totalyat >= 5000) {
                contract.setFsHakki(10);
                contract.setFsTutar(10);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static String date(int x) {
        SimpleDateFormat PnlDate = new SimpleDateFormat("yyyy-MM-dd");
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, x);
        return PnlDate.format(cal.getTime());
    }

    public static boolean FsEkle(FreeSpinContract contract, StringBuilder builder) throws IOException {

        String POST_PARAMS = "userProfileId=" + contract.getUserID() + "&datefrom=&dateto=" + date(7) + "&spincount=" + contract.getFsHakki() + "&slotids=vs5trjokers,vs5trjokers&category=pragmaticPlay&slotid=vs25pyramid&totalBetValue=&roundOptions=&betTry=&betUsd=&betEur=&campaignId=";
        URL obj = new URL(SlackBot.src + "GamblingFinances/AddFreeSpins");
        HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Cookie", SlackBot.Cookie);
        httpURLConnection.setRequestProperty("User-Agent", SlackBot.UserAgent);
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
            try {
                JSONObject jsonObject = JSONObject.fromObject(response.toString());
                String value = jsonObject.getString("message");
                if (value.equals("SUCCESS")) {
                    builder.append("\nŞuanda   ").append(contract.getFsHakki()).append("   FS eklendi.");
                    return true;
                } else {
                    builder.append("Ekleme işlemi sağlanamadı.");
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            out.println("POST request not worked");
        }
        return false;
    }

    public static boolean FbSorgula(String userID) {
        try {
            String POST_PARAMS = "userProfileID=" + userID + "&ExternalSystem=1&FromDate=&ToDate=";
            URL obj = new URL(SlackBot.src + "Bonuses/ExternalBonusActivationHistory");
            HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Cookie", SlackBot.Cookie);
            httpURLConnection.setRequestProperty("User-Agent", SlackBot.UserAgent);
            // For POST only - START
            httpURLConnection.setDoOutput(true);
            OutputStream os = httpURLConnection.getOutputStream();
            os.write(POST_PARAMS.getBytes(UTF_8));
            os.flush();
            os.close();
            // For POST only - END
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                JSONArray json = JSONArray.fromObject(response.toString());
                SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
                for (Object object : json) {
                    JSONObject item = JSONObject.fromObject(object);
                    long CreateTime = Long.parseLong(item.getString("CreateTime").replace("/Date(", "").replace(")/", ""));
                    if (format.format(new Date()).equals(format.format(CreateTime))) {
                        return true;
                    }
                }

            } else {
                out.println("POST request not worked");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean FsEkleYeni(FreeSpinContract contract) throws IOException {
        String POST_PARAMS = "ExternalSystem=29&BonusCode=&Amount=" + contract.getFsTutar() + "&Spins=" + contract.getFsHakki() + "&startDate=&expirationDate=" + date(7) + "&Users=" + contract.getUserID() + "&gameList=vs10bbsplxmas&BetTry=&BetUsd=&BetEur=&roundOptions=&charismaticCoinCount_TRY=1&charismaticCoinCount_IRR=1";
        URL obj = new URL(SlackBot.src + "Bonuses/AddExternalBonusActivationRequests");
        HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Cookie", SlackBot.Cookie);
        httpURLConnection.setRequestProperty("User-Agent", SlackBot.UserAgent);
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
            try {
                String value = JSONObject.fromObject(response.toString()).getString("errorMessage");
                if (value.equals("OK")) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            out.println("POST request not worked");
        }
        return false;
    }

    public static void FsSorgulaYeni(FreeSpinContract contract) throws IOException {
        String POST_PARAMS = "sEcho=8&iColumns=10&sColumns=%2C%2C%2C%2C%2C%2C%2C%2C%2C&iDisplayStart=0&iDisplayLength=1000&mDataProp_0=ID&mDataProp_1=UserProfileID&mDataProp_2=ExternalSystemName&mDataProp_3=BonusCode&mDataProp_4=ExpirationDateStr&mDataProp_5=IsProcessed&mDataProp_6=ActivationStatus&mDataProp_7=Comment&mDataProp_8=CreateTimeStr&mDataProp_9=LastModifyTimeStr&dateFrom=" + date(0) + "&dateTo=" + date(1) + "&ExternalSystem=29&userProfileID=" + contract.getUserID();
        URL obj = new URL(SlackBot.src + "Bonuses/ExternalBonusActivationHistory");
        HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Cookie", SlackBot.Cookie);
        httpURLConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        httpURLConnection.setRequestProperty("User-Agent", SlackBot.UserAgent);
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
                SlackBot.CookieRefresher(httpURLConnection.getHeaderField("Set-Cookie"));
            }
            StringBuilder degerler = new StringBuilder();
            try {
                JSONArray jsonArray = JSONObject.fromObject(response.toString()).getJSONObject("data").getJSONObject("Data").getJSONArray("aaData");
                for (Object obje : jsonArray) {
                    JSONObject item = JSONObject.fromObject(obje);
                    int Spins = item.getInt("Spins");
                    int Amount = item.getInt("Amount");
                    long LastModifyTime = Long.parseLong(item.getString("LastModifyTime").replace("/Date(", "").replace(")/", ""));
                    String ExternalSystemName = item.getString("ExternalSystemName");
                    degerler.append("(").append(Spins).append(",").append(LastModifyTime).append(",'").append(ExternalSystemName).append("','").append(Amount).append("'),\n");
                }
                if (!degerler.toString().isBlank() || !degerler.toString().isEmpty()) {
                    degerler.append("bitisnoktasi");
                    String deger = degerler.toString().replace(",\nbitisnoktasi", "");
                    AddFreeSpin(deger);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            out.println("POST request not worked");
        }
    }

    public static void createTables() {
        try {
            conn = new ObjectHelper().getConnection();
            Statement stmt = conn.createStatement();
            File f = new File("data.db");
            if (!f.exists()) {
                Files.createFile(Paths.get("data.db"));
                Files.write(Paths.get("data.db"), Collections.singletonList(""), StandardOpenOption.WRITE);
            }
            stmt.execute("CREATE TABLE IF NOT EXISTS FreeSpin (SpinCount INTEGER,CreateDate INTEGER,Category TEXT,Game TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS Yatırım (types TEXT,amount INTEGER,dateOf INTEGER)");
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}
