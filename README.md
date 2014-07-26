SamplePaymentProject
====================

This is a minimalistic sample project which shows how the <a href="https://github.com/jetonmemeti/android-nfc-payment-library">AndroidNFCPaymentLibrary</a> can be used.

If you want to use the external <a href="http://www.acs.com.hk/en/products/3/acr122u-usb-nfc-reader/">ACR122u USB NFC Reader</a> instead of the device's internal/build-in NFC controller, assure that you have plugged in the ACR122u before clicking on "Send" or "Request". If the ACR122u is plugged in, it will automatically be used once the <code>PaymentRequestInitializer</code> is initialized.

Prerequisites:
--------------
See the <i>Prerequisites</i> section in the <i>Readme</i> of the <a href="https://github.com/jetonmemeti/android-kitkat-nfc-library">AndroidKitKatNFCLibrary</a>.<br>
This project requires that you have successfully installed the <a href="https://github.com/jetonmemeti/android-nfc-payment-library">AndroidNFCPaymentLibrary</a> (and the <a href="https://github.com/jetonmemeti/android-kitkat-nfc-library">AndroidKitKatNFCLibrary</a>).

Installation Guidelines:
------------------------
<ul>
  <li>Clone this git repository.</li>
  <li>In Eclipse go to <code>File --> Import --> Android --> Existing Android Code Into Workspace</code>. In the appearing dialog under <code>Root Directory</code> enter the path to the checked out project in your git repository.</li>
  <li>Under <code>Project to Import</code> select the <i>SamplePaymentProject</i>. Consider renaming the project (see <code>New Project Name</code>), otherwise it will be called <i>MainActivity</i>.</li>
  <li>Click on <code>Finish</code>.</li>
  <li>Right click on the project and select <code>Properties --> Configure --> Convert to Maven Project</code>.</li>
</ul>
