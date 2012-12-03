package com.example.twitterapp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.MediaEntity;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;
import twitter4j.auth.AccessToken;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore.Images;
import android.support.v4.view.ViewPager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.twitterapp.R;
import com.github.ysamlan.horizontalpager.HorizontalPager;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.stablex.android.font.util.FontHelper;

public class MainActivity extends Activity {
	private static final String TOKEN = "930564715-Isej8FL9OdMHIGmtCNOE8OBEHBV94yihbBmTzpkM";
	private static final String SECRET = "Iqp9fpMTp6e7ZEh6wA0mZjqxUbkBOfQWmezLGw4UWA";
	private static final String CONSUMER_KEY = "2Rrl4sxityJCLfQJOnCIOQ";
	private static final String CONSUMER_SECRET = "TrLvOTv4R8WYloKUmy41pXJepb3udIhpL7d9C6NF4";
	private Twitter mTwitter;
	HorizontalPager realViewSwitcher;
	private ViewPager myViewPager;
	private ViewPager HelpViewPager;
	ArrayList<View> pics;
	ArrayList<Bitmap> picsBitmaps;
	ArrayList<String> imgUrls;
	ArrayList<String> ignoreUrls;
	MyViewPagerAdapter adapta;
	int viewPagerSize = 0;
	int refreshTimer = 0;
	int refreshInterval = 45;
	int coinsInserted = 0;
	int totalCoins = 0;
	int photoPrice = 200;
	int maxPix = 5;
	MediaPlayer mp;
	private UsbSerialDriver mSerialDevice;
	private UsbManager mUsbManager;
	private final ExecutorService mExecutor = Executors
			.newSingleThreadExecutor();

	private SerialInputOutputManager mSerialIoManager;

