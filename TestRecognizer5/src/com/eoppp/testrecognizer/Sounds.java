package com.eoppp.testrecognizer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.SoundPool;

public class Sounds {
	private static Context context;
	private static MediaPlayer mediaPlayer;
	private static SoundPool soundPool;
	private static int sidSE;

	public static void init(final Context context) {
		Sounds.context = context;
		soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
		sidSE = soundPool.load(context, R.raw.c7, 1);
	}

	public static void playSE() {
		soundPool.play(sidSE, 1.0F, 1.0F, 0, 0, 1.0F);

	}

	public static void playBGM() {
		initBGM(R.raw.c7);
	}

	public static void pauseBGM() {
		if (mediaPlayer != null)
			mediaPlayer.pause();
	}

	public static void stopBGM() {
		if (mediaPlayer != null)
			mediaPlayer.stop();
	}

	private static synchronized void initBGM(final int resourceId) {
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}

		mediaPlayer = MediaPlayer.create(context, resourceId);
		mediaPlayer.setLooping(false);
		mediaPlayer.setVolume(0.1F, 0.1F);
		mediaPlayer.start();
		mediaPlayer.setOnCompletionListener(new OnCompletionListener(){
			@Override
			public void onCompletion(MediaPlayer mediaPlayer){
				mediaPlayer.release();
			}
		});
	}
}
