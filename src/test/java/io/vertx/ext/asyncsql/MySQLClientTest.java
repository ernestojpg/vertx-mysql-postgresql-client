/*
 *  Copyright 2015 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.asyncsql;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

public class MySQLClientTest extends SQLTestBase {


  @Before
  public void init() {
    client = MySQLClient.createNonShared(vertx,
        new JsonObject()
            .put("host", System.getProperty("db.host", "localhost"))
    );
  }

  // Configure the expected time used in the date test

  /**
   * @return the String form of the time returned for "2015-02-22T07:15:01.234".
   */
  @Override
  public String getExpectedTime1() {
    return "2015-02-22T07:15:01.000";
  }

  /**
   * @return the String form of the time returned for "2014-06-27T17:50:02.468".
   */
  @Override
  public String getExpectedTime2() {
    return "2014-06-27T17:50:02.000";
  }

  @Override
  protected void setSqlModeIfPossible(Handler<Void> handler) {
    conn.execute("set SQL_MODE = 'STRICT_ALL_TABLES'", ar1 -> {
      // INFO: we ignore the result of this call because it is a mysql specific feature and not all versions support it
      // what is means is that we want the sql parser to be strict even if the engine e.g.: myisam does not implement
      // all constraints such as is the date Feb 31 a valid date. By specifying this we will tell for example that the
      // previous date is invalid.
      handler.handle(null);
    });
  }

  @Test
  public void testInsertedIds(TestContext context) {
    String name1 = "Adele";
    String name2 = "Betty";
    Async async = context.async();

    client.getConnection(ar -> {
      ensureSuccess(context, ar);
      conn = ar.result();
      setupAutoIncrementTable(conn, ar2 -> {
        ensureSuccess(context, ar2);
        conn.updateWithParams("INSERT INTO test_table (name) VALUES (?)", new JsonArray().add(name1), ar3 -> {
          ensureSuccess(context, ar3);
          UpdateResult updateResult1 = ar3.result();
          long id1 = updateResult1.getKeys().getLong(0);
          conn.updateWithParams("INSERT INTO test_table (name) VALUES (?)", new JsonArray().add(name2), ar4 -> {
            ensureSuccess(context, ar4);
            UpdateResult updateResult2 = ar4.result();
            long id2 = updateResult2.getKeys().getLong(0);
            checkConsistency(context, async, conn, id1, name1, id2, name2);
          });
        });
      });
    });
  }

  private void setupAutoIncrementTable(SQLConnection conn, Handler<AsyncResult<Void>> handler) {
    conn.execute("BEGIN",
        ar -> conn.execute("DROP TABLE IF EXISTS test_table",
            ar2 -> conn.execute("CREATE TABLE test_table (id BIGINT AUTO_INCREMENT, name VARCHAR(255), PRIMARY KEY(id))",
                ar3 -> conn.execute("COMMIT", handler::handle))));
  }
}
