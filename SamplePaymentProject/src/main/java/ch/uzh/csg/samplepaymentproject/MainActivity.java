package ch.uzh.csg.samplepaymentproject;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;
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
import android.util.Base64;
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
import ch.uzh.csg.mbps.customserialization.exceptions.UnknownPKIAlgorithmException;
import ch.uzh.csg.nfclib.NfcLibException;
import ch.uzh.csg.nfclib.Utils;
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
/**
 * 
 * @author Jeton Memeti
 * 
 */
public class MainActivity extends Activity {
	
	String publicKey = "MFowFAYHKoZIzj0CAQYJKyQDAwIIAQEHA0IABBPPH9M9blbhfZNSujH4LoBsml7yoyqBwyw5+MRLFWqzMLuPDaTQQdPzuY4f9JBF7qGtQeQ4K6d+lcCNjmknPSQ=";
	String privateKey = "MIGVAgEAMBQGByqGSM49AgEGCSskAwMCCAEBBwR6MHgCAQEEIEf5xuzP91nvSKpnOMZMncjOe1r6ZEqTKgNNWOuFBTo2oAsGCSskAwMCCAEBB6FEA0IABBPPH9M9blbhfZNSujH4LoBsml7yoyqBwyw5+MRLFWqzMLuPDaTQQdPzuY4f9JBF7qGtQeQ4K6d+lcCNjmknPSQ=";

	protected static final String PREF_UNIQUE_ID = "pref_unique_id";
	private static String uniqueID;

	private static final String TAG = "##NFC## MainActivity";
	
	private boolean paymentAccepted = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "start");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		try {
			
			final KeyPair keyPairServer = new KeyPair(decodePublicKey(publicKey), decodePrivateKey(privateKey));
			//final KeyPair keyPairServer = generateKeyPair();
			
			final ServerInfos serverInfos = new ServerInfos(keyPairServer.getPublic());
			
			final Button acceptButton = (Button) findViewById(R.id.button2);
			acceptButton.setEnabled(false);
			final Button rejectButton = (Button) findViewById(R.id.button3);
			rejectButton.setEnabled(false);
			
			final IPaymentEventHandler eventHandler = new IPaymentEventHandler() {

				@Override
				public void handleMessage(PaymentEvent event, Object object) {
					Log.i(TAG, "evt1:" + event + " obj:" + object);
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
			String userName = id(getApplicationContext());

			final KeyPair keyPair = generateKeyPair();
			
			//byte[] tmp1 = Base64.encode(keyPair.getPublic().getEncoded(), Base64.DEFAULT);
			//byte[] tmp2 = Base64.encode(keyPair.getPrivate().getEncoded(), Base64.DEFAULT);
			
			//System.err.println(new String(tmp1));
			//System.err.println(new String(tmp2));
			
			System.err.println("keypair: "+keyPair.getPublic());

			final UserInfos userInfos = new UserInfos(userName, keyPair.getPrivate(), PKIAlgorithm.DEFAULT, 1);

			final PaymentInfos paymentInfos = new PaymentInfos(Currency.BTC, 5);

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
					} catch (NfcLibException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});

			final IUserPromptPaymentRequest userPrompt = new IUserPromptPaymentRequest() {

				@Override
				public boolean isPaymentAccepted() {
					Log.i(TAG, "payment accepted");
					return paymentAccepted;
				}

				@Override
                public void promptUserPaymentRequest(String username, Currency currency, long amount, Answer answer) {
					acceptButton.setEnabled(true);
					rejectButton.setEnabled(true);
					Log.i(TAG, "user " + username + " wants " + amount);
		            showCustomDialog(username, currency, amount, answer);
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

	private void showCustomDialog(String username, Currency currency, long amount, final Answer answer2) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// Add the buttons
		builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   paymentAccepted = true;
		               answer2.success();
		           }
		       });
		builder.setNegativeButton("Reject", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   paymentAccepted = false;
		               answer2.failed();
		           }
		       });
		// Create the AlertDialog
		AlertDialog dialog = builder.create();
        
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
	
	public static KeyPair generateKeyPair() throws UnknownPKIAlgorithmException, NoSuchAlgorithmException,
    	NoSuchProviderException, InvalidAlgorithmParameterException {
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
	public static KeyPair generateKeyPair(PKIAlgorithm algorithm) throws UnknownPKIAlgorithmException,
	        NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		if (algorithm.getCode() != PKIAlgorithm.DEFAULT.getCode())
			throw new UnknownPKIAlgorithmException();

		ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(algorithm.getKeyPairSpecification());
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm.getKeyPairAlgorithm(), SECURITY_PROVIDER);
		keyGen.initialize(ecSpec, new SecureRandom());
		return keyGen.generateKeyPair();
	}
	
	public static PublicKey decodePublicKey(String publicKeyEncoded) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		return decodePublicKey(publicKeyEncoded, PKIAlgorithm.DEFAULT);
	}

	/**
	 * Decodes the given Base64 encoded String into a PublicKey, using the
	 * provided {@link PKIAlgorithm}.
	 * 
	 * @param publicKeyEncoded
	 *            the string to be decoded
	 * @param algorithm
	 *            the {@link PKIAlgorithm} to be used to generate the PublicKey
	 * @throws UnknownPKIAlgorithmException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeySpecException
	 */
	public static PublicKey decodePublicKey(String publicKeyEncoded, PKIAlgorithm algorithm) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		if (algorithm.getCode() != PKIAlgorithm.DEFAULT.getCode())
			throw new UnknownPKIAlgorithmException();
		
		byte[] decoded = Base64.decode(publicKeyEncoded.getBytes(), Base64.DEFAULT);
		EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decoded);
		
		KeyFactory keyFactory = KeyFactory.getInstance(algorithm.getKeyPairAlgorithm(), SECURITY_PROVIDER);
		return keyFactory.generatePublic(publicKeySpec);
	}
	
	/**
	 * Decodes the given Base64 encoded String into a PrivateKey, using the
	 * default {@link PKIAlgorithm}.
	 * 
	 * @param privateKeyEncoded
	 *            the string to be decoded
	 * @throws UnknownPKIAlgorithmException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeySpecException
	 */
	public static PrivateKey decodePrivateKey(String privateKeyEncoded) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		return decodePrivateKey(privateKeyEncoded, PKIAlgorithm.DEFAULT);
	}
	
	/**
	 * Decodes the given Base64 encoded String into a PrivateKey, using the
	 * provided {@link PKIAlgorithm}.
	 * 
	 * @param privateKeyEncoded
	 *            the string to be decoded
	 * @param algorithm
	 *            the {@link PKIAlgorithm} to be used to generate the PublicKey
	 * @return
	 * @throws UnknownPKIAlgorithmException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws InvalidKeySpecException
	 */
	public static PrivateKey decodePrivateKey(String privateKeyEncoded, PKIAlgorithm algorithm) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		if (algorithm.getCode() != PKIAlgorithm.DEFAULT.getCode())
			throw new UnknownPKIAlgorithmException();
		
		byte[] decoded = Base64.decode(privateKeyEncoded.getBytes(), Base64.DEFAULT);
		EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decoded);
		
		KeyFactory keyFactory = KeyFactory.getInstance(algorithm.getKeyPairAlgorithm(), SECURITY_PROVIDER);
		return keyFactory.generatePrivate(privateKeySpec);
	}
}
