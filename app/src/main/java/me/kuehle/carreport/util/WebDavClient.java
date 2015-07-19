/*
 * Copyright 2015 Jan Kühle
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
package me.kuehle.carreport.util;

import android.net.Uri;
import android.util.Log;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class WebDavClient extends HttpClient {
    private static final String TAG = "WebDavClient";
    private static final String USER_AGENT = "Android-me-kuehle-carreport";

    private Uri mBaseUri;

    public WebDavClient(String baseUrl, String userName, String password) {
        mBaseUri = Uri.parse(baseUrl);
        getParams().setParameter(HttpMethodParams.USER_AGENT, USER_AGENT);
        getParams().setAuthenticationPreemptive(true);
        getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(userName, password));
    }

    public boolean download(String remoteFilepath, File targetFile) throws IOException {
        GetMethod get = new GetMethod(mBaseUri.buildUpon().appendPath(remoteFilepath).toString());
        int status = executeMethod(get);
        if (status != HttpStatus.SC_OK) {
            return false;
        }

        BufferedInputStream bis = new BufferedInputStream(get.getResponseBodyAsStream());
        FileOutputStream fos = new FileOutputStream(targetFile);
        FileCopyUtil.copyFile(bis, fos);
        fos.close();
        bis.close();

        return true;
    }

    public boolean upload(File localFile, String remoteFilepath, String contentType) throws IOException {
        PutMethod put = new PutMethod(mBaseUri.buildUpon().appendPath(remoteFilepath).toString());
        put.setRequestEntity(new FileRequestEntity(localFile, contentType));
        int status = executeMethod(put);
        return status == HttpStatus.SC_CREATED || status == HttpStatus.SC_NO_CONTENT;
    }

    public Date getLastModified(String remoteFilepath) throws Exception {
        DavPropertyNameSet requestedProps = new DavPropertyNameSet();
        requestedProps.add(DavPropertyName.GETLASTMODIFIED);

        PropFindMethod propFind = new PropFindMethod(
                mBaseUri.buildUpon().appendPath(remoteFilepath).toString(),
                DavConstants.PROPFIND_BY_PROPERTY,
                requestedProps,
                DavConstants.DEPTH_0);
        int status = executeMethod(propFind);
        if (status == HttpStatus.SC_MULTI_STATUS) {
            MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
            MultiStatusResponse[] responses = multiStatus.getResponses();
            if (responses.length != 1) {
                throw new Exception("Multi status response does not contain exactly one response.");
            }

            DavProperty lastModifiedProp = responses[0].getProperties(HttpStatus.SC_OK).get(DavPropertyName.GETLASTMODIFIED);
            if (lastModifiedProp == null || lastModifiedProp.getValue() == null) {
                throw new Exception("Multi status response does not contain exactly one response.");
            }

            return DavConstants.modificationDateFormat.parse((String) lastModifiedProp.getValue());
        }

        return null;
    }

    public boolean testLogin() {
        try {
            HeadMethod head = new HeadMethod(mBaseUri.toString());
            int status = executeMethod(head);
            if (status == HttpStatus.SC_OK) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error testing login.", e);
        }

        return false;
    }
}
