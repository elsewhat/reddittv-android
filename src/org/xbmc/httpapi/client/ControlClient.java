/*
 *      Copyright (C) 2005-2009 Team XBMC
 *      http://xbmc.org
 *
 *  This Program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2, or (at your option)
 *  any later version.
 *
 *  This Program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with XBMC Remote; see the file license.  If not, write to
 *  the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *  http://www.gnu.org/copyleft/gpl.html
 *
 */

package org.xbmc.httpapi.client;

import org.xbmc.api.business.INotifiableManager;
import org.xbmc.api.object.Host;
import org.xbmc.httpapi.Connection;
import org.xbmc.httpapi.WrongDataFormatException;

/**
 * The ControlClient class takes care of everything related to controlling
 * XBMC. These are essentially play controls, navigation controls other actions
 * the user may wants to execute. It equally reads the information instead of
 * setting it.
 * 
 * @author Team XBMC
 */
public class ControlClient{

	private final Connection mConnection;

	/**
	 * Class constructor needs reference to HTTP client connection
	 * @param connection
	 */
	public ControlClient(Connection connection) {
		mConnection = connection;
	}
	
	/**
	 * Updates host info on the connection.
	 * @param host
	 */
	public void setHost(Host host) {
		mConnection.setHost(host);
	}
	
	
	/**
	 * Starts playing the media file <code>filename</code> .
	 * @param manager Manager reference
	 * @param filename File to play
	 * @return true on success, false otherwise.
	 */
	public boolean playFile(INotifiableManager manager, String filename) {
		return mConnection.getBoolean(manager, "PlayFile", filename);
	}
	
	/**
	 * Starts playing/showing the next media/image in the current playlist or,
	 * if currently showing a slideshow, the slideshow playlist. 
	 * @param manager Manager reference
	 * @return true on success, false otherwise.
	 */
	public boolean playNext(INotifiableManager manager) {
		return mConnection.getBoolean(manager, "PlayNext");
	}

	/**
	 * Starts playing/showing the previous media/image in the current playlist
	 * or, if currently showing a slidshow, the slideshow playlist.
	 * @param manager Manager reference
	 * @return true on success, false otherwise.
	 */
	public boolean playPrevious(INotifiableManager manager) {
		return mConnection.getBoolean(manager, "PlayPrev");
	}
	
	/**
	 * Pauses the currently playing media. 
	 * @param manager Manager reference
	 * @return true on success, false otherwise.
	 */
	public boolean pause(INotifiableManager manager) {
		return mConnection.getBoolean(manager, "Pause");
	}
	
	/**
	 * Stops the currently playing media. 
	 * @param manager Manager reference
	 * @return true on success, false otherwise.
	 */
	public boolean stop(INotifiableManager manager) {
		return mConnection.getBoolean(manager, "Stop");
	}
	
	/**
	 * Start playing the media file at the given URL
	 * @param manager Manager reference
	 * @param url An URL pointing to a supported media file
	 * @return true on success, false otherwise.
	 */
	public boolean playUrl(INotifiableManager manager, String url) {
		return mConnection.getBoolean(manager, "ExecBuiltin", "PlayMedia(" + url + ")");
	}
	
	
	
	/**
	 * Returns the current playlist identifier
	 * @param manager Manager reference
	 */
	public int getPlaylistId(INotifiableManager manager) {
		return mConnection.getInt(manager, "GetCurrentPlaylist");
	}
	
	/**
	 * Sets the current playlist identifier
	 * @param manager Manager reference
	 * @param id Playlist identifier
	 * @return True on success, false otherwise.
	 */
	public boolean setPlaylistId(INotifiableManager manager, int id) {
		return mConnection.getBoolean(manager, "SetCurrentPlaylist", String.valueOf(id));
	}
	
	/**
	 * Sets the current playlist position
	 * @param manager Manager reference
	 * @param position New playlist position
	 * @return True on success, false otherwise.
	 */
	public boolean setPlaylistPos(INotifiableManager manager, int position) {
		return mConnection.getBoolean(manager, "SetPlaylistSong", String.valueOf(position));
	}
	
	/**
	 * Clears a playlist.
	 * @param manager Manager reference
	 * @param int Playlist to clear (0 = music, 1 = video)
	 * @return True on success, false otherwise.
	 */
	public boolean clearPlaylist(INotifiableManager manager, String playlistId) {
		return mConnection.getBoolean(manager, "ClearPlayList", playlistId);
	}
	
	/**
	 * Sets current playlist
	 * @param manager Manager reference
	 * @param playlistId Playlist ID ("0" = music, "1" = video)
	 * @return True on success, false otherwise.
	 */
	public boolean setCurrentPlaylist(INotifiableManager manager, String playlistId) {
		return mConnection.getBoolean(manager, "SetCurrentPlaylist", playlistId);
	}
	
	/**
	 * Sets the correct response format to default values
	 * @param manager Manager reference	 
	 * @return True on success, false otherwise.
	 */
	public boolean setResponseFormat(INotifiableManager manager) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("WebHeader;true;");
			sb.append("WebFooter;true;");
			sb.append("Header; ;");
			sb.append("Footer; ;");
			sb.append("OpenTag;");sb.append(Connection.LINE_SEP);sb.append(";");
			sb.append("CloseTag;\n;");
			sb.append("CloseFinalTag;false");
			mConnection.assertBoolean(manager, "SetResponseFormat", sb.toString());
			
			sb = new StringBuilder();
			sb.append("OpenRecordSet; ;");
			sb.append("CloseRecordSet; ;");
			sb.append("OpenRecord; ;");
			sb.append("CloseRecord; ;");
			sb.append("OpenField;<field>;");
			sb.append("CloseField;</field>");
			mConnection.assertBoolean(manager, "SetResponseFormat", sb.toString());
			
			return true;
		} catch (WrongDataFormatException e) {
			return false;
		}
	}

	
	

}
