package com.raspelikan.usbaspconsole;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

/**
 * @author RasPelikan
 */
public class PreferencesPage extends PreferencePage implements
		IWorkbenchPreferencePage, SelectionListener, ModifyListener {

	private static final String CONFIG_TITLE = "Configuration";
	private static final String ACTIVATE_TITLE = "&Active:";
	private static final String BAUDRATE_TITLE = "&Baud rate:";
	private static final String TEST_TITLE = "Test USBasp+";
	private static final String CMD1_TITLE = "CMD&1";

	private Button activateButton;
	private Text baudRateText;
	private Button testButton;
	
	private void addSection(Composite composite) {

		final IPreferenceStore preferenceStore = doGetPreferenceStore();

		Group configGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		configGroup.setText(CONFIG_TITLE);
		applyLayout(configGroup);
		
		Label activateLabel = new Label(configGroup, SWT.NONE);
		activateLabel.setText(ACTIVATE_TITLE);

		activateButton = new Button(configGroup, SWT.CHECK);
		activateButton.setText("Search for USBasp+ devices and write their "
				+ "serial communication to the Console");

		final boolean activated = preferenceStore.getBoolean(USBaspConsoleActivator.ACTIVATED_PROPERTY);
		activateButton.setSelection(activated);

		Label baudrateLabel = new Label(configGroup, SWT.NONE);
		baudrateLabel.setText(BAUDRATE_TITLE);
		
		baudRateText = new Text(configGroup, SWT.SHADOW_ETCHED_IN);
		baudRateText.addListener(SWT.Verify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				final String newText = event.text;
				if (newText != null) {
					for (int i = 0; i < newText.length(); ++i) {
						char c = newText.charAt(i);
						if (!Character.isDigit(c)) {
							event.doit = false;
							return;
						}
					}
				}
			}
		});
		GridDataFactory.generate(baudRateText, baudRateText.computeSize(100, baudRateText.getSize().y));
		final int baudRate = preferenceStore.getInt(USBaspConsoleActivator.BAUDRATE_PROPERTY);
		baudRateText.setText(Integer.toString(baudRate));

		Group testGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
		testGroup.setText(TEST_TITLE);
		
		RowLayout layout = new RowLayout();
		layout.type = SWT.VERTICAL;
		testGroup.setLayout(layout);
		
		Label testLabel = new Label(testGroup, SWT.NONE);
		testLabel.setText("Use this button to test whether USBasp+ firmware is "
				+ "installed on the device you plugged in. If you push the button "
				+ "the LEDs of your USBasp device should blink. If not you might "
				+ "not have updated successfully.");
		
		testButton = new Button(testGroup, SWT.NONE);
		testButton.setEnabled(activated);
		testButton.setText(CMD1_TITLE);
		testButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event pushEvent) {

				final USBaspConsoleActivator plugin = USBaspConsoleActivator.getDefault();
				try {
					plugin.testCmd1();
				} catch (Exception e) {
					final String msg = "Could not run test command 'CMD1'! "
							+ "Maybe the device is not connected.";
					plugin.getLog().log(new Status(Status.ERROR, USBaspConsoleActivator.PLUGIN_ID,
							msg, e));
					MessageDialog.openWarning(
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
							"USBasp+ Console", msg);
				}
				
			}
		});
		
		Label hintLabel = new Label(testGroup, SWT.NONE);
		hintLabel.setText("Hint: If the active checkbox is not selected the button is "
				+ "disabled. If you selected the checkbox you have to confirm "
				+ "by pressing 'OK' or 'Apply' before the button becomes enabled.");

	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		addSection(composite);

		initializeValues();
		
		return composite;

	}

	private void applyLayout(Composite composite) {
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);
		
	}
	
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
        return USBaspConsoleActivator.getDefault().getPreferenceStore();
    }
	
	private void initializeDefaults() {
		activateButton.setSelection(USBaspConsoleActivator.ACTIVATED_DEFAULT);
	}
	
	/**
     * Initializes states of the controls from the preference store.
     */
    private void initializeValues() {
        IPreferenceStore store = getPreferenceStore();
		activateButton.setSelection(store.getBoolean(USBaspConsoleActivator.ACTIVATED_PROPERTY));
    }
	
	protected void performDefaults() {
		super.performDefaults();
		initializeDefaults();
	}

	@Override
	protected void performApply() {
		
		performOk();
		
	}
	
	public boolean performOk() {
		
		IPreferenceStore store = doGetPreferenceStore();
		
		final boolean activated = activateButton.getSelection();
		testButton.setEnabled(activated);
		
		store.setValue(USBaspConsoleActivator.ACTIVATED_PROPERTY, activated);
		USBaspConsoleActivator.getDefault().activatedPreferencesChanged(activated);
		
		return true;
	}

	@Override
	public void init(IWorkbench workbench) {
        // do nothing
    }
	
	@Override
	public void modifyText(ModifyEvent event) {
        // do nothing
    }
	
	@Override
	public void widgetDefaultSelected(SelectionEvent event) {
        // do nothing
    }

    @Override
	public void widgetSelected(SelectionEvent event) {
        // do nothing
    }
    
}
