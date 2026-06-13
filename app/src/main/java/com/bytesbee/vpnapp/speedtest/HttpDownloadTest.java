package com.bytesbee.vpnapp.speedtest;

import com.bytesbee.vpnapp.utils.Utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * @author erdigurbuz
 */
public class HttpDownloadTest extends Thread {

    public final String fileURL;
    long startTime = 0;
    long endTime = 0;
    double downloadElapsedTime = 0;
    int downloadedByte = 0;
    double finalDownloadRate = 0.0;
    boolean finished = false;
    double instantDownloadRate = 0;
    final int timeout = 8;

    HttpsURLConnection httpsConn = null;

    public HttpDownloadTest(String fileURL) {
        this.fileURL = fileURL;
    }

    private double round(double value) {
        BigDecimal bd;
        try {
            bd = new BigDecimal(value);
        } catch (Exception ex) {
            return 0.0;
        }
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public double getInstantDownloadRate() {
        return instantDownloadRate;
    }

    public void setInstantDownloadRate(int downloadedByte, double elapsedTime) {

        if (downloadedByte >= 0) {
            this.instantDownloadRate = round(((double) (downloadedByte * 8) / (1000 * 1000)) / elapsedTime);
        } else {
            this.instantDownloadRate = 0.0;
        }
    }

    public double getFinalDownloadRate() {
        return round(finalDownloadRate);
    }

    public boolean isFinished() {
        return finished;
    }

    @Override
    public void run() {
        URL url;
        downloadedByte = 0;
        int responseCode;

        List<String> fileUrls = new ArrayList<>();
        fileUrls.add(fileURL + "random4000x4000.jpg");
        fileUrls.add(fileURL + "random3000x3000.jpg");

        startTime = System.currentTimeMillis();

        outer:
        for (String link : fileUrls) {
            try {

                url = new URL(link);
                Reader inputString = new StringReader(link);
                BufferedReader reader = new BufferedReader(inputString);
                final String strHostName = reader.readLine();
                final String strVerifyHost = strHostName.split("://")[1].split(":")[0];

                httpsConn = (HttpsURLConnection) url.openConnection();
                httpsConn.setSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
//                httpsConn.setHostnameVerifier((hostname, session) -> true);
                httpsConn.setHostnameVerifier((hostname, session) -> hostname.equals(strVerifyHost));
                httpsConn.connect();
                responseCode = httpsConn.getResponseCode();
            } catch (Exception ex) {
                Utils.getErrors(ex);
                break;
            }

            try {
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    byte[] buffer = new byte[10240];

                    InputStream inputStream = httpsConn.getInputStream();
                    int len;

                    while ((len = inputStream.read(buffer)) != -1) {
                        downloadedByte += len;
                        endTime = System.currentTimeMillis();
                        downloadElapsedTime = (endTime - startTime) / 1000.0;
                        setInstantDownloadRate(downloadedByte, downloadElapsedTime);
                        if (downloadElapsedTime >= timeout) {
                            break outer;
                        }
                    }

                    inputStream.close();
                    httpsConn.disconnect();
                } else {
                    System.out.println("Link not found...");
                }
            } catch (Exception ex) {
                Utils.getErrors(ex);
            }
        }

        endTime = System.currentTimeMillis();
        downloadElapsedTime = (endTime - startTime) / 1000.0;
        finalDownloadRate = ((downloadedByte * 8) / (1000 * 1000.0)) / downloadElapsedTime;

        finished = true;
    }

}
