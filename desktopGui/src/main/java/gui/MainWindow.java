package gui;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import events.NextSong;
import events.PlayPauseEvent;
import events.PlayIntent;
import events.StopIntent;
import events.PlayStopped;
import events.PlayStarted;
import events.ProgressEvent;
import events.PrevSong;

import com.github.nikit.cpp.player.PlayList;
import com.github.nikit.cpp.player.Song;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import config.Config;
import events.DownloadEvent;
import service.DownloadService;
import service.DownloadServiceException;
import service.PlayerService;
import vk.VkPlayListBuilder;
import vk.VkPlayListBuilderException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.GridLayout;

public class MainWindow extends JFrame {
	
	private static final String SPRING_CONFIG = "spring-config.xml";
	private static final String STOPPED = "Stopped";
	private static Config config;
	private static VkPlayListBuilder playlistBuilder;
	private static EventBus eventBus;
	private static PlayerService playerService;
	private static DownloadService downloadService;
	private static Logger LOGGER = Logger.getLogger(MainWindow.class);
	private static final long serialVersionUID = 1L;
	private JList<Song> songsList;
	private JPanel contentsPanel;
	private JLabel statusLabel;
	static MainWindow instance = null;
	private JButton btnPrev;
	private JButton btnPlay;
	private JPanel controlPanel;
	private JButton btnNext;
	private JButton btnStop;
	private SelectedListCellRenderer listRenderer;
	private JPanel sliderPanel;
	private JPanel buttonsPanel;
	private JSlider slider;
	private JMenuBar menuBar;
	private JMenu menuFile;
	private JMenu mnNewMenu_1;
	private JMenuItem mntmNewMenuItem;
	private JSplitPane splitPane;
	private JScrollPane scrollRightPane;
	private JScrollPane scrollLeftPane;
	private JLabel imageLabel;
	private Hilighter hilighter;
	
	private static final String PLAY = "Play";
	private static final String PAUSE = "Pause";

