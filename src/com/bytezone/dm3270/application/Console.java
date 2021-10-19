package com.bytezone.dm3270.application;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.prefs.Preferences;

import com.bytezone.dm3270.display.Screen;
import com.bytezone.dm3270.display.ScreenDimensions;
import com.bytezone.dm3270.plugins.PluginsStage;
import com.bytezone.dm3270.session.Session;
import com.bytezone.dm3270.streams.TelnetState;
import com.bytezone.dm3270.utilities.Dm3270Utility;
import com.bytezone.dm3270.utilities.Site;
import com.bytezone.dm3270.utilities.WindowSaver;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Console extends Application {
	private static final int MAINFRAME_EMULATOR_PORT = 5555;
	private static final Site DEFAULT_MAINFRAME = new Site("mainframe", "localhost", MAINFRAME_EMULATOR_PORT, true, 2,
			false, "");

	private Stage primaryStage;
	private Rectangle2D primaryScreenBounds;
	private WindowSaver consoleWindowSaver;
	private WindowSaver spyWindowSaver;

	private Preferences prefs;
	private Screen screen;
	private final ScreenDimensions screenDimensions = new ScreenDimensions(24, 80);
	private ScreenDimensions alternateScreenDimensions;
	private TelnetState telnetState = new TelnetState();

	private OptionStage optionStage;
	private SpyPane spyPane;
	private ConsolePane consolePane;
	private ReplayStage replayStage;
	private MainframeStage mainframeStage;
	private PluginsStage pluginsStage;
	private boolean debug;
	ExecutorService executorService;
	private String host;
	private int port;

	public enum Function {
		SPY, REPLAY, TERMINAL, TEST
	}

	@Override
	public void init() throws Exception {
		super.init();
		ThreadFactory threadFactory = new NamedThreadFactory();
//		ThreadFactory namedThreadFactory = 
//				  new ThreadFactoryBuilder().setNameFormat("my-sad-thread-%d").build();
		executorService = Executors.newFixedThreadPool(5, threadFactory);

    Parameters parameters = this.getParameters();
    List<String> unnamed = parameters.getUnnamed();
    if (unnamed.contains("-debug"))
    	debug = true;
    	
    prefs = Preferences.userNodeForPackage (this.getClass ());
    for (String raw : getParameters ().getRaw ())
      if (raw.equalsIgnoreCase ("-reset"))
        prefs.clear ();

		Map<String, String> named = parameters.getNamed();
		for (Map.Entry<String, String> entry : named.entrySet()) {
			System.out.printf("%-18s : %s%n", entry.getKey(), entry.getValue());
			if (entry.getKey().equalsIgnoreCase("host"))
				host = entry.getValue();
			if (entry.getKey().equalsIgnoreCase("port"))
				port = Integer.parseInt(entry.getValue());
		}

		prefs = Preferences.userNodeForPackage(this.getClass());
		for (String raw : getParameters().getRaw())
			if (raw.equalsIgnoreCase("-reset"))
				prefs.clear();

		if (true) {
			String[] keys = prefs.keys();
			Arrays.sort(keys);
			for (String key : keys)
				System.out.printf("%-18s : %s%n", key, prefs.get(key, ""));
		}
		telnetState = new TelnetState();
		telnetState.setOnCancelled((cancelledEvent) -> {
	    	telnetState.close();
	     });
		telnetState.setOnFailed((failedEvent) -> {
	    	telnetState.close();
	     });
		telnetState.setOnSucceeded((succeededEvent) -> {
	    	telnetState.close();
	     });
		executorService.execute(telnetState);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		primaryStage.setOnCloseRequest(e -> Platform.exit());
		primaryStage.setResizable(false);

		pluginsStage = new PluginsStage(prefs);
		primaryScreenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
		if (false)
			System.out.println(javafx.stage.Screen.getPrimary().getDpi());
		if (true && host != null) {
			if (port <= 0)
				port = 23;
			Site site = new Site(host, host, port, true, 2, false, "");
			setModel(site);
			setConsolePane(createScreen(Function.TERMINAL, site), site);
			consolePane.connect(executorService);
		} else {
			optionStage = new OptionStage(prefs, pluginsStage);
			optionStage.okButton.setOnAction(e -> startSelectedFunction(executorService));
			optionStage.cancelButton.setOnAction(e -> optionStage.hide());
			optionStage.show();
		}

	}

	private void startSelectedFunction(ExecutorService executorService) {
		optionStage.hide();
		String errorMessage = "";

		Optional<Site> optionalServerSite = optionStage.serverSitesListStage.getSelectedSite();
		Optional<Site> optionalClientSite = optionStage.clientSitesListStage.getSelectedSite();

		String optionText = (String) optionStage.functionsGroup.getSelectedToggle().getUserData();
		switch (optionText) {
		case "Replay":
			Path path = Paths.get(optionStage.spyFolder + "/" + optionStage.fileComboBox.getValue());
			if (!Files.exists(path))
				errorMessage = path + " does not exist";
			else
				try {
					Session session = new Session(telnetState, path); // can throw Exception
					alternateScreenDimensions = session.getScreenDimensions();

					Optional<Site> serverSite = optionStage.serverSitesListStage
							.getSelectedSite(session.getServerName());
					if (serverSite.isPresent()) {
						Site site = serverSite.get();
						setConsolePane(createScreen(Function.REPLAY, site), site);
					} else {
						System.out.println("Couldn't find the server site for " + session.getServerName());
						setConsolePane(createScreen(Function.REPLAY, null), null);
					}

					replayStage = new ReplayStage(session, path, prefs, screen);
					replayStage.show();
				} catch (Exception e) {
					e.printStackTrace();
					errorMessage = "Error creating replay window";
				}

			break;

		case "Terminal":
			if (optionalServerSite.isPresent()) {
				Site serverSite = optionalServerSite.get();
				setModel(serverSite);
				setConsolePane(createScreen(Function.TERMINAL, serverSite), serverSite);
				consolePane.connect(executorService);
			} else
				errorMessage = "No server selected";

			break;

		case "Spy":
			if (!optionalServerSite.isPresent())
				errorMessage = "No server selected";
			else if (!optionalClientSite.isPresent())
				errorMessage = "No client selected";
			else {
				Site serverSite = optionalServerSite.get();
				Site clientSite = optionalClientSite.get();
				setSpyPane(createScreen(Function.SPY, null), serverSite, clientSite);
			}

			break;

		case "Test":
			if (!optionalClientSite.isPresent())
				errorMessage = "No client selected";
			else {
				Site clientSite = optionalClientSite.get();
				setSpyPane(createScreen(Function.TEST, null), DEFAULT_MAINFRAME, clientSite);
				mainframeStage = new MainframeStage(telnetState, MAINFRAME_EMULATOR_PORT);
				mainframeStage.show();
				mainframeStage.startServer();
			}

			break;
		}

		if (!errorMessage.isEmpty() && Dm3270Utility.showAlert(errorMessage))
			optionStage.show();
	}

	private void setModel(Site serverSite) {
		int model = serverSite.getModel();
		System.out.println("model: " + model);
		switch (model) {
		case 2:
			alternateScreenDimensions = new ScreenDimensions(24, 80);
			telnetState.setDoDeviceType(2);
			break;
		case 3:
			alternateScreenDimensions = new ScreenDimensions(32, 80);
			telnetState.setDoDeviceType(3);
			break;
		case 4:
			alternateScreenDimensions = new ScreenDimensions(43, 80);
			telnetState.setDoDeviceType(4);
			break;
		case 5:
			alternateScreenDimensions = new ScreenDimensions(27, 132);
			telnetState.setDoDeviceType(5);
		default:
			System.out.println("Invalid model number: " + model);
		}
	}

	// private Optional<Site> findSite (String serverName)
	// {
	// Optional<Site> optionalServerSite =
	// optionStage.serverSitesListStage.getSelectedSite (serverName);
	// return optionalServerSite;
	// }

	private void setConsolePane(Screen screen, Site serverSite) {
		consolePane = new ConsolePane(screen, serverSite, pluginsStage, debug);
		Scene scene = new Scene(consolePane);

		primaryStage.setScene(scene);
		primaryStage.setTitle("dm3270");

		if (screen.getFunction() == Function.TERMINAL) {
			consoleWindowSaver = new WindowSaver(prefs, primaryStage, "Terminal");
			if (!consoleWindowSaver.restoreWindow())
				primaryStage.centerOnScreen();
		} else {
			consoleWindowSaver = new WindowSaver(prefs, primaryStage, "Console");
			if (!consoleWindowSaver.restoreWindow()) {
				primaryStage.setX(0);
				primaryStage.setY(primaryScreenBounds.getMinY() + 100);
			}
		}

		scene.setOnKeyPressed(new ConsoleKeyPress(consolePane, screen));
		scene.setOnKeyTyped(new ConsoleKeyEvent(screen));

		primaryStage.sizeToScene();
		primaryStage.show();
	}

	private void setSpyPane(Screen screen, Site server, Site client) {
		spyPane = new SpyPane(screen, server, client, telnetState, debug);

		primaryStage.setScene(new Scene(spyPane));
		primaryStage.setTitle("Terminal Spy");

		spyWindowSaver = new WindowSaver(prefs, primaryStage, "Spy");
		if (!spyWindowSaver.restoreWindow()) {
			primaryStage.setX(0);
			primaryStage.setY(0);

			double height = primaryScreenBounds.getHeight() - 20;
			primaryStage.setHeight(Math.min(height, 1200));
		}

		primaryStage.show();
		spyPane.startServer();
	}

	@Override
	public void stop() {
		if (mainframeStage != null)
			mainframeStage.disconnect();

		if (spyPane != null)
			spyPane.disconnect();

		if (consolePane != null)
			consolePane.disconnect();

		if (replayStage != null)
			replayStage.disconnect();

		savePreferences();

		if (consoleWindowSaver != null)
			consoleWindowSaver.saveWindow();

		if (spyWindowSaver != null)
			spyWindowSaver.saveWindow();

		if (screen != null)
			screen.close();

		executorService.shutdown();

	}

	private void savePreferences() {
		if (optionStage != null) {
			prefs.put("Function", (String) optionStage.functionsGroup.getSelectedToggle().getUserData());

			if (screen != null) {
				prefs.put("FontName", screen.getFontManager().getFontName());
				prefs.put("FontSize", "" + screen.getFontManager().getFontSize());
			}

			prefs.put("Mode", optionStage.toggleModeMenuItem.isSelected() ? "Release" : "Debug");

			String filename = optionStage.fileComboBox.getValue();
			if (filename != null)
				prefs.put("ReplayFile", filename);

			prefs.put("SpyFolder", optionStage.spyFolder);
			prefs.put("ServerName", optionStage.serverComboBox.getSelectionModel().getSelectedItem());
			prefs.put("ClientName", optionStage.clientComboBox.getSelectionModel().getSelectedItem());
		}
	}

	private Screen createScreen(Function function, Site site) {
		screen = new Screen(screenDimensions, alternateScreenDimensions, prefs, function, pluginsStage, site,
				telnetState);
		return screen;
	}

	public boolean getDebug() {
		return debug;
	}

	public static void main(final String[] arguments) {
		Application.launch(arguments);
	}
}