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

public final class GeoHashUtils {

  private GeoHashUtils() {}

  public static class InvalidGeoHashException extends Exception {
    public InvalidGeoHashException(String message) {
      super(message);
    }
  }


  public static String encodeGeoHash(Envelope env, int precision) {
    return encodeGeoHash(env.getXMin(), env.getYMin(), env.getXMax(), env.getYMax(), precision, false);
  }

  public static String encodeGeoHash(double x, double y, int precision) {
    return encodeGeoHash(x, y, x, y, precision, true);
  }

  // a, i, l, o are skipped, see:
  // https://en.wikipedia.org/wiki/Geohash#Textual_representation
  private static String base32 = "0123456789bcdefghjkmnpqrstuvwxyz";

  public static String encodeGeoHash(double lon1, double lat1, double lon2, double lat2,
     int maxPrecision, boolean isPoint) {
    double minLon = -180;
    double maxLon = 180;
    double minLat = -90;
    double maxLat = 90;
    double lonDiff = 360;
    double latDiff = 180;
    if (maxPrecision == 0) maxPrecision = 20;

    // TODO: use char[], as Text is utf8 encoded?
    StringBuilder sb = new StringBuilder();
    int bits = 0;
    int ch = 0;
    int precision = 0;
    boolean even = true;
    while (precision < maxPrecision) {
      if (even) {
        // bisect by longitude
        lonDiff /= 2;
        double midLon = minLon + lonDiff;
        midLon = (minLon + maxLon) / 2;
        if (lon1 >= midLon) {
            ch = ch * 2 + 1;
            minLon = midLon;
        } else {
            ch = ch * 2;
            maxLon = midLon;
        }
      } else {
        // bisect by latitude
        latDiff /= 2;
        double midLat = minLat + latDiff;
        midLat = minLat + latDiff;
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
          // TODO: enough to check lon2 < minLon || lat2 > maxLat if lat1<=lat2&&lon1<=lon2
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

  // Based on Apacge Sedona implementation:
  // https://github.com/apache/sedona/blob/sedona-1.8.0/common/src/main/java/org/apache/sedona/common/utils/GeoHashDecoder.java#L42
  public static void decodeGeoHashBBox(String geohash, Integer precision, Envelope outEnv)
      throws InvalidGeoHashException {
    double minLon = -180;
    double maxLon = 180;
    double minLat = -90;
    double maxLat = 90;
    String geoHashLowered = geohash.toLowerCase();
    int geoHashLength = geohash.length();
    int targetPrecision = geoHashLength;
    if (precision != null) {
      if (precision < 0) throw new InvalidGeoHashException("Precision can not be negative");
      else targetPrecision = Math.min(geoHashLength, precision);
    }
    boolean isEven = true;

    for (int i = 0; i < targetPrecision; i++) {
      char c = geoHashLowered.charAt(i);
      byte cd = (byte) base32.indexOf(c);
      if (cd == -1) {
        throw new InvalidGeoHashException(
            String.format("Invalid character '%s' found at index %d", c, i));
      }
      for (int j = 0; j < 5; j++) {
        if (isEven) {
          double midLon = (minLon + maxLon) / 2;
          if (cd % 2 == 1) {
            minLon = midLon;
          } else {
            maxLon = midLon;
          }
        } else {
          double midLat = (minLat + maxLat) / 2;
          if (cd % 2 == 1) {
            minLat = midLat;
          } else {
            maxLat = midLat;
          }
        }
        cd >>= 1;
      }
      isEven = !isEven;
    }
    outEnv.setCoords(minLon, minLat, maxLon, maxLat);
  } 
}