	public MainWindow() throws ParserConfigurationException, VkPlayListBuilderException {
		initNonGui();
		eventBus.register(this);
		instance = this;
		hilighter = new Hilighter();
		setTitle("Vk Caching Player");
		setSize(600, 500);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		
		Collection<PlayList> cpl = new ArrayList<PlayList>();
		for (String groupName : config.getGroupNames()){
			cpl.addAll(playlistBuilder.getPlayListsFromGroup(groupName));
		}
		List<Song> data = new ArrayList<Song>();
		for (PlayList pl: cpl) {
			for (Song s : pl.getSongs()) {
				data.add(s);
			}
		}
		PlayList playList = new PlayList(data);
		if(config.getSearchFileOnDisk()){
			downloadService.updateFilesInPlayList(playList);
		}
		playerService.setPlayList(playList);
		
		final PlayListListModel playListModel = new PlayListListModel(playList);
		listRenderer = new SelectedListCellRenderer();

		
		contentsPanel = new JPanel();

		getContentPane().add(contentsPanel);
		contentsPanel.setLayout(new BorderLayout(0, 0));
		
		controlPanel = new JPanel();
		contentsPanel.add(controlPanel, BorderLayout.SOUTH);
		controlPanel.setLayout(new GridLayout(0, 1, 0, 0));
		
		sliderPanel = new JPanel();
		controlPanel.add(sliderPanel);
		sliderPanel.setLayout(new GridLayout(0, 1, 0, 0));
		
		slider = new JSlider();
		slider.setValue(0);
		sliderPanel.add(slider);

		
		buttonsPanel = new JPanel();
		controlPanel.add(buttonsPanel);
		
		btnPrev = new JButton("Prev");
		btnPrev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				eventBus.post(new PrevSong());
			}
		});
		buttonsPanel.add(btnPrev);
		
		btnPlay = new JButton("Play");
		btnPlay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				eventBus.post(new PlayPauseEvent());
			}
		});
		buttonsPanel.add(btnPlay);
		
		btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				eventBus.post(new StopIntent());
			}
		});
		buttonsPanel.add(btnStop);
		
		btnNext = new JButton("Next");
		buttonsPanel.add(btnNext);
				
		songsList = new JList<Song>(playListModel);
		songsList.setCellRenderer(listRenderer);
		songsList.addMouseListener(new MouseListener() {

			public void mouseReleased(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int index = songsList.locationToIndex(e.getPoint());
					Song s = (Song) playListModel.getElementAt(index);
					eventBus.post(new PlayIntent(s));

					LOGGER.debug("Double clicked on item " + index + " " + s);

				}
			}
		});
		imageLabel = new JLabel();
		scrollRightPane = new JScrollPane(songsList);
		scrollLeftPane = new JScrollPane(imageLabel);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollLeftPane, scrollRightPane);

		contentsPanel.add(splitPane, BorderLayout.CENTER);
				
		btnNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				eventBus.post(new NextSong());
			}
		});
		
		
		// create the status bar panel and shove it down the bottom of the frame
		JPanel statusPanel = new JPanel();
		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		getContentPane().add(statusPanel, BorderLayout.SOUTH);
		statusPanel.setPreferredSize(new Dimension(600, 16));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		statusLabel = new JLabel(STOPPED);
		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusPanel.add(statusLabel);
		
		menuBar = new JMenuBar();
		getContentPane().add(menuBar, BorderLayout.NORTH);
		
		menuFile = new JMenu("File");
		menuBar.add(menuFile);
		
		mntmNewMenuItem = new JMenuItem("New menu item");
		menuFile.add(mntmNewMenuItem);
		
		mnNewMenu_1 = new JMenu("New menu");
		menuBar.add(mnNewMenu_1);
	}
	public static void main(String[] args) throws ParserConfigurationException, VkPlayListBuilderException {
		
		MainWindow mainFrame = new MainWindow();
		mainFrame.setVisible( true );
	}
	
	public static MainWindow getInstance(){
		return instance ;
	}
	
	private void initNonGui(){
		// Non-GUI work
		ApplicationContext context = new ClassPathXmlApplicationContext(SPRING_CONFIG);

		playlistBuilder = (VkPlayListBuilder)context.getBean("vkPlaylistBuilder");
	    config = (Config)context.getBean("config");
	    eventBus = (EventBus) context.getBean("eventBus");
	    downloadService = (DownloadService) context.getBean("downloader");
	    playerService = (PlayerService) context.getBean("playerService");
	    eventBus.register(downloadService);
	    eventBus.register(playerService);
	}
	
	@AllowConcurrentEvents
	@Subscribe
	public void onPlayStarted(final PlayStarted e) throws DownloadServiceException {
		final String s = e.getSong().getFile().getAbsolutePath();
		final String message = "Playing '" + s + "'";
		LOGGER.debug(message);

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				btnPlay.setText(PAUSE);
				statusLabel.setText(message);
			}
		});
	}
	
	@AllowConcurrentEvents
	@Subscribe
	public void onPlayStopped(PlayStopped e){
		LOGGER.debug("Play stopped");
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				hilighter.resetPlaying();
				listRenderer.setHilighted(hilighter.getHilighted());
				songsList.updateUI();
				statusLabel.setText(STOPPED);
				btnPlay.setText(PLAY);
				slider.setValue(0);
			}
		});
	}

	@AllowConcurrentEvents
	@Subscribe
	public void onDownload(final DownloadEvent e) throws DownloadServiceException {
		final String message = "Downloading '" + e.getSong().getUrl() + "'";
		LOGGER.debug(message);
		final int index = playerService.getPlayList().getSongId(e.getSong().getId());
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				statusLabel.setText(message);
				
				hilighter.addHilightDownload(index);
				listRenderer.setHilighted(hilighter.getHilighted());
				songsList.updateUI();
				

			}
		});
	}
		
	
	@AllowConcurrentEvents
	@Subscribe
	public void onPlaying(final ProgressEvent playedProgress){
		SwingUtilities.invokeLater(new Runnable() {
			final Song song = playedProgress.getSong();
			final int index = playerService.getPlayList().getSongId(song.getId());

			@Override
			public void run() {
				int songLengh = playerService.getSongMaxSize();
				slider.setValue(songLengh - playedProgress.getAvailable());
				slider.setMaximum(songLengh);
				
				hilighter.addPlaying(index);
				listRenderer.setHilighted(hilighter.getHilighted());
				songsList.updateUI();
				statusLabel.setText("Playing " + song.getFile());
				
				try {
					if(playedProgress.getSong().getImage()!=null){
						ImageIcon icon;

			            byte[] imageBytes = playedProgress.getSong().getImage();
						BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
			            icon = new ImageIcon(img);

						imageLabel.setIcon(icon);
						imageLabel.updateUI();
					}
					
				} catch (Exception e) {
					LOGGER.error("Error on downloading image", e);
				}
				imageLabel.updateUI();
			}
		});
	}
	
	@AllowConcurrentEvents
	@Subscribe
	public void onPaused(PlayPauseEvent e){
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				if(playerService.getPaused()==true)
					btnPlay.setText(PLAY);
				else
					btnPlay.setText(PAUSE);
			}
		});
		
	}
	
	public static EventBus getEventBus() {
		return eventBus;
	}
	public static PlayerService getPlayerService() {
		return playerService;
	}
}


