package com.example.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;
import com.seuic.uhfdemo.R;

import android.Manifest;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;


public class InventoryFragement extends Fragment   {

	private static final String TAG = "MainActivity";

	public static final int MAX_LEN = 64;

	private static final int REQUEST_CAMERA_PERMISSION = 100;

	private UHFService mDevice;

	public static String barcode;
	public static String location;
	public static String rfid;
	ScannerService scannerService;
	public boolean mInventoryStart = false;
	private Thread mInventoryThread;

	private EditText scannedBarcodeTextView;
	private EditText scannedLocationTextView;
	private Button scanBarcodeButton;
	private Button scanLocationButton;

	private Button btn_read;
	private Button btn_write;
	private Button btn_clear;

	private TextView et_data;
	private EditText vData;

	int diff;


	View currentView;


	private BroadcastReceiver barcodeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			barcode = intent.getStringExtra(ScannerService.BAR_CODE);
			location = intent.getStringExtra(ScannerService.LOCATION_SERVICE);

			scannedBarcodeTextView.setText(barcode);
			scannedLocationTextView.setText(location);

		}
	};

	static int m_count = 0;

	private static InventoryFragement inventoryfragement;

	public static InventoryFragement getInstance() {
		if (inventoryfragement == null)
			inventoryfragement = new InventoryFragement();
		return inventoryfragement;
	}



	// sound
	private static SoundPool mSoundPool;
	private static int soundID;
	/*
	 * static { mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 20);
	 * soundID = mSoundPool.load(getContext(),R.raw.scan, 1); }
	 */

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mDevice = UHFService.getInstance();

		View view = initUI(inflater);



		return view;
	}



	// init UI
	private View initUI(LayoutInflater inflater) {
		currentView = inflater.inflate(R.layout.fragment_inventory, null);


		et_data = (TextView) currentView.findViewById(R.id.et_data);
		scannedBarcodeTextView = currentView.findViewById(R.id.scannedBarcodeTextView);
		scannedLocationTextView = currentView.findViewById(R.id.scannedLocationTextView);
		scanBarcodeButton =(Button) currentView.findViewById(R.id.scanBarcodeButton);
		scanBarcodeButton.setOnClickListener(new MyClickListener());
		scanLocationButton = (Button) currentView.findViewById(R.id.scanLocationButton);
		scanLocationButton.setOnClickListener(new MyClickListener());


		btn_read = (Button) currentView.findViewById(R.id.bt_read);
		btn_read.setOnClickListener(new MyClickListener());

		btn_write = (Button) currentView.findViewById(R.id.bt_write);
		btn_write.setOnClickListener(new MyClickListener());

		btn_clear = (Button) currentView.findViewById(R.id.bt_clear);
		btn_clear.setOnClickListener(new MyClickListener());



		mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 20);
		soundID = mSoundPool.load(currentView.getContext(), R.raw.scan, 1);



		return currentView;
	}



	private void startBarcodeScan() {
		Intent serviceIntent = new Intent(getActivity(), ScannerService.class);
		getActivity().startService(serviceIntent);
	}

	private void startLocationScan() {
		Intent serviceIntent = new Intent(getActivity(), ScannerService.class);
		getActivity().startService(serviceIntent);
	}


	// Button click event
	private class MyClickListener implements android.view.View.OnClickListener {
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.bt_read:
				BtnRead();
				break;
			case R.id.bt_write:
				BtnWrite();
				break;
			case R.id.bt_clear:
				BtnClear();
				break;
			case R.id.scanBarcodeButton:

				if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
						== PackageManager.PERMISSION_GRANTED) {
//					BarcodeClear();
					startBarcodeScan();
				} else {
					ActivityCompat.requestPermissions(getActivity(),
							new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
				}
				break;
			case R.id.scanLocationButton:
				if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
						== PackageManager.PERMISSION_GRANTED) {
//					LocationClear();

					startLocationScan();
				} else {
					ActivityCompat.requestPermissions(getActivity(),
							new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
				}
				break;
			default:
				break;
			}
		}
	}





	private void BtnRead()  {


			int bank = 1;
			int address = 4;
			int length = 12;

			String str_password = "00000000";

		EPC epc = new EPC();
		if (mDevice.inventoryOnce(epc, 100)) {
			String id = epc.getId();

			byte[] btPassword = new byte[16];
			BaseUtil.getHexByteArray(str_password, btPassword, btPassword.length);
			byte[] buffer = new byte[MAX_LEN];
			if (length > MAX_LEN) {
				buffer = new byte[length];
			}

//			String bar = null;
			if (!mDevice.readTagData(BaseUtil.getHexByteArray(id), btPassword, bank, address, length, buffer)) {

				Toast.makeText(getActivity(), "readTagData_faild", Toast.LENGTH_SHORT).show();
			} else {
//				Toast.makeText(getActivity(), "readTagData_sucess", Toast.LENGTH_SHORT).show();
				String data = BaseUtil.getHexString(buffer, length, " ");
				data = data.replace(" ", "");
				vData = scannedBarcodeTextView;
				String bar = vData.getText().toString().replace(" ", "");
				barcode = bar.replaceAll("[\r\n]+", "");

				diff = 24 - barcode.length();
				rfid = data.substring(diff);

				vData = scannedLocationTextView;
				String loc = vData.getText().toString().replace(" ", "");
				location = loc.replaceAll("[\r\n]+", "");



				if (rfid.equalsIgnoreCase(barcode) ){
					et_data.setText(rfid);
					String jsonData = "{\"RFID_NUMBER\":\"" + rfid + "\",\"BARCODE\":\"" + barcode + "\",\"LOCATION\":\"" + location + "\"}";

					new ApiCallTask().execute(jsonData);
					Toast.makeText(getActivity(), "Mapping_sucess", Toast.LENGTH_SHORT).show();
				}else {
					Toast.makeText(getActivity(), "Mapping Failed, Try Again" , Toast.LENGTH_SHORT).show();

				}


			}


//			String rfidNumber = rfid;
//			String barcodeNu = barcode;
//			String location = scannedLocationTextView.getText().toString();




		} else {
			Toast.makeText(getActivity(), "please_select_a_tag", Toast.LENGTH_SHORT).show();
		}



	}


	private class ApiCallTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			String apiUrl = "http://3.93.246.184:8000/api/endpoint";
			String jsonData = params[0];
			String response = "";

			try {
				Log.d(TAG, "JSON data sent to API: " + jsonData);
				URL url = new URL(apiUrl);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setDoOutput(true);

				OutputStream outputStream = connection.getOutputStream();
				outputStream.write(jsonData.getBytes());
				outputStream.flush();
				outputStream.close();

				int responseCode = connection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					String line;
					StringBuilder stringBuilder = new StringBuilder();
					while ((line = in.readLine()) != null) {
						stringBuilder.append(line);
					}
					in.close();
					response = stringBuilder.toString();
				} else {
					response = "Error: " + responseCode;
				}

				connection.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return response;
		}

		@Override
		protected void onPostExecute(String result) {
			try {
				JSONObject jsonResponse = new JSONObject(result);
				String message = jsonResponse.getString("message");
				Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
				Log.d(TAG, "API response: " + result);
				Log.d(TAG, "API response message: " + message);
			} catch (JSONException e) {
				e.printStackTrace();
				Toast.makeText(getActivity(), "Error parsing response", Toast.LENGTH_SHORT).show();
			}
		}
	}






	private void BtnWrite() {

			int bank = 1;
			int address = 4;
			int length = 12;
			String str_password = "00000000";

		//	String Epc = mEPCList.get(mSelectedIndex).getId();
		EPC epc = new EPC();
		if (mDevice.inventoryOnce(epc, 100)) {
			String id = epc.getId();
			byte[] btPassword = new byte[16];
			BaseUtil.getHexByteArray(str_password, btPassword, btPassword.length);
			vData = scannedBarcodeTextView;
			String str_data = vData.getText().toString().replace(" ", "");
			str_data = str_data.replaceAll("[\r\n]+", "");
			if (str_data.isEmpty()) {
				Toast.makeText(getActivity(), "writeData_cannot_be_empty", Toast.LENGTH_SHORT).show();
				return;
			}

			 diff = 24 - str_data.length();
			while (diff > 0){
				str_data = '0' + str_data;
				diff--;
			}

			byte[] buffer = new byte[MAX_LEN];
			if (length > MAX_LEN) {
				buffer = new byte[length];
			}
			BaseUtil.getHexByteArray(str_data, buffer, length);

			if (!mDevice.writeTagData(BaseUtil.getHexByteArray(id), btPassword, bank, address, length, buffer)) {

				Toast.makeText(getActivity(), "writeTagData_faild", Toast.LENGTH_SHORT).show();
			} else {

				Toast.makeText(getActivity(), "writeTagData_sucess : " + str_data+ " ", Toast.LENGTH_SHORT).show();

			}
		} else {
			Toast.makeText(getActivity(), "please_select_a_tag", Toast.LENGTH_SHORT).show();
		}
	}




	private void BtnClear() {
		if (et_data!= null && scannedBarcodeTextView!=null && scannedLocationTextView!=null && vData !=null) {
			et_data.setText(null);
			scannedBarcodeTextView.setText(null);
			scannedLocationTextView.setText(null);
			vData.setText(null);

		}else if (scannedBarcodeTextView!=null && scannedLocationTextView!=null){
			scannedBarcodeTextView.setText(null);
			scannedLocationTextView.setText(null);
		}else {
			return;
		}
	}

//	private void BarcodeClear(){
//		scannedBarcodeTextView.setText(null);
//	}
//
//	private void LocationClear(){
//		scannedLocationTextView.setText(null);
//	}


	private void playSound() {
		if (mSoundPool == null) {
			mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 20);
			soundID = mSoundPool.load(currentView.getContext(), R.raw.scan, 1);// "/system/media/audio/notifications/Antimony.ogg"
		}
		mSoundPool.play(soundID, 1, 1, 0, 0, 1);
	}



}
