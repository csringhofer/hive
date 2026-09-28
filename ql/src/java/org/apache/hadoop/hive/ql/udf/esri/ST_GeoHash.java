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
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Geometry.GeometryType;
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

@Description(name = "ST_GeoHash",
    value = "_FUNC_(ST_Geometry, maxchars) - enclosing geohash of geometry\n",
    extended = "Example:\n" + "  SELECT _FUNC_(ST_Point(1, 2)) FROM onerow;  --  POINT (1 2)\n") // TODO
public class ST_GeoHash extends ST_Geometry {

  static final Logger LOG = LoggerFactory.getLogger(ST_GeoHash.class.getName());

  public Text evaluate(BytesWritable geomref, IntWritable maxchars) {
    if (geomref == null || geomref.getLength() == 0) {
      LogUtils.Log_ArgumentsNull(LOG);
      return null;
    }

    OGCGeometry ogcGeometry = GeometryUtils.geometryFromEsriShape(geomref);
    if (ogcGeometry == null) {
      LogUtils.Log_ArgumentsNull(LOG);
      return null;
    }

    Geometry esriGeometry = ogcGeometry.getEsriGeometry();
    Envelope envBound = new Envelope();
    ogcGeometry.getEsriGeometry().queryEnvelope(envBound);
    String geoHash = null;
    if (esriGeometry.getType().value() == GeometryType.Point || maxchars.get() > 0) {
      // if precision is set calculate for geohash for center of complex geometries like
      // postgis
      geoHash = GeoHashUtils.encodeGeoHash(
          envBound.getCenterX(), envBound.getCenterY(), maxchars.get());
    } else {
      geoHash = GeoHashUtils.encodeGeoHash(envBound, maxchars.get());
    }

    return new Text(geoHash);
  }


}