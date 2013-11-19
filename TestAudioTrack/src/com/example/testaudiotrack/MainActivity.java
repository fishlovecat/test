package com.example.testaudiotrack;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
	private Button button;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		button = (Button) findViewById(R.id.button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(getApplicationContext(), "aa",
						Toast.LENGTH_SHORT).show();
				new Thread(new Runnable() {
					@Override
					public void run() {
						playWord("aa");
					}
				}).start();
			}
		});

	}

	public void playWord(String word) {
		String path = Environment.getExternalStorageDirectory() + "/test4.mp3";
		byte[] buf = new byte[4096];
		InputStream is = null;
		try {
			is = new FileInputStream(path);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		int bufsize = AudioTrack.getMinBufferSize(44100,// 每秒8K个点
				AudioFormat.CHANNEL_CONFIGURATION_STEREO,// 双声道
				AudioFormat.ENCODING_PCM_16BIT);// 一个采样点16比特-2个字节
		AudioTrack trackplayer = new AudioTrack(AudioManager.STREAM_MUSIC,
				44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
				AudioFormat.ENCODING_PCM_16BIT, bufsize, AudioTrack.MODE_STREAM);
//		int bufsize = AudioTrack.getMinBufferSize(22050,// 每秒8K个点
//				AudioFormat.CHANNEL_OUT_MONO,// 双声道
//				AudioFormat.ENCODING_PCM_16BIT);// 一个采样点16比特-2个字节
//		AudioTrack trackplayer = new AudioTrack(AudioManager.STREAM_MUSIC,
//				22050, AudioFormat.CHANNEL_OUT_MONO,
//				AudioFormat.ENCODING_PCM_16BIT, bufsize, AudioTrack.MODE_STREAM);
		trackplayer.play();
		int len = 0, start = 0;
		try {
			while ((len = is.read(buf)) != -1) {

				try {
					byte[] tmp = decode(path, start, len);
					start += len;
					Log.i("aa", "test" + tmp.length);
					trackplayer.write(tmp, 0, tmp.length);
				} catch (DecoderException e) {
					e.printStackTrace();
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		trackplayer.stop();
		trackplayer.release();
	}

	public static byte[] decode(String path, int startMs, int maxMs)
			throws IOException, DecoderException {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);

		float totalMs = 0;
		boolean seeking = true;

		File file = new File(path);
		InputStream inputStream = new BufferedInputStream(new FileInputStream(
				file), 8 * 1024);
		try {
			Bitstream bitstream = new Bitstream(inputStream);
			Decoder decoder = new Decoder();

			boolean done = false;
			while (!done) {
				Header frameHeader = bitstream.readFrame();
				if (frameHeader == null) {
					done = true;
				} else {
					totalMs += frameHeader.ms_per_frame();

					if (totalMs >= startMs) {
						seeking = false;
					}

					if (!seeking) {
						SampleBuffer output = (SampleBuffer) decoder
								.decodeFrame(frameHeader, bitstream);

						if (output.getSampleFrequency() != 44100
								|| output.getChannelCount() != 2) {
							// throw new DecoderException(
							// "mono or non-44100 MP3 not supported");
						}

						short[] pcm = output.getBuffer();
						for (short s : pcm) {
							outStream.write(s & 0xff);
							outStream.write((s >> 8) & 0xff);
						}
					}

					if (totalMs >= (startMs + maxMs)) {
						done = true;
					}
				}
				bitstream.closeFrame();
			}

			return outStream.toByteArray();
		} catch (BitstreamException e) {
			throw new IOException("Bitstream error: " + e);
		} catch (DecoderException e) {
			// Log.w(TAG, "Decoder error", e);
			// throw new DecoderException(e);
		} finally {
			// IOUtils.safeClose(inputStream);
		}
		return null;
	}
}
