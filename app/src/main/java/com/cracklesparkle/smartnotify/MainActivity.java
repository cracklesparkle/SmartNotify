package com.cracklesparkle.smartnotify;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.pytorch.IValue;
import org.pytorch.Module;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

public class MainActivity extends Activity {

	private static MainActivity instance;
	private TextView notificationTextView;
	private Module model;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		instance = this;

		notificationTextView = findViewById(R.id.notification_text);

		// Check if the app has notification access
		if (!isNotificationAccessGranted()) {
			// If not, open notification access settings
			openNotificationAccessSettings();
		}

		// Load the model
		LoadModelTask loadModelTask = new LoadModelTask(this); // or getApplicationContext() if not in an Activity
		loadModelTask.execute();

		// Button to generate and play audio
		Button playButton = findViewById(R.id.play_button);
		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					generateAndPlayAudio();
				} catch (Exception e) {
					Log.e("MainActivity", "Error generating and playing audio", e);
				}
			}
		});
	}

	// Check if the app has notification access
	private boolean isNotificationAccessGranted() {
		ComponentName cn = new ComponentName(this, NotificationListener.class);
		String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
		return flat != null && flat.contains(cn.flattenToString());
	}

	// Open notification access settings
	private void openNotificationAccessSettings() {
		Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
		startActivity(intent);
	}

	// Update the UI with the last notification text
	public void updateNotificationText(String text) {
		notificationTextView.setText(text);
	}

	// Static method to get the instance of MainActivity
	public static MainActivity getInstance() {
		return instance;
	}

	// AsyncTask to download and load the model

	// AsyncTask to download and load the model
	public class LoadModelTask extends AsyncTask<Void, Void, Boolean> {

		private Module model;
		private final String MODEL_FILENAME = "v4_ru.pt";

		private Context context;

		public LoadModelTask(Context context) {
			this.context = context;
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			try {
				// Check if the model file exists in internal storage
				File modelFile = new File(context.getFilesDir(), MODEL_FILENAME);
				if (!modelFile.exists()) {
					// If the model file doesn't exist, copy it from assets to internal storage
					boolean copySuccess = copyModelFromAssets(modelFile);
					if (!copySuccess) {
						Log.e("LoadModelTask", "Failed to copy model from assets");
						return false;
					}
				}

				// Load the model
				model = Module.load(modelFile.getAbsolutePath());
				return true;
			} catch (Exception e) {
				Log.e("LoadModelTask", "Error loading model", e);
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				Log.d("LoadModelTask", "Model loaded successfully");
				// Do any post-loading tasks here, such as enabling UI components
			} else {
				Log.e("LoadModelTask", "Failed to load model");
				// Handle the case where model loading fails, e.g., display an error message
			}
		}

		private boolean copyModelFromAssets(File destinationFile) {
			try {
				AssetManager assetManager = context.getAssets();
				InputStream inputStream = assetManager.open(MODEL_FILENAME);
				FileOutputStream outputStream = new FileOutputStream(destinationFile);
				byte[] buffer = new byte[1024];
				int read;
				while ((read = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, read);
				}
				outputStream.flush();
				outputStream.close();
				inputStream.close();
				return true;
			} catch (IOException e) {
				Log.e("LoadModelTask", "Error copying model from assets", e);
				return false;
			}
		}

		public Module getModel() {
			return model;
		}
	}

	// Generate audio from text and play it
	private void generateAndPlayAudio() throws Exception {
		if (model == null) {
			Log.e("MainActivity", "Model is not loaded");
			return;
		}

		// Get text from the notification
		String text = notificationTextView.getText().toString();

		// Run inference to generate audio
		IValue output = model.forward(IValue.from(text));

		// Convert output tensor to float array
		float[] audioArray = output.toTensor().getDataAsFloatArray();

		// Encode audio array into bytes
		byte[] audioBytes = encodeAudioToBytes(audioArray);

		// Play the generated audio
		playAudio(audioBytes);
	}

	// Encode audio array into bytes
	private byte[] encodeAudioToBytes(float[] audioArray) {
		// Assuming 16-bit PCM audio format
		int numSamples = audioArray.length;
		ByteBuffer byteBuffer = ByteBuffer.allocate(numSamples * 2); // 16-bit = 2 bytes per sample
		for (int i = 0; i < numSamples; i++) {
			short sample = (short) (audioArray[i] * Short.MAX_VALUE);
			byteBuffer.putShort(sample);
		}
		return byteBuffer.array();
	}

	// Play audio from byte array
	private void playAudio(byte[] audioData) {
		// Set audio track parameters
		int sampleRate = 44100; // Example sample rate (adjust as needed)
		int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
		int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
		int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

		// Create audio track
		AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat,
				bufferSize, AudioTrack.MODE_STREAM);

		// Start playback
		audioTrack.play();

		// Write audio data to audio track
		audioTrack.write(audioData, 0, audioData.length);

		// Wait for playback to finish
		audioTrack.flush();
		audioTrack.stop();
		audioTrack.release();
	}
}