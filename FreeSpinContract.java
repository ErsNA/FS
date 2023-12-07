package dmn.FS;

public class FreeSpinContract {
    private String UserID;
    private int FsHakki;
    private int FsTutar;
    private int AlinanFs;
    private String Today;
    private String FromDate;
    private long firstDepositLong;
    private long lastDepositLong;
    private String lastTransactions;
    private double lastDeposit;
    private double secondDeposit;
    private String Rank;
    private boolean krediKarti;
    private boolean pesPeseYatirim;
    private boolean isLast;

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    public double getSecondDeposit() {
        return secondDeposit;
    }

    public void setSecondDeposit(double secondDeposit) {
        this.secondDeposit = secondDeposit;
    }

    public boolean isPesPeseYatirim() {
        return pesPeseYatirim;
    }

    public void setPesPeseYatirim(boolean pesPeseYatirim) {
        this.pesPeseYatirim = pesPeseYatirim;
    }

    public boolean isKrediKarti() {
        return krediKarti;
    }

    public void setKrediKarti(boolean krediKarti) {
        this.krediKarti = krediKarti;
    }

    public long getLastDepositLong() {
        return lastDepositLong;
    }

    public void setLastDepositLong(long lastDepositLong) {
        this.lastDepositLong = lastDepositLong;
    }

    public String getLastTransactions() {
        return lastTransactions;
    }

    public void setLastTransactions(String lastTransactions) {
        this.lastTransactions = lastTransactions;
    }

    public double getLastDeposit() {
        return lastDeposit;
    }

    public void setLastDeposit(double lastDeposit) {
        this.lastDeposit = lastDeposit;
    }

    public long getFirstDepositLong() {
        return firstDepositLong;
    }

    public void setFirstDepositLong(long firstDepositLong) {
        this.firstDepositLong = firstDepositLong;
    }



    private double yat;

    public int getFsTutar() {
        return FsTutar;
    }

    public void setFsTutar(int fsTutar) {
        FsTutar = fsTutar;
    }

    public double getYat() {
        return yat;
    }

    public void setYat(double yat) {
        this.yat = yat;
    }

    public String getUserID() {
        return UserID;
    }

    public void setUserID(String userID) {
        UserID = userID;
    }

    public int getFsHakki() {
        return FsHakki;
    }

    public void setFsHakki(int fsHakki) {
        FsHakki = fsHakki;
    }

    public int getAlinanFs() {
        return AlinanFs;
    }

    public void setAlinanFs(int alinanFs) {
        AlinanFs = alinanFs;
    }

    public String getToday() {
        return Today;
    }

    public void setToday(String today) {
        Today = today;
    }

    public String getFromDate() {
        return FromDate;
    }

    public void setFromDate(String fromDate) {
        FromDate = fromDate;
    }

    public String getRank() {
        return Rank;
    }

    public void setRank(String rank) {
        Rank = rank;
    }

    private String type;
    private double amount;
    private long date;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }


}
