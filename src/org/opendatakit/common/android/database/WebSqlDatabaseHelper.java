/*
 * Copyright (C) 2012-2013 University of Washington
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

package org.opendatakit.common.android.database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class WebSqlDatabaseHelper {
  private static final String t = "WebSqlDatabaseHelper";

  private List<WebDbDefinition> webDatabasePaths;

  public WebSqlDatabaseHelper(String path) {
    WebDbDatabaseHelper mWebDb = new WebDbDatabaseHelper(path);

    List<WebDbDefinition> dbCandidates = new ArrayList<WebDbDefinition>();

    SQLiteDatabase db = null;
    Cursor c = null;
    try {
      db = mWebDb.getWritableDatabase();
      c = db.query(WebDbDatabaseHelper.WEBDB_DATABASES_TABLE, null, null, null, null, null, null);

      if (c.moveToFirst()) {
        do {
          String shortName = c.getString(c.getColumnIndex(WebDbDatabaseHelper.DATABASES_NAME));
          String displayName = c.getString(c
              .getColumnIndex(WebDbDatabaseHelper.DATABASES_DISPLAY_NAME));
          String relPath = c.getString(c.getColumnIndex(WebDbDatabaseHelper.COMMON_ORIGIN));
          String dbName = c.getString(c.getColumnIndex(WebDbDatabaseHelper.DATABASES_PATH));
          Integer estimatedSize = c.getInt(c
              .getColumnIndex(WebDbDatabaseHelper.DATABASES_ESTIMATED_SIZE));

          dbCandidates.add(new WebDbDefinition(shortName, displayName, estimatedSize, new File(path
              + File.separator + relPath + File.separator + dbName)));
        } while (c.moveToNext());
      }
    } finally {
      if (c != null) {
        c.close();
      }
      if (db != null) {
        db.close();
      }
    }

    webDatabasePaths = dbCandidates;
    Log.i(t, "Number of web databases found: " + webDatabasePaths.size());
  }

  public WebDbDefinition getWebKitDatabaseInfoHelper() {
    for (WebDbDefinition defn : webDatabasePaths) {
      if (defn.shortName.equalsIgnoreCase(WebDbDatabaseHelper.WEBDB_INSTANCE_DB_SHORT_NAME)) {
        return defn;
      }
    }
    return null;
  }

}
