package service;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import utils.IOHelper;

import com.github.nikit.cpp.player.Song;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import config.Config;
import events.DownloadEvent;
import events.PlayEvent;

public class DownloadService {
	private static Logger LOGGER = Logger.getLogger(DownloadService.class);
	
	private Config config;
	private EventBus eventBus;
	private static final String DOT_EXT = ".mp3";

	@Subscribe
	public void download(DownloadEvent e) throws DownloadServiceException {
		Song s = e.getSong();
		try {
			String filename = s.toString()+DOT_EXT;
			filename = IOHelper.toFileSystemSafeName(filename);
			File dest = new File(config.getCacheFolder(), filename);
			String url = s.getUrl();
			LOGGER.debug("Downloading "+ url +" to " + dest);
			FileUtils.copyURLToFile(new URL(url), dest);
			LOGGER.debug("Downloading complete ");
			
			LOGGER.debug("Sending PlayEvent ");
			eventBus.post(new PlayEvent(dest.getAbsolutePath()));
		} catch (IOException e1) {
			String message = "Error on downloading";
			LOGGER.error(message, e1);
			throw new DownloadServiceException(message, e1);
		}
	  }

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	public EventBus getEventBus() {
		return eventBus;
	}

}
