package com.raspelikan.usbaspconsole;


/**
 * Summarize USBasp+ functionality
 * 
 * @author RasPelikan
 */
public interface USBaspDevice {

	/**
	 * Retrieve the number of bytes available (received from connected MCU) 
	 * which are ready to retrieve
	 * 
	 * @return The number of bytes
	 */
	int getNumberOfBytesAvailable();
	
	Character getByte();
	
	String getString();

	/**
	 * Used to set the baud rate
	 * 
	 * @param deviceHandle
	 * @param baudRate
	 */
	void setBaudRate(final int baudRate);
	
	/**
	 * Test command
	 * 
	 * @return test result
	 */
	int testCmd1();
	
	/**
	 * Test command
	 * 
	 * @return test result
	 */
	int testCmd2();
	
	/**
	 * Test command
	 * 
	 * @return test result
	 */
	int testCmd3();
	
}