class PlayListListModel extends AbstractListModel<Song> {
	private static final long serialVersionUID = 1L;
	// здесь будем хранить данные
	private List<Song> data = new ArrayList<Song>();

	public PlayListListModel(PlayList playList){
		setDataSource(playList);
	}
	private void setDataSource(PlayList playList) {
		try {
			// получаем данные
			data.clear();
			
			synchronized (data) {
				data = playList.getSongs();
			}
			// оповещаем виды (если они есть)
			fireIntervalAdded(this, 0, data.size());
			fireContentsChanged(this, 0, data.size());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	// методы модели для выдачи данных списку
	public int getSize() {
		synchronized (data) {
			return data.size();
		}
	}

	public Song getElementAt(int idx) {
		synchronized (data) {
			return data.get(idx);
		}
	}
}

class SelectedListCellRenderer extends DefaultListCellRenderer {
	private static final long serialVersionUID = 1L;

	private List<HilightItem> items;
	public void setHilighted(List<HilightItem> items){
		this.items = items;
	}
	
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if(items!=null){
	        for(HilightItem i: items){
	        	int hilighted = i.getHilighted();
	        	Color color = i.getColor();
		        if (index==hilighted && color!=null) {
		            c.setBackground(color);
		        }
	        }
        }
        if(value instanceof Song){
        	Song song = (Song) value;
        	if(song.getFile()==null){
        		c.setForeground(Color.GRAY);
        	}
        }
        return c;
    }
}

class HilightItem{
	public HilightItem(int hilighted, Color color) {
		super();
		this.hilighted = hilighted;
		this.color = color;
	}
	private int hilighted = -1;
	private Color color = null;
	public int getHilighted() {
		return hilighted;
	}
	public Color getColor() {
		return color;
	}

}

class Hilighter{
	private List<HilightItem> items = new ArrayList<HilightItem>();
	private Set<Integer> playing = new HashSet<Integer>();

	public void resetPlaying() {
		items.clear();
	}
	
	public void addPlaying(int index) {
		items.add(new HilightItem(index, Color.GREEN));
		playing.add(index);
	}

	public List<HilightItem> getHilighted() {
		return items;
	}

	public void addHilightDownload(int index) {
		items.add(new HilightItem(index, Color.BLUE));
	}

	public void resetDownloaded() {
		System.err.println("Fixme!");
	}
}