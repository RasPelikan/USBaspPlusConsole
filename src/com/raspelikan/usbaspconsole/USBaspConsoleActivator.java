package com.raspelikan.usbaspconsole;

import java.nio.ByteBuffer;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.usb4java.Context;
import org.usb4java.DeviceHandle;
import org.usb4java.HotplugCallbackHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author RasPelikan
 */
public class USBaspConsoleActivator extends AbstractUIPlugin
		implements USBaspDevice, IStartup {

	public static final String PLUGIN_SCOPE = "com.raspelikan.usbaspconsole";
	public static final String ACTIVATED_PROPERTY = "com.raspelikan.usbaspconsole.Active";
	public static final boolean ACTIVATED_DEFAULT = false;
	public static final String BAUDRATE_PROPERTY = "com.raspelikan.usbaspconsole.BaudRate";
	public static final int BAUDRATE_DEFAULT = 9600;

	public static final short USBasp_idVendor = 5824;
	public static final short USBasp_idProduct = 1500;
	public static final short USBasp_interface = 0;
	
	public static final byte USBASP_FUNC_UART_PUTBYTE = 50;
	public static final byte USBASP_FUNC_UART_GETBYTE = 51;
	public static final byte USBASP_FUNC_UART_GETBYTECOUNT = 52;
	public static final byte USBASP_FUNC_UART_SETBAUDRATE = 53;
	public static final byte USBASP_RESULT_UART_SETBAUDRATE = 0x53;
	public static final byte USBASP_FUNC_TEST_CMD1 = 61;
	public static final byte USBASP_FUNC_TEST_CMD2 = 62;
	public static final byte USBASP_FUNC_TEST_CMD3 = 63;
	
	// The plug-in ID
	public static final String PLUGIN_ID = "USBaspPlusConsole"; //$NON-NLS-1$

	// The shared instance
	private static USBaspConsoleActivator plugin;

	private Context usbContext;
	private boolean usbDetachedFromSystemDriver;
	private HotplugCallbackHandle usbHotplugCallbackHandle;
	private UsbEventHandlingThread usbEventHandlingThread;
	private DeviceHandle usbaspDeviceHandle;
	
	private ConsoleLogger logger;

	/**
	 * The constructor
	 */
	public USBaspConsoleActivator() {
	}

	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		
		super.start(context);
		plugin = this;

		// start console logger thread
		logger = new ConsoleLogger(plugin.getLog(), this, false);
		logger.start();
		
		// initialize USB hotplug listener and USBasp device (if connected)
		if (isEnabledByPreferences()) {
			startUsb();
		}
		
	}
	
	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		
		// stop console logger thread
		this.logger.shutdown();
		
		// stop any USB services
		stopUsb();
		
		plugin = null;
		super.stop(context);
		
	}
	
	/**
	 * @return Enabled by preferences
	 */
	private boolean isEnabledByPreferences() {
		
		return getPreferenceStore().getBoolean(ACTIVATED_PROPERTY);
		
	}
	
	/**
	 * @return The baud rate configured by preferences
	 */
	public int getBaudRateByPreferences() {
		
		return getPreferenceStore().getInt(BAUDRATE_PROPERTY);
		
	}
	
	/**
	 * initialize USB hotplug listener and USBasp device (if connected)
	 */
	public void startUsb() throws Exception {
		
		if (usbContext != null) {
			return;
		}
		
		// build libusb context
		usbContext = new Context();
		int result = LibUsb.init(usbContext);
		if (result != LibUsb.SUCCESS) {
			throw new LibUsbException("Unable to initialize libusb.", result);
		}
		
		// start lister for device-plugin and device-unplug events
        final boolean hotplugAvailable = enableUsbHotplugHandler();
        
        if (! hotplugAvailable) {
        	
    		// build device handle if device is connected
    		final boolean deviceAvailable = buildUsbDeviceHandle();
    		
    		if (deviceAvailable) {
    			
				this.logger.enable();
				
	        	plugin.getLog().log(new Status(Status.INFO,
	        			USBaspConsoleActivator.PLUGIN_ID,
	        			"Found USBasp device. Enabled USBasp+ console at baud rate '"
	        			+ plugin.getBaudRateByPreferences() + "'!"));
	        	
        	}
        
        }
		
	}

	/**
	 * disable USB hotplug listener
	 */
	public void stopUsb() throws Exception {
		
		if (usbContext != null) {
			
			// stop reading further data
			this.logger.disable();
			
			// disable USB hotplug listener
			disableUsbHotplugHandler();
			
			// close any device connected
			closeUsbDeviceHandle();
			
			// shutdown libusb
			LibUsb.exit(usbContext);
			usbContext = null;
			
		}
		
	}
	
	/**
	 * start lister for device-plugin and device-unplug events
	 */
	private boolean enableUsbHotplugHandler() {
		
		int result;
		
		// Start the event handling thread
		usbEventHandlingThread = new UsbEventHandlingThread();
		usbEventHandlingThread.start();

		// check whether hotplug is available
		if (!LibUsb.hasCapability(LibUsb.CAP_HAS_HOTPLUG)) {
			
			plugin.getLog().log(new Status(Status.WARNING,
					PLUGIN_ID, "Libusb doesn't support hotplug on this system!"
					+ " At the moment no USBasp device is connected. "
					+ "Unfortunataly you have to restart Eclipse once "
					+ "you plugged in a device!"));
			
			return false;
		    
		} else {
			
		    usbHotplugCallbackHandle = new HotplugCallbackHandle();
		    result = LibUsb.hotplugRegisterCallback(null,
		        LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED
		            | LibUsb.HOTPLUG_EVENT_DEVICE_LEFT,
		        LibUsb.HOTPLUG_ENUMERATE,
		        LibUsb.HOTPLUG_MATCH_ANY,
		        LibUsb.HOTPLUG_MATCH_ANY,
		        LibUsb.HOTPLUG_MATCH_ANY,
		        new UsbHotplugHandler(this),
		        null,
		        usbHotplugCallbackHandle);
		    if (result != LibUsb.SUCCESS) {
		        throw new LibUsbException("Unable to register hotplug callback",
		            result);
		    }
		    
			return true;
		    
		}
		
	}

	/**
	 * stop lister for device-plugin and device-unplug events
	 */
	private void disableUsbHotplugHandler() {
		
		// if thread is running then abort the thread
		if (this.usbEventHandlingThread != null) {
			
			this.usbEventHandlingThread.abort();
			
			try {
				this.usbEventHandlingThread.join();
			} catch (InterruptedException e) {
				// never mind
			}
			
		}
		
		LibUsb.hotplugDeregisterCallback(null, usbHotplugCallbackHandle);
		
	}
	
	/**
	 * The plugin should start on starting Eclipse. So the plugin has to extend
	 * org.eclipse.ui.startup. This requires a class implementing IStartup
	 * even if the plugin is not interested in the event itself. 
	 */
	@Override
	public void earlyStartup() {
		
		// do nothing, everything is done in "start"
		
	}
	
	/**
	 * Called by UsbHotplugHandler if a device is plugged in
	 */
	public void enableLogger() {
		
		closeUsbDeviceHandle();    // maybe another device was already connected?
		buildUsbDeviceHandle();
		
		this.logger.enable();
		
	}
	
	/**
	 * Called by UsbHotplugHandler if a device is unplugged
	 */
	public void disableLogger() {
		
		closeUsbDeviceHandle();
		
		this.logger.disable();
		
	}
	
	/**
	 * Called once the preferences changes
	 */
	public void activatedPreferencesChanged(final boolean activated) {
		
		try {
			
			if (activated) {
				
				startUsb();
				
			} else {
				
				stopUsb();
				
			}
			
		} catch (Exception e) {
			
			this.getLog().log(new Status(Status.ERROR, PLUGIN_ID,
					"Could not start/stop usb service", e));
			
		}

	}

	/**
	 * Try to build a usb device handle if plugged in
	 * 
	 * @return The usb device handle
	 */
	public boolean buildUsbDeviceHandle() {

		// find device with USBasp's idVendor and idProduct
		usbaspDeviceHandle = LibUsb.openDeviceWithVidPid(usbContext, USBasp_idVendor, USBasp_idProduct);
		if (usbaspDeviceHandle == null) {
			return false;
		}
		
		// detach any system driver if attached
		boolean supportsDetachKernelDriver = LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER);
		if (supportsDetachKernelDriver) {
			int detach = LibUsb.kernelDriverActive(usbaspDeviceHandle, USBasp_interface);

			// Detach the kernel driver
			if (detach != 0) {
			    int result = LibUsb.detachKernelDriver(usbaspDeviceHandle, USBasp_interface);
			    if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to detach kernel driver", result);
			    usbDetachedFromSystemDriver = true;
			}
		}
		
		// claim the interface
		int result = LibUsb.claimInterface(usbaspDeviceHandle, USBasp_interface);
		if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to claim interface "
				+ USBasp_interface, result);
		
		// set baud rate
		final int baudRate = getBaudRateByPreferences();
		setBaudRate(baudRate);
		
		return true;
		
	}

	/**
	 * close usb device handle
	 */
	public void closeUsbDeviceHandle() {
		
		if (usbaspDeviceHandle == null) {
			return;
		}
		
		// unclaim the interface
		int result = LibUsb.releaseInterface(usbaspDeviceHandle, USBasp_interface);
		if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to release interface "
				+ USBasp_interface, result);

		// attach system driver if it was previously detached
		if (usbDetachedFromSystemDriver) {
			result = LibUsb.attachKernelDriver(usbaspDeviceHandle, USBasp_interface);
		    if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to re-attach kernel driver", result);
		}
		
		// close device handle
		if (usbaspDeviceHandle != null) {
		    LibUsb.close(usbaspDeviceHandle);
			usbaspDeviceHandle = null;
		}
		
	}
	
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static USBaspConsoleActivator getDefault() {
		
		return plugin;
		
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
		
	}

	/**
	 * Set the baud rate used for communication
	 */
	public void setBaudRate(final int baudRate) {
		
		short param1 = (short) baudRate;
		short param2 = (short) (baudRate >> 16);
		
		final ByteBuffer dataBuf = ByteBuffer.allocateDirect(100);

		//((short) baudBuf[1]) << 8 | baudBuf[0]),
		//((short) baudBuf[3]) << 8 | baudBuf[2]),

		final int result = LibUsb.controlTransfer(
				usbaspDeviceHandle,
				(byte) (LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_DEVICE | LibUsb.ENDPOINT_IN),
				USBASP_FUNC_UART_SETBAUDRATE,
				param1,
				param2,
				dataBuf,
				5000);
		
		if (result < 0) {
			throw new RuntimeException(LibUsb.strError(result));
		}
		
	}

	/**
	 * Run a simple USBasp+ command
	 * 
	 * @param cmd The command
	 * @return The result
	 */
	private Integer simpleCmd(byte cmd) {
		
		final ByteBuffer dataBuf = ByteBuffer.allocateDirect(4);
		
		final int bytesRead = LibUsb.controlTransfer(
				usbaspDeviceHandle,
				(byte) (LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_DEVICE | LibUsb.ENDPOINT_IN),
				cmd,
				(short) cmd,
				(short) 0,
				dataBuf,
				5000);
		
		if (bytesRead < 0) {
			throw new RuntimeException(LibUsb.strError(bytesRead));
		}
		
		if (bytesRead == 0) {
			return null;
		}
		
		return (int) dataBuf.get(0);
		
	}
	
	/**
	 * Run test-command which forces the device to blink
	 */
	public int testCmd1() {

		return simpleCmd(USBASP_FUNC_TEST_CMD1);
				
	}
	
	public int testCmd2() {

		return simpleCmd(USBASP_FUNC_TEST_CMD2);
				
	}

	public int testCmd3() {

		return simpleCmd(USBASP_FUNC_TEST_CMD3);
				
	}

	/**
	 * Retrieve the number of bytes available (received from connected MCU) 
	 * which are ready to retrieve
	 */
	public int getNumberOfBytesAvailable() {
		
		return simpleCmd(USBASP_FUNC_UART_GETBYTECOUNT);
		
	}
	
	/**
	 * Retrieve one byte (previously received from connected MCU)
	 */
	public Character getByte() {
		
		final Integer byteRead = simpleCmd(USBASP_FUNC_UART_GETBYTE);
		
		if (byteRead == null) {
			return null;
		}
		
		return new Character((char) byteRead.intValue());
		
	}
	
	/**
	 * Get all bytes available as a string
	 */
	public String getString() {
		
		int numberOfBytesAvailable = getNumberOfBytesAvailable();
		if (numberOfBytesAvailable == 0) {
			return null;
		}
		
		final StringBuffer result = new StringBuffer(numberOfBytesAvailable);
		
		while (numberOfBytesAvailable > 0) {
			
			final Character charRead = getByte();
			if (charRead == null) {
				
				numberOfBytesAvailable = 0; // avoid further access
				
			} else {
				
				result.append(charRead);
				--numberOfBytesAvailable;
				
			}
			
		}
		
		return result.toString();
		
	}
	
	/**
	 * Brings the console to foreground
	 */
	public static void bringConsoleToForeground() {
		
		final ConsolePlugin consolePlugin = ConsolePlugin.getDefault();
		final IConsoleManager consoleManager = consolePlugin.getConsoleManager();
		
		// find the USBasp+ console and show it
		for (final IConsole console : consoleManager.getConsoles()) {
			
			if (console.getName().equals(ConsoleLogger.CONSOLE_NAME)) {
				consoleManager.showConsoleView(console);
				break;
			}
			
		}
		
	}

	/**
	 * Brings the console to foreground
	 */
	public static void warnOfContentChange() {
		
		final ConsolePlugin consolePlugin = ConsolePlugin.getDefault();
		final IConsoleManager consoleManager = consolePlugin.getConsoleManager();
		
		// find the USBasp+ console and show it
		for (final IConsole console : consoleManager.getConsoles()) {
			
			if (console.getName().equals(ConsoleLogger.CONSOLE_NAME)) {
				consoleManager.warnOfContentChange(console);
				break;
			}
			
		}
		
	}
	
}
