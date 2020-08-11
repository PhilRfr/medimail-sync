package fr.philr.medimail.gui;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.security.auth.login.CredentialException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;

import fr.philr.medimail.api.MedimailSession;
import fr.philr.medimail.pop3access.FolderAsPOPBox;
import fr.philr.medimail.pop3access.POP3ClientSession;
import fr.philr.medimail.pop3access.POP3Server;
import fr.philr.medimail.pop3access.POP3ServerFactory;
import fr.philr.medimail.pop3access.POP3Session;
import fr.philr.medimail.pop3access.ProtocolSession;
import fr.philr.medimail.pop3access.ProtocolSessionFactory;
import fr.philr.medimail.pop3access.ProtocolSessionRegister;
import fr.philr.medimail.sync.FileSystemSync;

public class MainWindow {

	private JButton btnPickFolder;
	private JFrame frmPasserellePopMedimail;
	private JTextField textUser;
	private JPasswordField textPassword;
	private JTextField textPop;
	private JTextField textFolder;
	private JFileChooser chooser;
	private Timer timer;

	private String username;
	private String password;
	private int popPort;
	private Path folderPath;
	static File propFile = new File("medimail.properties");

	private void loadSavedProps() {
		Properties props = new Properties();
		if (Files.exists(propFile.toPath())) {
			try {
				props.load(new FileInputStream(propFile));
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
		}
		props.setProperty("login", props.getProperty("login", "utilisateur"));
		props.setProperty("password", props.getProperty("password", "motdepasse"));
		props.setProperty("popPort", props.getProperty("popPort", "8989"));
		props.setProperty("folderPath", props.getProperty("folderPath", System.getProperty("user.dir")));
		try {
			props.store(new FileOutputStream(propFile), "Passerelle MediMail");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		username = props.getProperty("login");
		password = props.getProperty("password");
		try {
			popPort = Integer.parseInt(props.getProperty("popPort"));
		} catch (NumberFormatException e) {
			popPort = 0;
		}
		folderPath = Paths.get(props.getProperty("folderPath"));

		textUser.setText(username);
		textPassword.setText(password);
		textPop.setText(Integer.toString(popPort));
		textFolder.setText(folderPath.toString());
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frmPasserellePopMedimail.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void chooseDirectory() {
		if (chooser.showOpenDialog(btnPickFolder) == JFileChooser.APPROVE_OPTION) {
			setDirectory(chooser.getSelectedFile().getAbsolutePath());
		}

	}

	private void setDirectory(String directory) {
		folderPath = Paths.get(directory);
		textFolder.setText(directory);
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		initialize();
	}

	private ProtocolSessionRegister reg;
	private TimerTask fsSync;

	private Thread popThread;

	public void startGateway() {
		timer = new Timer();
		fsSync = new TimerTask() {
			@Override
			public void run() {
				MedimailSession session;
				try {
					session = new MedimailSession(username, password);
					FileSystemSync.sync(folderPath.toAbsolutePath().toString(), session);
				} catch (CredentialException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		timer.schedule(fsSync, 0, 300000);
		popThread = new Thread(new Runnable() {

			@Override
			public void run() {
				POP3Server serv = new POP3Server(popPort, new POP3ServerFactory() {

					@Override
					public void handleConnection(Socket clientSocket) {
						POP3ClientSession newSession = new POP3ClientSession(clientSocket);
						newSession.attachBox(new FolderAsPOPBox(username, password,
								Paths.get(folderPath.toString(), "new"), Paths.get(folderPath.toString(), "read")));
						newSession.launch();
					}
				});
				serv.run();
			}
		});
		popThread.start();
	}

	public void stopGateway() {
		reg.cancelAll();
		timer.cancel();
		timer.purge();
		popThread.interrupt();
	}

	private void updateVars() {
		username = textUser.getText();
		password = new String(textPassword.getPassword());
		try {
			popPort = Integer.parseInt(textPop.getText());
		} catch (NumberFormatException e) {
			popPort = 0;
		}
		folderPath = Paths.get(textFolder.getText());

		Properties props = new Properties();
		props.setProperty("login", username);
		props.setProperty("password", password);
		props.setProperty("popPort", Integer.toString(popPort));
		props.setProperty("folderPath", folderPath.toString());
		try {
			props.store(new FileOutputStream(propFile), "Passerelle MediMail");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean checkVars() {
		if (popPort <= 0) {
			JOptionPane.showMessageDialog(btnPickFolder, "Le port ne peut pas être inférieur à 0.");
			return false;
		}
		// Test login & PW
		{
			try {
				new MedimailSession(username, password);
			} catch (CredentialException e) {
				JOptionPane.showMessageDialog(btnPickFolder, "Informations de connexion incorrectes.");
				return false;
			}

		}
		// Test folder
		if (!Files.isDirectory(folderPath)) {
			JOptionPane.showMessageDialog(btnPickFolder, "Le chemin indiqué n'est pas un répertoire.");
			return false;
		}
		return true;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmPasserellePopMedimail = new JFrame();
		frmPasserellePopMedimail.setResizable(false);
		frmPasserellePopMedimail.setTitle("Passerelle POP MediMail");
		frmPasserellePopMedimail.setBounds(100, 100, 414, 290);
		frmPasserellePopMedimail.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmPasserellePopMedimail.getContentPane().setLayout(null);

		chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setDialogTitle("Choisir un dossier où seront stockés les messages");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);

		JPanel panelMediMail = new JPanel();
		panelMediMail.setBorder(new TitledBorder(null, "MediMail", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelMediMail.setBounds(12, 12, 416, 80);
		frmPasserellePopMedimail.getContentPane().add(panelMediMail);
		panelMediMail.setLayout(null);

		JLabel lUser = new JLabel("Utilisateur :");
		lUser.setBounds(12, 25, 84, 15);
		panelMediMail.add(lUser);

		textUser = new JTextField();
		lUser.setLabelFor(textUser);
		textUser.setBounds(122, 23, 243, 19);
		panelMediMail.add(textUser);
		textUser.setColumns(10);

		JLabel lPassword = new JLabel("Mot de passe :");
		lPassword.setBounds(12, 52, 115, 15);
		panelMediMail.add(lPassword);

		textPassword = new JPasswordField();
		lPassword.setLabelFor(textPassword);
		textPassword.setBounds(122, 50, 243, 19);
		panelMediMail.add(textPassword);

		JPanel panelPop = new JPanel();
		panelPop.setBorder(
				new TitledBorder(null, "Passerelle POP", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelPop.setBounds(12, 92, 416, 80);
		frmPasserellePopMedimail.getContentPane().add(panelPop);
		panelPop.setLayout(null);

		JLabel lPopPort = new JLabel("Port POP local :");
		lPopPort.setBounds(12, 25, 123, 15);
		panelPop.add(lPopPort);

		textPop = new JTextField();
		lPopPort.setLabelFor(textPop);
		textPop.setBounds(124, 23, 114, 19);
		panelPop.add(textPop);
		textPop.setColumns(10);
		JLabel lblFolder = new JLabel("Dossier local :");
		lblFolder.setBounds(12, 52, 123, 15);
		panelPop.add(lblFolder);

		textFolder = new JTextField();
		textFolder.setBackground(Color.GRAY);
		textFolder.setEditable(false);
		lblFolder.setLabelFor(textFolder);
		textFolder.setBounds(124, 50, 221, 19);
		panelPop.add(textFolder);
		textFolder.setColumns(10);

		btnPickFolder = new JButton("...");
		btnPickFolder.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				chooseDirectory();
			}
		});
		btnPickFolder.setBounds(346, 47, 58, 25);
		panelPop.add(btnPickFolder);

		JToggleButton tglGateway = new JToggleButton("Activer la passerelle");
		tglGateway.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (tglGateway.isSelected()) {
					updateVars();
					if (checkVars()) {
						startGateway();
					} else {
						tglGateway.setSelected(false);
					}
				} else
					stopGateway();
			}
		});
		tglGateway.setBounds(111, 184, 202, 25);
		frmPasserellePopMedimail.getContentPane().add(tglGateway);

		JLabel lCopy = new JLabel("(c) Philippe ROUSSILLE, 2020. <3 pour Papa et Maman.");
		lCopy.setBounds(12, 229, 392, 15);
		frmPasserellePopMedimail.getContentPane().add(lCopy);

		loadSavedProps();
	}
}
