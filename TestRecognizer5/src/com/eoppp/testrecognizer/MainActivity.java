package com.eoppp.testrecognizer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eoppp.testrecognizer.ShakeListener.OnShakeListener;

public class MainActivity extends Activity implements OnClickListener,
		OnInitListener, Listener<JSONObject>, ErrorListener {
	private static final int REQUEST_CODE = 0;
	private static final int MAX_RESULT = 1;

	String resStr = "";
	String start = "こんばんは";

	private TextToSpeech tts;// tts関連

	private static final String URANAI_URL_PREFIX = "http://api.jugemkey.jp/api/horoscope/free";// 占いAPI
	private static final String OTENKI_URL_PREFIX = "http://weather.livedoor.com/forecast/webservice/json/v1?city=130010";// お天気API

	private static final int[] DIFFERENCE_THRESHOLD_LIST = { 700, 500, 300 };
	private SensorManager mSensorManager;
	private ShakeListener mShakeListener;

	private MediaPlayer mp;

	int i = 0;

	private void startSearch(final String url) {
		System.out.println("url:" + url);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tts = new TextToSpeech(getApplicationContext(), this);// tts関連

		Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(this);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mShakeListener = new ShakeListener();

	}

	/**
	 * ボタンが押されたら、音声認識をする
	 */
	public void onClick(View v) {
		try {
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
					RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
					Locale.JAPAN.toString());
			intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
					getString(R.string.Recognize));
			intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULT);
			startActivityForResult(intent, REQUEST_CODE);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG)
					.show();
		}
	}

	/**
	 * 音声認識された後の処理
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
			ArrayList<String> results = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			for (int i = 0; i < results.size(); i++) {
				resStr += results.get(i);
			}
			Toast.makeText(this, resStr, Toast.LENGTH_LONG).show();
			if (resStr.contains("おやすみ")) {
				Toast.makeText(this, "おやすみなさい、いい夢を", Toast.LENGTH_LONG).show();
				speechText("おやすみなさい、いい夢を");
				moveTaskToBack(true);// アプリ終了
			} else if (resStr.contains("占い")) {
				speechText("占います。");
				stop(1000);
				startvolley(URANAI_URL_PREFIX);
			} else if (resStr.contains("天気")) {
				speechText("お天気です。");
				stop(1000);
				startvolley(OTENKI_URL_PREFIX);
			} else if (resStr.contains("リラックス")) {
				speechText("雨のおと。");
				// Sounds.playBGM();
				try {
					mp = MediaPlayer.create(this, R.raw.rain);
					mp.prepare();
				} catch (Exception e) {
				}
				mp.start();
				resStr = "";
			} else if (resStr.contains("停止")) {
				mp.stop();
				resStr = "";
			} else {
				resStr = "";
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void speechText(String string) {
		tts.speak(string, TextToSpeech.QUEUE_FLUSH, null);
	}

	/**
	 * tts関連
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		tts.shutdown();
		mp.release();
	}

	/**
	 * tts関連
	 */
	@Override
	public void onInit(int status) {

	}

	/**
	 * volley通信成功時の処理
	 */
	@Override
	public void onResponse(JSONObject response) {
		if (resStr.contains("占い")) {
			try {
				final JSONObject horoscope = response
						.getJSONObject("horoscope");

				// 表示に必要な情報をJSONから取り出す
				Calendar now = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
				String strDate = sdf.format(now.getTime());
				final JSONObject Entry = horoscope.getJSONArray(strDate)
						.getJSONObject(1);
				final String sign = Entry.getString("sign");
				final String money = Entry.getString("money");
				final String job = Entry.getString("job");
				final String love = Entry.getString("love");
				final String total = Entry.getString("total");
				final String item = Entry.getString("item");
				final String content = Entry.getString("content");
				String kekka = sign + "の運勢。" + "きんうんは、" + money + "。仕事運は、"
						+ job + "。恋愛運は、" + love + "。総合運は、" + total
						+ "です。ラッキーアイテムは、" + item;
				Toast.makeText(this, kekka, Toast.LENGTH_LONG).show();
				tts.speak(kekka, TextToSpeech.QUEUE_FLUSH, null);// tts関連
				stop(13500);
				tts.speak(content, TextToSpeech.QUEUE_FLUSH, null);// tts関連
			} catch (JSONException e) {
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			}
		} else if (resStr.contains("天気")) {
			try {
				final JSONObject forecasts = response.getJSONArray("forecasts")
						.getJSONObject(1);

				// 表示に必要な情報をJSONから取り出す;
				final String city = response.getString("title");
				final String telop = forecasts.getString("telop");
				final String date = forecasts.getString("date");
				final String dataLabel = forecasts.getString("dateLabel");
				final String description = response
						.getJSONObject("description").getString("text");
				// final String maxtemperature = forecasts
				// .getJSONObject("temperature").getJSONObject("max")
				// .getString("celsius");
				// final String mintemperature = forecasts
				// .getJSONObject("temperature").getJSONObject("min")
				// .getString("celsius");
				final String happyou = dataLabel + city + "は、" + telop
						+ "でしょう。";
				// 最高気温は、" + maxtemperature + "度。最低気温は、"
				// + mintemperature + "度の見込みです。";
				Toast.makeText(this, happyou, Toast.LENGTH_LONG).show();
				tts.speak(happyou, TextToSpeech.QUEUE_FLUSH, null);// tts関連
			} catch (JSONException e) {
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			}
		}
		resStr = "";
	}

	/**
	 * volley通信失敗時の処理
	 */
	@Override
	public void onErrorResponse(final VolleyError error) {
		System.out.println("失敗");
	}

	/**
	 * urlへの通信を行うメソッド
	 */
	private void startvolley(final String url) {
		final RequestQueue requestQueue = VolleyRequestHolder
				.newRequestQueue(this);
		final JsonObjectRequest request = new JsonObjectRequest(url, null,
				this, this);
		requestQueue.add(request);
	}

	/**
	 * @param time
	 *            動きを止めるメソッド
	 */
	private void stop(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// シェイクのための
	@Override
	public void onResume() {
		super.onResume();
		mShakeListener.registerListener(mSensorManager, mOnShakeListener, true);
	}

	@Override
	public void onPause() {
		super.onPause();
		mShakeListener.unregisterListener(mSensorManager);

	}

	private OnShakeListener mOnShakeListener = new OnShakeListener() {
		// @Override
		public void onShaked(int direction) {
			if ((direction & ShakeListener.DIRECTION_X) > 0
					|| (direction & ShakeListener.DIRECTION_Y) > 0
					|| (direction & ShakeListener.DIRECTION_Z) > 0) {
				i++;
				if (i > 40) {
					try {
						Intent intent = new Intent(
								RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
						intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
								RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
						intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
								Locale.JAPAN.toString());
						intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
								getString(R.string.Recognize));
						intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,
								MAX_RESULT);
						startActivityForResult(intent, REQUEST_CODE);
					} catch (ActivityNotFoundException e) {
						Toast.makeText(MainActivity.this, e.getMessage(),
								Toast.LENGTH_LONG).show();
					}
					i = 0;
				}

			}
		}
	};

}
