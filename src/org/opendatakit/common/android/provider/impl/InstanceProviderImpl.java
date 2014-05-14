/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2011-2013 University of Washington
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

package org.opendatakit.common.android.provider.impl;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.opendatakit.aggregate.odktables.rest.TableConstants;
import org.opendatakit.common.android.R;
import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.database.DataModelDatabaseHelper.ColumnDefinition;
import org.opendatakit.common.android.database.DataModelDatabaseHelper.IdInstanceNameStruct;
import org.opendatakit.common.android.database.DataModelDatabaseHelperFactory;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.provider.InstanceColumns;
import org.opendatakit.common.android.utilities.ODKFileUtils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * TODO: convert to true app-scoped instance provider
 */
public abstract class InstanceProviderImpl extends ContentProvider {

  // private static final String t = "InstancesProviderImpl";

  private static final String DATA_TABLE_ID_COLUMN = DataTableColumns.ID;
  private static final String DATA_TABLE_SAVEPOINT_TIMESTAMP_COLUMN = DataTableColumns.SAVEPOINT_TIMESTAMP;
  private static final String DATA_TABLE_SAVEPOINT_TYPE_COLUMN = DataTableColumns.SAVEPOINT_TYPE;

  private static HashMap<String, String> sInstancesProjectionMap;

  public abstract String getInstanceAuthority();

  private static class IdStruct {
    public final String idUploadsTable;
    public final String idDataTable;

    IdStruct(String idUploadsTable, String idDataTable) {
      this.idUploadsTable = idUploadsTable;
      this.idDataTable = idDataTable;
    }
  }

