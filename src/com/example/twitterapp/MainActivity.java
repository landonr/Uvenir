package com.example.twitterapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.github.ysamlan.horizontalpager.HorizontalPager;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class MainActivity extends Activity {
	private static final String TOKEN = "930564715-Isej8FL9OdMHIGmtCNOE8OBEHBV94yihbBmTzpkM";
	private static final String SECRET = "Iqp9fpMTp6e7ZEh6wA0mZjqxUbkBOfQWmezLGw4UWA";
	private static final String CONSUMER_KEY = "2Rrl4sxityJCLfQJOnCIOQ";
	private static final String CONSUMER_SECRET = "TrLvOTv4R8WYloKUmy41pXJepb3udIhpL7d9C6NF4";
	private Twitter mTwitter;
	HorizontalPager realViewSwitcher;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		LinearLayout myView = (LinearLayout) findViewById(R.id.RelativeLayout1);
		mTwitter = new TwitterFactory().getInstance();
		mTwitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		// Button login = (Button) findViewById(R.id.login);

		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void loginClicked(View v) throws IOException, JSONException {
		Log.v("derp", "Log in clicked yo!!!!");
		loginAuthorisedUser();
	}

	private void loginAuthorisedUser() throws IOException, JSONException {
		Log.v("derp", "starting up");
		Log.v("derp", TOKEN);
		Log.v("derp", SECRET);
		AccessToken at = new AccessToken(TOKEN, SECRET);
		Log.v("derp", "got mah twitter factory!!!~!");
		mTwitter.setOAuthAccessToken(at);
		try {
			Log.v("derp", "trying timeline");
			// @SuppressWarnings("deprecation")
			// List<Status> statuses = mTwitter.getMentions();
			Log.v("derp", "got this far :v");

			realViewSwitcher = new HorizontalPager(getApplicationContext());

			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT, 800);
			lp.gravity = 0;
			lp.setMargins(100, 100, 100, 100);
			realViewSwitcher.setLayoutParams(lp);

			Query daQuery = new Query("@ungram");
			QueryResult result;
			result = mTwitter.search(daQuery);
			List<Tweet> statuses = result.getTweets();
			getImagesForSearch(statuses);

			daQuery.setQuery("@uvenir");
			result = mTwitter.search(daQuery);
			List<Tweet> tweets = result.getTweets();
			getImagesForSearch(tweets);

			setContentView(R.layout.activity_main);
			addContentView(realViewSwitcher, lp);
			// setContentView(realViewSwitcher, lp);

		} catch (TwitterException te) {
			te.printStackTrace();
			Log.v("derp", te.getMessage());
		}
	}

	private void getImagesForSearch(List<Tweet> tweets) throws IOException,
			JSONException {
		for (Tweet status : tweets) {
			
			MediaEntity[] mediaList = status.getMediaEntities();
			URLEntity[] urlList = status.getURLEntities();
			if (mediaList != null) {
				String imageURL = mediaList[0].getMediaURL().toString();
				Log.d("derp", imageURL);
				AbsoluteLayout card = convertStatusToCard(status, imageURL);
				realViewSwitcher.addView(card);
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
					AbsoluteLayout card = convertStatusToCard(status, imageURL);
					realViewSwitcher.addView(card);

				}
			}
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
			tweetView.setTextSize(12);
		else if (strippedTweet.length() > 80)
			tweetView.setTextSize(14);
		else if (strippedTweet.length() > 40)
			tweetView.setTextSize(16);
		else if (strippedTweet.length() > 20)
			tweetView.setTextSize(18);
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
		daImage.setImageBitmap(getSquareForBitmap(imageBitmap));
		// UrlImageViewHelper.setUrlDrawable(daImage, imageURL);
		// AbsoluteLayout card = convertImageToCard(iv);
		return card;
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
	
	private String getDateString(Date d)
	{
		CharSequence s  = DateFormat.format("h.mmaa, MMM dd, yyyy", d.getTime());
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
		if (imageBitmap.getHeight() <= imageBitmap.getWidth()) {
			finalBitmap = Bitmap.createBitmap(imageBitmap,
					imageBitmap.getWidth() / 2 - imageBitmap.getHeight() / 2,
					0, imageBitmap.getHeight(), imageBitmap.getHeight());
		} else {
			finalBitmap = Bitmap.createBitmap(imageBitmap, 0,
					imageBitmap.getHeight() / 2 - imageBitmap.getWidth() / 2,
					imageBitmap.getWidth(), imageBitmap.getWidth());
		}
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
}