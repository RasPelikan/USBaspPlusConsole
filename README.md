# USBasp+ Console
An Eclipse plugin showing UART communication in Console View by using USBasp+ devices.

Update-Site: http://raspelikan.github.io/USBaspPlusConsole

# Details
USBasp is a AVR-programmer developed by Thomas Fischl (see http://www.fischl.de/usbasp/). Fortunately Thomas Fischl added lines for UART communication to his circuit but the firmware does not support this feature. This is the reason why my chinese clone (bought at the guys who are making extreme deals) is wired in this way.
emklaus extended the firmware of the USBasp device (now called "USBasp+") for supporting UART communication
(see http://community.atmel.com/projects/usbasp-tty-usbasp-programmer-modified-serial-support-and-terminal-program). Unfortunately - as far as I understood - USBasp is a low speed USB device which cannot be a serial device per definition. This is the reason why emklaus added a small Windows program USBASP_tty.exe which acts as a terminal. USBasp+ uses a proprietary protocol to transfer the UART data between the USB device and the Windows program.

# About the plugin
The plugin adds another console view (called "USBasp+ Console") which outputs data received form the USBasp+ device. This is very convenient for developing since you don't have to connect another USB device to your circuit for receiving UART data.

Using the plugin's preferences page you can disable receiving UART data (it is disabled by default, so first you have to enable it). Additionally you can set the baud rate used for communication. There is also a button which fires a test command so this button can be used to test whether your firmware upgrade completed successfully.

The plugin contains native code provided by usb4java for the platforms osx/x86, osx/x86_64, windows/x86, windows/x86_64, linux/x86, linux/x86_64 and linux/arm. I use it on OSX but it should even run on a Raspberry Pi.

Hint: At the moment the plugin only supports printing data sent by your MCU. In the near future I will add the ability to sent data back to the MCU.

# KUDOs
Many thanks to the guys of the projects usb4java, USBasp+ and USBasp. I appreciate the work you've done!