	private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {

		@Override
		public void onRunError(Exception e) {
			Log.d("derp", "Runner stopped.");
		}

		@Override
		public void onNewData(final byte[] data) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateReceivedData(data);
				}
			});
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mTwitter = new TwitterFactory().getInstance();
		mTwitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		pics = new ArrayList<View>();
		imgUrls = new ArrayList<String>();
		picsBitmaps = new ArrayList<Bitmap>();
		ignoreUrls = new ArrayList<String>();

		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		setupArduino();
		refreshTimer = refreshInterval;

		TextView t = (TextView) findViewById(R.id.priceTextView);
		t.setOnClickListener(cardSelected);
		
		displayHelpMenu(null);

		Timer mTimer = new Timer();
		TimerTask mTimerTask = new TimerTask() {
			@Override
			public void run() {
				if (refreshTimer >= 0)
					refreshTimer++;
				// Log.v("derp", "timer = " + refreshTimer);
				updatePriceView();
				if (refreshTimer == -1) {
					updateRefreshView(refreshTimer);
				} else if (refreshTimer >= refreshInterval) {
					refreshTimer = -1;
					updateRefreshView(refreshTimer);
					UpdateTwitterList updateTask = new UpdateTwitterList();
					updateTask.execute("");
				} else
					updateRefreshView(refreshInterval - refreshTimer);
			}
		};

		mp = new MediaPlayer();

		AssetFileDescriptor descriptor;
		try {
			descriptor = getAssets().openFd("latinhustle.mp3");
			mp.setDataSource(descriptor.getFileDescriptor(),
					descriptor.getStartOffset(), descriptor.getLength());
			descriptor.close();
			mp.prepare();
			mp.setLooping(true);
			mp.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mTimer.scheduleAtFixedRate(mTimerTask, 1000, 1000);

		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

	}

	public void setupArduino() {
		mSerialDevice = UsbSerialProber.acquire(mUsbManager);
		Log.d("derp", "starting mSerialDevice=" + mSerialDevice);
		if (mSerialDevice == null) {
			Log.v("derp", "No serial device.");
		} else {
			try {
				mSerialDevice.open();
				mSerialIoManager = new SerialInputOutputManager(mSerialDevice,
						mListener);
				mExecutor.submit(mSerialIoManager);
			} catch (IOException e) {
				Log.e("derp", "Error setting up device: " + e.getMessage(), e);
				try {
					mSerialDevice.close();
				} catch (IOException e2) {
					// Ignore.
				}
				mSerialDevice = null;
				return;
			}
			Log.v("derp", "Serial device: " + mSerialDevice);
		}
	}

	private void updateReceivedData(byte[] data) {
		final String message = HexDump.dumpHexString(data);
		stripHex(message);
		Log.v("derp", message);
	}

	private void getImagesForSearch(List<Tweet> tweets) throws IOException,
			JSONException {
		Log.d("derp", "building pics");
		for (Tweet status : tweets) {
			MediaEntity[] mediaList = status.getMediaEntities();
			URLEntity[] urlList = status.getURLEntities();

			if (mediaList != null) {
				String imageURL = mediaList[0].getMediaURL().toString();
				if (!imgUrls.contains(imageURL)) {
					AbsoluteLayout card = convertStatusToCard(status, imageURL);

					//convertStatusToHighDefCard(status, imageURL);

					if (card != null)
						pics.add(card);

					imgUrls.add(imageURL);
				}
			} else if (urlList.length > 0) {
				// dont have to crop instagrams.
				URL urlForPic = urlList[0].getExpandedURL();
				if (urlForPic.getHost().toString().equals("instagr.am")) {
					String instagramCallbackURL = "http://instagram.com/api/v1/oembed/?url=";
					String largeImage = instagramCallbackURL.concat(urlForPic
							.toString());
					if (!imgUrls.contains(largeImage)) {
						JSONObject json = readJsonFromUrl(largeImage);
						String imageURL = json.get("url").toString();
						AbsoluteLayout card = convertStatusToCard(status,
								imageURL);

						//convertStatusToHighDefCard(status, imageURL);

						if (card != null)
							pics.add(card);
						imgUrls.add(largeImage);


					}
				}
			}
			if (pics.size() > maxPix) {
				imgUrls.remove(imgUrls.size() - 1);
				pics.remove(imgUrls.size() - 1);
				//picsBitmaps.remove(imgUrls.size() - 1);
			}
		}
	}

	public int addPage(View view, int position) {
		if (position >= 0) {
			adapta.data.add(position, view);
			adapta.notifyDataSetChanged();
			return position;
		} else {
			return -1;
		}
	}

	@SuppressWarnings("deprecation")
	private AbsoluteLayout convertStatusToCard(Tweet status, String imageURL) {
		// final ImageView iv = new ImageView(this);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		AbsoluteLayout card = (AbsoluteLayout) inflater.inflate(R.layout.card,
				null);

		setContentView(R.layout.card);
		String strippedTweet = stripTweet(status.getText());
		TextView tempText = (TextView) findViewById(R.id.tweetTextView);
		TextView tweetView = new TextView(this);
		tweetView.setText(strippedTweet);
		tweetView.setTextColor(Color.BLACK);
		Typeface face = Typeface.createFromAsset(getAssets(),
				"fonts/helveticaneue-bold.ttf");
		tweetView.setTypeface(face);
		if (strippedTweet.length() > 95)
			tweetView.setTextSize(14);
		else if (strippedTweet.length() > 80)
			tweetView.setTextSize(16);
		else if (strippedTweet.length() > 40)
			tweetView.setTextSize(18);
		else if (strippedTweet.length() > 20)
			tweetView.setTextSize(20);
		else
			tweetView.setTextSize(24);
		card.addView(tweetView, tempText.getLayoutParams());

		TextView tempUser = (TextView) findViewById(R.id.userTextView);
		TextView userView = new TextView(this);
		String userName = "@" + status.getFromUser();
		userView.setText(userName);
		userView.setTextSize(14);
		Typeface faceUser = Typeface.createFromAsset(getAssets(),
				"fonts/helveticaneue.ttf");
		userView.setTypeface(faceUser);
		card.addView(userView, tempUser.getLayoutParams());

		TextView tempDate = (TextView) findViewById(R.id.dateTextView);
		TextView dateView = new TextView(this);
		Date d = status.getCreatedAt();
		dateView.setText(getDateString(d));
		dateView.setTextSize(14);
		Typeface faceDate = Typeface.createFromAsset(getAssets(),
				"fonts/helveticaneue.ttf");
		dateView.setTypeface(faceDate);
		card.addView(dateView, tempDate.getLayoutParams());

		ImageView daImage = (ImageView) card.findViewById(R.id.photoView);
		Bitmap imageBitmap = getBitmapFromURL(imageURL);
		Bitmap squareBitmap = getSquareForBitmap(imageBitmap);

		if (squareBitmap != null) {
			daImage.setImageBitmap(squareBitmap);

			card.setOnClickListener(cardSelected);
			return card;
		}
		return null;
	}

	private void convertStatusToHighDefCard(Tweet status, String imageURL) {
		// final ImageView iv = new ImageView(this);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		AbsoluteLayout card = (AbsoluteLayout) inflater.inflate(
				R.layout.cardbig, null);

		setContentView(R.layout.cardbig);
		String strippedTweet = stripTweet(status.getText());
		TextView tempText = (TextView) findViewById(R.id.tweetTextViewBig);
		TextView tweetView = new TextView(this);
		tweetView.setText(strippedTweet);
		tweetView.setTextColor(Color.BLACK);
		Typeface face = Typeface.createFromAsset(getAssets(),
				"fonts/helveticaneue-bold.ttf");
		tweetView.setTypeface(face);
		if (strippedTweet.length() > 95)
			tweetView.setTextSize(24);
		else if (strippedTweet.length() > 80)
			tweetView.setTextSize(28);
		else if (strippedTweet.length() > 40)
			tweetView.setTextSize(31);
		else if (strippedTweet.length() > 20)
			tweetView.setTextSize(36);
		else
			tweetView.setTextSize(48);
		card.addView(tweetView, tempText.getLayoutParams());

		TextView tempUser = (TextView) findViewById(R.id.userTextViewBig);
		TextView userView = new TextView(this);
		String userName = "@" + status.getFromUser();
		userView.setText(userName);
		userView.setTextSize(28);
		Typeface faceUser = Typeface.createFromAsset(getAssets(),
				"fonts/helveticaneue.ttf");
		userView.setTypeface(faceUser);
		card.addView(userView, tempUser.getLayoutParams());

		TextView tempDate = (TextView) findViewById(R.id.dateTextViewBig);
		TextView dateView = new TextView(this);
		Date d = status.getCreatedAt();
		dateView.setText(getDateString(d));
		dateView.setTextSize(28);
		Typeface faceDate = Typeface.createFromAsset(getAssets(),
				"fonts/helveticaneue.ttf");
		dateView.setTypeface(faceDate);
		card.addView(dateView, tempDate.getLayoutParams());

		ImageView daImage = (ImageView) card.findViewById(R.id.photoViewBig);
		Bitmap imageBitmap = getBitmapFromURL(imageURL);
		daImage.setImageBitmap(getSquareForBitmap(imageBitmap));

		card.setDrawingCacheEnabled(true);

		// this is the important code :)
		// Without it the view will have a dimension of 0,0 and the bitmap will
		// be null
		card.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		card.layout(0, 0, card.getMeasuredWidth(), card.getMeasuredHeight());

		card.buildDrawingCache(true);
		Bitmap b = Bitmap.createBitmap(card.getDrawingCache());
		card.setDrawingCacheEnabled(false); // clear drawing cache

		// Uri uritoSend = saveBitmap(b);

		picsBitmaps.add(b);

	}

	public void stripHex(String hexString) {
		Pattern insideTags = Pattern.compile("(?<=[>])[^<>]+(?=[<])");
		Matcher m = insideTags.matcher(hexString);
		String moddedString = "";
		while (m.find())
			moddedString = m.group(0);

		String hexRegex2 = "\\.";
		moddedString = moddedString.replaceAll(hexRegex2, " ");
		if (moddedString.length() > 0) {

			coinsInserted = Integer.valueOf(moddedString);
		}
	}

	public void updatePriceView() {
		runOnUiThread(new Runnable() {
			public void run() {
				FontHelper
						.applyFont(getBaseContext(),
								findViewById(R.id.priceTextView),
								"fonts/franchise.ttf");
				TextView t = (TextView) findViewById(R.id.priceTextView);
				String setString;
				int currentCoins = coinsInserted - totalCoins;
				setString = "" + coinsInserted;

				if (currentCoins < photoPrice) {
					setString = "Insert $"
							+ String.format(
									"%.2f",
									((float) photoPrice - (float) currentCoins) / 100.0f);
				} else
					setString = "Tap Photo To Print!";

				t.setText(setString);
				t.setTextColor(getResources().getColor(R.color.black1));
			}
		});
	}

	public void updateRefreshView(final int time) {
		runOnUiThread(new Runnable() {
			public void run() {
				FontHelper.applyFont(getBaseContext(),
						findViewById(R.id.refreshTextView),
						"fonts/franchise.ttf");
				TextView t = (TextView) findViewById(R.id.refreshTextView);
				t.setTextColor(getResources().getColor(R.color.gray));

				if (time == -1)
					t.setText("Refreshing...");
				else if (time > refreshInterval)
					t.setText("Refreshing in " + refreshInterval + "s");
				else
					t.setText("Refreshing in " + time + "s");
			}
		});
	}

	public Uri saveBitmap(Bitmap savePic) {
		String filename = String.valueOf(System.currentTimeMillis());
		ContentValues values = new ContentValues();
		values.put(Images.Media.TITLE, filename);
		values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
		values.put(Images.Media.MIME_TYPE, "image/jpeg");

		Uri uri = this.getContentResolver().insert(
				Images.Media.EXTERNAL_CONTENT_URI, values);
		try {
			OutputStream outStream = this.getContentResolver()
					.openOutputStream(uri);
			savePic.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
			outStream.flush();
			outStream.close();
			Log.d("done", "done");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return uri;
	}

	private OnClickListener cardSelected = new OnClickListener() {

		public void onClick(View v) {
			TextView b = (TextView) findViewById(R.id.priceTextView);
			if (b.getText().toString().equals("Tap Photo To Print!")) {
				int viewIndex = pics.indexOf(v);
				Log.v("derp", "printing card at index " + viewIndex);
				Bitmap hdpic = picsBitmaps.get(viewIndex);
				
				Bitmap cardPic = loadBitmapFromView(v);
				
				printCard(hdpic);
				totalCoins = coinsInserted;
			}
		}
	};

	public static Bitmap loadBitmapFromView(View view) {
		Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(),
				view.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(returnedBitmap);
		Drawable bgDrawable = view.getBackground();
		if (bgDrawable != null)
			bgDrawable.draw(canvas);
		else
			canvas.drawColor(Color.WHITE);
		view.draw(canvas);
		return returnedBitmap;
	}

	private void printCard(Bitmap b) {
		Uri uritoSend = saveBitmap(b);

		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		sharingIntent.setType("image/jpeg");
		sharingIntent.setComponent(new ComponentName("com.android.bluetooth",
				"com.android.bluetooth.opp.BluetoothOppLauncherActivity"));
		sharingIntent.putExtra(Intent.EXTRA_STREAM, uritoSend);
		startActivity(sharingIntent);
	}

	public Uri getImageUri(Bitmap inImage) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
		String path = Images.Media.insertImage(this.getContentResolver(),
				inImage, "Title", null);
		return Uri.parse(path);
	}

	private String stripTweet(String tweet) {
		String mentionRegex = "(?i)\\s?(@){1}(uvenir)\\s?";
		String mention2Regex = "(?i)\\s?(@){1}(ungram)\\s?";
		String urlRegex = "(?i)(((f|ht){1}tp://)[-a-zA-Z0-9@:%_\\+.~#?&//=]+)";
		String moddedString = tweet.replaceAll(mentionRegex, "");
		moddedString = moddedString.replaceAll(mention2Regex, "");
		moddedString = moddedString.replaceAll(urlRegex, "");
		return moddedString;
	}

	private String getDateString(Date d) {
		CharSequence s = DateFormat.format("h.mmaa, MMM dd, yyyy", d.getTime());
		return s.toString();
	}

	@SuppressWarnings("unused")
	private void getImagesForMentions(List<Status> tweets) throws IOException,
			JSONException {
		for (Status status : tweets) {
			MediaEntity[] mediaList = status.getMediaEntities();
			URLEntity[] urlList = status.getURLEntities();
			if (mediaList != null) {
				String imageURL = mediaList[0].getMediaURL().toString();
				Log.d("derp", imageURL);
				ImageView iv = new ImageView(this);
				// UrlImageViewHelper.setUrlDrawable(iv, imageURL);
				Bitmap imageBitmap = getBitmapFromURL(imageURL);
				iv.setImageBitmap(getSquareForBitmap(imageBitmap));
				realViewSwitcher.addView(iv);
			}
			if (urlList.length > 0) {
				// dont have to crop instagrams.
				URL urlForPic = urlList[0].getExpandedURL();
				if (urlForPic.getHost().toString().equals("instagr.am")) {
					String instagramCallbackURL = "http://instagram.com/api/v1/oembed/?url=";
					String largeImage = instagramCallbackURL.concat(urlForPic
							.toString());
					JSONObject json = readJsonFromUrl(largeImage);
					String imageURL = json.get("url").toString();
					Log.d("derp", imageURL);
					ImageView iv = new ImageView(this);
					UrlImageViewHelper.setUrlDrawable(iv, imageURL);
					realViewSwitcher.addView(iv);
				}
			}
		}
	}

	private Bitmap getSquareForBitmap(Bitmap imageBitmap) {
		Bitmap finalBitmap = null;
		if (imageBitmap != null) {
			if (imageBitmap.getHeight() <= imageBitmap.getWidth()) {
				finalBitmap = Bitmap.createBitmap(imageBitmap,
						imageBitmap.getWidth() / 2 - imageBitmap.getHeight()
								/ 2, 0, imageBitmap.getHeight(),
						imageBitmap.getHeight());
			} else {
				finalBitmap = Bitmap.createBitmap(imageBitmap, 0,
						imageBitmap.getHeight() / 2 - imageBitmap.getWidth()
								/ 2, imageBitmap.getWidth(),
						imageBitmap.getWidth());
			}
		}
		return finalBitmap;
	}

	private Bitmap clipView(Bitmap imageBitmap) {
		Bitmap finalBitmap = null;
		finalBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, 399, 532);

		return finalBitmap;
	}

	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.i("derp", "New Intent Arrived");
		dealWithTwitterResponse(intent);
	}

	private void dealWithTwitterResponse(Intent intent) {
		Uri uri = intent.getData();
		Log.v("derp", uri.toString());
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException,
			JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is,
					Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	public static Bitmap getBitmapFromURL(String src) {
		try {
			URL url = new URL(src);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			Bitmap myBitmap = BitmapFactory.decodeStream(input);
			return myBitmap;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void displayHelpMenu(View v) {
		ImageButton helpButton = (ImageButton) findViewById(R.id.helpButton);
		int visibility = 666;
		if (myViewPager != null)
			visibility = myViewPager.getVisibility();
		if (visibility == View.INVISIBLE) {
			helpButton.setImageResource(R.drawable.help);
			myViewPager.setVisibility(View.VISIBLE);
			HelpViewPager.setVisibility(View.INVISIBLE);
		} else {
			helpButton.setImageResource(R.drawable.helpback);
			ArrayList<View> helppics = new ArrayList<View>();
			ImageView help1 = new ImageView(this);
			help1.setImageResource(R.drawable.help1);
			helppics.add(help1);
			ImageView help2 = new ImageView(this);
			help2.setImageResource(R.drawable.help2);
			helppics.add(help2);
			ImageView help3 = new ImageView(this);
			help3.setImageResource(R.drawable.help3);
			helppics.add(help3);

			MyViewPagerAdapter helpadapter = new MyViewPagerAdapter(null,
					helppics);
			HelpViewPager = (ViewPager) findViewById(R.id.helpviewpager);
			HelpViewPager.setAdapter(helpadapter);
			HelpViewPager.setOffscreenPageLimit(20);
			HelpViewPager.setVisibility(View.VISIBLE);
			HelpViewPager.setCurrentItem(0);
			if (visibility != 666)
				myViewPager.setVisibility(View.INVISIBLE);
		}
	}

	public void debugButton(View v) {
		// stripHex("tootototototo <>200<>");
	}

	// ////////

	public class UpdateTwitterList extends AsyncTask<String, Void, List<Tweet>> {

		@Override
		protected List<Tweet> doInBackground(String... params) {
			Log.v("derp", "starting up");

			AccessToken at = new AccessToken(TOKEN, SECRET);
			Log.v("derp", "got mah twitter factory!!!~!");
			mTwitter.setOAuthAccessToken(at);

			pics.clear();
			imgUrls.clear();
			try {
				Log.v("derp", "trying timeline");
				// @SuppressWarnings("deprecation")
				// List<Status> statuses = mTwitter.getMentions();

				Query daQuery = new Query("@Uvenir");
				QueryResult result;
				result = mTwitter.search(daQuery);
				List<Tweet> tweets = result.getTweets();

				daQuery.setQuery("@uvenir");
				QueryResult result2 = mTwitter.search(daQuery);
				List<Tweet> tweets2 = result2.getTweets();

				for (int i = 0; i < tweets2.size() - 1; i++) {
					tweets.add(tweets2.get(i));
				}

				Log.v("derp", "finished getting twats ");

				return tweets;
			} catch (TwitterException te) {
				te.printStackTrace();
				Log.v("derp", te.getMessage());
			}
			return null;
		}

		@Override
		protected void onPostExecute(List<Tweet> tweets) {
			try {
				getImagesForSearch(tweets);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.v("derp", "finished building pictures");
			setContentView(R.layout.activity_main);

			adapta = new MyViewPagerAdapter(null, pics);
			myViewPager = (ViewPager) findViewById(R.id.viewpager);
			myViewPager.setPageMargin(-225);
			myViewPager.setAdapter(adapta);
			myViewPager.setOffscreenPageLimit(20);
			refreshTimer = 0;
		}

		@Override
		protected void onPreExecute() {
			updateRefreshView(-1);
		}

		@Override
		protected void onProgressUpdate(Void... values) {
		}
	}
}