  @Override
  public boolean onCreate() {

    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
      String sortOrder) {
    List<String> segments = uri.getPathSegments();

    if (segments.size() < 2 || segments.size() > 3) {
      throw new SQLException("Unknown URI (too many segments!) " + uri);
    }

    String appName = segments.get(0);
    ODKFileUtils.verifyExternalStorageAvailability();
    ODKFileUtils.assertDirectoryStructure(appName);
    String uriFormId = segments.get(1);
    // _ID in UPLOADS_TABLE_NAME
    String instanceId = (segments.size() == 3 ? segments.get(2) : null);

    boolean success = false;
    SQLiteDatabase db = null;
    String fullQuery;
    String filterArgs[];
    Cursor c = null;
    try {
      DataModelDatabaseHelper dbh = DataModelDatabaseHelperFactory.getDbHelper(getContext(), appName);
      if ( dbh == null ) {
        throw new SQLException("Unable to access database for " + uri);
      }

      db = dbh.getWritableDatabase();
      db.beginTransaction();

      IdInstanceNameStruct ids;
      try {
        ids = DataModelDatabaseHelper.getIds(db, uriFormId);
      } catch ( Exception e ) {
        throw new SQLException("Unable to retrieve formId " + uri);
      }

      if (ids == null) {
        throw new SQLException("Unknown URI (no matching formId) " + uri);
      }
      String dbTableName;
      try {
        dbTableName = DataModelDatabaseHelper.getDbTableName(db, ids.tableId);
      } catch ( Exception e ) {
        e.printStackTrace();
        throw new SQLException("Unknown URI (exception retrieving data table for formId) " + uri);
      }
      if (dbTableName == null) {
        throw new SQLException("Unknown URI (missing data table for formId) " + uri);
      }

      dbTableName = "\"" + dbTableName + "\"";

      // ARGH! we must ensure that we have records in our UPLOADS_TABLE_NAME
      // for every distinct instance in the data table.
      StringBuilder b = new StringBuilder();
      //@formatter:off
      b.append("INSERT INTO ").append(DataModelDatabaseHelper.UPLOADS_TABLE_NAME).append("(")
          .append(InstanceColumns.DATA_INSTANCE_ID).append(",")
          .append(InstanceColumns.DATA_TABLE_TABLE_ID).append(",")
          .append(InstanceColumns.XML_PUBLISH_FORM_ID).append(") ").append("SELECT ")
          .append(InstanceColumns.DATA_INSTANCE_ID).append(",")
          .append(InstanceColumns.DATA_TABLE_TABLE_ID).append(",")
          .append(InstanceColumns.XML_PUBLISH_FORM_ID).append(" FROM (")
            .append("SELECT DISTINCT ").append(DATA_TABLE_ID_COLUMN).append(" as ")
            .append(InstanceColumns.DATA_INSTANCE_ID).append(",").append("? as ")
            .append(InstanceColumns.DATA_TABLE_TABLE_ID).append(",")
            .append(DataTableColumns.FORM_ID).append(" as ")
            .append(InstanceColumns.XML_PUBLISH_FORM_ID).append(" FROM ")
            .append(dbTableName).append(" EXCEPT SELECT DISTINCT ")
            .append(InstanceColumns.DATA_INSTANCE_ID).append(",")
            .append(InstanceColumns.DATA_TABLE_TABLE_ID).append(",")
            .append(InstanceColumns.XML_PUBLISH_FORM_ID).append(" FROM ")
            .append(DataModelDatabaseHelper.UPLOADS_TABLE_NAME).append(")");
      //@formatter:on

      // TODO: should we collapse across FORM_ID or leave it this way?
      String[] args = { ids.tableId };
      db.execSQL(b.toString(), args);

      // Can't get away with dataTable.* because of collision with _ID column
      // get map of (elementKey -> ColumnDefinition)
      Map<String, ColumnDefinition> defns;
      try {
        defns = DataModelDatabaseHelper.getColumnDefinitions(db, ids.tableId);
      } catch (JsonParseException e) {
        e.printStackTrace();
        throw new SQLException("Unable to retrieve column definitions for tableId " + ids.tableId);
      } catch (JsonMappingException e) {
        e.printStackTrace();
        throw new SQLException("Unable to retrieve column definitions for tableId " + ids.tableId);
      } catch (IOException e) {
        e.printStackTrace();
        throw new SQLException("Unable to retrieve column definitions for tableId " + ids.tableId);
      }

      // We can now join through and access the data table rows

      b.setLength(0);
      // @formatter:off
      b.append("SELECT ");
      b.append(DataModelDatabaseHelper.UPLOADS_TABLE_NAME).append(".")
          .append(InstanceColumns._ID).append(",")
       .append(DataModelDatabaseHelper.UPLOADS_TABLE_NAME).append(".")
          .append(InstanceColumns.DATA_INSTANCE_ID).append(",")
       .append(DataModelDatabaseHelper.UPLOADS_TABLE_NAME).append(".")
          .append(InstanceColumns.XML_PUBLISH_FORM_ID).append(",");
      // add the dataTable metadata except for _ID (which conflicts with InstanceColumns._ID)
      b.append(dbTableName).append(".").append(DataTableColumns.ROW_ETAG).append(",")
       .append(dbTableName).append(".").append(DataTableColumns.SYNC_STATE).append(",")
       .append(dbTableName).append(".").append(DataTableColumns.CONFLICT_TYPE).append(",")
       .append(dbTableName).append(".").append(DataTableColumns.FILTER_TYPE).append(",")
       .append(dbTableName).append(".").append(DataTableColumns.FILTER_VALUE).append(",")
       .append(dbTableName).append(".").append(DataTableColumns.FORM_ID).append(",")
       .append(dbTableName).append(".").append(DataTableColumns.LOCALE).append(",")
       .append(dbTableName).append(".").append(DataTableColumns.SAVEPOINT_TYPE).append(",")
       .append(dbTableName).append(".").append(DataTableColumns.SAVEPOINT_TIMESTAMP).append(",")
       .append(dbTableName).append(".").append(DataTableColumns.SAVEPOINT_CREATOR).append(",");
      // add the user-specified data fields in this dataTable
      for ( ColumnDefinition cd : defns.values() ) {
        if ( cd.isUnitOfRetention ) {
          b.append(dbTableName).append(".")
           .append(cd.elementKey).append(",");
        }
      }
      // b.append(dbTableName).append(".").append(InstanceColumns._ID).append(",");
      b.append("CASE WHEN ").append(DATA_TABLE_SAVEPOINT_TIMESTAMP_COLUMN).append(" IS NULL THEN null")
          .append(" WHEN ").append(InstanceColumns.XML_PUBLISH_TIMESTAMP)
          .append(" IS NULL THEN null").append(" WHEN ").append(DATA_TABLE_SAVEPOINT_TIMESTAMP_COLUMN)
          .append(" > ").append(InstanceColumns.XML_PUBLISH_TIMESTAMP).append(" THEN null")
          .append(" ELSE ").append(InstanceColumns.XML_PUBLISH_TIMESTAMP).append(" END as ")
          .append(InstanceColumns.XML_PUBLISH_TIMESTAMP).append(",");
      b.append("CASE WHEN ").append(DATA_TABLE_SAVEPOINT_TIMESTAMP_COLUMN).append(" IS NULL THEN null")
          .append(" WHEN ").append(InstanceColumns.XML_PUBLISH_TIMESTAMP)
          .append(" IS NULL THEN null").append(" WHEN ").append(DATA_TABLE_SAVEPOINT_TIMESTAMP_COLUMN)
          .append(" > ").append(InstanceColumns.XML_PUBLISH_TIMESTAMP).append(" THEN null")
          .append(" ELSE ").append(InstanceColumns.XML_PUBLISH_STATUS).append(" END as ")
          .append(InstanceColumns.XML_PUBLISH_STATUS).append(",");
      b.append("CASE WHEN ").append(DATA_TABLE_SAVEPOINT_TIMESTAMP_COLUMN).append(" IS NULL THEN null")
          .append(" WHEN ").append(InstanceColumns.XML_PUBLISH_TIMESTAMP)
          .append(" IS NULL THEN null").append(" WHEN ").append(DATA_TABLE_SAVEPOINT_TIMESTAMP_COLUMN)
          .append(" > ").append(InstanceColumns.XML_PUBLISH_TIMESTAMP).append(" THEN null")
          .append(" ELSE ").append(InstanceColumns.DISPLAY_SUBTEXT).append(" END as ")
          .append(InstanceColumns.DISPLAY_SUBTEXT).append(",");
      if ( ids.instanceName == null ) {
        b.append( "datetime(").append(DATA_TABLE_SAVEPOINT_TIMESTAMP_COLUMN).append("/1000000, 'unixepoch', 'localtime')");
      } else {
        b.append(ids.instanceName);
      }
      b.append(" as ").append(InstanceColumns.DISPLAY_NAME);
      b.append(" FROM ");
      b.append("( SELECT * FROM ").append(dbTableName).append(" GROUP BY ")
          .append(DATA_TABLE_ID_COLUMN).append(" HAVING ").append(DATA_TABLE_SAVEPOINT_TIMESTAMP_COLUMN)
          .append(" = MAX(").append(DATA_TABLE_SAVEPOINT_TIMESTAMP_COLUMN).append(")").append(") as ")
          .append(dbTableName);
      b.append(" JOIN ").append(DataModelDatabaseHelper.UPLOADS_TABLE_NAME).append(" ON ")
          .append(dbTableName).append(".").append(DATA_TABLE_ID_COLUMN).append("=")
          .append(DataModelDatabaseHelper.UPLOADS_TABLE_NAME).append(".")
          .append(InstanceColumns.DATA_INSTANCE_ID).append(" AND ").append("? =")
          .append(DataModelDatabaseHelper.UPLOADS_TABLE_NAME).append(".")
          .append(InstanceColumns.DATA_TABLE_TABLE_ID).append(" AND ").append("? =")
          .append(DataModelDatabaseHelper.UPLOADS_TABLE_NAME).append(".")
          .append(InstanceColumns.XML_PUBLISH_FORM_ID);
      b.append(" WHERE ").append(DATA_TABLE_SAVEPOINT_TYPE_COLUMN).append("=?");
      // @formatter:on

      if (instanceId != null) {
        b.append(" AND ").append(DataModelDatabaseHelper.UPLOADS_TABLE_NAME).append(".")
            .append(InstanceColumns._ID).append("=?");
        String tempArgs[] = { ids.tableId, ids.formId, InstanceColumns.STATUS_COMPLETE, instanceId };
        filterArgs = tempArgs;
      } else {
        String tempArgs[] = { ids.tableId, ids.formId, InstanceColumns.STATUS_COMPLETE };
        filterArgs = tempArgs;
      }

      if (selection != null) {
        b.append(" AND (").append(selection).append(")");
      }

      if (selectionArgs != null) {
        String[] tempArgs = new String[filterArgs.length + selectionArgs.length];
        for (int i = 0; i < filterArgs.length; ++i) {
          tempArgs[i] = filterArgs[i];
        }
        for (int i = 0; i < selectionArgs.length; ++i) {
          tempArgs[filterArgs.length + i] = selectionArgs[i];
        }
        filterArgs = tempArgs;
      }

      if (sortOrder != null) {
        b.append(" ORDER BY ").append(sortOrder);
      }

      fullQuery = b.toString();
      success = true;
    } finally {
      if ( !success ) {
        db.endTransaction();
        db.close();
        return null;
      }
    }

    c = db.rawQuery(fullQuery, filterArgs);
    // Tell the cursor what uri to watch, so it knows when its source data
    // changes
    c.setNotificationUri(getContext().getContentResolver(), uri);
    return c;
  }

