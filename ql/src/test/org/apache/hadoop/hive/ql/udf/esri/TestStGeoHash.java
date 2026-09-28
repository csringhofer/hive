/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hive.ql.udf.esri;

import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.serde2.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.BytesWritable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestStGeoHash {
  static int MAX_PRECISION = 20;

  private BytesWritable point(double lon, double lat) {
    ST_Point stPt = new ST_Point();
    return stPt.evaluate(new DoubleWritable(lon), new DoubleWritable(lat));
  }

  private BytesWritable line(double lon1, double lat1, double lon2, double lat2) {
    ST_LineString stLt = new ST_LineString();
    try {
      return stLt.evaluate(new DoubleWritable(lon1), new DoubleWritable(lat1),
                          new DoubleWritable(lon2), new DoubleWritable(lat2));
    } catch (UDFArgumentException ex) {
      assertTrue(false);
      return null;
    }
   }

   private void checkPoint(String expectedGeoHash, double lon, double lat) {
      IntWritable precision = new IntWritable();
      ST_GeoHash geoh = new ST_GeoHash();
      BytesWritable shape = point(lon, lat);
      for (int i = MAX_PRECISION; i > 0; i--) {
        precision.set(i);
        String expected = expectedGeoHash.substring(0, i);
        assertEquals(expected, geoh.evaluate(shape, precision).toString());
      }
   }

  private void checkLine(String expectedGeoHash,
      double lon1, double lat1, double lon2, double lat2, int charsWithNoPrec) {
    IntWritable precision = new IntWritable();
    ST_GeoHash geoh = new ST_GeoHash();
    BytesWritable shape = line(lon1, lat1, lon2,  lat2);
    for (int i = MAX_PRECISION; i > 0; i--) {
      precision.set(i);
      int expected_precision = Math.min(i, expectedGeoHash.length());
      String expected = expectedGeoHash.substring(0, expected_precision);
      assertEquals(Integer.toString(i), expected, geoh.evaluate(shape, precision).toString());
      if (i == charsWithNoPrec) {
        precision.set(0);
        assertEquals("0", expected, geoh.evaluate(shape, precision).toString());
      }
    }
  }

  @Test
  public void testStGeoHash() {
    checkPoint("s0000000000000000000", 0, 0);
    checkPoint("y0000000000000000000", 90, 45);
    checkPoint("kpbm5kup6c5pbdqcbntd", 0.5, -0.5);
    checkPoint("50000000000000000000", -45, -90);
    checkPoint("bpbpbpbpbpbpbpbpbpbp", -180, 90);
    checkPoint("s000000000000000d7w1", 0.0000000001, 0.0000000001);
    checkPoint("s000000000000000000y", 0.000000000001, 0.000000000001);
    checkPoint("s0000000000000000000", 0.0000000000001, 0.0000000000001);
    checkPoint("s0000000000000008581", 0, 0.0000000001);
    checkPoint("v000000000000000000y", 45.000000000001, 45.000000000001);
    checkPoint("z0000000000000000007", 135.0000000000005, 45.0000000000005);
    checkPoint("zzh7w1z0gs3y0zh7w1zs", 175.0000000000005, 85.0000000000005);
    

    checkLine("c0w37ejpw15wwmfsbgch", -126, 48, -126.1, 48.1, 4);
    checkLine("s000000001z7wsg7zzm6", 0, 0, 0.00002, 0.00002, 9);
    checkLine("s000000001z7zy3tmsg0", 0.0000001, 0.0000001, 0.00002, 0.00002,9);
    checkLine("s0000000000000000000", 0, 0.0000000000001, 0.0000000000001, 0.0000000000001, 20);
    checkLine("s000000000000000d7w1", 0.0000000001, 0.0000000001, 0.0000000001, 0.0000000001, 20);
    checkLine("s00000000000000095x1", 0, 0.0000000001, 0.0000000001, 0.0000000001, 20);
  }

}

