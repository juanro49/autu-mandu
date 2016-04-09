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
import java.util.LinkedList;
import java.util.List;

public class DavResponse {
    public static DavResponse read(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        DavResponse result = new DavResponse();
        List<DavPropertySet> propertySets = new LinkedList<>();

        parser.require(XmlPullParser.START_TAG, XmlHelper.NS, "response");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            switch (name) {
                case "href":
                    result.mHref = XmlHelper.readText(parser);
                    break;
                case "propstat":
                    propertySets.add(DavPropertySet.read(parser));
                    break;
                default:
                    XmlHelper.skip(parser);
                    break;
            }
        }

        result.mPropertySets = propertySets.toArray(new DavPropertySet[propertySets.size()]);
        return result;
    }

    private String mHref;
    private DavPropertySet[] mPropertySets;

    public String getHref() {
        return mHref;
    }

    public DavPropertySet[] getPropertySets() {
        return mPropertySets;
    }
}
