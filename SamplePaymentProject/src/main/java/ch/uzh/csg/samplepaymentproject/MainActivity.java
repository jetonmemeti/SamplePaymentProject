package ch.uzh.csg.samplepaymentproject;

import java.security.KeyPair;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import ch.uzh.csg.mbps.customserialization.Currency;
import ch.uzh.csg.mbps.customserialization.DecoderFactory;
import ch.uzh.csg.mbps.customserialization.PKIAlgorithm;
import ch.uzh.csg.mbps.customserialization.PaymentRequest;
import ch.uzh.csg.mbps.customserialization.PaymentResponse;
import ch.uzh.csg.mbps.customserialization.ServerPaymentRequest;
import ch.uzh.csg.mbps.customserialization.ServerPaymentResponse;
import ch.uzh.csg.mbps.customserialization.ServerResponseStatus;
import ch.uzh.csg.nfclib.NfcLibException;
import ch.uzh.csg.paymentlib.Answer;
import ch.uzh.csg.paymentlib.IPaymentEventHandler;
import ch.uzh.csg.paymentlib.IServerResponseListener;
import ch.uzh.csg.paymentlib.IUserPromptPaymentRequest;
import ch.uzh.csg.paymentlib.PaymentEvent;
import ch.uzh.csg.paymentlib.PaymentRequestHandler;
import ch.uzh.csg.paymentlib.PaymentRequestInitializer;
import ch.uzh.csg.paymentlib.PaymentRequestInitializer.PaymentType;
import ch.uzh.csg.paymentlib.container.PaymentInfos;
import ch.uzh.csg.paymentlib.container.ServerInfos;
import ch.uzh.csg.paymentlib.container.UserInfos;
import ch.uzh.csg.paymentlib.exceptions.IllegalArgumentException;
import ch.uzh.csg.paymentlib.persistency.IPersistencyHandler;
import ch.uzh.csg.paymentlib.persistency.PersistedPaymentRequest;

//TODO: javadoc
public class MainActivity extends Activity {
	
	String publicKey = "MFowFAYHKoZIzj0CAQYJKyQDAwIIAQEHA0IABBPPH9M9blbhfZNSujH4LoBsml7yoyqBwyw5+MRLFWqzMLuPDaTQQdPzuY4f9JBF7qGtQeQ4K6d+lcCNjmknPSQ=";
	String privateKey = "MIGVAgEAMBQGByqGSM49AgEGCSskAwMCCAEBBwR6MHgCAQEEIEf5xuzP91nvSKpnOMZMncjOe1r6ZEqTKgNNWOuFBTo2oAsGCSskAwMCCAEBB6FEA0IABBPPH9M9blbhfZNSujH4LoBsml7yoyqBwyw5+MRLFWqzMLuPDaTQQdPzuY4f9JBF7qGtQeQ4K6d+lcCNjmknPSQ=";

	protected static final String PREF_UNIQUE_ID = "pref_unique_id";
	private static String uniqueID;

	private static final String TAG = "##NFC## MainActivity";
	
	private KeyPair keyPairServer;
	private AlertDialog userPromptDialog;
	
	private volatile boolean responseReady = false;
	private boolean paymentAccepted = false;
	
