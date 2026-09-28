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

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.WktExportFlags;
import com.esri.core.geometry.ogc.OGCGeometry;
import org.apache.hadoop.hive.ql.udf.esri.GeometryUtils.OGCType;
import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Description(name = "ST_GeomFromGeoHash",
    value = "_FUNC_(ST_Geometry, maxchars) - enclosing geometry of geohash\n",
    extended = "Example:\n" + "  SELECT _FUNC_(ST_Point(1, 2)) FROM onerow;  --  POINT (1 2)\n") // TODO
public class ST_GeomFromGeoHash extends ST_Geometry {

  static final Logger LOG = LoggerFactory.getLogger(ST_GeoHash.class.getName());

  public Text evaluate(Text geoHash, IntWritable precision) {
    if (geoHash == null) {
      LogUtils.Log_ArgumentsNull(LOG);
      return null;
    }

    OGCGeometry ogcGeometry = GeometryUtils.geometryFromEsriShape(geomref);
    if (ogcGeometry == null) {
      LogUtils.Log_ArgumentsNull(LOG);
      return null;
    }

    Envelope envBound = new Envelope();
    ogcGeometry.getEsriGeometry().queryEnvelope(envBound);

    try {
      return new Text(encodeGeoHash(envBound, maxchars.get()));
    } catch (Exception e) {
      LOG.error(e.getMessage());
      return null;
    }
  }

  private String encodeGeoHash(Envelope env, int precision) {
    return encodeGeoHash(env.getXMin(), env.getYMin(), env.getXMax(), env.getYMax(), precision, false);
  }

  private static String base32 = "0123456789bcdefghjkmnpqrstuvwxyz";
  private String encodeGeoHash(double lon1, double lat1, double lon2, double lat2,
     int maxPrecision, boolean isPoint) {
    double minLon = -180;
    double maxLon = 180;
    double minLat = -90;
    double maxLat = 90;
    double lonDiff = maxLon;
    double latDiff = maxLat;
    
    StringBuilder sb = new StringBuilder();
    int bits = 0;
    int ch = 0;
    int precision = 0;
    boolean even = true;
    while (precision < maxPrecision) {
      if (even) {
        // bisect by longitude
        //double midLon = (minLon + maxLon) / 2;
        lonDiff /= 2;
        double midLon = minLon + lonDiff;
        if (lon1 >= midLon) {
            ch = ch * 2 + 1;
            minLon = midLon;
        } else {
            ch = ch * 2;
            maxLon = midLon;
        }
      } else {
        // bisect by latitude
        // double midLat = (minLat + maxLat) / 2;
        latDiff /= 2;
        double midLat = minLat + latDiff;
        if (lat1 >= midLat) {
            ch = ch * 2 + 1;
            minLat = midLat;
        } else {
            ch = ch * 2;
            maxLat = midLat;
        }
      }
      bits++;
      if (bits == 5) {
        if (!isPoint) {
          // check if other point is also in bbox, stop at this precision if not
          // TODO: assume that p2 is the other conrer, enough to check > maxlat/lo n
          if (lon2 < minLon || lon2 > maxLon || lat2 < minLat || lat2 > maxLat) break;
        }
        sb.append(base32.charAt(ch));
        bits = 0;
        ch = 0;
        precision++;
      }
      even = !even;
    }
    
    return sb.toString();
  }
}