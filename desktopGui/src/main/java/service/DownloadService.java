package service;


import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import utils.IOHelper;

import com.github.nikit.cpp.player.PlayList;
import com.github.nikit.cpp.player.Song;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import config.Config;
import events.DownloadEvent;
import events.DownloadFinished;

public class DownloadService {
	private static Logger LOGGER = Logger.getLogger(DownloadService.class);
	
	private Config config;
	private EventBus eventBus;
	private static final String DOT_EXT = ".mp3";
	
	private File getDestFile(Song song){
		String filename = song.toString()+DOT_EXT;
		filename = IOHelper.toFileSystemSafeName(filename);
		return new File(config.getCacheFolder(), filename);
	}

	@AllowConcurrentEvents
	@Subscribe
	public void download(DownloadEvent e) throws DownloadServiceException {
		try {
			Song s = e.getSong();
			File dest = getDestFile(s);
			
			String url = s.getUrl();
			LOGGER.debug("Downloading '"+ url +"' to '" + dest +"'");
			if(dest.exists()){
				LOGGER.debug("Downloading skipped because dest file are present.");
			}else{
				FileUtils.copyURLToFile(new URL(url), dest);
				LOGGER.debug("Downloading complete ");
			}
			s.setFile(dest);
			
			if(s.getImageUrl()!=null){
				URL imageUrl = new URL(s.getImageUrl());
			    BufferedImage originalImage=ImageIO.read(imageUrl);
			    ByteArrayOutputStream baos=new ByteArrayOutputStream();
			    ImageIO.write(originalImage, "jpg", baos );
			    byte[] image = baos.toByteArray();
			    s.setImage(image);
			    LOGGER.debug("setting image" + image);
			}
			
			LOGGER.debug("Sending PlayEvent ");
			eventBus.post(new DownloadFinished(s));
		} catch (Exception e1) {
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

	public void updateFilesInPlayList(PlayList playlist){
		for(Song song: playlist.getSongs()){
			File dest = getDestFile(song);
			if(dest.exists()) {
				song.setFile(dest);
			}
		}
	}
}
