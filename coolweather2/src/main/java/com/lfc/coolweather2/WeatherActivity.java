package com.lfc.coolweather2;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.lfc.coolweather2.gson.Weather;
import com.lfc.coolweather2.util.HttpUtil;
import com.lfc.coolweather2.util.Utility;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private static final String TAG = "WeatherActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        Log.d(TAG, "onCreate: ");
        init();
    }

    ScrollView weatherLayout;
    TextView txtTitleCity, titleUpdateTime, txtDegree, txtInfo, txtAqi, txtPm25;
    SwipeRefreshLayout swipeRefreshLayout;
    ImageView imgBing;
    DrawerLayout drawerLayout;
    Button btnLocate;

    void init() {
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        btnLocate = (Button) findViewById(R.id.btn_locate);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String weatherData = sharedPreferences.getString("weather_data", null);
        final String weatherId = sharedPreferences.getString("weather_id", null);

        // onCreate
        if (weatherData != null) {
            Weather weather = Utility.handleWeatherResponse(weatherData);
            showWeatherInfo(weather);
        } else {
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }

        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(sharedPreferences.getString("weather_id", null));
            }
        });

        btnLocate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        Log.d(TAG, "init: ");
    }

    @SuppressLint("SetTextI18n")
    private void showWeatherInfo(Weather weather) {
        txtTitleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        txtDegree = (TextView) findViewById(R.id.txt_degree);
        txtInfo = (TextView) findViewById(R.id.txt_weather_info);
        txtAqi = (TextView) findViewById(R.id.txt_aqi);
        txtPm25 = (TextView) findViewById(R.id.txt_ap25);
        imgBing = (ImageView) findViewById(R.id.img_bing);

        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature;
        String info = weather.now.more.info;

        txtTitleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        txtDegree.setText(degree + "℃");
        txtInfo.setText(info);
        if (weather.aqi != null) {
            txtAqi.setText(weather.aqi.city.aqi);
            txtPm25.setText(weather.aqi.city.pm25);
        } else {
            txtAqi.setText("暂无数据");
            txtPm25.setText("暂无数据");
            txtAqi.setSingleLine();
            txtPm25.setSingleLine();
        }

        weatherLayout.setVisibility(View.VISIBLE);
        Log.d(TAG, "showWeatherInfo");
        Log.d(TAG, "city = " + cityName + ", tmp = " + weather.now.temperature);
    }

    private void requestWeather(String weatherId) {
        String url = "http://guolin.tech/api/weather?cityid="
                + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.e(TAG, "on failure");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "onResponse: ");
                final String responseData = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseData);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("weather_data", responseData);
                            editor.apply();
                            showWeatherInfo(weather);
                            Log.e(TAG, "on response");
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                        Date date = new Date();
                        SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
                        String time = sdf.format(date);
                        Toast.makeText(WeatherActivity.this, time + "刷新成功", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    public void refresh(String weatherId) {
        requestWeather(weatherId);
        drawerLayout.closeDrawers();
        swipeRefreshLayout.setRefreshing(true);

    }

    boolean isAlreadyPress = false;
    long pressTime;

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else {
            if (isAlreadyPress && System.currentTimeMillis() - pressTime < 3000) {
                finish();
            } else {
                Toast.makeText(WeatherActivity.this, "再按一下退出", Toast.LENGTH_SHORT).show();
                pressTime = System.currentTimeMillis();
                isAlreadyPress = true;
            }
        }
    }
}
