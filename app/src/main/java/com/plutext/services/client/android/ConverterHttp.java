/*
 *  Copyright 2015-2016, Plutext Pty Ltd.
 *   
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

 */
package com.plutext.services.client.android;


import android.util.Log;

import com.google.common.io.ByteStreams;


import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Java client library for Plutext PDF Converter
 * 
 * @since 3.3.0
 */
public class ConverterHttp implements Converter {

    private static final String TAG = ConverterHttp.class.getSimpleName();

	private String endpointURL = null;


	public ConverterHttp() {
	}

	public ConverterHttp(String endpointURL) {

        Log.e(TAG, "starting, with endpointURL: " + endpointURL);

		if (endpointURL!=null) {
			this.endpointURL = endpointURL;
		}

	}



	/**
	 * Convert File fromFormat to toFormat, streaming result to OutputStream os.
	 *
	 * fromFormat supported: DOC, DOCX
	 *
	 * toFormat supported: PDF
	 *
	 * @param f
	 * @param fromFormat
	 * @param toFormat
	 * @param os
	 * @throws IOException
	 * @throws ConversionException
	 */
	public void convert(File f, Format fromFormat, Format toFormat, OutputStream os) throws IOException, ConversionException {

		checkParameters(fromFormat, toFormat);

		URL url;
		HttpURLConnection connection = null;
		try {
			//Create connection
			url = new URL(getUrlForFormat(toFormat));
			connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("POST");

			connection.setUseCaches (false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			connection.setChunkedStreamingMode(0); // default size
			ByteStreams.copy(new FileInputStream(f), connection.getOutputStream());
			/* Avoid using BufferedOutputStream here, as in:
				ByteStreams.copy(instream,
						new BufferedOutputStream(connection.getOutputStream()));
			*/

			//Get Response
			InputStream is = new BufferedInputStream(connection.getInputStream());
			ByteStreams.copy(is, os);

		} catch (Exception e) {

			e.printStackTrace();
			throw new ConversionException("Problem converting File", e);

		} finally {

			if(connection != null) {
				connection.disconnect();
			}
		}

	}

	private String getUrlForFormat(Format toFormat) {

        if (Format.TOC.equals(toFormat)) {
        	//httppost = new HttpPost(URL+"/?bookmarks");
//        	System.out.println(URL+"?format=application/json");
        	return (endpointURL+"?format=application/json");

        } else if (Format.DOCX.equals(toFormat)) {

        	return (endpointURL+"?application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        } else {
        	return (endpointURL);
        }

	}




	/**
	 * Convert InputStream fromFormat to toFormat, streaming result to OutputStream os.
	 *
	 * fromFormat supported: DOC, DOCX
	 *
	 * toFormat supported: PDF
	 *
	 * Note this uses a non-repeatable request entity, so it may not be suitable
	 * (depending on the endpoint config).
	 *
	 * @param instream
	 * @param fromFormat
	 * @param toFormat
	 * @param os
	 * @throws IOException
	 * @throws ConversionException
	 */
 	public void convert(InputStream instream, Format fromFormat, Format toFormat, OutputStream os) throws IOException, ConversionException {

		checkParameters(fromFormat, toFormat);

		URL url;
		HttpURLConnection connection = null;
		try {
			//Create connection
			url = new URL(getUrlForFormat(toFormat));
			connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("POST");

			connection.setUseCaches (false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			connection.setChunkedStreamingMode(0); // default size
			ByteStreams.copy(instream, connection.getOutputStream());
			/* Avoid using BufferedOutputStream here, as in:
				ByteStreams.copy(instream,
						new BufferedOutputStream(connection.getOutputStream()));
			*/

//			byte[] bytes = ByteStreams.toByteArray(instream);
//			connection.getOutputStream().write(bytes);

			//Get Response
			InputStream is = new BufferedInputStream(connection.getInputStream());
			ByteStreams.copy(is, os);

		} catch (Exception e) {

			e.printStackTrace();
			throw new ConversionException("Problem converting InputStream", e);

		} finally {

			if(connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Convert byte array fromFormat to toFormat, streaming result to OutputStream os.
	 *
	 * fromFormat supported: DOC, DOCX
	 *
	 * toFormat supported: PDF
	 *
	 * @param bytesIn
	 * @param fromFormat
	 * @param toFormat
	 * @param os
	 * @throws IOException
	 * @throws ConversionException
	 */
	public void convert(byte[] bytesIn, Format fromFormat, Format toFormat, OutputStream os) throws IOException, ConversionException {

		checkParameters(fromFormat, toFormat);

		URL url;
		HttpURLConnection connection = null;
		try {
			//Create connection
			url = new URL(getUrlForFormat(toFormat));
			connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("POST");

			connection.setFixedLengthStreamingMode(bytesIn.length);

			connection.setUseCaches (false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// If you need to monitor upload progress,
			// see https://stackoverflow.com/questions/18100096/is-there-any-way-to-get-upload-progress-correctly-with-httpurlconncetion

			connection.getOutputStream().write(bytesIn);;

			//Get Response
			InputStream is = new BufferedInputStream(connection.getInputStream());
			ByteStreams.copy(is, os);

		} catch (Exception e) {

			e.printStackTrace();
			throw new ConversionException("Problem converting byte[]", e);

		} finally {

			if(connection != null) {
				connection.disconnect();
			}
		}


	}



//	private ContentType map(Format f) {
//
//		if (Format.DOCX.equals(f)) {
//			return ContentType.create("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
//		} else if (Format.DOC.equals(f)) {
//			return ContentType.create("application/msword");
//		}
//		return null;
//	}

	private void checkParameters(Format fromFormat, Format toFormat) throws ConversionException {

		if (endpointURL==null) {
			throw new ConversionException("Endpoint URL not configured.");
		}

		if ( Format.DOCX.equals(fromFormat) ||  Format.DOC.equals(fromFormat) ) {
			// OK
		} else {
			throw new ConversionException("Conversion from format " + fromFormat + " not supported");
		}

		if (Format.PDF.equals(toFormat) || Format.TOC.equals(toFormat)
				|| Format.DOCX.equals(toFormat)) {
			// OK
		} else {
			throw new ConversionException("Conversion to format " + toFormat + " not supported");
		}
		
	}
	
}
