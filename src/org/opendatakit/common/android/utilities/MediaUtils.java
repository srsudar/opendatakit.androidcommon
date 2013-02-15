/*
 * Copyright (C) 2009-2013 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.common.android.utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

/**
 * Consolidate all interactions with media providers here.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public class MediaUtils {
	private static final String t = "MediaUtils";

	private MediaUtils() {
		// static methods only
	}

	private static String escapePath(String path) {
		String ep = path;
		ep = ep.replaceAll("\\!", "!!");
		ep = ep.replaceAll("_", "!_");
		ep = ep.replaceAll("%", "!%");
		return ep;
	}

	public static final Uri getImageUriFromMediaProvider(Context ctxt, String imageFile) {
		String selection = Images.ImageColumns.DATA + "=?";
		String[] selectArgs = { imageFile };
		String[] projection = { Images.ImageColumns._ID };
		Cursor c = null;
		try {
			c = ctxt.getContentResolver()
					.query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
							projection, selection, selectArgs, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				String id = c.getString(c
						.getColumnIndex(Images.ImageColumns._ID));

				return Uri
						.withAppendedPath(
								android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
								id);
			}
			return null;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public static final int deleteImageFileFromMediaProvider(Context ctxt, String imageFile) {
		if (imageFile == null)
			return 0;

		ContentResolver cr = ctxt.getContentResolver();
		// images
		int count = 0;
		Cursor imageCursor = null;
		try {
			String select = Images.Media.DATA + "=?";
			String[] selectArgs = { imageFile };

			String[] projection = { Images.ImageColumns._ID };
			imageCursor = cr
					.query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
							projection, select, selectArgs, null);
			if (imageCursor.getCount() > 0) {
				imageCursor.moveToFirst();
				List<Uri> imagesToDelete = new ArrayList<Uri>();
				do {
					String id = imageCursor.getString(imageCursor
							.getColumnIndex(Images.ImageColumns._ID));

					imagesToDelete
							.add(Uri.withAppendedPath(
									android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
									id));
				} while (imageCursor.moveToNext());

				for (Uri uri : imagesToDelete) {
					Log.i(t, "attempting to delete: " + uri);
					count += cr.delete(uri, null, null);
				}
			}
		} catch (Exception e) {
			Log.e(t, e.toString());
		} finally {
			if (imageCursor != null) {
				imageCursor.close();
			}
		}
		File f = new File(imageFile);
		if (f.exists()) {
			f.delete();
		}
		return count;
	}

	public static final int deleteImagesInFolderFromMediaProvider(Context ctxt, File folder) {
		if (folder == null)
			return 0;

		ContentResolver cr = ctxt.getContentResolver();
		// images
		int count = 0;
		Cursor imageCursor = null;
		try {
			String select = Images.Media.DATA + " like ? escape '!'";
			String[] selectArgs = { escapePath(folder.getAbsolutePath()) };

			String[] projection = { Images.ImageColumns._ID };
			imageCursor = cr
					.query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
							projection, select, selectArgs, null);
			if (imageCursor.getCount() > 0) {
				imageCursor.moveToFirst();
				List<Uri> imagesToDelete = new ArrayList<Uri>();
				do {
					String id = imageCursor.getString(imageCursor
							.getColumnIndex(Images.ImageColumns._ID));

					imagesToDelete
							.add(Uri.withAppendedPath(
									android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
									id));
				} while (imageCursor.moveToNext());

				for (Uri uri : imagesToDelete) {
					Log.i(t, "attempting to delete: " + uri);
					count += cr.delete(uri, null, null);
				}
			}
		} catch (Exception e) {
			Log.e(t, e.toString());
		} finally {
			if (imageCursor != null) {
				imageCursor.close();
			}
		}
		return count;
	}

	public static final Uri getAudioUriFromMediaProvider(Context ctxt, String audioFile) {
		String selection = Audio.AudioColumns.DATA + "=?";
		String[] selectArgs = { audioFile };
		String[] projection = { Audio.AudioColumns._ID };
		Cursor c = null;
		try {
			c = ctxt.getContentResolver()
					.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
							projection, selection, selectArgs, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				String id = c.getString(c
						.getColumnIndex(Audio.AudioColumns._ID));

				return Uri
						.withAppendedPath(
								android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
								id);
			}
			return null;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public static final int deleteAudioFileFromMediaProvider(Context ctxt, String audioFile) {
		if (audioFile == null)
			return 0;

		ContentResolver cr = ctxt.getContentResolver();
		// audio
		int count = 0;
		Cursor audioCursor = null;
		try {
			String select = Audio.Media.DATA + "=?";
			String[] selectArgs = { audioFile };

			String[] projection = { Audio.AudioColumns._ID };
			audioCursor = cr
					.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
							projection, select, selectArgs, null);
			if (audioCursor.getCount() > 0) {
				audioCursor.moveToFirst();
				List<Uri> audioToDelete = new ArrayList<Uri>();
				do {
					String id = audioCursor.getString(audioCursor
							.getColumnIndex(Audio.AudioColumns._ID));

					audioToDelete
							.add(Uri.withAppendedPath(
									android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
									id));
				} while (audioCursor.moveToNext());

				for (Uri uri : audioToDelete) {
					Log.i(t, "attempting to delete: " + uri);
					count += cr.delete(uri, null, null);
				}
			}
		} catch (Exception e) {
			Log.e(t, e.toString());
		} finally {
			if (audioCursor != null) {
				audioCursor.close();
			}
		}
		File f = new File(audioFile);
		if (f.exists()) {
			f.delete();
		}
		return count;
	}

	public static final int deleteAudioInFolderFromMediaProvider(Context ctxt, File folder) {
		if (folder == null)
			return 0;

		ContentResolver cr = ctxt.getContentResolver();
		// audio
		int count = 0;
		Cursor audioCursor = null;
		try {
			String select = Audio.Media.DATA + " like ? escape '!'";
			String[] selectArgs = { escapePath(folder.getAbsolutePath()) };

			String[] projection = { Audio.AudioColumns._ID };
			audioCursor = cr
					.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
							projection, select, selectArgs, null);
			if (audioCursor.getCount() > 0) {
				audioCursor.moveToFirst();
				List<Uri> audioToDelete = new ArrayList<Uri>();
				do {
					String id = audioCursor.getString(audioCursor
							.getColumnIndex(Audio.AudioColumns._ID));

					audioToDelete
							.add(Uri.withAppendedPath(
									android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
									id));
				} while (audioCursor.moveToNext());

				for (Uri uri : audioToDelete) {
					Log.i(t, "attempting to delete: " + uri);
					count += cr.delete(uri, null, null);
				}
			}
		} catch (Exception e) {
			Log.e(t, e.toString());
		} finally {
			if (audioCursor != null) {
				audioCursor.close();
			}
		}
		return count;
	}

	public static final Uri getVideoUriFromMediaProvider(Context ctxt, String videoFile) {
		String selection = Video.VideoColumns.DATA + "=?";
		String[] selectArgs = { videoFile };
		String[] projection = { Video.VideoColumns._ID };
		Cursor c = null;
		try {
			c = ctxt.getContentResolver()
					.query(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
							projection, selection, selectArgs, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				String id = c.getString(c
						.getColumnIndex(Video.VideoColumns._ID));

				return Uri
						.withAppendedPath(
								android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
								id);
			}
			return null;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public static final int deleteVideoFileFromMediaProvider(Context ctxt, String videoFile) {
		if (videoFile == null)
			return 0;

		ContentResolver cr = ctxt.getContentResolver();
		// video
		int count = 0;
		Cursor videoCursor = null;
		try {
			String select = Video.Media.DATA + "=?";
			String[] selectArgs = { videoFile };

			String[] projection = { Video.VideoColumns._ID };
			videoCursor = cr
					.query(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
							projection, select, selectArgs, null);
			if (videoCursor.getCount() > 0) {
				videoCursor.moveToFirst();
				List<Uri> videoToDelete = new ArrayList<Uri>();
				do {
					String id = videoCursor.getString(videoCursor
							.getColumnIndex(Video.VideoColumns._ID));

					videoToDelete
							.add(Uri.withAppendedPath(
									android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
									id));
				} while (videoCursor.moveToNext());

				for (Uri uri : videoToDelete) {
					Log.i(t, "attempting to delete: " + uri);
					count += cr.delete(uri, null, null);
				}
			}
		} catch (Exception e) {
			Log.e(t, e.toString());
		} finally {
			if (videoCursor != null) {
				videoCursor.close();
			}
		}
		File f = new File(videoFile);
		if (f.exists()) {
			f.delete();
		}
		return count;
	}

	public static final int deleteVideoInFolderFromMediaProvider(Context ctxt, File folder) {
		if (folder == null)
			return 0;

		ContentResolver cr = ctxt.getContentResolver();
		// video
		int count = 0;
		Cursor videoCursor = null;
		try {
			String select = Video.Media.DATA + " like ? escape '!'";
			String[] selectArgs = { escapePath(folder.getAbsolutePath()) };

			String[] projection = { Video.VideoColumns._ID };
			videoCursor = cr
					.query(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
							projection, select, selectArgs, null);
			if (videoCursor.getCount() > 0) {
				videoCursor.moveToFirst();
				List<Uri> videoToDelete = new ArrayList<Uri>();
				do {
					String id = videoCursor.getString(videoCursor
							.getColumnIndex(Video.VideoColumns._ID));

					videoToDelete
							.add(Uri.withAppendedPath(
									android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
									id));
				} while (videoCursor.moveToNext());

				for (Uri uri : videoToDelete) {
					Log.i(t, "attempting to delete: " + uri);
					count += cr.delete(uri, null, null);
				}
			}
		} catch (Exception e) {
			Log.e(t, e.toString());
		} finally {
			if (videoCursor != null) {
				videoCursor.close();
			}
		}
		return count;
	}

	public static String getPathFromUri(Context ctxt, Uri uri, String pathKey) {
		if (uri.toString().startsWith("file")) {
			return uri.toString().substring(7);
		} else {
			String[] projection = { pathKey };
			Cursor c = null;
			try {
				c = ctxt.getContentResolver()
						.query(uri, projection, null, null, null);
				int column_index = c.getColumnIndexOrThrow(pathKey);
				String path = null;
				if (c.getCount() > 0) {
					c.moveToFirst();
					path = c.getString(column_index);
				}
				return path;
			} finally {
				if (c != null) {
					c.close();
				}
			}
		}
	}
}