  @Override
  public String getType(Uri uri) {
    // don't see the point of trying to implement this call...
    return null;
    // switch (sUriMatcher.match(uri)) {
    // case INSTANCES:
    // return InstanceColumns.CONTENT_TYPE;
    //
    // case INSTANCE_ID:
    // return InstanceColumns.CONTENT_ITEM_TYPE;
    //
    // default:
    // throw new IllegalArgumentException("Unknown URI " + uri);
    // }
  }

  @Override
  public Uri insert(Uri uri, ContentValues initialValues) {
    throw new IllegalArgumentException("Insert not implemented!");
  }

  private String getDisplaySubtext(String xmlPublishStatus, Date xmlPublishDate) {
    if (xmlPublishDate == null) {
      return getContext().getString(R.string.not_yet_sent);
    } else if (InstanceColumns.STATUS_SUBMITTED.equalsIgnoreCase(xmlPublishStatus)) {
      return new SimpleDateFormat(getContext().getString(R.string.sent_on_date_at_time),
          Locale.getDefault()).format(xmlPublishDate);
    } else if (InstanceColumns.STATUS_SUBMISSION_FAILED.equalsIgnoreCase(xmlPublishStatus)) {
      return new SimpleDateFormat(getContext().getString(R.string.sending_failed_on_date_at_time),
          Locale.getDefault()).format(xmlPublishDate);
    } else {
      throw new IllegalStateException("Unrecognized xmlPublishStatus: " + xmlPublishStatus);
    }
  }

