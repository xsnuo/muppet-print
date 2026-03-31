package com.xuesinuo.muppet.ui;

import java.awt.Checkbox;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.TextField;
import java.awt.Button;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class MuppetPrinterUi {

	private final Frame frame = new Frame();
	private final Label portTitelLabel = new Label("Web Port:");
	private final Label portLabel = new Label("");
	private final TextField portTextField = new TextField(4);
	private final Label statusLabel = new Label("Status: Stopped");
	private final Button runButton = new Button("Run");
	private final Button stopButton = new Button("Stop");
	private final Button signinLocalPrinterButton = new Button("Signin local printer");
	private final Button signedButton = new Button("Signed");
	private final Label messageLabel = new Label("");
	private final Checkbox autoStartCheckbox = new Checkbox("auto start on boot");

	public MuppetPrinterUi(boolean signinEnable, Image icon, Runnable onRun, Runnable onStop, Runnable onSignin,
			Runnable onSigned, BooleanSupplier autoStartEnabledSupplier, Consumer<Boolean> onAutoStartChanged,
			String defaultPort) {
		frame.setTitle("Muppet Printer");
		if (icon != null) {
			frame.setIconImage(icon);
		}
		frame.setResizable(false);
		frame.setLayout(null);
		frame.setSize(0, 0);
		frame.setVisible(true);

		Insets insets = frame.getInsets();
		int titleBarHeight = insets.top;
		int hfs = getHalfFontSize();

		frame.setVisible(false);
		frame.setSize(80 * hfs, 50 * hfs);

		portTitelLabel.setBounds(2 * hfs, 1 * hfs + titleBarHeight, 12 * hfs, 4 * hfs);
		frame.add(portTitelLabel);

		portLabel.setBounds(14 * hfs, 1 * hfs + titleBarHeight, 14 * hfs, 4 * hfs);
		portLabel.setVisible(false);
		frame.add(portLabel);

		portTextField.setBounds(14 * hfs, 1 * hfs + titleBarHeight, 14 * hfs, 4 * hfs);
		portTextField.setText(defaultPort);
		frame.add(portTextField);

		statusLabel.setBounds(2 * hfs, 6 * hfs + titleBarHeight, 48 * hfs, 4 * hfs);
		frame.add(statusLabel);

		runButton.setBounds(2 * hfs, 11 * hfs + titleBarHeight, 20 * hfs, 4 * hfs);
		runButton.addActionListener(e -> onRun.run());
		frame.add(runButton);

		stopButton.setBounds(24 * hfs, 11 * hfs + titleBarHeight, 20 * hfs, 4 * hfs);
		stopButton.addActionListener(e -> onStop.run());
		frame.add(stopButton);

		if (signinEnable) {
			signinLocalPrinterButton.setBounds(2 * hfs, 16 * hfs + titleBarHeight, 30 * hfs, 4 * hfs);
			signinLocalPrinterButton.addActionListener(e -> onSignin.run());
			frame.add(signinLocalPrinterButton);

			signedButton.setBounds(34 * hfs, 16 * hfs + titleBarHeight, 18 * hfs, 4 * hfs);
			signedButton.addActionListener(e -> onSigned.run());
			frame.add(signedButton);
		}

		autoStartCheckbox.setBounds(2 * hfs, 21 * hfs + titleBarHeight, 28 * hfs, 4 * hfs);
		autoStartCheckbox.setState(autoStartEnabledSupplier.getAsBoolean());
		autoStartCheckbox.addItemListener(e -> onAutoStartChanged.accept(autoStartCheckbox.getState()));
		frame.add(autoStartCheckbox);

		messageLabel.setBounds(2 * hfs, 26 * hfs + titleBarHeight, 76 * hfs, 4 * hfs);
		messageLabel.setForeground(java.awt.Color.RED);
		frame.add(messageLabel);

		frame.setVisible(true);
	}

	private int getHalfFontSize() {
		Font defaultFont = new Label("Loading...").getFont();
		int fontSize = defaultFont == null ? 12 : defaultFont.getSize();
		return fontSize / 2 + fontSize % 2;
	}

	public Frame getFrame() {
		return frame;
	}

	public Label getPortLabel() {
		return portLabel;
	}

	public TextField getPortTextField() {
		return portTextField;
	}

	public Label getStatusLabel() {
		return statusLabel;
	}

	public Label getMessageLabel() {
		return messageLabel;
	}
}