	private PaymentRequestInitializer initializer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "start");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		try {
			keyPairServer = new KeyPair(KeyHandler.decodePublicKey(publicKey), KeyHandler.decodePrivateKey(privateKey));
			
			final ServerInfos serverInfos = new ServerInfos(keyPairServer.getPublic());
			
			String userName = id(getApplicationContext());

			final KeyPair keyPair = KeyHandler.generateKeyPair();
			final UserInfos userInfos = new UserInfos(userName, keyPair.getPrivate(), PKIAlgorithm.DEFAULT, 1);
			final PaymentInfos paymentInfos = new PaymentInfos(Currency.BTC, 5);

			Button requestButton = (Button) findViewById(R.id.button1);
			requestButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					NfcAdapter nfcAdapter = createAdapter(MainActivity.this);
					if (nfcAdapter == null) {
						Log.e(TAG, "no nfc adapter");
						return;
					}
					try {
						Log.i(TAG, "init payment REQUEST");
						
//						if (initializer != null) {
//							initializer.
//						}
						
						initializer = new PaymentRequestInitializer(MainActivity.this, eventHandler, userInfos, paymentInfos, serverInfos, PaymentType.REQUEST_PAYMENT);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (NfcLibException e) {
						e.printStackTrace();
					}
				}
			});
			
			Button sendButton = (Button) findViewById(R.id.button2);
			sendButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					NfcAdapter nfcAdapter = createAdapter(MainActivity.this);
					if (nfcAdapter == null) {
						Log.e(TAG, "no nfc adapter");
						return;
					}
					try {
						Log.i(TAG, "init payment SEND");
						initializer = new PaymentRequestInitializer(MainActivity.this, eventHandler, userInfos, paymentInfos, serverInfos, PaymentType.SEND_PAYMENT);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (NfcLibException e) {
						e.printStackTrace();
					}
				}
			});

			new PaymentRequestHandler(this, eventHandler, userInfos, serverInfos, userPrompt, persistencyHandler);
			Log.i(TAG, "payment handler initilazied");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private IPaymentEventHandler eventHandler = new IPaymentEventHandler() {

		@Override
		public void handleMessage(PaymentEvent event, Object object) {
			Log.i(TAG, "evt1:" + event + " obj:" + object);
			
			if (userPromptDialog!= null && userPromptDialog.isShowing()) {
				userPromptDialog.dismiss();
			}
			
			if (event == PaymentEvent.SUCCESS) {
				showSuccessDialog(object);
			}
		}

		@Override
        public void handleMessage(PaymentEvent event, Object object, IServerResponseListener caller) {
			Log.i(TAG, "evt2:" + event + " obj:" + object);
			
			switch (event) {
			case ERROR:
				break;
			case FORWARD_TO_SERVER:
				try {
					ServerPaymentRequest decode = DecoderFactory.decode(ServerPaymentRequest.class,
							(byte[]) object);
					PaymentRequest paymentRequestPayer = decode.getPaymentRequestPayer();
					
					PaymentResponse pr = new PaymentResponse(PKIAlgorithm.DEFAULT, 1,
							ServerResponseStatus.SUCCESS, null, paymentRequestPayer.getUsernamePayer(),
							paymentRequestPayer.getUsernamePayee(), paymentRequestPayer.getCurrency(),
							paymentRequestPayer.getAmount(), paymentRequestPayer.getTimestamp());
					pr.sign(keyPairServer.getPrivate());
					ServerPaymentResponse spr = new ServerPaymentResponse(pr);
					caller.onServerResponse(spr);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case NO_SERVER_RESPONSE:
				break;
			case SUCCESS:
				break;
			}
        }
		
	};
	
	private IUserPromptPaymentRequest userPrompt = new IUserPromptPaymentRequest() {

		@Override
		public boolean isPaymentAccepted() {
			Log.i(TAG, "payment accepted");
			return paymentAccepted;
		}

		@Override
        public void promptUserPaymentRequest(String username, Currency currency, long amount, Answer answer) {
			Log.i(TAG, "user " + username + " wants " + amount);
			showCustomDialog(username, currency, amount, answer);
        }
		
	};
	
	//TODO: implement this to have use case with removing for pressing button
	private IPersistencyHandler persistencyHandler = new IPersistencyHandler() {

		@Override
		public PersistedPaymentRequest getPersistedPaymentRequest(String username, Currency currency, long amount) {
			Log.i(TAG, "getPersistedPaymentRequest");
			return null;
		}

		@Override
		public void delete(PersistedPaymentRequest paymentRequest) {
			Log.i(TAG, "delete");
		}

		@Override
		public void add(PersistedPaymentRequest paymentRequest) {
			Log.i(TAG, "add");
		}
		
	};

	private void showCustomDialog(String username, Currency currency, long amount, final Answer answer2) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Incoming Payment Request")
			.setMessage("Do you want to pay "+amount+" "+currency.getCurrencyCode()+" to "+username+"?")
			.setCancelable(false)
			.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   responseReady = true;
		        	   paymentAccepted = true;
		               answer2.success();
		           }
		       })
		     .setNegativeButton("Reject", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   responseReady = true;
		        	   paymentAccepted = false;
		               answer2.failed();
		           }
		       });
		
		runOnUiThread(new Runnable() {
		    public void run() {
		    	userPromptDialog = builder.create();
				userPromptDialog.show();
		    }
		});
    }
	
	private void showSuccessDialog(Object object) {
		String msg;
		if (object == null) {
			msg = "object is null";
		} else if (!(object instanceof PaymentResponse)) {
			msg = "object is not instance of PaymentResponse";
		} else {
			PaymentResponse pr = (PaymentResponse) object;
			msg = "payed "+pr.getAmount() +" "+pr.getCurrency().getCurrencyCode()+" to "+pr.getUsernamePayee();
		}
		
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Payment Success!")
			.setMessage(msg)
			.setCancelable(true)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		
		runOnUiThread(new Runnable() {
		    public void run() {
		    	AlertDialog alert = builder.create();
				alert.show();
		    }
		});
		
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Create an NFC adapter, if NFC is enabled, return the adapter, otherwise
	 * null and open up NFC settings.
	 * 
	 * @param context
	 * @return
	 */
	private NfcAdapter createAdapter(Context context) {
		NfcAdapter nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(getApplicationContext());

		if (!nfcAdapter.isEnabled()) {
			AlertDialog.Builder alertbox = new AlertDialog.Builder(context);
			alertbox.setTitle("Info");
			alertbox.setMessage("Enable NFC");
			alertbox.setPositiveButton("Turn On", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
						startActivity(intent);
					} else {
						Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
						startActivity(intent);
					}
				}
			});
			alertbox.setNegativeButton("Close", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			});
			alertbox.show();
			return null;
		}
		return nfcAdapter;
	}

	/**
	 * As seen in
	 * http://stackoverflow.com/questions/2785485/is-there-a-unique-android
	 * -device-id
	 * 
	 * @param context
	 * @return
	 */
	public synchronized static String id(Context context) {
		if (uniqueID == null) {
			SharedPreferences sharedPrefs = context.getSharedPreferences(PREF_UNIQUE_ID, Context.MODE_PRIVATE);
			uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
			if (uniqueID == null) {
				uniqueID = UUID.randomUUID().toString();
				Editor editor = sharedPrefs.edit();
				editor.putString(PREF_UNIQUE_ID, uniqueID);
				editor.commit();
			}
		}
		return uniqueID;
	}

}