  /**
   * This method removes the entry from the content provider, and also removes
   * any associated files. files: form.xml, [formmd5].formdef, formname
   * {directory}
   */
  @Override
  public int delete(Uri uri, String where, String[] whereArgs) {
    List<String> segments = uri.getPathSegments();

    if (segments.size() < 2 || segments.size() > 3) {
      throw new SQLException("Unknown URI (too many segments!) " + uri);
    }

    String appName = segments.get(0);
    ODKFileUtils.verifyExternalStorageAvailability();
    ODKFileUtils.assertDirectoryStructure(appName);
    String uriFormId = segments.get(1);
    // _ID in UPLOADS_TABLE_NAME
    String instanceId = (segments.size() == 3 ? segments.get(2) : null);

    SQLiteDatabase db = null;
    List<IdStruct> idStructs = new ArrayList<IdStruct>();
    try {
      DataModelDatabaseHelper dbh = DataModelDatabaseHelperFactory.getDbHelper(getContext(), appName);
      if ( dbh == null ) {
        throw new SQLException("Unable to access database for " + uri);
      }

      db = dbh.getWritableDatabase();
      db.beginTransaction();

      IdInstanceNameStruct ids;
      try {
        ids = DataModelDatabaseHelper.getIds(db, uriFormId);
      } catch ( Exception e ) {
        throw new SQLException("Unable to retrieve formId " + uri);
      }

      if (ids == null) {
        throw new SQLException("Unknown URI (no matching formId) " + uri);
      }
      String dbTableName;
      try {
        dbTableName = DataModelDatabaseHelper.getDbTableName(db, ids.tableId);
      } catch ( Exception e ) {
        e.printStackTrace();
        throw new SQLException("Unknown URI (exception retrieving data table for formId) " + uri);
      }
      if (dbTableName == null) {
        throw new SQLException("Unknown URI (missing data table for formId) " + uri);
      }

      dbTableName = "\"" + dbTableName + "\"";

      if (segments.size() == 2) {
        where = "(" + where + ") AND (" + InstanceColumns.DATA_INSTANCE_ID + "=? )";
        if (whereArgs != null) {
          String[] args = new String[whereArgs.length + 1];
          for (int i = 0; i < whereArgs.length; ++i) {
            args[i] = whereArgs[i];
          }
          args[whereArgs.length] = instanceId;
          whereArgs = args;
        } else {
          whereArgs = new String[] { instanceId };
        }
      }

      Cursor del = null;
      try {
        del = this.query(uri, null, where, whereArgs, null);
        del.moveToPosition(-1);
        while (del.moveToNext()) {
          String iId = del.getString(del.getColumnIndex(InstanceColumns._ID));
          String iIdDataTable = del.getString(del.getColumnIndex(InstanceColumns.DATA_INSTANCE_ID));
          idStructs.add(new IdStruct(iId, iIdDataTable));
          String path = ODKFileUtils.getInstanceFolder(appName, ids.tableId, iIdDataTable);
          File f = new File(path);
          if (f.exists()) {
            if (f.isDirectory()) {
              FileUtils.deleteDirectory(f);
            } else {
              f.delete();
            }
          }

        }
      } catch (IOException e) {
        e.printStackTrace();
        throw new IllegalArgumentException("Unable to delete instance directory: " + e.toString());
      } finally {
        if (del != null) {
          del.close();
        }
      }

      for (IdStruct idStruct : idStructs) {
        db.delete(DataModelDatabaseHelper.UPLOADS_TABLE_NAME, InstanceColumns.DATA_INSTANCE_ID
            + "=?", new String[] { idStruct.idUploadsTable });
        db.delete(dbTableName, DATA_TABLE_ID_COLUMN + "=?", new String[] { idStruct.idDataTable });
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      db.close();
    }
    getContext().getContentResolver().notifyChange(uri, null);
    return idStructs.size();
  }

  @Override
  public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
    List<String> segments = uri.getPathSegments();

    if (segments.size() != 3) {
      throw new SQLException("Unknown URI (does not specify instance!) " + uri);
    }

    String appName = segments.get(0);
    ODKFileUtils.verifyExternalStorageAvailability();
    ODKFileUtils.assertDirectoryStructure(appName);

    String uriFormId = segments.get(1);
    // _ID in UPLOADS_TABLE_NAME
    String instanceId = segments.get(2);

    SQLiteDatabase db = null;
    int count = 0;
    try {
      DataModelDatabaseHelper dbh = DataModelDatabaseHelperFactory.getDbHelper(getContext(), appName);
      if ( dbh == null ) {
        throw new SQLException("Unable to access database for " + uri);
      }

      db = dbh.getWritableDatabase();

      IdInstanceNameStruct ids;
      try {
        ids = DataModelDatabaseHelper.getIds(db, uriFormId);
      } catch ( Exception e ) {
        throw new SQLException("Unable to retrieve formId " + uri);
      }

      if (ids == null) {
        throw new SQLException("Unknown URI (no matching formId) " + uri);
      }
      String dbTableName;
      try {
        dbTableName = DataModelDatabaseHelper.getDbTableName(db, ids.tableId);
      } catch ( Exception e ) {
        e.printStackTrace();
        throw new SQLException("Unknown URI (exception retrieving data table for formId) " + uri);
      }
      if (dbTableName == null) {
        throw new SQLException("Unknown URI (missing data table for formId) " + uri);
      }

      dbTableName = "\"" + dbTableName + "\"";

      // run the query to get all the ids...
      List<IdStruct> idStructs = new ArrayList<IdStruct>();
      Cursor ref = null;
      try {
        // use this provider's query interface to get the set of ids that
        // match (if any)
        ref = this.query(uri, null, where, whereArgs, null);
        if ( ref.getCount() != 0 ) {
          ref.moveToFirst();
          do {
            String iId = ref.getString(ref.getColumnIndex(InstanceColumns._ID));
            String iIdDataTable = ref.getString(ref.getColumnIndex(InstanceColumns.DATA_INSTANCE_ID));
            idStructs.add(new IdStruct(iId, iIdDataTable));
          } while (ref.moveToNext());
        }
      } finally {
        if (ref != null) {
          ref.close();
        }
      }

      // update the values string...
      if (values.containsKey(InstanceColumns.XML_PUBLISH_STATUS)) {
        Date xmlPublishDate = new Date();
        values.put(InstanceColumns.XML_PUBLISH_TIMESTAMP, TableConstants.nanoSecondsFromMillis(xmlPublishDate.getTime()));
        String xmlPublishStatus = values.getAsString(InstanceColumns.XML_PUBLISH_STATUS);
        if (values.containsKey(InstanceColumns.DISPLAY_SUBTEXT) == false) {
          String text = getDisplaySubtext(xmlPublishStatus, xmlPublishDate);
          values.put(InstanceColumns.DISPLAY_SUBTEXT, text);
        }
      }

      String[] args = new String[1];
      for (IdStruct idStruct : idStructs) {
        args[0] = idStruct.idUploadsTable;
        count += db.update(DataModelDatabaseHelper.UPLOADS_TABLE_NAME, values,
                           InstanceColumns._ID + "=?", args);
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      db.close();
    }
    getContext().getContentResolver().notifyChange(uri, null);
    return count;
  }

  static {

    sInstancesProjectionMap = new HashMap<String, String>();
    sInstancesProjectionMap.put(InstanceColumns._ID, InstanceColumns._ID);
    sInstancesProjectionMap.put(InstanceColumns.DATA_INSTANCE_ID,
        InstanceColumns.DATA_INSTANCE_ID);
    sInstancesProjectionMap.put(InstanceColumns.XML_PUBLISH_TIMESTAMP,
        InstanceColumns.XML_PUBLISH_TIMESTAMP);
    sInstancesProjectionMap.put(InstanceColumns.XML_PUBLISH_STATUS,
        InstanceColumns.XML_PUBLISH_STATUS);
  }

}
