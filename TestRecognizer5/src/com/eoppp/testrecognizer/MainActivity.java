package com.eoppp.testrecognizer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.TwitterException;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.media.ImageUpload;
import twitter4j.media.ImageUploadFactory;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eoppp.testrecognizer.ShakeListener.OnShakeListener;

public class MainActivity extends Activity implements OnInitListener,
		Listener<JSONObject>, ErrorListener {
	private static final int REQUEST_CODE = 0;
	private static final int MAX_RESULT = 1;
	private boolean machi = true;// 指示待ち状態

	String resStr = "";
	String start = "こんばんは";
	// tts関連
	private TextToSpeech tts;
	// 占いAPI
	private static final String URANAI_URL_PREFIX = "http://api.jugemkey.jp/api/horoscope/free";
	private int sei = 1;
	// お天気API
	private static String OTENKI_URL_PREFIX = "http://weather.livedoor.com/forecast/webservice/json/v1?city=";
	private static String bas = "130010";
	private static String newotenki;
	// シェイク用
	private SensorManager mSensorManager;
	private ShakeListener mShakeListener;
	int i = 0;
	// 音楽再生用
	private MediaPlayer mp;
	// Twitter用
	public static final String PREF_NAME = "access_token";
	public static final String TOKEN = "token";
	public static final String TOKEN_SECRET = "token_secret";

	private String token;
	private String tokenSecret;

	// private void startSearch(final String url) {
	// System.out.println("url:" + url);
	// }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tts = new TextToSpeech(getApplicationContext(), this);// tts関連
		// Button button = (Button) findViewById(R.id.button1);
		// button.setOnClickListener(this);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mShakeListener = new ShakeListener();
		mp = MediaPlayer.create(this, R.raw.rain);
		try {
			mp.prepare();
		} catch (Exception e) {
		}

		// Twitter用
		SharedPreferences preferences = getSharedPreferences(PREF_NAME,
				MODE_PRIVATE);
		token = preferences.getString(TOKEN, null);
		tokenSecret = preferences.getString(TOKEN_SECRET, null);

		if (token == null || tokenSecret == null) {
			Intent intent = new Intent(this, OAuthActivity.class);
			startActivity(intent);
			finish();
		}
	}

	/**
	 * ボタンが押されたら、音声認識をする
	 */
	// public void onClick(View v) {
	// try {
	// Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
	// intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
	// RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
	// intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
	// Locale.JAPAN.toString());
	// intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
	// getString(R.string.Recognize));
	// intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULT);
	// startActivityForResult(intent, REQUEST_CODE);
	// } catch (ActivityNotFoundException e) {
	// Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG)
	// .show();
	// }
	// }

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
			if (resStr.equals("おやすみ")) {
				Toast.makeText(this, "おやすみなさい、いい夢を", Toast.LENGTH_LONG).show();
				speechText("おやすみなさい、いい夢を");
				resStr = "";
				moveTaskToBack(true);// アプリ終了
			} else if (resStr.contains("占い")) {
				speechText("占います。");
				seizaint(resStr);
				stop(1000);
				startvolley(URANAI_URL_PREFIX);
			} else if (resStr.contains("天気")) {
				speechText("お天気です。");
				basint(resStr);
				newotenki = OTENKI_URL_PREFIX + bas;
				stop(1000);
				startvolley(newotenki);
			} else if (resStr.contains("リラックス")) {
				speechText("雨のおと。");
				// Sounds.playBGM();
				mp.reset();
				mp = MediaPlayer.create(this, R.raw.rain);
				try {
					mp.prepare();
				} catch (Exception e) {
				}
				mp.start();
				resStr = "";
			} else if (resStr.contains("停止") || resStr.contains("stop")) {
				mp.stop();
				try {
					mp.prepare();
				} catch (Exception e) {
				}
				resStr = "";
			} else if (resStr.contains("ツイッタ") || resStr.contains("ついった")) {
				resStr = resStr.replace("ツイッター", "");
				doTweet(resStr);
				resStr = "";
			} else {
				resStr = "";
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
		machi = true;// 待ち状態を解除
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
						.getJSONObject(sei);
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
						+ "でしょう。" + description;
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
				if (i > 30 && machi == true) {
					speechText("なあに？");
					machi = false;// 指示待ち状態を解除
					stop(100);
					if (mp.isPlaying()) {
						mp.setVolume(0.3F, 0.3F);
					}
					ninshiki();
					i = 0;
				}

			}
		}
	};

	private void doTweet(String tweet) {
		new TweetTask().execute(tweet);
	}

	public class TweetTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.setOAuthConsumerKey(getString(R.string.consumer_key));
			builder.setOAuthConsumerSecret(getString(R.string.consumer_secret));
			builder.setOAuthAccessToken(token);
			builder.setOAuthAccessTokenSecret(tokenSecret);
			// ここでMediaProviderをTwitterにする
			builder.setMediaProvider("TWITTER");

			Configuration conf = builder.build();

			ImageUpload imageUpload = new ImageUploadFactory(conf)
					.getInstance();

			String tweet = params[0];

			try {
				imageUpload.upload(null, tweet);
			} catch (TwitterException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			speechText("つぶやいたよ");
		}
	}

	private void showShortToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	private void ninshiki() {
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

	public void seizaint(String seiza) {
		if (seiza.contains("牡羊") || seiza.contains("おひつじ")) {
			sei = 0;
		} else if (seiza.contains("牡牛") || seiza.contains("おうし")) {
			sei = 1;
		} else if (seiza.contains("双子") || seiza.contains("ふたご")) {
			sei = 2;
		} else if (seiza.contains("蟹") || seiza.contains("かに")) {
			sei = 3;
		} else if (seiza.contains("獅子") || seiza.contains("しし")) {
			sei = 4;
		} else if (seiza.contains("乙女") || seiza.contains("おとめ")) {
			sei = 5;
		} else if (seiza.contains("天秤") || seiza.contains("てんびん")) {
			sei = 6;
		} else if (seiza.contains("蠍") || seiza.contains("さそり")) {
			sei = 7;
		} else if (seiza.contains("射手") || seiza.contains("いて")) {
			sei = 8;
		} else if (seiza.contains("山羊") || seiza.contains("やぎ")) {
			sei = 9;
		} else if (seiza.contains("水瓶") || seiza.contains("みずがめ")) {
			sei = 10;
		} else if (seiza.contains("魚") || seiza.contains("うお")) {
			sei = 11;
		}
	}
	
	public void basint(String basho){
		if(basho.contains("稚内")){bas = "011000";}
		else if(basho.contains("旭川")){bas = "012010";}
		else if(basho.contains("留萌")){bas = "012020";}
		else if(basho.contains("網走")){bas = "013010";}
		else if(basho.contains("北見")){bas = "013020";}
		else if(basho.contains("紋別")){bas = "013030";}
		else if(basho.contains("根室")){bas = "014010";}
		else if(basho.contains("釧路")){bas = "014020";}
		else if(basho.contains("帯広")){bas = "014030";}
		else if(basho.contains("室蘭")){bas = "015010";}
		else if(basho.contains("浦河")){bas = "015020";}
		else if(basho.contains("札幌")){bas = "016010";}
		else if(basho.contains("岩見沢")){bas = "016020";}
		else if(basho.contains("倶知安")){bas = "016030";}
		else if(basho.contains("函館")){bas = "017010";}
		else if(basho.contains("江差")){bas = "017020";}
		else if(basho.contains("青森")){bas = "020010";}
		else if(basho.contains("むつ")){bas = "020020";}
		else if(basho.contains("八戸")){bas = "020030";}
		else if(basho.contains("盛岡")){bas = "030010";}
		else if(basho.contains("宮古")){bas = "030020";}
		else if(basho.contains("大船渡")){bas = "030030";}
		else if(basho.contains("仙台")){bas = "040010";}
		else if(basho.contains("白石")){bas = "040020";}
		else if(basho.contains("秋田")){bas = "050010";}
		else if(basho.contains("横手")){bas = "050020";}
		else if(basho.contains("山形")){bas = "060010";}
		else if(basho.contains("米沢")){bas = "060020";}
		else if(basho.contains("酒田")){bas = "060030";}
		else if(basho.contains("新庄")){bas = "060040";}
		else if(basho.contains("福島")){bas = "070010";}
		else if(basho.contains("小名浜")){bas = "070020";}
		else if(basho.contains("若松")){bas = "070030";}
		else if(basho.contains("水戸")){bas = "080010";}
		else if(basho.contains("土浦")){bas = "080020";}
		else if(basho.contains("宇都宮")){bas = "090010";}
		else if(basho.contains("大田原")){bas = "090020";}
		else if(basho.contains("前橋")){bas = "100010";}
		else if(basho.contains("みなかみ")){bas = "100020";}
		else if(basho.contains("さいたま")){bas = "110010";}
		else if(basho.contains("熊谷")){bas = "110020";}
		else if(basho.contains("秩父")){bas = "110030";}
		else if(basho.contains("千葉")){bas = "120010";}
		else if(basho.contains("銚子")){bas = "120020";}
		else if(basho.contains("館山")){bas = "120030";}
		else if(basho.contains("東京")){bas = "130010";}
		else if(basho.contains("大島")){bas = "130020";}
		else if(basho.contains("八丈島")){bas = "130030";}
		else if(basho.contains("父島")){bas = "130040";}
		else if(basho.contains("横浜")){bas = "140010";}
		else if(basho.contains("小田原")){bas = "140020";}
		else if(basho.contains("新潟")){bas = "150010";}
		else if(basho.contains("長岡")){bas = "150020";}
		else if(basho.contains("高田")){bas = "150030";}
		else if(basho.contains("相川")){bas = "150040";}
		else if(basho.contains("富山")){bas = "160010";}
		else if(basho.contains("伏木")){bas = "160020";}
		else if(basho.contains("金沢")){bas = "170010";}
		else if(basho.contains("輪島")){bas = "170020";}
		else if(basho.contains("福井")){bas = "180010";}
		else if(basho.contains("敦賀")){bas = "180020";}
		else if(basho.contains("甲府")){bas = "190010";}
		else if(basho.contains("河口湖")){bas = "190020";}
		else if(basho.contains("長野")){bas = "200010";}
		else if(basho.contains("松本")){bas = "200020";}
		else if(basho.contains("飯田")){bas = "200030";}
		else if(basho.contains("岐阜")){bas = "210010";}
		else if(basho.contains("高山")){bas = "210020";}
		else if(basho.contains("静岡")){bas = "220010";}
		else if(basho.contains("網代")){bas = "220020";}
		else if(basho.contains("三島")){bas = "220030";}
		else if(basho.contains("浜松")){bas = "220040";}
		else if(basho.contains("名古屋")){bas = "230010";}
		else if(basho.contains("豊橋")){bas = "230020";}
		else if(basho.contains("津")){bas = "240010";}
		else if(basho.contains("尾鷲")){bas = "240020";}
		else if(basho.contains("大津")){bas = "250010";}
		else if(basho.contains("彦根")){bas = "250020";}
		else if(basho.contains("京都")){bas = "260010";}
		else if(basho.contains("舞鶴")){bas = "260020";}
		else if(basho.contains("大阪")){bas = "270000";}
		else if(basho.contains("神戸")){bas = "280010";}
		else if(basho.contains("豊岡")){bas = "280020";}
		else if(basho.contains("奈良")){bas = "290010";}
		else if(basho.contains("風屋")){bas = "290020";}
		else if(basho.contains("和歌山")){bas = "300010";}
		else if(basho.contains("潮岬")){bas = "300020";}
		else if(basho.contains("鳥取")){bas = "310010";}
		else if(basho.contains("米子")){bas = "310020";}
		else if(basho.contains("松江")){bas = "320010";}
		else if(basho.contains("浜田")){bas = "320020";}
		else if(basho.contains("西郷")){bas = "320030";}
		else if(basho.contains("岡山")){bas = "330010";}
		else if(basho.contains("津山")){bas = "330020";}
		else if(basho.contains("広島")){bas = "340010";}
		else if(basho.contains("庄原")){bas = "340020";}
		else if(basho.contains("下関")){bas = "350010";}
		else if(basho.contains("山口")){bas = "350020";}
		else if(basho.contains("柳井")){bas = "350030";}
		else if(basho.contains("萩")){bas = "350040";}
		else if(basho.contains("徳島")){bas = "360010";}
		else if(basho.contains("日和佐")){bas = "360020";}
		else if(basho.contains("高松")){bas = "370000";}
		else if(basho.contains("松山")){bas = "380010";}
		else if(basho.contains("新居浜")){bas = "380020";}
		else if(basho.contains("宇和島")){bas = "380030";}
		else if(basho.contains("高知")){bas = "390010";}
		else if(basho.contains("室戸岬")){bas = "390020";}
		else if(basho.contains("清水")){bas = "390030";}
		else if(basho.contains("福岡")){bas = "400010";}
		else if(basho.contains("八幡")){bas = "400020";}
		else if(basho.contains("飯塚")){bas = "400030";}
		else if(basho.contains("久留米")){bas = "400040";}
		else if(basho.contains("佐賀")){bas = "410010";}
		else if(basho.contains("伊万里")){bas = "410020";}
		else if(basho.contains("長崎")){bas = "420010";}
		else if(basho.contains("佐世保")){bas = "420020";}
		else if(basho.contains("厳原")){bas = "420030";}
		else if(basho.contains("福江")){bas = "420040";}
		else if(basho.contains("熊本")){bas = "430010";}
		else if(basho.contains("阿蘇乙姫")){bas = "430020";}
		else if(basho.contains("牛深")){bas = "430030";}
		else if(basho.contains("人吉")){bas = "430040";}
		else if(basho.contains("大分")){bas = "440010";}
		else if(basho.contains("中津")){bas = "440020";}
		else if(basho.contains("日田")){bas = "440030";}
		else if(basho.contains("佐伯")){bas = "440040";}
		else if(basho.contains("宮崎")){bas = "450010";}
		else if(basho.contains("延岡")){bas = "450020";}
		else if(basho.contains("都城")){bas = "450030";}
		else if(basho.contains("高千穂")){bas = "450040";}
		else if(basho.contains("鹿児島")){bas = "460010";}
		else if(basho.contains("鹿屋")){bas = "460020";}
		else if(basho.contains("種子島")){bas = "460030";}
		else if(basho.contains("名瀬")){bas = "460040";}
		else if(basho.contains("那覇")){bas = "471010";}
		else if(basho.contains("名護")){bas = "471020";}
		else if(basho.contains("久米島")){bas = "471030";}
		else if(basho.contains("南大東")){bas = "472000";}
		else if(basho.contains("宮古島")){bas = "473000";}
		else if(basho.contains("石垣島")){bas = "474010";}
		else if(basho.contains("与那国島")){bas = "474020";}
	}
}
