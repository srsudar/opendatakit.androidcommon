/*
 * Copyright (C) 2013 University of Washington
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

package org.opendatakit.common.android.provider;

import android.provider.BaseColumns;

/**
 * ODK Survey (only)
 *
 * Tracks the upload status of each row in each data table.
 */
public final class InstanceColumns implements BaseColumns {
  // saved status from row in data table:
  public static final String STATUS_INCOMPLETE = "INCOMPLETE";
  public static final String STATUS_COMPLETE = "COMPLETE";
  // xmlPublishStatus from instances db:
  public static final String STATUS_SUBMITTED = "submitted";
  public static final String STATUS_SUBMISSION_FAILED = "submissionFailed";

  // This class cannot be instantiated
  private InstanceColumns() {
  }

  public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.opendatakit.instance";
  public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.opendatakit.instance";

  // These are the only things needed for an insert
  // _ID is the index on the table maintained for ODK Survey purposes
  // DATA_TABLE_INSTANCE_ID ****MUST MATCH**** value used in javascript
  // joins on the data table...
  public static final String DATA_TABLE_INSTANCE_ID = "id";
  public static final String DATA_TABLE_TABLE_ID = "tableId";
  public static final String XML_PUBLISH_FORM_ID = "xmlPublishFormId";
  public static final String XML_PUBLISH_TIMESTAMP = "xmlPublishTimestamp";
  public static final String XML_PUBLISH_STATUS = "xmlPublishStatus";
  public static final String DISPLAY_NAME = "displayName";
  public static final String DISPLAY_SUBTEXT = "displaySubtext";

  /**
   * Get the create sql for the forms table (ODK Survey only).
   *
   * @return
   */
  public static String getTableCreateSql(String tableName) {
    //@formatter:off
       return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
           + _ID + " integer primary key, "
           + DATA_TABLE_INSTANCE_ID + " text, "
           + DATA_TABLE_TABLE_ID + " text, "
           + XML_PUBLISH_FORM_ID + " text, "
           + XML_PUBLISH_TIMESTAMP + " integer, "
           + XML_PUBLISH_STATUS + " text, "
           + DISPLAY_SUBTEXT + " text)";
     //@formatter:on
  }

}