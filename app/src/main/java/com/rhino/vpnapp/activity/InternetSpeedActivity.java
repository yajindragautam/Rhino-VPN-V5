package com.rhino.vpnapp.activity;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import com.rhino.vpnapp.R;
import com.rhino.vpnapp.speedtest.GetSpeedTestHostsHandler;
import com.rhino.vpnapp.speedtest.HttpDownloadTest;
import com.rhino.vpnapp.speedtest.HttpUploadTest;
import com.rhino.vpnapp.speedtest.PingTest;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/** @noinspection BusyWait*/
public class InternetSpeedActivity extends BaseAppActivity implements View.OnClickListener {

    private ImageView imgBack, imgBar;
    static int position = 0;
    static int lastPosition = 0;
    private GetSpeedTestHostsHandler getSpeedTestHostsHandler = null;
    //    private HashSet<String> tempBlackList;
    private Button btnStart;
    private TextView txtPing, txtDownload, txtUpload;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_internet_speed);
        setInsetMode(findViewById(R.id.rootView));
        init();
        initData();
        initClickListener();
        goBack();
    }

    public void init() {
        imgBack = findViewById(R.id.imgBack);
        btnStart = findViewById(R.id.btnStart);
        imgBar = findViewById(R.id.imgBar);
        txtPing = findViewById(R.id.txtPing);
        txtDownload = findViewById(R.id.txtDownload);
        txtUpload = findViewById(R.id.txtUpload);

    }

    public void initData() {

        final DecimalFormat dec = new DecimalFormat("#.##");
        btnStart.setText(getString(R.string.strCalculateSpeed));

//        tempBlackList = new HashSet<>();

        getSpeedTestHostsHandler = new GetSpeedTestHostsHandler();
        getSpeedTestHostsHandler.start();
        btnStart.setOnClickListener(v -> {
            btnStart.setEnabled(false);

            //Restart for testing if disconnected
            if (getSpeedTestHostsHandler == null) {
                getSpeedTestHostsHandler = new GetSpeedTestHostsHandler();
                getSpeedTestHostsHandler.start();
            }

            new Thread(new Runnable() {
                RotateAnimation rotate;

                @Override
                public void run() {
                    runOnUiThread(() -> btnStart.setText(getString(R.string.strServerSelection)));

                    //Get egcodes.speedtest hosts
                    int timeCount = 600; //1min
                    while (!getSpeedTestHostsHandler.isFinished()) {
                        timeCount--;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {
                        }
                        if (timeCount <= 0) {
                            runOnUiThread(() -> {
                                Toast.makeText(getApplicationContext(), "No Connection...", Toast.LENGTH_LONG).show();
                                btnStart.setEnabled(true);
                                btnStart.setTextSize(16);
                                btnStart.setText(getString(R.string.strRecalculateSpeed));
                            });
                            getSpeedTestHostsHandler = null;
                            return;
                        }
                    }

                    //Find closest server
                    HashMap<Integer, String> mapKey = getSpeedTestHostsHandler.getMapKey();
                    HashMap<Integer, List<String>> mapValue = getSpeedTestHostsHandler.getMapValue();
                    double selfLat = getSpeedTestHostsHandler.getSelfLat();
                    double selfLon = getSpeedTestHostsHandler.getSelfLon();
                    double tmp = 19349458;
//                    double dist = 0.0;
                    int findServerIndex = 0;
                    for (int index : mapKey.keySet()) {

                        Location source = new Location("Source");
                        source.setLatitude(selfLat);
                        source.setLongitude(selfLon);

                        List<String> ls = mapValue.get(index);
                        Location dest = new Location("Dest");
                        dest.setLatitude(Double.parseDouble(Objects.requireNonNull(ls).get(0)));
                        dest.setLongitude(Double.parseDouble(ls.get(1)));

                        double distance = source.distanceTo(dest);
                        if (tmp > distance) {
                            tmp = distance;
//                            dist = distance;
                            findServerIndex = index;
                        }
                    }
                    String testAddr = Objects.requireNonNull(mapKey.get(findServerIndex)).replace("http://", "https://");
                    final List<String> info = mapValue.get(findServerIndex);
//                    final double distance = dist;

                    if (info == null) {
                        runOnUiThread(() -> {
                            btnStart.setTextSize(12);
                            btnStart.setText(getString(R.string.strNoHost));
                        });
                        return;
                    }

                    runOnUiThread(() -> {
                        btnStart.setTextSize(13);
                        btnStart.setText(String.format(getString(R.string.strHostLocation), info.get(2)));
//                        btnStart.setText(String.format(getString(R.string.strHostLocation), info.get(2), new DecimalFormat("#.##").format(distance / 1000)));
                    });

                    //Init Ping graphic
                    final LinearLayout chartPing = findViewById(R.id.chartPing);
                    XYSeriesRenderer pingRenderer = new XYSeriesRenderer();
                    XYSeriesRenderer.FillOutsideLine pingFill = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
                    pingFill.setColor(getResources().getColor(R.color.colorGraphLine));
                    pingRenderer.addFillOutsideLine(pingFill);
                    pingRenderer.setDisplayChartValues(false);
                    pingRenderer.setShowLegendItem(false);
                    pingRenderer.setColor(getResources().getColor(R.color.colorGraphLine));
                    pingRenderer.setLineWidth(5);
                    final XYMultipleSeriesRenderer multiPingRenderer = new XYMultipleSeriesRenderer();
                    multiPingRenderer.setXLabels(0);
                    multiPingRenderer.setYLabels(0);
                    multiPingRenderer.setZoomEnabled(false);
                    multiPingRenderer.setXAxisColor(getResources().getColor(R.color.colorGraphXAxis));
                    multiPingRenderer.setYAxisColor(getResources().getColor(R.color.colorGraphYAxis));
                    multiPingRenderer.setPanEnabled(false, false);
                    multiPingRenderer.setZoomButtonsVisible(false);
                    multiPingRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
                    multiPingRenderer.addSeriesRenderer(pingRenderer);

                    //Init Download graphic
                    final LinearLayout chartDownload = findViewById(R.id.chartDownload);
                    XYSeriesRenderer downloadRenderer = new XYSeriesRenderer();
                    XYSeriesRenderer.FillOutsideLine downloadFill = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
                    downloadFill.setColor(getResources().getColor(R.color.colorGraphLine));
                    downloadRenderer.addFillOutsideLine(downloadFill);
                    downloadRenderer.setDisplayChartValues(false);
                    downloadRenderer.setColor(getResources().getColor(R.color.colorGraphLine));
                    downloadRenderer.setShowLegendItem(false);
                    downloadRenderer.setLineWidth(5);
                    final XYMultipleSeriesRenderer multiDownloadRenderer = new XYMultipleSeriesRenderer();
                    multiDownloadRenderer.setXLabels(0);
                    multiDownloadRenderer.setYLabels(0);
                    multiDownloadRenderer.setZoomEnabled(false);
                    multiDownloadRenderer.setXAxisColor(getResources().getColor(R.color.colorGraphXAxis));
                    multiDownloadRenderer.setYAxisColor(getResources().getColor(R.color.colorGraphYAxis));
                    multiDownloadRenderer.setPanEnabled(false, false);
                    multiDownloadRenderer.setZoomButtonsVisible(false);
                    multiDownloadRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
                    multiDownloadRenderer.addSeriesRenderer(downloadRenderer);

                    //Init Upload graphic
                    final LinearLayout chartUpload = findViewById(R.id.chartUpload);
                    XYSeriesRenderer uploadRenderer = new XYSeriesRenderer();
                    XYSeriesRenderer.FillOutsideLine uploadFill = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
                    uploadFill.setColor(getResources().getColor(R.color.colorGraphLine));
                    uploadRenderer.addFillOutsideLine(uploadFill);
                    uploadRenderer.setDisplayChartValues(false);
                    uploadRenderer.setColor(getResources().getColor(R.color.colorGraphLine));
                    uploadRenderer.setShowLegendItem(false);
                    uploadRenderer.setLineWidth(5);
                    final XYMultipleSeriesRenderer multiUploadRenderer = new XYMultipleSeriesRenderer();
                    multiUploadRenderer.setXLabels(0);
                    multiUploadRenderer.setYLabels(0);
                    multiUploadRenderer.setZoomEnabled(false);
                    multiUploadRenderer.setXAxisColor(getResources().getColor(R.color.colorGraphXAxis));
                    multiUploadRenderer.setYAxisColor(getResources().getColor(R.color.colorGraphYAxis));
                    multiUploadRenderer.setPanEnabled(false, false);
                    multiUploadRenderer.setZoomButtonsVisible(false);
                    multiUploadRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
                    multiUploadRenderer.addSeriesRenderer(uploadRenderer);

                    //Reset value, graphics
                    runOnUiThread(() -> {
                        txtPing.setText(getString(R.string.strStaticInternetSpeed));
                        chartPing.removeAllViews();
                        txtDownload.setText(getString(R.string.strStaticMbps));
                        chartDownload.removeAllViews();
                        txtUpload.setText(getString(R.string.strStaticMbps));
                        chartUpload.removeAllViews();
                    });
                    final List<Double> pingRateList = new ArrayList<>();
                    final List<Double> downloadRateList = new ArrayList<>();
                    final List<Double> uploadRateList = new ArrayList<>();
                    boolean pingTestStarted = false;
                    boolean pingTestFinished = false;
                    boolean downloadTestStarted = false;
                    boolean downloadTestFinished = false;
                    boolean uploadTestStarted = false;
                    boolean uploadTestFinished = false;

                    //Init Test
                    final PingTest pingTest = new PingTest(info.get(6).replace(":8080", ""), 3);
                    final HttpDownloadTest downloadTest = new HttpDownloadTest(testAddr.replace(testAddr.split("/")[testAddr.split("/").length - 1], ""));
                    final HttpUploadTest uploadTest = new HttpUploadTest(testAddr);


                    //Tests
                    while (true) {
                        if (!pingTestStarted) {
                            pingTest.start();
                            pingTestStarted = true;
                        }
                        if (pingTestFinished && !downloadTestStarted) {
                            downloadTest.start();
                            downloadTestStarted = true;
                        }
                        if (downloadTestFinished && !uploadTestStarted) {
                            uploadTest.start();
                            uploadTestStarted = true;
                        }


                        //Ping Test
                        if (pingTestFinished) {
                            //Failure
                            if (pingTest.getAvgRtt() == 0) {
                                System.out.println("Ping error...");
                            } else {
                                //Success
                                runOnUiThread(() -> txtPing.setText(dec.format(pingTest.getAvgRtt()) + getString(R.string.lblMS)));
                            }
                        } else {
                            pingRateList.add(pingTest.getInstantRtt());

                            runOnUiThread(() -> txtPing.setText(dec.format(pingTest.getInstantRtt()) + getString(R.string.lblMS)));

                            //Update chart
                            runOnUiThread(() -> {
                                // Creating an  XYSeries for Income
                                XYSeries pingSeries = new XYSeries("");
                                pingSeries.setTitle("");

                                int count = 0;
                                List<Double> tmpLs = new ArrayList<>(pingRateList);
                                for (Double val : tmpLs) {
                                    pingSeries.add(count++, val);
                                }

                                XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                                dataset.addSeries(pingSeries);

                                GraphicalView chartView = ChartFactory.getLineChartView(getBaseContext(), dataset, multiPingRenderer);
                                chartPing.addView(chartView, 0);

                            });
                        }


                        //Download Test
                        if (pingTestFinished) {
                            if (downloadTestFinished) {
                                //Failure
                                if (downloadTest.getFinalDownloadRate() == 0) {
                                    System.out.println("Download error...");
                                } else {
                                    //Success
                                    runOnUiThread(() -> txtDownload.setText(dec.format(downloadTest.getFinalDownloadRate()) + getString(R.string.lblMbps)));
                                }
                            } else {
                                //Calc position
                                double downloadRate = downloadTest.getInstantDownloadRate();
                                downloadRateList.add(downloadRate);
                                position = getPositionByRate(downloadRate);

                                runOnUiThread(() -> {
                                    rotate = new RotateAnimation(lastPosition, position, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                                    rotate.setInterpolator(new LinearInterpolator());
                                    rotate.setDuration(100);
                                    imgBar.startAnimation(rotate);
                                    txtDownload.setText(dec.format(downloadTest.getInstantDownloadRate()) + getString(R.string.lblMbps));

                                });
                                lastPosition = position;

                                //Update chart
                                runOnUiThread(() -> {
                                    // Creating an  XYSeries for Income
                                    XYSeries downloadSeries = new XYSeries("");
                                    downloadSeries.setTitle("");

                                    List<Double> tmpLs = new ArrayList<>(downloadRateList);
                                    int count = 0;
                                    for (Double val : tmpLs) {
                                        downloadSeries.add(count++, val);
                                    }

                                    XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                                    dataset.addSeries(downloadSeries);

                                    GraphicalView chartView = ChartFactory.getLineChartView(getBaseContext(), dataset, multiDownloadRenderer);
                                    chartDownload.addView(chartView, 0);
                                });

                            }
                        }


                        //Upload Test
                        if (downloadTestFinished) {
                            if (uploadTestFinished) {
                                //Failure
                                if (uploadTest.getFinalUploadRate() == 0) {
                                    System.out.println("Upload error...");
                                } else {
                                    //Success
                                    runOnUiThread(() -> txtUpload.setText(dec.format(uploadTest.getFinalUploadRate()) + getString(R.string.lblMbps)));
                                }
                            } else {
                                //Calc position
                                double uploadRate = uploadTest.getInstantUploadRate();
                                uploadRateList.add(uploadRate);
                                position = getPositionByRate(uploadRate);

                                runOnUiThread(() -> {
                                    rotate = new RotateAnimation(lastPosition, position, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                                    rotate.setInterpolator(new LinearInterpolator());
                                    rotate.setDuration(100);
                                    imgBar.startAnimation(rotate);
                                    txtUpload.setText(dec.format(uploadTest.getInstantUploadRate()) + getString(R.string.lblMbps));
                                });
                                lastPosition = position;

                                //Update chart
                                runOnUiThread(() -> {
                                    // Creating an  XYSeries for Income
                                    XYSeries uploadSeries = new XYSeries("");
                                    uploadSeries.setTitle("");

                                    int count = 0;
                                    List<Double> tmpLs = new ArrayList<>(uploadRateList);
                                    for (Double val : tmpLs) {
                                        if (count == 0) {
                                            val = 0.0;
                                        }
                                        uploadSeries.add(count++, val);
                                    }

                                    XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                                    dataset.addSeries(uploadSeries);

                                    GraphicalView chartView = ChartFactory.getLineChartView(getBaseContext(), dataset, multiUploadRenderer);
                                    chartUpload.addView(chartView, 0);
                                });

                            }
                        }

                        //Test bitti
                        if (pingTestFinished && downloadTestFinished && uploadTest.isFinished()) {
                            break;
                        }

                        if (pingTest.isFinished()) {
                            pingTestFinished = true;
                        }
                        if (downloadTest.isFinished()) {
                            downloadTestFinished = true;
                        }
                        if (uploadTest.isFinished()) {
                            uploadTestFinished = true;
                        }

                        if (!pingTestFinished) {
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException ignored) {
                            }
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ignored) {
                            }
                        }
                    }

                    //Thread bitiminde button yeniden aktif ediliyor
                    runOnUiThread(() -> {
                        btnStart.setEnabled(true);
                        btnStart.setTextSize(16);
                        btnStart.setText(getString(R.string.strRecalculateSpeed));
                    });


                }
            }).start();
        });
    }

    public int getPositionByRate(double rate) {
        if (rate <= 1) {
            return (int) (rate * 30);

        } else if (rate <= 10) {
            return (int) (rate * 6) + 30;

        } else if (rate <= 30) {
            return (int) ((rate - 10) * 3) + 90;

        } else if (rate <= 50) {
            return (int) ((rate - 30) * 1.5) + 150;

        } else if (rate <= 100) {
            return (int) ((rate - 50) * 1.2) + 180;
        }

        return 0;
    }

    public void initClickListener() {
        imgBack.setOnClickListener(this);
    }

    private void goBack() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.imgBack) {
            getOnBackPressedDispatcher().onBackPressed();
        }
    }
}