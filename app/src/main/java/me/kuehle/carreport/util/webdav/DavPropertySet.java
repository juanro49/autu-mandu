/*
 * Copyright 2016 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.kuehle.carreport.util.webdav;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import okhttp3.internal.http.StatusLine;

public class DavPropertySet {
    public static DavPropertySet read(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        DavPropertySet result = new DavPropertySet();

        parser.require(XmlPullParser.START_TAG, XmlHelper.NS, "propstat");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
                case "status":
                    StatusLine statusLine = StatusLine.parse(XmlHelper.readText(parser));
                    result.mStatus = statusLine.code;
                    break;
                case "prop":
                    result.readProperties(parser);
                    break;
                default:
                    XmlHelper.skip(parser);
                    break;
            }
        }

        return result;
    }

    private int mStatus;
    private Date mLastModified;

    public int getStatus() {
        return mStatus;
    }

    public Date getLastModified() {
        return mLastModified;
    }

    private void readProperties(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        parser.require(XmlPullParser.START_TAG, XmlHelper.NS, "prop");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
                case "getlastmodified":
                    mLastModified = XmlHelper.MODIFICATION_DATE_FORMAT.parse(XmlHelper.readText(parser));
                    break;
                default:
                    XmlHelper.skip(parser);
                    break;
            }
        }
    }
}
