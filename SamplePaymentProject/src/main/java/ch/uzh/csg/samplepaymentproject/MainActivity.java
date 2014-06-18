package ch.uzh.csg.samplepaymentproject;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.UUID;

import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECParameterSpec;

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
import android.widget.CompoundButton;
import android.widget.ToggleButton;
import ch.uzh.csg.mbps.customserialization.Currency;
import ch.uzh.csg.mbps.customserialization.PKIAlgorithm;
import ch.uzh.csg.mbps.customserialization.exceptions.UnknownPKIAlgorithmException;
import ch.uzh.csg.nfclib.exceptions.NfcNotEnabledException;
import ch.uzh.csg.nfclib.exceptions.NoNfcException;
import ch.uzh.csg.paymentlib.IUserPromptPaymentRequest;
import ch.uzh.csg.paymentlib.PaymentEvent;
import ch.uzh.csg.paymentlib.PaymentEventHandler;
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
/**
 * 
 * @author Jeton Memeti
 * 
 */
public class MainActivity extends Activity {

	protected static final String PREF_UNIQUE_ID = "pref_unique_id";
	private static String uniqueID;
	
	private static final String TAG = "##NFC## MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "start");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final PaymentEventHandler eventHandler = new PaymentEventHandler() {

			@Override
			public void handleMessage(PaymentEvent event, Object object) {
				Log.i(TAG, "evt:" + event + " obj:" + object);
			}
		};
		String userName = id(getApplicationContext());
		
		
		try {
			
			final KeyPair keyPair = generateKeyPair();
			
			final UserInfos userInfos = new UserInfos(userName, keyPair.getPrivate(), PKIAlgorithm.DEFAULT, 1);

			final PaymentInfos paymentInfos = new PaymentInfos(Currency.BTC, 5);
			
			final ServerInfos serverInfos = new ServerInfos(keyPair.getPublic());

			Button sendButton = (Button) findViewById(R.id.button1);
			sendButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					NfcAdapter nfcAdapter = createAdapter(MainActivity.this);
					if (nfcAdapter == null) {
						Log.e(TAG, "no nfc adapter");
						return;
					}
					try {
						Log.i(TAG, "init payment");
						PaymentRequestInitializer init = new PaymentRequestInitializer(MainActivity.this, eventHandler,
						        userInfos, paymentInfos, serverInfos, PaymentType.REQUEST_PAYMENT);
						
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NoNfcException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NfcNotEnabledException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});

			final IUserPromptPaymentRequest userPrompt = new IUserPromptPaymentRequest() {

				@Override
				public boolean isPaymentAccepted() {
					Log.i(TAG, "payment accepted");
					return true;
				}

				@Override
				public boolean getPaymentRequestAnswer(String username, Currency currency, long amount) {
					Log.i(TAG, "user " + username + " wants " + amount);
					return true;
				}
			};

			final IPersistencyHandler persistencyHandler = new IPersistencyHandler() {

				@Override
				public PersistedPaymentRequest getPersistedPaymentRequest(String username, Currency currency,
				        long amount) {
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

			PaymentRequestHandler handler = new PaymentRequestHandler(this, eventHandler, userInfos, serverInfos,
			        userPrompt, persistencyHandler);
			Log.i(TAG, "payment handler initilazied");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
	
	private static final String SECURITY_PROVIDER = "SC";
	
	/**
	 * Adds the spongy castle security provider in order to be able to generate
	 * ECC KeyPairs on Android. (See http://rtyley.github.io/spongycastle/ for
	 * more information.)
	 */
	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	/**
	 * Generates a KeyPair with the default {@link PKIAlgorithm}.
	 * 
	 * @throws UnknownPKIAlgorithmException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidAlgorithmParameterException
	 */
	public static KeyPair generateKeyPair() throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		return generateKeyPair(PKIAlgorithm.DEFAULT);
	}

	/**
	 * Generates a KeyPair with the provided {@link PKIAlgorithm}.
	 * 
	 * @param algorithm
	 *            the {@link PKIAlgorithm} to be used to generate the KeyPair
	 * @throws UnknownPKIAlgorithmException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidAlgorithmParameterException
	 */
	public static KeyPair generateKeyPair(PKIAlgorithm algorithm) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		if (algorithm.getCode() != PKIAlgorithm.DEFAULT.getCode())
			throw new UnknownPKIAlgorithmException();
		
		ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(algorithm.getKeyPairSpecification());
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm.getKeyPairAlgorithm(), SECURITY_PROVIDER);
		keyGen.initialize(ecSpec, new SecureRandom());
		return keyGen.generateKeyPair();
	}
}
